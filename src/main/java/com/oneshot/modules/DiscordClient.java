package com.oneshot.modules;

import com.oneshot.OneShotConfig;
import com.oneshot.OneShotPlugin;
import com.oneshot.utils.Constants;
import com.oneshot.utils.Icons;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.annotations.Component;
import net.runelite.api.clan.ClanRank;
import net.runelite.api.clan.ClanSettings;
import net.runelite.api.clan.ClanTitle;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ChatIconManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.util.QuantityFormatter;
import net.runelite.http.api.item.ItemPrice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import okhttp3.*;

import java.util.Base64;

@Singleton
public class DiscordClient {

    private static final Logger log = LoggerFactory.getLogger(DiscordClient.class);

    private final String partypete = "PARTYPETE";
    private final String towncrier = "TOWNCRIER";
    private final String death = "DEATHS";
    private final String appreciator = "APPRECIATOR";

    private DrawManager drawManager;
    private ClientThread clientThread;
    private OneShotConfig config;
    private Client client;
    private ChatIconManager chatIconManager;
    private SkillIconManager skillIconManager;

    private CompletableFuture<Image> pendingScreenshot;
    private boolean chatHiddenForScreenshot;
    private int screenshotDelayTicks;

    private static final MediaType JSON_MEDIA = MediaType.get("application/json; charset=utf-8");
    private static final String WORKER_URL = Constants.WORKER_URL;

    @Inject
    private OkHttpClient httpClient;


    @Inject
    private ItemManager itemManager;

    @Inject
    private ItemSearcher itemSearcher;

    @Inject
    public DiscordClient(
            OneShotConfig config,
            Client client,
            DrawManager drawManager,
            ClientThread clientThread,
            ChatIconManager chatIconManager,
            SkillIconManager skillIconManager
    ) {
        this.config = config;
        this.client = client;
        this.drawManager = drawManager;
        this.clientThread = clientThread;
        this.chatIconManager = chatIconManager;
        this.skillIconManager = skillIconManager;
    }

    public void sendLevelUp(Skill skill, int level) throws IOException
    {
        if (partypete == null) return;
        log.debug(String.format("Leveled up %s:%d",skill.getName(),level));
        if (level != 99) return;

        // ---- Level values ------------------------------------------------------
        String description = "";
        String title = String.format("Achieved %s Level %d", skill.getName(), level);

        // ---- Static values -----------------------------------------------------
        String username = null;
        String playerName = client.getLocalPlayer().getName();

        List<DiscordField> fields = List.of();

        BufferedImage skillIcon = skillIconManager.getSkillImage(skill);
        byte[] thumbnail = bufferedImageToBytes(skillIcon);

        byte[] rankIcon = getRankIcon(playerName);

        // ---- Send --------------------------------------------------------------
        CompletableFuture<Image> screenshotFuture = config.uploadscreenshots()
                ? getScreenshot()
                : CompletableFuture.completedFuture(null);

        screenshotFuture.thenAcceptAsync(img -> {
            try {
                byte[] screenshot = img != null
                        ? bufferedImageToBytes((BufferedImage) img)
                        : null;

                sendDiscordEmbed(
                        partypete,
                        title,
                        Constants.DISCORD_LEVELS_COLOR,
                        playerName,
                        username,
                        null,
                        description,
                        fields,
                        screenshot,
                        rankIcon,
                        thumbnail
                );
            } catch (Exception e) {
                log.error("Failed to send quest embed", e);
            }
        });

    }

