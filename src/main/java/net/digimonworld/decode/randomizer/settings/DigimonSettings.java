package net.digimonworld.decode.randomizer.settings;

import java.util.Map;

import com.amihaiemil.eoyaml.YamlMapping;

import de.phoenixstaffel.decodetools.keepdata.GlobalKeepData;
import de.phoenixstaffel.decodetools.keepdata.LanguageKeep;
import javafx.geometry.Pos;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import net.digimonworld.decode.randomizer.RandomizationContext;

public class DigimonSettings implements Setting {
    /*
     * Level?
     * Attribute?
     * Scale? (Joke Option)
     * Combat Speed
     * Special
     * Moves?
     * Finisher??
     * Sleep Schedule
     * Favorite Food?
     * Energy Cap?
     * Training Type
     * Area Preference?
     * Base Weight
     * Energy Usage Mod?
     * Stats Gains
     * Evolution Paths
     * Evolution Requirements
     */
    
    @Override
    public TitledPane create(GlobalKeepData data, LanguageKeep language) {
        VBox vbox = new VBox(4);
        TitledPane pane = new TitledPane("Digimon", vbox);
        vbox.setAlignment(Pos.TOP_RIGHT);
        pane.setCollapsible(false);
        
        return pane;
    }
    
    @Override
    public void randomize(RandomizationContext context) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public Map<String, Object> serialize() {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public void load(YamlMapping map) {
        if(map == null)
            return;
        
        // TODO Auto-generated method stub
        
    }
    
}
