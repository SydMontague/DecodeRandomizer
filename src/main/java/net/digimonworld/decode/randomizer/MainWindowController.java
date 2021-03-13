package net.digimonworld.decode.randomizer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import org.controlsfx.control.ToggleSwitch;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import net.digimonworld.decode.randomizer.settings.RandomizerSettings;
import net.digimonworld.decode.randomizer.utils.JavaFXUtils;
import net.digimonworld.decodetools.core.Access;
import net.digimonworld.decodetools.core.DeleteDirectoryFileVisitor;
import net.digimonworld.decodetools.core.FileAccess;
import net.digimonworld.decodetools.core.Utils;
import net.digimonworld.decodetools.keepdata.GlobalKeepData;
import net.digimonworld.decodetools.keepdata.LanguageKeep;
import net.digimonworld.decodetools.randomizer.Randomizer;
import net.digimonworld.decodetools.res.ResPayload;
import net.digimonworld.decodetools.res.kcap.AbstractKCAP;

public class MainWindowController {
    private static final ExtensionFilter CCIFilter = new ExtensionFilter("3DS (for Citra)", "*.3ds", "*.cci");
    private static final ExtensionFilter CIAFilter = new ExtensionFilter("CIA (for Console)", "*.cia");
    
    private static final Path WORKING_PATH = Paths.get("./working");
    
    @FXML
    private Scene root;
    @FXML
    private BorderPane rootPane;
    @FXML
    private TextField outputField;
    @FXML
    private TextField seedField;
    @FXML
    private Label romFoundSymbol;
    @FXML
    private TabPane settingsPane;
    @FXML
    private ToggleSwitch raceLogging;
    @FXML
    private ToggleSwitch dryFire;
    
    private RandomizerSettings settings = new RandomizerSettings();
    
    private GlobalKeepData inputData = null;
    private LanguageKeep languageKeep = null;
    
    @FXML
    public void initialize() {
        Platform.runLater(this::updatedRomStatus);
    }
    
    private void updateSettings() {
        settingsPane.getTabs().setAll(settings.create(inputData, languageKeep));
        root.getWindow().sizeToScene();
    }
    
    @FXML
    public void onAbout(ActionEvent e) throws IOException {
        ((Stage) new FXMLLoader(getClass().getResource("AboutScene.fxml")).load()).showAndWait();
    }
    
    @FXML
    public void onClose(ActionEvent e) {
        Platform.exit();
    }
    
    @FXML
    public void createRandomSeed(ActionEvent e) {
        seedField.setText(Long.toString(new Random().nextLong()));
    }
    
    @FXML
    public void selectOutputPath(ActionEvent e) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Where to store the randomized ROM?");
        chooser.setInitialDirectory(new File("."));
        chooser.getExtensionFilters().add(CCIFilter);
        chooser.getExtensionFilters().add(CIAFilter);
        
        File selected = chooser.showSaveDialog(root.getWindow());
        
