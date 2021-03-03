package net.digimonworld.decode.randomizer.settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;

import com.amihaiemil.eoyaml.YamlMapping;

import de.phoenixstaffel.decodetools.keepdata.Digimon;
import de.phoenixstaffel.decodetools.keepdata.DigimonRaising;
import de.phoenixstaffel.decodetools.keepdata.EvoRequirement;
import de.phoenixstaffel.decodetools.keepdata.EvoRequirement.Comperator;
import de.phoenixstaffel.decodetools.keepdata.EvoRequirement.Operator;
import de.phoenixstaffel.decodetools.keepdata.EvoRequirement.Requirement;
import de.phoenixstaffel.decodetools.keepdata.EvoRequirement.SuperGroup;
import de.phoenixstaffel.decodetools.keepdata.EvoRequirement.Type;
import de.phoenixstaffel.decodetools.keepdata.GlobalKeepData;
import de.phoenixstaffel.decodetools.keepdata.LanguageKeep;
import de.phoenixstaffel.decodetools.keepdata.enums.Level;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import net.digimonworld.decode.randomizer.RandoLogger.LogLevel;
import net.digimonworld.decode.randomizer.RandomizationContext;
import net.digimonworld.decode.randomizer.utils.JavaFXUtils;

public class EvolutionSettings implements Setting {
    
    private BooleanProperty randomizeRequirements = new SimpleBooleanProperty();
    private BooleanProperty randomizePaths = new SimpleBooleanProperty();
    private BooleanProperty randomizeStatsgains = new SimpleBooleanProperty();
    
    @Override
    public TitledPane create(GlobalKeepData inputData, LanguageKeep languageKeep) {
        VBox vbox = new VBox(4);
        TitledPane pane = new TitledPane("Evolution", vbox);
        vbox.setAlignment(Pos.TOP_RIGHT);
        pane.setCollapsible(false);
        
        vbox.getChildren().addAll(JavaFXUtils.buildToggleSwitch("Requirements", Optional.empty(), Optional.of(randomizeRequirements)),
                                  JavaFXUtils.buildToggleSwitch("Paths", Optional.empty(), Optional.of(randomizePaths)),
                                  JavaFXUtils.buildToggleSwitch("Stats Gains", Optional.empty(), Optional.of(randomizeStatsgains)));
        
        return pane;
    }
    
    @Override
    public void randomize(RandomizationContext context) {
        if (randomizeRequirements.get())
            randomizeRequirements(context);
        if (randomizePaths.get())
            randomizePaths(context);
        if (randomizeStatsgains.get())
            randomizeStatsgains(context);
    }
    
    private void randomizeStatsgains(RandomizationContext context) {
        Random rand = new Random(context.getInitialSeed() * "StatsGains".hashCode());
        
        for (ListIterator<Digimon> itr = context.getGlobalKeepData().getDigimonData().listIterator(); itr.hasNext();) {
            int id = itr.nextIndex();
            Digimon digi = itr.next();
            DigimonRaising raise = context.getGlobalKeepData().getRaiseData().get(id);
            String name = context.getLanguageKeep().getDigimonNames().getStringById(id + 1);
            
            if ((digi.getUnk38() & 1) == 1 && raise.getGainHP() != 0) {
                IntSupplier gen = getStatsGainGenerator(digi.getLevel(), rand);
                
                context.logLine(LogLevel.CASUAL, String.format("=== %s's Stats Gains ===", name));
                context.logLine(LogLevel.CASUAL,
                                String.format("Old: %5d %5d %4d %4d %4d %4d",
                                              raise.getGainHP(),
                                              raise.getGainMP(),
                                              raise.getGainOFF(),
                                              raise.getGainDEF(),
                                              raise.getGainSPD(),
                                              raise.getGainBRN()));
                
                raise.setGainHP((short) (gen.getAsInt() * 10));
                raise.setGainMP((short) (gen.getAsInt() * 10));
                raise.setGainOFF((short) gen.getAsInt());
                raise.setGainDEF((short) gen.getAsInt());
                raise.setGainSPD((short) gen.getAsInt());
                raise.setGainBRN((short) gen.getAsInt());
                
                context.logLine(LogLevel.CASUAL,
                                String.format("New: %5d %5d %4d %4d %4d %4d",
                                              raise.getGainHP(),
                                              raise.getGainMP(),
                                              raise.getGainOFF(),
                                              raise.getGainDEF(),
                                              raise.getGainSPD(),
                                              raise.getGainBRN()));
                context.logLine(LogLevel.CASUAL, "");
            }
        }
    }
    
