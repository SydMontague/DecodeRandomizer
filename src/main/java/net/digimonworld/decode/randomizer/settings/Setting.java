package net.digimonworld.decode.randomizer.settings;

import java.util.Map;

import de.phoenixstaffel.decodetools.keepdata.GlobalKeepData;
import de.phoenixstaffel.decodetools.keepdata.LanguageKeep;
import javafx.scene.control.TitledPane;
import net.digimonworld.decode.randomizer.RandomizationContext;

public interface Setting {

    public TitledPane create(GlobalKeepData inputData, LanguageKeep languageKeep);

    public void randomize(RandomizationContext context);
    
    public Map<String, Object> serialize();

    public void load(Map<String, Object> orDefault);
}
