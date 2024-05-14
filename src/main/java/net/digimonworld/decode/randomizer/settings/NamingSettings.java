package net.digimonworld.decode.randomizer.settings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.io.BufferedWriter;
import java.nio.file.StandardCopyOption;
import java.nio.file.Files;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.controlsfx.control.ToggleSwitch;

import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlSequence;

import net.digimonworld.decodetools.data.keepdata.GlobalKeepData;
import net.digimonworld.decodetools.data.keepdata.LanguageKeep;
import net.digimonworld.decodetools.res.kcap.NormalKCAP;
import net.digimonworld.decodetools.res.payload.BTXPayload;

import net.digimonworld.decodetools.core.DeleteDirectoryFileVisitor;
import net.digimonworld.decodetools.core.Tuple;
import net.digimonworld.decodetools.core.Utils;
import net.digimonworld.decodetools.res.payload.BTXPayload.BTXEntry;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.beans.binding.When;
import javafx.scene.control.Button;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Accordion;
import javafx.scene.layout.VBox;
import net.digimonworld.decode.randomizer.RandomizationContext;
import net.digimonworld.decode.randomizer.utils.JavaFXUtils;
import net.digimonworld.decode.randomizer.DecodeRandomizer;

public class NamingSettings implements Setting {

    private List<String> skippable = List.of("", "None", "Unused Item", "???", "NO DATA", "n");
    private BooleanProperty renameEnabled = new SimpleBooleanProperty();
    private BooleanProperty randomizeEnabled = new SimpleBooleanProperty();
    private BooleanProperty camelCase = new SimpleBooleanProperty();
    private BooleanProperty manualCsv = new SimpleBooleanProperty();
    private BooleanProperty replaceAll = new SimpleBooleanProperty();
    private BooleanProperty pickle = new SimpleBooleanProperty();
    private BooleanProperty orge = new SimpleBooleanProperty();
    private Map<Integer, BooleanProperty> propertyMap = new HashMap<>();

    private Accordion mainAc;
    // The tuple inside the ArrayList works like this: [0]
    public Map<String, ArrayList<int[]>> replacmentMap = new HashMap<>();

    private class Replacement {
        public String original;
        public String replacement;
        private List<String> excludedTerms;
        private List<PathPosition> disabledPaths;
        private int matchLength;
        private int index = -1;
        public boolean global = true;

        private class PathPosition {
            public int line = 0;
            public int col = 0;
            public String path = "";
            public boolean valid = true;

            public PathPosition(String pathDescriptor) {
                if (pathDescriptor.equals("")) {
                    this.valid = false;
                    return;
                } else {

                    String[] splitter = pathDescriptor.split(":", -1);
                    this.path = splitter[0];
                    this.line = Integer.parseInt(splitter[1]);
                    if (splitter[2] == null)
                        this.col = 0;
                    else
                        this.col = Integer.parseInt(splitter[2]);
                }
            }
        }

        public Replacement(String index, String original, String replacement, String rawExcludedTerms,
                String rawDisabledPaths) {
            this.index = Integer.parseInt(index);
            this.original = original;
            this.replacement = replacement;
            this.matchLength = original.length();
            this.excludedTerms = List.of(rawExcludedTerms.split(","));
            String[] pathos = rawDisabledPaths.split(",");
            for (int i = 0; i < pathos.length; i++) {
                String p = pathos[i];
                if (p.toLowerCase().equals("all"))
                    this.global = false;
                else if (!p.equals(""))
                    this.disabledPaths.add(new PathPosition(rawDisabledPaths));
            }
        }

        public void replaceExact(BTXPayload btx) {
            BTXEntry entry = btx.getEntryById(index).get();
            entry.setString(replacement);
        }

        private int realPosition(String path, int index) {
            ArrayList<int[]> repls = replacmentMap.get(path);
            if (repls == null)
                return index;
            int finalOffset = 0;
            for (int i = 0; i < repls.size(); i++) {
                int[] current = repls.get(i);
                int start = current[0];
                int offset = current[2];
                if (start > index)
                    break;
                finalOffset += offset;
            }
            return index + finalOffset;
        }

        private boolean isOverlapping(String path, int index) {
            ArrayList<int[]> repls = replacmentMap.get(path);
            if (repls == null)
                return false;
            int pos = realPosition(path, index);
            for (int i = 0; i < repls.size(); i++) {
                int[] current = repls.get(i);
                int start = current[0];
                int end = current[1];
                if (start > pos)
                    break;
                if (end < pos)
                    return true;

            }
            return false;
        }

        private void insertRepData(String path, int start, int end, int offset) {
            ArrayList<int[]> repls = replacmentMap.get(path);
            int[] entry = new int[] { start, end, offset };
            if (repls == null) {
                ArrayList<int[]> newList = new ArrayList<>();
                newList.add(entry);
                replacmentMap.put(path, newList);
                return;
            }
            for (int i = 0; i < entry.length; i++) {
                int current = repls.get(i)[0];
                if (current > start) {
                    repls.add(i, entry);
                    return;
                }
            }
            repls.add(entry);
        }

