package net.digimonworld.decode.randomizer;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RandoLogger implements AutoCloseable {
    private final PrintStream logger;
    private final boolean raceLogging;
    
    public RandoLogger(boolean raceLogging, long seed) throws IOException {
        Path path = Paths.get(String.format("./logs/random_%d.log", seed));
        
        Files.createDirectories(path.getParent());

        this.logger = new PrintStream(path.toFile());
        this.raceLogging = raceLogging;
    }
    
    public void logLine(LogLevel logLevel, String string) {
        if(isLogged(logLevel))
            logger.println(string);
    }
    
    private boolean isLogged(LogLevel level) {
        if(level == null)
            return true;
        
        switch(level) {
            case ALWAYS:
                return true;
            case CASUAL:
                return !raceLogging;
            case NOTHING:
                return false;
        }
        
        return true;
    }
    
    public enum LogLevel {
        ALWAYS("Full", "Logs every time"),
        CASUAL("Casual", "Logs only in casual mode"),
        NOTHING("Nothing", "Logs never");
        
        private final String displayName;
        private final String tooltip;
        
        private LogLevel(String displayName, String tooltip) {
            this.displayName = displayName;
            this.tooltip = tooltip;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public boolean isLogging(LogLevel level) {
            
            return this.ordinal() <= level.ordinal();
        }
        
        public String getTooltip() {
            return tooltip;
        }
        
        @Override
        public String toString() {
            return getDisplayName();
        }
    }

    @Override
    public void close() throws Exception {
        logger.close();
    }
    
    public boolean isRaceLogging() {
        return raceLogging;
    }
}
