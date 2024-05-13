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
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.controlsfx.control.CheckTreeView;

import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlSequence;

import net.digimonworld.decodetools.data.keepdata.Digimon;
import net.digimonworld.decodetools.data.keepdata.GlobalKeepData;
import net.digimonworld.decodetools.data.keepdata.LanguageKeep;
import net.digimonworld.decodetools.data.keepdata.enums.Level;
import net.digimonworld.decodetools.res.payload.BTXPayload;
import net.digimonworld.decodetools.core.DeleteDirectoryFileVisitor;
import net.digimonworld.decodetools.res.payload.BTXPayload.BTXEntry;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.Button;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import net.digimonworld.decode.randomizer.RandoLogger.LogLevel;
import net.digimonworld.decode.randomizer.RandomizationContext;
import net.digimonworld.decode.randomizer.utils.JavaFXUtils;
import net.digimonworld.decode.randomizer.MainWindowController;

public class NamingSettings implements Setting {

    private List<String> skippable = List.of("", "None", "Unused Item", "???", "NO DATA");
    private BooleanProperty enabled = new SimpleBooleanProperty();
    private Map<Integer, BooleanProperty> propertyMap = new HashMap<>();

    public void onSaveCsv() {
    };

    @Override
    public TitledPane create(GlobalKeepData data, LanguageKeep language) {
        VBox vbox = new VBox(8);
        TitledPane pane = new TitledPane("Namings", vbox);
        vbox.setAlignment(Pos.TOP_LEFT);
        pane.setCollapsible(false);
        EventHandler<ActionEvent> handler = new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {

                ArrayList<String> methodList = new ArrayList<String>();

                Method[] methods = language.getClass().getMethods();
                for (int i = 0; i < methods.length; i++) {
                    String methodName = methods[i].getName();
                    if (methodName.contains("Names"))
                        methodList.add(methodName);
                }
                System.out.println(methodList);

                try {
                    File csvDir = new File("./renamingPresets/");
                    Files.walkFileTree(csvDir.toPath(), new DeleteDirectoryFileVisitor());
                    csvDir.mkdir();

                } catch (IOException exc) {
                    exc.printStackTrace();
                    return;
                }

                methodList.forEach(s -> {
                    ArrayList<String> myList = new ArrayList<String>();
                    try {

                        BTXPayload fiba = (BTXPayload) language.getClass().getMethod(s).invoke(language);
                        fiba.getEntries().stream()
                                .forEach(e -> myList.add(e.getValue().getString()));
                    } catch (Exception e) {

                    }

                    File destFile = new File("./renamingPresets/" + s.substring(3) + ".csv");
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(destFile, StandardCharsets.UTF_8))) {

                        writer.write("original;replace\n");
                        String string = myList.stream().filter(str -> !skippable.contains(str))
                                .map(str -> str + ";" + str).collect(Collectors.joining(";\n"))
                                + ";";

                        writer.write(string);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

                event.consume();

            }
        };
        Button ponk = new Button("Export name CSVs");
        ponk.setOnAction(handler);
        vbox.getChildren().add(ponk);

        return pane;
    }

    @Override
    public void randomize(RandomizationContext context) {
        List<Integer> list = propertyMap.entrySet().stream().filter(a -> a.getValue().get()).map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (!enabled.get() || list.isEmpty())
            return;

        context.logLine(LogLevel.ALWAYS, "Randomizing starter Digimon...");
        Random rand = new Random(context.getInitialSeed() * "Starter".hashCode());

        int starterRina = list.get(rand.nextInt(list.size()));
        int starterTaiga = list.get(rand.nextInt(list.size()));
        int starterTaigaNGP = list.get(rand.nextInt(list.size()));

        context.addASM(".org 0x12CCC4");
        context.addASM(String.format(".byte 0x%02X", starterRina));

        context.addASM(".org 0x12D6A0");
        context.addASM(String.format(".byte 0x%02X", starterTaigaNGP));

        context.addASM(".org 0x27AA18");
        context.addASM(String.format(".byte 0x%02X", starterTaiga));

        context.logLine(LogLevel.CASUAL,
                String.format("Randomized Rina's Starter to %s.",
                        context.getLanguageKeep().getDigimonNames().getStringById(starterRina + 1)));
        context.logLine(LogLevel.CASUAL,
                String.format("Randomized Taiga's Starter to %s.",
                        context.getLanguageKeep().getDigimonNames().getStringById(starterTaiga + 1)));
        context.logLine(LogLevel.CASUAL,
                String.format("Randomized Taiga's BG+ Starter to %s.",
                        context.getLanguageKeep().getDigimonNames().getStringById(starterTaigaNGP + 1)));

    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("enabled", enabled.get());
        map.put("checked", propertyMap.entrySet().stream().filter(a -> a.getValue().get()).map(Map.Entry::getKey)
                .collect(Collectors.toList()));
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
