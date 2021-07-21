package net.digimonworld.decode.randomizer.settings;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import org.controlsfx.control.ToggleSwitch;

import com.amihaiemil.eoyaml.YamlMapping;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import net.digimonworld.decode.randomizer.RandoLogger.LogLevel;
import net.digimonworld.decode.randomizer.RandomizationContext;
import net.digimonworld.decode.randomizer.utils.JavaFXUtils;
import net.digimonworld.decode.randomizer.utils.RandomizerUtils;
import net.digimonworld.decodetools.core.MappedSet;
import net.digimonworld.decodetools.core.StreamAccess;
import net.digimonworld.decodetools.data.keepdata.Digimon;
import net.digimonworld.decodetools.data.keepdata.EnemyData;
import net.digimonworld.decodetools.data.keepdata.EnemyData.ItemDrop;
import net.digimonworld.decodetools.data.keepdata.GlobalKeepData;
import net.digimonworld.decodetools.data.keepdata.LanguageKeep;
import net.digimonworld.decodetools.data.keepdata.MapEnemyData;
import net.digimonworld.decodetools.data.keepdata.MapEnemyData.MapEnemyEntry;
import net.digimonworld.decodetools.data.keepdata.TreasureLoot;
import net.digimonworld.decodetools.data.keepdata.TypeAlignmentChart;
import net.digimonworld.decodetools.data.keepdata.enums.DropType;
import net.digimonworld.decodetools.data.keepdata.enums.Special;
import net.digimonworld.decodetools.data.map.MapItemSpawns;
import net.digimonworld.decodetools.data.map.MapItemSpawns.ItemSpawn;
import net.digimonworld.decodetools.data.map.MapItemSpawns.MapItemSpawn;
import net.digimonworld.decodetools.res.kcap.AbstractKCAP;
import net.digimonworld.decodetools.res.payload.GenericPayload;

public class WorldSettings implements Setting {
    
    private BooleanProperty randomizeTypeEffectiveness = new SimpleBooleanProperty();
    private BooleanProperty randomizeTreasureHunt = new SimpleBooleanProperty();
    private BooleanProperty randomizeItemSpawns = new SimpleBooleanProperty();
    
    private BooleanProperty randomizeEnemies = new SimpleBooleanProperty();
    private BooleanProperty randomizeEnemyStats = new SimpleBooleanProperty();
    private BooleanProperty randomizeEnemyDigimon = new SimpleBooleanProperty();
    private BooleanProperty randomizeEnemyMoves = new SimpleBooleanProperty();
    private BooleanProperty randomizeEnemyLoot = new SimpleBooleanProperty();
    
    @Override
    public TitledPane create(GlobalKeepData inputData, LanguageKeep languageKeep) {
        VBox vbox = new VBox(4);
        TitledPane pane = new TitledPane("World", vbox);
        vbox.setAlignment(Pos.TOP_RIGHT);
        pane.setCollapsible(false);
        
        ToggleSwitch enemies = JavaFXUtils.buildToggleSwitch("Enemies", Optional.empty(), Optional.of(randomizeEnemies));
        ToggleSwitch stats = JavaFXUtils.buildToggleSwitch("Enemy Stats", Optional.empty(), Optional.of(randomizeEnemyStats));
        ToggleSwitch digimon = JavaFXUtils.buildToggleSwitch("Enemy Digimon", Optional.empty(), Optional.of(randomizeEnemyDigimon));
        ToggleSwitch moves = JavaFXUtils.buildToggleSwitch("Enemy Moves", Optional.empty(), Optional.of(randomizeEnemyMoves));
        ToggleSwitch loot = JavaFXUtils.buildToggleSwitch("Enemy Loot", Optional.empty(), Optional.of(randomizeEnemyLoot));
        
        stats.disableProperty().bind(randomizeEnemies.not());
        digimon.disableProperty().bind(randomizeEnemies.not());
        moves.disableProperty().bind(randomizeEnemies.not());
        loot.disableProperty().bind(randomizeEnemies.not());
        
        vbox.getChildren().addAll(JavaFXUtils.buildToggleSwitch("Type Effectiveness", Optional.empty(), Optional.of(randomizeTypeEffectiveness)),
                                  JavaFXUtils.buildToggleSwitch("Treasure Hunt", Optional.empty(), Optional.of(randomizeTreasureHunt)),
                                  JavaFXUtils.buildToggleSwitch("Item Spawns", Optional.empty(), Optional.of(randomizeItemSpawns)),
                                  enemies,
                                  stats,
                                  digimon,
                                  moves,
                                  loot);
        return pane;
    }
    
