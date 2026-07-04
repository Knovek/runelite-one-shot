package com.oneshot;

import com.oneshot.utils.Constants.*;
import net.runelite.client.config.*;


@ConfigGroup(OneShotConfig.GROUP)
public interface OneShotConfig extends Config
{

    String GROUP = "oneshot";

    // --- Version Control ---
    @ConfigItem(
            keyName = "version",
            position = 0,
            name = "version",
            description = "version",
            hidden = true)
    default String version() {return "v1.3.2"; }

    // --- Sections ---
    @ConfigSection(
            name = "Discord Global",
            description = "",
            position = 1,
            closedByDefault = true
    )
    String DISCORD_GLOBAL = "Discord Global";

    @ConfigSection(
            name = "Discord Level 99",
            description = "",
            position = 1,
            closedByDefault = true
    )
    String DISCORD_LEVEL = "Discord Level 99";

    @ConfigSection(
            name = "Discord Maxed Level",
            description = "",
            position = 2,
            closedByDefault = true
    )
    String DISCORD_MAXED = "Discord Maxed Level";

    @ConfigSection(
            name = "Discord 200M Exp",
            description = "",
            position = 3,
            closedByDefault = true
    )
    String DISCORD_200M_XP = "Discord 200M Exp";

    @ConfigSection(
            name = "Discord GM Quests",
            description = "",
            position = 4,
            closedByDefault = true
    )
    String DISCORD_QUESTS = "Discord GM Quests";

    @ConfigSection(
            name = "Discord Elite Diaries",
            description = "",
            position = 5,
            closedByDefault = true
    )
    String DISCORD_DIARIES = "Discord Elite Diaries";

    @ConfigSection(
            name = "Discord Combat Achievements",
            description = "",
            position = 6,
            closedByDefault = true
    )
    String DISCORD_CA = "";

    @ConfigSection(
            name = "Discord Collection Logs",
            description = "",
            position = 7,
            closedByDefault = true
    )
    String DISCORD_CLOG = "Discord Collection Logs";

    @ConfigSection(
            name = "Discord Pets",
            description = "",
            position = 8,
            closedByDefault = true
    )
    String DISCORD_PET = "Discord Pets";

    @ConfigSection(
            name = "Discord Death",
            description = "",
            position = 9,
            closedByDefault = true
    )
    String DISCORD_DEATH = "Discord Death";

    @ConfigSection(
            name = "Leaderboards",
            description = "",
            position = 10,
            closedByDefault = true
    )
    String LEADERBOARDS = "Leaderboards";

    // --- Items ---
    @ConfigItem(
            keyName = "infoMessage",
            name = "Show notification in chat",
            description = "Sends a notification in chat whenever an announcement is sent to discord",
            section = DISCORD_GLOBAL,
            position = 1
    )
    default boolean infoMessage(){ return true; }

    @ConfigItem(
            keyName = "announceLevel",
            name = "Enable level announcements",
            description = "",
            section = DISCORD_LEVEL,
            position = 1
    )
    default boolean announceLevel(){ return true; }

    @ConfigItem(
            keyName = "announceLevelScreenshot",
            name = "Include screenshot",
            description = "",
            section = DISCORD_LEVEL,
            position = 2
    )
    default boolean announceLevelScreenshot(){ return true; }

    @ConfigItem(
            keyName = "announceLevelChatPrivacy",
            name = "Chat privacy",
            description = "",
            section = DISCORD_LEVEL,
            position = 3
    )
    default chatPrivacy announceLevelChatPrivacy(){ return chatPrivacy.ALL; }

    @ConfigItem(
            keyName = "announceMaxed",
            name = "Enable maxed announcements",
            description = "",
            section = DISCORD_MAXED,
            position = 1
    )
    default boolean announceMaxed(){ return true; }

    @ConfigItem(
            keyName = "announceMaxedScreenshot",
            name = "Include screenshot",
            description = "",
            section = DISCORD_MAXED,
            position = 2
    )
    default boolean announceMaxedScreenshot(){ return true; }

    @ConfigItem(
            keyName = "announceMaxedChatPrivacy",
            name = "Chat privacy",
            description = "",
            section = DISCORD_MAXED,
            position = 3
    )
    default chatPrivacy announceMaxedChatPrivacy(){ return chatPrivacy.ALL; }

    @ConfigItem(
            keyName = "announce200M",
            name = "Enable 200M XP announcements",
            description = "",
            section = DISCORD_200M_XP,
            position = 1
    )
    default boolean announce200M(){ return true; }

    @ConfigItem(
            keyName = "announce200MScreenshot",
            name = "Include screenshot",
            description = "",
            section = DISCORD_200M_XP,
            position = 2
    )
    default boolean announce200MScreenshot(){ return true; }

    @ConfigItem(
            keyName = "announce200MChatPrivacy",
            name = "Chat privacy",
            description = "",
            section = DISCORD_200M_XP,
            position = 3
    )
    default chatPrivacy announce200MChatPrivacy(){ return chatPrivacy.ALL; }

    @ConfigItem(
            keyName = "announceQuests",
            name = "Enable GM Quest announcements",
            description = "",
            section = DISCORD_QUESTS,
            position = 1
    )
    default boolean announceQuests(){ return true; }

