package net.digimonworld.decode.randomizer.settings;

import java.util.Map;

import com.amihaiemil.eoyaml.YamlMapping;

import net.digimonworld.decodetools.data.keepdata.GlobalKeepData;
import net.digimonworld.decodetools.data.keepdata.LanguageKeep;
import javafx.scene.control.TitledPane;
import net.digimonworld.decode.randomizer.RandomizationContext;

public interface Setting {

    static final String FORMAT_FLOAT = "%28s | %12s | %#.4f -> %#.4f";
    static final String FORMAT_INT = "%28s | %12s | %4d -> %4d";
    static final String FORMAT_STRING = "%28s | %12s | %8s -> %8s";

    public TitledPane create(GlobalKeepData inputData, LanguageKeep languageKeep);

    public void randomize(RandomizationContext context);
    
    public Map<String, Object> serialize();

    public void load(YamlMapping map);
}