    @Override
    public void randomize(RandomizationContext context) {
        if (randomizeTypeEffectiveness.get())
            randomizeTypeEffectiveness(context);
        if (randomizeTreasureHunt.get())
            randomizeTreasureHunt(context);
        if (randomizeEnemies.get())
            randomizeEnemies(context);
        if (randomizeItemSpawns.get())
            randomizeItemSpawns(context);
    }
    
    private void randomizeItemSpawns(RandomizationContext context) {
        Random rand = new Random(context.getInitialSeed() * "ItemSpawns".hashCode());
        
        context.logLine(LogLevel.ALWAYS, "Randomizing Item Spawns...");
        context.logLine(LogLevel.CASUAL, "");
        
        List<Integer> items = RandomizerUtils.getUseableItems(context.getGlobalKeepData());
        Map<String, Integer> mapMap = readItemSpawnMapping();
        
        mapMap.forEach((a, b) -> {
            String filePath = String.format("part0/arcv/map/%s.res", a);
            context.getFile(filePath).ifPresent(m -> {
                AbstractKCAP root = (AbstractKCAP) m;
                AbstractKCAP map = (AbstractKCAP) root.get(1);
                GenericPayload payload = (GenericPayload) map.get(b);
                
                @SuppressWarnings("resource")
                MapItemSpawns mapSpawns = new MapItemSpawns(new StreamAccess(payload.getData()));
                
                context.logLine(LogLevel.CASUAL, String.format("=== %s old spawns: ===", a));
                logItemSpawns(mapSpawns, context);
                    
                for (MapItemSpawn spawns : mapSpawns.getSpawns()) {
                    int numItems = 1 + rand.nextInt(8);
                    float chancePerItem = (0.1f + rand.nextFloat() * 0.9f) / numItems;
                    
                    for (int i = 0; i < 8; i++) {
                        ItemSpawn spawn = spawns.getSpawns()[i];
                        
                        // don't randomize firewood
                        if(spawn.getItemId() == 367)
                            continue;
                        
                        spawn.setChance(i < numItems ? chancePerItem : 0f);
                        spawn.setItemId(i < numItems ? items.get(rand.nextInt(items.size())) : 0);
                    }
                }
                
                context.logLine(LogLevel.CASUAL, String.format("=== %s new spawns: ===", a));
                logItemSpawns(mapSpawns, context);
                    
                payload.setData(mapSpawns.toByteArray());
                context.logLine(LogLevel.CASUAL, "");
            });
        });
    }
    
    private void logItemSpawns(MapItemSpawns mapSpawns, RandomizationContext context) {
        for (MapItemSpawn spawns : mapSpawns.getSpawns())
            for (ItemSpawn spawn : spawns.getSpawns())
                context.logLine(LogLevel.CASUAL,
                                String.format("%30s | %5.4f",
                                              context.getLanguageKeep().getItemNames().getStringById(spawn.getItemId()),
                                              spawn.getChance()));
    }
    
