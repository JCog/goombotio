package listeners.commands;

import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;
import com.jcog.utils.TwitchUserLevel;
import functions.ViewerQueueManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.concurrent.ScheduledExecutorService;

import static java.lang.System.out;

public class ViewerQueueManageListener extends CommandBase {

    private static final String START = "!startqueue";
    private static final String END = "!endqueue";
    private static final String NEXT = "!next";
    private static final String MESSAGE_FILENAME = "src/main/resources/viewer_queue_message.txt";

    private final ViewerQueueManager viewerQueueManager;
    private final ViewerQueueJoinListener joinListener;

    public ViewerQueueManageListener(
            ScheduledExecutorService scheduler,
            ViewerQueueManager viewerQueueManager,
            ViewerQueueJoinListener joinListener
    ) {
        super(CommandType.EXACT_MATCH_COMMAND, scheduler);
        this.viewerQueueManager = viewerQueueManager;
        this.joinListener = joinListener;
    }

    @Override
    public String getCommandWords() {
        return String.join("|", START, END, NEXT);
    }

    @Override
    protected TwitchUserLevel.USER_LEVEL getMinUserPrivilege() {
        return TwitchUserLevel.USER_LEVEL.BROADCASTER;
    }

    @Override
    protected int getCooldownLength() {
        return 0;
    }

    @Override
    protected void performCommand(String command, TwitchUser sender, TwitchMessage message) {
        switch (command) {
            case START:
                File file = new File(MESSAGE_FILENAME);
                Scanner sc;
                try {
                    sc = new Scanner(file);
                }
                catch (FileNotFoundException e) {
                    out.println("Error reading file");
                    return;
                }

                String whisper;
                if (sc.hasNext()) {
                    whisper = sc.next();
                }
                else {
                    out.println("Error getting message");
                    return;
                }

                int count;
                if (sc.hasNextInt()) {
                    count = sc.nextInt();
                }
                else {
                    out.println("Error getting count");
                    return;
                }
                sc.close();
                viewerQueueManager.startNewSession(count, whisper);
                joinListener.start();
                break;

            case END:
                viewerQueueManager.closeCurrentSession();
                joinListener.stop();
                break;

            case NEXT:
                viewerQueueManager.getNext();
        }
    }
}