    public void send200(Skill skill) throws IOException
    {
        if (partypete == null) return;
        String description = "";
        String title = String.format("Achieved 200M XP in %s", skill.getName());

        // ---- Static values -----------------------------------------------------
        String playerName = client.getLocalPlayer().getName();

        List<DiscordField> fields = List.of();

        BufferedImage skillIcon = skillIconManager.getSkillImage(skill);
        byte[] thumbnail = bufferedImageToBytes(skillIcon);


        byte[] rankIcon = getRankIcon(playerName);

        // ---- Send --------------------------------------------------------------
        CompletableFuture<Image> screenshotFuture = config.uploadscreenshots()
                ? getScreenshot()
                : CompletableFuture.completedFuture(null);

        screenshotFuture.thenAcceptAsync(img -> {
            try {
                byte[] screenshot = img != null
                        ? bufferedImageToBytes((BufferedImage) img)
                        : null;

                sendDiscordEmbed(
                        partypete,
                        title,
                        Constants.DISCORD_LEVELS_COLOR,
                        playerName,
                        null,
                        null,
                        description,
                        fields,
                        screenshot,
                        rankIcon,
                        thumbnail
                );
            } catch (Exception e) {
                log.error("Failed to send quest embed", e);
            }
        });

    }

    public void sendQuest(String questText) throws IOException {
        if (towncrier == null) return;
        // ---- Quest values ------------------------------------------------------
        int completedQuests = client.getVarbitValue(VarbitID.QUESTS_COMPLETED_COUNT);
        int totalQuests = client.getVarbitValue(VarbitID.QUESTS_TOTAL_COUNT);
        boolean validQuests = completedQuests > 0 && totalQuests > 0;

        int questPoints = client.getVarpValue(VarPlayerID.QP);
        int totalQuestPoints = client.getVarbitValue(VarbitID.QP_MAX);
        boolean validPoints = questPoints > 0 && totalQuestPoints > 0;

        if (!validPoints && !validQuests) { return; }

        String questName = QuestUtils.parseQuestWidget(questText);
        log.debug("Completed quest: " + questText);
        if (questName == null || !Constants.GM_QUESTS.contains(questName))
            return;

        // ---- Static values -----------------------------------------------------
        String username = null;
        String url = getWikiUrl(questName);
        String playerName = client.getLocalPlayer().getName();
        String description = String.format("[%s](%s)",questName,url);
        String title = "Quest completed";
        List<DiscordField> fields = new ArrayList<>();

        if (config.uploadTotalQuestPoints())
        {
            fields.add(new DiscordField(
                    "Quests completed",
                    String.format("%d/%d",completedQuests, totalQuests),
                    true
            ));
            fields.add(new DiscordField(
                    "Quest points",
                    String.format("%d/%d",questPoints, totalQuestPoints),
                    true
            ));
        }

        byte[] questThumbnail = scaleWithPadding(
                Icons.QUEST_IMAGE,
                Constants.DISCORD_THUMBNAIL_SIZE,
                1
        );

        byte[] rankIcon = getRankIcon(playerName);

        // ---- Send --------------------------------------------------------------
        CompletableFuture<Image> screenshotFuture = config.uploadscreenshots()
                ? getScreenshot()
                : CompletableFuture.completedFuture(null);

        screenshotFuture.thenAcceptAsync(img -> {
            try {
                byte[] screenshot = img != null
                        ? bufferedImageToBytes((BufferedImage) img)
                        : null;

                sendDiscordEmbed(
                        towncrier,
                        title,
                        Constants.DISCORD_QUESTS_COLOR,
                        playerName,
                        username,
                        null,
                        description,
                        fields,
                        screenshot,
                        rankIcon,
                        questThumbnail
                );
            } catch (Exception e) {
                log.error("Failed to send quest embed", e);
            }
        });

    }

