package com.oneshot;

import com.oneshot.modules.DiaryImages;
import com.oneshot.modules.DiscordClient;
import com.oneshot.modules.ModTools;
import com.oneshot.modules.ModToolsPanel;
import com.oneshot.utils.Constants;
import com.oneshot.utils.Icons;

import com.google.inject.Provides;

import net.runelite.api.*;
import net.runelite.api.annotations.Component;
import net.runelite.api.clan.*;
import net.runelite.api.events.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ChatIconManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.ClientToolbar;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.swing.*;

import net.runelite.client.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.runelite.api.Experience.MAX_REAL_LEVEL;

@PluginDescriptor(
	name = Constants.PLUGIN_NAME
)

public class OneShotPlugin extends Plugin
{
    private static final Logger log = LoggerFactory.getLogger(OneShotPlugin.class);

    @Inject
    private ModTools modTools;

    @Inject
	private Client client;

    @Inject
    private OneShotConfig config;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private ChatIconManager chatIconManager;

    @Inject
    private DrawManager drawManager;

    @Inject
    private ClientThread clientThread;

    @Inject
    private DiscordClient discordClient;

    @Inject
    private ConfigManager configManager;

    private static final String CURRENT_VERSION = "v1.3.2"; // bump when releasing

    private boolean isMember = false;
    private boolean isModerator = false;

    private NavigationButton navButton;
    private OneShotPanel panel;
    private ModToolsPanel modToolsPanel;

    private CompletableFuture<Image> pendingDeathScreenshot = null;
    private String pendingDeathKiller = null;
    private boolean deathAwaitingVarbit = false;

    private volatile boolean levelsInitialized = false;
    private volatile boolean diariesInitialized = false;
    private final Map<String, Integer> currentLevels = new HashMap<>();
    private final Map<Skill, Integer> currentXp = new EnumMap<>(Skill.class);
    public static final int LEVEL_FOR_MAX_XP = Experience.MAX_VIRT_LEVEL + 1; // 127
    private static final Set<WorldType> SPECIAL_WORLDS = Set.of(WorldType.PVP_ARENA, WorldType.QUEST_SPEEDRUNNING, WorldType.BETA_WORLD, WorldType.NOSAVE_MODE, WorldType.TOURNAMENT_WORLD, WorldType.DEADMAN, WorldType.SEASONAL);
    private static final int LOGIN_IGNORE_TICKS = 5;
    private int ticksSinceLogin = LOGIN_IGNORE_TICKS;
    private final Map<Integer, Integer> lastVarbitValues = new HashMap<>();

    private String clanRankName;

    private static final Pattern COLLECTION_LOG_ITEM_REGEX = Pattern.compile("New item added to your collection log:.*");
    private static final String COLLECTION_LOG_TEXT = "New item added to your collection log: ";
//    private static final Pattern PET_LOG_ITEM_REGEX = Pattern.compile("You (?:have a funny feeling like you|feel something weird sneaking).*");
    private static final Pattern COMBAT_TIER_REGEX = Pattern.compile("You've completed enough Combat Achievement tasks to unlock (\\w+) Tier rewards!.*");
//    private static final Pattern COMPLETION_REGEX = Pattern.compile("Congratulations! You have completed all of the (?<difficulty>.+) tasks in the (?<area>.+) area");