    public IntSupplier getStatsGainGenerator(Level level, Random rand) {
        switch (level) {
            case BABY1:
                return () -> 8 + rand.nextInt(10);
            case BABY2:
                return () -> 16 + rand.nextInt(19);
            case CHILD:
                return () -> 30 + rand.nextInt(101);
            case ADULT:
                return () -> 100 + rand.nextInt(101);
            case PERFECT:
                return () -> 250 + rand.nextInt(251);
            case ULTIMATE:
                return () -> 430 + rand.nextInt(571);
            default:
                return () -> 0;
        }
    }
    
    private void randomizePaths(RandomizationContext context) {
        Random rand = new Random(context.getInitialSeed() * "EvoPaths".hashCode());
        
        Map<Level, List<Integer>> digimonMap = new EnumMap<>(Level.class);
        
        for (ListIterator<Digimon> itr = context.getGlobalKeepData().getDigimonData().listIterator(); itr.hasNext();) {
            int id = itr.nextIndex();
            Digimon digi = itr.next();
            
            if ((digi.getUnk38() & 1) == 1 && id != 67) {
                digimonMap.computeIfAbsent(digi.getLevel(), a -> new ArrayList<Integer>()).add(id);
                DigimonRaising raise = context.getGlobalKeepData().getRaiseData().get(id);
                if (digi.getLevel() != Level.BABY2)
                    raise.setEvolveFrom(new short[5]);
                if (digi.getLevel() != Level.BABY1)
                    raise.setEvolveTo(new short[6]);
            }
        }
        
        LinkedList<Integer> listBaby2 = new LinkedList<>(digimonMap.get(Level.BABY2));
        LinkedList<Integer> listAdult = new LinkedList<>(digimonMap.get(Level.ADULT));
        LinkedList<Integer> listChild = new LinkedList<>(digimonMap.get(Level.CHILD));
        LinkedList<Integer> listPerfect = new LinkedList<>(digimonMap.get(Level.PERFECT));
        LinkedList<Integer> listUltimate = new LinkedList<>(digimonMap.get(Level.ULTIMATE));
        
        Map<Integer, Set<Integer>> baby2Targets = distributeEvolutionPaths(rand, listBaby2, listChild, 5, () -> 5);
        Map<Integer, Set<Integer>> childTargets = distributeEvolutionPaths(rand, listChild, listAdult, 5, () -> 5);
        Map<Integer, Set<Integer>> adultTargets = distributeEvolutionPaths(rand, listAdult, listPerfect, 3, () -> 3);
        Map<Integer, Set<Integer>> perfectTargets = distributeEvolutionPaths(rand, listPerfect, listUltimate, 3, () -> 1 + rand.nextInt(3));
        
        baby2Targets.forEach((digimonId, targetSet) -> setEvolutions(digimonId, targetSet, context));
        childTargets.forEach((digimonId, targetSet) -> setEvolutions(digimonId, targetSet, context));
        adultTargets.forEach((digimonId, targetSet) -> setEvolutions(digimonId, targetSet, context));
        perfectTargets.forEach((digimonId, targetSet) -> setEvolutions(digimonId, targetSet, context));
    }
    
