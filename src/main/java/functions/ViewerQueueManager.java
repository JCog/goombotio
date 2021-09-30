package functions;

import com.gikk.twirk.types.users.TwitchUser;
import com.google.common.collect.ComparisonChain;
import database.DbManager;
import database.entries.ViewerQueueEntry;
import database.misc.ViewerQueueDb;
import util.TwirkInterface;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

import static java.lang.System.out;

public class ViewerQueueManager {

    private final TwirkInterface twirk;
    private final ViewerQueueDb viewerQueueDb;
    private ArrayList<ViewerQueueEntry> viewers;
    private int position;
    private int requestedCount;
    private String message;

    public ViewerQueueManager(TwirkInterface twirk, DbManager dbManager) {
        this.twirk = twirk;
        viewerQueueDb = dbManager.getViewerQueueDb();
        viewers = new ArrayList<>();
        position = 0;
        message = "";
    }

    public void startNewSession(int count, String message) {
        viewers = new ArrayList<>();
        viewerQueueDb.incrementSessionId();
        this.requestedCount = count;
        twirk.channelMessage("JCog is looking for viewers to play with! If you want to join, type \"!join\" to have a" +
                                     " chance. Subs have priority, but you can still get in if there are no more subs left in the queue.");
        out.println("Starting viewer queue");
        this.message = message;
    }

    public void addViewer(TwitchUser twitchUser) {
        for (ViewerQueueEntry entry : viewers) {
            if (entry.id == twitchUser.getUserID()) {
                return;
            }
        }
        ViewerQueueEntry entry = viewerQueueDb.getUser(twitchUser.getUserID());
        viewerQueueDb.setUserAttempted(twitchUser.getUserID());
        entry.subbed = twitchUser.isSub();
        entry.username = twitchUser.getDisplayName();
        viewers.add(entry);
        out.printf("%s joined the queue%n", entry.username);
    }

    public void closeCurrentSession() {
        viewers.sort(new ViewersComparator());
        printQueue();
        out.println("Inviting the first " + requestedCount);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Time's up! I choose ");
        int i;
        for (i = 0; i < requestedCount - 1 && i < viewers.size() - 1; i++) {
            stringBuilder.append(String.format("@%s, ", viewers.get(i).username));
        }
        if (i == 1) {
            stringBuilder.deleteCharAt(stringBuilder.length() - 2);
        }
        if (i > 0) {
            stringBuilder.append(String.format("and @%s! ", viewers.get(i).username));
        }
        stringBuilder.append("Check your whispers for info on how to join. jcogComfy");
        twirk.channelMessage(stringBuilder.toString());
        Thread thread = new Thread(() -> {
            for (int j = 0; j < requestedCount && j < viewers.size(); j++) {
                getNext();
                try {
                    TimeUnit.MILLISECONDS.sleep(1000);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    public void getNext() {
        if (position < viewers.size()) {
            ViewerQueueEntry nextUser = viewers.get(position);
            position++;
            viewerQueueDb.setUserJoined(nextUser.id);
            twirk.whisper(nextUser.username, message);
            out.println("inviting " + nextUser.username);
        }
    }

    private void printQueue() {
        out.println("Sorted queue:");
        out.println("(Username, ID, Subbed, Attempts, Total Sessions, Last Session ID)");
        for (int i = 0; i < viewers.size(); i++) {
            out.printf("%d. %s%n", i + 1, viewers.get(i).toString());
        }
    }

    private static class ViewersComparator implements Comparator<ViewerQueueEntry> {

        @Override
        public int compare(ViewerQueueEntry viewer1, ViewerQueueEntry viewer2) {
            return ComparisonChain.start()
                    .compareTrueFirst(viewer1.subbed, viewer2.subbed)
                    .compare(viewer2.attemptsSinceLastSession, viewer1.attemptsSinceLastSession)
                    .compare(viewer1.totalSessions, viewer2.totalSessions)
                    .compare(viewer1.lastSessionId, viewer2.lastSessionId)
                    .result();
        }
    }
}
