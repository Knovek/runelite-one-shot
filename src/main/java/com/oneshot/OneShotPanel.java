package com.oneshot;

import com.google.common.base.MoreObjects;
import com.google.inject.Inject;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Named;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;

import com.oneshot.utils.Constants;
import com.oneshot.utils.Icons;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ClanChannelChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.RuneLiteProperties;
import net.runelite.client.account.SessionManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.SessionClose;
import net.runelite.client.events.SessionOpen;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;
import net.runelite.api.events.ClanChannelChanged;

public class OneShotPanel extends PluginPanel
{
    private JPanel actionsContainer;

    @Inject
    private Client client;

    @Inject
    private EventBus eventBus;

    private JPanel userPanel = new JPanel();

    void init()
    {
        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // Version Panel
        JPanel versionPanel = new JPanel();
        versionPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        versionPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        versionPanel.setLayout(new GridLayout(0, 1));

        final Font smallFont = FontManager.getRunescapeSmallFont();

        JLabel version = new JLabel(htmlLabel("Plugin version: ", Constants.version));
        version.setFont(smallFont);
        versionPanel.add(version);

        // User Panel
        userPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        userPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        userPanel.setLayout(new GridLayout(0, 1));

        cleanPanel();

        add(versionPanel, BorderLayout.NORTH);
        add(userPanel, BorderLayout.CENTER);

        eventBus.register(this);
    }

    void deinit()
    {
        eventBus.unregister(this);
    }


    private static JPanel buildRank(BufferedImage image, String name)
    {
        JPanel container = new JPanel();
        container.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        container.setBorder(new EmptyBorder(1, 1, 1, 1));

        ImageIcon icon = new ImageIcon(image);
        icon.setImage(icon.getImage().getScaledInstance(13,13, Image.SCALE_DEFAULT));

        JLabel iconLabel = new JLabel(icon);
        JLabel nameLabel = new JLabel(name);

        container.add(iconLabel);
        container.add(nameLabel);

        return container;
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged)
    {
        if (gameStateChanged.getGameState() == GameState.LOGIN_SCREEN){
            cleanPanel();
        }
    }

    private void cleanPanel(){
        userPanel.removeAll();
        JLabel info = new JLabel(errorLabel("Please log in to validate your cc"));
        userPanel.add(info);
        revalidate();
        repaint();
    }

    @Subscribe
    public void onClanChannelChanged(ClanChannelChanged clanChannelChanged)
    {
        if (clanChannelChanged.getClanChannel() != null) {
            var clanName = clanChannelChanged.getClanChannel().getName();
            if (clanName.equals("One Shot") && !clanChannelChanged.isGuest()) {
                var playerName = client.getLocalPlayer().getName();
                var clanSettings = client.getClanSettings();
                assert clanSettings != null;
                var clanRank = Objects.requireNonNull(clanSettings.findMember(playerName)).getRank();
                var clanTitle = Objects.requireNonNull(clanSettings.titleForRank(clanRank)).getName();
                userPanel.removeAll();
                userPanel.add(buildRank(Icons.HELM, playerName + " | " + clanTitle));
                revalidate();
                repaint();
            }
        }
        else{
            cleanPanel();
        }
    } // TODO: Logic to enable/disable PANEL + check if user is in guest?

    private static String htmlLabel(String key, String value)
    {
        return "<html><body style = 'color:#a5a5a5'>" + key + "<span style = 'color:white'>" + value + "</span></body></html>";
    }

    private static String errorLabel(String value)
    {
        return "<html><body style = 'color:#ff0000'>" + value + "</body></html>";
    }
}