package net.digimonworld.decode.randomizer.settings;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import org.controlsfx.control.CheckTreeView;

import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlSequence;

import de.phoenixstaffel.decodetools.keepdata.Digimon;
import de.phoenixstaffel.decodetools.keepdata.GlobalKeepData;
import de.phoenixstaffel.decodetools.keepdata.LanguageKeep;
import de.phoenixstaffel.decodetools.keepdata.enums.Level;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import net.digimonworld.decode.randomizer.RandoLogger.LogLevel;
import net.digimonworld.decode.randomizer.RandomizationContext;
import net.digimonworld.decode.randomizer.utils.JavaFXUtils;

public class StarterSettings implements Setting {
    
    private class DigimonEntry {
        int id;
        String name;
        
        public DigimonEntry(int id, String name) {
            this.id = id;
            this.name = name;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
    
    private BooleanProperty enabled = new SimpleBooleanProperty();
    private Map<Integer, BooleanProperty> propertyMap = new HashMap<>();
    
    @Override
    public TitledPane create(GlobalKeepData data, LanguageKeep language) {
        VBox vbox = new VBox(8);
        TitledPane pane = new TitledPane("Starter", vbox);
        vbox.setAlignment(Pos.TOP_LEFT);
        pane.setCollapsible(false);
        pane.setPrefHeight(400);
        
        CheckBoxTreeItem<DigimonEntry> root = new CheckBoxTreeItem<>(new DigimonEntry(-10, "Root"));
        Map<Level, List<DigimonEntry>> map = new EnumMap<>(Level.class);
        
        for (ListIterator<Digimon> itr = data.getDigimonData().listIterator(); itr.hasNext();) {
            int id = itr.nextIndex() + 1;
            Digimon digi = itr.next();
            
            if ((digi.getUnk38() & 1) == 1)
                map.computeIfAbsent(digi.getLevel(), a -> new ArrayList<DigimonEntry>())
                   .add(new DigimonEntry(id, language.getDigimonNames().getStringById(id)));
        }
        
        map.entrySet().forEach(a -> {
            CheckBoxTreeItem<DigimonEntry> item = new CheckBoxTreeItem<>(new DigimonEntry(-a.getKey().ordinal(), a.getKey().getDisplayName()));
            item.getChildren().addAll(a.getValue().stream().map(b -> {
                CheckBoxTreeItem<DigimonEntry> i = new CheckBoxTreeItem<>(b);
                BooleanProperty prop = new SimpleBooleanProperty();
                i.selectedProperty().bindBidirectional(prop);
                propertyMap.put(b.id, prop);
                return i;
            }).collect(Collectors.toList()));
            root.getChildren().add(item);
        });
        
        CheckTreeView<DigimonEntry> tree = new CheckTreeView<>(root);
        tree.setShowRoot(false);
        tree.disableProperty().bind(enabled.not());
        
        vbox.getChildren().addAll(JavaFXUtils.buildToggleSwitch("Enabled", Optional.empty(), Optional.of(enabled)), tree);
        return pane;
    }
    
    @Override
    public void randomize(RandomizationContext context) {
        List<Integer> list = propertyMap.entrySet().stream().filter(a -> a.getValue().get()).map(Map.Entry::getKey).collect(Collectors.toList());
        
        if (!enabled.get() || list.isEmpty())
            return;
        
        context.logLine(LogLevel.ALWAYS, "Randomizing starter Digimon...");
        Random rand = new Random(context.getInitialSeed() * "Starter".hashCode());
        
        int starterRina = list.get(rand.nextInt(list.size()));
        int starterTaiga = list.get(rand.nextInt(list.size()));
        
        context.addASM(".org 0x12CCC4");
        context.addASM(String.format(".byte 0x%02X", starterRina));
        
        context.addASM(".org 0x27AA18");
        context.addASM(String.format(".byte 0x%02X", starterTaiga));
        
        context.logLine(LogLevel.CASUAL,
                        String.format("Randomized Rina's Starter to %s.", context.getLanguageKeep().getDigimonNames().getStringById(starterRina + 1)));
        context.logLine(LogLevel.CASUAL,
                        String.format("Randomized Taiga's Starter to %s.", context.getLanguageKeep().getDigimonNames().getStringById(starterTaiga + 1)));
    }
    
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("enabled", enabled.get());
        map.put("checked", propertyMap.entrySet().stream().filter(a -> a.getValue().get()).map(Map.Entry::getKey).collect(Collectors.toList()));
        return map;
    }
    
    @Override
    public void load(YamlMapping map) {
        if (map == null)
            return;
        
        YamlSequence list = map.yamlSequence("checked");
        List<Integer> activeList = list == null ? new ArrayList<>()
                : list.values().stream().map(a -> Integer.parseInt(a.asScalar().value())).collect(Collectors.toList());
        propertyMap.forEach((a, b) -> b.set(activeList.contains(a)));
        enabled.set(Boolean.parseBoolean(map.string("enabled")));
    }
}
