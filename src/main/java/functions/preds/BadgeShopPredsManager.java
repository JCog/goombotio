package functions.preds;

import com.github.twitch4j.helix.domain.Moderator;
import com.github.twitch4j.helix.domain.Subscription;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import database.preds.BadgeShopLeaderboardDb;
import util.CommonUtils;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.System.out;

public class BadgeShopPredsManager extends PredsManagerBase {
    private static final String START_MESSAGE =
            "Get your predictions in! Send a message with three of either BadSpin1 BadSpin2 BadSpin3 or SpoodlySpun " +
            "(or a message with 3 digits from 1 to 4) to guess the order the badges will show up in the badge shop! " +
            "If you get all three right and don't have a sub, you'll win one! Type !preds to learn more.";
    private static final String ANSWER_REGEX = "[1-4]{3}";
    
    private static final String DISCORD_CHANNEL_WINS = "badge-shop-wins";
    private static final String DISCORD_CHANNEL_POINTS = "badge-shop-points";
    private static final int POINTS_3 = 20;
    private static final int POINTS_2 = 5;
    private static final int POINTS_1 = 1;
    private static final int POINTS_WRONG_ORDER = 1;
    private static final int REWARD_3_CORRECT = 20;
    private static final int REWARD_2_CORRECT = 5;
    private static final int REWARD_1_CORRECT = 2;
    private static final int REWARD_WRONG_ORDER = 2;
    private static final int REWARD_0_CORRECT = 1;
    private static final Set<String> BADGE_CHOICES = new HashSet<>(Arrays.asList(
            "badspin1",
            "badspin2",
            "badspin3",
            "spoodlyspun"
    ));

    public enum Badge {
        BAD_SPIN1,
        BAD_SPIN2,
        BAD_SPIN3,
        SPOODLY_SPUN
    }
    
    private record PapePredsObject(String displayName, Badge left, Badge middle, Badge right) {}
    
    private final Map<String, PapePredsObject> predictionList = new HashMap<>();
    private final BadgeShopLeaderboardDb badgeShopLeaderboardDb;
    
    public BadgeShopPredsManager(CommonUtils commonUtils) {
        super(commonUtils, START_MESSAGE, ANSWER_REGEX);
        badgeShopLeaderboardDb = commonUtils.dbManager().getBadgeShopLeaderboardDb();
    }

    /**
     * Given the three correct badges, sets the game to an ended state, determines and records points won, and sends a
     * message to the chat to let them know who, if anyone, got all three correct, as well as the current monthly
     * leaderboard.
     */
    @Override
    public void submitPredictions(String answer) {
        Badge one = Badge.values()[Character.getNumericValue(answer.charAt(0)) - 1];
        Badge two = Badge.values()[Character.getNumericValue(answer.charAt(1)) - 1];
        Badge three = Badge.values()[Character.getNumericValue(answer.charAt(2)) - 1];
        isEnabled = false;
        waitingForAnswer = false;

        List<String> winners = getWinners(one, two, three);
        List<String> unsubbedWinners = getUnsubbedWinners(winners);
        StringBuilder message = new StringBuilder();
        switch (winners.size()) {
            case 0 -> message.append("Nobody guessed it. jcogThump");
            case 1 -> message.append(String.format(
                    "Congrats to @%s%s on guessing correctly! jcogChamp",
                    winners.get(0),
                    unsubbedWinners.contains(winners.get(0).toLowerCase()) ? "*" : ""
            ));
            case 2 -> message.append(String.format(
                    "Congrats to @%s%s and @%s%s on guessing correctly! jcogChamp",
                    winners.get(0),
                    unsubbedWinners.contains(winners.get(0).toLowerCase()) ? "*" : "",
                    winners.get(1),
                    unsubbedWinners.contains(winners.get(1).toLowerCase()) ? "*" : ""
            ));
            default -> {
                message.append("Congrats to ");
                for (int i = 0; i < winners.size() - 1; i++) {
                    message.append("@").append(winners.get(i));
                    if (unsubbedWinners.contains(winners.get(i).toLowerCase())) {
                        message.append("*");
                    }
                    message.append(", ");
                }
                message.append("and @").append(winners.get(winners.size() - 1));
                if (unsubbedWinners.contains(winners.get(winners.size() - 1).toLowerCase())) {
                    message.append("*");
                }
                message.append(" on guessing correctly! jcogChamp");
            }
        }
        message.append(" Use !raffle to check your updated entry count.");
    
        twitchApi.channelAnnouncement(String.format(
                "The correct answer was %s %s %s - %s",
                badgeToString(one),
                badgeToString(two),
                badgeToString(three),
                message
        ));
    
        updateDiscordLeaderboardWins(
                DISCORD_CHANNEL_WINS,
                "Badge Shop Prediction Wins:",
                badgeShopLeaderboardDb.getAllSortedWins()
        );
    
        updateDiscordLeaderboardPoints(
                DISCORD_CHANNEL_POINTS,
                "Badge Shop Prediction Points:",
                badgeShopLeaderboardDb.getAllSortedPoints()
        );
    }

