package com.oneshot.utils;


import com.google.common.collect.ImmutableList;
import net.runelite.api.clan.ClanRank;
import net.runelite.client.hiscore.HiscoreSkill;

import java.util.List;

import static net.runelite.client.hiscore.HiscoreSkill.*;


public class Constants {
    public static final String version = "0.2.0";

    public static final String PLUGIN_NAME = "One Shot";
    public static final String CLAN_NAME = "One Shot";
    public static final int DEFAULT_PRIORITY = 5;

    // configmanager for hcim scout
    public static final String CONFIG_KEY = "one-shot";
    public static final String CONFIG_KEY_SCOUT = "hcim-scout";
    public static final String CONFIG_KEY_SCOUT_TEXT = "hcim-scout-text";
    public static final String CONFIG_KEY_SCOUT_ICON = "hcim-scout-icon";
    public static final String CONFIG_KEY_SCOUT_HIDE_PLAYERS = "hcim-scout-hide-players";
    public static final String CONFIG_KEY_SCOUT_WILDERNESS_DISABLE = "hcim-scout-wilderness-disable";
    public static final String CONFIG_KEY_SCOUT_LOOKUP_COOLDOWN = "hcim-scout-lookup-cooldown";

    public static final String LINK_DISCORD = "https://www.discord.gg/one-shot";
    public static final String TIP_DISCORD = "Opens One Shot discord invitation on your browser";
    public static final String TIP_RANKS = "Not available in this version";
    public static final String TIP_INFO = "Show top players";
    public static final String TIP_SCOUT = "Open HCIM Scout Menu";

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

    public static final List<HiscoreSkill> ACTIVITIES = ImmutableList.of(
            CLUE_SCROLL_ALL, //LEAGUE_POINTS, LAST_MAN_STANDING,
            //SOUL_WARS_ZEAL, RIFTS_CLOSED, COLOSSEUM_GLORY,
            COLLECTIONS_LOGGED//, BOUNTY_HUNTER_ROGUE, BOUNTY_HUNTER_HUNTER,
            //PVP_ARENA_RANK
    );