        private boolean termExclusion(String text, int index) {
            for (int i = 0; i < excludedTerms.size(); i++) {
                String term = excludedTerms.get(i);
                int exDex = text.indexOf(term);
                if (exDex == -1)
                    continue;
                int subDex = term.indexOf(original);
                if (exDex + subDex == index)
                    return true;
            }
            return false;
        }

        private boolean pathExclusion(String text, String path, int index) {
            int[] posData = getLinePos(text, realPosition(path, index));
            int line = posData[0];
            int col = posData[1];
            for (int i = 0; i < disabledPaths.size(); i++) {
                PathPosition p = disabledPaths.get(i);
                if (p.path == path && p.line == line && (p.col == 0 || p.col == col))
                    return true;
            }
            return false;
        }

        private int[] getLinePos(String text, int index) {
            String subtext = text.substring(0, index);
            List<String> splitlist = List.of(subtext.split("\n", -1));
            int lineNo = splitlist.size();
            int linePos = index - subtext.lastIndexOf("\n");

            return new int[] { lineNo, linePos };
        }

        private boolean findInText(String text, String path) {
            int idx = text.indexOf(original);
            if (idx == -1 || termExclusion(text, idx) || pathExclusion(text, path, idx) || isOverlapping(path, idx))
                return false;
            return true;
        }

    }

    private ArrayList<String> getNameListMethods(LanguageKeep lang) {
        ArrayList<String> methodList = new ArrayList<String>();
        Method[] methods = lang.getClass().getMethods();
        for (int i = 0; i < methods.length; i++) {
            String methodName = methods[i].getName();
            if (methodName.contains("Names"))
                methodList.add(methodName);
        }
        return methodList;
    }

    boolean clearExportDir(File dir) {
        try {
            if (dir.exists())
                Files.walkFileTree(dir.toPath(), new DeleteDirectoryFileVisitor());
        } catch (IOException exc) {
            exc.printStackTrace();
            return false;
        }
        dir.mkdir();
        return true;
    }

