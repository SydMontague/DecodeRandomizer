package net.digimonworld.decode.randomizer;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
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
 * enemy spawns
 *  - type
 *  - stats
 *  - drops
 *  - moves
 *  - bits?
 *  - decode XP?
 *  - accessory??
 * treasure hunt loot?
 * shops/vending machines?
 * 
 * patches
 *  - camera distance
 *  - priority randomization (rookies)
 *  - player character
 */

public class App extends Application {
    private static App instance;
    
    public App() {
        synchronized (App.class) {
            if(instance == null)
                instance = this;
            else
                throw new UnsupportedOperationException("Tried to instantiate the App's main class more than once.");
        }
    }
    
    public static App getInstance() {
        return instance;
    }
    
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("MainWindow.fxml"));
        stage.setScene(loader.load());
        stage.setTitle("Digimon World Re:Digitize Decode Randomizer v0.0.1");
        stage.show();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }

}