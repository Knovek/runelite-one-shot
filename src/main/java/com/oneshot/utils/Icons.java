package com.oneshot.utils;

import net.runelite.client.util.ImageUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Icons {
    public static final BufferedImage RED_HELM_IMAGE = ImageUtil.loadImageResource(Icons.class, "/redHelm.png");

    public static final BufferedImage WORLD_IMAGE = ImageUtil.loadImageResource(Icons.class, "/World_map_icon.png");

    public static final BufferedImage QUEST_IMAGE = ImageUtil.loadImageResource(Icons.class, "/iconQuestHelper.png");
    public static final BufferedImage DEATH_IMAGE = ImageUtil.loadImageResource(Icons.class, "/iconDamageHitsplat.png");
    public static final BufferedImage LEVEL_IMAGE = ImageUtil.loadImageResource(Icons.class, "/iconStats.png");
    public static final BufferedImage TASKS_IMAGE = ImageUtil.loadImageResource(Icons.class, "/iconTasks.png");

    public static final ImageIcon RED_HELM = new ImageIcon(RED_HELM_IMAGE);
    public static final ImageIcon RED_HELM_SMALLER = new ImageIcon(RED_HELM_IMAGE.getScaledInstance(Constants.BUTTON_SIZE -2, Constants.BUTTON_SIZE -2, Image.SCALE_DEFAULT));
    public static final ImageIcon WORLD = new ImageIcon(WORLD_IMAGE.getScaledInstance(12, 12, Image.SCALE_SMOOTH));

}