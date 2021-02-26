package net.digimonworld.decode.randomizer;

import javafx.event.ActionEvent;

public class AboutWindowController {
    private static final String LICENSE_URL = "";
    private static final String GITHUB_URL = "";
    private static final String DISCORD_URL = "";
    
    public void clickLicense(ActionEvent e) {
        App.getInstance().getHostServices().showDocument(LICENSE_URL);
    }

    public void clickGitHub(ActionEvent e) {
        App.getInstance().getHostServices().showDocument(GITHUB_URL);
    }
    
    public void clickDiscord(ActionEvent e) {
        App.getInstance().getHostServices().showDocument(DISCORD_URL);
    }
}