    private void setEvolutions(Integer digimonId, Set<Integer> targetSet, RandomizationContext context) {
        DigimonRaising digi = context.getGlobalKeepData().getRaiseData().get(digimonId);
        String name1 = context.getLanguageKeep().getDigimonNames().getStringById(digimonId + 1);
        String namesNew = targetSet.stream().map(c -> context.getLanguageKeep().getDigimonNames().getStringById(c + 1)).collect(Collectors.joining(" "));
        
        int i = 0;
        for (Integer target : targetSet) {
            DigimonRaising digi2 = context.getGlobalKeepData().getRaiseData().get(target);
            
            for (int j = 0; j < 6; j++)
                if (digi2.getEvolveFrom(j) == 0) {
                    digi2.setEvolveFrom(j, (short) (digimonId + 1));
                    break;
                }
            
            digi.setEvolveTo(i++, (short) (target + 1));
        }
        
        context.logLine(LogLevel.CASUAL, String.format("=== %s's Evolutions ===", name1));
        context.logLine(LogLevel.CASUAL, String.format("New: %s", namesNew));
        context.logLine(LogLevel.CASUAL, "");
    }
    
    private Map<Integer, Set<Integer>> distributeEvolutionPaths(Random rand, List<Integer> from, List<Integer> to, int max1, IntSupplier max2) {
        Map<Integer, Set<Integer>> map = new HashMap<>();
        
        for (Integer i : to) {
            boolean notFound = true;
            while (notFound) {
                int target = from.get(rand.nextInt(from.size()));
                Set<Integer> set = map.computeIfAbsent(target, a -> new HashSet<>());
                if (set.size() < max1)
                    notFound = !set.add(i);
            }
        }
        
        LinkedList<Integer> clonedTo = new LinkedList<>();
        clonedTo.addAll(to);
        clonedTo.addAll(to);
        clonedTo.addAll(to);
        clonedTo.addAll(to);
        
        for (Integer i : from) {
            Set<Integer> set = map.computeIfAbsent(i, a -> new HashSet<>());
            int limit = max2.getAsInt();
            while (set.size() < limit)
                set.add(clonedTo.poll());
        }
        
        return map;
    }
    
    private void randomizeRequirements(RandomizationContext context) {
        Random rand = new Random(context.getInitialSeed() * "EvoRequirements".hashCode());
        
        context.logLine(LogLevel.ALWAYS, "Randomizing Evolution Requirements...");
        
        for (ListIterator<Digimon> itr = context.getGlobalKeepData().getDigimonData().listIterator(); itr.hasNext();) {
            int i = itr.nextIndex();
            Digimon a = itr.next();
            
            if (((a.getUnk38() & 1) != 1))
                continue;
            
            EvoRequirement evo = context.getGlobalKeepData().getEvoRequirements().get(i);
            List<Requirement> requirements = evo.getRequirements();
            
            switch (a.getLevel()) {
                case CHILD:
                    requirements.add(new Requirement(SuperGroup.BONUS, (byte) requirements.size(), Operator.AND, Comperator.GREATER_THAN, Type.HAPPINESS, 0));
                    break;
                case ADULT:
                    requirements = generateAdultRequirements(rand);
                    break;
                case PERFECT:
                    requirements = generatePerfectRequirements(rand);
                    break;
                case ULTIMATE:
                    requirements = generateUltimateRequirements(rand);
                    break;
                default:
                    break;
            }
            
            String digimonName = context.getLanguageKeep().getDigimonNames().getStringById(i + 1);
            context.logLine(LogLevel.CASUAL, String.format("=== %s ===", digimonName));
            context.logLine(LogLevel.CASUAL, "== Old ==");
            evo.getRequirements().forEach(b -> context.logLine(LogLevel.CASUAL, b.toString()));
            context.logLine(LogLevel.CASUAL, "== New ==");
            requirements.forEach(b -> context.logLine(LogLevel.CASUAL, b.toString()));
            context.logLine(LogLevel.CASUAL, "");
            
            evo.setRequirements(requirements);
        }
    }
    
