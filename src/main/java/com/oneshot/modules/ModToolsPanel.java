package com.oneshot.modules;

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

//@Singleton
public class ModToolsPanel extends PluginPanel
{
    @Inject
    private OkHttpClient httpClient;

    @Inject
    private Gson gson;

    private static final Logger log = LoggerFactory.getLogger(ModToolsPanel.class);
    private final ConfigManager configManager;
    private final OneShotConfig config;

    private Map<String, String> womPlayers = new HashMap<>();
    private JTable womTable;
    private String womFilter = "All";


    private JLabel queuedLabel;
    private JLabel checkedLabel;
    private JLabel hcimLabel;

    private JPanel titlePanel = new JPanel();
    private JPanel checkPanel = new JPanel();

    {
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        checkPanel.setLayout(new BoxLayout(checkPanel, BoxLayout.Y_AXIS));
    }

    private boolean statsChildHidden = true;


    @Inject
    public ModToolsPanel(ConfigManager configManager, OneShotConfig config, ModTools modTools)
    {
//        log.debug("ModToolsPanel constructed, instance: {}", System.identityHashCode(this));

        this.configManager = configManager;
        this.config = config;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        init();

        add(titlePanel);
        add(checkPanel);

        createTitlePanel();
        createIronmanCheckSection();

    }

    public void init()
    {
        queuedLabel = new JLabel();
        checkedLabel = new JLabel();
        hcimLabel = new JLabel();
        queuedLabel.setText("In Queue: 0");
        checkedLabel.setText("Total Checked: 0");
        hcimLabel.setText("Total HCIM: 0");
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
        JsonObject root = gson.fromJson(json, JsonObject.class);
        JsonArray memberships = root.getAsJsonArray("memberships");
        womPlayers.clear();

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

    private DefaultTableModel womTableModel = new DefaultTableModel(
            new Object[]{"Player", "Type"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false; // NON-EDITABLE
        }
    };

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
}