    // WOM
//    "activities":{
//        "league_points":{
//            "metric":"league_points",
//                    "score":0,
//                    "rank":-1,
//                    "player":{
//                "id":138674,
//                        "username":"eddapt",
//                        "displayName":"Eddapt",
//                        "type":"hardcore",
//                        "build":"main",
//                        "status":"active",
//                        "country":null,
//                        "patron":false,
//                        "exp":1997479054,
//                        "ehp":7150.83862,
//                        "ehb":175.70793,
//                        "ttm":0.05968000000007123,
//                        "tt200m":12547.74380762925,
//                        "registeredAt":"2021-01-16T03:12:29.288Z",
//                        "updatedAt":"2025-11-24T16:06:17.783Z",
//                        "lastChangedAt":"2025-11-24T16:06:17.783Z",
//                        "lastImportedAt":"2023-07-10T16:39:35.547Z"
//            }
//        },
//        "bounty_hunter_hunter":{
//            "metric":"bounty_hunter_hunter",
//                    "score":1,
//                    "rank":-1,
//                    "player":{
//                "id":2108208,
//                        "username":"relo",
//                        "displayName":"Relo",
//                        "type":"hardcore",
//                        "build":"main",
//                        "status":"active",
//                        "country":null,
//                        "patron":false,
//                        "exp":171880156,
//                        "ehp":741.4951199999999,
//                        "ehb":419.26173,
//                        "ttm":978.1306500000001,
//                        "tt200m":18957.08730762924,
//                        "registeredAt":"2024-10-19T00:09:17.358Z",
//                        "updatedAt":"2025-11-24T22:25:15.662Z",
//                        "lastChangedAt":"2025-11-24T22:25:15.662Z",
//                        "lastImportedAt":null
//            }
//        },
//        "bounty_hunter_rogue":{
//            "metric":"bounty_hunter_rogue",
//                    "score":0,
//                    "rank":-1,
//                    "player":{
//                "id":138674,
//                        "username":"eddapt",
//                        "displayName":"Eddapt",
//                        "type":"hardcore",
//                        "build":"main",
//                        "status":"active",
//                        "country":null,
//                        "patron":false,
//                        "exp":1997479054,
//                        "ehp":7150.83862,
//                        "ehb":175.70793,
//                        "ttm":0.05968000000007123,
//                        "tt200m":12547.74380762925,
//                        "registeredAt":"2021-01-16T03:12:29.288Z",
//                        "updatedAt":"2025-11-24T16:06:17.783Z",
//                        "lastChangedAt":"2025-11-24T16:06:17.783Z",
//                        "lastImportedAt":"2023-07-10T16:39:35.547Z"
//            }
//        },
//        "clue_scrolls_all":{
//            "metric":"clue_scrolls_all",
//                    "score":2846,
//                    "rank":62,
//                    "player":{
//                "id":1143767,
//                        "username":"clog hunter",
//                        "displayName":"Clog Hunter",
//                        "type":"hardcore",
//                        "build":"main",
//                        "status":"active",
//                        "country":"LT",
//                        "patron":false,
//                        "exp":570103808,
//                        "ehp":2138.10709,
//                        "ehb":449.1913,
//                        "ttm":0.05588000000011561,
//                        "tt200m":17560.47533762924,
//                        "registeredAt":"2023-09-04T16:48:42.177Z",
//                        "updatedAt":"2025-11-24T22:43:52.351Z",
//                        "lastChangedAt":"2025-11-24T22:43:52.351Z",
//                        "lastImportedAt":null
//            }
//        },
//        "clue_scrolls_beginner":{
//            "metric":"clue_scrolls_beginner",
//                    "score":700,
//                    "rank":182,
//                    "player":{
//                "id":1095058,
//                        "username":"zenesis 2",
//                        "displayName":"Zenesis 2",
//                        "type":"hardcore",
//                        "build":"main",
//                        "status":"active",
//                        "country":null,
//                        "patron":false,
//                        "exp":78822805,
//                        "ehp":512.32677,
//                        "ehb":39.20178,
//                        "ttm":1198.92008,
//                        "tt200m":19186.25565762924,
//                        "registeredAt":"2023-07-23T19:37:50.238Z",
//                        "updatedAt":"2025-11-24T23:45:19.290Z",
//                        "lastChangedAt":"2025-11-23T23:44:06.801Z",
//                        "lastImportedAt":null
//            }
//        },
//        "clue_scrolls_easy":{
//            "metric":"clue_scrolls_easy",
//                    "score":765,
//                    "rank":125,
//                    "player":{
//                "id":1143767,
//                        "username":"clog hunter",
//                        "displayName":"Clog Hunter",
//                        "type":"hardcore",
//                        "build":"main",
//                        "status":"active",
//                        "country":"LT",
//                        "patron":false,
//                        "exp":570103808,
//                        "ehp":2138.10709,
//                        "ehb":449.1913,
//                        "ttm":0.05588000000011561,
//                        "tt200m":17560.47533762924,
//                        "registeredAt":"2023-09-04T16:48:42.177Z",
//                        "updatedAt":"2025-11-24T22:43:52.351Z",
//                        "lastChangedAt":"2025-11-24T22:43:52.351Z",
//                        "lastImportedAt":null
//            }
//        },
//        "clue_scrolls_medium":{
//            "metric":"clue_scrolls_medium",
//                    "score":692,
//                    "rank":128,
//                    "player":{
//                "id":1143767,
//                        "username":"clog hunter",
//                        "displayName":"Clog Hunter",
//                        "type":"hardcore",
//                        "build":"main",
//                        "status":"active",
//                        "country":"LT",
//                        "patron":false,
//                        "exp":570103808,
//                        "ehp":2138.10709,
//                        "ehb":449.1913,
//                        "ttm":0.05588000000011561,
//                        "tt200m":17560.47533762924,
//                        "registeredAt":"2023-09-04T16:48:42.177Z",
//                        "updatedAt":"2025-11-24T22:43:52.351Z",
//                        "lastChangedAt":"2025-11-24T22:43:52.351Z",
//                        "lastImportedAt":null
//            }
//        },
//        "clue_scrolls_hard":{
//            "metric":"clue_scrolls_hard",
//                    "score":683,
//                    "rank":16,
//                    "player":{
//                "id":1143767,
//                        "username":"clog hunter",
//                        "displayName":"Clog Hunter",
//                        "type":"hardcore",
//                        "build":"main",
//                        "status":"active",
//                        "country":"LT",
//                        "patron":false,
//                        "exp":570103808,
//                        "ehp":2138.10709,
//                        "ehb":449.1913,
//                        "ttm":0.05588000000011561,
//                        "tt200m":17560.47533762924,
//                        "registeredAt":"2023-09-04T16:48:42.177Z",
//                        "updatedAt":"2025-11-24T22:43:52.351Z",
//                        "lastChangedAt":"2025-11-24T22:43:52.351Z",
//                        "lastImportedAt":null
//            }
//        },
//        "clue_scrolls_elite":{
//            "metric":"clue_scrolls_elite",
//                    "score":163,
//                    "rank":36,
//                    "player":{
//                "id":1143767,
//                        "username":"clog hunter",
//                        "displayName":"Clog Hunter",
//                        "type":"hardcore",
//                        "build":"main",
//                        "status":"active",
//                        "country":"LT",
//                        "patron":false,
//                        "exp":570103808,
//                        "ehp":2138.10709,
//                        "ehb":449.1913,
//                        "ttm":0.05588000000011561,
//                        "tt200m":17560.47533762924,
//                        "registeredAt":"2023-09-04T16:48:42.177Z",
//                        "updatedAt":"2025-11-24T22:43:52.351Z",
//                        "lastChangedAt":"2025-11-24T22:43:52.351Z",
//                        "lastImportedAt":null
//            }
//        },
//        "clue_scrolls_master":{
//            "metric":"clue_scrolls_master",
//                    "score":37,
//                    "rank":116,
//                    "player":{
//                "id":1143767,
//                        "username":"clog hunter",
//                        "displayName":"Clog Hunter",
//                        "type":"hardcore",
//                        "build":"main",
//                        "status":"active",
//                        "country":"LT",
//                        "patron":false,
//                        "exp":570103808,
//                        "ehp":2138.10709,
//                        "ehb":449.1913,
//                        "ttm":0.05588000000011561,
//                        "tt200m":17560.47533762924,
//                        "registeredAt":"2023-09-04T16:48:42.177Z",
//                        "updatedAt":"2025-11-24T22:43:52.351Z",
//                        "lastChangedAt":"2025-11-24T22:43:52.351Z",
//                        "lastImportedAt":null
//            }
//        },
//        "last_man_standing":{
//            "metric":"last_man_standing",
//                    "score":3250,
//                    "rank":386,
//                    "player":{
//                "id":1157310,
//                        "username":"fokusert",
//                        "displayName":"Fokusert",
//                        "type":"hardcore",
//                        "build":"main",
//                        "status":"active",
//                        "country":null,
//                        "patron":false,
//                        "exp":135430201,
//                        "ehp":737.93444,
//                        "ehb":37.23483999999999,
//                        "ttm":985.2838899999999,
//                        "tt200m":18960.64798762924,
//                        "registeredAt":"2023-09-15T18:49:42.813Z",
//                        "updatedAt":"2025-11-24T16:15:32.740Z",
//                        "lastChangedAt":"2025-11-20T15:58:22.585Z",
//                        "lastImportedAt":null
//            }
//        },
//        "pvp_arena":{
//            "metric":"pvp_arena",
//                    "score":5333,
//                    "rank":17,
//                    "player":{
//                "id":2014927,
//                        "username":"universiteit",
//                        "displayName":"Universiteit",
//                        "type":"hardcore",
//                        "build":"main",
//                        "status":"active",
//                        "country":"NL",
//                        "patron":false,
//                        "exp":43545415,
//                        "ehp":337.09131,
//                        "ehb":2.19268,
//                        "ttm":1374.15554,
//                        "tt200m":19361.49111762924,
//                        "registeredAt":"2024-08-25T12:37:43.122Z",
//                        "updatedAt":"2025-11-25T11:05:32.327Z",
//                        "lastChangedAt":"2025-11-23T10:55:13.115Z",
//                        "lastImportedAt":null
//            }
//        },
//        "soul_wars_zeal":{
//            "metric":"soul_wars_zeal",
//                    "score":72989,
//                    "rank":66,
//                    "player":{
//                "id":2346671,
//                        "username":"racquetball",
//                        "displayName":"Racquetball",
//                        "type":"hardcore",
//                        "build":"main",
//                        "status":"active",
//                        "country":null,
//                        "patron":false,
//                        "exp":12465184,
//                        "ehp":82.31778000000001,
//                        "ehb":0,
//                        "ttm":1628.92907,
//                        "tt200m":19616.26464762924,
//                        "registeredAt":"2025-04-06T21:43:51.595Z",
//                        "updatedAt":"2025-11-24T15:45:45.821Z",
//                        "lastChangedAt":"2025-11-19T15:20:17.216Z",
//                        "lastImportedAt":null
//            }
//        },
//        "guardians_of_the_rift":{
//            "metric":"guardians_of_the_rift",
//                    "score":2228,
//                    "rank":108,
//                    "player":{
//                "id":2170343,
//                        "username":"hardironbwc",
//                        "displayName":"HardIronBWC",
//                        "type":"hardcore",
//                        "build":"main",
//                        "status":"active",
//                        "country":null,
//                        "patron":false,
//                        "exp":95342039,
//                        "ehp":546.85632,
//                        "ehb":3.49242,
//                        "ttm":1166.14992,
//                        "tt200m":19151.72610762925,
//                        "registeredAt":"2024-12-09T00:20:37.658Z",
//                        "updatedAt":"2025-11-24T17:15:43.705Z",
//                        "lastChangedAt":"2025-11-24T17:15:43.705Z",
//                        "lastImportedAt":null
//            }
//        },
//        "colosseum_glory":{
//            "metric":"colosseum_glory",
//                    "score":38759,
//                    "rank":37,
//                    "player":{
//                "id":1946232,
//                        "username":"l am evil",
//                        "displayName":"l AM EVIL",
//                        "type":"hardcore",
//                        "build":"main",
//                        "status":"active",
//                        "country":"NL",
//                        "patron":false,
//                        "exp":222530364,
//                        "ehp":846.56379,
//                        "ehb":308.6791,
//                        "ttm":873.18477,
//                        "tt200m":18852.01863762924,
//                        "registeredAt":"2024-07-12T15:10:12.857Z",
//                        "updatedAt":"2025-11-24T22:51:18.828Z",
//                        "lastChangedAt":"2025-11-24T22:51:18.828Z",
//                        "lastImportedAt":null
//            }
//        },
//        "collections_logged":{
//            "metric":"collections_logged",
//                    "score":1109,
//                    "rank":15,
//                    "player":{
//                "id":1143767,
//                        "username":"clog hunter",
//                        "displayName":"Clog Hunter",
//                        "type":"hardcore",
//                        "build":"main",
//                        "status":"active",
//                        "country":"LT",
//                        "patron":false,
//                        "exp":570103808,
//                        "ehp":2138.10709,
//                        "ehb":449.1913,
//                        "ttm":0.05588000000011561,
//                        "tt200m":17560.47533762924,
//                        "registeredAt":"2023-09-04T16:48:42.177Z",
//                        "updatedAt":"2025-11-24T22:43:52.351Z",
//                        "lastChangedAt":"2025-11-24T22:43:52.351Z",
//                        "lastImportedAt":null
//            }
//        }



}