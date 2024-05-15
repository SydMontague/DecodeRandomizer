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
import java.text.ParseException;
import java.util.NoSuchElementException;

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
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.When;
import javafx.scene.control.Button;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Accordion;
import javafx.scene.layout.VBox;
import net.digimonworld.decode.randomizer.RandomizationContext;
import net.digimonworld.decode.randomizer.utils.JavaFXUtils;
import net.digimonworld.decode.randomizer.DecodeRandomizer;

public class NamingSettings implements Setting {

    private final List<String> skippable = List.of("", "None", "Unused Item", "???", "NO DATA", "n");
    private final BooleanProperty renameEnabled = new SimpleBooleanProperty();
    private final BooleanProperty randomizeEnabled = new SimpleBooleanProperty();
    private final BooleanProperty camelCase = new SimpleBooleanProperty(true);
    private final BooleanProperty manualCsv = new SimpleBooleanProperty();
    private final BooleanProperty replaceAll = new SimpleBooleanProperty();
    private final BooleanProperty pickle = new SimpleBooleanProperty(false);
    private final BooleanProperty ogre = new SimpleBooleanProperty(false);
    private final BooleanProperty blackPrefix = new SimpleBooleanProperty(false);
    private final Map<Integer, BooleanProperty> propertyMap = new HashMap<>();
    private final Map<String, BooleanProperty> randoMap = new HashMap<>();
    private final List<String> randoTypes = List.of("Digimon Names", "Finisher Names", "Skill Names", "Character Names", "Item Names", "Medal Names");

    private Accordion mainAc;
    /**
     * Visualization of a replacement map:
     *
     * { "part0/arcv/Keep/LanguageKeep_jp.res/11:12": [ [0,4,2], [8,13,-1] ] }
     *
     * Path schema works like this:
     *
     * [string path to the actual file]/[index of BTX file]:[BTX line]
     *
     * Replacement info pattern:
     *
     * [match start, match end, offset of result]
     *
     * Replacements are saved to prevent replacing terms that have already been
     * processed by a prior replacement. The offsets are used to map a position
     * of a match to its position in the unmodified line, making it possible to
     * exclude replacing matches at specific indices in a line based on the
     * original file.
     */
    public Map<String, ArrayList<int[]>> replacementMap = new HashMap<>();

    protected enum TermType {
        GENERAL,
        DIGIMON,
        DIGIMONMULTI
    }

    private static TermType classifyTerm(String term, String path) {
        List<String> digiNamePaths = List.of(11, 27, 28).stream()
                .map(n -> "part0/arcv/Keep/LanguageKeep_jp.res/" + n).collect(Collectors.toList());
        if (!digiNamePaths.contains(path)) {
            return TermType.GENERAL;
        }
        return term.matches("[a-z][A-Z]") ? TermType.DIGIMONMULTI : TermType.DIGIMON;
    }

    private void btxSwitch(BTXEntry btxA, BTXEntry btxB) {
        String a = btxA.getString();
        String b = btxB.getString();
        btxA.setString(b);
        btxB.setString(a);
    }

    private class Replacement {

        public String original;
        public String replacement;
        private final List<String> excludedTerms;
        private List<PathPosition> disabledPaths;
        private final int matchLength;
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
                }