        if (selected != null)
            outputField.setText(selected.toString());
    }
    
    @FXML
    public void setupBaseROM(ActionEvent e) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select base ROM");
        chooser.setInitialDirectory(new File("."));
        chooser.getExtensionFilters().add(CCIFilter);
        chooser.getExtensionFilters().add(CIAFilter);
        
        File selected = chooser.showOpenDialog(root.getWindow());
        
        if (selected == null)
            return;
        
        if (chooser.getSelectedExtensionFilter() == CIAFilter) {
            System.out.println("CIA is not yet supported.");
            return;
        }
        
        Alert alert = new Alert(AlertType.NONE);
        alert.setTitle("Extracting ROM...");
        alert.setHeaderText(null);
        alert.setContentText("Extracting ROM. This might take a minute.");
        alert.show();
        
        CompletableFuture.supplyAsync(() -> Randomizer.extract3DSFile(WORKING_PATH, selected.toPath())).thenAccept(a -> Platform.runLater(() -> {
            alert.setResult(ButtonType.FINISH);
            if (!a.booleanValue()) {
                JavaFXUtils.showAndWaitAlert(AlertType.ERROR,
                                             "Error while extracting ROM",
                                             null,
                                             "Error while extracting ROM. Please make sure the input ROM is valid!");
                
                try {
                    Files.walkFileTree(WORKING_PATH, new DeleteDirectoryFileVisitor());
                }
                catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            updatedRomStatus();
        }));
    }
    
    @FXML
    public void onRandomize() throws IOException {
        Path outputFile = Paths.get(outputField.getText());
        
        if (outputField.getText().isBlank() || (Files.exists(outputFile) && !Files.isRegularFile(outputFile))) {
            JavaFXUtils.showAndWaitAlert(AlertType.ERROR, "Error", null, "Output path is invalid or empty.");
            return;
        }
        
        long seed = parseSeed(seedField.getText());
        Path modFolder = Files.createTempDirectory(Paths.get("."), "rando");
        boolean isDryFire = dryFire.isSelected();
        
        Alert alert = new Alert(AlertType.NONE);
        alert.setTitle("Rebuilding ROM...");
        alert.setHeaderText(null);
        alert.setContentText("Rebuilding ROM. This might take a minute.");
        alert.show();
        
        CompletableFuture.runAsync(() -> {
            try (RandomizationContext context = new RandomizationContext(seed, raceLogging.isSelected(), WORKING_PATH, modFolder)) {
                settings.randomize(context);
                context.build();
            }
            catch (Exception e1) {
                e1.printStackTrace();
            }
        }).thenApplyAsync(a -> isDryFire || Randomizer.rebuild3DS(WORKING_PATH, outputFile, List.of(modFolder))).thenAccept(a -> Platform.runLater(() -> {
            alert.setResult(ButtonType.FINISH);
            if (!a.booleanValue())
                JavaFXUtils.showAndWaitAlert(AlertType.ERROR, "Error while rebuilding ROM", null, "Error while rebuilding ROM.");
            
            try {
                Files.walkFileTree(modFolder, new DeleteDirectoryFileVisitor());
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }));
    }
    
    private long parseSeed(String input) {
        return Utils.parseLongOrDefault(input, input.isBlank() ? new Random().nextLong() : input.hashCode());
    }
    
    @FXML
    public void onSaveSettings() throws IOException {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Where to save the settings?");
        chooser.setInitialDirectory(new File("."));
        chooser.getExtensionFilters().add(new ExtensionFilter("YAML", "*.yml"));
        
        File selected = chooser.showSaveDialog(root.getWindow());
        
        if (selected == null)
            return;
        
        Map<String, Object> configMap = new HashMap<>();
        
        configMap.put("seed", seedField.getText());
        configMap.put("settings", settings.serialize());
        configMap.put("raceLogging", raceLogging.isSelected());
        
        try (BufferedWriter writer = Files.newBufferedWriter(selected.toPath(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            Yaml.createYamlPrinter(writer).print(Yaml.createYamlDump(configMap).dump());
        }
    }
    
    @FXML
    public void onLoadSettings() throws IOException {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select the settings to load");
        chooser.setInitialDirectory(new File("."));
        chooser.getExtensionFilters().add(new ExtensionFilter("YAML", "*.yml"));
        File selected = chooser.showOpenDialog(root.getWindow());
        
        if (selected == null)
            return;
        
        try (InputStream is = Files.newInputStream(selected.toPath(), StandardOpenOption.READ)) {
            YamlMapping mapping = Yaml.createYamlInput(is).readYamlMapping();
            seedField.setText(mapping.string("seed"));
            raceLogging.setSelected(Boolean.parseBoolean(mapping.string("raceLogging")));
            settings.load(mapping.yamlMapping("settings"));
        }
    }
    
    private void updatedRomStatus() {
        boolean exists = Files.exists(WORKING_PATH);
        romFoundSymbol.setText(exists ? "✔" : "✘");
        romFoundSymbol.setTextFill(exists ? Color.GREEN : Color.RED);
        
        if (exists) {
            Alert alert = new Alert(AlertType.NONE);
            alert.setTitle("Loading Data...");
            alert.setHeaderText(null);
            alert.setContentText("Loading Data. This might take a few seconds.");
            alert.show();
            
            CompletableFuture.runAsync(() -> {
                try (Access access = new FileAccess(WORKING_PATH.resolve("part0/arcv/Keep/GlobalKeepData.res").toFile());
                        Access access2 = new FileAccess(WORKING_PATH.resolve("part0/arcv/Keep/LanguageKeep_jp.res").toFile())) {
                    this.inputData = new GlobalKeepData((AbstractKCAP) ResPayload.craft(access));
                    this.languageKeep = new LanguageKeep((AbstractKCAP) ResPayload.craft(access2));
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }).thenRun(() -> Platform.runLater(() -> {
                updateSettings();
                alert.setResult(ButtonType.FINISH);
            }));
        }
    }
}
