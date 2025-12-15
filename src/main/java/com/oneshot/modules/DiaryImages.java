package com.oneshot.modules;

import net.runelite.api.gameval.VarbitID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public final class DiaryImages
{
    private static final Logger log = LoggerFactory.getLogger(DiaryImages.class);
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
}

