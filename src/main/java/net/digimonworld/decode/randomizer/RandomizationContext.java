package net.digimonworld.decode.randomizer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import de.phoenixstaffel.decodetools.core.Access;
import de.phoenixstaffel.decodetools.core.FileAccess;
import de.phoenixstaffel.decodetools.keepdata.GlobalKeepData;
import de.phoenixstaffel.decodetools.keepdata.LanguageKeep;
import de.phoenixstaffel.decodetools.res.ResPayload;
import de.phoenixstaffel.decodetools.res.kcap.AbstractKCAP;
import net.digimonworld.decode.randomizer.RandoLogger.LogLevel;

public class RandomizationContext implements AutoCloseable {
    private final long initialSeed;
    private final GlobalKeepData globalKeepData;
    private final LanguageKeep languageKeep;
    private final Path modPath;
    private final RandoLogger logger;
    private final List<String> codeBinASM = new ArrayList<>();
    
    public RandomizationContext(long seed, boolean logLevel, Path workingPath, Path modPath) throws IOException {
        this.initialSeed = seed;
        this.modPath = modPath;
        
        try(Access access = new FileAccess(workingPath.resolve("part0/arcv/Keep/GlobalKeepData.res").toFile());
            Access access2 = new FileAccess(workingPath.resolve("part0/arcv/Keep/LanguageKeep_jp.res").toFile())) {
            this.globalKeepData = new GlobalKeepData((AbstractKCAP) ResPayload.craft(access));
            this.languageKeep = new LanguageKeep((AbstractKCAP) ResPayload.craft(access2));
        }
        
        Files.createDirectories(Path.of("./logs"));
        this.logger = new RandoLogger(logLevel, seed);
        
        codeBinASM.add(".open \"part0/exefs/code.bin\",0x100000");
        codeBinASM.add(".nds");
    }

    public void build() throws IOException {
        globalKeepData.toKCAP().repack(modPath.resolve("part0/arcv/Keep/GlobalKeepData.res").toFile());
        
        codeBinASM.add(".close");
        Files.write(modPath.resolve("code.bin.asm"), codeBinASM);
    }
    
    public long getInitialSeed() {
        return initialSeed;
    }
    
    public GlobalKeepData getGlobalKeepData() {
        return globalKeepData;
    }
    
    public LanguageKeep getLanguageKeep() {
        return languageKeep;
    }
    
    public void addASM(String string) {
        codeBinASM.add(string);
    }
    
    public void logLine(LogLevel level, String string) {
        logger.logLine(level, string);
    }
    
    @Override
    public void close() throws Exception {
        logger.close();
    }
}
