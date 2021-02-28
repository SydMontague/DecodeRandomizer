package net.digimonworld.decode.randomizer.utils;

import java.util.Optional;

import org.controlsfx.control.ToggleSwitch;

import javafx.beans.property.BooleanProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Tooltip;

public class JavaFXUtils {
    private JavaFXUtils() {
    }
    
    public static Optional<ButtonType> showAndWaitAlert(AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        return alert.showAndWait();
    }
    
    public static ToggleSwitch buildToggleSwitch(String name, Optional<String> tooltip, Optional<BooleanProperty> bindingProperty) {
        ToggleSwitch toggleSwitch = new ToggleSwitch(name);
        bindingProperty.ifPresent(a -> toggleSwitch.selectedProperty().bindBidirectional(a));
        tooltip.ifPresent(a -> toggleSwitch.setTooltip(new Tooltip(a)));
        
        return toggleSwitch;
    }
}