    private List<Requirement> generateUltimateRequirements(Random rand) {
        byte quotaId = 0;
        int numStats = 3 + rand.nextInt(4);
        int numBonus = 1 + rand.nextInt(2);
        
        List<Requirement> list = new ArrayList<>();
        list.add(new Requirement(SuperGroup.QUOTA, quotaId++, Operator.QUOTA, Comperator.QUOTA, Type.QUOTA, numStats + 2));
        
        LinkedList<Type> queue = new LinkedList<>(Arrays.asList(Type.HP, Type.MP, Type.OFFENSE, Type.DEFENSE, Type.SPEED, Type.BRAINS));
        LinkedList<Type> bonusQueue = new LinkedList<>(Arrays.asList(Type.HAPPINESS, Type.DISCIPLINE, Type.BATTLES));
        Collections.shuffle(queue, rand);
        Collections.shuffle(bonusQueue, rand);
        
        for (int i = 0; i < numStats; i++) {
            Type t = queue.poll();
            int val = 300 + rand.nextInt(500);
            if (t == Type.HP || t == Type.MP)
                val *= 10;
            
            list.add(new Requirement(SuperGroup.NORMAL, quotaId++, Operator.AND, Comperator.GREATER_THAN, t, val));
        }
        
        Comperator mode = rand.nextBoolean() ? Comperator.GREATER_THAN : Comperator.LESS_THAN;
        list.add(new Requirement(SuperGroup.NORMAL, quotaId++, Operator.AND, mode, Type.WEIGHT, 25 + rand.nextInt(50)));
        
        if (rand.nextBoolean())
            list.add(new Requirement(SuperGroup.NORMAL, (byte) 5, Operator.AND, Comperator.LESS_THAN, Type.CARE, rand.nextInt(20)));
        else
            list.add(new Requirement(SuperGroup.NORMAL, (byte) 5, Operator.AND, Comperator.GREATER_THAN, Type.CARE, 1 + rand.nextInt(6)));
        
        for (int i = 0; i < numBonus; i++)
            switch (bonusQueue.poll()) {
                case HAPPINESS:
                    list.add(new Requirement(SuperGroup.BONUS, quotaId++, Operator.AND, Comperator.GREATER_THAN, Type.HAPPINESS, 70 + rand.nextInt(30)));
                    break;
                case DISCIPLINE:
                    list.add(new Requirement(SuperGroup.BONUS, quotaId++, Operator.AND, rand.nextBoolean() ? Comperator.GREATER_THAN : Comperator.LESS_THAN,
                            Type.DISCIPLINE, rand.nextInt(100)));
                    break;
                case BATTLES:
                    list.add(new Requirement(SuperGroup.BONUS, quotaId++, Operator.AND, rand.nextBoolean() ? Comperator.GREATER_THAN : Comperator.LESS_THAN,
                            Type.BATTLES, 25 + rand.nextInt(50)));
                    break;
                default:
                    break;
            }
        
        list.add(new Requirement(SuperGroup.BONUS, quotaId++, Operator.AND, Comperator.GREATER_THAN, Type.TECHS, 25 + rand.nextInt(30)));
        list.add(new Requirement(SuperGroup.BONUS, quotaId, Operator.AND, Comperator.GREATER_THAN, Type.DECODE_LEVEL, 10 + rand.nextInt(10)));
        
        return list;
    }
    
