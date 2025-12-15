package com.oneshot;

import com.google.gson.*;

import com.oneshot.modules.ModToolsPanel;
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
import java.util.List;
import java.util.concurrent.*;
import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.text.StyleContext;

import net.runelite.api.Client;
import net.runelite.api.gameval.SpriteID;
import net.runelite.api.Experience;
import net.runelite.client.callback.ClientThread;
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
{
    private static final Logger log = LoggerFactory.getLogger(OneShotPanel.class);

    private ModToolsPanel modToolsPanel;

    private final Map<HiscoreSkill, JButton> skillButtons = new HashMap<>();
    private final RateLimitedHttpCache rateLimitedHttpCache = new RateLimitedHttpCache(20, 5);

    JLabel intro_top_text = new JLabel("", SwingConstants.CENTER);
    JLabel intro_bottom_text = new JLabel("", SwingConstants.CENTER);
    JPanel panelMainContent = new JPanel();

    String playerRank;

    ArrayList<OneShotPlugin.OneShotMember> allMembersRanksInfo;
    Map<String, ImageIcon> allMembersIcons;
    Map<String, String> allMembersDisplayNames;

    private boolean isInInfoPanel = false;
    private boolean isModerator = false;

    private Font titleFont;

    ClientThread clientThread;
    Client client;

    @Inject
    private SpriteManager spriteManager;
    private String playerName;

    public void init(Client client, ClientThread clientThread, ModToolsPanel modToolsPanel)
    {
        this.clientThread = clientThread;
        this.client = client;
        this.modToolsPanel = modToolsPanel;
        loadFonts();
        buildIntroPanel();
    }

    public void deinit()
    {
        isInInfoPanel = false;
    }

    private void update()
    {
        revalidate();
        repaint();
    }

    public void refresh(boolean isModerator, String playerName, String clanRankName, ImageIcon iconRank,
                        ArrayList<OneShotPlugin.OneShotMember> allMembersRanksInfo, Map<String, ImageIcon> members,
                        Map<String, String> allMembersDisplayNames) throws IOException, InterruptedException {
        this.allMembersRanksInfo = allMembersRanksInfo;
        this.allMembersIcons = members;
        this.allMembersDisplayNames = allMembersDisplayNames;
        this.playerRank = clanRankName;
        if (isModerator != this.isModerator)
        {
            buildMainPanel(isModerator, playerName, clanRankName, iconRank,
                allMembersRanksInfo, members,
                allMembersDisplayNames);
        }
        if (isInInfoPanel) buildRolesPanel();
        update();
    }

    private void loadFonts()
    {
        try (InputStream in = FontManager.class.getResourceAsStream("runescape.ttf"))
        {
            Font baseFont = Font.createFont(0, in).deriveFont(0, 16.0F);
            titleFont = StyleContext.getDefaultStyleContext().getFont(baseFont.getName(), 0, 64);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to load runescape.ttf", e);
        }
    }

    private JPanel createTitlePanel(String text)
    {
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        container.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1),
                BorderFactory.createEmptyBorder(4, 6, 4, 6) // Padding sized to text height
        ));
        JLabel titleText = new JLabel(text, SwingConstants.CENTER);
        container.add(titleText);
        return container;
    }

    public void buildIntroPanel()
    {
        removeAll();
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // Main title
        JLabel title = new JLabel("One Shot", SwingConstants.CENTER);
        title.setFont(titleFont);

        // Icon
        JLabel iconLabel = new JLabel(Icons.RED_HELM);

        // Default intro text
        changeIntroText1("Welcome to One Shot Plugin");
        changeIntroText2("Please enter the clan chat to continue");

        // Footer label
        JLabel hardcoreInfo = new JLabel("Hardcore Ironman exclusive", SwingConstants.CENTER);

        add(title);
        add(hardcoreInfo);
        add(Box.createVerticalStrut(10));
        add(iconLabel);
        add(Box.createVerticalStrut(15));
        add(intro_top_text);
        add(intro_bottom_text);

        revalidate();
        repaint();
    }

    public void changeIntroText1(String text)
    {
        intro_top_text.setText(text);
    }

    public void changeIntroText2(String text)
    {
        intro_bottom_text.setText(text);
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

        // button panel
        int nIcons = isModerator ? Constants.BUTTON_NUMBER : Constants.BUTTON_NUMBER - 1;
        this.isModerator = isModerator;

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1, nIcons, 5, 5));

        ImageIcon iconInfo = Icons.INFO;
        ImageIcon iconRanks = Icons.RANKING;
        ImageIcon iconDiscord = Icons.DISCORD;
        ImageIcon iconScout = Icons.MODTOOLS;

        JButton infoButton = buildButton(iconInfo, () -> {
            try {
                buildRolesPanel();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, Constants.TIP_ROLES);
        JButton ranksButtons = buildButton(iconRanks, this::buildLeaderboardsPanel, Constants.TIP_LEADERBOARDS);
        JButton discordButton = buildButton(iconDiscord, this::buildDiscordPanel, Constants.TIP_DISCORD);
        JButton scoutButton = buildButton(iconScout, this::buildModToolsPanel, Constants.TIP_MODTOOLS);


        ImageIcon icon = Icons.RED_HELM_SMALLER;
        JLabel image = new JLabel(icon);

        Font fontTitle;

        try (
                InputStream inRunescape = FontManager.class.getResourceAsStream("runescape.ttf");
        ) {
            Font font = Font.createFont(0, inRunescape).deriveFont(0, 16.0F);
            fontTitle = StyleContext.getDefaultStyleContext().getFont(font.getName(), 0, 32);
        } catch (FontFormatException ex) {
            throw new RuntimeException("Font loaded, but format incorrect.", ex);
        } catch (IOException ex) {
            throw new RuntimeException("Font file not found.", ex);
        }

        JLabel title = new JLabel("One Shot", SwingConstants.CENTER);
        title.setFont(fontTitle);

        add(image);
        add(Box.createGlue());
        add(title);

        buttonPanel.add(infoButton);
        buttonPanel.add(ranksButtons);
        buttonPanel.add(discordButton);
        if (isModerator)
        {
            buttonPanel.add(scoutButton);
        }
        add(buttonPanel);

        // Main Panel
        panelMainContent.removeAll();
        add(panelMainContent);


        update();
    }

    private void buildRolesPanel() throws IOException, InterruptedException {
        isInInfoPanel = true;

        panelMainContent.removeAll();
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        JPanel titlePanel = createTitlePanel("Clan Roles");
        titlePanel.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 20, 24));
        container.add(titlePanel);
        JPanel allMembersRanks = buildAllMembersRanksTotal();
        container.add(allMembersRanks);
        panelMainContent.add(container);
        update();
    }

    private JPanel buildAllMembersRanksTotal() {
        JPanel wrapper = new JPanel(new BorderLayout());
//        wrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
//        wrapper.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6)); // padding from edges

        JPanel container = new JPanel();
        container.setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));
        container.setBackground(ColorScheme.DARKER_GRAY_COLOR);

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

        wrapper.add(container, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildTopChartsPanel() throws IOException, InterruptedException {
        JPanel container = new JPanel();
        container.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        container.setBackground(ColorScheme.DARK_GRAY_COLOR);
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));

        JPanel title = createTitlePanel("Top players Skills and KCs");

        String response = rateLimitedHttpCache.fetch(Constants.URI_WOM_LEADERS);

        JsonParser jsonParser = new JsonParser();
        JsonElement jsonElement = jsonParser.parse(response);

        JsonElement metricLeaders = jsonElement.getAsJsonObject().get(Constants.URI_WOM_LEADERS_OBJECT);

        container.add(title);

        // Panel that holds skill icons
        JPanel statsPanel = new JPanel();
        statsPanel.setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));
        statsPanel.setLayout(new GridLayout(8, 3));
        statsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        // For each skill on the ingame skill panel, create a Label and add it to the UI
        for (HiscoreSkill skill : SKILLS)
        {
            JPanel panel = makeHiscorePanel(skill);
            statsPanel.add(panel);
        }

        container.add(statsPanel);

        JPanel totalPanel = new JPanel();
        totalPanel.setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));
        totalPanel.setLayout(new GridLayout(1, 1));
        totalPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        totalPanel.add(makeHiscorePanel(OVERALL));

        container.add(totalPanel);

        JPanel minigamePanel = new JPanel();
        minigamePanel.setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));
        minigamePanel.setLayout(new GridLayout(1, 2));
        minigamePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        for (HiscoreSkill skill : ACTIVITIES)
        {
            JPanel panel = makeHiscorePanel(skill);
            minigamePanel.add(panel);
        }

        container.add(minigamePanel);

        JPanel bossPanel = new JPanel();
        bossPanel.setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));
        bossPanel.setLayout(new GridLayout(0, 3));
        bossPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        // For each boss on the hi-scores, create a Label and add it to the UI
        for (HiscoreSkill skill : BOSSES)
        {
            JPanel panel = makeHiscorePanel(skill);
            bossPanel.add(panel);
        }

        container.add(bossPanel);

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
        button.setPreferredSize(new Dimension(60,25));


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


        // Add skill header
        addHeader(container, c, skill);

        JComponent table = buildPlayerTable(
                arr, skill, skillName,
                playerIndex, playerLimit, highlightPosition);

        container.add(table, c);
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
//        log.debug(String.valueOf(playerPosition));

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


    private void buildDiscordPanel() {
        LinkBrowser.browse(Constants.LINK_DISCORD);
//        clientThread.invokeLater(() ->
//        {
//            client.addChatMessage(
//                    ChatMessageType.GAMEMESSAGE,
//                    "",
//                    "You've completed enough Combat Achievement tasks to unlock Easy Tier rewards! You can now claim your rewards from Ghommal.",
//                    null
//            );
//        });
    }

    private void buildLeaderboardsPanel()
    {
        isInInfoPanel = false;
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

    private void buildModToolsPanel() {
        panelMainContent.removeAll();
        isInInfoPanel = false;
        panelMainContent.add(modToolsPanel);

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
        container.add(buildSkillHeader(skill), c);
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

    private JComponent buildPlayerTable(
            JsonArray arr,
            HiscoreSkill skill,
            String skillName,
            int startIndex,
            int count,
            int highlightIndex)
    {
        boolean isSkill = SKILLS.contains(skill) || skillName.equals("overall");
        boolean isBoss = BOSSES.contains(skill);

        List<PlayerRow> rows = new ArrayList<>();

        for (int i = startIndex; i < startIndex + count && i < arr.size(); i++)
        {
            JsonObject entry = arr.get(i).getAsJsonObject();
            JsonObject pdata = entry.getAsJsonObject("player");
            JsonObject ddata = entry.getAsJsonObject("data");

            String lower = pdata.get("username").getAsString().replace("\u00A0", " ");
            String display = allMembersDisplayNames.get(lower);
            ImageIcon icon = allMembersIcons.get(lower);

            int level = isSkill ? ddata.get("level").getAsInt()
                    : isBoss ? ddata.get("kills").getAsInt()
                    : ddata.get("score").getAsInt();

            long xp = isSkill ? ddata.get("experience").getAsLong() : 0;

            rows.add(new PlayerRow(
                    i + 1,
                    display,
                    icon,
                    level,
                    isSkill ? formatNumber(xp) : "",
                    i == highlightIndex
            ));
        }

        PlayerTableModel model = new PlayerTableModel(rows, isSkill, isBoss);
        JTable table = new JTable(model)
        {
            @Override
            public Component prepareRenderer(
                    javax.swing.table.TableCellRenderer r, int row, int col)
            {
                Component c = super.prepareRenderer(r, row, col);

                if (!isRowSelected(row))
                {
                    c.setBackground(
                            row % 2 == 0
                                    ? new Color(26, 26, 26)
                                    : new Color(32, 32, 32)
                    );
                }

                return c;
            }
        };

        styleTable(table, isSkill);

        // Fix phantom extra column
        while (table.getColumnModel().getColumnCount() > model.getColumnCount())
        {
            table.getColumnModel().removeColumn(
                    table.getColumnModel().getColumn(
                            table.getColumnModel().getColumnCount() - 1
                    )
            );
        }

        // Optional: stop JTable from resizing last column to fill viewport
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(null); // optional: cleaner look
        return scrollPane;
    }


    private static class PlayerRow {
        final int rank;
        final String name;
        final ImageIcon icon;
        final int level;
        final String exp;
        final boolean highlight;

        PlayerRow(int rank, String name, ImageIcon icon,
                  int level, String exp, boolean highlight)
        {
            this.rank = rank;
            this.name = name;
            this.icon = icon;
            this.level = level;
            this.exp = exp;
            this.highlight = highlight;
        }
    }

    private JPanel buildSkillHeader(HiscoreSkill skill) {
        JPanel header = new JPanel() {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, super.getPreferredSize().height);
            }
        };
        header.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 5));
        header.setBackground(ColorScheme.DARK_GRAY_COLOR);

        // Skill icon
        JLabel iconLabel = new JLabel();
        spriteManager.getSpriteAsync(
                skill == null ? SpriteID.SideIcons.COMBAT : skill.getSpriteId(),
                0,
                sprite -> SwingUtilities.invokeLater(() -> {
                    BufferedImage scaled = ImageUtil.resizeImage(ImageUtil.resizeCanvas(sprite, 25, 25), 30, 30);
                    iconLabel.setIcon(new ImageIcon(scaled));
                })
        );

        // Skill name (with shrinking only if needed)
        final float originalFontSize = 18f;
        JLabel nameLabel = new JLabel(skill.getName()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                Font font = getFont().deriveFont(originalFontSize);
                FontMetrics fm = g2d.getFontMetrics(font);

                int availableWidth = 180; // small padding
                String text = getText();

                // Only shrink if text width exceeds available width
                if (fm.stringWidth(text) > availableWidth) {
                    while (fm.stringWidth(text) > availableWidth && font.getSize() > 8) {
                        font = font.deriveFont((float) (font.getSize() - 1));
                        fm = g2d.getFontMetrics(font);
                    }
                }

                setFont(font);
                super.paintComponent(g);
            }
        };

        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(FontManager.getRunescapeBoldFont().deriveFont(originalFontSize));

        header.add(iconLabel);
        header.add(nameLabel);

        return header;
    }



    private static class PlayerTableModel extends javax.swing.table.AbstractTableModel {

        private final List<PlayerRow> rows;
        private final String[] cols;

        PlayerTableModel(List<PlayerRow> rows, boolean skill, boolean boss)
        {
            this.rows = rows;
            this.cols = skill
                    ? new String[]{"#", "Player", "Level", "Exp"}
                    : boss ? new String[]{"#", "Player", "Kills"}
                    : new String[]{"#", "Player", "Total"};
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }

        @Override
        public Object getValueAt(int r, int c)
        {
            PlayerRow row = rows.get(r);

            if (cols.length == 4) { // Skill
                switch(c) {
                    case 0: return row.rank;
                    case 1: return row;
                    case 2: return row.level;
                    case 3: return row.exp;
                }
            } else { // Boss / Activity
                switch(c) {
                    case 0: return row.rank;
                    case 1: return row;
                    case 2: return row.level; // kills or total
                }
            }

            return null;
        }


        @Override
        public Class<?> getColumnClass(int col)
        {
            return col == 0 || col == 2 ? Integer.class : Object.class;
        }
    }

    private void styleTable(JTable table, boolean isSkill) {
        table.setRowHeight(22);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setBackground(ColorScheme.DARK_GRAY_COLOR);
        table.setForeground(Color.WHITE);
        table.setFont(FontManager.getRunescapeSmallFont());

        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setResizingAllowed(false);

        // Column widths
        if (isSkill){
            table.getColumnModel().getColumn(0).setMaxWidth(35);  // #
            table.getColumnModel().getColumn(1).setMaxWidth(125); // name + icon
            table.getColumnModel().getColumn(2).setMaxWidth(40);  // level/total
            table.getColumnModel().getColumn(3).setMaxWidth(50); // only for skills
        }
        else {
            table.getColumnModel().getColumn(0).setMaxWidth(55);  // #
            table.getColumnModel().getColumn(1).setMaxWidth(130); // name + icon
            table.getColumnModel().getColumn(2).setMaxWidth(60);  // kills/score
        }


        table.getColumnModel().getColumn(1).setCellRenderer(new PlayerRenderer());

        // Numeric columns
        for (int col = 0; col < table.getColumnCount(); col++) {
            if (col != 1) {
                table.getColumnModel().getColumn(col).setCellRenderer(new DefaultTableCellRenderer() {
                    @Override
                    public Component getTableCellRendererComponent(
                            JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                        PlayerTableModel model = (PlayerTableModel) table.getModel();
                        PlayerRow playerRow = model.rows.get(row);

                        if (playerRow.highlight) {
                            setForeground(Color.GREEN);
                            setBackground(new Color(0, 40, 0));
                        } else if (isSelected) {
                            setForeground(Color.WHITE);
                            setBackground(new Color(50, 50, 50));
                        } else {
                            setForeground(Color.WHITE);
                            setBackground(row % 2 == 0 ? new Color(26, 26, 26) : new Color(32, 32, 32));
                        }

                        setHorizontalAlignment(SwingConstants.CENTER);
                        return this;
                    }
                });
            }
        }

        // Adaptive width for the last column (Exp/Kills/Total)
        if (isSkill) {
            table.getColumnModel().getColumn(3).setMaxWidth(80);
            table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        } else {
            table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        }

        table.setPreferredScrollableViewportSize(
                new Dimension(panelMainContent.getWidth(), table.getRowHeight() * table.getRowCount())
        );
        table.setFillsViewportHeight(true);
    }



    private static class PlayerRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col)
        {
            super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, col);

            PlayerRow r = value instanceof PlayerRow ? (PlayerRow) value : null;

            if (r != null) {
                setText(" " + r.name);
                setIcon(r.icon);
                setHorizontalAlignment(LEFT);

                if (r.highlight) {
                    setForeground(Color.GREEN);
                    setBackground(new Color(0, 40, 0)); // optional darker green background
                } else if (isSelected) {
                    setForeground(Color.WHITE);
                    setBackground(new Color(50, 50, 50)); // selection color
                } else {
                    setForeground(Color.WHITE);
                    setBackground(row % 2 == 0 ? new Color(26, 26, 26) : new Color(32, 32, 32));
                }
            }

            return this;
        }
    }



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




}



