package Util;

import java.io.PrintWriter;

public class FileWriter {
    // use only for one-time-use file writes
    public static boolean writeToFile(String location, String filename, String content) {
        try {
            PrintWriter writer = new PrintWriter(location + filename, "UTF-8");
            writer.print(content);
            writer.close();
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
