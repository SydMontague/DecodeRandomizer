package net.digimonworld.decode.randomizer.settings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import com.amihaiemil.eoyaml.YamlMapping;

import de.phoenixstaffel.decodetools.keepdata.Digimon;
import de.phoenixstaffel.decodetools.keepdata.DigimonRaising;
import de.phoenixstaffel.decodetools.keepdata.GlobalKeepData;
import de.phoenixstaffel.decodetools.keepdata.Item;
import de.phoenixstaffel.decodetools.keepdata.Item.ItemType;
import de.phoenixstaffel.decodetools.keepdata.LanguageKeep;
import de.phoenixstaffel.decodetools.keepdata.enums.Attribute;
import de.phoenixstaffel.decodetools.keepdata.enums.Level;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import net.digimonworld.decode.randomizer.RandoLogger.LogLevel;
import net.digimonworld.decode.randomizer.RandomizationContext;
import net.digimonworld.decode.randomizer.utils.JavaFXUtils;

public class DigimonSettings implements Setting {
    private static final Attribute[] ATTRIBUTE_LIST = new Attribute[] { Attribute.DATA, Attribute.VACCINE, Attribute.VIRUS };
    
    BooleanProperty randomizeAttribute = new SimpleBooleanProperty();
    BooleanProperty randomizeCombatSpeed = new SimpleBooleanProperty();
    BooleanProperty randomizeSleepSchedule = new SimpleBooleanProperty();
    BooleanProperty randomizeFavoriteFood = new SimpleBooleanProperty();
    BooleanProperty randomizeTrainingType = new SimpleBooleanProperty();
    BooleanProperty randomizeBaseWeight = new SimpleBooleanProperty();
    BooleanProperty randomizeScale = new SimpleBooleanProperty();
    
    @Override
    public TitledPane create(GlobalKeepData data, LanguageKeep language) {
        VBox vbox = new VBox(4);
        TitledPane pane = new TitledPane("Digimon", vbox);
        vbox.setAlignment(Pos.TOP_RIGHT);
        pane.setCollapsible(false);
        
        vbox.getChildren().addAll(JavaFXUtils.buildToggleSwitch("Attribute", Optional.empty(), Optional.of(randomizeAttribute)),
                                  JavaFXUtils.buildToggleSwitch("Combat Speed", Optional.empty(), Optional.of(randomizeCombatSpeed)),
                                  JavaFXUtils.buildToggleSwitch("Sleep Schedule", Optional.empty(), Optional.of(randomizeSleepSchedule)),
                                  JavaFXUtils.buildToggleSwitch("Favorite Food", Optional.empty(), Optional.of(randomizeFavoriteFood)),
                                  JavaFXUtils.buildToggleSwitch("Training Type", Optional.empty(), Optional.of(randomizeTrainingType)),
                                  JavaFXUtils.buildToggleSwitch("Base Weight", Optional.empty(), Optional.of(randomizeBaseWeight)),
                                  JavaFXUtils.buildToggleSwitch("Scale",
                                                                Optional.of("Randomize the size of Digimon between 0.5 and 2.0 their vanilla size."),
                                                                Optional.of(randomizeScale)));
        
        return pane;
    }
    
    @Override
    public void randomize(RandomizationContext context) {
        if (randomizeAttribute.get())
            randomizeAttributes(context);
        if (randomizeCombatSpeed.get())
            randomizeCombatSpeed(context);
        if (randomizeSleepSchedule.get())
            randomizeSleepSchedule(context);
        if (randomizeFavoriteFood.get())
            randomizeFavoriteFood(context);
        if (randomizeTrainingType.get())
            randomizeTrainingType(context);
        if (randomizeBaseWeight.get())
            randomizeBaseWeight(context);
        if (randomizeScale.get())
            randomizeScale(context);
    }
    
    private void randomizeScale(RandomizationContext context) {
        Random rand = new Random(context.getInitialSeed() * "Scale".hashCode());
        
        context.logLine(LogLevel.ALWAYS, "Randomizing Digimon Scale...");
        
        for (ListIterator<Digimon> itr = context.getGlobalKeepData().getDigimonData().listIterator(); itr.hasNext();) {      
            int i = itr.nextIndex();
            Digimon a = itr.next();
            
            float val = rand.nextFloat();
            if(val > 0.5f)
                val = 1f + 2f * (val - 0.5f);
            else
                val = 1f + (val - 0.5f);
            
            float newScale = a.getScale() * val;
            context.logLine(LogLevel.CASUAL,
                            String.format(FORMAT_FLOAT,
                                          context.getLanguageKeep().getDigimonNames().getStringById(i + 1),
                                          "Scale",
                                          a.getScale(),
                                          newScale));
            a.setScale(newScale);
            a.setDigiviceScale(a.getDigiviceScale() / val);
        }
    }

