package com.oneshot;

import com.google.gson.*;

import com.oneshot.modules.ModToolsPanel;
import com.oneshot.utils.Constants;
import com.oneshot.utils.Icons;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.StyleContext;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;

import net.runelite.api.ChatMessageType;
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

import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class OneShotPanel extends PluginPanel
{
    private static final Logger log = LoggerFactory.getLogger(OneShotPanel.class);

    private ModToolsPanel modToolsPanel;
    private OneShotConfig config;

    private final Map<HiscoreSkill, JButton> skillButtons = new HashMap<>();
    private RateLimitedHttpCache rateLimitedHttpCache;

    JLabel intro_top_text = new JLabel("", SwingConstants.CENTER);
    JLabel intro_bottom_text = new JLabel("", SwingConstants.CENTER);
    JPanel panelMainContent = new JPanel();

    private JTable currentTable;
    private JScrollPane currentScroll;
    private PlayerTableModel currentModel;
    private JPanel skillViewRoot;

    private HiscoreSkill currentSkill;

    // --- Skill view search state ---
    private SuggestionTextField playerSearchField;
    private JLabel playerSearchCount;
    private final String[] playerSearchSuggestion = { null };

    private JsonArray currentSkillArr;
    private List<Integer> currentSkillFilteredIdx;
    private String currentSkillQuery = "";

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
    private OkHttpClient httpClient;

    @Inject
    private SpriteManager spriteManager;
    private String playerName;

    private final Map<String, JButton> tabButtons = new HashMap<>();
    private final Map<String, JPanel> tabUnderlines = new HashMap<>();
    private String activeTabKey = "info";
    private static final Color TAB_UNDERLINE = new Color(139, 0, 0); // Dark red
    private static final Color TAB_UNDERLINE_OFF = new Color(0, 0, 0, 0); // No color

    private static final Color ROW_A = new Color(26, 26, 26);
    private static final Color ROW_B = new Color(32, 32, 32);

    public void init(Client client, ClientThread clientThread, ModToolsPanel modToolsPanel, OneShotConfig config)
    {
        this.clientThread = clientThread;
        this.client = client;
        this.modToolsPanel = modToolsPanel;
        this.config = config;
        loadFonts();
        buildIntroPanel();
        rateLimitedHttpCache = new RateLimitedHttpCache(20, 5);
    }

    public void deinit()
    {
        isInInfoPanel = false;
        rateLimitedHttpCache.shutdown();
    }

    private void update()
    {
        revalidate();
        repaint();
    }

    public void refresh(boolean isModerator, String playerName, String clanRankName,
                        ArrayList<OneShotPlugin.OneShotMember> allMembersRanksInfo, Map<String, ImageIcon> members,
                        Map<String, String> allMembersDisplayNames) throws IOException, InterruptedException {
        this.allMembersRanksInfo = allMembersRanksInfo;
        this.allMembersIcons = members;
        this.allMembersDisplayNames = allMembersDisplayNames;
        this.playerRank = clanRankName;
        if (isModerator != this.isModerator)
        {
            buildMainPanel(isModerator, playerName, clanRankName,
                allMembersRanksInfo, members,
                allMembersDisplayNames);
        }
        if (isInInfoPanel) buildInfoPanel();
        update();
    }

    private void loadFonts()
    {
        try (InputStream in = FontManager.class.getResourceAsStream("runescape.ttf"))
        {
            Font baseFont = Font.createFont(0, in).deriveFont(Font.PLAIN, 16.0F);
            titleFont = StyleContext.getDefaultStyleContext().getFont(baseFont.getName(), 0, 64);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to load runescape.ttf", e);
        }
    }

    private JPanel createWorldPanel()
    {
        JPanel container = new JPanel();
        container.setLayout(new BorderLayout());
        container.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        container.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));

        JLabel worldLabel = new JLabel("507", Icons.WORLD, SwingConstants.CENTER);
        worldLabel.setHorizontalAlignment(SwingConstants.CENTER);
        worldLabel.setHorizontalTextPosition(SwingConstants.RIGHT);
        worldLabel.setForeground(client.getWorld() == 507 ? Color.GREEN : Color.RED);

        container.add(worldLabel, BorderLayout.CENTER);

        int h = 28;
        container.setPreferredSize(new Dimension(0, h));
        container.setMinimumSize(new Dimension(0, h));
        container.setMaximumSize(new Dimension(Integer.MAX_VALUE, h));

        return container;
    }

    private JPanel createTitlePanel(String text)
    {
        JPanel container = new JPanel(new BorderLayout());
        container.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        JLabel titleText = new JLabel(text, SwingConstants.CENTER);
        titleText.setFont(FontManager.getRunescapeBoldFont());
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


    public void buildMainPanel(boolean isModerator, String playerName, String clanRankName,
                               ArrayList<OneShotPlugin.OneShotMember> allMembersRanksInfo, Map<String, ImageIcon> members,
                               Map<String, String> allMembersDisplayNames) throws IOException, InterruptedException {

        this.allMembersRanksInfo = allMembersRanksInfo;
        this.allMembersIcons = members;
        this.allMembersDisplayNames = allMembersDisplayNames;
        this.playerName = playerName;
        this.playerRank = clanRankName;
        this.isModerator = isModerator;

        removeAll();

        Font fontTitle;

        try (
                InputStream inRunescape = FontManager.class.getResourceAsStream("runescape.ttf")
        ) {
            Font font = Font.createFont(0, inRunescape).deriveFont(Font.PLAIN, 16.0F);
            fontTitle = StyleContext.getDefaultStyleContext().getFont(font.getName(), 0, 32);
        } catch (FontFormatException ex) {
            throw new RuntimeException("Font loaded, but format incorrect.", ex);
        } catch (IOException ex) {
            throw new RuntimeException("Font file not found.", ex);
        }

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);

        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, header.getPreferredSize().height));

        JLabel iconLabel = new JLabel(Icons.RED_HELM_SMALLER);
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel("One Shot", SwingConstants.CENTER);
        titleLabel.setFont(fontTitle);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, titleLabel.getPreferredSize().height));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        header.add(iconLabel);
        header.add(Box.createVerticalStrut(4));
        header.add(titleLabel);

        add(header);

        // button panel
        JPanel tabsPanel = new JPanel();
        tabsPanel.setLayout(new BoxLayout(tabsPanel, BoxLayout.X_AXIS));
        tabsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        tabsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        tabButtons.clear();
        tabUnderlines.clear();

        tabsPanel.add(Box.createHorizontalGlue());

        tabsPanel.add(makeTab("info", "Info", () -> {
            try { buildInfoPanel(); } catch (Exception ex) { throw new RuntimeException(ex); }
        }));
        tabsPanel.add(Box.createHorizontalGlue());

        tabsPanel.add(makeTab("ranks", "Ranks", this::buildLeaderboardsPanel));
        tabsPanel.add(Box.createHorizontalGlue());

        tabsPanel.add(makeTab("discord", "Discord", this::buildDiscordPanel));

        if (isModerator)
        {
            tabsPanel.add(Box.createHorizontalGlue());
            tabsPanel.add(makeTab("dev", "Dev", this::buildModToolsPanel));
        }

        tabsPanel.add(Box.createHorizontalGlue());
        add(tabsPanel);

        // Main Panel
        panelMainContent.removeAll();
        panelMainContent.setLayout(new BorderLayout());
        add(panelMainContent);

        setActiveTab("info");

        SwingUtilities.invokeLater(() -> {
            buildInfoPanel();
            panelMainContent.revalidate();
            panelMainContent.repaint();
        });

        update();
    }

    private JComponent makeTab(String key, String text, Runnable onClick)
    {
        JButton button = buildTab(text, () -> {
            onClick.run();
            setActiveTab(key);
        });

        // Make the button not grow / not add padding changes
        button.setMargin(new Insets(0, 0, 0, 0));

        button.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                if (!key.equals(activeTabKey)) button.setForeground(Color.WHITE);
            }
            @Override public void mouseExited(MouseEvent e) {
                button.setForeground(key.equals(activeTabKey) ? Color.WHITE : ColorScheme.TEXT_COLOR.darker());
            }
        });

        JPanel underline = new JPanel();
        underline.setOpaque(true);
        underline.setPreferredSize(new Dimension(button.getPreferredSize().width + 15, 2));
        underline.setMinimumSize(new Dimension(1, 2));
        underline.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
        underline.setBackground(TAB_UNDERLINE_OFF);

        JPanel tab = new JPanel();
        tab.setLayout(new BoxLayout(tab, BoxLayout.Y_AXIS));
        tab.setOpaque(false);

        // keep width stable: use the button’s preferred size
        Dimension d = button.getPreferredSize();
        tab.setPreferredSize(new Dimension(d.width + 15, d.height + 4));
        tab.setMinimumSize(tab.getPreferredSize());
        tab.setMaximumSize(tab.getPreferredSize());

        // Center the button inside the tab container
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        underline.setAlignmentX(Component.CENTER_ALIGNMENT);

        tab.add(button);
        tab.add(Box.createVerticalStrut(2));
        tab.add(underline);

        tabButtons.put(key, button);
        tabUnderlines.put(key, underline);

        return tab;
    }

    private static JButton buildTab(String text, Runnable callback)
    {
        JButton button = new JButton(text);

        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setRolloverEnabled(false);

        button.setBackground(ColorScheme.DARK_GRAY_COLOR);
        button.setForeground(ColorScheme.TEXT_COLOR);



        button.addMouseListener(new MouseAdapter() {
            @Override public void mouseReleased(MouseEvent e) {
                callback.run();
            }
        });

        return button;
    }

    private void setActiveTab(String key)
    {
        activeTabKey = key;

        for (Map.Entry<String, JPanel> e : tabUnderlines.entrySet())
        {
            boolean active = e.getKey().equals(key);
            e.getValue().setBackground(active ? TAB_UNDERLINE : TAB_UNDERLINE_OFF);
        }

        for (Map.Entry<String, JButton> e : tabButtons.entrySet())
        {
            boolean active = e.getKey().equals(key);
            e.getValue().setForeground(active ? Color.WHITE : ColorScheme.TEXT_COLOR.darker());
        }

        revalidate();
        repaint();
    }

    private void buildInfoPanel() {
        isInInfoPanel = true;

        panelMainContent.removeAll();
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.add(createTitlePanel("Clan World"));
        container.add(createWorldPanel());
        container.add(Box.createVerticalStrut(4));
        container.add(createTitlePanel("Roles"));
        JPanel allMembersRanks = buildAllMembersRanksTotal();
        container.setOpaque(false);
        container.setAlignmentX(Component.CENTER_ALIGNMENT);
        allMembersRanks.setAlignmentX(Component.CENTER_ALIGNMENT);
        container.add(allMembersRanks);
        panelMainContent.add(container, BorderLayout.CENTER);
        panelMainContent.revalidate();
        panelMainContent.repaint();
        update();
    }


    private JPanel buildAllMembersRanksTotal()
    {
        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));

        // ---- Build table model data ----

        DefaultTableModel model = new DefaultTableModel(new String[] { "", "Rank", "Online", "Total" }, 0)
        {
            @Override public boolean isCellEditable(int r, int c) { return false; }

            @Override public Class<?> getColumnClass(int c)
            {
                switch (c)
                {
                    case 0: return Icon.class;
                    case 2:
                    case 3:
                        return Integer.class;
                    default: return String.class;
                }
            }
        };

        for (OneShotPlugin.OneShotMember m : allMembersRanksInfo)
        {
            if (m == null) continue;

            String name = m.getName();
            if (name == null || name.trim().isEmpty()) continue; // <-- prevents blank trailing row

            model.addRow(new Object[] { m.getIcon(), name, m.getOnline(), m.getTotal() });
        }

        JTable table = new JTable(model);
        applyBaseTableStyle(table);

        // Column sizing
        table.getColumnModel().getColumn(2).setMaxWidth(60);  // online
        table.getColumnModel().getColumn(3).setMaxWidth(60);  // total

        // --- Icon column renderer ---
        table.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer()
        {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int col)
            {
                super.getTableCellRendererComponent(t, "", false, false, row, col);

                setHorizontalAlignment(SwingConstants.CENTER);
                setIcon(value instanceof Icon ? (Icon) value : null);

                setBackground(row % 2 == 0 ? ROW_B : ROW_A);
                setForeground(ColorScheme.TEXT_COLOR);

                String rankName = String.valueOf(t.getModel().getValueAt(row, 1));
                if (Objects.equals(playerRank, rankName))
                    setForeground(Color.GREEN);

                return this;
            }
        });

        // --- Rank column renderer (text) ---
        table.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer()
        {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int col)
            {
                super.getTableCellRendererComponent(t, value, false, false, row, col);

                setHorizontalAlignment(SwingConstants.LEFT);
                setBackground(row % 2 == 0 ? ROW_B : ROW_A);
                setForeground(ColorScheme.TEXT_COLOR);

                String rankName = String.valueOf(t.getModel().getValueAt(row, 1));
                if (Objects.equals(playerRank, rankName))
                    setForeground(Color.GREEN);

                return this;
            }
        });

        // --- Number columns renderer (center) ---
        DefaultTableCellRenderer number = new DefaultTableCellRenderer()
        {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int col)
            {
                super.getTableCellRendererComponent(t, value, false, false, row, col);

                setHorizontalAlignment(SwingConstants.CENTER);
                setBackground(row % 2 == 0 ? ROW_B : ROW_A);
                setForeground(ColorScheme.TEXT_COLOR);

                String rankName = String.valueOf(t.getModel().getValueAt(row, 1));
                if (Objects.equals(playerRank, rankName))
                    setForeground(Color.GREEN);

                return this;
            }
        };

        table.getColumnModel().getColumn(2).setCellRenderer(number);
        table.getColumnModel().getColumn(3).setCellRenderer(number);

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setResizingAllowed(false);
        header.setBorder(BorderFactory.createEmptyBorder());

        header.setDefaultRenderer(new TableCellRenderer()
        {
            private final DefaultTableCellRenderer r = new DefaultTableCellRenderer();

            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int col)
            {
                r.setOpaque(true);
                r.setBackground(ROW_A);
                r.setForeground(Color.WHITE);
                r.setFont(FontManager.getRunescapeSmallFont());
                r.setBorder(BorderFactory.createEmptyBorder());

                r.setText(value == null ? "" : value.toString());

                // Rank header left aligned, others centered
                if (col == 1) {
                    r.setHorizontalAlignment(SwingConstants.LEFT);
                    r.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 0)); // just padding
                } else {
                    r.setHorizontalAlignment(SwingConstants.CENTER);
                    r.setBorder(BorderFactory.createEmptyBorder());
                }

                return r;
            }
        });

        table.getColumnModel().getColumn(0).setPreferredWidth(32);
        table.getColumnModel().getColumn(0).setMaxWidth(32);

        table.getColumnModel().getColumn(2).setPreferredWidth(50);
        table.getColumnModel().getColumn(2).setMaxWidth(50);

        table.getColumnModel().getColumn(3).setPreferredWidth(50);
        table.getColumnModel().getColumn(3).setMaxWidth(50);

        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        JScrollPane scroll = new JScrollPane(table);
        // ---- Clamp scroll height to exactly header + rows (removes bottom gap) ----
        int rowsH = table.getRowHeight() * table.getRowCount();
        int headerH = table.getTableHeader().getPreferredSize().height;
        int totalH = rowsH + headerH;

        Dimension fixed = new Dimension(Short.MAX_VALUE, totalH);
        scroll.setPreferredSize(new Dimension(0, totalH));
        scroll.setMaximumSize(fixed);
        scroll.setMinimumSize(new Dimension(0, totalH));

        // No need for a vertical scrollbar if we're exact-height
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setViewportBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setBackground(ColorScheme.DARKER_GRAY_COLOR);

        JViewport vp = scroll.getViewport();
        vp.setOpaque(false);
        vp.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        // Optional: keep the horizontal scrollbar away
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        wrapper.add(scroll);
        wrapper.add(Box.createVerticalStrut(2));

        JLabel discordPlug = new JLabel("/rank in discord #bot-commands", SwingConstants.CENTER);
        discordPlug.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        discordPlug.setAlignmentX(Component.CENTER_ALIGNMENT);
        wrapper.add(discordPlug);

        return wrapper;
    }

    private void applyBaseTableStyle(JTable table)
    {
        table.setRowHeight(22);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFocusable(false);
        table.setRowSelectionAllowed(false);
        table.setColumnSelectionAllowed(false);
        table.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        table.setForeground(ColorScheme.TEXT_COLOR);
        table.setFont(FontManager.getRunescapeSmallFont());
    }

    private JPanel buildTopChartsSkeleton()
    {
        final int GAP = 4;

        JPanel container = new JPanel();
        container.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));

        skillButtons.clear();

        // Skills: 3 columns
        container.add(createTitlePanel("Skills"));
        container.add(buildButtonGrid(SKILLS, GAP));
        container.add(Box.createVerticalStrut(GAP));

        // Overall: 1 column
        container.add(buildButtonGrid(Collections.singletonList(OVERALL), GAP));
        container.add(Box.createVerticalStrut(GAP));

        // Activities: 2 columns
        container.add(createTitlePanel("Activities"));
        container.add(buildButtonGrid(ACTIVITIES, GAP));
        container.add(Box.createVerticalStrut(GAP));

        // Bosses: 3 columns
        container.add(createTitlePanel("Bosses"));
        container.add(buildButtonGrid(BOSSES, GAP));

        return container;
    }

    private void fetchAndPopulateTopChartsAsync()
    {
        SwingWorker<JsonElement, Void> worker = new SwingWorker<>()
        {
            @Override
            protected JsonElement doInBackground() throws Exception
            {
                String response = rateLimitedHttpCache.fetch(Constants.URI_WOM_LEADERS);
                if (response == null) return null;

                JsonParser jsonParser = new JsonParser();
                JsonElement jsonElement = jsonParser.parse(response);
                return jsonElement.getAsJsonObject().get(Constants.URI_WOM_LEADERS_OBJECT);
            }

            @Override
            protected void done()
            {
                try
                {
                    JsonElement metricLeaders = get();
                    if (metricLeaders == null) return;

                    populateMetricLeadersAsync(metricLeaders);
                }
                catch (Exception e)
                {
                    log.error(e.getMessage());
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
                    log.error(e.getMessage());
                }
            }
        };

        worker.execute();
    }

    private static @Nullable JsonObject getObj(JsonObject parent, String key)
    {
        if (parent == null) return null;
        JsonElement el = parent.get(key);
        return (el != null && el.isJsonObject()) ? el.getAsJsonObject() : null;
    }

    private static String keys(JsonObject o)
    {
        return o == null ? "null" : o.keySet().toString();
    }

    private ButtonUpdate computeButtonInfo(HiscoreSkill skill, JsonElement metricLeadersEl)
    {
        JsonObject metricLeaders = metricLeadersEl.getAsJsonObject();

        String name = normalizeSkillName(skill);
        String element;
        String id;

        if (SKILLS.contains(skill) || name.equals("overall")) {
            element = "skills";
            id = "experience";
        } else if (BOSSES.contains(skill)) {
            element = "bosses";
            id = "kills";
        } else if (ACTIVITIES.contains(skill)) {
            element = "activities";
            id = "score";
        } else {
            return new ButtonUpdate(Color.RED, "?", "Unknown");
        }

        JsonObject bucket = getObj(metricLeaders, element);
        if (bucket == null) {
            log.warn("metricLeaders missing bucket '{}'. topKeys={}", element, keys(metricLeaders));
            return new ButtonUpdate(Color.RED, "?", "Missing bucket");
        }

        JsonObject metric = getObj(bucket, name);
        if (metric == null) {
            log.warn("Missing metric '{}.{}'. availableKeys={}", element, name, keys(bucket));
            return new ButtonUpdate(Color.RED, "?",
                    "Missing metric");
        }

        JsonObject player = getObj(metric, "player");
        String displayName = player != null && player.has("displayName") ? player.get("displayName").getAsString() : "";

        if (element.equals("skills")) {
            Color col = displayName.equals(playerName) ? Color.GREEN : Color.WHITE;

            String text = name.equals("overall")
                    ? metric.get("level").getAsString()
                    : String.valueOf(Experience.getLevelForXp(metric.get(id).getAsInt()));

            return new ButtonUpdate(col, text, detailsHtml(skill, metric));
        }

        if (element.equals("bosses")) {
            double kills = metric.get(id).getAsDouble();
            Color col = kills == 0 ? Color.RED : (displayName.equals(playerName) ? Color.GREEN : Color.WHITE);
            return new ButtonUpdate(col, metric.get(id).getAsString(), detailsHtml(skill, metric));
        }

        // activities
        double score = metric.get(id).getAsDouble();
        Color col = score == 0 ? Color.RED : (displayName.equals(playerName) ? Color.GREEN : Color.WHITE);
        return new ButtonUpdate(col, metric.get(id).getAsString(), detailsHtml(skill, metric));
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
        button.setText(pad(skillType));
        button.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        button.setMargin(new Insets(0,0,0,0));
        button.setBorderPainted(false);
        button.setPreferredSize(new Dimension(0, 35));
        button.setMinimumSize(new Dimension(0, 35));


        BufferedImage clueImg = getClueScrollIcon(skill);
        if (clueImg != null) {
            final BufferedImage scaledSprite = ImageUtil.resizeImage(ImageUtil.resizeCanvas(clueImg, 25, 25), 20, 20);
            button.setIcon(new ImageIcon(scaledSprite));
        }
        else {
            spriteManager.getSpriteAsync(skill == null ? SpriteID.SideIcons.COMBAT : skill.getSpriteId(), 0, (sprite) ->
                    SwingUtilities.invokeLater(() ->
                    {
                        // Icons are all 25x25 or smaller, so they're fit into a 25x25 canvas to give them a consistent size for
                        // better alignment. Further, they are then scaled down to 20x20 to not be overly large in the panel.
                        final BufferedImage scaledSprite = ImageUtil.resizeImage(ImageUtil.resizeCanvas(sprite, 25, 25), 20, 20);
                        button.setIcon(new ImageIcon(scaledSprite));
                    }));
        }

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
                buildSkillPlayersAsync(skill);
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

        JPanel skillPanel = new JPanel(new BorderLayout());
        skillPanel.setOpaque(false);
        skillPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        skillPanel.add(button, BorderLayout.CENTER);

        skillButtons.put(skill, button);

        return skillPanel;
    }

    private JPanel buildButtonGrid(List<HiscoreSkill> items, int gap)
    {
        int count = items.size();
        int cols = Math.max(1, Math.min(3, count));

        JPanel grid = new JPanel(new GridLayout(0, cols, gap, gap));
        grid.setOpaque(false);

        for (HiscoreSkill s : items)
        {
            grid.add(makeHiscorePanel(s));
        }
        return grid;
    }

    private void showLoadingIfFirstOpenForSkill()
    {
        boolean firstOpenForThisSkill = (skillViewRoot == null);

        if (!firstOpenForThisSkill)
        {
            return;
        }

        panelMainContent.removeAll();
        panelMainContent.setLayout(new BorderLayout());

        JPanel loading = new JPanel(new BorderLayout());
        loading.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JLabel loadingText = new JLabel("Loading...", SwingConstants.CENTER);
        loading.add(loadingText, BorderLayout.CENTER);

        panelMainContent.add(loading, BorderLayout.CENTER);
        panelMainContent.revalidate();
        panelMainContent.repaint();
    }


    private void buildSkillPlayersAsync(HiscoreSkill skill) {

        final HiscoreSkill requestedSkill = skill;

        SwingUtilities.invokeLater(this::showLoadingIfFirstOpenForSkill);

        SwingWorker<JsonArray, Void> worker = new SwingWorker<>() {

            @Override
            protected JsonArray doInBackground() throws Exception {
                String skillName = normalizeSkillName(requestedSkill);
                return fetchSkillData(skillName);   // <-- NO UI freeze now
            }

            @Override
            protected void done() {
                try {
                    JsonArray arr = get();
                    // now build UI on EDT
                    buildSkillPlayersUI(requestedSkill, arr);

                } catch (Exception e) {
                    log.error(e.getMessage());
                    buildSkillPlayersUI(requestedSkill, null);
                }
            }
        };

        worker.execute();
    }

    private void showSkillMessage(String line1, String line2, Color c1, Color c2)
    {
        panelMainContent.removeAll();
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(goBackButton());

        JLabel a = new JLabel(line1);
        a.setForeground(c1);
        p.add(a);

        if (line2 != null)
        {
            JLabel b = new JLabel(line2);
            b.setForeground(c2);
            p.add(b);
        }

        panelMainContent.add(p);
        update();
    }

    private void buildSkillPlayersUI(HiscoreSkill skill, JsonArray arr) {

        if (arr == null) {
            showSkillMessage("Hey wow, too fast!", "Please slow down", Color.RED, Color.RED);
            return;
        }

        if (arr.size() == 0) {
            showSkillMessage("Seems no one is on the Hiscores", null, Color.RED, Color.RED);
            return;
        }

        // Cache full dataset for the skill search bar
        currentSkillArr = arr;

        // ---- reset search/filter when opening a (new) skill/boss ----
        currentSkillQuery = "";
        currentSkillFilteredIdx = null;
        playerSearchSuggestion[0] = null;

        if (playerSearchField != null)
        {
            playerSearchField.setText("");
            playerSearchField.setSuggestion(null);
        }

        // Apply current query (if any) and render from filtered set
        rebuildSkillFilterAndShow(skill);
    }


    private void ensureSkillViewBuilt(HiscoreSkill skill, boolean isSkill, boolean isBoss) {
        if (skillViewRoot != null && currentSkill == skill) {
            // if user navigated away via tabs, the view is no longer in panelMainContent
            if (skillViewRoot.getParent() == null || skillViewRoot.getParent() != panelMainContent)
            {
                panelMainContent.removeAll();
                panelMainContent.setLayout(new BorderLayout());
                panelMainContent.add(skillViewRoot, BorderLayout.CENTER);
                panelMainContent.revalidate();
                panelMainContent.repaint();
            }
            return;
        }

        currentSkill = skill;

        // Root container
        skillViewRoot = new JPanel(new BorderLayout());
        skillViewRoot.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        /* ---------------- Header ---------------- */

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(ColorScheme.DARK_GRAY_COLOR);

        /* --- Top row: Go Back (left aligned) --- */
        JPanel backRow = new JPanel(new BorderLayout());
        backRow.setBackground(ColorScheme.DARK_GRAY_COLOR);
        backRow.add(goBackButton(), BorderLayout.WEST);

        /* --- Bottom row: Skill header (centered) --- */
        JPanel skillHeaderRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        skillHeaderRow.setBackground(ColorScheme.DARK_GRAY_COLOR);
        skillHeaderRow.add(buildSkillHeader(skill));

        header.add(backRow);
        header.add(skillHeaderRow);

        // --- Player search row (ghost suggestion + TAB commit) ---
        JPanel searchRow = new JPanel();
        searchRow.setLayout(new BoxLayout(searchRow, BoxLayout.Y_AXIS));
        searchRow.setBackground(ColorScheme.DARK_GRAY_COLOR);

        playerSearchField = new SuggestionTextField();
        playerSearchField.setFont(FontManager.getRunescapeSmallFont());
        playerSearchField.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        playerSearchField.setForeground(ColorScheme.TEXT_COLOR);
        playerSearchField.setCaretColor(Color.WHITE);
        playerSearchField.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        playerSearchField.setToolTipText("Search players (TAB accepts suggestion)");
        playerSearchField.setFocusTraversalKeysEnabled(false);

        int initialCount = 0;

        // If we already have data, show total (or filtered total if filter is already built)
        if (currentSkillFilteredIdx != null)
        {
            initialCount = currentSkillFilteredIdx.size();
        }
        else if (currentSkillArr != null)
        {
            // If there's a query already, count matching rows; otherwise just total size
            String q = currentSkillQuery == null ? "" : currentSkillQuery.trim().toLowerCase();
            if (q.isEmpty())
            {
                initialCount = currentSkillArr.size();
            }
            else
            {
                int c = 0;
                for (int i = 0; i < currentSkillArr.size(); i++)
                {
                    JsonObject entry = currentSkillArr.get(i).getAsJsonObject();
                    String display = getDisplayNameFromEntry(entry);
                    if (display != null && display.toLowerCase().contains(q)) c++;
                }
                initialCount = c;
            }
        }

        playerSearchCount = new JLabel(initialCount + " results");
        playerSearchCount.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        playerSearchCount.setFont(FontManager.getRunescapeSmallFont());
        playerSearchCount.setBorder(BorderFactory.createEmptyBorder(2, 2, 0, 0));

        searchRow.add(playerSearchField);
        searchRow.add(playerSearchCount);

        header.add(searchRow);

        // TAB commits suggestion
        playerSearchField.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("TAB"), "acceptPlayerSuggestion");
        playerSearchField.getActionMap().put("acceptPlayerSuggestion", new AbstractAction()
        {
            @Override public void actionPerformed(ActionEvent e)
            {
                String sugg = playerSearchSuggestion[0];
                if (sugg == null) return;

                playerSearchField.setText(sugg);
                playerSearchField.setCaretPosition(sugg.length());
                playerSearchField.setSuggestion(null);
            }
        });

        // On typing: filter + compute ghost suggestion
        Runnable applySearch = () -> {
            String q = playerSearchField.getText();
            String qLower = q == null ? "" : q.trim().toLowerCase();

            currentSkillQuery = qLower;

            // ghost suggestion (prefix only)
            String sugg = findFirstPrefixPlayerSuggestion(qLower);
            if (sugg != null && sugg.equalsIgnoreCase(q.trim())) sugg = null;

            playerSearchSuggestion[0] = sugg;
            playerSearchField.setSuggestion(sugg);

            // filter results immediately based on typed text
            rebuildSkillFilterAndShow(skill);
        };

        playerSearchField.getDocument().addDocumentListener(new DocumentListener()
        {
            private void update() { SwingUtilities.invokeLater(applySearch); }
            @Override public void insertUpdate(DocumentEvent e) { update(); }
            @Override public void removeUpdate(DocumentEvent e) { update(); }
            @Override public void changedUpdate(DocumentEvent e) { update(); }
        });

        skillViewRoot.add(header, BorderLayout.NORTH);

        /* ---------------- Table ---------------- */

        currentModel = new PlayerTableModel(new ArrayList<>(), isSkill, isBoss);

        currentTable = new JTable(currentModel)
        {
            @Override
            public Component prepareRenderer(TableCellRenderer r, int row, int col)
            {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row))
                {
                    c.setBackground(row % 2 == 0 ? ROW_B : ROW_A);
                }
                return c;
            }
        };

        styleTable(currentTable, isSkill);

        currentScroll = new JScrollPane(currentTable);
        currentScroll.setBorder(BorderFactory.createEmptyBorder());
        currentScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        currentScroll.getViewport().setBackground(ColorScheme.DARKER_GRAY_COLOR);

        int clampH = 300;
        currentScroll.setPreferredSize(new Dimension(0, clampH));
        currentScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, clampH));

        skillViewRoot.add(currentScroll, BorderLayout.CENTER);

        /* ---------------- Attach ---------------- */

        panelMainContent.removeAll();
        panelMainContent.setLayout(new BorderLayout());
        panelMainContent.add(skillViewRoot, BorderLayout.CENTER);
        panelMainContent.revalidate();
        panelMainContent.repaint();
    }

    private void rebuildSkillFilterAndShow(HiscoreSkill skill)
    {

        if (currentSkillArr == null)
        {
            return;
        }

        // Build filtered indices
        List<Integer> idx = new ArrayList<>();
        String q = currentSkillQuery == null ? "" : currentSkillQuery.trim().toLowerCase();

        for (int i = 0; i < currentSkillArr.size(); i++)
        {
            JsonObject entry = currentSkillArr.get(i).getAsJsonObject();
            String display = getDisplayNameFromEntry(entry);
            if (display == null) continue;

            if (q.isEmpty() || display.toLowerCase().contains(q))
            {
                idx.add(i);
            }
        }

        currentSkillFilteredIdx = idx;

        // Update count label if it exists
        if (playerSearchCount != null)
        {
            playerSearchCount.setText(idx.size() + " results");
        }

        // Re-render using existing buildSkillPlayersUI logic, but with filtered indices
        buildSkillPlayersUIFiltered(skill);
    }

    private void buildSkillPlayersUIFiltered(HiscoreSkill skill)
    {
        if (currentSkillArr == null)
        {
            buildSkillPlayersUI(skill, null);
            return;
        }

        if (currentSkillFilteredIdx == null)
        {
            currentSkillFilteredIdx = new ArrayList<>();
            for (int i = 0; i < currentSkillArr.size(); i++) currentSkillFilteredIdx.add(i);
        }

        String skillName = normalizeSkillName(skill);
        boolean isSkill = SKILLS.contains(skill) || skillName.equals("overall");
        boolean isBoss  = BOSSES.contains(skill);

        ensureSkillViewBuilt(skill, isSkill, isBoss);

        if (currentSkillFilteredIdx.isEmpty())
        {
            currentModel.setRows(Collections.emptyList());
            if (playerSearchCount != null) playerSearchCount.setText("0 results");
            currentTable.repaint();
            return;
        }

        List<PlayerRow> allRows = new ArrayList<>(currentSkillFilteredIdx.size());

        for (int arrIndex : currentSkillFilteredIdx) {
            JsonObject entry = currentSkillArr.get(arrIndex).getAsJsonObject();
            JsonObject pdata = entry.getAsJsonObject("player");
            JsonObject ddata = entry.getAsJsonObject("data");

            String username = pdata.get("username").getAsString();
            String lower = username.replace("\u00A0", " ");
            String display = allMembersDisplayNames.get(lower);
            ImageIcon icon = allMembersIcons.get(lower);


            boolean isPlayer = normName(username).equals(normName(playerName));

            long xp = isSkill && ddata.has("experience") ? ddata.get("experience").getAsLong() : -1;
            if (xp < 0) xp = 0;

            int stat;
            if (isSkill) {
                if (config.displayVirtualLevels() && !skillName.equals("overall")) {
                    stat = Experience.getLevelForXp((int) Math.min(Integer.MAX_VALUE, xp));
                } else {
                    stat = ddata.has("level") ? Math.max(0, ddata.get("level").getAsInt()) : 0;
                }
            } else if (isBoss) {
                stat = ddata.has("kills") ? Math.max(0, ddata.get("kills").getAsInt()) : 0;
            } else {
                stat = ddata.has("score") ? Math.max(0, ddata.get("score").getAsInt()) : 0;
            }

            String expStr = isSkill ? (xp > 0 ? formatNumber(xp) : "--") : "";

            // IMPORTANT: keep # as global/unfiltered rank
            allRows.add(new PlayerRow(
                    arrIndex + 1,
                    display,
                    icon,
                    stat,
                    expStr,
                    isPlayer
            ));
        }

        currentModel.setRows(allRows);
        currentTable.repaint();
    }

    private static String normName(String s)
    {
        if (s == null) return "";
        return s.replace('\u00A0', ' ')   // NBSP -> space
                .trim()
                .replaceAll("\\s+", " ")  // collapse multiple spaces
                .toLowerCase();
    }

    private String getDisplayNameFromEntry(JsonObject entry)
    {
        try
        {
            JsonObject pdata = entry.getAsJsonObject("player");
            String lower = pdata.get("username").getAsString().replace("\u00A0", " ");
            return allMembersDisplayNames.get(lower);
        }
        catch (Exception ignored)
        {
            return null;
        }
    }

    private String findFirstPrefixPlayerSuggestion(String qLower)
    {
        if (currentSkillArr == null || qLower == null || qLower.isEmpty()) return null;

        for (int i = 0; i < currentSkillArr.size(); i++)
        {
            String display = getDisplayNameFromEntry(currentSkillArr.get(i).getAsJsonObject());
            if (display == null) continue;

            if (display.toLowerCase().startsWith(qLower))
            {
                return display;
            }
        }
        return null;
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
        JButton goBack = new JButton("< Go Back");
        goBack.setBorderPainted(false);

        final Color hoverColor = ColorScheme.DARKER_GRAY_HOVER_COLOR;
        final Color pressedColor = ColorScheme.DARKER_GRAY_COLOR.brighter();

        // Optional: smaller preferred width to reduce horizontal space
        goBack.setPreferredSize(new Dimension(90, 20));
        goBack.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        goBack.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                goBack.setBackground(pressedColor);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                // reset cached skill view so clicking again rebuilds/reattaches
                skillViewRoot = null;
                currentSkill = null;
                currentTable = null;
                currentScroll = null;
                currentModel = null;

                panelMainContent.removeAll();
                buildLeaderboardsPanel();
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

    private static String pad(HiscoreSkillType type)
    {
        // Left pad label text to keep labels aligned
        int pad = type == HiscoreSkillType.BOSS ? 4 : 2;
        return StringUtils.leftPad("--", pad);
    }


    private void buildDiscordPanel()
    {
        panelMainContent.removeAll();
        isInInfoPanel = false;
        final Color hoverColor = ColorScheme.DARKER_GRAY_HOVER_COLOR;
        final Color pressedColor = ColorScheme.DARKER_GRAY_COLOR.brighter();

        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.add(Box.createVerticalStrut(8));

        JLabel stats = new JLabel("Loading server stats…");
        stats.setHorizontalAlignment(SwingConstants.CENTER);
        stats.setAlignmentX(Component.CENTER_ALIGNMENT);
        container.add(stats);
        container.add(Box.createVerticalStrut(8));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton joinDiscord = getJoinDiscord(pressedColor, hoverColor);
        JButton copyInvite = getCopyInvite(pressedColor, hoverColor);
        buttons.add(joinDiscord);
        buttons.add(copyInvite);
        container.add(buttons);
        container.add(Box.createVerticalStrut(10));

        JLabel helpText = getHelpText();
        container.add(helpText);
        container.add(Box.createVerticalStrut(10));
        container.add(createTitlePanel("Announceable clogs"));
        container.add(buildSearchTablePanel());

        panelMainContent.add(container);

        populateDiscordCountsAsync(stats);

        update();
    }

    private JPanel buildSearchTablePanel()
    {
        JPanel root = new JPanel();
        root.setOpaque(false);
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));

        // ---------- Build rows ----------
        final List<Row> rows = new ArrayList<>(Constants.Pets.size() + Constants.ITEMS_WHITELIST.size());
        for (String p : Constants.Pets) rows.add(new Row("Pet", p));
        for (String i : Constants.ITEMS_WHITELIST) rows.add(new Row("Item", i));

        rows.sort((a, b) -> {
            int n = a.name.compareToIgnoreCase(b.name);
            return n != 0 ? n : a.type.compareToIgnoreCase(b.type);
        });

        final RowTableModel model = new RowTableModel(rows);

        // ---------- Search ----------
        final SuggestionTextField search = new SuggestionTextField();
        search.setToolTipText("Search (TAB accepts suggestion)");
        search.setFont(FontManager.getRunescapeSmallFont());
        search.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        search.setForeground(ColorScheme.TEXT_COLOR);
        search.setCaretColor(Color.WHITE);
        search.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        search.setFocusTraversalKeysEnabled(false); // capture TAB

        final JLabel count = new JLabel(rows.size() + " results", SwingConstants.LEFT);
        count.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        count.setFont(FontManager.getRunescapeSmallFont());
        count.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));

        JPanel searchWrap = new JPanel();
        searchWrap.setOpaque(false);
        searchWrap.setLayout(new BoxLayout(searchWrap, BoxLayout.Y_AXIS));
        searchWrap.add(search);
        searchWrap.add(Box.createVerticalStrut(2));
        searchWrap.add(count);

        root.add(searchWrap);
        root.add(Box.createVerticalStrut(4));

        // ---------- Table ----------
        final JTable table = new JTable(model);
        applyBaseTableStyle(table);

        // Column sizing
        table.getColumnModel().getColumn(0).setPreferredWidth(55);
        table.getColumnModel().getColumn(0).setMaxWidth(55);

        // Type column (center)
        table.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer()
        {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int col)
            {
                super.getTableCellRendererComponent(t, value, false, false, row, col);
                setHorizontalAlignment(SwingConstants.CENTER);
                setBackground(row % 2 == 0 ? ROW_B : ROW_A);
                setForeground(ColorScheme.TEXT_COLOR);
                setBorder(BorderFactory.createEmptyBorder());
                return this;
            }
        });

        // Name column (left, padded)
        table.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer()
        {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int col)
            {
                super.getTableCellRendererComponent(t, value, false, false, row, col);
                setHorizontalAlignment(SwingConstants.LEFT);
                setBackground(row % 2 == 0 ? ROW_B : ROW_A);
                setForeground(ColorScheme.TEXT_COLOR);
                setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 0));
                return this;
            }
        });

        // Header styling
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setResizingAllowed(false);
        header.setBorder(BorderFactory.createEmptyBorder());

        header.setDefaultRenderer(new TableCellRenderer()
        {
            private final DefaultTableCellRenderer r = new DefaultTableCellRenderer();

            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int col)
            {
                r.setOpaque(true);
                r.setBackground(ROW_A);
                r.setForeground(Color.WHITE);
                r.setFont(FontManager.getRunescapeSmallFont());
                r.setBorder(BorderFactory.createEmptyBorder());

                r.setText(value == null ? "" : value.toString());

                if (col == 1)
                {
                    r.setHorizontalAlignment(SwingConstants.LEFT);
                    r.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 0));
                }
                else
                {
                    r.setHorizontalAlignment(SwingConstants.CENTER);
                }

                return r;
            }
        });

        // ---------- Sorter + Filter ----------
        final TableRowSorter<RowTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        // Scrollpane style
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setViewportBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(ColorScheme.DARKER_GRAY_COLOR);

        // Clamp height: keep it inside RL panel
        scroll.setPreferredSize(new Dimension(0, 240));
        scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 240));

        root.add(scroll);

        // ---------- Suggestion state ----------
        final String[] currentSuggestion = { null };

        // TAB commits suggestion
        search.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("TAB"), "acceptSuggestion");
        search.getActionMap().put("acceptSuggestion", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                String sugg = currentSuggestion[0];
                if (sugg == null) return;

                search.setText(sugg);
                search.setCaretPosition(sugg.length());
                search.setSuggestion(null);
            }
        });

        // Filtering + ghost suggestion computation
        Runnable apply = () -> {
            String q = search.getText();
            String qLower = q.trim().toLowerCase();

            if (qLower.isEmpty())
            {
                sorter.setRowFilter(null);
                count.setText(rows.size() + " results");
                currentSuggestion[0] = null;
                search.setSuggestion(null);
                return;
            }

            final String needle = qLower;

            sorter.setRowFilter(new RowFilter<>() {
                @Override
                public boolean include(Entry<? extends RowTableModel, ? extends Integer> entry) {
                    int modelRow = entry.getIdentifier();
                    Row r = model.getRow(modelRow);
                    return r.name.toLowerCase().contains(needle) || r.type.toLowerCase().contains(needle);
                }
            });

            count.setText(table.getRowCount() + " results");

            String sugg = findFirstPrefixSuggestion(rows, qLower);
            if (sugg != null && sugg.equalsIgnoreCase(q.trim())) sugg = null;

            currentSuggestion[0] = sugg;
            search.setSuggestion(sugg);
        };

        search.getDocument().addDocumentListener(new DocumentListener()
        {
            private void update() { SwingUtilities.invokeLater(apply); }
            @Override public void insertUpdate(DocumentEvent e) { update(); }
            @Override public void removeUpdate(DocumentEvent e) { update(); }
            @Override public void changedUpdate(DocumentEvent e) { update(); }
        });

        SwingUtilities.invokeLater(apply);

        return root;
    }

    private String findFirstPrefixSuggestion(List<Row> rows, String qLower)
    {
        if (qLower == null || qLower.isEmpty()) return null;

        for (Row r : rows)
        {
            String n = r.name.toLowerCase();
            if (n.startsWith(qLower))
            {
                return r.name;
            }
        }
        return null;
    }

    private static class Row
    {
        final String type;
        final String name;
        Row(String type, String name) { this.type = type; this.name = name; }
    }

    private static class RowTableModel extends AbstractTableModel
    {
        private final List<Row> rows;
        private final String[] cols = { "Type", "Name" };

        RowTableModel(List<Row> rows) { this.rows = rows; }

        Row getRow(int modelRow) { return rows.get(modelRow); }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        @Override public Class<?> getColumnClass(int c) { return String.class; }

        @Override
        public Object getValueAt(int r, int c)
        {
            Row row = rows.get(r);
            return c == 0 ? row.type : row.name;
        }
    }

    /**
     * A JTextField that paints a "ghost" suggestion after the user's typed text.
     * The suggestion is purely cosmetic and does NOT affect the document content.
     */
    private static class SuggestionTextField extends JTextField
    {
        private String suggestion;

        public void setSuggestion(String suggestion)
        {
            this.suggestion = suggestion;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            super.paintComponent(g);

            if (suggestion == null) return;

            String typed = getText();
            if (typed == null) typed = "";

            // Only show ghost remainder when suggestion starts with typed text
            if (typed.isEmpty()) return;
            if (typed.length() >= suggestion.length()) return;

            String sugLower = suggestion.toLowerCase();
            String typedLower = typed.toLowerCase();
            if (!sugLower.startsWith(typedLower)) return;

            String remainder = suggestion.substring(typed.length());

            Graphics2D g2 = (Graphics2D) g.create();
            try
            {
                // Use a subtle color similar to placeholder text
                g2.setColor(new Color(180, 180, 180, 120)); // light + semi-transparent
                g2.setFont(getFont().deriveFont(Font.ITALIC));

                Insets insets = getInsets();
                FontMetrics fm = g2.getFontMetrics(getFont());

                // X position = start + width of typed text
                int x = insets.left + fm.stringWidth(typed);

                // Y position = baseline of text in field
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();

                g2.drawString(remainder, x, y);
            }
            finally
            {
                g2.dispose();
            }
        }
    }

    private static JLabel getHelpText() {
        JLabel helpText = new JLabel(
                "<html><div style='text-align:center; line-height:1.3'>" +
                        "<b>Rankings • Events • Discussion</b><br>" +
                        "Join the clan community!" +
                        "</div></html>"
        );

        helpText.setAlignmentX(Component.CENTER_ALIGNMENT);
        helpText.setHorizontalAlignment(SwingConstants.CENTER);
        helpText.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        return helpText;
    }

    private JButton getCopyInvite(Color pressedColor, Color hoverColor) {
        JButton copyInvite = new JButton("Copy Invite");
        copyInvite.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        copyInvite.addMouseListener(new MouseAdapter()
        {
            @Override public void mousePressed(MouseEvent e) {
                copyInvite.setBackground(pressedColor);
            }
            @Override public void mouseReleased(MouseEvent e) {
                copyToClipboard(Constants.LINK_DISCORD);
                copyInvite.setBackground(hoverColor);
            }
            @Override public void mouseEntered(MouseEvent e) {
                copyInvite.setBackground(hoverColor);
                copyInvite.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
            @Override public void mouseExited(MouseEvent e)  {
                copyInvite.setBackground(ColorScheme.DARKER_GRAY_COLOR);
                copyInvite.setCursor(Cursor.getDefaultCursor());
            }
        });
        return copyInvite;
    }

    private static JButton getJoinDiscord(Color pressedColor, Color hoverColor) {
        JButton joinDiscord = new JButton("Join Discord");
        joinDiscord.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        joinDiscord.addMouseListener(new MouseAdapter()
        {
            @Override public void mousePressed(MouseEvent e) {
                joinDiscord.setBackground(pressedColor);
            }
            @Override public void mouseReleased(MouseEvent e) {
                LinkBrowser.browse(Constants.LINK_DISCORD);
                joinDiscord.setBackground(hoverColor);
            }
            @Override public void mouseEntered(MouseEvent e) {
                joinDiscord.setBackground(hoverColor);
                joinDiscord.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
            @Override public void mouseExited(MouseEvent e)  {
                joinDiscord.setBackground(ColorScheme.DARKER_GRAY_COLOR);
                joinDiscord.setCursor(Cursor.getDefaultCursor());
            }
        });
        return joinDiscord;
    }

    private void populateDiscordCountsAsync(JLabel statsLabel) {

        SwingWorker<DiscordCounts, Void> worker = new SwingWorker<>() {

            @Override
            protected DiscordCounts doInBackground() throws Exception {
                return fetchDiscordCounts();
            }

            @Override
            protected void done() {
                try {
                    DiscordCounts counts = get();

                    if (counts == null) {
                        statsLabel.setText("Discord stats unavailable");
                        return;
                    }

                    statsLabel.setText(
                            "<html><div style='text-align:center'>" +
                                    "👥 "
                                    + formatNumber(counts.members) + " members"
                                    + nonBreakingSpaces(5) +
                                    "<span style='color:#00ff00;'>⬤</span> "
                                    + formatNumber(counts.online) + " online" +
                                    "</div></html>"
                    );

                } catch (Exception e) {
                    statsLabel.setText("Discord stats unavailable.");
                    log.debug("Discord stats error", e);
                }
            }
        };

        worker.execute();
    }

    private String nonBreakingSpaces(int n)
    {
        return "&nbsp;".repeat(Math.max(0, n));
    }

    private void copyToClipboard(String text)
    {
        try
        {
            Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .setContents(new StringSelection(text), null);
        }
        catch (Exception ignored)
        {
            // ignored
        }
    }

    private void buildLeaderboardsPanel()
    {
        isInInfoPanel = false;
        panelMainContent.removeAll();
        panelMainContent.setLayout(new BorderLayout());

        // 1) show UI immediately (unpopulated)
        JPanel skeleton = buildTopChartsSkeleton();
        panelMainContent.add(skeleton, BorderLayout.CENTER);

        panelMainContent.revalidate();
        panelMainContent.repaint();

        // 2) populate afterward
        fetchAndPopulateTopChartsAsync();
    }

    private void buildModToolsPanel() {
        panelMainContent.removeAll();
        isInInfoPanel = false;
        panelMainContent.add(modToolsPanel);

        update();
    }

    private String normalizeSkillName(HiscoreSkill skill) {
        String name = skill.toString().toLowerCase();
        return Constants.NORMALIZED_NAMES.getOrDefault(name, name);
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

            String username = player.get("username").getAsString().replace("\u00A0", " ");

            if (
                    (data.has("kills") && data.get("kills").getAsInt() <= 0) ||
                    (data.has("experience") && data.get("experience").getAsLong() <= 0) ||
                    (data.has("score") && data.get("score").getAsInt() <= 0)
            ) {
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

    private JLabel buildAutoSizedTitle(String text, int maxWidth, float startSize) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);

        Font base = FontManager.getRunescapeBoldFont().deriveFont(startSize);
        FontMetrics fm = label.getFontMetrics(base);

        float size = startSize;
        while (fm.stringWidth(text) > maxWidth && size > 8f) {
            size -= 1f;
            base = base.deriveFont(size);
            fm = label.getFontMetrics(base);
        }

        label.setFont(base);
        return label;
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
        BufferedImage clueImg = getClueScrollIcon(skill);

        if (clueImg != null) {
            BufferedImage scaled = ImageUtil.resizeImage(ImageUtil.resizeCanvas(clueImg, 25, 25), 30, 30);
            iconLabel.setIcon(new ImageIcon(scaled));
        }
        else {
            spriteManager.getSpriteAsync(
                    skill == null ? SpriteID.SideIcons.COMBAT : skill.getSpriteId(),
                    0,
                    sprite -> SwingUtilities.invokeLater(() -> {
                        BufferedImage scaled = ImageUtil.resizeImage(ImageUtil.resizeCanvas(sprite, 25, 25), 30, 30);
                        iconLabel.setIcon(new ImageIcon(scaled));
                    })
            );
        }

        JLabel nameLabel = buildAutoSizedTitle(skill.getName(), 180, 18f);

        header.add(iconLabel);
        header.add(nameLabel);

        return header;
    }



    private static class PlayerTableModel extends AbstractTableModel {

        private List<PlayerRow> rows;
        private final String[] cols;

        PlayerTableModel(List<PlayerRow> rows, boolean skill, boolean boss)
        {
            this.rows = rows;
            this.cols = skill
                    ? new String[]{"#", "Player", "Level", "Exp"}
                    : boss ? new String[]{"#", "Player", "Kills"}
                    : new String[]{"#", "Player", "Total"};
        }

        void setRows(List<PlayerRow> newRows) {
            this.rows = newRows;
            fireTableDataChanged();
        }

        List<PlayerRow> getRows() {
            return rows;
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
        applyBaseTableStyle(table);

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setResizingAllowed(false);
        header.setBorder(BorderFactory.createEmptyBorder());

        // Header renderer
        header.setDefaultRenderer(new TableCellRenderer()
        {
            private final DefaultTableCellRenderer r = new DefaultTableCellRenderer();

            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int col)
            {
                r.setOpaque(true);
                r.setBackground(ROW_A);
                r.setForeground(Color.WHITE);
                r.setFont(FontManager.getRunescapeSmallFont());
                r.setBorder(BorderFactory.createEmptyBorder());

                r.setText(value == null ? "" : value.toString());

                // "#": center, "Player": left padded, rest center
                if (col == 1) {
                    r.setHorizontalAlignment(SwingConstants.LEFT);
                    r.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 0));
                } else {
                    r.setHorizontalAlignment(SwingConstants.CENTER);
                }

                return r;
            }
        });

        if (isSkill){
            table.getColumnModel().getColumn(0).setMaxWidth(30);  // #
            table.getColumnModel().getColumn(2).setMaxWidth(40);  // level/total
            table.getColumnModel().getColumn(3).setMaxWidth(40);  // exp
        }
        else {
            table.getColumnModel().getColumn(0).setMaxWidth(30);  // #
            table.getColumnModel().getColumn(2).setMaxWidth(40);  // kills/score
        }

        table.getColumnModel().getColumn(1).setCellRenderer(new PlayerRenderer());

        for (int col = 0; col < table.getColumnCount(); col++) {
            if (col != 1) {
                table.getColumnModel().getColumn(col).setCellRenderer(new DefaultTableCellRenderer() {
                    @Override
                    public Component getTableCellRendererComponent(
                            JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                        PlayerTableModel model = (PlayerTableModel) table.getModel();
                        PlayerRow playerRow = model.getRows().get(row);

                        if (playerRow.highlight) {
                            setForeground(Color.GREEN);
                        } else {
                            setForeground(ColorScheme.TEXT_COLOR);
                        }
                        setBackground(row % 2 == 0 ? ROW_B : ROW_A);

                        setHorizontalAlignment(SwingConstants.CENTER);
                        setBorder(BorderFactory.createEmptyBorder());
                        return this;
                    }
                });
            }
        }

        table.setFillsViewportHeight(true);

        table.setAutoResizeMode(isSkill
                ? JTable.AUTO_RESIZE_LAST_COLUMN
                : JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
    }
    @Nullable
    private static BufferedImage getClueScrollIcon(HiscoreSkill skill)
    {
        if (skill == null)
            return null;

        switch (skill)
        {
            case CLUE_SCROLL_BEGINNER: return Icons.CLUE_SCROLL_BEGINNER;
            case CLUE_SCROLL_EASY:     return Icons.CLUE_SCROLL_EASY;
            case CLUE_SCROLL_MEDIUM:   return Icons.CLUE_SCROLL_MEDIUM;
            case CLUE_SCROLL_HARD:     return Icons.CLUE_SCROLL_HARD;
            case CLUE_SCROLL_ELITE:    return Icons.CLUE_SCROLL_ELITE;
            case CLUE_SCROLL_MASTER:   return Icons.CLUE_SCROLL_MASTER;
            default: return null;
        }
    }



    private static class PlayerRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col)
        {
            super.getTableCellRendererComponent(table, "", false, false, row, col);

            // Always reset everything (renderer is reused)
            setIcon(null);
            setText("");
            setHorizontalAlignment(LEFT);
            setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 0));

            // Default row striping
            setForeground(ColorScheme.TEXT_COLOR);
            setBackground(row % 2 == 0 ? ROW_B : ROW_A);

            PlayerRow pr = value instanceof PlayerRow ? (PlayerRow) value : null;
            if (pr != null)
            {
                setText(" " + pr.name);
                setIcon(pr.icon);

                if (pr.highlight)
                {
                    setForeground(Color.GREEN);
                }
            }

            return this;
        }
    }

    private static class DiscordCounts {
        final int members;
        final int online;

        DiscordCounts(int members, int online) {
            this.members = members;
            this.online = online;
        }
    }

    private DiscordCounts fetchDiscordCounts() throws IOException, InterruptedException {
        String response = rateLimitedHttpCache.fetch(Constants.LINK_DISCORD_API);
        if (response == null) {
            return null;
        }

        JsonParser jsonParser = new JsonParser();
        JsonObject obj = jsonParser.parse(response).getAsJsonObject();

        int members = obj.has("approximate_member_count") ? obj.get("approximate_member_count").getAsInt() : -1;
        int online  = obj.has("approximate_presence_count") ? obj.get("approximate_presence_count").getAsInt() : -1;

        return new DiscordCounts(members, online);
    }


    public class RateLimitedHttpCache {

        private static final long TTL_MILLIS = 60 * 1000; // 1 minute
        private final ConcurrentHashMap<String, CachedItem> cache = new ConcurrentHashMap<>();
        private final Semaphore rateLimiter;
        private final ScheduledExecutorService scheduler;


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
        public String fetch(String url) throws IOException {
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

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            Call call = httpClient.newCall(request);

            try (Response response = call.execute())
            {
                if (!response.isSuccessful())
                {
                    log.debug("HTTP error {} for {}", response.code(), url);
                    return null;
                }

                ResponseBody body = response.body();
                if (body == null)
                {
                    return null;
                }

                String responseBody = body.string();
                cache.put(url, new CachedItem(responseBody, now));
                return responseBody;
            }
        }

        public void shutdown() {
            scheduler.shutdown();
        }
    }

}
