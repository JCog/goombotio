package dev.jcog.goombotio.util;

public class StartupException extends RuntimeException {
    public StartupException(String message) {
        super("StartupException: " + message);
    }
}