    private void randomizeBaseWeight(RandomizationContext context) {
        Random rand = new Random(context.getInitialSeed() * "BaseWeight".hashCode());
        
        context.logLine(LogLevel.ALWAYS, "Randomizing Digimon Base Weight...");
        
        for (ListIterator<DigimonRaising> itr = context.getGlobalKeepData().getRaiseData().listIterator(); itr.hasNext();) {
            int i = itr.nextIndex();
            DigimonRaising a = itr.next();
            Digimon digimon = context.getGlobalKeepData().getDigimonData().get(i);
            
            if (digimon.getLevel() == Level.NONE || digimon.getLevel() == Level.UNUSED || a.getBaseWeight() == 0)
                continue;
            
            int level = Math.max(digimon.getLevel().ordinal() - 1, 1) + (a.getBaseWeight() == 100 ? 1 : 0);
            int newWeight = 5 * level + rand.nextInt(5 * level + 10 * (level - 1));
            
            context.logLine(LogLevel.CASUAL,
                            String.format(FORMAT_INT,
                                          context.getLanguageKeep().getDigimonNames().getStringById(i + 1),
                                          "Base Weight",
                                          a.getBaseWeight(),
                                          newWeight));
            a.setBaseWeight((short) newWeight);
        }
        
        context.logLine(LogLevel.ALWAYS, "");
    }
    
    private void randomizeTrainingType(RandomizationContext context) {
        Random rand = new Random(context.getInitialSeed() * "TrainingType".hashCode());
        
        context.logLine(LogLevel.ALWAYS, "Randomizing Digimon Training Types...");
        
        for (ListIterator<DigimonRaising> itr = context.getGlobalKeepData().getRaiseData().listIterator(); itr.hasNext();) {
            int i = itr.nextIndex();
            DigimonRaising a = itr.next();
            
            if (a.getTrainingType() > 5)
                continue;
            
            int newType = rand.nextInt(6);
            context.logLine(LogLevel.CASUAL,
                            String.format(FORMAT_INT,
                                          context.getLanguageKeep().getDigimonNames().getStringById(i + 1),
                                          "Training Type",
                                          a.getTrainingType(),
                                          newType));
            a.setTrainingType((byte) newType);
        }
        
        context.logLine(LogLevel.ALWAYS, "");
    }
    
    private void randomizeFavoriteFood(RandomizationContext context) {
        Random rand = new Random(context.getInitialSeed() * "FavoriteFood".hashCode());
        
        context.logLine(LogLevel.ALWAYS, "Randomizing Digimon Favorite Food...");
        
        List<Integer> foodItems = new ArrayList<>();
        for (ListIterator<Item> itr = context.getGlobalKeepData().getItems().listIterator(); itr.hasNext();) {
            int id = itr.nextIndex() + 1;
            Item item = itr.next();
            
            if (item.getType() == ItemType.FOOD)
                foodItems.add(id);
        }
        
        for (ListIterator<DigimonRaising> itr = context.getGlobalKeepData().getRaiseData().listIterator(); itr.hasNext();) {
            int i = itr.nextIndex();
            DigimonRaising a = itr.next();
            
            if (a.getFavoriteFood() == 0)
                continue;
            
            int newFavFood = foodItems.get(rand.nextInt(foodItems.size()));
            context.logLine(LogLevel.CASUAL,
                            String.format(FORMAT_STRING,
                                          context.getLanguageKeep().getDigimonNames().getStringById(i + 1),
                                          "Favorite Food",
                                          context.getLanguageKeep().getItemNames().getStringById(a.getFavoriteFood()),
                                          context.getLanguageKeep().getItemNames().getStringById(newFavFood)));
            a.setFavoriteFood(newFavFood);
        }
        
        context.logLine(LogLevel.ALWAYS, "");
    }
    