    private Map<String, Integer> readItemSpawnMapping() {
        Map<String, Integer> map = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(WorldSettings.class.getResourceAsStream("itemSpawnMapping.csv")))) {
            String str;
            while ((str = reader.readLine()) != null) {
                String[] arr = str.split(",");
                map.put(arr[0], Integer.parseInt(arr[1]));
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        
        return map;
    }
    
    private void randomizeEnemies(RandomizationContext context) {
        Random rand = new Random(context.getInitialSeed() * "Enemies".hashCode());
        
        context.logLine(LogLevel.ALWAYS, "Randomizing Map Encounters...");
        context.logLine(LogLevel.CASUAL, "");
        
        GlobalKeepData keep = context.getGlobalKeepData();
        MappedSet<Short, EnemyData> enemyData = keep.getEnemyData();
        List<Integer> digimonList = RandomizerUtils.getEnemyDigimonList(keep);
        
        int i = 7000;
        for (MapEnemyData a : keep.getEnemyMapData()) {
            for (MapEnemyEntry entry : a.getEntries()) {
                if (entry.getEnemyId() == 0)
                    continue;
                
                EnemyData oldData = enemyData.get(entry.getEnemyId());
                EnemyData newData = new EnemyData(oldData);
                
                entry.setEnemyId((short) i);
                newData.setEnemyId((short) i++);
                
                // Digimon ID
                if (randomizeEnemyDigimon.get())
                    newData.setDigimonId(digimonList.get(rand.nextInt(digimonList.size())).shortValue());
                
                Digimon digi = keep.getDigimonData().get(newData.getDigimonId() - 1);
                
                // Stats
                if (randomizeEnemyStats.get())
                    randomizeEnemyStats(rand, newData);
                
                // Moves
                List<Byte> moves = RandomizerUtils.getMoveList(digi);
                Collections.shuffle(moves, rand);
                
                if (randomizeEnemyMoves.get() || !moves.contains(newData.getMove1()))
                    newData.setMove1(moves.isEmpty() ? 0 : moves.get(0));
                if (randomizeEnemyMoves.get() || !moves.contains(newData.getMove2()))
                    newData.setMove2(moves.size() < 2 ? 0 : moves.get(1));
                if (randomizeEnemyMoves.get() || !moves.contains(newData.getMove3()))
                    newData.setMove3(moves.size() < 3 ? 0 : moves.get(2));
                
                // Loot
                if (randomizeEnemyLoot.get())
                    randomizeLoot(rand, newData, keep);
                
                enemyData.add(newData);
                
                context.logLine(LogLevel.CASUAL, String.format("=== Map %s | Spawn %d", a.getMap(), a.getSpawnPoint()));
                context.logLine(LogLevel.CASUAL, "");
                RandomizerUtils.logEnemyData(context, oldData);
                context.logLine(LogLevel.CASUAL, "");
                RandomizerUtils.logEnemyData(context, newData);
                context.logLine(LogLevel.CASUAL, "");
                context.logLine(LogLevel.CASUAL, "");
            }
        }
    }
    
    private void randomizeLoot(Random rand, EnemyData newData, GlobalKeepData keep) {
        List<Integer> itemList = RandomizerUtils.getUseableItems(keep);
        
        short bits = newData.getBits();
        short decodeXP = newData.getDecodeXP();
        
        bits *= 1 + (rand.nextBoolean() ? -rand.nextFloat() / 2 : rand.nextFloat());
        decodeXP *= 1 + (rand.nextBoolean() ? -rand.nextFloat() / 2 : rand.nextFloat());
        
        newData.setBits(bits);
        newData.setDecodeXP(decodeXP);
        
        for (int j = 0; j < 3; j++) {
            int r1 = rand.nextInt(10);
            ItemDrop drop = newData.getDrop(j);
            
            if (r1 < 5) {
                drop.setType(DropType.ITEM);
                drop.setId(itemList.get(rand.nextInt(itemList.size())).shortValue());
                drop.setChance(Math.min(1.0f, 0.05f + rand.nextFloat()));
            }
            else if (r1 < 7) {
                drop.setType(DropType.CARD);
                drop.setId((short) (1 + rand.nextInt(588)));
                drop.setChance(Math.min(1.0f, 0.05f + rand.nextFloat()));
            }
            else {
                drop.setType(DropType.ITEM);
                drop.setId((short) 0);
                drop.setChance(0f);
            }
        }
    }
    
    private void randomizeEnemyStats(Random rand, EnemyData newData) {
        int hp = newData.getHp();
        int mp = newData.getMp();
        short off = newData.getOffense();
        short def = newData.getDefense();
        short speed = newData.getSpeed();
        short brains = newData.getBrains();
        
        hp *= 1 + (rand.nextBoolean() ? -rand.nextFloat() / 2 : rand.nextFloat());
        mp *= 1 + (rand.nextBoolean() ? -rand.nextFloat() / 2 : rand.nextFloat());
        off *= 1 + (rand.nextBoolean() ? -rand.nextFloat() / 2 : rand.nextFloat());
        def *= 1 + (rand.nextBoolean() ? -rand.nextFloat() / 2 : rand.nextFloat());
        speed *= 1 + (rand.nextBoolean() ? -rand.nextFloat() / 2 : rand.nextFloat());
        brains *= 1 + (rand.nextBoolean() ? -rand.nextFloat() / 2 : rand.nextFloat());
        
        newData.setHp(hp);
        newData.setMp(mp);
        newData.setOffense(off);
        newData.setDefense(def);
        newData.setSpeed(speed);
        newData.setBrains(brains);
    }
    
    private void randomizeTreasureHunt(RandomizationContext context) {
        Random rand = new Random(context.getInitialSeed() * "TreasureLoot".hashCode());
        List<Integer> itemList = RandomizerUtils.getUseableItems(context.getGlobalKeepData());
        
        context.logLine(LogLevel.ALWAYS, "Randomizing Treasure Hunt Loot...");
        context.logLine(LogLevel.CASUAL, "Old Table: ");
        
        for (int i = 0; i < 120; i++) {
            if (i % 20 == 0)
                Collections.shuffle(itemList, rand);
            
            TreasureLoot loot = context.getGlobalKeepData().getTreasureLoot().get(i);
            context.logLine(LogLevel.CASUAL,
                            String.format("%s - %3.2f", context.getLanguageKeep().getItemNames().getStringById(loot.getItem()), loot.getChance()));
            
            loot.setItem(itemList.get(i % 20));
            loot.setChance(0.05f);
        }
        
        context.logLine(LogLevel.CASUAL, "");
        context.logLine(LogLevel.CASUAL, "New Table: ");
        
        for (TreasureLoot loot : context.getGlobalKeepData().getTreasureLoot())
            context.logLine(LogLevel.CASUAL,
                            String.format("%s - %3.2f", context.getLanguageKeep().getItemNames().getStringById(loot.getItem()), loot.getChance()));
    }
    
    private void randomizeTypeEffectiveness(RandomizationContext context) {
        Random rand = new Random(context.getInitialSeed() * "TypeAlignment".hashCode());
        
        TypeAlignmentChart chart = context.getGlobalKeepData().getTypeAlignmentChart();
        
        StringBuilder sbOld = new StringBuilder();
        StringBuilder sbNew = new StringBuilder();
        
        for (Special attacker : TypeAlignmentChart.VALUES) {
            for (Special victim : TypeAlignmentChart.VALUES) {
                int val = rand.nextInt(101);
                int value = 50 + (Math.min(val, 50) + Math.max(2 * (val - 50), 0));
                sbOld.append(Byte.toUnsignedInt(chart.get(attacker, victim))).append(" ");
                sbNew.append(value).append(" ");
                chart.set(attacker, victim, value);
            }
            sbOld.append("\n");
            sbNew.append("\n");
        }
        
        context.logLine(LogLevel.ALWAYS, "Randomizing type alignments...");
        context.logLine(LogLevel.CASUAL, "Old Alignments: ");
        context.logLine(LogLevel.CASUAL, sbOld.toString());
        context.logLine(LogLevel.CASUAL, "New Alignments: ");
        context.logLine(LogLevel.CASUAL, sbNew.toString());
    }
    
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        
        map.put("randomizeTypeEffectiveness", randomizeTypeEffectiveness.get());
        map.put("randomizeTreasureHunt", randomizeTreasureHunt.get());
        map.put("randomizeEnemies", randomizeEnemies.get());
        map.put("randomizeEnemyStats", randomizeEnemyStats.get());
        map.put("randomizeEnemyDigimon", randomizeEnemyDigimon.get());
        map.put("randomizeEnemyMoves", randomizeEnemyMoves.get());
        map.put("randomizeEnemyLoot", randomizeEnemyLoot.get());
        map.put("randomizeItemSpawns", randomizeItemSpawns.get());
        
        return map;
    }
    
    @Override
    public void load(YamlMapping map) {
        if (map == null)
            return;
        
        this.randomizeTypeEffectiveness.set(Boolean.parseBoolean(map.string("randomizeTypeEffectiveness")));
        this.randomizeTreasureHunt.set(Boolean.parseBoolean(map.string("randomizeTreasureHunt")));
        this.randomizeEnemies.set(Boolean.parseBoolean(map.string("randomizeEnemies")));
        this.randomizeEnemyStats.set(Boolean.parseBoolean(map.string("randomizeEnemyStats")));
        this.randomizeEnemyDigimon.set(Boolean.parseBoolean(map.string("randomizeEnemyDigimon")));
        this.randomizeEnemyMoves.set(Boolean.parseBoolean(map.string("randomizeEnemyMoves")));
        this.randomizeEnemyLoot.set(Boolean.parseBoolean(map.string("randomizeEnemyLoot")));
        this.randomizeItemSpawns.set(Boolean.parseBoolean(map.string("randomizeItemSpawns")));
    }
    
}
