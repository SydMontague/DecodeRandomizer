package net.digimonworld.decode.randomizer.settings;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;

import net.digimonworld.decodetools.core.Tuple;
import net.digimonworld.decodetools.data.keepdata.GlobalKeepData;
import net.digimonworld.decodetools.data.keepdata.LanguageKeep;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Tab;
import javafx.scene.layout.FlowPane;
import net.digimonworld.decode.randomizer.RandoLogger.LogLevel;
import net.digimonworld.decode.randomizer.RandomizationContext;

public class RandomizerSettings {
    private SkillsSettings skillSettings = new SkillsSettings();
    private DigimonSettings digimonSettings = new DigimonSettings();
    private EvolutionSettings evolutionSettings = new EvolutionSettings();
    private StarterSettings starterSettings = new StarterSettings();
    private WorldSettings worldSettings = new WorldSettings();
    private PatchSettings patchSettings = new PatchSettings();
    private PlayerSettings playerSettings = new PlayerSettings();
    
    public void randomize(RandomizationContext context) {
        logSettings(context);

        patchSettings.randomize(context);
        skillSettings.randomize(context);
        digimonSettings.randomize(context);
        evolutionSettings.randomize(context);
        starterSettings.randomize(context);
        worldSettings.randomize(context);
        playerSettings.randomize(context);
    }
    
    private void logSettings(RandomizationContext context) {
        context.logLine(LogLevel.ALWAYS, "Randomizer Settings: ");
        
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("seed", context.getInitialSeed());
        configMap.put("settings", serialize());
        configMap.put("raceLogging", context.isRaceLogging());
        
        try (StringWriter writer = new StringWriter()) {
            Yaml.createYamlPrinter(writer).print(Yaml.createYamlDump(configMap).dump());
            context.logLine(LogLevel.ALWAYS, writer.toString());
        }
        catch (IOException e) {
            // should never happen
            e.printStackTrace();
        }
        
        context.logLine(LogLevel.ALWAYS, "");        
    }

    public List<Tab> create(GlobalKeepData inputData, LanguageKeep languageKeep) {
        return getSettingsMap().stream().map(a -> {
            FlowPane generalPane = new FlowPane();
            generalPane.setVgap(10);
            generalPane.setHgap(10);
            generalPane.setPadding(new Insets(10));
            generalPane.setOrientation(Orientation.VERTICAL);
            generalPane.setPrefWrapLength(400);
            
            for (Setting setting : a.getValue())
                generalPane.getChildren().add(setting.create(inputData, languageKeep));
            
            return new Tab(a.getKey(), generalPane);
        }).collect(Collectors.toList());
    }
    
    private List<Tuple<String, List<Setting>>> getSettingsMap() {
        return List.of(Tuple.of("General", Arrays.asList(digimonSettings, evolutionSettings, skillSettings, worldSettings, patchSettings)),
                       Tuple.of("New Game", Arrays.asList(starterSettings, playerSettings)));
    }
    
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("skillSettings", skillSettings.serialize());
        map.put("digimonSettings", digimonSettings.serialize());
        map.put("evolutionSettings", evolutionSettings.serialize());
        map.put("starterSettings", starterSettings.serialize());
        map.put("worldSettings", worldSettings.serialize());
        map.put("patchSettings", patchSettings.serialize());
        map.put("playerSettings", playerSettings.serialize());
        
        return map;
    }
    
    public void load(YamlMapping map) {
        if (map == null)
            return;
        
        skillSettings.load(map.yamlMapping("skillSettings"));
        digimonSettings.load(map.yamlMapping("digimonSettings"));
        evolutionSettings.load(map.yamlMapping("evolutionSettings"));
        starterSettings.load(map.yamlMapping("starterSettings"));
        worldSettings.load(map.yamlMapping("worldSettings"));
        patchSettings.load(map.yamlMapping("patchSettings"));
        playerSettings.load(map.yamlMapping("playerSettings"));
    }
}
