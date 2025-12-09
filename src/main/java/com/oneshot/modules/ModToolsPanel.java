package com.oneshot.modules;

//import com.google.gson.Gson;
//import com.oneshot.OneShotConfig;
//import net.runelite.client.config.ConfigManager;
//import net.runelite.client.ui.ColorScheme;
//import net.runelite.client.ui.PluginPanel;
//import javax.swing.*;
//import java.awt.*;
//
//
//import okhttp3.*;
//
//import javax.inject.Inject;
//import javax.inject.Singleton;
//import javax.swing.*;
//import java.awt.*;
//import java.awt.datatransfer.StringSelection;
//import java.awt.event.ActionListener;
//import java.awt.image.BufferedImage;
//import java.io.IOException;


import com.google.gson.*;

import com.oneshot.OneShotConfig;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import net.runelite.client.config.*;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Callback;

//@Singleton
public class ModToolsPanel extends PluginPanel
{
    private static final Logger log = LoggerFactory.getLogger(ModToolsPanel.class);
    private final ConfigManager configManager;
    private final OneShotConfig config;

    private Map<String, Integer> hcimPlayers = new HashMap<>();
    private Map<String, String> womPlayers = new HashMap<>();

    private JTable hcimTable;
    private JTable womTable;

    private String womFilter = "All";

    private final ModTools modToolsInstance;



    @Inject
    private OkHttpClient httpClient;

    @Inject
    private Gson gson;


    private JLabel queuedLabel;
    private JLabel checkedLabel;
    private JLabel hcimLabel;

    private JPanel titlePanel = new JPanel();
    private JPanel checkPanel = new JPanel();
    private JPanel scoutPanel = new JPanel();

    {
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        checkPanel.setLayout(new BoxLayout(checkPanel, BoxLayout.Y_AXIS));
        scoutPanel.setLayout(new BoxLayout(scoutPanel, BoxLayout.Y_AXIS));
    }

    private boolean scoutChildHidden = true;
    private boolean statsChildHidden = true;


    @Inject
    public ModToolsPanel(ConfigManager configManager, OneShotConfig config, ModTools modTools)
    {
//        log.debug("ModToolsPanel constructed, instance: {}", System.identityHashCode(this));

        this.configManager = configManager;
        this.config = config;
        this.modToolsInstance = modTools;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        init();

        add(titlePanel);
        add(checkPanel);
        add(scoutPanel);

        createTitlePanel();
        createIronmanCheckSection();
        createIronmanScoutSection();

    }

    public void init()
    {
        queuedLabel = new JLabel();
        checkedLabel = new JLabel();
        hcimLabel = new JLabel();
        queuedLabel.setText("In Queue: 0");
        checkedLabel.setText("Total Checked: 0");
        hcimLabel.setText("Total HCIM found: 0");
    }

