package util;

import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class ChatLogger {
    private static final String UTC_TIMEZONE = "UTC";
    private static final String LOCATION = "chat_logs/";
    private static final String TIME_FORMAT = "yyyy-MM-dd HH:mm:ss z";
    private static final String FILENAME_FORMAT = "yyyy-MM-dd";

    private PrintWriter writer = null;
    private Calendar currentDate;

    public ChatLogger() {
        openNewFile();
        currentDate = getCurrentDateUTC();
    }

    public void close() {
        if (writer != null) {
            writer.close();
        }
    }

    public void logMessage(TwitchUser user, TwitchMessage message) {
        logMessage(user.getUserID(), user.getDisplayName(), message.getContent());
    }

    public void logMessage(long userId, String displayName, String message) {
        if (isNewDayUTC()) {
            currentDate = getCurrentDateUTC();
            openNewFile();
        }
        if (writer != null) {
            writer.write(String.format(
                    "%s %d %s: %s\n",
                    getDateString(),
                    userId,
                    displayName,
                    message
            ));
            writer.flush();
        }
    }

    private void openNewFile() {
        if (writer != null) {
            writer.close();
        }
        String filename = LOCATION + getFileName() + ".log";
        FileWriter fw;
        try {
            fw = new FileWriter(filename, true);
        }
        catch (IOException e) {
            System.out.println(String.format("ERROR: IOException for filename \"%s\"", filename));
            e.printStackTrace();
            return;
        }
        BufferedWriter bw = new BufferedWriter(fw);
        writer = new PrintWriter(bw);
    }

    private String getDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat(TIME_FORMAT);
        sdf.setTimeZone(TimeZone.getTimeZone(UTC_TIMEZONE));
        return sdf.format(new Date());
    }

    private String getFileName() {
        SimpleDateFormat sdf = new SimpleDateFormat(FILENAME_FORMAT);
        sdf.setTimeZone(TimeZone.getTimeZone(UTC_TIMEZONE));
        return sdf.format(new Date());
    }

    private boolean isNewDayUTC() {
        Calendar newTime = getCurrentDateUTC();
        return newTime.get(Calendar.DAY_OF_MONTH) != currentDate.get(Calendar.DAY_OF_MONTH);
    }

    private Calendar getCurrentDateUTC() {
        return Calendar.getInstance(TimeZone.getTimeZone(UTC_TIMEZONE));
    }
}
