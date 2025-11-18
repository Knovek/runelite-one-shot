package com.oneshot.modules;

import com.oneshot.OneShotConfig;

import com.oneshot.utils.Icons;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;


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
        // stop if disabled
        if (!config.hcimscoutRedText() && !config.hcimscoutRedHelm()){ return; }

        // construct text
        String text = config.hcimscoutRedText() ? "HCIM: " + player.getName() : "";

        final int zOffset = player.getLogicalHeight() + ACTOR_OVERHEAD_TEXT_MARGIN;

        Point textLocation = player.getCanvasTextLocation(graphics, text, zOffset);

        if (textLocation == null)
        {
            return;
        }

        // construct icon
        if (config.hcimscoutRedHelm())
        {
            final int textHeight = graphics.getFontMetrics().getHeight() - graphics.getFontMetrics().getMaxDescent();
            final int imageMargin = 5;
            Image icon = Icons.RED_HELM_IMAGE;

            icon = icon.getScaledInstance(textHeight, textHeight, Image.SCALE_SMOOTH);
            textLocation = new Point(textLocation.getX() + (imageMargin + icon.getWidth(null)) / 2, textLocation.getY());

            Point iconLoc = new Point(
                    textLocation.getX() - imageMargin - icon.getWidth(null),
                    textLocation.getY() - textHeight / 2 - icon.getHeight(null) / 2
            );
            graphics.drawImage(icon, iconLoc.getX(), iconLoc.getY(), null);
        }


        // render text
        OverlayUtil.renderTextLocation(graphics, textLocation, text, color);
    }


}