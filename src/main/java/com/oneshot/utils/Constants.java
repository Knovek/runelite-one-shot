package com.oneshot.utils;


import com.google.common.collect.ImmutableList;
import net.runelite.api.clan.ClanRank;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.hiscore.HiscoreSkill;

import java.awt.*;
import java.util.*;
import java.util.List;

import static net.runelite.client.hiscore.HiscoreSkill.*;


public class Constants {
    public static final String PLUGIN_NAME = "One Shot";
    public static final String CLAN_NAME = "One Shot";
    public static final int DEFAULT_PRIORITY = 5;

    public static String WORKER_URL = "https://runelite-webhook-api.pqnarte.workers.dev/send";

    // Main Panel Buttons
    public static final String LINK_DISCORD = "https://www.discord.gg/one-shot";
    public static final String TIP_DISCORD = "Opens One Shot discord invitation on your browser";
    public static final String TIP_LEADERBOARDS = "Check One Shot leaderboards";
    public static final String TIP_ROLES = "Show clan roles";
    public static final String TIP_MODTOOLS = "Open mod tools";
    public static final int BUTTON_NUMBER = 4;
    public static final int BUTTON_SIZE = 40;
    public static final int TEXT_ICON_SIZE = 12;

    // WiseOldMan Links
    public static final String URI_WOM_LEADERS = "https://api.wiseoldman.net/v2/groups/2647/statistics";
    public static final String URI_WOM_LEADERS_OBJECT = "metricLeaders";
    public static final String URI_WOM_SKILL_LEADERS = "https://api.wiseoldman.net/v2/groups/2647/hiscores?metric=";
    public static final String URI_WOM_SKILL_LEADERS_LIMIT = "&limit=500";

    // clan moderators
    public static final ClanRank RANK_OWNER = new ClanRank(126);
    public static final ClanRank RANK_DEPUTY_OWNER = new ClanRank(125);
    public static final ClanRank RANK_ASTRAL = new ClanRank(120);
    public static final ClanRank RANK_CAPTAIN = new ClanRank(115);

    // Discord Embeds
    public static final Color DISCORD_LEVELS_COLOR = EmbedColors.NEON_GREEN;
    public static final Color DISCORD_DIARIES_COLOR = EmbedColors.WHITE;
    public static final Color DISCORD_COMBAT_ACHIEVEMENTS_COLOR = EmbedColors.PURPLE;
    public static final Color DISCORD_QUESTS_COLOR = EmbedColors.NEON_BLUE;
    public static final Color DISCORD_PETS_COLOR = EmbedColors.NEON_PINK;
    public static final Color DISCORD_LOOT_COLOR = EmbedColors.NEON_YELLOW;
    public static final Color DISCORD_DEATHS_COLOR = EmbedColors.DARK_RED;

    public static final int DISCORD_THUMBNAIL_SIZE = 25;
    public static final int DISCORD_AUTHOR_ICON_SIZE = 20;
    public static final double DISCORD_AUTHOR_ICON_SCALE = 0.7;

    public static final String WIKI_SEARCH = "https://oldschool.runescape.wiki/w/Special:Search?search=";
    public static final String WIKI_COMBAT_ACHIEVEMENTS_REWARDS = "https://oldschool.runescape.wiki/w/Combat_Achievements#Rewards";



    //Real skills, ordered in the way they should be displayed in the panel.
    public static final List<HiscoreSkill> SKILLS = ImmutableList.of(
            ATTACK, HITPOINTS, MINING,
            STRENGTH, AGILITY, SMITHING,
            DEFENCE, HERBLORE, FISHING,
            RANGED, THIEVING, COOKING,
            PRAYER, CRAFTING, FIREMAKING,
            MAGIC, FLETCHING, WOODCUTTING,
            RUNECRAFT, SLAYER, FARMING,
            CONSTRUCTION, HUNTER, SAILING
    );

