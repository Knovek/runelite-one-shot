package com.oneshot.modules;

import com.oneshot.utils.Constants;
import net.runelite.api.gameval.VarbitID;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class DiaryImages
{
    public static class DiaryInfo
    {
        private final String area;
        private final String tier;

        public DiaryInfo(String area, String tier)
        {
            this.area = area;
            this.tier = tier;
        }

        public String getArea()
        {
            return area;
        }

        public String getTier()
        {
            return tier;
        }
    }

    private static String getDiaryTier(int varbitId) {
        Set<Integer> easy = Set.of(
                VarbitID.ARDOUGNE_DIARY_EASY_COMPLETE,
                VarbitID.FALADOR_DIARY_EASY_COMPLETE,
                VarbitID.WILDERNESS_DIARY_EASY_COMPLETE,
                VarbitID.WESTERN_DIARY_EASY_COMPLETE,
                VarbitID.KANDARIN_DIARY_EASY_COMPLETE,
                VarbitID.VARROCK_DIARY_EASY_COMPLETE,
                VarbitID.DESERT_DIARY_EASY_COMPLETE,
                VarbitID.MORYTANIA_DIARY_EASY_COMPLETE,
                VarbitID.FREMENNIK_DIARY_EASY_COMPLETE,
                VarbitID.LUMBRIDGE_DIARY_EASY_COMPLETE,
                VarbitID.KOUREND_DIARY_EASY_COMPLETE,
                VarbitID.ATJUN_EASY_DONE
        );

        Set<Integer> medium = Set.of(
                VarbitID.ARDOUGNE_DIARY_MEDIUM_COMPLETE,
                VarbitID.FALADOR_DIARY_MEDIUM_COMPLETE,
                VarbitID.WILDERNESS_DIARY_MEDIUM_COMPLETE,
                VarbitID.WESTERN_DIARY_MEDIUM_COMPLETE,
                VarbitID.KANDARIN_DIARY_MEDIUM_COMPLETE,
                VarbitID.VARROCK_DIARY_MEDIUM_COMPLETE,
                VarbitID.DESERT_DIARY_MEDIUM_COMPLETE,
                VarbitID.MORYTANIA_DIARY_MEDIUM_COMPLETE,
                VarbitID.FREMENNIK_DIARY_MEDIUM_COMPLETE,
                VarbitID.LUMBRIDGE_DIARY_MEDIUM_COMPLETE,
                VarbitID.KOUREND_DIARY_MEDIUM_COMPLETE,
                VarbitID.ATJUN_MED_DONE
        );

        Set<Integer> hard = Set.of(
                VarbitID.ARDOUGNE_DIARY_HARD_COMPLETE,
                VarbitID.FALADOR_DIARY_HARD_COMPLETE,
                VarbitID.WILDERNESS_DIARY_HARD_COMPLETE,
                VarbitID.WESTERN_DIARY_HARD_COMPLETE,
                VarbitID.KANDARIN_DIARY_HARD_COMPLETE,
                VarbitID.VARROCK_DIARY_HARD_COMPLETE,
                VarbitID.DESERT_DIARY_HARD_COMPLETE,
                VarbitID.MORYTANIA_DIARY_HARD_COMPLETE,
                VarbitID.FREMENNIK_DIARY_HARD_COMPLETE,
                VarbitID.LUMBRIDGE_DIARY_HARD_COMPLETE,
                VarbitID.KOUREND_DIARY_HARD_COMPLETE,
                VarbitID.ATJUN_HARD_DONE
        );

        Set<Integer> elite = Set.of(
                VarbitID.ARDOUGNE_DIARY_ELITE_COMPLETE,
                VarbitID.FALADOR_DIARY_ELITE_COMPLETE,
                VarbitID.WILDERNESS_DIARY_ELITE_COMPLETE,
                VarbitID.WESTERN_DIARY_ELITE_COMPLETE,
                VarbitID.KANDARIN_DIARY_ELITE_COMPLETE,
                VarbitID.VARROCK_DIARY_ELITE_COMPLETE,
                VarbitID.DESERT_DIARY_ELITE_COMPLETE,
                VarbitID.MORYTANIA_DIARY_ELITE_COMPLETE,
                VarbitID.FREMENNIK_DIARY_ELITE_COMPLETE,
                VarbitID.LUMBRIDGE_DIARY_ELITE_COMPLETE,
                VarbitID.KOUREND_DIARY_ELITE_COMPLETE,
                VarbitID.KARAMJA_DIARY_ELITE_COMPLETE
        );

        if (easy.contains(varbitId)) return "Easy";
        if (medium.contains(varbitId)) return "Medium";
        if (hard.contains(varbitId)) return "Hard";
        if (elite.contains(varbitId)) return "Elite";

        return "Unknown"; // fallback if no match
    }

    public static DiaryInfo getDiaryInfo(int varbitId)
    {
        String area;
        String tier;

        // ----- AREA -----
        if (varbitId == VarbitID.ATJUN_EASY_DONE
                || varbitId == VarbitID.ATJUN_MED_DONE
                || varbitId == VarbitID.ATJUN_HARD_DONE)
        {
            area = "Karamja";
        }
        else if (varbitId >= VarbitID.ARDOUGNE_DIARY_EASY_COMPLETE
                && varbitId <= VarbitID.ARDOUGNE_DIARY_ELITE_COMPLETE)
        {
            area = "Ardougne";
        }
        else if (varbitId >= VarbitID.FALADOR_DIARY_EASY_COMPLETE
                && varbitId <= VarbitID.FALADOR_DIARY_ELITE_COMPLETE)
        {
            area = "Falador";
        }
        else if (varbitId >= VarbitID.WILDERNESS_DIARY_EASY_COMPLETE
                && varbitId <= VarbitID.WILDERNESS_DIARY_ELITE_COMPLETE)
        {
            area = "Wilderness";
        }
        else if (varbitId >= VarbitID.WESTERN_DIARY_EASY_COMPLETE
                && varbitId <= VarbitID.WESTERN_DIARY_ELITE_COMPLETE)
        {
            area = "Western Provinces";
        }
        else if (varbitId >= VarbitID.KANDARIN_DIARY_EASY_COMPLETE
                && varbitId <= VarbitID.KANDARIN_DIARY_ELITE_COMPLETE)
        {
            area = "Kandarin";
        }
        else if (varbitId >= VarbitID.VARROCK_DIARY_EASY_COMPLETE
                && varbitId <= VarbitID.VARROCK_DIARY_ELITE_COMPLETE)
        {
            area = "Varrock";
        }
        else if (varbitId >= VarbitID.DESERT_DIARY_EASY_COMPLETE
                && varbitId <= VarbitID.DESERT_DIARY_ELITE_COMPLETE)
        {
            area = "Desert";
        }
        else if (varbitId >= VarbitID.MORYTANIA_DIARY_EASY_COMPLETE
                && varbitId <= VarbitID.MORYTANIA_DIARY_ELITE_COMPLETE)
        {
            area = "Morytania";
        }
        else if (varbitId >= VarbitID.FREMENNIK_DIARY_EASY_COMPLETE
                && varbitId <= VarbitID.FREMENNIK_DIARY_ELITE_COMPLETE)
        {
            area = "Fremennik Province";
        }
        else if (varbitId >= VarbitID.LUMBRIDGE_DIARY_EASY_COMPLETE
                && varbitId <= VarbitID.LUMBRIDGE_DIARY_ELITE_COMPLETE)
        {
            area = "Lumbridge & Draynor";
        }
        else if (varbitId >= VarbitID.KOUREND_DIARY_EASY_COMPLETE
                && varbitId <= VarbitID.KOUREND_DIARY_ELITE_COMPLETE)
        {
            area = "Kourend & Kebos";
        }
        else if (varbitId == VarbitID.KARAMJA_DIARY_ELITE_COMPLETE)
        {
            area = "Karamja";
        }
        else
        {
            return null;
        }

        // ----- TIER -----
        tier = getDiaryTier(varbitId);

        return new DiaryInfo(area, tier);
    }


    // small region -> base filename map
    private static final Map<String, String> DIARY_REGION_ITEM_BASE;
    static
    {
        Map<String, String> m = new HashMap<>();
        m.put("ARDOUGNE", "Ardougne_cloak");
        m.put("DESERT", "Desert_amulet");
        m.put("FALADOR", "Falador_shield");
        m.put("FREMENNIK", "Fremennik_sea_boots");
        m.put("KANDARIN", "Kandarin_headgear");
        m.put("KARAMJA", "Karamja_gloves");
        m.put("KOUREND", "Rada's_blessing");
        m.put("LUMBRIDGE", "Explorer's_ring");
        m.put("MORYTANIA", "Morytania_legs");
        m.put("VARROCK", "Varrock_armour");
        m.put("WESTERN", "Western_banner");
        m.put("WILDERNESS", "Wilderness_sword");
        DIARY_REGION_ITEM_BASE = Collections.unmodifiableMap(m);
    }

    // alias map for special varbit prefixes
    private static final Map<String, String> DIARY_REGION_ALIASES;
    static
    {
        Map<String, String> m = new HashMap<>();
        m.put("ATJUN", "KARAMJA"); // special-case mapping
        DIARY_REGION_ALIASES = Collections.unmodifiableMap(m);
    }

    private static Map<Integer, String> buildVarbitNameMap()
    {
        Map<Integer, String> out = new HashMap<>();
        try
        {
            // Reflect over VarbitID public static fields
            for (Field f : VarbitID.class.getFields())
            {
                int mods = f.getModifiers();
                if (!Modifier.isStatic(mods) || f.getType() != int.class)
                {
                    continue;
                }
                try
                {
                    int value = f.getInt(null);
                    String name = f.getName();
                    out.put(value, name);
                }
                catch (IllegalAccessException ignore)
                {
                    // should not happen for public fields; skip defensively
                }
            }
        }
        catch (Throwable t)
        {
            // Replace with your logger if available
            System.err.println("Failed to reflect VarbitID: " + t);
        }
        return Collections.unmodifiableMap(out);
    }

    private static int tierNumber(String varbitName)
    {
        if (varbitName.contains("EASY") || varbitName.contains("_EASY_") || varbitName.endsWith("_EASY_DONE")) return 1;
        if (varbitName.contains("MEDIUM") || varbitName.contains("_MED_") || varbitName.endsWith("_MED_DONE")) return 2;
        if (varbitName.contains("HARD") || varbitName.contains("_HARD_") || varbitName.endsWith("_HARD_DONE")) return 3;
        if (varbitName.contains("ELITE") || varbitName.contains("_ELITE_") || varbitName.endsWith("_ELITE_COMPLETE")) return 4;
        return -1;
    }

    private static String resolveRegion(String varbitName)
    {
        // try known regions first
        for (String region : DIARY_REGION_ITEM_BASE.keySet())
        {
            if (varbitName.startsWith(region + "_") || varbitName.startsWith(region + " "))
            {
                return region;
            }
        }

        // then aliases
        for (Map.Entry<String, String> e : DIARY_REGION_ALIASES.entrySet())
        {
            if (varbitName.startsWith(e.getKey() + "_") || varbitName.startsWith(e.getKey() + " "))
            {
                return e.getValue();
            }
        }

        // last resort: try prefix match (handles different naming patterns)
        for (String region : DIARY_REGION_ITEM_BASE.keySet())
        {
            if (varbitName.startsWith(region))
            {
                return region;
            }
        }

        for (String alias : DIARY_REGION_ALIASES.keySet())
        {
            if (varbitName.startsWith(alias))
            {
                return DIARY_REGION_ALIASES.get(alias);
            }
        }

        return null;
    }
}