    private void randomizeSleepSchedule(RandomizationContext context) {
        Random rand = new Random(context.getInitialSeed() * "SleepSchedule".hashCode());
        
        context.logLine(LogLevel.ALWAYS, "Randomizing Digimon Sleep Schedules...");
        
        for (ListIterator<DigimonRaising> itr = context.getGlobalKeepData().getRaiseData().listIterator(); itr.hasNext();) {
            int i = itr.nextIndex();
            DigimonRaising a = itr.next();
            
            if (a.getSleepSchedule() < 2)
                continue;
            
            int newSchedule = 3 + rand.nextInt(6);
            context.logLine(LogLevel.CASUAL,
                            String.format(FORMAT_INT,
                                          context.getLanguageKeep().getDigimonNames().getStringById(i + 1),
                                          "Sleep Schedule",
                                          a.getSleepSchedule(),
                                          newSchedule));
            a.setSleepSchedule(newSchedule);
        }
        
        context.logLine(LogLevel.ALWAYS, "");
    }
    
    private void randomizeCombatSpeed(RandomizationContext context) {
        Random rand = new Random(context.getInitialSeed() * "CombatSpeed".hashCode());
        
        context.logLine(LogLevel.ALWAYS, "Randomizing Digimon Combat Speed...");
        
        for (ListIterator<Digimon> itr = context.getGlobalKeepData().getDigimonData().listIterator(); itr.hasNext();) {
            int i = itr.nextIndex();
            Digimon a = itr.next();
            
            float newSpeed = 3.5f + 5f * rand.nextFloat();
            context.logLine(LogLevel.CASUAL,
                            String.format(FORMAT_FLOAT,
                                          context.getLanguageKeep().getDigimonNames().getStringById(i + 1),
                                          "Combat Speed",
                                          a.getCombatSpeed(),
                                          newSpeed));
            a.setCombatSpeed(newSpeed);
        }
        
        context.logLine(LogLevel.ALWAYS, "");
    }
    
    private void randomizeAttributes(RandomizationContext context) {
        Random rand = new Random(context.getInitialSeed() * "Attribute".hashCode());
        
        context.logLine(LogLevel.ALWAYS, "Randomizing Digimon Attributes...");
        
        for (ListIterator<Digimon> itr = context.getGlobalKeepData().getDigimonData().listIterator(); itr.hasNext();) {
            int i = itr.nextIndex();
            Digimon a = itr.next();
            
            if (a.getAttribute() == Attribute.NONE || a.getAttribute() == Attribute.UNKNOWN || a.getAttribute() == Attribute.FREE)
                continue;
            
            Attribute newAttribute = ATTRIBUTE_LIST[rand.nextInt(3)];
            context.logLine(LogLevel.CASUAL,
                            String.format(FORMAT_STRING,
                                          context.getLanguageKeep().getDigimonNames().getStringById(i + 1),
                                          "Attribute",
                                          a.getAttribute(),
                                          newAttribute));
            a.setAttribute(newAttribute);
        }
        
        context.logLine(LogLevel.ALWAYS, "");
    }
    
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        
        map.put("randomizeAttribute", randomizeAttribute.get());
        map.put("randomizeBaseWeight", randomizeBaseWeight.get());
        map.put("randomizeCombatSpeed", randomizeCombatSpeed.get());
        map.put("randomizeFavoriteFood", randomizeFavoriteFood.get());
        map.put("randomizeSleepSchedule", randomizeSleepSchedule.get());
        map.put("randomizeTrainingType", randomizeTrainingType.get());
        map.put("randomizeScale", randomizeScale.get());
        
        return map;
    }
    
    @Override
    public void load(YamlMapping map) {
        if (map == null)
            return;
        
        this.randomizeAttribute.set(Boolean.parseBoolean(map.string("randomizeAttribute")));
        this.randomizeBaseWeight.set(Boolean.parseBoolean(map.string("randomizeBaseWeight")));
        this.randomizeCombatSpeed.set(Boolean.parseBoolean(map.string("randomizeCombatSpeed")));
        this.randomizeFavoriteFood.set(Boolean.parseBoolean(map.string("randomizeFavoriteFood")));
        this.randomizeSleepSchedule.set(Boolean.parseBoolean(map.string("randomizeSleepSchedule")));
        this.randomizeTrainingType.set(Boolean.parseBoolean(map.string("randomizeTrainingType")));
        this.randomizeScale.set(Boolean.parseBoolean(map.string("randomizeScale")));
    }
    
}
