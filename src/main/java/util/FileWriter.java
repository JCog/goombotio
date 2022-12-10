package util;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

public class FileWriter {
    // use only for one-time-use file writes
    public static boolean writeToFile(String location, String filename, String content) {
        try {
            PrintWriter writer = new PrintWriter(location + filename, StandardCharsets.UTF_8);
            writer.print(content);
            writer.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
