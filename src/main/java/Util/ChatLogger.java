package Util;

import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
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
        if (isNewDayUTC()) {
            currentDate = getCurrentDateUTC();
            openNewFile();
        }
        if (writer != null) {
            writer.println();
            writer.write(String.format(
                    "%s %d %s: %s",
                    getDateString(),
                    user.getUserID(),
                    user.getDisplayName(),
                    message.getContent()
            ));
        }
    }
    
    private void openNewFile() {
        if (writer != null) {
            writer.close();
        }
        String filename = LOCATION + getFileName() + ".log";
        try {
            writer = new PrintWriter(filename, "UTF-8");
        } catch (FileNotFoundException e) {
            writer = null;
            System.out.println(String.format("ERROR: file \"%s\" not found", filename));
        } catch (UnsupportedEncodingException e) {
            //no idea how this would happen
            e.printStackTrace();
        }
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
