package fr.insa.crypto.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Système de journalisation pour l'application
 */
public class Logger {
    // Niveaux de log
    public enum Level {
        DEBUG(0), INFO(1), WARNING(2), ERROR(3);

        private final int value;

        Level(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    // Codes de couleurs ANSI
    private static final String RESET = "\u001B[0m";
    private static final String DEBUG_COLOR = "\u001B[36m"; // Cyan
    private static final String INFO_COLOR = "\u001B[32m"; // Vert
    private static final String WARNING_COLOR = "\u001B[33m"; // Jaune
    private static final String ERROR_COLOR = "\u001B[31m"; // Rouge

    private static Level currentLevel = Level.INFO; // Niveau par défaut
    private static boolean logToFile = false;
    private static boolean useColors = true; // Couleurs activées par défaut
    private static String logFilePath = "application.log";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Méthodes de configuration
    public static void setLevel(Level level) {
        currentLevel = level;
    }

    public static void enableFileLogging(boolean enable) {
        logToFile = enable;
    }

    public static void setLogFilePath(String path) {
        logFilePath = path;
    }

    public static void enableColors(boolean enable) {
        useColors = enable;
    }

    // Méthodes de journalisation
    public static void debug(String message) {
        log(Level.DEBUG, message);
    }

    public static void info(String message) {
        log(Level.INFO, message);
    }

    public static void warning(String message) {
        log(Level.WARNING, message);
    }

    public static void error(String message) {
        log(Level.ERROR, message);
    }

    public static void error(String message, Throwable throwable) {
        log(Level.ERROR, message + ": " + throwable.getMessage());
    }

    private static void log(Level level, String message) {
        if (level.getValue() >= currentLevel.getValue()) {
            String formattedMessage = formatLogMessage(level, message);

            // Afficher dans la console
            System.out.println(formattedMessage);

            // Écrire dans un fichier si activé
            if (logToFile) {
                writeToFile(formattedMessage);
            }
        }
    }

    private static String formatLogMessage(Level level, String message) {
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(formatter);

        if (useColors) {
            String colorCode;
            switch (level) {
                case DEBUG:
                    colorCode = DEBUG_COLOR;
                    break;
                case INFO:
                    colorCode = INFO_COLOR;
                    break;
                case WARNING:
                    colorCode = WARNING_COLOR;
                    break;
                case ERROR:
                    colorCode = ERROR_COLOR;
                    break;
                default:
                    colorCode = RESET;
            }
            return String.format("%s[%s] [%s] %s%s", colorCode, timestamp, level, message, RESET);
        } else {
            return String.format("[%s] [%s] %s", timestamp, level, message);
        }
    }

    private static void writeToFile(String message) {
        try (PrintWriter out = new PrintWriter(new FileWriter(logFilePath, true))) {
            out.println(message);
        } catch (IOException e) {
            System.err.println("Erreur lors de l'écriture dans le fichier de log: " + e.getMessage());
        }
    }
}