    @Override
    public void makePredictionIfValid(String userId, String displayName, String message) {
        String[] split = message.split("\\s");

        if (split.length == 3) {
            //using FFZ emotes
            List<String> badgeGuess = new ArrayList<>();
            for (String word : split) {
                if (BADGE_CHOICES.contains(word.toLowerCase()) && !badgeGuess.contains(word.toLowerCase())) {
                    badgeGuess.add(word.toLowerCase());
                }
            }

            if (badgeGuess.size() == 3) {
                predictionList.put(userId, new PapePredsObject(
                        displayName,
                        stringToBadge(badgeGuess.get(0)),
                        stringToBadge(badgeGuess.get(1)),
                        stringToBadge(badgeGuess.get(2))
                ));
                System.out.printf(
                        "%s has predicted %s %s %s%n",
                        displayName,
                        badgeGuess.get(0),
                        badgeGuess.get(1),
                        badgeGuess.get(2)
                );
            }
        } else if (split.length == 1 && split[0].matches("[1-4]{3}")) {
            //using numbers, e.g. "412"
            List<Integer> badgeGuess = new ArrayList<>();
            for (int i = 0; i < split[0].length(); i++) {
                int guess = Character.getNumericValue(split[0].charAt(i));
                if (!badgeGuess.contains(guess)) {
                    badgeGuess.add(guess);
                }
            }

            if (badgeGuess.size() == 3) {
                Badge badge1 = intToBadge(badgeGuess.get(0));
                Badge badge2 = intToBadge(badgeGuess.get(1));
                Badge badge3 = intToBadge(badgeGuess.get(2));

                predictionList.put(userId, new PapePredsObject(displayName, badge1, badge2, badge3));

                System.out.printf(
                        "%s has predicted %s %s %s%n",
                        displayName,
                        badgeToString(badge1),
                        badgeToString(badge2),
                        badgeToString(badge3)
                );
            }
        }
    }
    
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private List<String> getWinners(Badge leftAnswer, Badge middleAnswer, Badge rightAnswer) {
        Set<Badge> answerSet = new HashSet<>();
        answerSet.add(leftAnswer);
        answerSet.add(middleAnswer);
        answerSet.add(rightAnswer);
        
        Set<String> modIds = twitchApi.getMods(twitchApi.getStreamerUser().getId())
                .stream()
                .map(Moderator::getUserId)
                .collect(Collectors.toSet());

        List<String> winners = new ArrayList<>();
        for (Map.Entry<String, PapePredsObject> pred : predictionList.entrySet()) {
            String userId = pred.getKey();
            String displayName = pred.getValue().displayName();
            Badge leftGuess = pred.getValue().left();
            Badge middleGuess = pred.getValue().middle();
            Badge rightGuess = pred.getValue().right();
            Set<Badge> guessSet = new HashSet<>();
            guessSet.add(leftGuess);
            guessSet.add(middleGuess);
            guessSet.add(rightGuess);

            int vipRaffleEntries;
            if (leftGuess == leftAnswer && middleGuess == middleAnswer && rightGuess == rightAnswer) {
                winners.add(displayName);
                badgeShopLeaderboardDb.addWin(userId, displayName);
                badgeShopLeaderboardDb.addPoints(userId, displayName, POINTS_3);
                vipRaffleEntries = REWARD_3_CORRECT;
                out.printf("%s guessed 3 correctly. Adding %d points and a win.%n", displayName, POINTS_3);
            } else if ((leftGuess == leftAnswer && middleGuess == middleAnswer) ||
                    (leftGuess == leftAnswer && rightGuess == rightAnswer) ||
                    (middleGuess == middleAnswer && rightGuess == rightAnswer)) {
                badgeShopLeaderboardDb.addPoints(userId, displayName, POINTS_2);
                vipRaffleEntries = REWARD_2_CORRECT;
                out.printf("%s guessed 2 correctly. Adding %d points.%n", displayName, POINTS_2);
            } else if (leftGuess == leftAnswer || middleGuess == middleAnswer || rightGuess == rightAnswer) {
                badgeShopLeaderboardDb.addPoints(userId, displayName, POINTS_1);
                vipRaffleEntries = REWARD_1_CORRECT;
                out.printf("%s guessed 1 correctly. Adding %d point.%n", displayName, POINTS_1);
            } else if (answerSet.equals(guessSet)) {
                badgeShopLeaderboardDb.addPoints(userId, displayName, POINTS_WRONG_ORDER);
                vipRaffleEntries = REWARD_WRONG_ORDER;
                out.printf("%s guessed 0 correctly, but got all 3 badges. Adding %d point.%n",
                        displayName, POINTS_WRONG_ORDER);
            } else {
                vipRaffleEntries = REWARD_0_CORRECT;
                out.printf("%s guessed 0 correctly.%n", displayName);
            }
    
            if (!modIds.contains(userId) && !vipDb.isPermanentVip(userId)) {
                vipRaffleDb.incrementEntryCount(userId, displayName, vipRaffleEntries);
            }
        }
        return winners;
    }
    
