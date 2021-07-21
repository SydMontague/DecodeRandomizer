package net.digimonworld.decode.randomizer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.digimonworld.decode.randomizer.RandoLogger.LogLevel;
import net.digimonworld.decodetools.core.Access;
import net.digimonworld.decodetools.core.FileAccess;
import net.digimonworld.decodetools.data.digimon.PartnerDigimon;
import net.digimonworld.decodetools.data.keepdata.GlobalKeepData;
import net.digimonworld.decodetools.data.keepdata.LanguageKeep;
import net.digimonworld.decodetools.res.ResPayload;
import net.digimonworld.decodetools.res.kcap.AbstractKCAP;

public class RandomizationContext implements AutoCloseable {
    private final long initialSeed;
    
    private final Path modPath;
    private final Path workingPath;
    
    private final GlobalKeepData globalKeepData;
    private final LanguageKeep languageKeep;
    private final Map<String, ResPayload> fileMap = new HashMap<>();
    private final Map<Short, PartnerDigimon> partnerDigimon = new HashMap<>();
    
    private final RandoLogger logger;
    private final List<String> codeBinASM = new ArrayList<>();
    
    public RandomizationContext(long seed, boolean logLevel, Path workingPath, Path modPath) throws IOException {
        this.initialSeed = seed;
        this.modPath = modPath;
        this.workingPath = workingPath;
        
        try (Access access = new FileAccess(workingPath.resolve("part0/arcv/Keep/GlobalKeepData.res").toFile());
                Access access2 = new FileAccess(workingPath.resolve("part0/arcv/Keep/LanguageKeep_jp.res").toFile())) {
            this.globalKeepData = new GlobalKeepData((AbstractKCAP) ResPayload.craft(access));
            this.languageKeep = new LanguageKeep((AbstractKCAP) ResPayload.craft(access2));
        }
        
        Files.createDirectories(Path.of("./logs"));
        this.logger = new RandoLogger(logLevel, seed);
        
        codeBinASM.add(".open \"part0/exefs/code.bin\",0x100000");
        codeBinASM.add(".nds");
    }
    
    public Optional<PartnerDigimon> getDigimon(short id) {
        return getDigimon(id, false);
    }
    
    public Optional<PartnerDigimon> getDigimon(short id, boolean readOnly) {
        if(partnerDigimon.containsKey(id))
            return Optional.of(partnerDigimon.get(id));
        
        String path = String.format("part0/arcv/Digimon/Partner/digi%d.res", id);
        Path filePath = workingPath.resolve(path);

        PartnerDigimon payload = null;
        if (Files.isRegularFile(filePath)) {
            try (Access access = new FileAccess(filePath.toFile())) {
                payload = new PartnerDigimon((AbstractKCAP) ResPayload.craft(access));
                if(!readOnly)
                    partnerDigimon.put(id, payload);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
            
        return Optional.ofNullable(payload);
    }
    
    public Optional<ResPayload> getFile(String path) {
        if (fileMap.containsKey(path))
            return Optional.of(fileMap.get(path));
        
        Path filePath = workingPath.resolve(path);
        
        ResPayload payload = null;
        if (Files.isRegularFile(filePath)) {
            try (Access access = new FileAccess(filePath.toFile())) {
                payload = fileMap.computeIfAbsent(path, a -> ResPayload.craft(access));
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        return Optional.ofNullable(payload);
    }

    public void setFile(String path, ResPayload data) {
        if(data != null)
            fileMap.put(path, data);
    }
    
    
    public void build() throws IOException {
        globalKeepData.toKCAP().repack(modPath.resolve("part0/arcv/Keep/GlobalKeepData.res").toFile());
        
        fileMap.forEach((a, b) -> b.repack(modPath.resolve(a).toFile()));
        partnerDigimon.forEach((a, b) -> b.toKCAP().repack(modPath.resolve(String.format("part0/arcv/Digimon/Partner/digi%s.res", a)).toFile()));
        
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
    
    public boolean isRaceLogging() {
        return logger.isRaceLogging();
    }

}
