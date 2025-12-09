package com.oneshot;

import net.runelite.client.config.*;


@ConfigGroup(OneShotConfig.GROUP)
public interface OneShotConfig extends Config
{

    String GROUP = "oneshot";

    @ConfigItem(
            keyName = "version",
            position = 0,
            name = "version",
            description = "version")
    default String version() {return "v1.0.1"; }

    @ConfigSection(
            name = "Discord Announcements",
            description = "Which achievements should be posted in One Shot discord?",
            position = 1
    )
    String DISCORD_SECTION = "Game Achievements";

    @ConfigItem(
            keyName = "announceLevel",
            name = "Announce Level 99 or 200M XP",
            description = "Should we announce when you reach Level 99 or 200M XP in a skill to discord #achievements?",
            section = DISCORD_SECTION,
            position = 3
    )
    default boolean announceLevel(){
        return true;
    }

    @ConfigItem(
            keyName = "announceelites",
            name = "Announce Elite Diaries",
            description = "Should we announce when you complete Elite Diaries to discord #achievements?",
            section = DISCORD_SECTION,
            position = 4
    )
    default boolean announceelites(){
        return true;
    }

    @ConfigItem(
            keyName = "announcegmquests",
            name = "Announce Grandmaster Quests",
            description = "Should we announce when you complete Grandmaster quests to discord #achievements?",
            section = DISCORD_SECTION,
            position = 5
    )
    default boolean announcegmquests(){
        return true;
    }

    @ConfigItem(
            keyName = "announcepets",
            name = "Announce Pets",
            description = "Should we announce when you receive a new pet in #loot-drop",
            section = DISCORD_SECTION,
            position = 7
    )
    default boolean announcepets(){
        return true;
    }

    @ConfigItem(
            keyName = "announceloot",
            name = "Announce Poggers Collection Logs",
            description = "Should we announce when you receive a poggers collection log to discord #loot-drop",
            section = DISCORD_SECTION,
            position = 8
    )
    default boolean announceloot(){
        return true;
    }


    @ConfigItem(
            keyName = "announcedeaths",
            name = "Announce Deaths",
            description = "Should we announce when you die to discord #deaths?",
            section = DISCORD_SECTION,
            position = 9
    )
    default boolean announcedeaths(){
        return true;
    }


    @ConfigItem(
            keyName = "announcecas",
            name = "Announce Combat Achievements Tier Rewards",
            description = "Should we announce when you unlock a new elite/master/grandmaster combat achievement tier reward?",
            section = DISCORD_SECTION,
            position = 10
    )
    default boolean announcecas(){
        return true;
    }



    @ConfigSection(
            name = "Discord Options",
            description = "To customize your announcements a bit more",
            position = 1
    )
    String DISCORD_SECTION_OPTIONS = "Discord Options";

    @ConfigItem(
            keyName = "uploadscreenshots",
            name = "Upload screenshots",
            description = "Should we include a screenshot of your achievements?",
            section = DISCORD_SECTION_OPTIONS,
            position = 1
    )
    default boolean uploadscreenshots(){
        return true;
    }

    @ConfigItem(
            keyName = "hidechat",
            name = "Hide chats",
            description = "Hides chat when sending screenshots",
            section = DISCORD_SECTION_OPTIONS,
            position = 2
    )
    default boolean hidechats(){
        return true;
    }


    @ConfigItem(
            keyName = "uploadTotalQuestPoints",
            name = "Include quest stats",
            description = "Should we include total quests done and quest points when announcing a quest completion?",
            section = DISCORD_SECTION_OPTIONS,
            position = 3
    )
    default boolean uploadTotalQuestPoints(){
        return true;
    }

    @ConfigItem(
            keyName = "webhooks",
            name = "Webhooks",
            description = "You can get this info from discord",
            section = DISCORD_SECTION_OPTIONS,
            position = 4,
            secret = true
    )
    default String webhooks() {return "";}


    @ConfigItem(
            keyName = "hcimscoutEnable",
            name = "Enable Scouting",
            description = "Enable to start scouting HCIM players",
            hidden = true
    )
    default boolean hcimscoutEnable(){
        return false;
    }


    @ConfigItem(
            keyName = "hcimscoutMinimumTotal",
            name = "Total scout filter",
            description = "Set the desired minimum total level",
            hidden = true
    )
    @Range(min = 1, max = 3000)
    default int hcimscoutMinimumTotal(){
        return 1000;
    }


    @ConfigItem(
            keyName = "hcimscoutHide",
            name = "Hide non-HCIM players",
            description = "Enable to hide every player that is not an hardcore ironman",
            hidden = true
    )
    default boolean hcimscoutHide(){
        return false;
    }

    @ConfigItem(
            keyName = "hcimscoutRedHelm",
            name = "Draw Helm",
            description = "Draw Red Helms above HCIM Players",
            hidden = true
    )
    default boolean hcimscoutRedHelm(){
        return false;
    }

    @ConfigItem(
            keyName = "hcimscoutRedText",
            name = "Draw Text",
            description = "Draw Red text above HCIM Players",
            hidden = true
    )
    default boolean hcimscoutRedText(){
        return false;
    }

    @ConfigItem(
            keyName = "hcimscoutWilderness",
            name = "Disable in Wilderness",
            description = "Enable to prevent entity hider from working in wilderness",
            hidden = true
    )
    default boolean hcimscoutWilderness(){
        return true;
    }

    @ConfigItem(
            keyName = "lookupCooldown",
            name = "Lookup cooldown",
            description = "Lookup cooldown (ticks)",
            hidden = true
    )
    @Range(min = 1, max = 20)
    default int lookupCooldown(){ return 2; }


}