    public void sendAchievementDiary(String areaStr, String tierStr) throws IOException {
        if (towncrier == null) return;
        if (!Objects.equals(tierStr, "Elite")) return;
        // Capture client-thread-safe data first
        String playerName = client.getLocalPlayer().getName();

        String title = String.format("%s %s diaries completed", areaStr, tierStr);
        String descriptionText = String.format("%s %s Diaries", areaStr, tierStr);
        String itemWikiUrl = getWikiUrl(String.format("%s Diary#%s", areaStr, tierStr));
        String description = String.format("[%s](%s)", descriptionText, itemWikiUrl);

        List<DiscordField> fields = null;
        String username = null;
        String url = null;

        // ---- Rank icon -----------------------------------------------------
        byte[] rankIcon = getRankIcon(playerName);

        // ---- Thumbnail -----------------------------------------------------
        byte[] taskThumbnail = scaleWithPadding(
                Icons.TASKS_IMAGE,
                Constants.DISCORD_THUMBNAIL_SIZE,
                1
        );

        // ---- Send --------------------------------------------------------------
        CompletableFuture<Image> screenshotFuture = config.uploadscreenshots()
                ? getScreenshot()
                : CompletableFuture.completedFuture(null);

        screenshotFuture.thenAcceptAsync(img -> {
            try {
                byte[] screenshot = img != null
                        ? bufferedImageToBytes((BufferedImage) img)
                        : null;

                sendDiscordEmbed(
                        towncrier,
                        title,
                        Constants.DISCORD_DIARIES_COLOR,
                        playerName,
                        username,
                        url,
                        description,
                        fields,
                        screenshot,
                        rankIcon,
                        taskThumbnail
                );
            } catch (Exception e) {
                log.error("Failed to send quest embed", e);
            }
        });
    }


    public void sendCombatAchievement(String combatTier) throws IOException
    {
        if (towncrier == null) return;
        List<String> allowedTiers = List.of("Elite","Master","Grandmaster");
        if (!allowedTiers.contains(combatTier)) return;

        String playerName = client.getLocalPlayer().getName();

        // ---- Text ----------------------------------------------------------
        String title = combatTier + " Tier Rewards unlocked";
        String itemWikiUrl = Constants.WIKI_COMBAT_ACHIEVEMENTS_REWARDS;
        String description = String.format("[%s](%s)","Combat Achievement Rewards",itemWikiUrl);

        List<DiscordField> fields = null;
        String username = null;
        String url = null;

        // ---- Rank icon -----------------------------------------------------
        byte[] rankIcon = getRankIcon(playerName);

        // ---- Thumbnail -----------------------------------------------------
        String itemImageUrl = Constants.COMBAT_ACHIEVEMENT_REWARDS_IMAGE_URL.get(combatTier);

        // ---- Send --------------------------------------------------------------
        CompletableFuture<Image> screenshotFuture = config.uploadscreenshots()
                ? getScreenshot()
                : CompletableFuture.completedFuture(null);

        screenshotFuture.thenAcceptAsync(img -> {
            try {
                byte[] screenshot = img != null
                        ? bufferedImageToBytes((BufferedImage) img)
                        : null;

                sendDiscordEmbed(
                        towncrier,
                        title,
                        Constants.DISCORD_COMBAT_ACHIEVEMENTS_COLOR,
                        playerName,
                        username,
                        url,
                        description,
                        fields,
                        screenshot,
                        rankIcon,
                        itemImageUrl
                );
            } catch (Exception e) {
                log.error("Failed to send quest embed", e);
            }
        });

    }



    public void sendPet(String itemName) throws IOException
    {
        if (appreciator == null) return;
        String playerName = client.getLocalPlayer().getName();

        // ---- Text ----------------------------------------------------------
        String title = "New pet";
        String itemWikiUrl = getWikiUrl(itemName);
        String description = String.format("[%s](%s)",itemName,itemWikiUrl);

        List<DiscordField> fields = null;
        String username = null;
        String url = null;

        // ---- Rank icon -----------------------------------------------------
        byte[] rankIcon = getRankIcon(playerName);

        // ---- Thumbnail -----------------------------------------------------
        int itemID = getItemID(itemName);
        String itemImageUrl;
        if (itemID >= 0){
            itemImageUrl = itemImageUrl(itemID);
        } else {
            itemImageUrl = null;
        }

        // ---- Send --------------------------------------------------------------
        CompletableFuture<Image> screenshotFuture = config.uploadscreenshots()
                ? getScreenshot(4)
                : CompletableFuture.completedFuture(null);

        screenshotFuture.thenAcceptAsync(img -> {
            try {
                byte[] screenshot = img != null
                        ? bufferedImageToBytes((BufferedImage) img)
                        : null;

                sendDiscordEmbed(
                        appreciator,
                        title,
                        Constants.DISCORD_PETS_COLOR,
                        playerName,
                        username,
                        url,
                        description,
                        fields,
                        screenshot,
                        rankIcon,
                        itemImageUrl
                );
            } catch (Exception e) {
                log.error("Failed to send quest embed", e);
            }
        });

    }

