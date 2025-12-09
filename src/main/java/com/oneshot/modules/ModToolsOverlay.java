package com.oneshot.modules;

import com.oneshot.OneShotConfig;

import com.oneshot.utils.Icons;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.util.Text;

import javax.inject.Inject;
import java.awt.*;


public class ModToolsOverlay extends Overlay {

    private final ModTools modTools;
    private final OneShotConfig config;
    private static final int OVERHEAD_TEXT_MARGIN = 40;


    @Inject
    ModToolsOverlay(OneShotConfig config, ModTools modTools)
    {
        this.config = config;
        this.modTools = modTools;
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(PRIORITY_MED);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        modTools.forEachPlayer((player, total) -> RenderPlayerOverlay(graphics, player, total));
        return null;
    }

    private void RenderPlayerOverlay(Graphics2D graphics, Player player, Integer total)
    {
        if (!config.hcimscoutEnable() || !config.hcimscoutRedText())
            return;

        final int zOffset = player.getLogicalHeight() + OVERHEAD_TEXT_MARGIN;

        final String name = Text.sanitize(String.format("%s (%d)", player.getName(), total));
        Point textLocation = player.getCanvasTextLocation(graphics, name, zOffset);

        if (textLocation == null)
        {
            return;
        }

        OverlayUtil.renderTextLocation(graphics, textLocation, name, Color.RED);

    }


}