    @ConfigItem(
            keyName = "announceQuestsScreenshot",
            name = "Include screenshot",
            description = "",
            section = DISCORD_QUESTS,
            position = 2
    )
    default boolean announceQuestsScreenshot(){ return true; }

    @ConfigItem(
            keyName = "announceQuestsChatPrivacy",
            name = "Chat privacy",
            description = "",
            section = DISCORD_QUESTS,
            position = 3
    )
    default chatPrivacy announceQuestsChatPrivacy(){ return chatPrivacy.ALL; }

    @ConfigItem(
            keyName = "announceQuestsStats",
            name = "Include total quest points",
            description = "",
            section = DISCORD_QUESTS,
            position = 4
    )
    default boolean announceQuestsStats(){ return true; }

    @ConfigItem(
            keyName = "announceDiaries",
            name = "Enable Elite Diary announcements",
            description = "",
            section = DISCORD_DIARIES,
            position = 1
    )
    default boolean announceDiaries(){ return true; }

    @ConfigItem(
            keyName = "announceDiariesScreenshot",
            name = "Include screenshot",
            description = "",
            section = DISCORD_DIARIES,
            position = 2
    )
    default boolean announceDiariesScreenshot(){ return true; }

    @ConfigItem(
            keyName = "announceDiariesChatPrivacy",
            name = "Chat privacy",
            description = "",
            section = DISCORD_DIARIES,
            position = 3
    )
    default chatPrivacy announceDiariesChatPrivacy(){ return chatPrivacy.ALL; }

    @ConfigItem(
            keyName = "announceCombatAchievements",
            name = "Enable CA rewards announcements",
            description = "",
            section = DISCORD_CA,
            position = 1
    )
    default boolean announceCombatAchievements(){ return true; }

    @ConfigItem(
            keyName = "announceCombatAchievementsScreenshot",
            name = "Include screenshot",
            description = "",
            section = DISCORD_CA,
            position = 2
    )
    default boolean announceCombatAchievementsScreenshot(){ return true; }

    @ConfigItem(
            keyName = "announceCombatAchievementsChatPrivacy",
            name = "Chat privacy",
            description = "",
            section = DISCORD_CA,
            position = 3
    )
    default chatPrivacy announceCombatAchievementsChatPrivacy(){ return chatPrivacy.ALL; }

    @ConfigItem(
            keyName = "announceCollectionLogs",
            name = "Enable clog announcements",
            description = "",
            section = DISCORD_CLOG,
            position = 1
    )
    default boolean announceCollectionLogs(){ return true; }

    @ConfigItem(
            keyName = "announceCollectionLogsScreenshot",
            name = "Include screenshot",
            description = "",
            section = DISCORD_CLOG,
            position = 2
    )
    default boolean announceCollectionLogsScreenshot(){ return true; }

    @ConfigItem(
            keyName = "announceCollectionLogsChatPrivacy",
            name = "Chat privacy",
            description = "",
            section = DISCORD_CLOG,
            position = 3
    )
    default chatPrivacy announceCollectionLogsChatPrivacy(){ return chatPrivacy.ALL; }

    @ConfigItem(
            keyName = "announceCollectionLogsStats",
            name = "Include total clogs",
            description = "",
            section = DISCORD_CLOG,
            position = 4
    )
    default boolean announceCollectionLogsStats(){ return true; }

    @ConfigItem(
            keyName = "announcePets",
            name = "Enable pet announcements",
            description = "",
            section = DISCORD_PET,
            position = 1
    )
    default boolean announcePets(){ return true; }

    @ConfigItem(
            keyName = "announcePetsScreenshot",
            name = "Include screenshot",
            description = "",
            section = DISCORD_PET,
            position = 2
    )
    default boolean announcePetsScreenshot(){ return true; }

    @ConfigItem(
            keyName = "announcePetsChatPrivacy",
            name = "Chat privacy",
            description = "",
            section = DISCORD_PET,
            position = 3
    )
    default chatPrivacy announcePetsChatPrivacy(){ return chatPrivacy.ALL; }

    @ConfigItem(
            keyName = "announceDeaths",
            name = "Enable death announcements",
            description = "",
            section = DISCORD_DEATH,
            position = 1
    )
    default boolean announceDeaths(){ return true; }

    @ConfigItem(
            keyName = "announceDeathsScreenshot",
            name = "Include screenshot",
            description = "",
            section = DISCORD_DEATH,
            position = 2
    )
    default boolean announceDeathsScreenshot(){ return true; }

    @ConfigItem(
            keyName = "announceDeathsChatPrivacy",
            name = "Chat privacy",
            description = "",
            section = DISCORD_DEATH,
            position = 3
    )
    default chatPrivacy announceDeathsChatPrivacy(){ return chatPrivacy.ALL; }

    @ConfigItem(
            keyName = "announceDeathsStats",
            name = "Include combat and total level",
            description = "",
            section = DISCORD_DEATH,
            position = 4
    )
    default boolean announceDeathsStats(){ return true; }

    @ConfigItem(
            keyName = "displayVirtualLevels",
            name = "Display Virtual Levels",
            description = "Should virtual levels be displayed in the skills leaderboards?",
            section = LEADERBOARDS,
            position = 1
    )
    default boolean displayVirtualLevels() { return true; }

}