    private List<Requirement> generatePerfectRequirements(Random rand) {
        List<Requirement> list = new ArrayList<>();
        list.add(new Requirement(SuperGroup.QUOTA, (byte) 0, Operator.QUOTA, Comperator.QUOTA, Type.QUOTA, 5));
        
        LinkedList<Type> queue1 = new LinkedList<>(Arrays.asList(Type.HP, Type.MP));
        LinkedList<Type> queue2 = new LinkedList<>(Arrays.asList(Type.OFFENSE, Type.DEFENSE));
        LinkedList<Type> queue3 = new LinkedList<>(Arrays.asList(Type.SPEED, Type.BRAINS));
        Collections.shuffle(queue1, rand);
        Collections.shuffle(queue2, rand);
        Collections.shuffle(queue3, rand);
        int numStats1 = 1 + rand.nextInt(2);
        int numStats2 = 1 + rand.nextInt(2);
        int numStats3 = 1 + rand.nextInt(2);
        
        for (int i = 0; i < numStats1; i++) {
            int val = 1000 + rand.nextInt(300) * 10;
            list.add(new Requirement(SuperGroup.NORMAL, (byte) 1, Operator.AND, Comperator.GREATER_THAN, queue1.poll(), val));
        }
        for (int i = 0; i < numStats2; i++) {
            int val = 100 + rand.nextInt(300);
            list.add(new Requirement(SuperGroup.NORMAL, (byte) 2, Operator.AND, Comperator.GREATER_THAN, queue2.poll(), val));
        }
        for (int i = 0; i < numStats3; i++) {
            int val = 100 + rand.nextInt(300);
            list.add(new Requirement(SuperGroup.NORMAL, (byte) 3, Operator.AND, Comperator.GREATER_THAN, queue3.poll(), val));
        }
        
        Comperator mode = rand.nextBoolean() ? Comperator.GREATER_THAN : Comperator.LESS_THAN;
        list.add(new Requirement(SuperGroup.NORMAL, (byte) 4, Operator.AND, mode, Type.WEIGHT, 20 + rand.nextInt(45)));
        
        if (rand.nextBoolean())
            list.add(new Requirement(SuperGroup.NORMAL, (byte) 5, Operator.AND, Comperator.LESS_THAN, Type.CARE, rand.nextInt(20)));
        else
            list.add(new Requirement(SuperGroup.NORMAL, (byte) 5, Operator.AND, Comperator.GREATER_THAN, Type.CARE, 1 + rand.nextInt(6)));
        
        LinkedList<Type> bonusQueue = new LinkedList<>(Arrays.asList(Type.HAPPINESS, Type.DISCIPLINE, Type.BATTLES));
        Collections.shuffle(bonusQueue, rand);
        
        int numBonus = 1 + (rand.nextFloat() < 0.25 ? 1 : 0);
        for (int i = 0; i < numBonus; i++)
            switch (bonusQueue.poll()) {
                case HAPPINESS:
                    list.add(new Requirement(SuperGroup.BONUS, (byte) (6 + i), Operator.AND, Comperator.GREATER_THAN, Type.HAPPINESS, 70 + rand.nextInt(30)));
                    break;
                case DISCIPLINE:
                    list.add(new Requirement(SuperGroup.BONUS, (byte) (6 + i), Operator.AND,
                            rand.nextBoolean() ? Comperator.GREATER_THAN : Comperator.LESS_THAN, Type.DISCIPLINE, 50 + rand.nextInt(46)));
                    break;
                case BATTLES:
                    list.add(new Requirement(SuperGroup.BONUS, (byte) (6 + i), Operator.AND,
                            rand.nextBoolean() ? Comperator.GREATER_THAN : Comperator.LESS_THAN, Type.BATTLES, 20 + rand.nextInt(25)));
                    break;
                default:
                    break;
            }
        
        list.add(new Requirement(SuperGroup.BONUS, (byte) (6 + numBonus), Operator.AND, Comperator.GREATER_THAN, Type.TECHS, 25 + rand.nextInt(30)));
        list.add(new Requirement(SuperGroup.BONUS, (byte) (7 + numBonus), Operator.AND, Comperator.GREATER_THAN, Type.DECODE_LEVEL, 10 + rand.nextInt(10)));
        
        return list;
    }
    
