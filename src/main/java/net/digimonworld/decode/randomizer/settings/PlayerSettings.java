package net.digimonworld.decode.randomizer.settings;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import org.controlsfx.control.CheckListView;

import com.amihaiemil.eoyaml.YamlMapping;

import de.phoenixstaffel.decodetools.keepdata.GlobalKeepData;
import de.phoenixstaffel.decodetools.keepdata.LanguageKeep;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.TitledPane;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.VBox;
import net.digimonworld.decode.randomizer.RandomizationContext;
import net.digimonworld.decode.randomizer.RandoLogger.LogLevel;
import net.digimonworld.decode.randomizer.utils.JavaFXUtils;

public class PlayerSettings implements Setting {
    // TODO pronouns, requires translation patch 1.1
    
    private BooleanProperty enabled = new SimpleBooleanProperty();
    private Map<Character, BooleanProperty> checkedMap = new EnumMap<>(Character.class);
    
    @Override
    public TitledPane create(GlobalKeepData inputData, LanguageKeep languageKeep) {
        VBox vbox = new VBox(8);
        TitledPane pane = new TitledPane("Starter", vbox);
        vbox.setAlignment(Pos.TOP_LEFT);
        pane.setCollapsible(false);
        
        Arrays.stream(Character.values()).forEach(a -> checkedMap.put(a, new SimpleBooleanProperty()));
        CheckListView<Character> list = new CheckListView<>(FXCollections.observableArrayList(Character.values()));
        list.disableProperty().bind(enabled.not());
        list.setCellFactory(listView -> new CheckBoxListCell<>(checkedMap::get) {
            @Override
            public void updateItem(Character a, boolean b) {
                super.updateItem(a, b);
                setText(a == null ? "" : languageKeep.getCharacterNames().getStringById(a.getStringId()));
            }
        });
        
        vbox.getChildren().addAll(JavaFXUtils.buildToggleSwitch("Enabled", Optional.empty(), Optional.of(enabled)), list);
        return pane;
    }
    
    @Override
    public void randomize(RandomizationContext context) {
        if (enabled.get())
            randomizePlayer(context);
    }
    
    private void randomizePlayer(RandomizationContext context) {
        List<Character> list = checkedMap.entrySet().stream().filter(a -> a.getValue().get()).map(Map.Entry::getKey).collect(Collectors.toList());
        
        if (list.isEmpty())
            return;
        
        context.logLine(LogLevel.ALWAYS, "Randomizing player character...");
        Random rand = new Random(context.getInitialSeed() * "Player".hashCode());
        
        Character starterRina = list.get(rand.nextInt(list.size()));
        Character starterTaiga = list.get(rand.nextInt(list.size()));
        
        context.addASM(".org 0x4A1CDE");
        context.addASM(String.format(".ascii \"%s\"", starterTaiga.getFileName()));
        context.addASM(".org 0x4A1D4E");
        context.addASM(String.format(".ascii \"%s\"", starterRina.getFileName()));
        
        context.logLine(LogLevel.CASUAL,
                        String.format("Randomized Rina's Character to %s.",
                                      context.getLanguageKeep().getCharacterNames().getStringById(starterRina.getStringId())));
        context.logLine(LogLevel.CASUAL,
                        String.format("Randomized Taiga's Character to %s.",
                                      context.getLanguageKeep().getCharacterNames().getStringById(starterTaiga.getStringId())));
    }
    
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        
        map.put("enabled", enabled.get());
        map.put("checked", checkedMap.entrySet().stream().collect(Collectors.toMap(a -> a.getKey().toString(), a -> a.getValue().get())));
        
        return map;
    }
    
    @Override
    public void load(YamlMapping map) {
        if (map == null)
            return;
        
        YamlMapping list = map.yamlMapping("checked");
        Arrays.asList(Character.values())
              .forEach(a -> checkedMap.computeIfAbsent(a, b -> new SimpleBooleanProperty()).setValue(Boolean.parseBoolean(list.string(a.toString()))));
        enabled.set(Boolean.parseBoolean(map.string("enabled")));
    }
    
    public enum Character {
        TAIGA("pc01.res", 1062, Pronoun.HE),
        RINA("pc02.res", 1063, Pronoun.SHE),
        AKIHO("Npc01.res", 1002, Pronoun.SHE),
        NIKO("Npc02.res", 1003, Pronoun.HE),
        YUUYA("Npc03.res", 1004, Pronoun.HE),
        MIREI("Npc04.res", 1005, Pronoun.SHE),
        PETROV("Npc05.res", 1006, Pronoun.HE),
        LILY("Npc10.res", 1007, Pronoun.SHE);
        
        private final String fileName;
        private final int stringId;
        private final Pronoun pronoun;
        
        private Character(String fileName, int stringId, Pronoun pronoun) {
            this.fileName = fileName;
            this.stringId = stringId;
            this.pronoun = pronoun;
        }
        
        public String getFileName() {
            return fileName;
        }
        
        public int getStringId() {
            return stringId;
        }
        
        public Pronoun getPronoun() {
            return pronoun;
        }
    }
    
    public enum Pronoun {
        HE,
        SHE,
        THEY;
    }
}