    public void sendLootDrop(String itemName) throws IOException
    {
        if (appreciator == null) return;
        String playerName = client.getLocalPlayer().getName();
        boolean isAllowed = Constants.ITEMS_WHITELIST.contains(itemName);
        log.debug(String.format("new collection log: %s - %s",itemName,isAllowed ? "Allowed" : "Not allowed"));
        if (!isAllowed) return;

        // ---- Text ----------------------------------------------------------
        String title = "New collection log";
        String itemWikiUrl = getWikiUrl(itemName);
        int itemHAPrice = getHAPrice(itemName);
        int itemPrice = getWikiPrice(itemName);
        String description = String.format("[%s](%s)",itemName,itemWikiUrl);

        //if (itemPrice < 5e6) return;

        List<DiscordField> fields;

        fields = new ArrayList<>();
        if (itemPrice > 0) {
            fields.add(new DiscordField(
                    "GE price",
                    String.format("%s", QuantityFormatter.formatNumber(itemPrice)),
                    true
            ));
        }
        if (itemHAPrice > 0) {
            fields.add(new DiscordField(
                    "HA price",
                    String.format("%s", QuantityFormatter.formatNumber(itemHAPrice)),
                    true
            ));
        }
        clientThread.invokeLater(() -> {
            int totalCollectionLogs = client.getVarpValue(VarPlayerID.COLLECTION_COUNT_MAX);
            int collectedLogs = client.getVarpValue(VarPlayerID.COLLECTION_COUNT) + 1; // varp is not updated on time

            fields.add(new DiscordField(
                    "Collections logged",
                    String.format("%d/%d", collectedLogs, totalCollectionLogs),
                    true
            ));
        });

        String username = null;
        String url = null;

        // ---- Rank icon -----------------------------------------------------
        byte[] rankIcon = getRankIcon(playerName);

        // ---- Thumbnail -----------------------------------------------------
        int itemID = getItemID(itemName);
        String itemImageUrl;
        if (itemID >= 0){
            itemImageUrl = itemImageUrl(itemID);
        } else {
            itemImageUrl = null;
        }

        // ---- Send --------------------------------------------------------------
        CompletableFuture<Image> screenshotFuture = config.uploadscreenshots()
                ? getScreenshot(4)
                : CompletableFuture.completedFuture(null);

        screenshotFuture.thenAcceptAsync(img -> {
            try {
                byte[] screenshot = img != null
                        ? bufferedImageToBytes((BufferedImage) img)
                        : null;

                sendDiscordEmbed(
                        appreciator,
                        title,
                        Constants.DISCORD_LOOT_COLOR,
                        playerName,
                        username,
                        url,
                        description,
                        fields,
                        screenshot,
                        rankIcon,
                        itemImageUrl
                );
            } catch (Exception e) {
                log.error("Failed to send quest embed", e);
            }
        });

    }

