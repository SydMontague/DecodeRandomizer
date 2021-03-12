package net.digimonworld.decode.randomizer;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;

/*
 * Dependencies:
 * - 3dstex | No License, Download in runtime | https://github.com/Cruel/3dstex/releases
 * - 3dstool | MIT, bundle | https://github.com/dnasdw/3dstool
 * - armips | MIT, bundle | https://github.com/Kingcom/armips
 * - ctrtool | MIT, bundle | https://github.com/3DSGuy/Project_CTR/tree/master/ctrtool
 * - makerom | MIT, bundle | https://github.com/3DSGuy/Project_CTR/tree/master/makerom
 * - xdelta | Apache 2.0, bundle | https://github.com/jmacd/xdelta
 */

/* TODO features
 * item spawns
 * 
 * patches
 *  - camera distance
 *  - priority randomization (rookies)
 */

public class DecodeRandomizer extends Application {
    private static DecodeRandomizer instance;
    
    public DecodeRandomizer() {
        synchronized (DecodeRandomizer.class) {
            if (instance == null)
                instance = this;
            else
                throw new UnsupportedOperationException("Tried to instantiate the App's main class more than once.");
        }
    }
    
    public static DecodeRandomizer getInstance() {
        return instance;
    }
    
    @Override
    public void start(Stage stage) throws IOException {
        Alert alert = new Alert(AlertType.NONE);
        alert.setTitle("Loading data, please wait...");
        alert.setHeaderText(null);
        alert.setContentText(null);
        alert.show();
        
        FXMLLoader loader = new FXMLLoader(DecodeRandomizer.class.getResource("MainWindow.fxml"));
        stage.setScene(loader.load());
        stage.setTitle("Digimon World Re:Digitize Decode Randomizer");
        stage.show();
        
        alert.setResult(ButtonType.FINISH);
    }
    
    public static void main(String[] args) {
        Application.launch(args);
    }
    
}
