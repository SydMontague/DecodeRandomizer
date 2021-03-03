package net.digimonworld.decode.randomizer.settings;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import com.amihaiemil.eoyaml.YamlMapping;

import de.phoenixstaffel.decodetools.keepdata.GlobalKeepData;
import de.phoenixstaffel.decodetools.keepdata.LanguageKeep;
import de.phoenixstaffel.decodetools.keepdata.TypeAlignmentChart;
import de.phoenixstaffel.decodetools.keepdata.enums.Special;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import net.digimonworld.decode.randomizer.RandoLogger.LogLevel;
import net.digimonworld.decode.randomizer.RandomizationContext;
import net.digimonworld.decode.randomizer.utils.JavaFXUtils;

public class WorldSettings implements Setting {
    
    private BooleanProperty randomizeTypeEffectiveness = new SimpleBooleanProperty();
    
    @Override
    public TitledPane create(GlobalKeepData inputData, LanguageKeep languageKeep) {
        VBox vbox = new VBox(4);
        TitledPane pane = new TitledPane("World", vbox);
        vbox.setAlignment(Pos.TOP_RIGHT);
        pane.setCollapsible(false);
        
        vbox.getChildren().addAll(JavaFXUtils.buildToggleSwitch("Type Alignment", Optional.empty(), Optional.of(randomizeTypeEffectiveness)));
        return pane;
    }
    
    @Override
    public void randomize(RandomizationContext context) {
        if(randomizeTypeEffectiveness.get())
            randomizeTypeEffectiveness(context);
    }
    
    private void randomizeTypeEffectiveness(RandomizationContext context) {
        Random rand = new Random(context.getInitialSeed() * "TypeAlignment".hashCode());
        
        TypeAlignmentChart chart = context.getGlobalKeepData().getTypeAlignmentChart();
        
        StringBuilder sbOld = new StringBuilder();
        StringBuilder sbNew = new StringBuilder();
        
        for(Special attacker : TypeAlignmentChart.VALUES) {
            for(Special victim : TypeAlignmentChart.VALUES) {
                int val = rand.nextInt(101);
                int value = 50 + (Math.min(val, 50) + Math.max(2 * (val - 50), 0));
                sbOld.append(Byte.toUnsignedInt(chart.get(attacker, victim))).append(" ");
                sbNew.append(value).append(" ");
                chart.set(attacker, victim, value);
            }
            sbOld.append("\n");
            sbNew.append("\n");
        }
        
        context.logLine(LogLevel.ALWAYS, "Randomizing type alignments...");
        context.logLine(LogLevel.CASUAL, "Old Alignments: ");
        context.logLine(LogLevel.CASUAL, sbOld.toString());
        context.logLine(LogLevel.CASUAL, "New Alignments: ");
        context.logLine(LogLevel.CASUAL, sbNew.toString());
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        
        map.put("randomizeTypeEffectiveness", randomizeTypeEffectiveness.get());

        return map;
    }
    
    @Override
    public void load(YamlMapping map) {
        if(map == null)
            return;
        
        this.randomizeTypeEffectiveness.set(Boolean.parseBoolean(map.string("randomizeTypeEffectiveness")));
        
    }
    
}
