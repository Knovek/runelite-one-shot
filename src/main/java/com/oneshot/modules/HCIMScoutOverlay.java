package com.oneshot.modules;

import com.oneshot.OneShotConfig;

import net.runelite.api.Client;
import net.runelite.api.IndexedObjectSet;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.inject.Inject;
import java.awt.*;


public class HCIMScoutOverlay extends Overlay {

    private final HCIMScout hcimScout;
    private final OneShotConfig config;

    private static final int ACTOR_OVERHEAD_TEXT_MARGIN = 25;
    private static final int ACTOR_HORIZONTAL_TEXT_MARGIN = 10;

    @Inject
    private Client client;

    @Inject
    HCIMScoutOverlay(OneShotConfig config, HCIMScout hcimScout)
    {
        this.config = config;
        this.hcimScout = hcimScout;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        hcimScout.forEachPlayer((player, color) -> RenderPlayerOverlay(graphics, player, color));
        return null;
    }

    private void RenderPlayerOverlay(Graphics2D graphics, Player player, Color color)
    {
        String text = "HCIM: " + player.getName();

        final int zOffset = player.getLogicalHeight() + ACTOR_OVERHEAD_TEXT_MARGIN;

        Point textLocation = player.getCanvasTextLocation(graphics, text, zOffset);

        if (textLocation == null)
        {
            return;
        }

        OverlayUtil.renderTextLocation(graphics, textLocation, text, color);
    }


}