    //Bosses, ordered in the way they should be displayed in the panel.
    public static final List<HiscoreSkill> BOSSES = ImmutableList.of(
            ABYSSAL_SIRE, ALCHEMICAL_HYDRA, AMOXLIATL,
            ARAXXOR, ARTIO, BARROWS_CHESTS,
            BRYOPHYTA, CALLISTO, CALVARION,
            CERBERUS, CHAMBERS_OF_XERIC, CHAMBERS_OF_XERIC_CHALLENGE_MODE,
            CHAOS_ELEMENTAL, CHAOS_FANATIC, COMMANDER_ZILYANA,
            CORPOREAL_BEAST, CRAZY_ARCHAEOLOGIST, DAGANNOTH_PRIME,
            DAGANNOTH_REX, DAGANNOTH_SUPREME, DERANGED_ARCHAEOLOGIST,
            DOOM_OF_MOKHAIOTL, DUKE_SUCELLUS, GENERAL_GRAARDOR,
            GIANT_MOLE, GROTESQUE_GUARDIANS, HESPORI,
            KALPHITE_QUEEN, KING_BLACK_DRAGON, KRAKEN,
            KREEARRA, KRIL_TSUTSAROTH, LUNAR_CHESTS,
            MIMIC, NEX, NIGHTMARE,
            PHOSANIS_NIGHTMARE, OBOR, PHANTOM_MUSPAH,
            SARACHNIS, SCORPIA, SCURRIUS,
            SHELLBANE_GRYPHON, SKOTIZO, SOL_HEREDIT,
            SPINDEL, TEMPOROSS, THE_GAUNTLET,
            THE_CORRUPTED_GAUNTLET, THE_HUEYCOATL, THE_LEVIATHAN,
            THE_ROYAL_TITANS, THE_WHISPERER, THEATRE_OF_BLOOD,
            THEATRE_OF_BLOOD_HARD_MODE, THERMONUCLEAR_SMOKE_DEVIL, TOMBS_OF_AMASCUT,
            TOMBS_OF_AMASCUT_EXPERT, TZKAL_ZUK, TZTOK_JAD,
            VARDORVIS, VENENATIS, VETION,
            VORKATH, WINTERTODT, YAMA,
            ZALCANO, ZULRAH
    );

    // Activities
    public static final List<HiscoreSkill> ACTIVITIES = ImmutableList.of(
            CLUE_SCROLL_ALL, //LEAGUE_POINTS, LAST_MAN_STANDING,
            //SOUL_WARS_ZEAL, RIFTS_CLOSED, COLOSSEUM_GLORY,
            COLLECTIONS_LOGGED//, BOUNTY_HUNTER_ROGUE, BOUNTY_HUNTER_HUNTER,
            //PVP_ARENA_RANK
    );

