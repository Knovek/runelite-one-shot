package com.oneshot;

import net.runelite.client.config.*;


@ConfigGroup(OneShotConfig.GROUP)
public interface OneShotConfig extends Config
{

    String GROUP = "oneshot";

    @ConfigSection(
            name = "Discord Achievements",
            description = "Which achievements should be posted in One Shot discord?",
            position = 1,
            closedByDefault = true
    )
    String SECTION_ACHIEVEMENTS = "Game Achievements";

    @ConfigItem(
            keyName = "uploadscreenshots",
            name = "Upload screenshots",
            description = "Should we include a screenshot of your achievements?",
            section = SECTION_ACHIEVEMENTS,
            position = 1
    )
    default boolean uploadscreenshots(){
        return true;
    }

    @ConfigItem(
            keyName = "announce99",
            name = "Announce Level 99",
            description = "Should we announce when you reach Level 99 in a skill to discord #achievements?",
            section = SECTION_ACHIEVEMENTS,
            position = 2
    )
    default boolean announce99(){
        return false;
    }

    @ConfigItem(
            keyName = "announcemaxed",
            name = "Announce Maxed",
            description = "Should we announce when you reach Level 99 in all skills to discord #achievements?",
            section = SECTION_ACHIEVEMENTS,
            position = 3
    )
    default boolean announcemaxed(){
        return false;
    }

    @ConfigItem(
            keyName = "announcegmquests",
            name = "Announce Grandmaster Quests",
            description = "Should we announce when you complete Grandmaster quests to discord #achievements?",
            section = SECTION_ACHIEVEMENTS,
            position = 4
    )
    default boolean announcegmquests(){
        return false;
    }

    @ConfigItem(
            keyName = "announceelites",
            name = "Announce Elite Diaries",
            description = "Should we announce when you complete Elite Diaries to discord #achievements?",
            section = SECTION_ACHIEVEMENTS,
            position = 5
    )
    default boolean announceelites(){
        return false;
    }

    @ConfigItem(
            keyName = "announcedeaths",
            name = "Announce Deaths",
            description = "Should we announce when you die to discord #deaths?",
            section = SECTION_ACHIEVEMENTS,
            position = 6
    )
    default boolean announcedeaths(){
        return true;
    }

    @ConfigSection(
            name = "HCIM Scout",
            description = "Enable to hide every non-HCIM player",
            position = 2,
            closedByDefault = true
    )
    String SECTION_SCOUT = "HCIM Scout";

    @ConfigItem(
            keyName = "hcimscoutHide",
            name = "Hide non-HCIM players",
            description = "Enable to hide every player that is not an hardcore ironman",
            section = SECTION_SCOUT,
            position = 1
    )
    default boolean hcimscoutHide(){
        return false;
    }

    @ConfigItem(
            keyName = "hcimscoutRedHelm",
            name = "Draw Helm",
            description = "Draw Red Helms above HCIM Players",
            section = SECTION_SCOUT,
            position = 2
    )
    default boolean hcimscoutRedHelm(){
        return false;
    }

    @ConfigItem(
            keyName = "hcimscoutRedText",
            name = "Draw Text",
            description = "Draw Red text above HCIM Players",
            section = SECTION_SCOUT,
            position = 3
    )
    default boolean hcimscoutRedHelm(){
        return false;
    }

    @ConfigItem(
            keyName = "hcimscoutWilderness",
            name = "Disable in Wilderness",
            description = "Enable to prevent entity hider from working in wilderness",
            section = SECTION_SCOUT,
            position = 4
    )
    default boolean hcimscoutWilderness(){
        return true;
    }

    @ConfigItem(
            keyName = "hcimscoutCooldown",
            name = "Lookup cooldown",
            description = "Ticks between each lookup cooldown, to avoid spamming API",
            section = SECTION_SCOUT,
            position = 4
    )
    @Range(
            min = 1,
            max = 20
    )
    default int lookupCooldown()
    {
        return 2;
    }

    @ConfigItem(
            keyName = "cacheDuration",
            name = "Lookup cache duration",
            description = "Duration in minutes to cache player status",
            section = SECTION_SCOUT,
            position = 5
    )
    @Range(
            min = 10,
            max = 360
    )
    default int cacheDuration()
    {
        return 360;
    }


    @ConfigSection(
            name = "Default World 507",
            description = "Enable to default world to 507 when starting RuneLite",
            position = 3,
            closedByDefault = true
    )
    String SECTION_DEFAULTWORLD = "Default World 507";

    @ConfigItem(
            keyName = "world507",
            name = "Default World 507",
            description = "Enable to default world to 507 when starting RuneLite",
            section = SECTION_DEFAULTWORLD,
            position = 1
    )
    default boolean world507(){
        return false;
    }


}