    public void sendDeath(String actorInteraction, CompletableFuture<Image> screenshotFuture) throws IOException
    {
        if (death == null) return;
        String playerName = client.getLocalPlayer().getName();

        // ---- Text ----------------------------------------------------------
        String title = Objects.equals(actorInteraction, "") ?  playerName + " has died!" : playerName + " has died to " + actorInteraction + "!";
        String description = "";
        String username = null;
        String url = null;

        // ---- Fields --------------------------------------------------------
        List<DiscordField> fields = new ArrayList<>();
        fields.add(new DiscordField("Total Level", String.valueOf(client.getTotalLevel()), true));
        fields.add(new DiscordField(
                "Combat Level",
                String.valueOf(client.getLocalPlayer().getCombatLevel()),
                true
        ));

        // ---- Rank icon + field ---------------------------------------------
        byte[] chatImageBytes;
        ClanSettings clan = client.getClanSettings();

        if (clan != null)
        {
            ClanRank rank = clan.findMember(playerName).getRank();
            ClanTitle titleRank = clan.titleForRank(rank);

            fields.add(new DiscordField("Rank", titleRank.getName(), true));

            BufferedImage rankImage = chatIconManager.getRankImage(titleRank);
            chatImageBytes = scaleWithPadding(
                    rankImage,
                    Constants.DISCORD_AUTHOR_ICON_SIZE,
                    Constants.DISCORD_AUTHOR_ICON_SCALE
            );
        }
        else
        {
            chatImageBytes = null;
            fields.add(new DiscordField("Rank", "not in clan", true));
        }

        // ---- Thumbnail -----------------------------------------------------
        byte[] deathThumbnail = scaleWithPadding(
                Icons.DEATH_IMAGE,
                Constants.DISCORD_THUMBNAIL_SIZE,
                1
        );

        // ---- Screenshot handling -------------------------------------------
        if (config.uploadscreenshots())
        {
            screenshotFuture.thenAcceptAsync(img -> {
                try {
                    byte[] screenshot = bufferedImageToBytes((BufferedImage) img);

                    sendDiscordEmbed(
                            death,
                            title,
                            Constants.DISCORD_DEATHS_COLOR,
                            playerName,
                            username,
                            url,
                            description,
                            fields,
                            screenshot,
                            chatImageBytes,
                            deathThumbnail
                    );
                } catch (Exception e) {
                    log.error("Failed to send death embed", e);
                }
            });
        }
        else
        {
            sendDiscordEmbed(
                    death,
                    title,
                    Constants.DISCORD_DEATHS_COLOR,
                    playerName,
                    username,
                    url,
                    description,
                    fields,
                    null,
                    chatImageBytes,
                    deathThumbnail
            );
        }
    }

    private String getWikiUrl(String wikiName){
        String encoded = wikiName.replace(" ", "_");
        return Constants.WIKI_SEARCH + encoded;
    }

    private static String itemImageUrl(int itemId)
    {
        return "https://static.runelite.net/cache/item/icon/" + itemId + ".png";
    }

    @Nullable
    private Integer getItemID(String itemName)
    {
        return itemSearcher.findItemId(itemName);
    }


    private int getHAPrice(String itemName) {
        ItemPrice item = findItem(itemName);
        if (item == null) return -1;

        return client.getItemDefinition(item.getId()).getHaPrice();
    }


    private int getWikiPrice(String itemName) {
        ItemPrice item = findItem(itemName);
        if (item == null) return -1;

        return itemManager.getWikiPrice(item);
    }


    private ItemPrice findItem(String itemName) {
        return itemManager.search(itemName).stream()
                .filter(it -> it.getName().equalsIgnoreCase(itemName))
                .findFirst()
                .orElse(null);
    }

    private byte[] getRankIcon(String playerName) throws IOException
    {
        ClanSettings clan = client.getClanSettings();
        if (clan == null)
            return null;

        ClanRank rank = clan.findMember(playerName).getRank();
        ClanTitle title = clan.titleForRank(rank);

        BufferedImage icon = chatIconManager.getRankImage(title);
        return scaleWithPadding(icon,
                Constants.DISCORD_AUTHOR_ICON_SIZE,
                Constants.DISCORD_AUTHOR_ICON_SCALE);
    }