    @Provides
    OneShotConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(OneShotConfig.class);
    }

    @Override
    protected void startUp() {
        log.debug("Startup");

        clientThread.invoke(() ->
        {
            panel = injector.getInstance(OneShotPanel.class);
            modToolsPanel = injector.getInstance(ModToolsPanel.class);
            panel.init(client, clientThread, modToolsPanel, config);

            ClanChannel clan = client.getClanChannel();
            if (clan != null)
            {
                ClanChannelMember local = clan.findMember(client.getLocalPlayer().getName());

                if (local != null)
                {
                    ClanRank clanRank = local.getRank();
                    ClanSettings clanSettings = client.getClanSettings();
                    ClanTitle clanTitle = clanSettings.titleForRank(clanRank);

                    this.clanRankName = mapRankTitle(clanTitle);

                    boolean moderator = isModerator(clanRank);
                    isMember = true;
                    isModerator = moderator;

                    if (moderator)
                        modTools.init(modToolsPanel);

                    try {
                        panel.buildMainPanel(
                                moderator,
                                client.getLocalPlayer().getName(),
                                mapRankTitle(clanTitle),
                                getAllMembersInfo(),
                                getMembersIcons(),
                                getMembersDisplayName()
                        );
                    } catch (IOException | InterruptedException e) {
                        log.error(e.getMessage());
                    }
                }
            }

            // UI BUTTON MUST BE ADDED ON SWING THREAD
            SwingUtilities.invokeLater(() -> {
                navButton = NavigationButton.builder()
                        .tooltip(Constants.PLUGIN_NAME)
                        .icon(Icons.RED_HELM_IMAGE)
                        .priority(Constants.DEFAULT_PRIORITY)
                        .panel(panel)
                        .build();

                clientToolbar.addNavigation(navButton);
            });
        });
    }


    @Override
	protected void shutDown() {
        log.debug("Shutdown");
        panel.deinit();
        clientToolbar.removeNavigation(navButton);
        panel = null;
        navButton = null;
	}

    private void updateClanPanel() throws IOException, InterruptedException
    {
        if (!isMember)
            return;

        ClanChannel clan = client.getClanChannel();
        if (clan == null)
            return;

        ClanChannelMember local = clan.findMember(client.getLocalPlayer().getName());
        if (local == null)
            return;

        ClanRank rank = local.getRank();
        ClanSettings settings = client.getClanSettings();
        ClanTitle title = settings.titleForRank(rank);

        isModerator = isModerator(rank);

        panel.refresh(
                isModerator,
                client.getLocalPlayer().getName(),
                mapRankTitle(title),
                getAllMembersInfo(),
                getMembersIcons(),
                getMembersDisplayName()
        );
    }

    private void sendGameMessage(String msg)
    {
        clientThread.invoke(() ->
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "[One Shot] " + msg, null)
        );
    }


    private void checkAndAnnouncePluginUpdate()
    {
        // Read what is actually stored (may be null if never set)
        String stored = configManager.getConfiguration(OneShotConfig.GROUP, "version");

        // If nothing stored yet, treat as "first run"
        if (stored == null)
        {
            configManager.setConfiguration(OneShotConfig.GROUP, "version", CURRENT_VERSION);
            return;
        }

        if (!CURRENT_VERSION.equals(stored))
        {
            sendGameMessage("Updated to " + CURRENT_VERSION + "!");
            sendGameMessage("Added new quest The Blood Moon Rises drops and unlocks");

            // Persist so it doesn't spam next login
            configManager.setConfiguration(OneShotConfig.GROUP, "version", CURRENT_VERSION);
        }
    }



    @Subscribe
    public void onClanMemberJoined(ClanMemberJoined e) throws IOException, InterruptedException
    {
        updateClanPanel();
    }

    @Subscribe
    public void onClanMemberLeft(ClanMemberLeft e) throws IOException, InterruptedException
    {
        updateClanPanel();
    }


    @Subscribe
    public void onClanChannelChanged(ClanChannelChanged clanChannelChanged) throws IOException, InterruptedException {
        if (clanChannelChanged.isGuest()) return;

        ClanChannel channel = clanChannelChanged.getClanChannel();

        if (channel == null)
        {
            panel.buildIntroPanel();
            isMember = false;
            isModerator = false;
            return;
        }

        String clanName = channel.getName();
        panel.buildIntroPanel();

        // Not One Shot CC
        if (!isOneShotMember(clanName))
        {
            panel.changeIntroText1("You are not part of One Shot CC");
            panel.changeIntroText2("You don't have access, sorry");

            isMember = false;
            isModerator = false;
            return;
        }

        // REAL MEMBER
        String playerName = client.getLocalPlayer().getName();
        ClanSettings settings = Objects.requireNonNull(client.getClanSettings());
        ClanRank rank = Objects.requireNonNull(settings.findMember(playerName)).getRank();
        ClanTitle title = Objects.requireNonNull(settings.titleForRank(rank));

        isMember = true;
        isModerator = isModerator(rank);

        if (isModerator)
            modTools.init(modToolsPanel);

        panel.buildMainPanel(
                isModerator,
                playerName,
                mapRankTitle(title),
                getAllMembersInfo(),
                getMembersIcons(),
                getMembersDisplayName()
        );
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged e) throws IOException
    {
        if (!isMember)
            return;

        int id = e.getVarbitId();
        int value = e.getValue();

        // -----------------------------------------------------
        // 1. HANDLE HCIM DEATH VARBIT
        // -----------------------------------------------------
        handleHardcoreDeathVarbit(id);

        // -----------------------------------------------------
        // 2. HANDLE DIARY COMPLETION VARBITS
        // -----------------------------------------------------
        if (diariesInitialized)
            handleDiaryVarbit(id, value);
    }

    private void handleHardcoreDeathVarbit(int id)
    {
        if (id != VarbitID.IRONMAN_HARDCORE_DEAD)
            return;

        if (!deathAwaitingVarbit)
            return;

        log.debug("HCIM death varbit triggered!");

        if (config.announceDeaths() && discordAnnouncementsEnabled() && isMember)
        {
            CompletableFuture<Image> screenshotFuture =
                    (config.announceDeathsScreenshot() && pendingDeathScreenshot != null)
                            ? pendingDeathScreenshot
                            : CompletableFuture.completedFuture(null);

            screenshotFuture.thenAccept(img -> {
                try
                {
                    discordClient.sendDeath(
                            pendingDeathKiller != null ? pendingDeathKiller : "",
                            CompletableFuture.completedFuture(img)
                    );
                }
                catch (Exception ex)
                {
                    log.error("Failed sending HCIM death notification", ex);
                }
            });
        }

        // Reset death-tracking state
        deathAwaitingVarbit = false;
        pendingDeathScreenshot = null;
        pendingDeathKiller = null;
    }

    private void handleDiaryVarbit(int varbitId, int newValue) throws IOException {
        if (!Constants.ACHIEVEMENT_DIARIES_COMPLETE_VARBITS.contains(varbitId))
            return;

        if (newValue == 0)
            return;

        Integer oldValue = lastVarbitValues.put(varbitId, newValue);

        if (oldValue == null || oldValue.equals(newValue))
            return;

        log.debug("ID: {} - Val: {}->{} - Area: {} - Tier: {}",
                varbitId,
                oldValue,
                newValue,
                DiaryImages.getDiaryInfo(varbitId).getArea(),
                DiaryImages.getDiaryInfo(varbitId).getTier()
        );

        if (!config.announceDiaries() || !discordAnnouncementsEnabled() || !isMember)
            return;

        if (isDiaryComplete(varbitId, newValue))
        {
            DiaryImages.DiaryInfo info = DiaryImages.getDiaryInfo(varbitId);

            if (info == null)
            {
                log.warn("Missing diary mapping for varbit {}", varbitId);
                return;
            }

            discordClient.sendAchievementDiary(info.getArea(), info.getTier());
        }
    }

    public static boolean isDiaryComplete(int id, int value) {
        if (id == VarbitID.ATJUN_EASY_DONE || id == VarbitID.ATJUN_MED_DONE || id == VarbitID.ATJUN_HARD_DONE) {
            // Karamja special case (except Elite): 0 = not started, 1 = started, 2 = completed tasks
            return value > 1;
        } else {
            // otherwise: 0 = not started, 1 = completed
            return value > 0;
        }
    }

    @Subscribe
    public void onActorDeath(ActorDeath actorDeath)
    {
        if (!isMember && !config.announceDeaths()) return;
        if (!discordAnnouncementsEnabled()) return;

        Actor actor = actorDeath.getActor();

        if (!(actor instanceof Player))
            return;

        Player player = (Player) actor;

        if (player != client.getLocalPlayer())
            return;

        // Save the killer’s name (may be null)
        Actor killer = actor.getInteracting();
        pendingDeathKiller = (killer != null) ? killer.getName() : "";

        // Capture screenshot now
        pendingDeathScreenshot = discordClient.getScreenshot(0, config.announceDeathsChatPrivacy());

        // Signal that a death has occurred
        deathAwaitingVarbit = true;

        log.debug("Player death recorded; waiting for HCIM varbit.");
    }

    public static boolean hideWidget(boolean shouldHide, Client client, @net.runelite.api.annotations.Component int info) {
        if (!shouldHide)
            return false;

        Widget widget = client.getWidget(info);
        log.debug(widget.getName());
        if (widget == null || widget.isHidden())
            return false;

        widget.setHidden(true);
        return true;
    }

    public static void unhideWidget(boolean shouldUnhide, Client client, ClientThread clientThread, @Component int info) {
        if (!shouldUnhide)
            return;

        clientThread.invoke(() -> {
            Widget widget = client.getWidget(info);
            if (widget != null)
                widget.setHidden(false);
        });
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event) throws IOException {
        if (!isMember || !discordAnnouncementsEnabled()) return;
        // quest
        if (event.getGroupId() == InterfaceID.QUESTSCROLL && config.announceQuests()) {
            Widget quest = client.getWidget(InterfaceID.Questscroll.QUEST_TITLE);
            if (quest != null) {
                String questText = quest.getText();
                log.debug(questText);
                // 1 tick delay to ensure relevant varbits have been processed by the client
                discordClient.sendQuest(questText);
            }
        }
    }


    @Subscribe
    public void onChatMessage(ChatMessage event) throws IOException {
        if (!isMember) return;
        if (!discordAnnouncementsEnabled()) return;
        if ((event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.SPAM)) return;

        String inputMessage = event.getMessage();
        String outputMessage = Text.removeTags(inputMessage);
        String item;

        boolean isCollectionLog = COLLECTION_LOG_ITEM_REGEX.matcher(outputMessage).matches();
        boolean isCombatAchievement = COMBAT_TIER_REGEX.matcher(outputMessage).matches();

        if (isCollectionLog)
        {
            item = outputMessage.substring(COLLECTION_LOG_TEXT.length());
            boolean isPet = Constants.Pets.contains(item);
            if (isPet && config.announcePets())  discordClient.sendPet(item);
            if (!isPet && config.announceCollectionLogs()) discordClient.sendLootDrop(item);
        }

        if (isCombatAchievement && config.announceCombatAchievements())
            discordClient.sendCombatAchievement(parseCombatTier(outputMessage));

    }

    private boolean isInSpecialWorld()
    {
        final Set<WorldType> types = client.getWorldType();
        if (types == null || types.isEmpty()) { return false; }

        for (WorldType t : types)
        {
            if (SPECIAL_WORLDS.contains(t)) { return true; }
        }
        return false;
    }

    private boolean discordAnnouncementsEnabled() { return !isInSpecialWorld(); }

    @Nullable
    public static String parseCombatTier(String message)
    {
        Matcher m = COMBAT_TIER_REGEX.matcher(message);
        if (m.find())
        {
            return m.group(1);
        }
        return null;
    }

    @Subscribe
    public void onStatChanged(StatChanged statChange) throws IOException {
        if (!isMember || !discordAnnouncementsEnabled()) return;
        handleLevelUp(statChange.getSkill(), statChange.getLevel(), statChange.getXp());
    }

    @Subscribe
    public void onGameTick(final GameTick tick) throws IOException, InterruptedException
    {
        if (!isMember)
            return;

        updateRankAndPanel();
        discordClient.onGameTick();

        handleLoginInitialization();
    }

    private void updateRankAndPanel() throws IOException, InterruptedException
    {
        ClanChannel clan = client.getClanChannel();
        if (clan == null)
            return;

        ClanChannelMember local = clan.findMember(client.getLocalPlayer().getName());
        if (local == null)
            return;

        ClanRank rank = local.getRank();
        ClanTitle title = client.getClanSettings().titleForRank(rank);

        String newRankName = mapRankTitle(title);

        // Only update interface if rank actually changed
        if (!Objects.equals(newRankName, clanRankName))
        {
            boolean nowModerator = isModerator(rank);

            if (this.isModerator != nowModerator)
            {
                this.isModerator = nowModerator;
                if (nowModerator) modTools.init(modToolsPanel);
            }

            panel.refresh(
                    nowModerator,
                    client.getLocalPlayer().getName(),
                    newRankName,
                    getAllMembersInfo(),
                    getMembersIcons(),
                    getMembersDisplayName()
            );

            clanRankName = newRankName;
        }
    }

    private void handleLoginInitialization()
    {
        //log.debug(String.format("%d <> %d", ticksSinceLogin, LOGIN_IGNORE_TICKS));
        if (ticksSinceLogin >= LOGIN_IGNORE_TICKS)
            return;

        ticksSinceLogin++;

        if (ticksSinceLogin == LOGIN_IGNORE_TICKS)
        {
            clientThread.invoke(this::initLevels);
            clientThread.invoke(this::initDiaries);
            clientThread.invoke(this::initCollectionLogs);
        }
    }

    private void initCollectionLogs() {
        int collectionlogs = client.getVarpValue(VarPlayerID.COLLECTION_COUNT);
//        log.debug(String.format("initCollectionLogs: %d",collectionlogs));
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged)
    {
        if (gameStateChanged.getGameState() == GameState.LOADING) return;
        if (gameStateChanged.getGameState() == GameState.LOGIN_SCREEN) {
            this.resetLevels();
            this.resetDiaries();
            panel.buildIntroPanel();
        } else if (gameStateChanged.getGameState() == GameState.LOGGED_IN && !SPECIAL_WORLDS.contains(client.getWorldType())) {
            ticksSinceLogin = 0;
            clientThread.invoke(this::initLevels);
            clientThread.invoke(this::initDiaries);

            checkAndAnnouncePluginUpdate();
        }
    }

    private void initLevels() {
        // make sure we run on client thread - if not, re-schedule and return
        if (!client.isClientThread())
        {
            clientThread.invoke(this::initLevels);
            return;
        }
//        log.debug("Levels initialized");

        currentXp.clear();
        currentLevels.clear();

        for (Skill skill : Skill.values()) {
            int xp = client.getSkillExperience(skill);
            int level = client.getRealSkillLevel(skill); // O(1)
            if (level >= MAX_REAL_LEVEL) {
                level = getLevel(xp);
            }
            currentLevels.put(skill.getName(), level);
            currentXp.put(skill, xp);
        }

        levelsInitialized = true; // <-- only set after maps are populated
        //log.debug("Initialized current skill levels: {}", currentLevels);
    }

    private void initDiaries() {
        // make sure we run on client thread - if not, re-schedule and return
        diariesInitialized = false;
        if (!client.isClientThread())
        {
            clientThread.invoke(this::initDiaries);
            return;
        }
//        log.debug("Diaries initialized");

        Integer value = null;
        currentLevels.clear();

        for (int varbitID : Constants.ACHIEVEMENT_DIARIES_COMPLETE_VARBITS)
        {
            value = lastVarbitValues.put(varbitID, client.getVarbitValue(varbitID));
            //log.debug(String.format("Init varbit %d with value %d",varbitID,value));
        }

        if (value == null)
        {
            ticksSinceLogin = 0;
            return;
        }

        for (Skill skill : Skill.values()) {
            int xp = client.getSkillExperience(skill);
            int level = client.getRealSkillLevel(skill); // O(1)
            if (level >= MAX_REAL_LEVEL) {
                level = getLevel(xp);
            }
            currentLevels.put(skill.getName(), level);
            currentXp.put(skill, xp);
        }
        diariesInitialized = true; // <-- only set after maps are populated
    }




    private int getLevel(int xp) {
        // treat 200M XP as level 127
        if (xp >= Experience.MAX_SKILL_XP)
            return LEVEL_FOR_MAX_XP;

        // log(n) operation to support virtual levels
        return Experience.getLevelForXp(xp);
    }

    public void resetLevels() {
        levelsInitialized = false;

        clientThread.invoke(() -> {
            currentXp.clear();
            currentLevels.clear();
            levelsInitialized = false;
            log.debug("resetLevels: cleared level state on client thread");
        });
    }

    public void resetDiaries() {
        lastVarbitValues.clear();
        diariesInitialized = false;
    }

    private void handleLevelUp(Skill skill, int level, int xp) throws IOException {
        if (ticksSinceLogin < LOGIN_IGNORE_TICKS)
        {
            log.debug("Ignoring StatChanged on login: {}", skill);
            return;
        }

        if (xp <= 0 || level <= 1) return;

        if (!levelsInitialized) {
            // optionally track ticks to force init later, but do not process level-ups
            log.debug("Ignoring StatChanged for {} while levels not initialised", skill);
            return;
        }


        Integer previousXp = currentXp.put(skill, xp);
        if (previousXp == null) {
            return;
        }

        String skillName = skill.getName();
        int virtualLevel = level < MAX_REAL_LEVEL ? level : getLevel(xp); // avoid log(n) query when not needed
        Integer previousLevel = currentLevels.put(skillName, virtualLevel);


        if (previousLevel == null || previousLevel == 0) {
            return;
        }

        if (virtualLevel < previousLevel || xp < previousXp) {
            // base skill level should never regress; reset notifier state
            resetLevels();
            return;
        }

        int totalLevel = client.getTotalLevel();

        // Check normal skill level up for only 99 and maxed
        if (virtualLevel > previousLevel && virtualLevel <= MAX_REAL_LEVEL) {
            if (totalLevel == Constants.MAX_TOTAL_LEVEL && config.announceMaxed()) {
                discordClient.sendLevelMaxed(totalLevel);
            }
            else if (config.announceLevel()) {
                discordClient.sendLevelUp(skill, virtualLevel);
            }
        }

        // 200M XP announcement (independent of level-up)
        if (level >= MAX_REAL_LEVEL
                && xp > previousXp
                && xp >= Experience.MAX_SKILL_XP
                && config.announce200M())
        {
            discordClient.sendXP200(skill);
        }
    }

    public ImageIcon getRankIcon(ClanTitle clanTitle)
    {
        BufferedImage chatIcon = chatIconManager.getRankImage(clanTitle);
        assert chatIcon != null;
        return new ImageIcon(chatIcon.getScaledInstance(Constants.TEXT_ICON_SIZE, Constants.TEXT_ICON_SIZE, Image.SCALE_DEFAULT));
    }

    private static boolean isModerator(ClanRank clanRank)
    {
        return Arrays.asList(Constants.RANK_OWNER, Constants.RANK_DEPUTY_OWNER, Constants.RANK_ASTRAL,
                Constants.RANK_CAPTAIN).contains(clanRank);
    }

    private static boolean isOneShotMember(String clanName)
    {
        return Objects.equals(clanName, Constants.CLAN_NAME);
    }

    private ArrayList<OneShotMember> getAllMembersInfo() {

        ArrayList<OneShotMember> oneShotMembers = new ArrayList<>();
        ArrayList<Integer> tmpIndexList = new ArrayList<>();

        // checks all members offline and online
        ClanSettings clanSettings = client.getClanSettings();
        assert clanSettings != null;
        java.util.List<ClanMember> clanMembers = clanSettings.getMembers();

        for (ClanMember clanMember : clanMembers)
        {
            ClanRank clanRank = clanMember.getRank();
            int index = clanRank.getRank();
            if (!tmpIndexList.contains(index))
            {
                tmpIndexList.add(index);
                ClanTitle clanTitle = clanSettings.titleForRank(clanRank);
                String displayTitle = mapRankTitle(clanTitle);

                OneShotMember oneShotMember = new OneShotMember(index, clanTitle, displayTitle);
                oneShotMember.addTotal();
                oneShotMembers.add(oneShotMember);
            }
            else
            {
                for (OneShotMember oneShotMember : oneShotMembers)
                {
                    if (oneShotMember.index == index)
                    {
                        oneShotMember.addTotal();
                    }
                }
            }
        }

        // checks for online members only
        ClanChannel clanChannel = client.getClanChannel();
        assert clanChannel != null;
        List<ClanChannelMember> clanChannelMembers = clanChannel.getMembers();

        for (ClanChannelMember clanChannelMember : clanChannelMembers)
        {
            int index = clanChannelMember.getRank().getRank();
            for (OneShotMember oneShotMember : oneShotMembers)
            {
                if (oneShotMember.index == index)
                {
                    oneShotMember.addOnline();
                }
            }
        }

        oneShotMembers.sort(Comparator.comparing(OneShotMember::getIndex).reversed());

        return oneShotMembers;
    }

    private String mapRankTitle(ClanTitle title)
    {
        if (title == null)
        {
            return "Unknown";
        }

        switch (title.getName())
        {
            case "Owner":
                return "Founder";

            case "Deputy Owner":
                return "Co-Founder";

            case "Astral":
                return "Administrator";

            case "Captain":
                return "Moderator";

            case "Lieutenant":
                return "Trial Moderator";

            case "Witch":
            case "Necromancer":
            case "Seer":
                return "Event Team";

            default:
                return title.getName();
        }
    }


    private Map<String, ImageIcon> getMembersIcons()
    {
        Map<String, ImageIcon> Members = new HashMap<>();

        ClanSettings clanSettings = client.getClanSettings();
        assert clanSettings != null;
        java.util.List<ClanMember> clanMembers = clanSettings.getMembers();

        for (ClanMember clanMember : clanMembers)
        {
            String memberName = clanMember.getName();
//            log.debug(memberName.toLowerCase().replace(" ", " ") + ": " + convertToHexString(memberName.toLowerCase().replace(" ", " ").getBytes()));
            ImageIcon icon = getRankIcon(clanSettings.titleForRank(clanMember.getRank()));
            Members.put(memberName.toLowerCase().replace(" ", " "), icon);
        }
        return Members;
    }

    private Map<String, String> getMembersDisplayName()
    {
        Map<String, String> Members = new HashMap<>();

        ClanSettings clanSettings = client.getClanSettings();
        assert clanSettings != null;
        java.util.List<ClanMember> clanMembers = clanSettings.getMembers();

        for (ClanMember clanMember : clanMembers)
        {
            String memberName = clanMember.getName();
            Members.put(memberName.toLowerCase().replace(" ", " "), memberName);
        }
        return Members;
    }

    public class OneShotMember {
        int index;
        ImageIcon icon;
        String name;
        int online = 0;
        int total = 0;

        public OneShotMember(int index, ClanTitle clanTitle, String displayTitle)
        {
            this.index = index;
            this.icon = getRankIcon(clanTitle);
            this.name = displayTitle;
        }

        public void addOnline()
        {
            this.online++;
        }

        public void addTotal()
        {
            this.total++;
        }

        public String toString()
        {
            return name + " | " + online + " | " + total;
        }

        public int getIndex()
        {
            return index;
        }

        public ImageIcon getIcon()
        {
            return icon;
        }

        public String getName()
        {
            return name;
        }

        public int getOnline()
        {
            return online;
        }

        public int getTotal()
        {
            return total;
        }
    }


}