    private void createTitlePanel()
    {
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        container.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1),
                BorderFactory.createEmptyBorder(4, 6, 4, 6) // Padding sized to text height
        ));
        JLabel titleText = new JLabel("Mod Tools", SwingConstants.CENTER);
        container.add(titleText);
        titlePanel.add(container);
    }

    private void createIronmanCheckSection()
    {
        JPanel childPanel = createIronmanCheckContentPanel();

        checkPanel.add(createCollapsibleHeader("WOM Non-Hardcore Status", childPanel));
        checkPanel.add(childPanel);
        childPanel.setVisible(!statsChildHidden);
    }

    private JPanel createIronmanCheckContentPanel()
    {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        container.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

        JButton btn = new JButton("Fetch from WOM");
        btn.setForeground(ColorScheme.TEXT_COLOR);
        btn.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        btn.setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR));
        btn.setFocusPainted(false);
        btn.setOpaque(true);

        btn.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Hover effect
        btn.addMouseListener(new java.awt.event.MouseAdapter()
        {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e)
            {
                btn.setBackground(ColorScheme.DARK_GRAY_HOVER_COLOR);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e)
            {
                btn.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            }

            @Override
            public void mouseClicked(java.awt.event.MouseEvent e)
            {
                fetchWiseOldManGroup(ModToolsPanel.this::refreshWomTable);
            }
        });

        container.add(btn);
        container.add(createWomFilter());
        container.add(createWomList());

        return container;
    }

    private JPanel createWomFilter() {
        JPanel container = new JPanel(new BorderLayout());

        // ---------- FILTER DROPDOWN ----------
        String[] filterOptions = {"All", "Regular", "Ironman"};
        JComboBox<String> filterBox = new JComboBox<>(filterOptions);
        filterBox.setSelectedItem("All");
        filterBox.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        filterBox.addActionListener(e -> {
            womFilter = (String) filterBox.getSelectedItem();
            refreshWomTable();
        });

        JPanel filterPanel = new JPanel(new BorderLayout());
        filterPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        filterPanel.add(new JLabel("Type: "), BorderLayout.WEST);
        filterPanel.add(filterBox, BorderLayout.CENTER);

        container.add(filterPanel, BorderLayout.NORTH);

        return container;
    }

    private JPanel createWomList()
    {
        JPanel container = new JPanel(new BorderLayout());

        // --- TABLE ---
        womTable = new JTable(womTableModel);
        TableRowSorter<DefaultTableModel> sorter =
                new TableRowSorter<>(womTableModel);
        womTable.setRowSorter(sorter);
        womTable.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        womTable.setForeground(ColorScheme.TEXT_COLOR);
        womTable.setFillsViewportHeight(true);
        womTable.setShowGrid(false);

        // Fill with current data
        refreshWomTable();

        // --- CENTER ALIGN COLUMNS ---
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        womTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        womTable.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);

        // --- COLUMN WIDTHS (70% / 30%) ---
        int totalWidth = PluginPanel.PANEL_WIDTH - 20; // minus scroll padding
        womTable.getColumnModel().getColumn(0).setPreferredWidth((int)(totalWidth * 0.50));
        womTable.getColumnModel().getColumn(1).setPreferredWidth((int)(totalWidth * 0.50));

        // --- SCROLLPANE (scrolls when needed) ---
        JScrollPane scrollPane = new JScrollPane(womTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR));
        scrollPane.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 12, 160));

        container.add(scrollPane, BorderLayout.CENTER);

        return container;
    }

    private void refreshWomTable()
    {
        womTableModel.setRowCount(0); // clear

        if (womPlayers.isEmpty())
        {
            womTableModel.addRow(new Object[]{"Empty", ""});
        }
        else
        {
            womPlayers.forEach((name, type) -> {
                // FILTER logic:
                if (womFilter.equals("All") || womFilter.toLowerCase().equals(type))
                {
                    womTableModel.addRow(new Object[]{name, type});
                }
            });
        }

        womTable.revalidate();
        womTable.repaint();
    }

    private void fetchWiseOldManGroup(Runnable onFinished)
    {
        // WOM group ID — hardcoded for now
        int groupId = 2647;

        String url = "https://api.wiseoldman.net/v2/groups/" + groupId;

        Request request = new Request.Builder()
                .url(url)
                .build();

        httpClient.newCall(request).enqueue(new okhttp3.Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                log.error("Failed to fetch WOM group data", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException
            {
                if (!response.isSuccessful())
                {
                    log.error("Bad response from WOM: {}", response.code());
                    return;
                }

                String json = response.body().string();
                parseWom(json);

                SwingUtilities.invokeLater(onFinished);
            }
        });
    }

    private void parseWom(String json)
    {
        JsonObject root = new Gson().fromJson(json, JsonObject.class);
        JsonArray memberships = root.getAsJsonArray("memberships");

        for (JsonElement el : memberships)
        {
            JsonObject membership = el.getAsJsonObject();
            JsonObject player = membership.getAsJsonObject("player");

            String name = player.get("displayName").getAsString();
            String type = player.get("type").getAsString();

            if (!type.equals("hardcore"))
            {

                // Add to your UI list:
                SwingUtilities.invokeLater(() ->
                        womPlayers.put(name, type)
                );
            }
        }
    }



    private void createIronmanScoutSection()
    {
        JPanel childPanel = createIronmanScoutContentPanel();

        scoutPanel.add(createCollapsibleHeader("HCIM Scouter", childPanel));
        scoutPanel.add(childPanel);
        childPanel.setVisible(!scoutChildHidden);
    }

    private JPanel createIronmanScoutContentPanel()
    {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        container.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_COLOR, 1),
                BorderFactory.createEmptyBorder(4, 6, 4, 6) // Padding sized to text height
        ));

        container.add(createLabeledStatRow(queuedLabel));
        container.add(createLabeledStatRow(checkedLabel));
        container.add(createLabeledStatRow(hcimLabel));
        container.add(createIronmanScoutList());
        container.add(createIronmanScoutSettings());

        return container;
    }

    private JPanel createConfigPanel(String keyName, Callback callback)
    {
        return createConfigPanel(OneShotConfig.GROUP, keyName, callback);
    }

    private JPanel createConfigPanel(String groupName, String keyName, Callback callback)
    {
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        String displayName = getConfigItemName(groupName, keyName);
        JLabel label = new JLabel(displayName);
        JCheckBox checkBox = new JCheckBox();
        checkBox.setSelected(callback.get());

        // When user toggles the checkbox, update the config
        checkBox.addItemListener(e ->
                configManager.setConfiguration(groupName, keyName, checkBox.isSelected())
        );

        container.add(label, BorderLayout.WEST);
        container.add(checkBox, BorderLayout.EAST);

        return container;
    }

    private JPanel createIntConfigPanel(String keyName, IntCallback callback)
    {
        return createIntConfigPanel(OneShotConfig.GROUP, keyName, callback);
    }

    private JPanel createIntConfigPanel(String groupName, String keyName, IntCallback callback)
    {
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        // Display label
        String displayName = getConfigItemName(groupName, keyName);
        JLabel label = new JLabel(displayName);

        // Get range metadata
        Range range = getRange(keyName);
        int min = (range != null) ? range.min() : Integer.MIN_VALUE;
        int max = (range != null) ? range.max() : Integer.MAX_VALUE;

        // Spinner with range bounds
        int currentValue = callback.get();
        SpinnerNumberModel model = new SpinnerNumberModel(currentValue, min, max, 1);
        JSpinner spinner = new JSpinner(model);

        // Update config on change
        spinner.addChangeListener(e -> {
            configManager.setConfiguration(groupName, keyName, spinner.getValue().toString());

            if (keyName.equals("hcimscoutMinimumTotal")) {
                refreshIronmanScoutTable();
            }
        });


        container.add(label, BorderLayout.WEST);
        container.add(spinner, BorderLayout.EAST);

        return container;
    }


    private JPanel createIronmanScoutSettings() {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));

        container.add(createConfigPanel("hcimscoutEnable",  config::hcimscoutEnable));
        container.add(createConfigPanel("hcimscoutRedText", config::hcimscoutRedText));
        container.add(createConfigPanel("hcimscoutHide", config::hcimscoutHide));
        container.add(createIntConfigPanel("lookupCooldown", config::lookupCooldown));
        container.add(createIntConfigPanel("hcimscoutMinimumTotal", config::hcimscoutMinimumTotal));

        return container;
    }

    @FunctionalInterface
    interface Callback {
        boolean get();
    }

    @FunctionalInterface
    interface IntCallback {
        int get();
    }

    private String getConfigItemName(String groupName, String keyName)
    {
        ConfigDescriptor descriptor = configManager.getConfigDescriptor(config);

        for (ConfigItemDescriptor itemDesc : descriptor.getItems())
        {
            ConfigItem item = itemDesc.getItem();
            if (item.keyName().equals(keyName))
            {
                return item.name();
            }
        }
        return keyName;
    }

    private Range getRange(String keyName)
    {
        ConfigDescriptor descriptor = configManager.getConfigDescriptor(config);

        for (ConfigItemDescriptor itemDesc : descriptor.getItems())
        {
            ConfigItem item = itemDesc.getItem();
            if (item.keyName().equals(keyName))
            {
                return itemDesc.getRange();  // <-- This gives the @Range annotation
            }
        }
        return null;
    }



    /**
     *default boolean hcimscoutEnable(){
     *         return false;
     *     }
     *
     *     default boolean hcimscoutRedText(){
     *         return false;
     *     }
     *
     *     default int lookupCooldown(){ return 2; }
     *     @Range(min = 1, max = 20)
     *
     *      default int hcimscoutMinimumTotal(){
     *         return 1000;
     *     }
     *
     */

    private DefaultTableModel hcimTableModel = new DefaultTableModel(
            new Object[]{"Player", "Total"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column)
        {
            return false;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex)
        {
            // Player column is String
            if (columnIndex == 0)
                return String.class;

            // Total column is Integer
            if (columnIndex == 1)
                return Integer.class;

            return Object.class;
        }
    };



    private DefaultTableModel womTableModel = new DefaultTableModel(
            new Object[]{"Player", "Type"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false; // NON-EDITABLE
        }
    };

    public void refreshIronmanScoutTable()
    {
        hcimTableModel.setRowCount(0); // clear

        int minTotal = config.hcimscoutMinimumTotal();   // <--- FILTER VALUE

        if (hcimPlayers.isEmpty())
        {
            hcimTableModel.addRow(new Object[]{"Empty", ""});
        }
        else
        {
            hcimPlayers.forEach((name, total) ->
            {
                if (total >= minTotal)     // <---- SHOW ONLY VALID ENTRIES
                {
                    hcimTableModel.addRow(new Object[]{name, total});
                }
            });
        }

        hcimTable.revalidate();
        hcimTable.repaint();
    }

    private JPanel createIronmanScoutList()
    {
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(ColorScheme.DARK_GRAY_COLOR);
        container.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

//        // --- TABLE MODEL (NON-EDITABLE) ---
//        hcimTableModel = new DefaultTableModel(new Object[]{"Player", "Total"}, 0)
//        {
//            @Override
//            public boolean isCellEditable(int row, int column)
//            {
//                return false; // makes table read-only
//            }
//        };

        // --- TABLE ---
        hcimTable = new JTable(hcimTableModel);
        TableRowSorter<DefaultTableModel> sorter =
                new TableRowSorter<>(hcimTableModel);
        hcimTable.setRowSorter(sorter);
        hcimTable.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        hcimTable.setForeground(ColorScheme.TEXT_COLOR);
        hcimTable.setFillsViewportHeight(true);
        hcimTable.setShowGrid(false);

        // Fill with current data
        refreshIronmanScoutTable();

        // --- CENTER ALIGN SECOND COLUMN ---
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        hcimTable.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);

        // --- COLUMN WIDTHS (70% / 30%) ---
        int totalWidth = PluginPanel.PANEL_WIDTH - 20; // minus scroll padding
        hcimTable.getColumnModel().getColumn(0).setPreferredWidth((int)(totalWidth * 0.70));
        hcimTable.getColumnModel().getColumn(1).setPreferredWidth((int)(totalWidth * 0.30));

        // --- SCROLLPANE (scrolls when needed) ---
        JScrollPane scrollPane = new JScrollPane(hcimTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR));
        scrollPane.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 12, 160));

        container.add(scrollPane, BorderLayout.CENTER);

        return container;
    }


    private JPanel createLabeledStatRow(JLabel label)
    {
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        container.add(label,BorderLayout.WEST);
        return container;
    }

    private JPanel createCollapsibleHeader(String text, JPanel childPanel)
    {
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        container.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1),
                BorderFactory.createEmptyBorder(4, 6, 4, 6) // Padding sized to text height
        ));

        JLabel title = new JLabel(text);
        JButton toggleBtn = createHeaderToggleButton(childPanel);

        container.add(title, BorderLayout.WEST);
        container.add(toggleBtn, BorderLayout.EAST);

        return container;
    }

    private JButton createHeaderToggleButton(JPanel childPanel)
    {
        JButton btn = new JButton("▼");

        btn.setForeground(ColorScheme.TEXT_COLOR);
        btn.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        btn.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        btn.setFocusPainted(false);
        btn.setOpaque(true);

        // Hover effect
        btn.addMouseListener(new java.awt.event.MouseAdapter()
        {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e)
            {
                btn.setBackground(ColorScheme.DARK_GRAY_HOVER_COLOR);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e)
            {
                btn.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            }

            @Override
            public void mouseClicked(java.awt.event.MouseEvent e)
            {
                boolean isVisible = childPanel.isVisible();
                childPanel.setVisible(!isVisible);
                btn.setText(!isVisible ? "▲" : "▼");

                // Force relayout and repaint so the gap collapses
                childPanel.getParent().revalidate();
                childPanel.getParent().repaint();
            }
        });

        return btn;
    }

    private void update()
    {
        revalidate();
        repaint();
    }


    /*--------------------------------------------------------
     *                PUBLIC API FOR DATA UPDATES
     *--------------------------------------------------------*/
    public void updateCounts(int queued, int checked, int hcimFound)
    {
//        log.debug("updateCounts on panel instance: {}", System.identityHashCode(this));
//        log.debug("Update Q: {} C: {} F: {}", queued, checked, hcimFound);

        queuedLabel.setText("In Queue: " + queued);
        checkedLabel.setText("Total Checked: " + checked);
        hcimLabel.setText("Total HCIM found: " + hcimFound);

        update();
    }

    public void setList(Map<String, Integer> hcimPlayers) {
        this.hcimPlayers = hcimPlayers;
        refreshIronmanScoutTable();
        update();
    }
}