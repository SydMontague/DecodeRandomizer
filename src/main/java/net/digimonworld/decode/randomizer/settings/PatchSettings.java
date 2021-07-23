package net.digimonworld.decode.randomizer.settings;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.amihaiemil.eoyaml.YamlMapping;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import net.digimonworld.decode.randomizer.RandoLogger.LogLevel;
import net.digimonworld.decode.randomizer.RandomizationContext;
import net.digimonworld.decode.randomizer.utils.JavaFXUtils;
import net.digimonworld.decodetools.PixelFormat;
import net.digimonworld.decodetools.data.DigimonList;
import net.digimonworld.decodetools.data.digimon.PartnerDigimon;
import net.digimonworld.decodetools.data.keepdata.Digimon;
import net.digimonworld.decodetools.data.keepdata.DigimonRaising;
import net.digimonworld.decodetools.data.keepdata.EvoRequirement.Comperator;
import net.digimonworld.decodetools.data.keepdata.EvoRequirement.Operator;
import net.digimonworld.decodetools.data.keepdata.EvoRequirement.Requirement;
import net.digimonworld.decodetools.data.keepdata.EvoRequirement.SuperGroup;
import net.digimonworld.decodetools.data.keepdata.EvoRequirement.Type;
import net.digimonworld.decodetools.data.keepdata.GlobalKeepData;
import net.digimonworld.decodetools.data.keepdata.LanguageKeep;
import net.digimonworld.decodetools.res.kcap.AbstractKCAP;
import net.digimonworld.decodetools.res.kcap.GMIPKCAP;
import net.digimonworld.decodetools.res.payload.GMIOPayload;
import net.digimonworld.decodetools.res.payload.GMIOPayload.TextureFiltering;
import net.digimonworld.decodetools.res.payload.GMIOPayload.TextureWrap;
import net.digimonworld.decodetools.res.payload.GMIOPayload.UnknownEnum;
import net.digimonworld.decodetools.res.payload.GenericPayload;

public class PatchSettings implements Setting {
    
    // TODO evo priority
    // TODO faster menus/training
    // TODO increase MP recovery rate
    // TODO remove intro cutscene
    
    private BooleanProperty patchViewDistance = new SimpleBooleanProperty();
    private BooleanProperty patchBrainsChance = new SimpleBooleanProperty();
    private DoubleProperty patchBrainsChanceFactor = new SimpleDoubleProperty();
    private BooleanProperty patchStartMPDisc = new SimpleBooleanProperty();
    private BooleanProperty patchDisable90FBattles = new SimpleBooleanProperty();
    private BooleanProperty patchMovementSpeed = new SimpleBooleanProperty();
    private BooleanProperty patchAddRecolorDigimon = new SimpleBooleanProperty();
    
    @Override
    public TitledPane create(GlobalKeepData inputData, LanguageKeep languageKeep) {
        VBox vbox = new VBox(4);
        TitledPane pane = new TitledPane("Patches", vbox);
        vbox.setAlignment(Pos.TOP_RIGHT);
        pane.setCollapsible(false);
        
        Slider brainChanceSlider = new Slider(1D, 10D, 1D);
        brainChanceSlider.setShowTickLabels(true);
        brainChanceSlider.setShowTickMarks(true);
        brainChanceSlider.setMajorTickUnit(1D);
        brainChanceSlider.setMinorTickCount(3);
        brainChanceSlider.setSnapToTicks(true);
        brainChanceSlider.setBlockIncrement(0.25D);
        brainChanceSlider.disableProperty().bind(patchBrainsChance.not());
        
        Label lbl = new Label("100%");
        lbl.textProperty().bind(brainChanceSlider.valueProperty().multiply(100D).asString("%4.0f%%"));
        lbl.setMinWidth(45);
        lbl.setAlignment(Pos.CENTER_RIGHT);
        
        patchBrainsChanceFactor.bindBidirectional(brainChanceSlider.valueProperty());
        
        vbox.getChildren()
            .addAll(JavaFXUtils.buildToggleSwitch("View Distance",
                                                  Optional.of("Increases view distance, allowing to see Digimon from a further distance."),
                                                  Optional.of(patchViewDistance)),
                    JavaFXUtils.buildToggleSwitch("Increase Movement Speed",
                                                  Optional.of("Increases running speed of player and enenmies."),
                                                  Optional.of(patchMovementSpeed)),
                    JavaFXUtils.buildToggleSwitch("Brain Learn Chance", Optional.empty(), Optional.of(patchBrainsChance)),
                    new HBox(brainChanceSlider, lbl),
                    JavaFXUtils.buildToggleSwitch("Start with MP Disc",
                                                  Optional.of("Replaces the starting Meat with MP Discs.\nStrongly recommended when playing with randomized MP costs!"),
                                                  Optional.of(patchStartMPDisc)),
                    JavaFXUtils.buildToggleSwitch("Skip 90F Battles",
                                                  Optional.of("Disabled the battles on 90F where you control your allies.\nThis is useful to prevent these fight from being unwinnable."),
                                                  Optional.of(patchDisable90FBattles)),
                    JavaFXUtils.buildToggleSwitch("Enable additional Digimon",
                                                  Optional.of("Makes 19 previously unobtainable recolor Digimon available.\nBlackAgumon, BlackGabumon, Tsukaimon, Psychemon,\nSnowAgumon, Solarmon, BlackGarurumon, Gururumon,\nYellowGrowlmon, BlackGrowlmon, IceDevimon,\nGeremon, MetalGreymon (Virus), BlackWarGrowlmon,\nOrangeWarGrowlmon, BlackWereGarurumon, BlackWarGreymon,\nBlackMetalGarurumon, ChaosDukemon"),
                                                  Optional.of(patchAddRecolorDigimon)));
        return pane;
    }
    
    @Override
    public void randomize(RandomizationContext context) {
        if (patchViewDistance.get())
            patchViewDistance(context);
        if (patchBrainsChance.get())
            patchBrainsChance(context);
        if (patchStartMPDisc.get())
            patchStartMPDisc(context);
        if (patchDisable90FBattles.get())
            disable90FBattles(context);
        if (patchMovementSpeed.get())
            patchMovementSpeed(context);
        if (patchAddRecolorDigimon.get())
            patchAddRecolorDigimon(context);
    }
    
    private void patchAddRecolorDigimon(RandomizationContext context) {
        context.logLine(LogLevel.ALWAYS, "Adding new Digimon...");
        
        addBlackAgumon(context);
        addBlackGabumon(context);
        addTsukaimon(context);
        addPsychemon(context);
        addSnowAgumon(context);
        addSolarmon(context);
        
        addBlackGarurumon(context);
        addGururumon(context);
        addOrangeGrowlmon(context);
        addBlackGrowlmon(context);
        addIceDevimon(context);
        addGeremon(context);
        
        addMetalGreymonVirus(context);
        addWarGrowlmonBlack(context);
        addWarGrowlmonOrange(context);
        addBlackWereGarurumon(context);
        
        addBlackWarGreymon(context);
        addChaosDukemon(context);
        addBlackMetalGarurumon(context);
    }
    
