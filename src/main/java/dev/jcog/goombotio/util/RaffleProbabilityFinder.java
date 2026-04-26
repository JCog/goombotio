package dev.jcog.goombotio.util;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.*;

/*
 created by RubixCetera
 */

public class RaffleProbabilityFinder {
    private static final int STEPS_INITIAL = 30;
    private static final int STEPS_CONTINUE = 8;

    static {
        // make sure the classes are loaded before starting timer for first run
        Finder.init();
        Finder.Collector.init();
        RapidWeightedPicker.init();
    }

    /**
     * find the probability for a given participant to win the raffle. doing this exhaustively is very time-consuming,
     * so we instead find as close an estimate as we can.
     *
     * <p>note that although it does report an accuracy of the estimate, there is no option to run until a certain
     * accuracy is achieved. this is because in many cases, including this one, mean and variance are in some way
     * correlated. this means that running until variance is below a certain point will bias the result in the
     * direction that correlates to lower variance, as the other side is more likely to be resampled.
     *
     * <p>known issues: the result biases very slightly upward. this is presumably an artifact of the above
     * mean-variance correlation. fixing this would require more investigation, but it is small enough to be assumed
     * negligible.
     *
     * @param weights     relative weights of each participant. does not need to be normalized.
     * @param winCount    number of participants to be selected as winners.
     * @param targetIndex index of the participant in question, corresponding to {@code weights}
     * @param timeout     time to wait before stopping
     * @return a {@link Result} containing the estimated probability of winning and the standard error of the estimate
     */
    public static Result getWinChance(List<Double> weights, int winCount, int targetIndex, Duration timeout) {
        // trivial cases
        if (winCount <= 0) return new Result(0, 0);
        if (weights.size() <= winCount) return new Result(1, 0);

        List<Double> others = new ArrayList<>(weights);
        others.remove(targetIndex);

        Finder finder = new Finder(others, winCount, weights.get(targetIndex));
        new Thread(() -> {
            try {
                Thread.sleep(timeout);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            finder.finish();
        }).start();

        return finder.run();
    }

    /**
     * search is split into multiple collectors, one for each 'nth winner'. this way for each layer, we can
     * search only the space where the target does not previously win. this means we can always take the
     * probability of immediately winning and combine the results at the end.
     *
     * <p>note that the probability of any situation and probability of winning at that point are correlated. if the
     * path goes through heavier weights, the situation is more likely to occur and the target is more likely to
     * win. this means we cannot select parts of the search space equally and weight the samples. instead, we must
     * simulate the raffle up to that point.
     */
    private static class Finder {
        private final double target;
        private final List<Collector> collectors = new ArrayList<>();
        private final RapidWeightedPicker picker;
        private boolean finished = false;

        private static void init() {
        }

        public Finder(List<Double> others, int winCount, double target) {
            this.target = target;
            this.picker = new RapidWeightedPicker(others, true);
            // skip the first winner as it's trivial
            for (int depth = 1; depth < winCount; depth++) {
                var collector = new Collector(depth);
                collectors.add(collector);
            }
        }

        public Result run() {
            // sample everything at least some before waiting for the timeout
            collectors.forEach(collector -> collector.advance(STEPS_INITIAL));
            while (!finished) {
                collectors.forEach(collector -> collector.advance(STEPS_CONTINUE));
            }

            // do the first winner specially
            double mean = 1 - target / (picker.sum() + target);
            double var = 0;
            // do everything else
            for (var collector : collectors) {
                // we need to multiply compliments rather than add
                double itemMean = 1 - collector.estimate();
                double itemVar = collector.errorVariance();
                var = var * itemVar + var * itemMean * itemMean + itemVar * mean * mean;
                mean *= itemMean;
            }
            return new Result(1 - mean, Math.sqrt(var));
        }

        public void finish() {
            finished = true;
        }

        private class Collector {
            private double mean = 0;
            private double meanDelta2 = 0;
            protected long count = 0;
            private final int depth;

            private static void init() {
            }

            protected Collector(int depth) {
                this.depth = depth;
            }

            void advance(int steps) {
                for (int j = 0; j < steps; j++) {
                    for (int i = 0; i < depth; i++) {
                        picker.removeRandom();
                    }
                    double value = target / (picker.sum() + target);
                    count++;
                    double oldMean = mean;
                    mean += (value - oldMean) / count;
                    meanDelta2 += (value - mean) * (value - oldMean);
                    picker.reset();
                }
            }

            public double estimate() {
                return mean;
            }

            public double errorVariance() {
                // /(count-1) for sample variance, /count to turn variance into error
                return meanDelta2 / ((count - 1) * count);
            }

        }

    }

    public record Result(double estimate, double stdError) {
    }

    /**
     * simulates repeated raffles with minimal overhead, configured for speed.
     *
     * <p>uses a tree (more of a trie) with the weights of participants solely at the leaves, with each node representing
     * the sum of all weights of its children. this allows it to very efficiently select a winner while accounting for
     * weights, then readjust the tree to make it valid again. this also improves numeric stability: even with rounding,
     * the state of the tree is based purely on what has been selected and not the order.
     */
    public static class RapidWeightedPicker {
        private final int[] ids;
        /// buffer for implicit tree; 1-indexed by not using item 0 as it makes the math work out more nicely
        private final double[] buffer;
        private final double[] initialBuffer;
        private final int[] resetStack;
        private final Random random = new Random();

        private static void init() {
        }


        public RapidWeightedPicker(List<Double> weights, boolean reusable) {
            int size = weights.size();
            this.resetStack = new int[Integer.numberOfTrailingZeros(Integer.highestOneBit(size-1)) + 1];
            List<PickerSortEntry> entries = new ArrayList<>(weights.size());
            int i = 0;
            for (double value : weights) {
                entries.add(new PickerSortEntry(i++, value));
            }
            entries.sort(Comparator.comparing(PickerSortEntry::weight));
            // sort to make reset marginally faster, probably
            // split into two halves so the heaviest values are actually at the far right side of the tree
            int split = Integer.highestOneBit(size - 1) * 2 - size;
            buffer = new double[Math.max(size, 1) * 2];
            ids = new int[size];
            i = split;
            for (var entry : entries) {
                ids[i] = entry.id();
                buffer[size + i] = entry.weight();
                i = (i + 1) % size;
            }

            for (i = size - 1; i > 0; i--) {
                buffer[i] = buffer[i * 2] + buffer[i * 2 + 1];
            }
            initialBuffer = reusable ? Arrays.copyOf(buffer, buffer.length) : null;
        }

        public double sum() {
            return buffer[1];
        }

        /**
         * select a random participant (weighted) and remove it from the pool, returning its original index (primarily
         * for testing purposes, the result is never needed by the algorithm).
         *
         * @return id of the winner, or -1 if no winners remain
         */
        public int removeRandom() {
            var position = random.nextDouble(sum());
            // start at root
            int index = 1;
            while (index < buffer.length >> 1) {
                // move to left child
                index <<= 1;
                // if the position is in the sibling, move there instead
                double positionInSibling = position - buffer[index];
                if (positionInSibling >= 0) {
                    position = positionInSibling;
                    index++;
                }
            }
            // if empty, no need to adjust the tree
            if (buffer[index] == 0) return -1;
            int result = ids[index - ids.length];
            buffer[index] = 0;
            double climbValue = 0;
            // climb back to the root of the tree, adjusting values. instead of querying both children for each node,
            // we keep the result from previous iteration and only query the sibling.
            while (index > 1) {
                // because of the layout, siblings always differ by only the final bit
                climbValue += buffer[index ^ 1];
                index >>= 1;
                buffer[index] = climbValue;
            }
            return result;
        }

        /**
         * reset the picker to the point where nothing has been selected
         */
        public void reset() {
            if (initialBuffer == null) {
                throw new UnsupportedOperationException();
            }
            int i = 1;
            int stackIndex = 0;
            // traverse through the tree, if the node has changed then copy it back and traverse into children if it has any
            while (true) {
                if (buffer[i] != initialBuffer[i]) {
                    buffer[i] = initialBuffer[i];
                    i <<= 1;
                    // unlike an implicit heap, we never have any nodes with exactly 1 child
                    if (i < buffer.length) {
                        // save right child for later
                        resetStack[stackIndex++] = i + 1;
                        // immediately handle left
                        continue;
                    }
                }
                // if node is correct, all its children are correct, so move back
                if (stackIndex == 0) return;
                i = resetStack[--stackIndex];
            }
        }
    }

    private record PickerSortEntry(int id, double weight) {
    }


    public static class Testing {

        public static List<Double> getSetup(int participants, double scale) {
            List<Double> weights = new ArrayList<>(participants);
            Random random = new Random();
            double randRangeStart = 1d / scale;
            double randRangeScale = 1d - randRangeStart;
            for (int i = 0; i < participants; i++) {
                weights.add(1 / (random.nextDouble() * randRangeScale + randRangeStart));
            }
            return weights;
        }

        public static void runTest(List<Double> weights, int winners, Duration timeout, boolean silent, boolean summaryOnly) {
            int participants = weights.size();
            if (!silent && !summaryOnly) {
                System.out.printf("participants: %d\nwinners: %d\ntimeout: %s\n", participants, winners, timeout.toString());
                System.out.println("  id      share%        win%    rel err%           nanos");
            }
            double total = weights.stream().mapToDouble(x -> x).sum();
            double sumShare = 0;
            double sumEstimate = 0;
            long sumNanos = 0;
            for (int i = 0; i < participants; i++) {
                double share = weights.get(i) / total;
                long startTime = Clock.getNow();
                Result result = getWinChance(weights, winners, i, timeout);
                long endTime = Clock.getNow();
                if (!silent && !summaryOnly)
                    System.out.printf("%4d%12.6g%12.6g%12.6g%,16d\n", i, share * 100, result.estimate() * 100, result.stdError() / result.estimate() * 100, endTime - startTime);
                sumShare += share;
                sumEstimate += result.estimate();
                sumNanos += endTime - startTime;
            }
            if (!silent) {
                System.out.printf(" sum%12.6g%12.6g%,28d\n", sumShare * 100, sumEstimate * 100, sumNanos);
            }
        }

        public static void compareToSimulate(List<Double> weights, int winners, Duration timeout, Duration simulateTimeout) {
            int participants = weights.size();
            List<Double> winRates = new ArrayList<>(participants);
            int[] observedWins = new int[participants];
            for (int i = 0; i < participants; i++) {
                winRates.add(getWinChance(weights, winners, i, timeout).estimate());
            }

            long endTime = Clock.getNow() + simulateTimeout.toNanos();
            var picker = new RapidWeightedPicker(weights, true);
            double totalWeight = weights.stream().mapToDouble(x -> x).sum();
            int total = 0;
            while (Clock.getNow() < endTime) {
                for (int j = 0; j < 64; j++) {
                    for (int i = 0; i < winners; i++) {
                        int index = picker.removeRandom();
                        observedWins[index]++;
                    }
                    picker.reset();
                    total++;
                }
            }
            System.out.printf("trials: %,d\n", total);
            System.out.println("  id      share%   predicted    observed       ratio");
            for (int i = 0; i < participants; i++) {
                double share = weights.get(i) / totalWeight * 100;
                double expected = winRates.get(i) * 100;
                double observed = ((double) observedWins[i]) / total * 100;
                System.out.printf("%4d%12.6g%12.6g%12.6g%12.6g\n", i, share, expected, observed, expected / observed);
            }
        }

        public static void testPicker(List<Double> weights, Set<Integer> toRemove, int trials) {
            var picker = new RapidWeightedPicker(weights, true);
            int[] recorded = new int[weights.size()];
            Set<Integer> actualRemoved = new HashSet<>();
            for (int i = 0; i < trials; i++) {
                while (true) {
                    picker.reset();
                    actualRemoved.clear();
                    for (int j = 0; j < toRemove.size(); j++) {
                        actualRemoved.add(picker.removeRandom());
                    }
                    if (!actualRemoved.equals(toRemove)) continue;
                    int chosen = picker.removeRandom();
                    recorded[chosen]++;
                    break;
                }
            }

            double total = 0;
            for (int i = 0; i < weights.size(); i++) {
                if (toRemove.contains(i)) continue;
                total += weights.get(i);
            }

            System.out.println("  id     chance%   observed%       ratio");
            for (int i = 0; i < weights.size(); i++) {
                double chance = weights.get(i) / total * 100;
                double observed = (double) recorded[i] / trials * 100;
                if (toRemove.contains(i)) {
                    System.out.printf("%4d%12s\n", i, "(%.5f)".formatted(chance));
                } else {
                    System.out.printf("%4d%12.5g%12.5g%12.5g\n", i, chance, observed, observed / chance);
                }
            }

        }

        /**
         * abstraction to use cpu time if available (as its more accurate for timing stuff) but fall back to wall time if not
         */
        public static class Clock {
            private static final Impl IMPL;

            static {
                var bean = ManagementFactory.getThreadMXBean();
                if (bean.isThreadCpuTimeEnabled()) {
                    IMPL = new Impl() {
                        @Override
                        protected long getNow() {
                            return bean.getCurrentThreadCpuTime();
                        }

                        @Override
                        protected boolean isCpuTime() {
                            return true;
                        }
                    };
                } else {
                    if (bean.isThreadCpuTimeSupported()) {
                        System.err.println("warning: thread time is supported but VM has disabled it angyface");
                    }
                    IMPL = new Impl() {
                        @Override
                        protected long getNow() {
                            return System.nanoTime();
                        }

                        @Override
                        protected boolean isCpuTime() {
                            return false;
                        }
                    };
                }
            }

            static long getNow() {
                return IMPL.getNow();
            }

            static boolean isCpuTime() {
                return IMPL.isCpuTime();
            }

            private static abstract class Impl {
                abstract protected long getNow();

                abstract protected boolean isCpuTime();
            }

        }
    }
}
