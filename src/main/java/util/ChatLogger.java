package util;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.chat.events.channel.ModAnnouncementEvent;
import com.github.twitch4j.helix.domain.User;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class ChatLogger {
    private static final String UTC_TIMEZONE = "UTC";
    private static final String LOCATION = "chat_logs/";
    private static final String TIME_FORMAT = "yyyy-MM-dd HH:mm:ss z";
    private static final String FILENAME_FORMAT = "yyyy-MM-dd";
    private static final String ANNOUNCEMENT_COMMAND = "/announce ";
    
    public static void logAnnouncement(ModAnnouncementEvent announcementEvent) {
        logMessage(
                announcementEvent.getMessageEvent().getUserId(),
                announcementEvent.getMessageEvent().getUser().getName(),
                ANNOUNCEMENT_COMMAND + announcementEvent.getMessage()
        );
    }
    
    public static void logAnnouncement(User user, String message) {
        logMessage(
                user.getId(),
                user.getDisplayName(),
                ANNOUNCEMENT_COMMAND + message
        );
    }
    
    public static void logMessage(ChannelMessageEvent messageEvent) {
        logMessage(
                messageEvent.getUser().getId(),
                messageEvent.getUser().getName(),
                messageEvent.getMessage()
        );
    }
    
    public static void logMessage(User user, String message) {
        logMessage(
                user.getId(),
                user.getDisplayName(),
                message
        );
    }

    public static void logMessage(String userId, String displayName, String message) {
        write(String.format(
                "%s %s %s: %s\n",
                getDateString(),
                userId,
                displayName,
                message
        ));
    }

    private static void write(String input) {
        SimpleDateFormat sdf = new SimpleDateFormat(FILENAME_FORMAT);
        sdf.setTimeZone(TimeZone.getTimeZone(UTC_TIMEZONE));
        String filename = LOCATION + sdf.format(new Date()) + ".log";
        
        FileWriter fw;
        try {
            fw = new FileWriter(filename, true);
        } catch (IOException e) {
            System.out.printf("ERROR: IOException for filename \"%s\"%n", filename);
            e.printStackTrace();
            return;
        }
        PrintWriter writer = new PrintWriter(new BufferedWriter(fw));
        writer.write(input);
        writer.close();
    }

    private static String getDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat(TIME_FORMAT);
        sdf.setTimeZone(TimeZone.getTimeZone(UTC_TIMEZONE));
        return sdf.format(new Date());
    }
}