    private EventHandler<ActionEvent> buildHandler(String resourcePath, File targetDir) {
        return new EventHandler<>() {
            public void handle(ActionEvent e) {
                e.consume();
                clearExportDir(targetDir);
                URL origin = DecodeRandomizer.class.getResource(resourcePath);
                if (origin == null)
                    return;
                List<File> fls = Utils.listFiles(new File(origin.getFile()));
                fls.forEach(f -> {
                    try {
                        Files.copy(f.toPath(), new File(targetDir.toString() + "/" + f.getName()).toPath(),
                                StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException exc) {
                        exc.printStackTrace();
                        return;
                    }
                });
            }
        };
    }

    @Override
    public TitledPane create(GlobalKeepData data, LanguageKeep language) {

        mainAc = new Accordion();

        VBox restoreBox = new VBox(8);
        VBox randoBox = new VBox(8);

        restoreBox.setAlignment(Pos.TOP_LEFT);
        randoBox.setAlignment(Pos.TOP_LEFT);

        TitledPane pane = new TitledPane("Edit Names", mainAc);
        TitledPane restorePane = new TitledPane("Restore original names", restoreBox);
        restorePane.setId("restore");
        mainAc.setExpandedPane(restorePane);
        TitledPane randoPane = new TitledPane("Randomize names", randoBox);
        randoPane.setId("random");

        camelCase.set(true);
        pane.setCollapsible(false);
        File csvDir = new File("./renamingPresets/");
        manualCsv.set(csvDir.exists() && csvDir.isDirectory() && csvDir.listFiles().length != 0);

        ToggleSwitch camel = JavaFXUtils.buildToggleSwitch("camelCase names", Optional.empty(), Optional.of(camelCase));
        ToggleSwitch manCs = JavaFXUtils.buildToggleSwitch("use Manual CSV", Optional.empty(), Optional.of(manualCsv));
        ToggleSwitch repAll = JavaFXUtils.buildToggleSwitch("Replace terms in ALL text", Optional.empty(),
                Optional.of(replaceAll));

        manCs.disableProperty().bind(renameEnabled.not());
        repAll.disableProperty().bind(renameEnabled.not());
        camel.disableProperty().bind(new When(renameEnabled).then(manualCsv).otherwise(renameEnabled.not()));

        EventHandler<ActionEvent> rawExportHandler = new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                event.consume();
                ArrayList<String> methodList = getNameListMethods(language);

                System.out.println(methodList);
                clearExportDir(csvDir);
                methodList.forEach(s -> {
                    ArrayList<Tuple<Integer, String>> myList = new ArrayList<>();
                    try {
                        BTXPayload btx = (BTXPayload) language.getClass().getMethod(s).invoke(language);
                        btx.getEntries().stream()
                                .forEach(e -> myList
                                        .add(new Tuple<Integer, String>(e.getKey(), e.getValue().getString())));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    File destFile = new File("./renamingPresets/" + s.substring(3) + ".csv");
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(destFile, StandardCharsets.UTF_8))) {

                        writer.write("index;original;replace;excludeTerms;excludePaths\n");
                        String string = myList.stream()
                                .filter(str -> !skippable.contains(str.getValue()))
                                .map(str -> str.getKey().toString() + ';' + str.getValue() + ";" + str.getValue())
                                .collect(Collectors.joining(";;\n"))
                                + ";;";

                        writer.write(string);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }
                });
            }
        };

        Button camelExp = new Button("Export CSVs for CamelCase names");
        Button spaceExp = new Button("Export CSVs for spaced names");
        Button curExp = new Button("Export CSVs for current names");
        curExp.setOnAction(rawExportHandler);
        camelExp.setOnAction(buildHandler("renamingPresets/camelCase/", csvDir));
        spaceExp.setOnAction(buildHandler("renamingPresets/space/", csvDir));

        restoreBox.getChildren().addAll(
                JavaFXUtils.buildToggleSwitch("Enabled", Optional.empty(), Optional.of(renameEnabled)),
                manCs,
                camel,
                repAll,
                curExp,
                camelExp,
                spaceExp);
        randoBox.getChildren().addAll(
                JavaFXUtils.buildToggleSwitch("Enabled", Optional.empty(), Optional.of(randomizeEnabled)));

        mainAc.getPanes().addAll(restorePane, randoPane);
        return pane;
    }

    private class PathResolver {
        private RandomizationContext context;
        public Map<String, String> shortcuts = new HashMap<>();
        public Map<String, String> keepMap = new HashMap<>();

        public PathResolver(RandomizationContext context) {
            this.context = context;
            this.shortcuts.put("keep", "Keep/LanguageKeep_jp.res");
            this.shortcuts.put("map", "map/text");
            this.keepMap.put("DigimonNames", "11");
            this.keepMap.put("ItemNames", "0");
            this.keepMap.put("KeyItemNames", "3");
            this.keepMap.put("AccessoryNames", "5");
            this.keepMap.put("SkillNames", "7");
            this.keepMap.put("CharacterNames", "13");
            this.keepMap.put("NatureNames", "16");
            this.keepMap.put("MedalNames", "17");
            this.keepMap.put("GlossaryNames", "25");
            this.keepMap.put("CardNames1", "27");
            this.keepMap.put("CardNames2", "28");
            this.keepMap.put("CardSetNames", "30");
        }

        public BTXPayload resolve(String path) {
            ArrayList<String> frag = new ArrayList<>(
                    List.of((keepMap.containsKey(path) ? ("keep-" + keepMap.get(path)) : path).split("-")));
            int btxIndex = Integer.parseInt(frag.remove(frag.size() - 1));
            String finalPath = "part0/arcv/" + frag.stream()
                    .map(s -> shortcuts.containsKey(s) ? shortcuts.get(s) : s)
                    .collect(Collectors.joining("/"));
            NormalKCAP pk = (NormalKCAP) context.getFile(finalPath).get();
            if (frag.get(frag.size() - 1).equals("keep"))
                pk = (NormalKCAP) pk.get(0);
            return (BTXPayload) pk.get(btxIndex);
        }
    }

    private void targetedBtxReplacement(BTXPayload btx, File f) {
        System.out.println(f.getName());
        try {
            List<String> lines = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
            ArrayList<Replacement> reps = new ArrayList<>();
            for (int i = 0; i < lines.size(); i++) {
                if (i == 0)
                    continue;
                String[] entries = lines.get(i).split(";", -1);
                if (entries[1].equals(entries[2]))
                    continue;
                Replacement rep = new Replacement(entries[0], entries[1], entries[2], entries[3], entries[4]);
                rep.replaceExact(btx);
                if (replaceAll.get() && rep.global)
                    reps.add(rep);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void randomize(RandomizationContext context) {
        String mode = mainAc.getExpandedPane().getId();
        if (!(mode.equals("restore") ? renameEnabled.get() : randomizeEnabled.get()))
            return;

        PathResolver res = new PathResolver(context);
        if (mode.equals("restore")) {
            File manualCsvDir = new File("./renamingPresets/");
            File origin;
            if (manualCsv.get() && manualCsvDir.exists()) {
                origin = manualCsvDir;
            } else {
                String resourcePath = "renamingPresets/" + (camelCase.get() ? "camelCase" : "space") + "/";
                origin = new File(DecodeRandomizer.class.getResource(resourcePath).getFile());
            }
            List<File> presets = List.of(origin.listFiles());
            presets.stream().filter(f -> f.getName().contains("-")).forEach(p -> {
                String pName = p.getName();
                BTXPayload foundBtx = res.resolve(pName.substring(0, pName.length() - 4));
                targetedBtxReplacement(foundBtx, p);
            });

            if (!replaceAll.get())
                return;

            return;
        }

    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("enabled", renameEnabled.get());
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
        renameEnabled.set(Boolean.parseBoolean(map.string("enabled")));
    }
}
