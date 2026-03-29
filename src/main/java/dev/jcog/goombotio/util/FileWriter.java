package dev.jcog.goombotio.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

public class FileWriter {
    private static final Logger log = LoggerFactory.getLogger(FileWriter.class);

    // use only for one-time-use file writes
    public static boolean writeToFile(String location, String filename, String content) {
        try {
            PrintWriter writer = new PrintWriter(location + filename, StandardCharsets.UTF_8);
            writer.print(content);
            writer.close();
            return true;
        } catch (IOException e) {
            log.error("IOException writing to file at {}{}", location, filename);
            return false;
        }
    }
}