    public byte[] scaleWithPadding(BufferedImage original, int iconSize, double scaleFactor) throws IOException
    {
        int visibleSize = (int) (iconSize * scaleFactor);

        // Create 20×20 transparent canvas
        BufferedImage padded = new BufferedImage(iconSize, iconSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = padded.createGraphics();

        // Enable high-quality scaling
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Scale original image to visibleSize
        Image scaled = original.getScaledInstance(visibleSize, visibleSize, Image.SCALE_SMOOTH);

        // Center inside the transparent 20×20 canvas
        int x = (iconSize - visibleSize) / 2;
        int y = (iconSize - visibleSize) / 2;

        g.drawImage(scaled, x, y, null);
        g.dispose();

        return bufferedImageToBytes(padded);
    }

    private byte[] bufferedImageToBytes(BufferedImage img) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


//    public CompletableFuture<Image> getScreenshot()
//    {
//        CompletableFuture<Image> f = new CompletableFuture<>();
//        boolean privacyMode = config.hidechats();
//
//        clientThread.invoke(() ->
//        {
//            boolean chatHidden = hideWidget(privacyMode, client, InterfaceID.Chatbox.CHATAREA);
//
//            drawManager.requestNextFrameListener(image ->
//            {
//                f.complete(image);
//
//                clientThread.invoke(() ->
//                        unhideWidget(chatHidden, client, clientThread, InterfaceID.Chatbox.CHATAREA)
//                );
//            });
//        });
//
//        return f;
//    }

    public void onGameTick()
    {
        if (pendingScreenshot == null)
        {
            return;
        }

//        if (screenshotDelayTicks == 1)
//        {
//            boolean privacyMode = config.hidechats();
//
//            clientThread.invoke(() ->
//            {
//                chatHiddenForScreenshot = hideWidget(
//                        privacyMode,
//                        client,
//                        InterfaceID.Chatbox.CHATAREA
//                );
//            });
//        }

        if (screenshotDelayTicks > 0)
        {
            screenshotDelayTicks--;
            return;
        }

        boolean privacyMode = config.hidechats();

        // STEP 1 — run on client thread
        clientThread.invoke(() ->
        {
            chatHiddenForScreenshot = hideWidget(
                    privacyMode,
                    client,
                    InterfaceID.Chatbox.CHATAREA
            );

            // STEP 2 — request render AFTER widgets are hidden
            drawManager.requestNextFrameListener(image ->
            {
                pendingScreenshot.complete(image);

                // STEP 3 — restore UI AFTER screenshot is taken
                clientThread.invoke(() ->
                {
                    unhideWidget(
                            chatHiddenForScreenshot,
                            client,
                            clientThread,
                            InterfaceID.Chatbox.CHATAREA
                    );

                    pendingScreenshot = null;
                });
            });
        });
    }

    public CompletableFuture<Image> getScreenshot()
    {
        return getScreenshot(1);
    }

    public CompletableFuture<Image> getScreenshot(int delayTicks)
    {
        CompletableFuture<Image> future = new CompletableFuture<>();

        pendingScreenshot = future;
        screenshotDelayTicks = delayTicks;
        return future;
    }


    public static boolean hideWidget(boolean shouldHide, Client client, @Component int info) {
        if (!shouldHide)
            return false;

        Widget widget = client.getWidget(info);
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

    private static class DiscordField{

        private final String name;
        private final String value;
        private final boolean inline;

        public DiscordField(String name, String value, boolean inline)
        {
            this.name = name;
            this.value = value;
            this.inline = inline;
        }

        public String getName() { return this.name; }

        public String getValue() { return this.value; }

        public boolean getInline() { return this.inline; }
    }

    private void sendDiscordEmbed(
            String webhookKey,
            String title,
            Color color,
            @Nullable String authorName,
            @Nullable String username,
            @Nullable String url,
            @Nullable String description,
            @Nullable List<DiscordField> fields,
            @Nullable byte[] screenshot,
            @Nullable byte[] userIcon,
            @Nullable byte[] thumbnailBytes
    ) {
        byte[] footerIcon = bufferedImageToBytes(Icons.RED_HELM_IMAGE);

        DiscordWebhook.EmbedObject embed = new DiscordWebhook.EmbedObject()
                .setTitle(title)
                .setColor(color)
                .setFooter("One Shot Plugin", footerIcon, "footericon.png");

        if (description != null) embed.setDescription(description);
        if (url != null) embed.setUrl(url);
        if (userIcon != null) embed.setAuthor(authorName, userIcon, "usericon.png");
        if (thumbnailBytes != null) embed.setThumbnail(thumbnailBytes, "thumb.png");
        if (screenshot != null) embed.setImage(screenshot, "screenshot.png");

        if (fields != null) {
            for (DiscordField f : fields) {
                embed.addField(f.getName(), f.getValue(), f.getInline());
            }
        }

        sendViaWorker(
                webhookKey,
                embed,
                screenshot,
                userIcon,
                footerIcon,
                thumbnailBytes
        );
    }


    private void sendDiscordEmbed(
            String webhookKey,
            String title,
            Color color,
            @Nullable String authorName,
            @Nullable String username,
            @Nullable String url,
            @Nullable String description,
            @Nullable List<DiscordField> fields,
            @Nullable byte[] screenshot,
            @Nullable byte[] userIcon,
            @Nullable String thumbnailUrl
    ) throws IOException //throws IOException
    {
        byte[] footerIcon = bufferedImageToBytes(Icons.RED_HELM_IMAGE);

        DiscordWebhook.EmbedObject embed = new DiscordWebhook.EmbedObject()
                .setTitle(title)
                .setColor(color)
                .setFooter("One Shot Plugin", footerIcon, "footericon.png");

        if (description != null) embed.setDescription(description);
        if (url != null) embed.setUrl(url);
        if (userIcon != null) embed.setAuthor(authorName, userIcon, "usericon.png");
        if (url != null) embed.setUrl(url);
        if (screenshot != null) embed.setImage(screenshot, "screenshot.png");

        if (fields != null) {
            for (DiscordField f : fields) {
                embed.addField(f.getName(), f.getValue(), f.getInline());
            }
        }

        sendViaWorker(
                webhookKey,
                embed,
                screenshot,
                userIcon,
                footerIcon,
                null
        );
    }

    private void sendViaWorker(
            String webhookKey,
            DiscordWebhook.EmbedObject embed,
            @Nullable byte[] screenshot,
            @Nullable byte[] userIcon,
            @Nullable byte[] footerIcon,
            @Nullable byte[] thumbnailBytes
    ) {
        try {
            // 1. Build embed JSON (like your toJson() method)
            DiscordWebhook.JSONObject embedJsonObj = embed.toJson(); // you added this earlier
            String embedJson = embedJsonObj.toString();

            String playerName = client.getLocalPlayer().getName();

            StringBuilder attachmentsJson = new StringBuilder();
            attachmentsJson.append("[");

            boolean first = true;

            if (screenshot != null) {
                if (!first) attachmentsJson.append(",");
                first = false;
                attachmentsJson.append(buildAttachmentJson("screenshot.png", screenshot));
            }
            if (userIcon != null) {
                if (!first) attachmentsJson.append(",");
                first = false;
                attachmentsJson.append(buildAttachmentJson("usericon.png", userIcon));
            }
            if (footerIcon != null) {
                if (!first) attachmentsJson.append(",");
                first = false;
                attachmentsJson.append(buildAttachmentJson("footericon.png", footerIcon));
            }
            if (thumbnailBytes != null) {
                if (!first) attachmentsJson.append(",");
                first = false;
                attachmentsJson.append(buildAttachmentJson("thumb.png", thumbnailBytes));
            }

            attachmentsJson.append("]");

            String jsonBody =
                    "{"
                            + "\"username\":\"" + escape(playerName) + "\","
                            + "\"clan_name\":\"One Shot\","
                            + "\"webhook\":\"" + escape(webhookKey) + "\","
                            + "\"content\":null,"
                            + "\"embeds\":[" + embedJson + "],"
                            + "\"attachments\":" + attachmentsJson
                            + "}";

            RequestBody body = RequestBody.create(JSON_MEDIA, jsonBody);

            Request request = new Request.Builder()
                    .url(WORKER_URL)
                    .post(body)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {
                    log.error("Failed to send to Worker", e);
                }

                @Override public void onResponse(Call call, Response response) throws IOException {
                    response.close();
                }
            });
        } catch (Exception e) {
            log.error("Error building Worker request", e);
        }
    }

    private String buildAttachmentJson(String filename, byte[] data) {
        String b64 = Base64.getEncoder().encodeToString(data);
        return "{"
                + "\"filename\":\"" + escape(filename) + "\","
                + "\"content_type\":\"image/png\","
                + "\"data\":\"" + b64 + "\""
                + "}";
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

}
