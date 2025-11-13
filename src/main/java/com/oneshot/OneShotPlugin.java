package com.oneshot;


import com.oneshot.utils.Constants;
import com.oneshot.utils.Icons;

import lombok.extern.slf4j.Slf4j;

import com.google.inject.Provides;

import javax.inject.Inject;

import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ClanChannelChanged;

import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import java.util.Objects;

import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.ClientToolbar;

@Slf4j
@PluginDescriptor(
	name = "One Shot"
)

public class OneShotPlugin extends Plugin
{
    @Inject
	private Client client;

	@Inject
	private OneShotConfig config;

    //private fetchRecords

    @Inject
    private ClientToolbar clientToolbar;

    private NavigationButton navButton;
    private OneShotPanel panel;

	@Override
	protected void startUp() throws Exception
	{
        log.debug("Startup");
        panel = injector.getInstance(OneShotPanel.class);
        panel.init();

        navButton = NavigationButton.builder()
                .tooltip(Constants.PLUGIN_NAME)
                .icon(Icons.HELM)
                .priority(Constants.DEFAULT_PRIORITY)
                .panel(panel)
                .build();

        clientToolbar.addNavigation(navButton);
	} // TODO: INITIALIZE PANEL and BUILD PANEL



	@Override
	protected void shutDown() throws Exception
	{
        log.debug("Shutdown");
        panel.deinit();
        clientToolbar.removeNavigation(navButton);
        panel = null;
        navButton = null;
	}

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged)
    {

        if (client.getGameState() == GameState.LOGGED_IN){
            Player local = client.getLocalPlayer();
            if (local != null) {
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",local.getName(), null);
                log.debug("gameState LOGGEDIN: " + local.getName());
            }
        } // TODO: save name to variable
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
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", clanName, null);
                log.debug("onClanChannelChanged: " + clanName + " | " + clanTitle);
            }
        }
    } // TODO: Logic to enable/disable PANEL + check if user is in guest?

    @Subscribe
    public void onVarbitChanged(VarbitChanged varbitChanged)
    {
        if (varbitChanged.getVarbitId() == VarbitID.IRONMAN_HARDCORE_DEAD) {
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "[ONESHOT] You Died!!", null);
            // TODO: Add screenshot to discord #deaths and verify that no false positives are sent
        }
    }


	@Provides
    OneShotConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(OneShotConfig.class);
	}
}
