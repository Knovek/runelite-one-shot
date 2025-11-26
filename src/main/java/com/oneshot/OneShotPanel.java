package com.oneshot;

import com.google.gson.*;

import com.oneshot.utils.Constants;
import com.oneshot.utils.Icons;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.StyleContext;

import net.runelite.api.Client;
import net.runelite.api.gameval.SpriteID;
import net.runelite.api.Experience;
import net.runelite.client.game.ChatIconManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.hiscore.*;

import static com.oneshot.utils.Constants.BOSSES;
import static com.oneshot.utils.Constants.SKILLS;
import static com.oneshot.utils.Constants.ACTIVITIES;
import static net.runelite.client.hiscore.HiscoreSkill.*;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;
import net.runelite.client.util.QuantityFormatter;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class OneShotPanel extends PluginPanel
{   private static final Logger log = LoggerFactory.getLogger(OneShotPanel.class);

    private final Map<HiscoreSkill, JButton> skillButtons = new HashMap<>();
    private final RateLimitedHttpCache rateLimitedHttpCache = new RateLimitedHttpCache(20, 5);

    JLabel intro_top_text = new JLabel("", SwingConstants.CENTER);
    JLabel intro_bottom_text = new JLabel("", SwingConstants.CENTER);
    JPanel panelMainContent = new JPanel();

    String playerRank;

    ArrayList<OneShotPlugin.OneShotMember> allMembersRanksInfo;
    Map<String, ImageIcon> allMembersIcons;
    Map<String, String> allMembersDisplayNames;

    boolean isInInfoPanel = false;


    @Inject
    private ChatIconManager chatIconManager;

    @Inject
    private SpriteManager spriteManager;
    private String playerName;

    public class RateLimitedHttpCache {

        private static final long TTL_MILLIS = 60 * 1000; // 1 minute
        private final ConcurrentHashMap<String, CachedItem> cache = new ConcurrentHashMap<>();

        private final Semaphore rateLimiter;
        private final ScheduledExecutorService scheduler;
        private final HttpClient httpClient = HttpClient.newHttpClient();

        private class CachedItem {
            final String response;
            final long timestamp;

            CachedItem(String response, long timestamp) {
                this.response = response;
                this.timestamp = timestamp;
            }
        }

        public RateLimitedHttpCache(int maxRequests, int refillIntervalSeconds) {
            this.rateLimiter = new Semaphore(maxRequests);
            this.scheduler = Executors.newScheduledThreadPool(1);

            // Refill one token every interval
            scheduler.scheduleAtFixedRate(() -> {
                if (rateLimiter.availablePermits() < maxRequests) {
                    rateLimiter.release();
                }
            }, refillIntervalSeconds, refillIntervalSeconds, TimeUnit.SECONDS);
        }

        /**
         * Fetch a URL: returns cached response if fresh.
         * Returns null if rate limit has been exhausted.
         */
        public String fetch(String url) throws IOException, InterruptedException {
            CachedItem item = cache.get(url);
            long now = System.currentTimeMillis();

            if (item != null && now - item.timestamp < TTL_MILLIS) {
                return item.response;
            }

            // Non-blocking check for rate limiter
            boolean allowed = rateLimiter.tryAcquire();
            if (!allowed) {
                // Rate limit exhausted → return null immediately
                log.debug("no more tokens available");
                return null;
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();

            // Store in cache
            cache.put(url, new CachedItem(body, now));

            return body;
        }

        public void shutdown() {
            scheduler.shutdown();
        }
    }



    public void init()
    {
        buildIntroPanel();
    }

    public void deinit()
    {
        // TODO
    }

    private void update()
    {
        revalidate();
        repaint();
    }

    public void refresh(ArrayList<OneShotPlugin.OneShotMember> allMembersRanksInfo, Map<String, ImageIcon> membersIcons, Map<String, String> membersDisplayNames) throws IOException, InterruptedException {
        this.allMembersRanksInfo = allMembersRanksInfo;
        this.allMembersIcons = membersIcons;
        this.allMembersDisplayNames = membersDisplayNames;
    }

    public void buildIntroPanel(){
        removeAll();
        ImageIcon icon = Icons.RED_HELM;
        JLabel image = new JLabel(icon);

        setBorder(new EmptyBorder(10, 10, 10, 10));

        Font fontTitle;

        try (
                InputStream inRunescape = FontManager.class.getResourceAsStream("runescape.ttf");
        ) {
            Font font = Font.createFont(0, inRunescape).deriveFont(0, 16.0F);
            fontTitle = StyleContext.getDefaultStyleContext().getFont(font.getName(), 0, 64);
        } catch (FontFormatException ex) {
            throw new RuntimeException("Font loaded, but format incorrect.", ex);
        } catch (IOException ex) {
            throw new RuntimeException("Font file not found.", ex);
        }

        JLabel title = new JLabel("One Shot", SwingConstants.CENTER);
        title.setFont(fontTitle);

        changeIntroText1("Welcome to One Shot Plugin", Color.WHITE);
        changeIntroText2("Please enter the clan chat to continue", Color.WHITE);


        add(title);
        add(Box.createGlue());
        add(image);
        add(Box.createGlue());
        add(Box.createGlue());
        add(intro_top_text);
        add(intro_bottom_text);

        update();
    }

    public void changeIntroText1(String text, Color color){
        intro_top_text.setText(text);
        intro_top_text.setForeground(color);
        update();
    }

    public void changeIntroText2(String text, Color color){
        intro_bottom_text.setText(text);
        intro_bottom_text.setForeground(color);
        update();
    }

    public void buildMainPanel(boolean isModerator, String playerName, String clanRankName, ImageIcon iconRank,
                               ArrayList<OneShotPlugin.OneShotMember> allMembersRanksInfo, Map<String, ImageIcon> members,
                               Map<String, String> allMembersDisplayNames)
    {

        this.allMembersRanksInfo = allMembersRanksInfo;
        this.allMembersIcons = members;
        this.allMembersDisplayNames = allMembersDisplayNames;

        removeAll();

        this.playerName = playerName;
        this.playerRank = clanRankName;

        JPanel playerRankPanel = buildPlayerPanel(playerName, clanRankName, iconRank);

        // button panel
        int nIcons = isModerator ? Constants.BUTTON_NUMBER : Constants.BUTTON_NUMBER - 1;

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1, nIcons, 5, 5));

        ImageIcon iconInfo = Icons.INFO;
        ImageIcon iconRanks = Icons.EVENT;
        ImageIcon iconDiscord = Icons.DISCORD;
        ImageIcon iconScout = Icons.SCOUT;

        JButton infoButton = buildButton(iconInfo, () -> {
            try {
                buildInfoPanel();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, Constants.TIP_INFO);
        JButton ranksButtons = buildButton(iconRanks, this::buildRanksPanel, Constants.TIP_RANKS);
        JButton discordButton = buildButton(iconDiscord, this::buildDiscordPanel, Constants.TIP_DISCORD);
        JButton scoutButton = buildButton(iconScout, this::buildScoutPanel, Constants.TIP_SCOUT);

        buttonPanel.add(infoButton);
        buttonPanel.add(ranksButtons);
        buttonPanel.add(discordButton);
        if (isModerator)
        {
            buttonPanel.add(scoutButton);
        }

        add(Box.createVerticalStrut(5));
        add(playerRankPanel);

        add(Box.createVerticalStrut(5));
        add(buttonPanel);

        // Main Panel
        panelMainContent.removeAll();
        add(panelMainContent);

        // Version Panel
        update();
    }

    private void buildInfoPanel() throws IOException, InterruptedException {
        isInInfoPanel = true;

        // all members ranks table TODO: add reminder to rank up if not done so yet

        panelMainContent.removeAll();
        JPanel allMembersRanks = buildAllMembersRanksTotal();
        panelMainContent.add(allMembersRanks);

        update();
    }

    private JPanel buildTopChartsPanel() throws IOException, InterruptedException {
        JPanel container = new JPanel();

        container.setBackground(ColorScheme.DARK_GRAY_COLOR);
        container.setLayout(new GridBagLayout());

        JLabel bottomText = new JLabel("One Shot top players Skills and KCs", SwingConstants.CENTER);

        String response = rateLimitedHttpCache.fetch(Constants.URI_WOM_LEADERS);

        //log.debug(String.valueOf(Instant.now().getEpochSecond()));

        JsonParser jsonParser = new JsonParser();
        JsonElement jsonElement = jsonParser.parse(response);

        JsonElement metricLeaders = jsonElement.getAsJsonObject().get(Constants.URI_WOM_LEADERS_OBJECT);

        // Expand sub items to fit width of panel, align to top of panel
        GridBagConstraints c = createDefaultGBC();
        c.insets = new Insets(1, 1, 1, 1);

        container.add(bottomText, c);
        c.gridy++;

        // Panel that holds skill icons
        JPanel statsPanel = new JPanel();
        statsPanel.setLayout(new GridLayout(8, 3));
        statsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        statsPanel.setBorder(new EmptyBorder(5, 0, 5, 0));

        // For each skill on the ingame skill panel, create a Label and add it to the UI
        for (HiscoreSkill skill : SKILLS)
        {
            JPanel panel = makeHiscorePanel(skill);
            statsPanel.add(panel);
        }

        container.add(statsPanel, c);
        c.gridy++;

        JPanel totalPanel = new JPanel();
        totalPanel.setLayout(new GridLayout(1, 1));
        totalPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        totalPanel.add(makeHiscorePanel(OVERALL));

        container.add(totalPanel, c);
        c.gridy++;

        JPanel minigamePanel = new JPanel();
        minigamePanel.setLayout(new GridLayout(1, 2));
        minigamePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        for (HiscoreSkill skill : ACTIVITIES)
        {
            JPanel panel = makeHiscorePanel(skill);
            minigamePanel.add(panel);
        }

        container.add(minigamePanel, c);
        c.gridy++;

        JPanel bossPanel = new JPanel();
        bossPanel.setLayout(new GridLayout(0, 3));
        bossPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        // For each boss on the hi-scores, create a Label and add it to the UI
        for (HiscoreSkill skill : BOSSES)
        {
            JPanel panel = makeHiscorePanel(skill);
            bossPanel.add(panel);
        }

        container.add(bossPanel, c);
        c.gridy++;

        //populateMetricLeaders(metricLeaders);
        populateMetricLeadersAsync(metricLeaders);

        return container;
    }

    private void buildTopChartsAsync() {

        SwingWorker<JPanel, Void> worker = new SwingWorker<>() {

            @Override
            protected JPanel doInBackground() throws Exception {
                // EVERYTHING SLOW happens here
                return buildTopChartsPanel();
            }

            @Override
            protected void done() {
                try {
                    JPanel panel = get();

                    // Add panel to your UI (EDT)
                    panelMainContent.removeAll();
                    panelMainContent.add(panel);
                    panelMainContent.revalidate();
                    panelMainContent.repaint();

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };

        worker.execute();
    }

    private void populateMetricLeadersAsync(JsonElement metricLeaders) {

        SwingWorker<Map<JButton, ButtonUpdate>, Void> worker = new SwingWorker<>() {

            @Override
            protected Map<JButton, ButtonUpdate> doInBackground() {
                Map<JButton, ButtonUpdate> updates = new HashMap<>();

                for (Map.Entry<HiscoreSkill, JButton> entry : skillButtons.entrySet()) {

                    HiscoreSkill skill = entry.getKey();
                    JButton button = entry.getValue();

                    // compute everything in the background
                    ButtonUpdate update = computeButtonInfo(skill, metricLeaders);

                    // store result (no Swing calls here!)
                    updates.put(button, update);
                }

                return updates;
            }

            @Override
            protected void done() {
                try {
                    // safely apply updates on the Swing thread
                    Map<JButton, ButtonUpdate> updates = get();

                    for (Map.Entry<JButton, ButtonUpdate> entry : updates.entrySet()) {
                        JButton button = entry.getKey();
                        ButtonUpdate update = entry.getValue();

                        button.setForeground(update.color);
                        button.setText(update.text);
                        button.setToolTipText(update.tooltip);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        worker.execute();
    }

    private ButtonUpdate computeButtonInfo(HiscoreSkill skill, JsonElement metricLeaders) {

        String name = normalizeSkillName(skill);
        String element;
        String id;
        JsonElement metric;

        if (SKILLS.contains(skill) || name.equals("overall")) {
            element = "skills";
            id = "experience";
            metric = metricLeaders.getAsJsonObject().get(element)
                    .getAsJsonObject().get(name);

            Color col = metric.getAsJsonObject()
                    .get("player").getAsJsonObject()
                    .get("displayName").getAsString().equals(playerName)
                    ? Color.GREEN : Color.WHITE;

            String text = name.equals("overall")
                    ? metric.getAsJsonObject().get("level").getAsString()
                    : String.valueOf(Experience.getLevelForXp(
                    metric.getAsJsonObject().get(id).getAsInt()));

            return new ButtonUpdate(col, text, detailsHtml(skill, metric));
        }

        else if (BOSSES.contains(skill)) {
            element = "bosses";
            id = "kills";
            metric = metricLeaders.getAsJsonObject().get(element)
                    .getAsJsonObject().get(name);

            double kills = metric.getAsJsonObject().get(id).getAsDouble();
            Color col = kills == 0 ? Color.RED :
                    metric.getAsJsonObject().get("player")
                            .getAsJsonObject().get("displayName")
                            .getAsString().equals(playerName)
                            ? Color.GREEN : Color.WHITE;

            return new ButtonUpdate(col, metric.getAsJsonObject().get(id).getAsString(),
                    detailsHtml(skill, metric));
        }

        else if (ACTIVITIES.contains(skill)) {
            element = "activities";
            id = "score";
            metric = metricLeaders.getAsJsonObject().get(element)
                    .getAsJsonObject().get(name);

            double score = metric.getAsJsonObject().get(id).getAsDouble();
            Color col = score == 0 ? Color.RED :
                    metric.getAsJsonObject().get("player")
                            .getAsJsonObject().get("displayName")
                            .getAsString().equals(playerName)
                            ? Color.GREEN : Color.WHITE;

            return new ButtonUpdate(col,
                    metric.getAsJsonObject().get(id).getAsString(),
                    detailsHtml(skill, metric));
        }

        // fallback
        return new ButtonUpdate(Color.WHITE, "?", "Unknown");
    }

    private static class ButtonUpdate {
        final Color color;
        final String text;
        final String tooltip;

        ButtonUpdate(Color color, String text, String tooltip) {
            this.color = color;
            this.text = text;
            this.tooltip = tooltip;
        }
    }

    private String detailsHtml(HiscoreSkill skill, JsonElement metricLeader) {

        JsonObject obj = metricLeader.getAsJsonObject();
        JsonObject player = obj.getAsJsonObject("player");
        String displayName = player.get("displayName").getAsString();

        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='padding:5px;color:#989898'>");

        boolean isSkill = SKILLS.contains(skill) || skill.getName().equalsIgnoreCase("overall");
        boolean isBoss  = BOSSES.contains(skill);
        boolean isActivity = ACTIVITIES.contains(skill);

        if (isSkill) {
            sb.append("<p><span style='color:white'>Skill:</span> ").append(skill.getName()).append("</p>");
            sb.append("<p><span style='color:white'>Name:</span> ").append(displayName).append("</p>");
            sb.append("<p><span style='color:white'>Rank:</span> ").append(QuantityFormatter.formatNumber(obj.get("rank").getAsDouble())).append("</p>");
            //sb.append("<p><span style='color:white'>Level:</span> ").append(QuantityFormatter.formatNumber(obj.get("level").getAsDouble())).append("</p>");
            sb.append("<p><span style='color:white'>Experience:</span> ").append(QuantityFormatter.formatNumber(obj.get("experience").getAsDouble())).append("</p>");
        }
        else if (isBoss) {
            sb.append("<p><span style='color:white'>Boss:</span> ").append(skill.getName()).append("</p>");

            double kills = obj.get("kills").getAsDouble();
            if (kills > 0) {
                double rank = obj.get("rank").getAsDouble();

                sb.append("<p><span style='color:white'>Name:</span> ").append(displayName).append("</p>");
                sb.append("<p><span style='color:white'>Rank:</span> ")
                        .append(rank > 0 ? QuantityFormatter.formatNumber(rank) : "--")
                        .append("</p>");
                //sb.append("<p><span style='color:white'>KC:</span> ").append(QuantityFormatter.formatNumber(kills)).append("</p>");
            } else {
                sb.append("<p>No one is ranked yet</p>");
            }
        }
        else if (isActivity) {
            sb.append("<p><span style='color:white'>").append(skill.getName()).append("</span></p>");

            double rank = obj.get("rank").getAsDouble();
            if (rank > 0) {
                sb.append("<p><span style='color:white'>Name:</span> ").append(displayName).append("</p>");
                sb.append("<p><span style='color:white'>Rank:</span> ").append(QuantityFormatter.formatNumber(rank))
                        .append("</p>");
            } else {
                sb.append("<p>No one is ranked yet</p>");
            }
        }
        else {
            log.debug("Houston, we have a problem");
            return "";
        }

        sb.append("</body></html>");
        return sb.toString();
    }


    private JPanel makeHiscorePanel(HiscoreSkill skill)
    {
        HiscoreSkillType skillType = skill == null ? HiscoreSkillType.SKILL : skill.getType();

        JButton button = new JButton();
        button.setToolTipText(skill == null ? "Combat" : skill.getName());
        button.setFont(FontManager.getRunescapeSmallFont());
        button.setText(pad("--", skillType));
        button.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        button.setMargin(new Insets(0,0,0,0));
        button.setBorderPainted(false);
        button.setPreferredSize(new Dimension(65,25));


        spriteManager.getSpriteAsync(skill == null ? SpriteID.SideIcons.COMBAT : skill.getSpriteId(), 0, (sprite) ->
                SwingUtilities.invokeLater(() ->
                {
                    // Icons are all 25x25 or smaller, so they're fit into a 25x25 canvas to give them a consistent size for
                    // better alignment. Further, they are then scaled down to 20x20 to not be overly large in the panel.
                    final BufferedImage scaledSprite = ImageUtil.resizeImage(ImageUtil.resizeCanvas(sprite, 25, 25), 20, 20);
                    button.setIcon(new ImageIcon(scaledSprite));
                }));

        boolean totalLabel = skill == OVERALL || skill == null; //overall or combat
        button.setIconTextGap(totalLabel ? 10 : 4);


        final Color hoverColor = ColorScheme.DARKER_GRAY_HOVER_COLOR;
        final Color pressedColor = ColorScheme.DARKER_GRAY_COLOR.brighter();

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                button.setBackground(pressedColor);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                buildSkillPlayersAsync(skill, 1);
                button.setBackground(hoverColor);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hoverColor);
                button.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(ColorScheme.DARKER_GRAY_COLOR);
                button.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });

        JPanel skillPanel = new JPanel();
        skillPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        skillPanel.setBorder(new EmptyBorder(2, 0, 2, 0));
        skillButtons.put(skill, button);
        skillPanel.add(button);

        return skillPanel;
    }

    private void buildSkillPlayers(HiscoreSkill skill, int pageNumber) throws IOException, InterruptedException {
        panelMainContent.removeAll();

        String skillName = normalizeSkillName(skill);
        JsonArray arr = fetchSkillData(skillName);

        if (arr == null)
        {
            JPanel empty = new JPanel();
            empty.setLayout(new BoxLayout(empty, BoxLayout.Y_AXIS));
            empty.add(goBackButton());

            JLabel message1 = new JLabel("Hey wow, too fast!");
            message1.setForeground(Color.RED); // red text
            empty.add(message1);

            JLabel message2 = new JLabel("Please slow down");
            message2.setForeground(Color.RED); // red text
            empty.add(message2);

            panelMainContent.add(empty);
            update();
            return;
        }

        if (arr.size() == 0) {
            JPanel empty = new JPanel();
            empty.setLayout(new BoxLayout(empty, BoxLayout.Y_AXIS));
            empty.add(goBackButton());

            JLabel message = new JLabel("Seems no one is on the Hiscores");
            message.setForeground(Color.RED); // red text
            empty.add(message);

            panelMainContent.add(empty);
            update();
            return;
        }

        int nPages = (int) Math.ceil(arr.size() / 10.0);
        int playerIndex = (pageNumber - 1) * 10;
        int playerLimit = Math.min(10, arr.size() - playerIndex);

        int highlightPosition = findPlayerPosition(arr, this.playerName);
        //log.debug("Player position: {}", highlightPosition);

        JPanel container = new JPanel(new GridBagLayout());
        GridBagConstraints c = createDefaultGBC();

        // Top UI header
        addHeader(container, c, skill);

        // Player list panel
        JPanel playerList = new JPanel(new GridBagLayout());
        buildPlayerList(
                playerList,
                arr,
                skill,
                skillName,
                playerIndex,
                playerLimit,
                highlightPosition
        );

        container.add(playerList, c);
        c.gridy++;

        // Page scroller
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.weighty = 0; // page scroller doesn't expand vertically
        container.add(
                buildSkillPlayersScroller(skill, pageNumber, nPages, highlightPosition),
                c
        );
        c.gridy++;

        panelMainContent.add(container);
        update();
    }

    private void buildSkillPlayersAsync(HiscoreSkill skill, int pageNumber) {

        // STEP 1: show a temporary loading indicator
        panelMainContent.removeAll();
        panelMainContent.add(new JLabel("Loading...", SwingConstants.CENTER));
        update(); // revalidate+repaint

        SwingWorker<JsonArray, Void> worker = new SwingWorker<>() {

            @Override
            protected JsonArray doInBackground() throws Exception {
                String skillName = normalizeSkillName(skill);
                return fetchSkillData(skillName);   // <-- NO UI freeze now
            }

            @Override
            protected void done() {
                try {
                    JsonArray arr = get();
                    // now build UI on EDT
                    buildSkillPlayersUI(skill, pageNumber, arr);

                } catch (Exception e) {
                    e.printStackTrace();
                    buildSkillPlayersUI(skill, pageNumber, null);
                }
            }
        };

        worker.execute();
    }

    private void buildSkillPlayersUI(HiscoreSkill skill, int pageNumber, JsonArray arr) {
        panelMainContent.removeAll();

        if (arr == null)
        {
            JPanel empty = new JPanel();
            empty.setLayout(new BoxLayout(empty, BoxLayout.Y_AXIS));
            empty.add(goBackButton());

            JLabel message1 = new JLabel("Hey wow, too fast!");
            message1.setForeground(Color.RED);
            empty.add(message1);

            JLabel message2 = new JLabel("Please slow down");
            message2.setForeground(Color.RED);
            empty.add(message2);

            panelMainContent.add(empty);
            update();
            return;
        }

        if (arr.size() == 0) {
            JPanel empty = new JPanel();
            empty.setLayout(new BoxLayout(empty, BoxLayout.Y_AXIS));
            empty.add(goBackButton());

            JLabel message = new JLabel("Seems no one is on the Hiscores");
            message.setForeground(Color.RED);
            empty.add(message);

            panelMainContent.add(empty);
            update();
            return;
        }

        String skillName = normalizeSkillName(skill);

        int nPages = (int) Math.ceil(arr.size() / 10.0);
        int playerIndex = (pageNumber - 1) * 10;
        int playerLimit = Math.min(10, arr.size() - playerIndex);

        int highlightPosition = findPlayerPosition(arr, this.playerName);

        JPanel container = new JPanel(new GridBagLayout());
        GridBagConstraints c = createDefaultGBC();

        addHeader(container, c, skill);

        JPanel playerList = new JPanel(new GridBagLayout());
        buildPlayerList(
                playerList,
                arr,
                skill,
                skillName,
                playerIndex,
                playerLimit,
                highlightPosition
        );

        container.add(playerList, c);
        c.gridy++;

        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.weighty = 0;
        container.add(
                buildSkillPlayersScroller(skill, pageNumber, nPages, highlightPosition),
                c
        );
        c.gridy++;

        panelMainContent.add(container);
        update();
    }



    private JPanel buildSkillPlayersScroller(HiscoreSkill skill, int currentPage, int nPages, int playerPosition) {

        JPanel container = new JPanel(new GridBagLayout());

        GridBagConstraints c = createDefaultGBC();
        log.debug(String.valueOf(playerPosition));

        int playerPage = (playerPosition / 10) + 1; // each page shows 10 players

        // Navigation actions
        Runnable goFirst = () -> safeBuildSkillPlayers(skill, 1);
        Runnable goPrev  = () -> safeBuildSkillPlayers(skill, currentPage - 1);
        Runnable goNext  = () -> safeBuildSkillPlayers(skill, currentPage + 1);
        Runnable goLast  = () -> safeBuildSkillPlayers(skill, nPages);
        Runnable gotoPlayer  = () -> safeBuildSkillPlayers(skill, playerPage);

        // Create the navigation buttons
        JButton btnFirst = buildButton("<<", currentPage > 1, 35, 20, goFirst);
        JButton btnPrev  = buildButton("<",  currentPage > 1, 35, 20, goPrev);
        JButton btnNext  = buildButton(">",  currentPage < nPages, 35, 20, goNext);
        JButton btnLast  = buildButton(">>", currentPage < nPages, 35, 20, goLast);
        JButton btnGoToPlayer = buildButton("Go to my position", currentPage != playerPage && playerPosition != -1, 0, 20, gotoPlayer);

        JLabel pageLabel = new JLabel(String.valueOf(currentPage), SwingConstants.CENTER);

        // Layout
        container.add(btnFirst, c);
        c.gridx++;
        container.add(btnPrev, c);
        c.gridx++;
        container.add(pageLabel, c);
        c.gridx++;
        container.add(btnNext, c);
        c.gridx++;
        container.add(btnLast, c);

        // -----------------------------
        // Add "Go to Player" button below
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 5;
        c.fill = GridBagConstraints.HORIZONTAL;

        container.add(btnGoToPlayer, c);

        return container;
    }

    private void safeBuildSkillPlayers(HiscoreSkill skill, int page) {
        SwingUtilities.invokeLater(() -> {   // safe UI thread update
            buildSkillPlayersAsync(skill, page);
        });
    }


    private JButton buildButton(String displayText, boolean enableClick, int width, int height, Runnable callback)
    {
        final Color hoverColor = ColorScheme.DARKER_GRAY_HOVER_COLOR;
        final Color pressedColor = ColorScheme.DARKER_GRAY_COLOR.brighter();
        final Color defaultColor = enableClick ? ColorScheme.DARKER_GRAY_COLOR : ColorScheme.DARK_GRAY_COLOR;

        JButton button = new JButton(displayText);
        button.setPreferredSize(new Dimension(width, height));
        button.setBackground(defaultColor);
        button.setBorderPainted(enableClick);

        if (enableClick) {
            button.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    button.setBackground(pressedColor);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    callback.run();
                    button.setBackground(hoverColor);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    button.setBackground(hoverColor);
                    button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    button.setBackground(defaultColor);
                    button.setCursor(Cursor.getDefaultCursor());
                }
            });
        } else {
            button.setForeground(defaultColor);
            // Disabled button: just ensure consistent color, no hover effects needed
            button.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) { button.setBackground(defaultColor); }
                @Override
                public void mouseExited(MouseEvent e) { button.setBackground(defaultColor); }
                @Override
                public void mousePressed(MouseEvent e) { button.setBackground(defaultColor); }
                @Override
                public void mouseReleased(MouseEvent e) { button.setBackground(defaultColor); }
            });
            button.setRolloverEnabled(false);
            button.setFocusPainted(false);
//            button.setEnabled(false);
        }

        return button;
    }

    private int findPlayerPosition(JsonArray jsonArray, String playerName){
        for (int i = 0; i < jsonArray.size(); i++){
            String displayName = jsonArray.get(i).getAsJsonObject().get("player").getAsJsonObject().get("username").getAsString();
            displayName = displayName.replace(" ", " ");
            if (Objects.equals(playerName.toLowerCase(), displayName)) { return i; }
        }
        return -1;
    }

    private static final NavigableMap<Long, String> suffixes = new TreeMap<> ();
    static {
        suffixes.put(1_000L, "k");
        suffixes.put(1_000_000L, "M");
        suffixes.put(1_000_000_000L, "B");
    }

    private String formatNumber (long value)
    {
        //Long.MIN_VALUE == -Long.MIN_VALUE so we need an adjustment here
        if (value == Long.MIN_VALUE) return formatNumber(Long.MIN_VALUE + 1);
        if (value < 0) return "-" + formatNumber(-value);
        if (value < 1000) return Long.toString(value); //deal with easy case

        Map.Entry<Long, String> e = suffixes.floorEntry(value);
        Long divideBy = e.getKey();
        String suffix = e.getValue();

        long div = value / divideBy;          // Whole part
        long dec1 = (value * 10 / divideBy) % 10;     // One decimal
        long dec2 = (value * 100 / divideBy) % 100;   // Two decimals

        boolean show2 = div < 10;
        boolean show1 = div < 100;

        if (show2) {
            return String.format("%d.%02d%s", div, dec2, suffix);
        } else if (show1) {
            return String.format("%d.%d%s", div, dec1, suffix);
        } else {
            return String.format("%d%s", div, suffix);
        }
    }

    private JButton goBackButton() {
        JButton goBack = new JButton("< Go Back"); // added "<"
        goBack.setBorderPainted(false);

        final Color hoverColor = ColorScheme.DARKER_GRAY_HOVER_COLOR;
        final Color pressedColor = ColorScheme.DARKER_GRAY_COLOR.brighter();

        // Optional: smaller preferred width to reduce horizontal space
        goBack.setPreferredSize(new Dimension(80, 20));
        goBack.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        goBack.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                goBack.setBackground(pressedColor);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                panelMainContent.removeAll();
                buildTopChartsAsync();
                goBack.setBackground(hoverColor);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                goBack.setBackground(hoverColor);
                goBack.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                goBack.setBackground(ColorScheme.DARKER_GRAY_COLOR);
                goBack.setCursor(Cursor.getDefaultCursor());
            }
        });

        // Align to the left in its container
        goBack.setHorizontalAlignment(SwingConstants.LEFT);

        return goBack;
    }

    private static String pad(String str, HiscoreSkillType type)
    {
        // Left pad label text to keep labels aligned
        int pad = type == HiscoreSkillType.BOSS ? 4 : 2;
        return StringUtils.leftPad(str, pad);
    }

    private JPanel buildAllMembersRanksTotal() {

        JPanel container = new JPanel();

        container.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.ipadx = 10;
        c.ipady = 1;
        c.weightx=0.5;

        JLabel HeaderIcon = new JLabel("Icon");
        HeaderIcon.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel HeaderRank = new JLabel("Rank");
        JLabel HeaderOnline = new JLabel("Online");
        HeaderOnline.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel HeaderTotal = new JLabel("Total");
        HeaderTotal.setHorizontalAlignment(SwingConstants.CENTER);
        container.add(HeaderIcon, c);
        c.gridx = 1;
        container.add(HeaderRank, c);
        c.gridx = 2;
        container.add(HeaderOnline, c);
        c.gridx = 3;
        container.add(HeaderTotal, c);

        //ArrayList<OneShotPlugin.MemberRank> allMembersRanksInfo;

        for (OneShotPlugin.OneShotMember oneShotMember : allMembersRanksInfo)
        {
            JLabel iconLabel = new JLabel();
            iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
            iconLabel.setIcon(oneShotMember.getIcon());
            JLabel rankLabel = new JLabel();
            rankLabel.setText(oneShotMember.getName());
            JLabel onlineLabel = new JLabel();
            onlineLabel.setHorizontalAlignment(SwingConstants.CENTER);
            onlineLabel.setText(String.valueOf(oneShotMember.getOnline()));
            JLabel totalLabel = new JLabel();
            totalLabel.setHorizontalAlignment(SwingConstants.CENTER);
            totalLabel.setText(String.valueOf(oneShotMember.getTotal()));
            if (Objects.equals(playerRank, oneShotMember.getName())) {
                rankLabel.setForeground(Color.GREEN);
                onlineLabel.setForeground(Color.GREEN);
                totalLabel.setForeground(Color.GREEN);
            };

            c.gridy++;
            c.gridx = 0;
            container.add(iconLabel, c);
            c.gridx = 1;
            container.add(rankLabel, c);
            c.gridx = 2;
            container.add(onlineLabel, c);
            c.gridx = 3;
            container.add(totalLabel, c);
        }

        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 4;

        JLabel discordPlug = new JLabel("/rank in discord #bot-commands", SwingConstants.CENTER);
        discordPlug.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        container.add(discordPlug, c);

        return container;
    }

    private void buildDiscordPanel() {
        LinkBrowser.browse(Constants.LINK_DISCORD);
    }

    private void buildRanksPanel()
    {
        panelMainContent.removeAll();
        try {
            buildTopChartsAsync();
        } catch (NullPointerException e)
        {
            panelMainContent.add(new JLabel("Something went wrong"));
            log.error(e.getMessage());
        }
        update();
    }

    private void buildScoutPanel() {
        panelMainContent.removeAll();
        isInInfoPanel = false;
        JLabel info = new JLabel("Work In Progress");
        panelMainContent.add(info);

        update();
    }

    private static JButton buildButton(ImageIcon icon, Runnable callback)
    {
        return buildButton(icon, callback, "");
    }

    private static JButton buildButton(ImageIcon icon, Runnable callback, String tip)
    {
        JButton button = new JButton(icon);

        button.setBackground(ColorScheme.DARK_GRAY_COLOR);
        button.setBorderPainted(false);

        if (!Objects.equals(tip, ""))
        {
            button.setToolTipText(tip);
        }

        final Color hoverColor = ColorScheme.DARKER_GRAY_HOVER_COLOR;
        final Color pressedColor = ColorScheme.DARKER_GRAY_COLOR.brighter();

        button.setPreferredSize(new Dimension(Constants.BUTTON_SIZE, Constants.BUTTON_SIZE));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                button.setBackground(pressedColor);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                callback.run();
                button.setBackground(hoverColor);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hoverColor);
                button.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(ColorScheme.DARK_GRAY_COLOR);
                button.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });

        return button;
    }


    private static JPanel buildPlayerPanel(String playerName, String clanRankName, ImageIcon iconRank)
    {

        JPanel container  = new JPanel();
        container.setLayout(new GridLayout(1, 1));

        JPanel rankPanel = buildRankPanel(clanRankName, iconRank);

        JLabel playerLabel = new JLabel(playerName);
        JPanel playerPanel = new JPanel();
        playerPanel.add(playerLabel, BorderLayout.CENTER);
        container.add(playerPanel, BorderLayout.WEST);
        container.add(rankPanel, BorderLayout.EAST);

        return container;
    }

    private static JPanel buildRankPanel(String clanRankName, ImageIcon iconRank)
    {
        JLabel clanRankLabel = new JLabel(clanRankName);
        JLabel iconLabel = new JLabel(iconRank);
        JPanel container = new JPanel();

        container.add(iconLabel, BorderLayout.CENTER);
        container.add(clanRankLabel, BorderLayout.CENTER);

        return container;
    }

    private static JLabel buildVersionPanel()
    {
        JLabel versionLabel = new JLabel();
        String versionText = "<html><body style = 'color:#a5a5a5'>Plugin version: " + Constants.version + "</span></body></html>";
        versionLabel.setText(versionText);
        return versionLabel;
    }

    private GridBagConstraints createDefaultGBC()
    {
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.ipadx = 10;
        c.ipady = 1;
        c.weightx = 1;
        c.weighty = 0;
        return c;
    }

    private void addHeader(JPanel container, GridBagConstraints c, HiscoreSkill skill) {
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0;
        container.add(goBackButton(), c);
        c.gridy++;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        container.add(new JLabel(" "), c);
        c.gridy++;
        container.add(new JLabel(skill.getName(), SwingConstants.CENTER), c);
        c.gridy++;
        container.add(new JLabel(" "), c);
        c.gridy++;
    }

    private String normalizeSkillName(HiscoreSkill skill) {
        String name = skill.toString().toLowerCase();
        return name.equals("runecraft") ? "runecrafting" : name.equals("clue_scroll_all") ? "clue_scrolls_all" : name;
    }

    private JsonArray fetchSkillData(String name)
            throws IOException, InterruptedException
    {
        String url = Constants.URI_WOM_SKILL_LEADERS + name + Constants.URI_WOM_SKILL_LEADERS_LIMIT;
        String response = rateLimitedHttpCache.fetch(url);
        if (response == null) { return null; }

        JsonParser jsonParser = new JsonParser();
        JsonArray arr = jsonParser.parse(response).getAsJsonArray();

        return cleanSkillJson(arr);
    }


    private JsonArray cleanSkillJson(JsonArray arr) {
        JsonArray cleaned = new JsonArray();

        for (int i = 0; i < arr.size(); i++) {
            JsonObject obj = arr.get(i).getAsJsonObject();
            JsonObject player = obj.getAsJsonObject("player");
            JsonObject data = obj.getAsJsonObject("data");

            int rank = data.get("rank").getAsInt();
            String username = player.get("username").getAsString().replace("\u00A0", " ");

            if (rank == -1) {
                //log.debug("Rank -1 found at index {} ({}), stopping and cutting the tail", i, username);
                break; // stop processing the rest of the array
            }

            if (!allMembersDisplayNames.containsKey(username)) {
                //log.debug("Removed unknown username {} ({})", i, username);
                continue; // skip this element, don’t add to cleaned array
            }

            cleaned.add(obj); // only valid elements are added
        }

        //log.debug("Array size after cleaning: {}", cleaned.size());

        return cleaned;
    }


    private void buildPlayerList(
            JPanel playerList,
            JsonArray arr,
            HiscoreSkill skill,
            String skillName,
            int startIndex,
            int count,
            int highlightIndex)
    {
        GridBagConstraints c = createDefaultGBC();

        boolean isSkill = SKILLS.contains(skill) || skillName.equals("overall");
        boolean isBoss = BOSSES.contains(skill);

        // Header row
        playerList.add(new JLabel("#", SwingConstants.CENTER), c);
        c.gridx++;
        playerList.add(new JLabel("Player", SwingConstants.CENTER), c);
        c.gridx++;
        playerList.add(new JLabel(isSkill ? "Level" : isBoss ? "Kills" : "Total", SwingConstants.CENTER), c);
        c.gridx++;
        if (isSkill)
            playerList.add(new JLabel("Exp", SwingConstants.CENTER), c);
        c.gridy++;

        // Player rows
        for (int i = startIndex; i < startIndex + count; i++)
        {
            JsonObject entry = arr.get(i).getAsJsonObject();
            JsonObject pdata = entry.getAsJsonObject("player");
            JsonObject ddata = entry.getAsJsonObject("data");

            String lowerName = pdata.get("username").getAsString().replace("\u00A0", " ");
            String displayName = allMembersDisplayNames.get(lowerName);
            ImageIcon icon = allMembersIcons.get(lowerName);

            // Position
            c.gridx = 0;
            playerList.add(new JLabel(String.valueOf(i + 1), SwingConstants.CENTER), c);
            c.gridx++;

            // Name (highlight player)
            JLabel nameLabel = new JLabel(displayName, icon, SwingConstants.LEFT);
            if (i == highlightIndex)
                nameLabel.setForeground(Color.GREEN);

            playerList.add(nameLabel, c);
            c.gridx++;

            // Level or KC
            if (isSkill) {
                playerList.add(new JLabel(ddata.get("level").getAsString(), SwingConstants.CENTER), c);
                c.gridx++;

                long xp = ddata.get("experience").getAsLong();
                JLabel xpLabel = new JLabel(formatNumber(xp), SwingConstants.CENTER);
                xpLabel.setToolTipText(QuantityFormatter.formatNumber(xp));
                playerList.add(xpLabel, c);
            }
            else if (isBoss){
                playerList.add(new JLabel(ddata.get("kills").getAsString(), SwingConstants.CENTER), c);
            }
            else {
                playerList.add(new JLabel(ddata.get("score").getAsString(), SwingConstants.CENTER), c);
            }

            c.gridy++;
        }
    }
}



