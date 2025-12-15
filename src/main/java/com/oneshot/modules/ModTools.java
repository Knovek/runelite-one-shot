package com.oneshot.modules;

import com.oneshot.OneShotConfig;

import net.runelite.api.Client;
import net.runelite.api.IndexedObjectSet;
import net.runelite.api.Player;
import net.runelite.api.Renderable;

import net.runelite.client.callback.Hooks;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.hiscore.HiscoreClient;
import net.runelite.client.hiscore.HiscoreEndpoint;
import net.runelite.client.hiscore.HiscoreResult;

import java.awt.*;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

import net.runelite.client.ui.overlay.OverlayManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ModTools extends Plugin {

    private ModToolsPanel modToolsPanel;

    public void init(ModToolsPanel modToolsPanel)
    {
        this.modToolsPanel = modToolsPanel;
    }
}
