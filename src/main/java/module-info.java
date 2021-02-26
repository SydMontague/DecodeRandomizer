module net.digimonworld.decode.decode_randomizer {
    requires transitive javafx.graphics;
    requires transitive javafx.controls;
    requires javafx.fxml;
    requires javafx.base;
    requires org.controlsfx.controls;
    requires transitive net.digimonworld.decode.decode_tools;
    requires java.logging;
    requires com.amihaiemil.eoyaml;
    
    opens net.digimonworld.decode.randomizer to javafx.fxml;
    exports net.digimonworld.decode.randomizer;
}