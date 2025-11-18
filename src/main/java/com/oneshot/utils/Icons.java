package com.oneshot.utils;

import net.runelite.client.util.ImageUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Icons {
    public static final BufferedImage RED_HELM_IMAGE = ImageUtil.loadImageResource(Icons.class, "/redHelm.png");
    private static final BufferedImage DISCORD_IMAGE = ImageUtil.loadImageResource(Icons.class, "/iconDiscord.png");
    private static final BufferedImage EVENT_IMAGE = ImageUtil.loadImageResource(Icons.class, "/iconEvent.png");
    private static final BufferedImage SCOUT_IMAGE = ImageUtil.loadImageResource(Icons.class, "/iconScout.png");
    private static final BufferedImage INFO_IMAGE = ImageUtil.loadImageResource(Icons.class, "/iconInfo.png");

    public static final ImageIcon RED_HELM = new ImageIcon(RED_HELM_IMAGE);
    public static final ImageIcon DISCORD = new ImageIcon(DISCORD_IMAGE.getScaledInstance(Constants.BUTTON_SIZE -2, Constants.BUTTON_SIZE -2, Image.SCALE_DEFAULT));
    public static final ImageIcon EVENT = new ImageIcon(EVENT_IMAGE.getScaledInstance(Constants.BUTTON_SIZE -2, Constants.BUTTON_SIZE -2, Image.SCALE_DEFAULT));
    public static final ImageIcon SCOUT = new ImageIcon(SCOUT_IMAGE.getScaledInstance(Constants.BUTTON_SIZE -2, Constants.BUTTON_SIZE -2, Image.SCALE_DEFAULT));
    public static final ImageIcon INFO = new ImageIcon(INFO_IMAGE.getScaledInstance(Constants.BUTTON_SIZE -2, Constants.BUTTON_SIZE -2, Image.SCALE_DEFAULT));

}