    private List<String> getUnsubbedWinners(List<String> winners) {
        if (winners.size() == 0) {
            return new ArrayList<>();
        }
    
        List<String> unsubbedWinners = new ArrayList<>();
        List<String> subList;
        try {
            subList = twitchApi.getSubList(twitchApi.getStreamerUser().getId())
                    .stream()
                    .map(Subscription::getUserLogin)
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());
        } catch (HystrixRuntimeException e) {
            out.println("Unable to get sub status of winners.");
            return unsubbedWinners;
        }
        
        for (String winner : winners) {
            if (!subList.contains(winner.toLowerCase())) {
                unsubbedWinners.add(winner);
            }
        }
        return unsubbedWinners;
    }

    private static String badgeToString(Badge badge) {
        return switch (badge) {
            case BAD_SPIN1 -> "BadSpin1";
            case BAD_SPIN2 -> "BadSpin2";
            case BAD_SPIN3 -> "BadSpin3";
            default -> "SpoodlySpun";
        };
    }

    private static Badge stringToBadge(String badge) {
        return switch (badge.toLowerCase()) {
            case "badspin1" -> Badge.BAD_SPIN1;
            case "badspin2" -> Badge.BAD_SPIN2;
            case "badspin3" -> Badge.BAD_SPIN3;
            case "spoodlyspun" -> Badge.SPOODLY_SPUN;
            default -> null;
        };
    }

    private static Badge intToBadge(int badge) {
        return switch (badge) {
            case 1 -> Badge.BAD_SPIN1;
            case 2 -> Badge.BAD_SPIN2;
            case 3 -> Badge.BAD_SPIN3;
            default -> Badge.SPOODLY_SPUN;
        };
    }
    
}
