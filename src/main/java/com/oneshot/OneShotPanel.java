package com.oneshot;

import com.oneshot.utils.Constants;
import com.oneshot.utils.Icons;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.StyleContext;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OneShotPanel extends PluginPanel
{
    private static final Logger log = LoggerFactory.getLogger(OneShotPanel.class);

    JLabel intro_top_text = new JLabel("", SwingConstants.CENTER);
    JLabel intro_bottom_text = new JLabel("", SwingConstants.CENTER);

    JPanel panelMainContent = new JPanel();

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

    public void buildMainPanel(boolean isModerator, String playerName, String clanRankName, ImageIcon iconRank)
    {
        removeAll();

        JPanel playerRankPanel = buildPlayerPanel(playerName, clanRankName, iconRank);

        // button panel
        int nIcons = isModerator ? Constants.BUTTON_NUMBER : Constants.BUTTON_NUMBER - 1;

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1, nIcons, 5, 5));

        ImageIcon iconInfo = Icons.INFO;
        ImageIcon iconDiscord = Icons.DISCORD;
        ImageIcon iconEvents = Icons.EVENT;
        ImageIcon iconScout = Icons.SCOUT;

        JButton infoButton = buildButton(iconInfo, this::buildInfoPanel);
        JButton discordButton = buildButton(iconDiscord, this::buildDiscordPanel, Constants.DISCORD_TIP);
        JButton eventsButton = buildButton(iconEvents, this::buildEventsPanel);
        JButton scoutButton = buildButton(iconScout, this::buildScoutPanel);

        buttonPanel.add(infoButton);
        buttonPanel.add(discordButton);
        buttonPanel.add(eventsButton);
        if (isModerator)
        {
            buttonPanel.add(scoutButton);
        }

        add(Box.createGlue());
        add(playerRankPanel);

        add(Box.createGlue());
        add(buttonPanel);

        // Main Panel
        add(Box.createGlue());
        add(panelMainContent);

        // Version Panel
        update();
    }

    private void buildInfoPanel()
    {
        panelMainContent.removeAll();
        JLabel info = new JLabel("Work In Progress");
        panelMainContent.add(info);

        update();
    }

    private void buildDiscordPanel() {
        LinkBrowser.browse(Constants.DISCORD_LINK);
    }

    private void buildEventsPanel()
    {
        panelMainContent.removeAll();
        JLabel info = new JLabel("Work In Progress");
        panelMainContent.add(info);
        update();
    }

    private void buildScoutPanel()
    {
        panelMainContent.removeAll();
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
        container.setLayout(new GridLayout(1, 2));

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
        //container.setLayout(new GridLayout(1, 2));

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


}

