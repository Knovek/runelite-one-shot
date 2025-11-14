package com.oneshot;


import com.oneshot.modules.HCIMScoutOverlay;
import com.oneshot.utils.Constants;
import com.oneshot.utils.Icons;
import com.oneshot.modules.HCIMScout;

import lombok.extern.slf4j.Slf4j;

import com.google.inject.Provides;

import javax.inject.Inject;

import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.ClanChannelChanged;

import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import java.util.Objects;

import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.overlay.OverlayManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//@Slf4j
@PluginDescriptor(
	name = "One Shot"
)

public class OneShotPlugin extends Plugin
{
    private static final Logger log = LoggerFactory.getLogger(OneShotPlugin.class);

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private HCIMScout hcimScout;

    @Inject
	private Client client;

	@Inject
	private OneShotConfig config;

    @Inject
    private HCIMScoutOverlay hcimScoutOverlay;

    @Inject
    private ClientToolbar clientToolbar;

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
                .icon(Icons.HELM)
                .priority(Constants.DEFAULT_PRIORITY)
                .panel(panel)
                .build();

        clientToolbar.addNavigation(navButton);

        overlayManager.add(hcimScoutOverlay);
	} // TODO: BUILD PANEL

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
        overlayManager.remove(hcimScoutOverlay);
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
                log.debug("onClanChannelChanged: " + clanName + " | " + clanRank + " | " + clanTitle);
            }
        }
    } // TODO: Logic to disable panel if user logs out

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


}