    private List<Requirement> generateAdultRequirements(Random rand) {
        List<Requirement> list = new ArrayList<>();
        list.add(new Requirement(SuperGroup.QUOTA, (byte) 0, Operator.QUOTA, Comperator.QUOTA, Type.QUOTA, 3));
        
        LinkedList<Type> queue = new LinkedList<>(Arrays.asList(Type.HP, Type.MP, Type.OFFENSE, Type.DEFENSE, Type.SPEED, Type.BRAINS));
        Collections.shuffle(queue, rand);
        
        int numStats = 1 + rand.nextInt(5);
        for (int i = 0; i < numStats; i++) {
            Type t = queue.poll();
            int val = 50 + rand.nextInt(120);
            if (t == Type.HP || t == Type.MP)
                val *= 10;
            
            list.add(new Requirement(SuperGroup.NORMAL, (byte) 1, Operator.AND, Comperator.GREATER_THAN, t, val));
        }
        
        Comperator mode = rand.nextBoolean() ? Comperator.GREATER_THAN : Comperator.LESS_THAN;
        list.add(new Requirement(SuperGroup.NORMAL, (byte) 2, Operator.AND, mode, Type.WEIGHT, 5 + rand.nextInt(40)));
        
        if (rand.nextBoolean())
            list.add(new Requirement(SuperGroup.NORMAL, (byte) 3, Operator.AND, Comperator.LESS_THAN, Type.CARE, rand.nextInt(11)));
        else
            list.add(new Requirement(SuperGroup.NORMAL, (byte) 3, Operator.AND, Comperator.GREATER_THAN, Type.CARE, 1 + rand.nextInt(3)));
        
        LinkedList<Type> bonusQueue = new LinkedList<>(Arrays.asList(Type.HAPPINESS, Type.DISCIPLINE, Type.BATTLES));
        Collections.shuffle(bonusQueue, rand);
        
        switch (bonusQueue.poll()) {
            case HAPPINESS:
                list.add(new Requirement(SuperGroup.BONUS, (byte) 4, Operator.AND, Comperator.GREATER_THAN, Type.HAPPINESS, 50 + rand.nextInt(46)));
                break;
            case DISCIPLINE:
                list.add(new Requirement(SuperGroup.BONUS, (byte) 4, Operator.AND, rand.nextBoolean() ? Comperator.GREATER_THAN : Comperator.LESS_THAN,
                        Type.DISCIPLINE, 50 + rand.nextInt(46)));
                break;
            case BATTLES:
                list.add(new Requirement(SuperGroup.BONUS, (byte) 4, Operator.AND, rand.nextBoolean() ? Comperator.GREATER_THAN : Comperator.LESS_THAN,
                        Type.BATTLES, 5 + rand.nextInt(26)));
                break;
            default:
                break;
        }
        
        list.add(new Requirement(SuperGroup.BONUS, (byte) 5, Operator.AND, Comperator.GREATER_THAN, Type.TECHS, 10 + rand.nextInt(30)));
        list.add(new Requirement(SuperGroup.BONUS, (byte) 6, Operator.AND, Comperator.GREATER_THAN, Type.DECODE_LEVEL, 5 + rand.nextInt(13)));
        
        return list;
    }
    
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        
        map.put("randomizeRequirements", randomizeRequirements.get());
        map.put("randomizePaths", randomizePaths.get());
        map.put("randomizeStatsgains", randomizeStatsgains.get());
        
        return map;
    }
    
    @Override
    public void load(YamlMapping map) {
        if (map == null)
            return;
        
        this.randomizeRequirements.set(Boolean.parseBoolean(map.string("randomizeRequirements")));
        this.randomizePaths.set(Boolean.parseBoolean(map.string("randomizePaths")));
        this.randomizeStatsgains.set(Boolean.parseBoolean(map.string("randomizeStatsgains")));
    }
}
