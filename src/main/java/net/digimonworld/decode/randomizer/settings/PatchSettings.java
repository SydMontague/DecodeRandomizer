package net.digimonworld.decode.randomizer.settings;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.amihaiemil.eoyaml.YamlMapping;

import net.digimonworld.decodetools.keepdata.GlobalKeepData;
import net.digimonworld.decodetools.keepdata.LanguageKeep;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import net.digimonworld.decode.randomizer.RandomizationContext;
import net.digimonworld.decode.randomizer.RandoLogger.LogLevel;
import net.digimonworld.decode.randomizer.utils.JavaFXUtils;

public class PatchSettings implements Setting {
    
    // TODO evo priority
    
    private BooleanProperty patchViewDistance = new SimpleBooleanProperty();
    private BooleanProperty patchBrainsChance = new SimpleBooleanProperty();
    private DoubleProperty patchBrainsChanceFactor = new SimpleDoubleProperty();
    
    @Override
    public TitledPane create(GlobalKeepData inputData, LanguageKeep languageKeep) {
        VBox vbox = new VBox(4);
        TitledPane pane = new TitledPane("Patches", vbox);
        vbox.setAlignment(Pos.TOP_RIGHT);
        pane.setCollapsible(false);
        
        Slider brainChanceSlider = new Slider(1D, 10D, 1D);
        brainChanceSlider.setShowTickLabels(true);
        brainChanceSlider.setShowTickMarks(true);
        brainChanceSlider.setMajorTickUnit(1D);
        brainChanceSlider.setMinorTickCount(3);
        brainChanceSlider.setSnapToTicks(true);
        brainChanceSlider.setBlockIncrement(0.25D);
        brainChanceSlider.disableProperty().bind(patchBrainsChance.not());
        
        Label lbl = new Label("100%");
        lbl.textProperty().bind(brainChanceSlider.valueProperty().multiply(100D).asString("%4.0f%%"));
        lbl.setMinWidth(45);
        lbl.setAlignment(Pos.CENTER_RIGHT);
        
        patchBrainsChanceFactor.bindBidirectional(brainChanceSlider.valueProperty());
        
        vbox.getChildren()
            .addAll(JavaFXUtils.buildToggleSwitch("View Distance",
                                                  Optional.of("Increases view distance, allowing to see Digimon from a further distance."),
                                                  Optional.of(patchViewDistance)),
                    JavaFXUtils.buildToggleSwitch("Brain Learn Chance", Optional.empty(), Optional.of(patchBrainsChance)),
                    new HBox(brainChanceSlider, lbl));
        return pane;
    }
    
    @Override
    public void randomize(RandomizationContext context) {
        if (patchViewDistance.get())
            patchViewDistance(context);
        if (patchBrainsChance.get())
            patchBrainsChance(context);
        
    }
    
    private void patchBrainsChance(RandomizationContext context) {
        context.logLine(LogLevel.ALWAYS, "Patching Brains learning chance...");
        
        context.addASM(".org 0x14B820");
        context.addASM(String.format(Locale.US, ".float %5.3f", 0.002 * Math.max(1D, patchBrainsChanceFactor.get())));
    }
    
    private void patchViewDistance(RandomizationContext context) {
        context.logLine(LogLevel.ALWAYS, "Patching view distance...");
        
        context.addASM(".org 0x4E5118");
        context.addASM(".float 1000.0");
        context.addASM(".float 1000.0");
        context.addASM(".org 0x44CC48");
        context.addASM(".float 1000.0");
        context.addASM(".float 1000.0");
        context.addASM(".org 0x1F9F74");
        context.addASM(".float 1000.0");
        context.addASM(".float 1000.0");
        context.addASM(".org 0x2C56B8");
        context.addASM("NOP");
        context.addASM(".org 0x2C56C4");
        context.addASM("NOP");
    }
    
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        
        map.put("patchViewDistance", patchViewDistance.get());
        map.put("patchBrainsChance", patchBrainsChance.get());
        map.put("patchBrainsChanceFactor", patchBrainsChanceFactor.get());
        
        return map;
    }
    
    @Override
    public void load(YamlMapping map) {
        if (map == null)
            return;
        
        this.patchViewDistance.set(Boolean.parseBoolean(map.string("patchViewDistance")));
        this.patchBrainsChance.set(Boolean.parseBoolean(map.string("patchBrainsChance")));
        this.patchBrainsChanceFactor.set(map.doubleNumber("patchBrainsChanceFactor"));
    }
    
}