    public static final List<Integer> ACHIEVEMENT_DIARIES_COMPLETE_VARBITS = List.of(
            VarbitID.ARDOUGNE_DIARY_EASY_COMPLETE,
            VarbitID.ARDOUGNE_DIARY_MEDIUM_COMPLETE,
            VarbitID.ARDOUGNE_DIARY_HARD_COMPLETE,
            VarbitID.ARDOUGNE_DIARY_ELITE_COMPLETE,
            VarbitID.FALADOR_DIARY_EASY_COMPLETE,
            VarbitID.FALADOR_DIARY_MEDIUM_COMPLETE,
            VarbitID.FALADOR_DIARY_HARD_COMPLETE,
            VarbitID.FALADOR_DIARY_ELITE_COMPLETE,
            VarbitID.WILDERNESS_DIARY_EASY_COMPLETE,
            VarbitID.WILDERNESS_DIARY_MEDIUM_COMPLETE,
            VarbitID.WILDERNESS_DIARY_HARD_COMPLETE,
            VarbitID.WILDERNESS_DIARY_ELITE_COMPLETE,
            VarbitID.WESTERN_DIARY_EASY_COMPLETE,
            VarbitID.WESTERN_DIARY_MEDIUM_COMPLETE,
            VarbitID.WESTERN_DIARY_HARD_COMPLETE,
            VarbitID.WESTERN_DIARY_ELITE_COMPLETE,
            VarbitID.KANDARIN_DIARY_EASY_COMPLETE,
            VarbitID.KANDARIN_DIARY_MEDIUM_COMPLETE,
            VarbitID.KANDARIN_DIARY_HARD_COMPLETE,
            VarbitID.KANDARIN_DIARY_ELITE_COMPLETE,
            VarbitID.VARROCK_DIARY_EASY_COMPLETE,
            VarbitID.VARROCK_DIARY_MEDIUM_COMPLETE,
            VarbitID.VARROCK_DIARY_HARD_COMPLETE,
            VarbitID.VARROCK_DIARY_ELITE_COMPLETE,
            VarbitID.DESERT_DIARY_EASY_COMPLETE,
            VarbitID.DESERT_DIARY_MEDIUM_COMPLETE,
            VarbitID.DESERT_DIARY_HARD_COMPLETE,
            VarbitID.DESERT_DIARY_ELITE_COMPLETE,
            VarbitID.MORYTANIA_DIARY_EASY_COMPLETE,
            VarbitID.MORYTANIA_DIARY_MEDIUM_COMPLETE,
            VarbitID.MORYTANIA_DIARY_HARD_COMPLETE,
            VarbitID.MORYTANIA_DIARY_ELITE_COMPLETE,
            VarbitID.FREMENNIK_DIARY_EASY_COMPLETE,
            VarbitID.FREMENNIK_DIARY_MEDIUM_COMPLETE,
            VarbitID.FREMENNIK_DIARY_HARD_COMPLETE,
            VarbitID.FREMENNIK_DIARY_ELITE_COMPLETE,
            VarbitID.LUMBRIDGE_DIARY_EASY_COMPLETE,
            VarbitID.LUMBRIDGE_DIARY_MEDIUM_COMPLETE,
            VarbitID.LUMBRIDGE_DIARY_HARD_COMPLETE,
            VarbitID.LUMBRIDGE_DIARY_ELITE_COMPLETE,
            VarbitID.KOUREND_DIARY_EASY_COMPLETE,
            VarbitID.KOUREND_DIARY_MEDIUM_COMPLETE,
            VarbitID.KOUREND_DIARY_HARD_COMPLETE,
            VarbitID.KOUREND_DIARY_ELITE_COMPLETE,
            VarbitID.KARAMJA_DIARY_ELITE_COMPLETE,
            VarbitID.ATJUN_EASY_DONE,
            VarbitID.ATJUN_MED_DONE,
            VarbitID.ATJUN_HARD_DONE
    );

    // Pets
    public static final Set<String> Pets = Set.of(
            "Abyssal orphan",
            "Ikkle hydra",
            "Chompy chick",
            "Quetzin",
            "Herbi",
            "Rock golem",
            "Rocky",
            "Beaver",
            "Heron",
            "Tangleroot",
            "Giant squirrel",
            "Rift guardian",
            "Baby chinchompa",
            "Moxi",
            "Nid",
            "Pet penance queen",
            "Callisto cub",
            "Hellpuppy",
            "Olmlet",
            "Pet chaos elemental",
            "Pet zilyana",
            "Pet dark core",
            "Pet dagannoth rex",
            "Pet dagannoth supreme",
            "Pet dagannoth prime",
            "Dom",
            "Baron",
            "Smol heredit",
            "Pet general graardor",
            "Baby mole",
            "Noon",
            "Abyssal protector",
            "Kalphite princess",
            "Prince black dragon",
            "Pet kraken",
            "Pet kree'arra",
            "Pet k'ril tsutsaroth",
            "Bloodhound",
            "Nexling",
            "Muphin",
            "Bran",
            "Sraracha",
            "Scorpia's offspring",
            "Scurry",
            "Gull (pet)",
            "Soup",
            "Skotos",
            "Lil' creator",
            "Tiny tempor",
            "Tzrek-jad",
            "Youngllef",
            "Huberte",
            "Jal-nib-rek",
            "Lil'viathan",
            "Little nightmare",
            "Wisp",
            "Lil' zik",
            "Pet smoke devil",
            "Tumeken's guardian",
            "Butch",
            "Venenatis spiderling",
            "Vet'ion jr.",
            "Vorki",
            "Phoenix",
            "Yami",
            "Smolcano",
            "Pet snakeling"
    );

