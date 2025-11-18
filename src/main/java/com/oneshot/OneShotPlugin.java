package com.oneshot;


import com.oneshot.modules.HCIMScoutOverlay;
import com.oneshot.utils.Constants;
import com.oneshot.utils.Icons;
import com.oneshot.modules.HCIMScout;

import lombok.extern.slf4j.Slf4j;

import com.google.inject.Provides;

import javax.inject.Inject;
import javax.swing.*;

import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.clan.ClanRank;
import net.runelite.api.clan.ClanSettings;
import net.runelite.api.clan.ClanTitle;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.ClanChannelChanged;

import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ChatIconManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.overlay.OverlayManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//@Slf4j
@PluginDescriptor(
	name = Constants.PLUGIN_NAME
)

public class OneShotPlugin extends Plugin
{
    private static final Logger log = LoggerFactory.getLogger(OneShotPlugin.class);

    @Inject
    private HCIMScout hcimScout;

    @Inject
	private Client client;

    @Inject
    private OneShotConfig config;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private ChatIconManager chatIconManager;

    private NavigationButton navButton;
    private OneShotPanel panel;

    @Provides
    OneShotConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(OneShotConfig.class);
    }

	@Override
	protected void startUp() throws Exception
	{
        log.debug("Startup");
        hcimScout.init();
        panel = injector.getInstance(OneShotPanel.class);
        panel.init();

        navButton = NavigationButton.builder()
                .tooltip(Constants.PLUGIN_NAME)
                .icon(Icons.RED_HELM_IMAGE)
                .priority(Constants.DEFAULT_PRIORITY)
                .panel(panel)
                .build();

        clientToolbar.addNavigation(navButton);

	}

	@Override
	protected void shutDown() throws Exception
	{
        log.debug("Shutdown");
        hcimScout.deinit();
        panel.deinit();
        clientToolbar.removeNavigation(navButton);
        panel = null;
        navButton = null;

        hcimScout.clearCache();
	}

    @Subscribe
    public void onClanChannelChanged(ClanChannelChanged clanChannelChanged)
    {
        if (clanChannelChanged.getClanChannel() != null) {
            panel.buildIntroPanel();
            String clanName = clanChannelChanged.getClanChannel().getName();
            if (isOneShotMember(clanName) && !clanChannelChanged.isGuest()) { //clan member
                String playerName = client.getLocalPlayer().getName();
                ClanSettings clanSettings = client.getClanSettings();
                assert clanSettings != null;
                ClanRank clanRank = Objects.requireNonNull(clanSettings.findMember(playerName)).getRank();
                ClanTitle clanTitle = Objects.requireNonNull(clanSettings.titleForRank(clanRank));
                log.debug("onClanChannelChanged: " + clanName + " | " + clanRank + " | " + clanTitle);
                panel.buildMainPanel(
                        isModerator(clanRank),
                        playerName,
                        clanTitle.getName(),
                        getRankIcon(clanTitle)
                );
            } else if (isOneShotMember(clanName)) { // guest
                panel.changeIntroText1("You are currently a guest of One Shot", Color.RED);
                panel.changeIntroText2("Please apply to enter", Color.RED);
            } else { //not in clan chat
                panel.changeIntroText1("You are not part of One Shot CC", Color.RED);
                panel.changeIntroText2("You don't have access", Color.RED);
            }
        }
        else
        {
            panel.buildIntroPanel();
        }
    }


    @Subscribe
    public void onVarbitChanged(VarbitChanged varbitChanged)
    {
        if (varbitChanged.getVarbitId() == VarbitID.IRONMAN_HARDCORE_DEAD) {
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "One Shot", "You Died!!", null);
            // TODO: Add screenshot to discord #deaths and verify that no false positives are sent
        }
    }

    @Subscribe
    public void onGameTick(final GameTick event)
    {
        hcimScout.gameTick();
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged)
    {
        if (gameStateChanged.getGameState() == GameState.LOGIN_SCREEN){
            panel.buildIntroPanel();
        }
    }

    private ImageIcon getRankIcon(ClanTitle clanTitle)
    {
        BufferedImage chatIcon = chatIconManager.getRankImage(clanTitle);
        assert chatIcon != null;
        return new ImageIcon(chatIcon.getScaledInstance(Constants.TEXT_ICON_SIZE, Constants.TEXT_ICON_SIZE, Image.SCALE_DEFAULT));
    }

    private static boolean isModerator(ClanRank clanRank)
    {
        return Arrays.asList(Constants.RANK_OWNER, Constants.RANK_DEPUTY_OWNER, Constants.RANK_ASTRAL,
                Constants.RANK_CAPTAIN, Constants.RANK_LIEUTENANT).contains(clanRank);
    }

    private static boolean isOneShotMember(String clanName)
    {
        return Objects.equals(clanName, Constants.CLAN_NAME);
    }

}
