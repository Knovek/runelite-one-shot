package com.oneshot.utils;

import net.runelite.client.util.ImageUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Icons {
    public static final BufferedImage RED_HELM_IMAGE = ImageUtil.loadImageResource(Icons.class, "/redHelm.png");

    private static final BufferedImage DISCORD_IMAGE = ImageUtil.loadImageResource(Icons.class, "/iconDiscordON.png");
    private static final BufferedImage RANKING_IMAGE = ImageUtil.loadImageResource(Icons.class, "/iconLeaderboardsON.png");
    private static final BufferedImage MODTOOLS_IMAGE = ImageUtil.loadImageResource(Icons.class, "/iconModToolsON.png");
    private static final BufferedImage INFO_IMAGE = ImageUtil.loadImageResource(Icons.class, "/iconRankingsON.png");

    public static final BufferedImage QUEST_IMAGE = ImageUtil.loadImageResource(Icons.class, "/iconQuestHelper.png");
    public static final BufferedImage DEATH_IMAGE = ImageUtil.loadImageResource(Icons.class, "/iconDamageHitsplat.png");
    public static final BufferedImage LEVEL_IMAGE = ImageUtil.loadImageResource(Icons.class, "/iconStats.png");
    public static final BufferedImage TASKS_IMAGE = ImageUtil.loadImageResource(Icons.class, "/iconTasks.png");

    public static final ImageIcon RED_HELM = new ImageIcon(RED_HELM_IMAGE);
    public static final ImageIcon RED_HELM_SMALLER = new ImageIcon(RED_HELM_IMAGE.getScaledInstance(Constants.BUTTON_SIZE -2, Constants.BUTTON_SIZE -2, Image.SCALE_DEFAULT));
    public static final ImageIcon RED_HELM_TINY = new ImageIcon(RED_HELM_IMAGE.getScaledInstance(24, 24, Image.SCALE_SMOOTH));
    public static final ImageIcon DISCORD = new ImageIcon(DISCORD_IMAGE.getScaledInstance(Constants.BUTTON_SIZE -2, Constants.BUTTON_SIZE -2, Image.SCALE_SMOOTH));
    public static final ImageIcon RANKING = new ImageIcon(RANKING_IMAGE.getScaledInstance(Constants.BUTTON_SIZE -2, Constants.BUTTON_SIZE -2, Image.SCALE_SMOOTH));
    public static final ImageIcon MODTOOLS = new ImageIcon(MODTOOLS_IMAGE.getScaledInstance(Constants.BUTTON_SIZE -2, Constants.BUTTON_SIZE -2, Image.SCALE_SMOOTH));
    public static final ImageIcon INFO = new ImageIcon(INFO_IMAGE.getScaledInstance(Constants.BUTTON_SIZE -2, Constants.BUTTON_SIZE -2, Image.SCALE_SMOOTH));

}