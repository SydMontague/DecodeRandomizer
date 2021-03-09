package net.digimonworld.decode.randomizer;

import javafx.event.ActionEvent;

public class AboutWindowController {
    private static final String LICENSE_URL = "https://github.com/SydMontague/DecodeRandomizer/LICENSE";
    private static final String GITHUB_URL = "https://github.com/SydMontague/DecodeRandomizer";
    private static final String DISCORD_URL = "https://discord.gg/AeRYeGJF2P";
    private static final String THIRD_PARTY_LICENSE_URL = "https://github.com/SydMontague/DecodeRandomizer/THIRD-PARTY-NOTICE";
    
    private static final String PATREON_URL = "https://patreon.com/sydmontague";
    private static final String PAYPAL_URL = "https://paypal.me/sydmontague";
    
    public void clickLicense(ActionEvent e) {
        App.getInstance().getHostServices().showDocument(LICENSE_URL);
    }
    
    public void clickThirdPartyLicense(ActionEvent e) {
        App.getInstance().getHostServices().showDocument(THIRD_PARTY_LICENSE_URL);
    }
    
    public void clickGitHub(ActionEvent e) {
        App.getInstance().getHostServices().showDocument(GITHUB_URL);
    }
    
    public void clickDiscord(ActionEvent e) {
        App.getInstance().getHostServices().showDocument(DISCORD_URL);
    }
    
    public void clickPatreon(ActionEvent e) {
        App.getInstance().getHostServices().showDocument(PATREON_URL);
    }
    
    public void clickPayPal(ActionEvent e) {
        App.getInstance().getHostServices().showDocument(PAYPAL_URL);
    }
}