                String[] splitter = pathDescriptor.split(":", -1);
                this.path = splitter[0];
                this.line = Integer.parseInt(splitter[1]);
                if (splitter[2] == null) {
                    this.col = 0;
                } else {
                    this.col = Integer.parseInt(splitter[2]);
                }

            }
        }

        public Replacement(String index, String original, String replacement, String rawExcludedTerms, String rawDisabledPaths, String origin) {
            this.index = Integer.parseInt(index);
            this.original = original;

            TermType tType = classifyTerm(replacement, origin);

            if (!manualCsv.get() && tType != TermType.GENERAL) {
                if (ogre.get() && replacement.equals("Orgemon")) {
                    this.replacement = "Ogremon";
                }
                if (pickle.get() && replacement.equals("Piccolomon")) {
                    this.replacement = "Picklemon";
                } else if (blackPrefix.get() && replacement.endsWith("mon (Black)")) {
                    this.replacement = "Black" + replacement.substring(0, replacement.length() - 8);
                } else {
                    this.replacement = replacement;
                }
            } else {
                this.replacement = replacement;
            }

            if (tType == TermType.DIGIMONMULTI && !camelCase.get()) {
                this.replacement = this.replacement.replaceAll("([a-z])([A-Z])", "$1 $2");
            }

            this.matchLength = original.length();
            this.excludedTerms = List.of(rawExcludedTerms.split(","));
            String[] pathos = rawDisabledPaths.split(",");
            for (String p : pathos) {
                if (p.toLowerCase().equals("all")) {
                    this.global = false;
                } else if (!p.equals("")) {
                    this.disabledPaths.add(new PathPosition(rawDisabledPaths));
                }
            }
        }

        public void replaceExact(BTXPayload btx, String path) {
            BTXEntry entry = btx.getEntryById(index).get();
            if (!original.equals(replacement)) {
                entry.setString(replacement);
            }
            // Exact replacements block any future replacements on this particular line.
            insertRepData(path, -1, Integer.MAX_VALUE, 0);
        }

        private int realPosition(String path, int index) {
            ArrayList<int[]> repls = replacementMap.get(path);
            if (repls == null) {
                return index;
            }
            int finalOffset = 0;
            for (int i = 0; i < repls.size(); i++) {
                int[] current = repls.get(i);
                int start = current[0];
                int offset = current[2];
                if (start > index) {
                    break;
                }
                finalOffset += offset;
            }
            return index + finalOffset;
        }

        private boolean isOverlapping(String path, int index) {
            ArrayList<int[]> repls = replacementMap.get(path);
            if (repls == null) {
                return false;
            }
            int pos = realPosition(path, index);
            for (int i = 0; i < repls.size(); i++) {
                int[] current = repls.get(i);
                int start = current[0];
                int end = current[1];
                if (start > pos) {
                    break;
                }
                if (end < pos) {
                    return true;
                }

            }
            return false;
        }

        private void insertRepData(String path, int start, int end, int offset) {
            int[] entry = new int[]{start, end, offset};
            if (!replacementMap.containsKey(path)) {
                ArrayList<int[]> newList = new ArrayList<>();
                newList.add(entry);
                replacementMap.put(path, newList);
                return;
            }
            ArrayList<int[]> repls = replacementMap.get(path);
            for (int i = 0; i < repls.size(); i++) {
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
                if (exDex == -1) {
                    continue;
                }
                int subDex = term.indexOf(original);
                if (exDex + subDex == index) {
                    return true;
                }
            }
            return false;
        }

        private boolean pathExclusion(String text, String path, int index) {
            int[] posData = getLinePos(text, realPosition(path, index));
            int line = posData[0];
            int col = posData[1];
            for (int i = 0; i < disabledPaths.size(); i++) {
                PathPosition p = disabledPaths.get(i);
                if (p.path.equals(path) && p.line == line && (p.col == 0 || p.col == col)) {
                    return true;
                }
            }
            return false;
        }

        private int[] getLinePos(String text, int index) {
            String subtext = text.substring(0, index);
            List<String> splitlist = List.of(subtext.split("\n", -1));
            int lineNo = splitlist.size();
            int linePos = index - subtext.lastIndexOf("\n");

            return new int[]{lineNo, linePos};
        }

        private boolean findInText(String text, String path) {
            int idx = text.indexOf(original);
            return !(idx == -1 || termExclusion(text, idx) || pathExclusion(text, path, idx) || isOverlapping(path, idx));
        }

    }

    private ArrayList<String> getNameListMethods(LanguageKeep lang) {
        ArrayList<String> methodList = new ArrayList<>();
        Method[] methods = lang.getClass().getMethods();
        for (Method method : methods) {
            String methodName = method.getName();
            if (methodName.contains("Names")) {
                methodList.add(methodName);
            }
        }
        return methodList;
    }

    boolean clearExportDir(File dir) {
        try {
            if (dir.exists()) {
                Files.walkFileTree(dir.toPath(), new DeleteDirectoryFileVisitor());
            }
        } catch (IOException exc) {
            exc.printStackTrace();
            return false;
        }
        dir.mkdir();
        return true;
    }

    private EventHandler<ActionEvent> buildHandler(String resourcePath, File targetDir) {
        return (ActionEvent e) -> {
            e.consume();
            clearExportDir(targetDir);
            URL origin = DecodeRandomizer.class.getResource(resourcePath);
            if (origin == null) {
                return;
            }
            List<File> fls = Utils.listFiles(new File(origin.getFile()));
            fls.forEach(f -> {
                try {
                    Files.copy(f.toPath(), new File(targetDir.toString() + "/" + f.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException exc) {
                    exc.printStackTrace();
                }
            });
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

        pane.setCollapsible(false);
        File csvDir = new File("./renamingPresets/");
        manualCsv.set(csvDir.exists() && csvDir.isDirectory() && csvDir.listFiles().length != 0);

        ToggleSwitch camel = JavaFXUtils.buildToggleSwitch("camelCase names", Optional.empty(), Optional.of(camelCase));
        ToggleSwitch manCs = JavaFXUtils.buildToggleSwitch("use Manual CSV", Optional.empty(), Optional.of(manualCsv));
        ToggleSwitch orgeCheck = JavaFXUtils.buildToggleSwitch("'Ogremon' spelling", Optional.empty(), Optional.of(ogre));
        ToggleSwitch pickleCheck = JavaFXUtils.buildToggleSwitch("'Picklemon' spelling", Optional.empty(), Optional.of(pickle));
        ToggleSwitch blackCheck = JavaFXUtils.buildToggleSwitch("Always use 'Black' as prefix", Optional.empty(), Optional.of(blackPrefix));
        ToggleSwitch repAll = JavaFXUtils.buildToggleSwitch("Replace terms in ALL text", Optional.empty(), Optional.of(replaceAll));

        BooleanBinding manLink = new When(renameEnabled).then(manualCsv).otherwise(renameEnabled.not());

        manCs.disableProperty().bind(renameEnabled.not());
        repAll.disableProperty().bind(renameEnabled.not());
        orgeCheck.disableProperty().bind(manLink);
        blackCheck.disableProperty().bind(manLink);
        pickleCheck.disableProperty().bind(manLink);
        camel.disableProperty().bind(manLink);

        EventHandler<ActionEvent> rawExportHandler = (ActionEvent event) -> {
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
                }
            });
        };

        Button camelExp = new Button("Export CSVs for restoration preset");
        Button curExp = new Button("Export CSVs for current names");
        curExp.setOnAction(rawExportHandler);
        camelExp.setOnAction(buildHandler("renamingPresets/", csvDir));

        restoreBox.getChildren().addAll(
                JavaFXUtils.buildToggleSwitch("Enabled", Optional.empty(), Optional.of(renameEnabled)),
                manCs,
                camel,
                repAll,
                blackCheck,
                orgeCheck,
                pickleCheck,
                curExp,
                camelExp);
        randoBox.getChildren().addAll(
                JavaFXUtils.buildToggleSwitch("Enabled", Optional.empty(), Optional.of(randomizeEnabled)));

        for (String r : randoTypes) {
            randoMap.put(r, new SimpleBooleanProperty(false));
            ToggleSwitch swit = JavaFXUtils.buildToggleSwitch(r, Optional.empty(), Optional.of(randoMap.get(r)));
            swit.disableProperty().bind(randomizeEnabled.not());
            randoBox.getChildren().add(swit);
        }

        mainAc.getPanes().addAll(restorePane, randoPane);
        return pane;
    }

    private class PathResolver {

        private final RandomizationContext context;
        private final Map<String, String> shortcuts = new HashMap<>();
        public final Map<String, String> keepMap = new HashMap<>();

        public PathResolver(RandomizationContext context) {
            this.context = context;
            this.shortcuts.put("keep", "Keep/LanguageKeep_jp.res");
            this.shortcuts.put("map", "map/text");
            this.keepMap.put("DigimonNames", "11");
            this.keepMap.put("ItemNames", "0");
            this.keepMap.put("KeyItemNames", "3");
            this.keepMap.put("AccessoryNames", "5");
            this.keepMap.put("SkillNames", "7");
            this.keepMap.put("FinisherNames", "9");
            this.keepMap.put("CharacterNames", "13");
            this.keepMap.put("NatureNames", "16");
            this.keepMap.put("MedalNames", "17");
            this.keepMap.put("GlossaryNames", "25");
            this.keepMap.put("CardNames1", "27");
            this.keepMap.put("CardNames2", "28");
            this.keepMap.put("CardSetNames", "30");
        }

        public Tuple<String, BTXPayload> resolve(String path) throws ParseException {
            ArrayList<String> frag = new ArrayList<>(
                    List.of((keepMap.containsKey(path) ? ("keep-" + keepMap.get(path)) : path).split("-")));
            int btxIndex = Integer.parseInt(frag.remove(frag.size() - 1));
            String finalPath = "part0/arcv/" + frag.stream()
                    .map(s -> shortcuts.containsKey(s) ? shortcuts.get(s) : s)
                    .collect(Collectors.joining("/"));
            try {
                NormalKCAP pk = (NormalKCAP) context.getFile(finalPath).get();
                if (frag.get(frag.size() - 1).equals("keep")) {
                    pk = (NormalKCAP) pk.get(0);
                }
                return new Tuple<>(finalPath + "/" + btxIndex, (BTXPayload) pk.get(btxIndex));
            } catch (NoSuchElementException exc) {
                throw new ParseException("csv not correctly mapped", 0);
            }
        }
    }

    private void targetedBtxReplacement(BTXPayload btx, File f, String path) {
        System.out.println(f.getName());
        try {
            List<String> lines = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
            ArrayList<Replacement> reps = new ArrayList<>();
            for (int i = 0; i < lines.size(); i++) {
                if (i == 0) {
                    continue;
                }
                String[] entries = lines.get(i).split(";", -1);
                // Even if we don't replace a term, if it's a multipart Digimon name it will be changed if the camelCase option is not set.
                if (entries[1].equals(entries[2]) && (camelCase.get() || classifyTerm(entries[2], path) != TermType.DIGIMONMULTI)) {
                    continue;
                }
                Replacement rep = new Replacement(entries[0], entries[1], entries[2], entries[3], entries[4], path);
                rep.replaceExact(btx, path);
                if (replaceAll.get() && rep.global) {
                    reps.add(rep);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void randomize(RandomizationContext context) {
        String mode = mainAc.getExpandedPane().getId();
        if (!(mode.equals("restore") ? renameEnabled.get() : randomizeEnabled.get())) {
            return;
        }

        PathResolver res = new PathResolver(context);
        if (mode.equals("restore")) {
            File manualCsvDir = new File("./renamingPresets/");
            File origin;
            if (manualCsv.get() && manualCsvDir.exists()) {
                origin = manualCsvDir;
            } else {
                String resourcePath = "renamingPresets/";
                origin = new File(DecodeRandomizer.class.getResource(resourcePath).getFile());
            }
            List<File> presets = List.of(origin.listFiles());
            presets.stream().forEach(p -> {
                String pName = p.getName();
                try {
                    Tuple<String, BTXPayload> foundBtx = res.resolve(pName.substring(0, pName.length() - 4));
                    targetedBtxReplacement(foundBtx.getValue(), p, foundBtx.getKey());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            });

            if (!replaceAll.get()) {
                return;
            }

        } else {
            Random rand = new Random(context.getInitialSeed() * "ShuffleTerms".hashCode());
            randoTypes.stream().filter(k -> randoMap.get(k).get()).map(s -> s.replaceAll(" ", "")).forEach(name -> {
                try {
                    //creating a list of all btx entries in the payload without empty/filler fields
                    ArrayList<BTXEntry> entries = new ArrayList<>(res.resolve(name).getValue().getEntries().stream().map(e -> e.getValue()).filter(v -> !skippable.contains(v.getString())).collect(Collectors.toList()));

                    BTXEntry firstEntry = null;
                    //Switching the value of a random pair of BTX entries and removing them from the list.
                    while (entries.size() > 1) {
                        int i = rand.nextInt(entries.size());
                        BTXEntry btxA = entries.remove(i);
                        if (firstEntry == null) {
                            firstEntry = btxA;
                        }
                        int n = rand.nextInt(entries.size());
                        BTXEntry btxB = entries.remove(n);
                        btxSwitch(btxA, btxB);
                    }
                    //In case there's an uneven number of entries we switch the leftover entry with the first entry we processed previously
                    if (entries.size() == 1) {
                        btxSwitch(firstEntry, entries.get(0));
                    }
                } catch (ParseException e) {
                }
            });
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
        if (map == null) {
            return;
        }

        YamlSequence list = map.yamlSequence("checked");
        List<Integer> activeList = list == null ? new ArrayList<>()
                : list.values().stream().map(a -> Integer.parseInt(a.asScalar().value())).collect(Collectors.toList());
        propertyMap.forEach((a, b) -> b.set(activeList.contains(a)));
        renameEnabled.set(Boolean.parseBoolean(map.string("enabled")));
    }
}
