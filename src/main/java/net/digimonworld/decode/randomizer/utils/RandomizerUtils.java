package net.digimonworld.decode.randomizer.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import net.digimonworld.decodetools.keepdata.Digimon;
import net.digimonworld.decodetools.keepdata.EnemyData;
import net.digimonworld.decodetools.keepdata.EnemyData.ItemDrop;
import net.digimonworld.decodetools.keepdata.GlobalKeepData;
import net.digimonworld.decodetools.keepdata.Item;
import net.digimonworld.decodetools.keepdata.Item.ItemType;
import net.digimonworld.decodetools.keepdata.enums.DropType;
import net.digimonworld.decode.randomizer.RandoLogger.LogLevel;
import net.digimonworld.decode.randomizer.RandomizationContext;

public class RandomizerUtils {
    private RandomizerUtils() {
    }
    
    public static List<Integer> getUseableItems(GlobalKeepData data) {
        List<Integer> itemList = new ArrayList<>();
        for (ListIterator<Item> itr = data.getItems().listIterator(); itr.hasNext();) {
            int i = itr.nextIndex();
            Item a = itr.next();
            
            // filter technical, unused and Mt. Infinity items
            if (a.getType() == ItemType.BASE || a.getUnk30() == 0 || (a.getUnk31() & 65) != 0)
                continue;
            
            itemList.add(i + 1);
        }
        return itemList;
    }
    
    public static List<Byte> getMoveList(Digimon digi) {
        List<Byte> moves = new ArrayList<>();
        for (byte b : digi.getSkills())
            if (b != 0)
                moves.add(b);
            
        return moves;
    }
    
    public static List<Integer> getEnemyDigimonList(GlobalKeepData keep) {
        List<Integer> itemList = new ArrayList<>();
        for (ListIterator<Digimon> itr = keep.getDigimonData().listIterator(); itr.hasNext();) {
            int i = itr.nextIndex();
            Digimon a = itr.next();
            
            if (a.getSkills()[0] == 0)
                continue;
            
            itemList.add(i + 1);
        }
        return itemList;
    }
    
    public static void logEnemyData(RandomizationContext context, EnemyData oldData) {
        context.logLine(LogLevel.CASUAL,
                        String.format("ID %d | %s | Stats: %d %d %d %d %d %d",
                                      oldData.getEnemyId(),
                                      context.getLanguageKeep().getDigimonNames().getStringById(oldData.getDigimonId()),
                                      oldData.getHp(),
                                      oldData.getMp(),
                                      oldData.getOffense(),
                                      oldData.getDefense(),
                                      oldData.getSpeed(),
                                      oldData.getBrains()));
        
        context.logLine(LogLevel.CASUAL,
                        String.format("Moves: %s | %s | %s",
                                      context.getLanguageKeep().getSkillNames().getStringById(oldData.getMove1()),
                                      context.getLanguageKeep().getSkillNames().getStringById(oldData.getMove2()),
                                      context.getLanguageKeep().getSkillNames().getStringById(oldData.getMove3())));
        
        for (int i = 0; i < 3; i++) {
            ItemDrop drop = oldData.getDrop(i);
            String name = drop.getType() == DropType.CARD ? context.getLanguageKeep().getCardNames1().getStringById(drop.getId())
                    : context.getLanguageKeep().getItemNames().getStringById(drop.getId());
            
            context.logLine(LogLevel.CASUAL, String.format("Drop %d: %s | %s | %1.3f", i, drop.getType().name(), name, drop.getChance()));
        }
        
    }
}
