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
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.NoSuchElementException;
import java.util.Comparator;
import java.util.stream.Collector;

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
import net.digimonworld.decodetools.res.ResPayload;
import net.digimonworld.decodetools.res.ResPayload.Payload;

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
    private final BooleanProperty replaceAll = new SimpleBooleanProperty(true);
    private final BooleanProperty pickle = new SimpleBooleanProperty(false);
    private final BooleanProperty ogre = new SimpleBooleanProperty(false);
    private final BooleanProperty blackPrefix = new SimpleBooleanProperty(false);
    private final Map<Integer, BooleanProperty> propertyMap = new HashMap<>();
    private final Map<String, BooleanProperty> randoMap = new HashMap<>();
    private final List<String> randoTypes = List.of("Digimon Names", "Finisher Names", "Skill Names", "Character Names", "Item Names", "Medal Names");
    private final List<String> priorities = List.of("DigimonNames.csv", "CardNames1.csv");
    private final Map<String, Replacement> repMap = new HashMap<>();

    private Accordion mainAc;
    /**
     * Visualization of a replacement map:
     *
     * { "part0\arcv\Keep\LanguageKeep_jp.res\11:12" : [ [0,4,2], [8,13,-1] ] }
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
        //These files contain digimon names
        List<String> digiNamePaths = List.of(11, 27, 28).stream()
                .map(n -> "part0\\arcv\\Keep\\LanguageKeep_jp.res\\" + n).collect(Collectors.toList());
        if (!digiNamePaths.contains(path)) {
            return TermType.GENERAL;
        }
        return term.matches(".*[a-z][A-Z].*") ? TermType.DIGIMONMULTI : TermType.DIGIMON;
    }

    private static void btxSwitch(BTXEntry btxA, BTXEntry btxB) {
        String a = btxA.getString();
        String b = btxB.getString();
        btxA.setString(b);
        btxB.setString(a);
    }

    private class Replacement {

        public String original;
        public String replacement;
        private final List<String> excludedTerms;
        private final ArrayList<PathPosition> disabledPaths = new ArrayList();
        private final int matchLength;
        private int index = -1;
        private final boolean diffS;
        private final boolean diffArt;
        private final int baseOffset;
        public boolean global = true;
        private final List<String> vow = List.of("A", "E", "I", "O", "U", "Ü", "Ö", "Ä");

        private class PathPosition {

            public int line = 0;
            public int col = -1;
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
                if (splitter.length == 1) {
                    this.col = -1;
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

            this.diffS = replacement.endsWith("s") != original.endsWith("s");
            this.diffArt = vow.contains(this.replacement.substring(0, 1)) != vow.contains(this.original.substring(0, 1));

            this.matchLength = original.length();

            this.baseOffset = replacement.length() - original.length();

            this.excludedTerms = List.of(rawExcludedTerms.split(",")).stream().map(p -> p.replaceAll("\\$", original)).collect(Collectors.toList());
            String[] pathos = rawDisabledPaths.split(",");
            for (String p : pathos) {
                if (p.toLowerCase().equals("all")) {
                    this.global = false;
                } else if (!p.equals("")) {
                    this.disabledPaths.add(new PathPosition(p));
                }
            }
        }

        public void replaceExact(BTXPayload btx, String path) {
            BTXEntry entry = btx.getEntryById(index).get();
            if (!original.equals(replacement)) {
                entry.setString(replacement);
                System.out.println(original + " -> " + replacement);
            }
            // Exact replacements block any future replacements on this particular line.
            insertRepData(path + ":" + index, -1, Integer.MAX_VALUE, 0);
        }

        //Fixing mistakes like Davis' -> Daisuke' etc
        private int correctApostrophe(BTXEntry btx, int end) {
            if (!diffS) {
                return 0;
            }
            String text = btx.getString();
            if (text.length() == end) {
                return 0;
            }
            if (replacement.endsWith("s")) {
                // remove s after apostrophe
                if (text.substring(end, Math.min(end + 2, text.length())).equals("'s")) {
                    btx.setString(text.substring(0, end + 1) + text.substring(end + 2));
                    return -1;
                } else {
                    return 0;
                }
            } else {
                // add s after apostrophe
                if (text.substring(end, Math.min(end + 1, text.length())).equals("'")) {
                    btx.setString(text.substring(0, end + 1) + "s" + text.substring(end + 1));
                    return 1;
                }
                return 0;
            }
        }

        //Fixing mistakes like "a Scorpiomon" -> "a Anomalicarimon"
        private int correctArticle(BTXEntry btx, int start) {
            if (!diffArt || start == 0) {
                return 0;
            }
            String text = btx.getString();
            if (vow.contains(replacement.substring(0, 1))) {
                if (text.substring(Math.max(0, start - 3), start).matches(".*\ba\b.*")) {
                    btx.setString(text.substring(0, start - 1) + "n" + text.substring(start - 1));
                    return 1;
                }
                return 0;
            } else {
                if (text.substring(Math.max(0, start - 4), start).matches(".*\ban\b.*")) {
                    btx.setString(text.substring(0, start - 2) + text.substring(start - 1));
                    return -1;
                }
                return 0;
            }
        }

        public void replaceDynamic(BTXEntry btx, String path) {
            String origText = btx.getString();
            int matchStart = findInText(origText, path);
            if (matchStart == -1) {
                return;
            }

            int matchEnd = matchStart + matchLength;
            btx.setString(origText.substring(0, matchStart) + replacement + origText.substring(matchEnd));

            int artOff = correctArticle(btx, matchStart);
            int apOff = correctApostrophe(btx, matchEnd);

            matchStart = matchStart + artOff;
            matchEnd = matchEnd + apOff;
            int finalOffset = baseOffset + artOff + apOff;
            System.out.println(path + " | " + origText.substring(matchStart, matchEnd) + " -> " + btx.getString().substring(matchStart, matchEnd + finalOffset));

            insertRepData(path, matchStart, matchEnd, finalOffset);
        }

        private int realPosition(String path, int index) {
            ArrayList<int[]> repls = replacementMap.get(path);
            if (repls == null) {
                return index;
            }
            int finalOffset = 0;
            for (int[] current : repls) {
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
            for (int[] current : repls) {
                int start = current[0];
                int end = current[1];
                // If the start position is bigger than the end of our match we don't need to check the rest
                if (start >= pos + (matchLength - 1)) {
                    break;
                }
                if (end >= pos && start < pos) {
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
            //Keeping the list of replacements of each BTX well ordered by inserting it before the first bigger start value
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
            for (String term : excludedTerms) {
                if (term.equals("[]")) {
                    if (!text.substring(Math.max(0, index - 2), Math.min(text.length(), index + 2)).matches(".*\b" + original + "\b.*")) {
                        return true;
                    }
                    continue;
                }
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

        private boolean pathExclusion(String path, int index) {
            for (PathPosition p : disabledPaths) {
                if (p.path.equals(path) && (p.col == -1 || p.col == index)) {
                    return true;
                }
            }
            return false;
        }

        private int findInText(String text, String path) {
            int idx = text.indexOf(original);
            //If any of the exclusion 
            return (idx == -1 || termExclusion(text, idx) || pathExclusion(path, idx) || isOverlapping(path, idx)) ? -1 : idx;
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

    private static boolean clearExportDir(File dir) {
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

    private static EventHandler<ActionEvent> buildHandler(String resourcePath, File targetDir) {
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
                    Files.copy(f.toPath(), Path.of(targetDir.toString() + "\\" + f.getName()), StandardCopyOption.REPLACE_EXISTING);
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
        File csvDir = new File(".\\renamingPresets\\");
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

                File destFile = new File(".\\renamingPresets\\" + s.substring(3) + ".csv");
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(destFile, StandardCharsets.UTF_8))) {
                    writer.write("index;original;replace;excludeTerms;excludePaths\n");
                    String string = myList.stream()
                            .filter(str -> !skippable.contains(str.getValue()))
                            .map(str -> str.getKey().toString() + ';' + str.getValue() + ";" + str.getValue())
                            .collect(Collectors.joining(";;\n"))
                            + ";;";
                    writer.write(string);
                } catch (IOException e) {
                }
            });
        };

        Button camelExp = new Button("Export CSVs for restoration preset");
        Button curExp = new Button("Export CSVs for current names");
        curExp.setOnAction(rawExportHandler);
        camelExp.setOnAction(buildHandler("renamingPresets\\", csvDir));

        restoreBox.getChildren().addAll(
                JavaFXUtils.buildToggleSwitch("Enabled", Optional.empty(), Optional.of(renameEnabled)),
                repAll,
                manCs,
                camel,
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
            this.shortcuts.put("keep", "Keep\\LanguageKeep_jp.res");
            this.shortcuts.put("map", "map\\text");
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

        public ResPayload resolveRaw(String finalPath) throws IOException {
            try {
                return context.getFile(finalPath).get();
            } catch (NoSuchElementException exc) {
                throw new IOException("Path " + finalPath + " does not exist.");
            }
        }

        public Tuple<String, BTXPayload> resolve(String path) throws ParseException {
            ArrayList<String> frag = new ArrayList<>(
                    List.of((keepMap.containsKey(path) ? ("keep-" + keepMap.get(path)) : path).split("-")));
            int btxIndex = Integer.parseInt(frag.remove(frag.size() - 1));
            String finalPath = "part0\\arcv\\" + frag.stream()
                    .map(s -> shortcuts.containsKey(s) ? shortcuts.get(s) : s)
                    .collect(Collectors.joining("\\"));
            try {
                NormalKCAP pk = (NormalKCAP) context.getFile(finalPath).get();
                if (frag.get(frag.size() - 1).equals("keep")) {
                    pk = (NormalKCAP) pk.get(0);
                }
                return new Tuple<>(finalPath + "\\" + btxIndex, (BTXPayload) pk.get(btxIndex));
            } catch (NoSuchElementException exc) {
                throw new ParseException("csv not correctly mapped", 0);
            }
        }
    }

    private void targetedBtxReplacement(BTXPayload btx, File f, String path) {
        System.out.println(f.getName());
        try {
            List<String> lines = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
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
                //By adding only the first replacement of a word to the global replacement list, we only need to the global rules once.
                if (replaceAll.get() && rep.global && !repMap.containsKey(rep.original)) {
                    repMap.put(rep.original, rep);
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
            File manualCsvDir = new File(".\\renamingPresets\\");
            File origin;
            if (manualCsv.get() && manualCsvDir.exists()) {
                origin = manualCsvDir;
            } else {
                origin = new File(DecodeRandomizer.class.getResource("renamingPresets/").getFile());
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

            File startDir = new File(".\\working\\part0\\arcv\\");

            //Sorting 
            List<Replacement> sortedReps = repMap.values().stream().sorted(Comparator.comparing(v -> v.original.length() * -1)).collect(Collectors.toList());
            ArrayList<Tuple<String, BTXEntry>> fileEntries = new ArrayList();
            Utils.listFiles(startDir).stream()
                    //Everything that could contain BTX
                    .filter(s -> s.getName().endsWith("_jp.res")
                    || s.getName().endsWith(".pack")
                    ).forEach(fA -> {
                        try {
                            Path longPath = fA.toPath();
                            Path normalPath = longPath.subpath(2, longPath.getNameCount());

                            var elements = res.resolveRaw(normalPath.toString()).getElementsWithType(Payload.BTX);
                            if (elements.isEmpty()) {
                                return;
                            }
                            for (int i = 0; i < elements.size(); i++) {
                                var payload = (BTXPayload) elements.get(i);
                                String partialPath = normalPath.toString() + "\\" + (i);
                                payload.getEntries().forEach(bt -> {
                                    fileEntries.add(new Tuple(partialPath + ":" + bt.getKey(), bt.getValue()));
                                });
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
            sortedReps.forEach(rep -> {
                fileEntries.forEach(t -> rep.replaceDynamic(t.getValue(), t.getKey()));
            });

            repMap.clear();
            replacementMap.clear();

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
                    e.printStackTrace();
                }
            });
        }
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("renameEnabled", renameEnabled.get());
        map.put("randomizeEnabled", randomizeEnabled.get());
        map.put("camelCase", camelCase.get());
        map.put("manualCsv", manualCsv.get());
        map.put("replaceAll", replaceAll.get());
        map.put("pickle", pickle.get());
        map.put("ogre", ogre.get());
        map.put("blackPrefix", blackPrefix.get());
        map.put("randomChecked", randoMap.entrySet().stream().filter(a -> a.getValue().get()).map(Map.Entry::getKey)
                .collect(Collectors.toList()));
        return map;
    }

    @Override
    public void load(YamlMapping map) {
        if (map == null) {
            return;
        }
        YamlSequence list = map.yamlSequence("randomChecked");
        List<String> activeList = list == null ? new ArrayList<>()
                : list.values().stream().map(a -> a.toString()).collect(Collectors.toList());
        randoMap.forEach((a, b) -> b.set(activeList.contains(a)));
        renameEnabled.set(Boolean.parseBoolean(map.string("enabled")));
    }
}