    private void addBlackAgumon(RandomizationContext context) {
        context.logLine(LogLevel.ALWAYS, "Adding BlackAgumon...");
        
        GlobalKeepData keep = context.getGlobalKeepData();
        final short ownId = DigimonList.BLACKAGUMON;
        final short baseId = DigimonList.AGUMON;
        
        // Digimon Data
        applyBaseData(context, ownId, baseId, (short) 100);
        
        // Raise Data
        DigimonRaising raise = context.getGlobalKeepData().getRaiseData().get(ownId - 1);
        raise.setSleepSchedule(5);
        raise.setFavoriteFood(39);
        raise.setTrainingType((byte) 0);
        raise.setLikedAreas((short) 4128);
        raise.setDislikedAreas((short) 272);
        raise.setGains(500, 1200, 50, 70, 90, 40);
        
        // Evo Requirements
        List<Requirement> requirements = new ArrayList<>();
        requirements.add(new Requirement(SuperGroup.QUOTA, (byte) 0, Operator.QUOTA, Comperator.QUOTA, Type.QUOTA, 2));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 1, Operator.AND, Comperator.HIGHEST, Type.MP_DIV10, 0));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 2, Operator.AND, Comperator.LESS_THAN, Type.WEIGHT, 12));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 3, Operator.AND, Comperator.EQUALS, Type.DIGIMON, DigimonList.KOROMON));
        
        keep.getEvoRequirements().get(ownId - 1).setRequirements(requirements);
        
        // Evo Paths
        setEvolveFrom(keep, ownId, DigimonList.KOROMON);
        setEvolveTo(keep, ownId, DigimonList.BLACK_WARGROWLMON, DigimonList.GREYMON, DigimonList.SEADRAMON, DigimonList.GROWLMON, DigimonList.WOODMON);
        
        setDescriptionString(context, ownId, "Focus on MP and low weight.\nEvolves from Koromon.");
    }
    
    private void addBlackGabumon(RandomizationContext context) {
        context.logLine(LogLevel.ALWAYS, "Adding BlackGabumon...");
        
        GlobalKeepData keep = context.getGlobalKeepData();
        final short ownId = DigimonList.BLACKGABUMON;
        final short baseId = DigimonList.GABUMON;
        
        // Digimon Data
        applyBaseData(context, ownId, baseId, (short) 101);
        
        // Raise Data
        DigimonRaising raise = context.getGlobalKeepData().getRaiseData().get(ownId - 1);
        raise.setSleepSchedule(5);
        raise.setFavoriteFood(39);
        raise.setTrainingType((byte) 1);
        raise.setLikedAreas((short) 516);
        raise.setDislikedAreas((short) 160);
        raise.setGains(800, 1000, 50, 80, 80, 40);
        
        // Evo Requirements
        List<Requirement> requirements = new ArrayList<>();
        requirements.add(new Requirement(SuperGroup.QUOTA, (byte) 0, Operator.QUOTA, Comperator.QUOTA, Type.QUOTA, 2));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 1, Operator.AND, Comperator.HIGHEST, Type.MP_DIV10, 0));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 2, Operator.AND, Comperator.GREATER_THAN, Type.WEIGHT, 14));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 3, Operator.AND, Comperator.EQUALS, Type.DIGIMON, DigimonList.TSUNOMON));
        
        keep.getEvoRequirements().get(ownId - 1).setRequirements(requirements);
        
        // Evo Paths
        setEvolveFrom(keep, ownId, DigimonList.TSUNOMON);
        setEvolveTo(keep, ownId, DigimonList.BLACKGARURUMON, DigimonList.GARURUMON, DigimonList.GAOGAMON, DigimonList.VEEDRAMON, DigimonList.LEOMON);

        setDescriptionString(context, ownId, "Focus on MP and high weight.\nEvolves from Tsunomon.");
    }
    
    private void addTsukaimon(RandomizationContext context) {
        context.logLine(LogLevel.ALWAYS, "Adding Tsukaimon...");
        
        GlobalKeepData keep = context.getGlobalKeepData();
        final short ownId = DigimonList.TSUKAIMON;
        final short baseId = DigimonList.PATAMON;
        
        // Digimon Data
        applyBaseData(context, ownId, baseId, (short) 102);
        
        // Raise Data
        DigimonRaising raise = context.getGlobalKeepData().getRaiseData().get(ownId - 1);
        raise.setSleepSchedule(7);
        raise.setFavoriteFood(45);
        raise.setTrainingType((byte) 3);
        raise.setLikedAreas((short) 160);
        raise.setDislikedAreas((short) 516);
        raise.setGains(700, 500, 80, 40, 100, 80);
        
        // Evo Requirements
        List<Requirement> requirements = new ArrayList<>();
        requirements.add(new Requirement(SuperGroup.QUOTA, (byte) 0, Operator.QUOTA, Comperator.QUOTA, Type.QUOTA, 2));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 1, Operator.AND, Comperator.HIGHEST, Type.SPEED, 0));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 2, Operator.AND, Comperator.GREATER_THAN, Type.WEIGHT, 15));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 3, Operator.AND, Comperator.EQUALS, Type.DIGIMON, DigimonList.TOKOMON));
        
        keep.getEvoRequirements().get(ownId - 1).setRequirements(requirements);
        
        // Evo Paths
        setEvolveFrom(keep, ownId, DigimonList.TOKOMON);
        setEvolveTo(keep, ownId, DigimonList.DEVIMON, DigimonList.BLACKGATOMON, DigimonList.OGREMON, DigimonList.AIRDRAMON, DigimonList.KABUTERIMON);
        
        setDescriptionString(context, ownId, "Focus on Speed and high weight.\nEvolves from Tokomon.");
    }
    
    private void addPsychemon(RandomizationContext context) {
        context.logLine(LogLevel.ALWAYS, "Adding Psychemon...");
        
        GlobalKeepData keep = context.getGlobalKeepData();
        final short ownId = DigimonList.PSYCHEMON;
        final short baseId = DigimonList.GABUMON;
        
        // Digimon Data
        applyBaseData(context, ownId, baseId, (short) 103);
        
        // Raise Data
        DigimonRaising raise = context.getGlobalKeepData().getRaiseData().get(ownId - 1);
        raise.setSleepSchedule(8);
        raise.setFavoriteFood(39);
        raise.setTrainingType((byte) 4);
        raise.setLikedAreas((short) 160);
        raise.setDislikedAreas((short) 516);
        raise.setGains(300, 500, 130, 130, 40, 40);
        
        // Evo Requirements
        List<Requirement> requirements = new ArrayList<>();
        requirements.add(new Requirement(SuperGroup.QUOTA, (byte) 0, Operator.QUOTA, Comperator.QUOTA, Type.QUOTA, 2));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 1, Operator.AND, Comperator.HIGHEST, Type.HP_DIV10, 0));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 2, Operator.AND, Comperator.GREATER_THAN, Type.WEIGHT, 13));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 3, Operator.AND, Comperator.EQUALS, Type.DIGIMON, DigimonList.WANYAMON));
        
        keep.getEvoRequirements().get(ownId - 1).setRequirements(requirements);
        
        // Evo Paths
        setEvolveFrom(keep, ownId, DigimonList.WANYAMON);
        setEvolveTo(keep, ownId, DigimonList.GURURUMON, DigimonList.KYUBIMON, DigimonList.BAKEMON, DigimonList.TOGEMON, DigimonList.KUWAGAMON);
        
        setDescriptionString(context, ownId, "Focus on HP and high weight.\nEvolves from Wanyamon.\n\nDon't trust this false prophet.");
    }
    
    private void addSnowAgumon(RandomizationContext context) {
        context.logLine(LogLevel.ALWAYS, "Adding SnowAgumon...");
        
        GlobalKeepData keep = context.getGlobalKeepData();
        final short ownId = DigimonList.SNOWAGUMON;
        final short baseId = DigimonList.AGUMON;
        
        // Digimon Data
        applyBaseData(context, ownId, baseId, (short) 104);
        
        // Raise Data
        DigimonRaising raise = keep.getRaiseData().get(ownId - 1);
        raise.setSleepSchedule(3);
        raise.setFavoriteFood(39);
        raise.setTrainingType((byte) 3);
        raise.setLikedAreas((short) 272);
        raise.setDislikedAreas((short) 640);
        raise.setGains(600, 1000, 60, 40, 110, 50);
        
        // Evo Requirements
        List<Requirement> requirements = new ArrayList<>();
        requirements.add(new Requirement(SuperGroup.QUOTA, (byte) 0, Operator.QUOTA, Comperator.QUOTA, Type.QUOTA, 2));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 1, Operator.AND, Comperator.HIGHEST, Type.DEFENSE, 0));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 2, Operator.AND, Comperator.LESS_THAN, Type.WEIGHT, 12));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 3, Operator.AND, Comperator.EQUALS, Type.DIGIMON, DigimonList.GIGIMON));
        
        keep.getEvoRequirements().get(ownId - 1).setRequirements(requirements);
        
        // Evo Paths
        setEvolveFrom(keep, ownId, DigimonList.GIGIMON);
        setEvolveTo(keep, ownId, DigimonList.ICEDEVIMON, DigimonList.IKKAKUMON, DigimonList.GURURUMON, DigimonList.EXVEEMON, DigimonList.CENTARUMON);

        setDescriptionString(context, ownId, "Focus on Defense and low weight.\nEvolves from Gigimon.");
    }
    
    private void addSolarmon(RandomizationContext context) {
        context.logLine(LogLevel.ALWAYS, "Adding Solarmon...");
        
        GlobalKeepData keep = context.getGlobalKeepData();
        final short baseId = DigimonList.HAGURUMON;
        final short ownId = DigimonList.SOLARMON;
        
        // Digimon Data
        applyBaseData(context, ownId, baseId, (short) 105);
        
        // Raise Data
        DigimonRaising raise = keep.getRaiseData().get(ownId - 1);
        raise.setSleepSchedule(4);
        raise.setFavoriteFood(50);
        raise.setTrainingType((byte) 5);
        raise.setLikedAreas((short) 1056);
        raise.setDislikedAreas((short) 24);
        raise.setGains(400, 1000, 120, 50, 55, 55);
        
        // Evo Requirements
        List<Requirement> requirements = new ArrayList<>();
        requirements.add(new Requirement(SuperGroup.QUOTA, (byte) 0, Operator.QUOTA, Comperator.QUOTA, Type.QUOTA, 2));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 1, Operator.AND, Comperator.HIGHEST, Type.OFFENSE, 0));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 2, Operator.AND, Comperator.GREATER_THAN, Type.WEIGHT, 15));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 3, Operator.AND, Comperator.EQUALS, Type.DIGIMON, DigimonList.MOTIMON));
        
        keep.getEvoRequirements().get(ownId - 1).setRequirements(requirements);
        
        // Evo Paths
        setEvolveFrom(keep, ownId, DigimonList.MOTIMON);
        setEvolveTo(keep, ownId, DigimonList.GUARDROMON, DigimonList.GROWLMON_ORANGE, DigimonList.BIRDRAMON, DigimonList.VEGIEMON, DigimonList.GEREMON);

        setDescriptionString(context, ownId, "Focus on Offense and high weight.\nEvolves from Motimon.");
    }
    
    private void addBlackGarurumon(RandomizationContext context) {
        context.logLine(LogLevel.ALWAYS, "Adding BlackGarurumon...");
        
        GlobalKeepData keep = context.getGlobalKeepData();
        final short ownId = DigimonList.BLACKGARURUMON;
        final short baseId = DigimonList.GARURUMON;
        
        // Digimon Data
        applyBaseData(context, ownId, baseId, (short) 100);
        
        // Raise Data
        DigimonRaising raise = keep.getRaiseData().get(ownId - 1);
        raise.setSleepSchedule(5);
        raise.setFavoriteFood(40);
        raise.setTrainingType((byte) 1);
        raise.setLikedAreas((short) 320);
        raise.setDislikedAreas((short) 40);
        raise.setGains(2100, 1200, 140, 200, 220, 160);
        
        // Evo Requirements
        List<Requirement> requirements = new ArrayList<>();
        requirements.add(new Requirement(SuperGroup.QUOTA, (byte) 0, Operator.QUOTA, Comperator.QUOTA, Type.QUOTA, 3));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 1, Operator.AND, Comperator.GREATER_THAN, Type.HP, 1000));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 1, Operator.AND, Comperator.GREATER_THAN, Type.SPEED, 85));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 1, Operator.AND, Comperator.GREATER_THAN, Type.DEFENSE, 60));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 2, Operator.AND, Comperator.LESS_THAN, Type.WEIGHT, 25));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 3, Operator.AND, Comperator.LESS_THAN, Type.CARE, 4));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 4, Operator.AND, Comperator.GREATER_THAN, Type.HAPPINESS, 70));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 5, Operator.AND, Comperator.GREATER_THAN, Type.TECHS, 25));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 6, Operator.AND, Comperator.GREATER_THAN, Type.DECODE_LEVEL, 10));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 7, Operator.AND, Comperator.EQUALS, Type.DIGIMON, DigimonList.BLACKGABUMON));
        
        keep.getEvoRequirements().get(ownId - 1).setRequirements(requirements);
        
        // Evo Paths
        setEvolveFrom(keep, ownId, DigimonList.BLACKGABUMON, DigimonList.GABUMON, DigimonList.SALAMON, DigimonList.RENAMON);
        setEvolveTo(keep, ownId, DigimonList.BLACKWEREGARURUMON, DigimonList.BLUEMERAMON, DigimonList.WARUMONZAEMON);

        setDescriptionString(context, ownId, "Requires HP, DEF and SPD.\nDon't get too heavy. Being happy helps.\nEvolves easier from BlackGabumon.");
    }

    private void addGururumon(RandomizationContext context) {
        context.logLine(LogLevel.ALWAYS, "Adding Gururumon...");
        
        GlobalKeepData keep = context.getGlobalKeepData();
        final short ownId = DigimonList.GURURUMON;
        final short baseId = DigimonList.GARURUMON;
        
        // Digimon Data
        applyBaseData(context, ownId, baseId, (short) 101);
        
        // Raise Data
        DigimonRaising raise = keep.getRaiseData().get(ownId - 1);
        raise.setSleepSchedule(6);
        raise.setFavoriteFood(42);
        raise.setTrainingType((byte) 5);
        raise.setLikedAreas((short) 320);
        raise.setDislikedAreas((short) 40);
        raise.setGains(2400, 1200, 140, 210, 130, 210);
        
        // Evo Requirements
        List<Requirement> requirements = new ArrayList<>();
        requirements.add(new Requirement(SuperGroup.QUOTA, (byte) 0, Operator.QUOTA, Comperator.QUOTA, Type.QUOTA, 3));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 1, Operator.AND, Comperator.GREATER_THAN, Type.HP, 900));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 1, Operator.AND, Comperator.GREATER_THAN, Type.OFFENSE, 65));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 1, Operator.AND, Comperator.GREATER_THAN, Type.DEFENSE, 110));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 1, Operator.AND, Comperator.GREATER_THAN, Type.SPEED, 65));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 2, Operator.AND, Comperator.LESS_THAN, Type.WEIGHT, 25));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 3, Operator.AND, Comperator.GREATER_THAN, Type.CARE, 2));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 4, Operator.AND, Comperator.LESS_THAN, Type.DISCIPLINE, 60));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 5, Operator.AND, Comperator.GREATER_THAN, Type.TECHS, 20));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 6, Operator.AND, Comperator.GREATER_THAN, Type.DECODE_LEVEL, 9));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 7, Operator.AND, Comperator.EQUALS, Type.DIGIMON, DigimonList.PSYCHEMON));
        
        keep.getEvoRequirements().get(ownId - 1).setRequirements(requirements);
        
        // Evo Paths
        setEvolveFrom(keep, ownId, DigimonList.PSYCHEMON, DigimonList.SNOWAGUMON, DigimonList.VEEMON, DigimonList.GAOMON);
        setEvolveTo(keep, ownId, DigimonList.WEREGARURUMON, DigimonList.ICELEOMON, DigimonList.TAOMON);

        setDescriptionString(context, ownId, "Requires high Defense. Don't get too heavy.\nDoesn't respect authority.\nEvolves easier from Psychemon.");
    }
    
    private void addOrangeGrowlmon(RandomizationContext context) {
        context.logLine(LogLevel.ALWAYS, "Adding OrangeGrowlmon...");
        
        GlobalKeepData keep = context.getGlobalKeepData();
        final short ownId = DigimonList.GROWLMON_ORANGE;
        final short baseId = DigimonList.GROWLMON;
        
        // Digimon Data
        applyBaseData(context, ownId, baseId, (short) 102);
        
        // Raise Data
        DigimonRaising raise = keep.getRaiseData().get(ownId - 1);
        raise.setSleepSchedule(5);
        raise.setFavoriteFood(40);
        raise.setTrainingType((byte) 4);
        raise.setLikedAreas((short) 4224);
        raise.setDislikedAreas((short) 272);
        raise.setGains(1800, 2000, 160, 150, 220, 140);
        
        // Evo Requirements
        List<Requirement> requirements = new ArrayList<>();
        requirements.add(new Requirement(SuperGroup.QUOTA, (byte) 0, Operator.QUOTA, Comperator.QUOTA, Type.QUOTA, 3));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 1, Operator.AND, Comperator.GREATER_THAN, Type.DEFENSE, 110));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 1, Operator.AND, Comperator.GREATER_THAN, Type.SPEED, 150));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 2, Operator.AND, Comperator.GREATER_THAN, Type.WEIGHT, 28));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 3, Operator.AND, Comperator.LESS_THAN, Type.CARE, 4));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 4, Operator.AND, Comperator.GREATER_THAN, Type.BATTLES, 10));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 5, Operator.AND, Comperator.GREATER_THAN, Type.TECHS, 18));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 6, Operator.AND, Comperator.GREATER_THAN, Type.DECODE_LEVEL, 12));
        
        keep.getEvoRequirements().get(ownId - 1).setRequirements(requirements);
        
        // Evo Paths
        setEvolveFrom(keep, ownId, DigimonList.SOLARMON, DigimonList.GOBURIMON, DigimonList.LALAMON, DigimonList.PATAMON);
        setEvolveTo(keep, ownId, DigimonList.WARGROWLMON_ORANGE, DigimonList.RIZEGREYMON, DigimonList.METALGREYMON);

        setDescriptionString(context, ownId, "Requires DEF and SPD. Likes to fight.");
    }
    
    private void addBlackGrowlmon(RandomizationContext context) {
        context.logLine(LogLevel.ALWAYS, "Adding BlackGrowlmon...");
        
        GlobalKeepData keep = context.getGlobalKeepData();
        final short ownId = DigimonList.BLACKGROWLMON;
        final short baseId = DigimonList.GROWLMON;
        
        // Digimon Data
        applyBaseData(context, ownId, baseId, (short) 103);
        
        // Raise Data
        DigimonRaising raise = keep.getRaiseData().get(ownId - 1);
        raise.setSleepSchedule(5);
        raise.setFavoriteFood(40);
        raise.setTrainingType((byte) 5);
        raise.setLikedAreas((short) 4224);
        raise.setDislikedAreas((short) 272);
        raise.setGains(2000, 1900, 210, 180, 140, 130);
        
        // Evo Requirements
        List<Requirement> requirements = new ArrayList<>();
        requirements.add(new Requirement(SuperGroup.QUOTA, (byte) 0, Operator.QUOTA, Comperator.QUOTA, Type.QUOTA, 3));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 1, Operator.AND, Comperator.GREATER_THAN, Type.HP, 800));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 1, Operator.AND, Comperator.GREATER_THAN, Type.OFFENSE, 110));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 1, Operator.AND, Comperator.GREATER_THAN, Type.DEFENSE, 110));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 2, Operator.AND, Comperator.GREATER_THAN, Type.WEIGHT, 28));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 3, Operator.AND, Comperator.LESS_THAN, Type.CARE, 5));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 4, Operator.AND, Comperator.GREATER_THAN, Type.BATTLES, 15));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 5, Operator.AND, Comperator.GREATER_THAN, Type.TECHS, 19));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 6, Operator.AND, Comperator.GREATER_THAN, Type.DECODE_LEVEL, 13));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 7, Operator.AND, Comperator.EQUALS, Type.DIGIMON, DigimonList.BLACKAGUMON));
        
        keep.getEvoRequirements().get(ownId - 1).setRequirements(requirements);
        
        // Evo Paths
        setEvolveFrom(keep, ownId, DigimonList.BLACKAGUMON, DigimonList.AGUMON, DigimonList.BETAMON, DigimonList.GUILMON);
        setEvolveTo(keep, ownId, DigimonList.BLACK_WARGROWLMON, DigimonList.METALGREYMON_VIRUS, DigimonList.MEGADRAMON);

        setDescriptionString(context, ownId, "Requires HP, Offense and Defense.\nLikes to fight.\nEvolves easier from BlackAgumon.");
    }
    
    private void addIceDevimon(RandomizationContext context) {
        context.logLine(LogLevel.ALWAYS, "Adding IceDevimon...");
        
        GlobalKeepData keep = context.getGlobalKeepData();
        final short ownId = DigimonList.ICEDEVIMON;
        final short baseId = DigimonList.DEVIMON;
        
        // Digimon Data
        applyBaseData(context, ownId, baseId, (short) 104);
        
        // Raise Data
        DigimonRaising raise = keep.getRaiseData().get(ownId - 1);
        raise.setSleepSchedule(4);
        raise.setFavoriteFood(49);
        raise.setTrainingType((byte) 4);
        raise.setLikedAreas((short) 272);
        raise.setDislikedAreas((short) 640);
        raise.setGains(2200, 1200, 150, 200, 2200, 140);
        
        // Evo Requirements
        List<Requirement> requirements = new ArrayList<>();
        requirements.add(new Requirement(SuperGroup.QUOTA, (byte) 0, Operator.QUOTA, Comperator.QUOTA, Type.QUOTA, 3));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 1, Operator.AND, Comperator.GREATER_THAN, Type.MP, 1150));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 1, Operator.AND, Comperator.GREATER_THAN, Type.SPEED, 85));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 1, Operator.AND, Comperator.GREATER_THAN, Type.BRAINS, 85));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 2, Operator.AND, Comperator.LESS_THAN, Type.WEIGHT, 30));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 3, Operator.AND, Comperator.GREATER_THAN, Type.CARE, 2));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 4, Operator.AND, Comperator.GREATER_THAN, Type.DISCIPLINE, 70));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 5, Operator.AND, Comperator.GREATER_THAN, Type.TECHS, 25));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 6, Operator.AND, Comperator.GREATER_THAN, Type.DECODE_LEVEL, 12));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 7, Operator.AND, Comperator.EQUALS, Type.DIGIMON, DigimonList.SNOWAGUMON));
        
        keep.getEvoRequirements().get(ownId - 1).setRequirements(requirements);
        
        // Evo Paths
        setEvolveFrom(keep, ownId, DigimonList.SNOWAGUMON, DigimonList.DEMIDEVIMON, DigimonList.GOMAMON, DigimonList.KAMEMON);
        setEvolveTo(keep, ownId, DigimonList.ZUDOMON, DigimonList.LADYDEVIMON, DigimonList.DIGITAMAMON);

        setDescriptionString(context, ownId, "Requires MP, Speed and Brains.\nDon't get too heavy. Being disciplined helps.\nEvolves easier from SnowAgumon.");
    }
    
    private void addGeremon(RandomizationContext context) {
        context.logLine(LogLevel.ALWAYS, "Adding Geremon...");
        
        GlobalKeepData keep = context.getGlobalKeepData();
        final short ownId = DigimonList.GEREMON;
        final short baseId = DigimonList.NUMEMON;
        
        // Digimon Data
        applyBaseData(context, ownId, baseId, (short) 105);
        
        // Raise Data
        DigimonRaising raise = keep.getRaiseData().get(ownId - 1);
        raise.setSleepSchedule(7);
        raise.setFavoriteFood(47);
        raise.setTrainingType((byte) 1);
        raise.setLikedAreas((short) 96);
        raise.setDislikedAreas((short) 2304);
        raise.setGains(1300, 1600, 180, 200, 150, 130);
        
        // Evo Requirements
        List<Requirement> requirements = new ArrayList<>();
        requirements.add(new Requirement(SuperGroup.QUOTA, (byte) 0, Operator.QUOTA, Comperator.QUOTA, Type.QUOTA, 3));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 1, Operator.AND, Comperator.GREATER_THAN, Type.HP, 900));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 1, Operator.AND, Comperator.GREATER_THAN, Type.OFFENSE, 65));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 1, Operator.AND, Comperator.GREATER_THAN, Type.DEFENSE, 110));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 1, Operator.AND, Comperator.GREATER_THAN, Type.SPEED, 65));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 2, Operator.AND, Comperator.LESS_THAN, Type.WEIGHT, 25));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 3, Operator.AND, Comperator.GREATER_THAN, Type.CARE, 2));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 4, Operator.AND, Comperator.LESS_THAN, Type.DISCIPLINE, 80));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 5, Operator.AND, Comperator.GREATER_THAN, Type.TECHS, 20));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 6, Operator.AND, Comperator.GREATER_THAN, Type.DECODE_LEVEL, 9));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 7, Operator.AND, Comperator.EQUALS, Type.DIGIMON, DigimonList.PSYCHEMON));
        
        keep.getEvoRequirements().get(ownId - 1).setRequirements(requirements);
        
        // Evo Paths
        setEvolveFrom(keep, ownId, DigimonList.SOLARMON, DigimonList.CHUUMON);
        setEvolveTo(keep, ownId, DigimonList.MONZAEMON, DigimonList.ETEMON);

        setDescriptionString(context, ownId, "Requires high Defense. Don't get too heavy.\nYou probably want to avoid this one.\nEvolves easier from Psychemon.");
    }
    
    private void addMetalGreymonVirus(RandomizationContext context) {
        context.logLine(LogLevel.ALWAYS, "Adding MetalGreymon (Virus)...");
        
        GlobalKeepData keep = context.getGlobalKeepData();
        final short ownId = DigimonList.METALGREYMON_VIRUS;
        final short baseId = DigimonList.METALGREYMON;
        
        // Digimon Data
        applyBaseData(context, ownId, baseId, (short) 100);
        
        // Raise Data
        DigimonRaising raise = keep.getRaiseData().get(ownId - 1);
        raise.setSleepSchedule(6);
        raise.setFavoriteFood(41);
        raise.setTrainingType((byte) 5);
        raise.setLikedAreas((short) 4224);
        raise.setDislikedAreas((short) 2064);
        raise.setGains(3000, 4200, 380, 340, 310, 350);
        
        // Evo Requirements
        List<Requirement> requirements = new ArrayList<>();
        requirements.add(new Requirement(SuperGroup.QUOTA, (byte) 0, Operator.QUOTA, Comperator.QUOTA, Type.QUOTA, 5));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 1, Operator.AND, Comperator.GREATER_THAN, Type.HP, 2000));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 1, Operator.AND, Comperator.GREATER_THAN, Type.MP, 3000));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 2, Operator.AND, Comperator.GREATER_THAN, Type.OFFENSE, 350));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 2, Operator.AND, Comperator.GREATER_THAN, Type.DEFENSE, 200));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 3, Operator.AND, Comperator.GREATER_THAN, Type.BRAINS, 270));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 4, Operator.AND, Comperator.GREATER_THAN, Type.WEIGHT, 45));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 5, Operator.AND, Comperator.GREATER_THAN, Type.CARE, 3));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 6, Operator.AND, Comperator.GREATER_THAN, Type.DISCIPLINE, 75));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 6, Operator.AND, Comperator.GREATER_THAN, Type.BATTLES, 25));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 7, Operator.AND, Comperator.GREATER_THAN, Type.TECHS, 32));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 8, Operator.AND, Comperator.GREATER_THAN, Type.DECODE_LEVEL, 15));
        
        keep.getEvoRequirements().get(ownId - 1).setRequirements(requirements);
        
        // Evo Paths
        setEvolveFrom(keep, ownId, DigimonList.BLACKGROWLMON, DigimonList.GREYMON);
        setEvolveTo(keep, ownId, DigimonList.BLACKWARGREYMON, DigimonList.MACHINEDRAMON);

        setDescriptionString(context, ownId, "Requires high overall stats.\nDon't save on food.\nBeing disciplined helps.");
    }
    
    private void addWarGrowlmonBlack(RandomizationContext context) {
        context.logLine(LogLevel.ALWAYS, "Adding BlackWarGrowlmon...");
        
        GlobalKeepData keep = context.getGlobalKeepData();
        final short ownId = DigimonList.BLACK_WARGROWLMON;
        final short baseId = DigimonList.WARGROWLMON;
        
        // Digimon Data
        applyBaseData(context, ownId, baseId, (short) 101);
        
        // Raise Data
        DigimonRaising raise = keep.getRaiseData().get(ownId - 1);
        raise.setSleepSchedule(5);
        raise.setFavoriteFood(41);
        raise.setTrainingType((byte) 4);
        raise.setLikedAreas((short) 4224);
        raise.setDislikedAreas((short) 272);
        raise.setGains(3000, 2800, 340, 400, 460, 320);
        
        // Evo Requirements
        List<Requirement> requirements = new ArrayList<>();
        requirements.add(new Requirement(SuperGroup.QUOTA, (byte) 0, Operator.QUOTA, Comperator.QUOTA, Type.QUOTA, 5));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 1, Operator.AND, Comperator.GREATER_THAN, Type.HP, 3000));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 2, Operator.AND, Comperator.GREATER_THAN, Type.DEFENSE, 360));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 3, Operator.AND, Comperator.GREATER_THAN, Type.SPEED, 230));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 3, Operator.AND, Comperator.GREATER_THAN, Type.BRAINS, 290));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 4, Operator.AND, Comperator.GREATER_THAN, Type.WEIGHT, 50));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 5, Operator.AND, Comperator.LESS_THAN, Type.CARE, 10));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 6, Operator.AND, Comperator.GREATER_THAN, Type.BATTLES, 30));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 7, Operator.AND, Comperator.GREATER_THAN, Type.TECHS, 28));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 8, Operator.AND, Comperator.GREATER_THAN, Type.DECODE_LEVEL, 13));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 9, Operator.AND, Comperator.EQUALS, Type.DIGIMON, DigimonList.BLACKGROWLMON));
        
        keep.getEvoRequirements().get(ownId - 1).setRequirements(requirements);
        
        // Evo Paths
        setEvolveFrom(keep, ownId, DigimonList.BLACKGROWLMON, DigimonList.GROWLMON);
        setEvolveTo(keep, ownId, DigimonList.CHAOSGALLANTMON, DigimonList.DARKDRAMON);

        setDescriptionString(context, ownId, "Requires HP, Defense and Brains.\nKeep some food around. Likes to fight.\nEvolves easier from BlackGrowlmon.");
    }
    
    private void addWarGrowlmonOrange(RandomizationContext context) {
        context.logLine(LogLevel.ALWAYS, "Adding OrangeWarGrowlmon...");
        
        GlobalKeepData keep = context.getGlobalKeepData();
        final short ownId = DigimonList.WARGROWLMON_ORANGE;
        final short baseId = DigimonList.WARGROWLMON;
        
        // Digimon Data
        applyBaseData(context, ownId, baseId, (short) 102);
        
        // Raise Data
        DigimonRaising raise = keep.getRaiseData().get(ownId - 1);
        raise.setSleepSchedule(5);
        raise.setFavoriteFood(41);
        raise.setTrainingType((byte) 1);
        raise.setLikedAreas((short) 4224);
        raise.setDislikedAreas((short) 272);
        raise.setGains(4000, 2500, 250, 410, 310, 480);
        
        // Evo Requirements
        List<Requirement> requirements = new ArrayList<>();
        requirements.add(new Requirement(SuperGroup.QUOTA, (byte) 0, Operator.QUOTA, Comperator.QUOTA, Type.QUOTA, 5));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 1, Operator.AND, Comperator.GREATER_THAN, Type.HP, 2700));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 2, Operator.AND, Comperator.GREATER_THAN, Type.OFFENSE, 210));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 2, Operator.AND, Comperator.GREATER_THAN, Type.DEFENSE, 360));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 3, Operator.AND, Comperator.GREATER_THAN, Type.BRAINS, 370));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 4, Operator.AND, Comperator.GREATER_THAN, Type.WEIGHT, 45));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 5, Operator.AND, Comperator.LESS_THAN, Type.CARE, 10));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 6, Operator.AND, Comperator.GREATER_THAN, Type.BATTLES, 30));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 7, Operator.AND, Comperator.GREATER_THAN, Type.TECHS, 30));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 8, Operator.AND, Comperator.GREATER_THAN, Type.DECODE_LEVEL, 13));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 9, Operator.AND, Comperator.EQUALS, Type.DIGIMON, DigimonList.GROWLMON_ORANGE));
        
        keep.getEvoRequirements().get(ownId - 1).setRequirements(requirements);
        
        // Evo Paths
        setEvolveFrom(keep, ownId, DigimonList.GROWLMON_ORANGE, DigimonList.TYRANNOMON);
        setEvolveTo(keep, ownId, DigimonList.RUSTTYRANNOMON, DigimonList.PRINCEMAMEMON);

        setDescriptionString(context, ownId, "Requires HP, Defense and Brains.\nKeep some food around. Likes to fight.\nEvolves easier from OrangeGrowlmon.");
    }
    
    private void addBlackWereGarurumon(RandomizationContext context) {
        context.logLine(LogLevel.ALWAYS, "Adding BlackWereGarurumon...");
        
        GlobalKeepData keep = context.getGlobalKeepData();
        final short ownId = DigimonList.BLACKWEREGARURUMON;
        final short baseId = DigimonList.WEREGARURUMON;
        
        // Digimon Data
        applyBaseData(context, ownId, baseId, (short) 103);
        
        // Raise Data
        DigimonRaising raise = keep.getRaiseData().get(ownId - 1);
        raise.setSleepSchedule(5);
        raise.setFavoriteFood(53);
        raise.setTrainingType((byte) 1);
        raise.setLikedAreas((short) 576);
        raise.setDislikedAreas((short) 4112);
        raise.setGains(3200, 2600, 400, 330, 440, 350);
        
        // Evo Requirements
        List<Requirement> requirements = new ArrayList<>();
        requirements.add(new Requirement(SuperGroup.QUOTA, (byte) 0, Operator.QUOTA, Comperator.QUOTA, Type.QUOTA, 5));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 1, Operator.AND, Comperator.GREATER_THAN, Type.HP, 2000));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 1, Operator.AND, Comperator.GREATER_THAN, Type.MP, 2000));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 2, Operator.AND, Comperator.GREATER_THAN, Type.OFFENSE, 310));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 3, Operator.AND, Comperator.GREATER_THAN, Type.SPEED, 350));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 4, Operator.AND, Comperator.LESS_THAN, Type.WEIGHT, 40));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 5, Operator.AND, Comperator.LESS_THAN, Type.CARE, 15));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 6, Operator.AND, Comperator.GREATER_THAN, Type.HAPPINESS, 80));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 6, Operator.AND, Comperator.GREATER_THAN, Type.BATTLES, 25));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 7, Operator.AND, Comperator.GREATER_THAN, Type.TECHS, 35));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 8, Operator.AND, Comperator.GREATER_THAN, Type.DECODE_LEVEL, 14));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 9, Operator.AND, Comperator.EQUALS, Type.DIGIMON, DigimonList.BLACKGARURUMON));
        
        keep.getEvoRequirements().get(ownId - 1).setRequirements(requirements);
        
        // Evo Paths
        setEvolveFrom(keep, ownId, DigimonList.BLACKGARURUMON, DigimonList.GARURUMON, DigimonList.BLACKGATOMON);
        setEvolveTo(keep, ownId, DigimonList.BLACKMETALGARURUMON, DigimonList.PIEDMON);

        setDescriptionString(context, ownId, "Strong, fast and nimble fighter.\nEvolves easier from BlackGarurumon.");
    }
    
    private void addBlackWarGreymon(RandomizationContext context) {
        context.logLine(LogLevel.ALWAYS, "Adding BlackWarGreymon...");
        
        GlobalKeepData keep = context.getGlobalKeepData();
        final short ownId = DigimonList.BLACKWARGREYMON;
        final short baseId = DigimonList.WARGREYMON;
        
        // Digimon Data
        applyBaseData(context, ownId, baseId, (short) 100);
        
        // Raise Data
        DigimonRaising raise = keep.getRaiseData().get(ownId - 1);
        raise.setSleepSchedule(5);
        raise.setFavoriteFood(57);
        raise.setTrainingType((byte) 4);
        raise.setLikedAreas((short) 40);
        raise.setDislikedAreas((short) 1152);
        raise.setGains(5800, 6700, 770, 460, 660, 870);
        
        // Evo Requirements
        List<Requirement> requirements = new ArrayList<>();
        requirements.add(new Requirement(SuperGroup.QUOTA, (byte) 0, Operator.QUOTA, Comperator.QUOTA, Type.QUOTA, 6));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 1, Operator.AND, Comperator.GREATER_THAN, Type.HP, 5500));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 2, Operator.AND, Comperator.GREATER_THAN, Type.OFFENSE, 650));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 3, Operator.AND, Comperator.GREATER_THAN, Type.DEFENSE, 550));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 4, Operator.AND, Comperator.GREATER_THAN, Type.BRAINS, 420));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 5, Operator.AND, Comperator.GREATER_THAN, Type.WEIGHT, 55));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 6, Operator.AND, Comperator.LESS_THAN, Type.CARE, 10));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 7, Operator.AND, Comperator.GREATER_THAN, Type.DISCIPLINE, 80));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 8, Operator.AND, Comperator.GREATER_THAN, Type.TECHS, 40));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 9, Operator.AND, Comperator.GREATER_THAN, Type.DECODE_LEVEL, 16));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 10, Operator.AND, Comperator.EQUALS, Type.DIGIMON, DigimonList.METALGREYMON_VIRUS));
        
        keep.getEvoRequirements().get(ownId - 1).setRequirements(requirements);
        
        // Evo Paths
        setEvolveFrom(keep, ownId, DigimonList.METALGREYMON_VIRUS, DigimonList.METALGREYMON);
        
        setDescriptionString(context, ownId, "A perfect balance of offensive.\nand defense capabilities.\nRequires disciplined training and good food.\nEvolves from MetalGreymon.");
    }
    
    private void addChaosDukemon(RandomizationContext context) {
        context.logLine(LogLevel.ALWAYS, "Adding ChaosDukemon...");
        
        GlobalKeepData keep = context.getGlobalKeepData();
        final short ownId = DigimonList.CHAOSGALLANTMON;
        final short baseId = DigimonList.GALLANTMON;
        
        // Digimon Data
        applyBaseData(context, ownId, baseId, (short) 101);
        
        // Raise Data
        DigimonRaising raise = keep.getRaiseData().get(ownId - 1);
        raise.setSleepSchedule(4);
        raise.setFavoriteFood(60);
        raise.setTrainingType((byte) 0);
        raise.setLikedAreas((short) 2056);
        raise.setDislikedAreas((short) 1056);
        raise.setGains(6300, 6500, 750, 600, 600, 620);
        
        // Evo Requirements
        List<Requirement> requirements = new ArrayList<>();
        requirements.add(new Requirement(SuperGroup.QUOTA, (byte) 0, Operator.QUOTA, Comperator.QUOTA, Type.QUOTA, 8));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 1, Operator.AND, Comperator.GREATER_THAN, Type.HP, 4000));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 2, Operator.AND, Comperator.GREATER_THAN, Type.MP, 3000));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 3, Operator.AND, Comperator.GREATER_THAN, Type.OFFENSE, 600));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 4, Operator.AND, Comperator.GREATER_THAN, Type.DEFENSE, 600));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 5, Operator.AND, Comperator.GREATER_THAN, Type.SPEED, 700));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 6, Operator.AND, Comperator.GREATER_THAN, Type.BRAINS, 500));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 7, Operator.AND, Comperator.GREATER_THAN, Type.WEIGHT, 60));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 8, Operator.AND, Comperator.LESS_THAN, Type.CARE, 10));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 9, Operator.AND, Comperator.GREATER_THAN, Type.HAPPINESS, 80));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 10, Operator.AND, Comperator.GREATER_THAN, Type.TECHS, 42));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 11, Operator.AND, Comperator.GREATER_THAN, Type.DECODE_LEVEL, 17));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 12, Operator.AND, Comperator.EQUALS, Type.DIGIMON, DigimonList.BLACK_WARGROWLMON));
        
        keep.getEvoRequirements().get(ownId - 1).setRequirements(requirements);
        
        // Evo Paths
        setEvolveFrom(keep, ownId, DigimonList.BLACK_WARGROWLMON, DigimonList.WARGROWLMON);

        setDescriptionString(context, ownId, "Requires high overall stats. Likes good food.\nEvolves easier from BlackWarGrowlmon.");
    }
    
    private void addBlackMetalGarurumon(RandomizationContext context) {
        context.logLine(LogLevel.ALWAYS, "Adding BlackMetalGarurumon...");
        
        GlobalKeepData keep = context.getGlobalKeepData();
        final short ownId = DigimonList.BLACKMETALGARURUMON;
        final short baseId = DigimonList.METALGARURUMON;
        
        // Digimon Data
        applyBaseData(context, ownId, baseId, (short) 102);
        
        // Raise Data
        DigimonRaising raise = keep.getRaiseData().get(ownId - 1);
        raise.setSleepSchedule(4);
        raise.setFavoriteFood(59);
        raise.setTrainingType((byte) 0);
        raise.setLikedAreas((short) 320);
        raise.setDislikedAreas((short) 136);
        raise.setGains(9300, 4700, 520, 690, 750, 450);
        
        // Evo Requirements
        List<Requirement> requirements = new ArrayList<>();
        requirements.add(new Requirement(SuperGroup.QUOTA, (byte) 0, Operator.QUOTA, Comperator.QUOTA, Type.QUOTA, 7));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 1, Operator.AND, Comperator.GREATER_THAN, Type.MP, 4500));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 2, Operator.AND, Comperator.GREATER_THAN, Type.OFFENSE, 500));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 3, Operator.AND, Comperator.GREATER_THAN, Type.DEFENSE, 700));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 4, Operator.AND, Comperator.GREATER_THAN, Type.SPEED, 700));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 5, Operator.AND, Comperator.GREATER_THAN, Type.BRAINS, 400));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 6, Operator.AND, Comperator.LESS_THAN, Type.WEIGHT, 50));
        requirements.add(new Requirement(SuperGroup.NORMAL, (byte) 7, Operator.AND, Comperator.LESS_THAN, Type.CARE, 15));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 8, Operator.AND, Comperator.GREATER_THAN, Type.HAPPINESS, 80));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 9, Operator.AND, Comperator.GREATER_THAN, Type.DISCIPLINE, 80));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 10, Operator.AND, Comperator.GREATER_THAN, Type.TECHS, 44));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 11, Operator.AND, Comperator.GREATER_THAN, Type.DECODE_LEVEL, 17));
        requirements.add(new Requirement(SuperGroup.BONUS, (byte) 12, Operator.AND, Comperator.EQUALS, Type.DIGIMON, DigimonList.BLACKWEREGARURUMON));
        
        keep.getEvoRequirements().get(ownId - 1).setRequirements(requirements);
        
        // Evo Paths
        setEvolveFrom(keep, ownId, DigimonList.BLACKWEREGARURUMON, DigimonList.WEREGARURUMON);

        setDescriptionString(context, ownId, "Very fast and defensive.\nKeep a disciplined diet, but don't get depressed.\nEvolves easier from BlackWereGarurumon.");
    }
    
    private void setDescriptionString(RandomizationContext context, short ownId, String string) {
        context.getLanguageKeep().getDigimonDescription().getEntryById(ownId).ifPresent(a -> a.setString(string));
    }
    
    @SuppressWarnings("unused")
    private static void registerSprite(RandomizationContext context, short id, BufferedImage image) {
        Optional<GMIPKCAP> images = context.getFile("part0/arcv/Keep/GlobalKeepRes.res").map(AbstractKCAP.class::cast).map(a -> (GMIPKCAP) a.get(8));
        
        if(images.isEmpty())
            return;
        
        GMIOPayload gmio = new GMIOPayload(null);
        gmio.setImage(image);
        gmio.setFormat(PixelFormat.RGB5551);
        gmio.setMinFilter(TextureFiltering.LINEAR);
        gmio.setMagFilter(TextureFiltering.LINEAR);
        gmio.setUnknown(UnknownEnum.NORMAL);
        gmio.setUVHeightAbsolute(16);
        gmio.setUVWidthAbsolute(68);
        gmio.setWrapS(TextureWrap.CLAMP_TO_EDGE);
        gmio.setWrapT(TextureWrap.CLAMP_TO_EDGE);
        
        images.get().add(gmio);
        
        for(int offset : SPRITE_TABLES) {
            context.addASM(String.format(".org 0x%X", offset + id * 2));
            context.addASM(String.format(".halfword %d", 0x145 + images.get().getEntryCount() - 1));
        }
    }
    
    private static final int[] SPRITE_TABLES = { 0x4A4330, 0x4A4E00, 0x4A527C, 0x4A6E00, 0x4A9AB0, 0x4AA564, 0x4AAFBC, 0x4AB678, 0x4ABD40, 0x4AC510, 0x4ACA90,
            0x4AE12C, 0x4AE828, 0x4AF108, 0x4AFE08 };
    
    private static void applyBaseData(RandomizationContext context, short self, short base, short evoListPos) {
        Digimon baseData = context.getGlobalKeepData().getDigimonData().get(base - 1);
        Digimon data = context.getGlobalKeepData().getDigimonData().get(self - 1);
        
        data.setEvoListPos(evoListPos);
        data.setInitialY(baseData.getInitialY());
        data.setInitialZ(baseData.getInitialZ());
        data.setMinY(baseData.getMinY());
        data.setMinZ(baseData.getMinZ());
        data.setMaxY(baseData.getMaxY());
        data.setMaxZ(baseData.getMaxZ());
        data.setInitialRotation(baseData.getInitialRotation());
        data.setDigiviceScale(baseData.getDigiviceScale());
        data.setUnk7(baseData.getUnk7());
        data.setUnk8(baseData.getUnk8());
        data.setUnk11(baseData.getUnk11());
        data.setUnk38((short) (data.getUnk38() | 65));

        Optional<PartnerDigimon> baseDigi = context.getDigimon(base, true);
        Optional<PartnerDigimon> modDigi = context.getDigimon(self);
        
        if(baseDigi.isEmpty() || modDigi.isEmpty())
            return;
        
        if(modDigi.get().getAnim5().isEmpty())
            modDigi.ifPresent(b -> b.setAnim5(baseDigi.map(a -> a.getAnim5().orElse(null))));
        
        if(modDigi.get().getAccessoryData().isEmpty())
            modDigi.ifPresent(b -> b.setAccessoryData(baseDigi.map(PartnerDigimon::getAccessoryData).orElse(Collections.emptyList())));
    }
    
    private static void setEvolveFrom(GlobalKeepData keep, short self, short... sources) {
        DigimonRaising data = keep.getRaiseData().get(self - 1);
        
        for (short source : sources) {
            data.addEvolveFrom(source);
            keep.getRaiseData().get(source - 1).addEvolveTo(self);
        }
    }
    
    private static void setEvolveTo(GlobalKeepData keep, short self, short... targets) {
        DigimonRaising data = keep.getRaiseData().get(self - 1);
        
        for (short target : targets) {
            data.addEvolveTo(target);
            keep.getRaiseData().get(target - 1).addEvolveFrom(self);
        }
    }
    
    private void patchMovementSpeed(RandomizationContext context) {
        context.logLine(LogLevel.ALWAYS, "Patching running speed...");
        
        float newRunSpeed = 50f * 1.5f;
        float newWalkSpeed = 21f;
        float newEnemySpeed = 57.5f * 1.5f;
        
        context.addASM(".org 0x44B8B8");
        context.addASM(String.format(Locale.US, ".float %5.3f", newRunSpeed));
        context.addASM(String.format(Locale.US, ".float %5.3f", newWalkSpeed));
        
        context.addASM(".org 0x2FDCE8");
        context.addASM(String.format(Locale.US, ".float %5.3f", newEnemySpeed));
    }
    
    private void disable90FBattles(RandomizationContext context) {
        context.logLine(LogLevel.ALWAYS, "Patching out 90F battle...");
        
        context.getFile("part0/arcv/map/are90.res").ifPresent(a -> {
            byte[] data = ((GenericPayload) ((AbstractKCAP) a).get(0)).getData();
            data[0x30D8] = 0x7D; // JZ -> J
            data[0x30D9] = 0x60; // 101 -> 96
        });
    }
    
    private void patchStartMPDisc(RandomizationContext context) {
        context.logLine(LogLevel.ALWAYS, "Patching starting MP Disc...");
        
        context.addASM(".org 0x27AD7C");
        context.addASM("MOV R0, #5");
    }
    
    private void patchBrainsChance(RandomizationContext context) {
        context.logLine(LogLevel.ALWAYS, "Patching Brains learning chance...");
        
        context.addASM(".org 0x14B820");
        context.addASM(String.format(Locale.US, ".float %5.3f", 0.002 * Math.max(1D, patchBrainsChanceFactor.get())));
    }
    
    private void patchViewDistance(RandomizationContext context) {
        context.logLine(LogLevel.ALWAYS, "Patching view distance...");
        
        context.addASM(".org 0x4E5118");
        context.addASM(".float 1000.0");
        context.addASM(".float 1000.0");
        context.addASM(".org 0x44CC48");
        context.addASM(".float 1000.0");
        context.addASM(".float 1000.0");
        context.addASM(".org 0x1F9F74");
        context.addASM(".float 1000.0");
        context.addASM(".float 1000.0");
        context.addASM(".org 0x2C56B8");
        context.addASM("NOP");
        context.addASM(".org 0x2C56C4");
        context.addASM("NOP");
    }
    
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        
        map.put("patchViewDistance", patchViewDistance.get());
        map.put("patchBrainsChance", patchBrainsChance.get());
        map.put("patchBrainsChanceFactor", patchBrainsChanceFactor.get());
        map.put("patchStartMPDisc", patchStartMPDisc.get());
        map.put("patchDisable90FBattles", patchDisable90FBattles.get());
        map.put("patchMovementSpeed", patchMovementSpeed.get());
        map.put("patchAddRecolorDigimon", patchAddRecolorDigimon.get());
        
        return map;
    }
    
    @Override
    public void load(YamlMapping map) {
        if (map == null)
            return;
        
        this.patchViewDistance.set(Boolean.parseBoolean(map.string("patchViewDistance")));
        this.patchBrainsChance.set(Boolean.parseBoolean(map.string("patchBrainsChance")));
        this.patchBrainsChanceFactor.set(map.doubleNumber("patchBrainsChanceFactor"));
        this.patchStartMPDisc.set(Boolean.parseBoolean(map.string("patchStartMPDisc")));
        this.patchDisable90FBattles.set(Boolean.parseBoolean(map.string("patchDisable90FBattles")));
        this.patchMovementSpeed.set(Boolean.parseBoolean(map.string("patchMovementSpeed")));
        this.patchAddRecolorDigimon.set(Boolean.parseBoolean(map.string("patchAddRecolorDigimon")));
    }
    
}