    // Items whitelisting and blacklisting
    public static final Set<String> ITEMS_WHITELIST = Set.of(
            "Golden tench",
            "Hydra leather",
            "Hydra's claw",
            "Noxious point",
            "Noxious blade",
            "Noxious pommel",
            "Araxyte fang",
            "Fighter torso",
            "Ahrim's robetop",
            "Karil's leathertop",
            "Ahrim's robeskirt",
            "Karil's leatherskirt",
            "Bryophyta's essence",
            "Voidwaker hilt",
            "Dragon pickaxe",
            "Pegasian crystal",
            "Eternal crystal",
            "Primordial crystal",
            "Arcane prayer scroll",
            "Dinh's bulwark",
            "Twisted buckler",
            "Dragon hunter crossbow",
            "Dexterous prayer scroll",
            "Dragon claws",
            "Ancestral hat",
            "Kodai insignia",
            "Elder maul",
            "Ancestral robe bottom",
            "Ancestral robe top",
            "Twisted bow",
            "Giant champion scroll",
            "Goblin champion scroll",
            "Skeleton champion scroll",
            "Zombie champion scroll",
            "Imp champion scroll",
            "Lesser demon champion scroll",
            "Hobgoblin champion scroll",
            "Ghoul champion scroll",
            "Earth warrior champion scroll",
            "Jogre champion scroll",
            "Champion's cape",
            "Saradomin hilt",
            "Armadyl crossbow",
            "Holy elixir",
            "Spectral sigil",
            "Arcane sigil",
            "Elysian sigil",
            "Dragon defender",
            "Berserker ring",
            "Dragon axe",
            "Avernic treads",
            "Eye of ayak",
            "Mokhaiotl cloth",
            "Virtus mask",
            "Virtus robe bottom",
            "Virtus robe top",
            "Gilded d'hide vambraces",
            "Gilded axe",
            "Gilded d'hide chaps",
            "Gilded scimitar",
            "3rd age cloak",
            "3rd age wand",
            "3rd age bow",
            "3rd age longsword",
            "Sunfire fanatic chausses",
            "Tonalztics of ralos (uncharged)",
            "Dizana's quiver (uncharged)",
            "Bandos hilt",
            "Bandos tassets",
            "Bandos chestplate",
            "Zenyte shard",
            "Granite hammer",
            "Ring of endurance (uncharged)",
            "Gilded 2h sword",
            "Gilded full helm",
            "Gilded platelegs",
            "Gilded kiteshield",
            "Gilded platebody",
            "3rd age vambraces",
            "3rd age range coif",
            "3rd age range legs",
            "3rd age mage hat",
            "3rd age range top",
            "3rd age robe",
            "3rd age plateskirt",
            "3rd age full helmet",
            "3rd age amulet",
            "3rd age kiteshield",
            "3rd age platelegs",
            "3rd age robe top",
            "3rd age platebody",
            "Bottomless compost bucket",
            "Trident of the seas (full)",
            "Armadyl hilt",
            "Armadyl helmet",
            "Armadyl chainskirt",
            "Armadyl chestplate",
            "Zamorakian spear",
            "Staff of the dead",
            "Zamorak hilt",
            "Victor's cape (1000)",
            "3rd age druidic staff",
            "3rd age druidic cloak",
            "3rd age axe",
            "3rd age druidic robe bottoms",
            "3rd age pickaxe",
            "3rd age druidic robe top",
            "Ranger boots",
            "Broken zombie axe",
            "Dragon warhammer",
            "Amulet of eternal glory",
            "Dragon full helm",
            "Dragon metal slice",
            "Eclipse atlatl",
            "Dual macuahuitl",
            "Ancient hilt",
            "Zaryte vambraces",
            "Torva platelegs (damaged)",
            "Torva platebody (damaged)",
            "Torva full helm (damaged)",
            "Nihil horn",
            "Venator shard",
            "Stale baguette",
            "Ancient effigy",
            "Craw's bow (u)",
            "Ancient relic",
            "Dragon cannon barrel",
            "Bottled storm",
            "Broken dragon hook",
            "Belle's folly (tarnished)",
            "Leaf-bladed battleaxe",
            "Blood shard",
            "Horn of plenty (empty)",
            "Eternal gem",
            "Aquanite tendon",
            "Basilisk jaw",
            "Wyvern visage",
            "Imbued heart",
            "Black mask (10)",
            "Fish barrel",
            "Dragon harpoon",
            "Fire cape",
            "Crystal armour seed",
            "Enhanced crystal weapon seed",
            "Dragon hunter wand",
            "Infernal cape",
            "Inquisitor's great helm",
            "Nightmare staff",
            "Volatile orb",
            "Inquisitor's hauberk",
            "Inquisitor's plateskirt",
            "Eldritch orb",
            "Inquisitor's mace",
            "Harmonised orb",
            "Justiciar legguards",
            "Justiciar chestguard",
            "Justiciar faceguard",
            "Sanguinesti staff (uncharged)",
            "Ghrazi rapier",
            "Avernic defender hilt",
            "Scythe of vitur (uncharged)",
            "Occult necklace",
            "Lightbearer",
            "Masori mask",
            "Osmumten's fang",
            "Masori chaps",
            "Masori body",
            "Tumeken's shadow (uncharged)",
            "Thread of elidinis",
            "Tormented synapse",
            "Voidwaker gem",
            "Dragon pickaxe (broken)",
            "Skeletal visage",
            "Vorkath's head",
            "Oathplate helm",
            "Oathplate legs",
            "Oathplate chest",
            "Crystal tool seed",
            "Serpentine visage",
            "Magic fang",
            "Tanzanite fang"
    );

    // Combat Achievements
    public static final Map<String, String> COMBAT_ACHIEVEMENT_REWARDS_IMAGE_URL = Map.of(
//            "Easy", "https://oldschool.runescape.wiki/images/Combat_Achievements_-_easy_tier_icon.png",
//            "Medium", "https://oldschool.runescape.wiki/images/Combat_Achievements_-_medium_tier_icon.png",
//            "Hard", "https://oldschool.runescape.wiki/images/Combat_Achievements_-_hard_tier_icon.png",
            "Elite", "https://oldschool.runescape.wiki/images/Combat_Achievements_-_elite_tier_icon.png",
            "Master", "https://oldschool.runescape.wiki/images/Combat_Achievements_-_master_tier_icon.png",
            "Grandmaster", "https://oldschool.runescape.wiki/images/Combat_Achievements_-_grandmaster_tier_icon.png"
    );

    // Achievement Diaries
//    public static final Map<String, String> ELITE_ACHIEVEMENT_DIARIES_IMAGE_URL;
//
//    static
//    {
//        Map<Integer, String> map = new HashMap<>();
//
//        map.put(VarbitID.ARDOUGNE_DIARY_ELITE_COMPLETE, "https://oldschool.runescape.wiki/images/Ardougne_cloak_4_detail.png");
//        map.put(VarbitID.DESERT_DIARY_ELITE_COMPLETE, "https://oldschool.runescape.wiki/images/Desert_amulet_4_detail.png");
//        map.put(VarbitID.FALADOR_DIARY_ELITE_COMPLETE, "https://oldschool.runescape.wiki/images/Falador_shield_4_detail.png");
//        map.put(VarbitID.FREMENNIK_DIARY_ELITE_COMPLETE, "https://oldschool.runescape.wiki/images/Fremennik_sea_boots_4_detail.png");
//        map.put(VarbitID.KANDARIN_DIARY_ELITE_COMPLETE, "https://oldschool.runescape.wiki/images/Kandarin_headgear_4_detail.png");
//        map.put(VarbitID.KARAMJA_DIARY_ELITE_COMPLETE, "https://oldschool.runescape.wiki/images/Karamja_gloves_4_detail.png");
//        map.put("Kourend & Kebos", "https://oldschool.runescape.wiki/images/Radas_blessing_4_detail.png");
//        map.put("Lumbridge & Draynor", "https://oldschool.runescape.wiki/images/Explorers_ring_4_detail.png");
//        map.put("Morytania", "https://oldschool.runescape.wiki/images/Morytania_legs_4_detail.png");
//        map.put("Varrock", "https://oldschool.runescape.wiki/images/Varrock_armour_4_detail.png");
//        map.put("Western Provinces", "https://oldschool.runescape.wiki/images/Western_banner_4_detail.png");
//        map.put("Wilderness", "https://oldschool.runescape.wiki/images/Wilderness_sword_4_detail.png");
//
//        ELITE_ACHIEVEMENT_DIARIES_IMAGE_URL = Collections.unmodifiableMap(map);
//    }


    // Grandmaster quests
    public static final Set<String> GM_QUESTS = Set.of(
            "Desert Treasure II - The Fallen Empire",
            "Dragon Slayer II",
            "Monkey Madness II",
            "Song of the Elves",
            "While Guthix Sleeps");
}