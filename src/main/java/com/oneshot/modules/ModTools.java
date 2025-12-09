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

    @Inject
    private Client client;

    @Inject
    private HiscoreClient hiscoreClient;

    @Inject
    private Hooks hooks;

    @Inject
    private ModToolsOverlay modToolsOverlay;

    @Inject
    private OverlayManager overlayManager;

    private ModToolsPanel modToolsPanel;

    @Inject
    private OneShotConfig config;

    private static final Logger log = LoggerFactory.getLogger(ModTools.class);
    private final Map<String, cachedLookup> lookupCache = new ConcurrentHashMap<>();
    private final Queue<String> lookupQueue = new ConcurrentLinkedQueue<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Hooks.RenderableDrawListener drawListener = this::shouldDrawPlayer;
    private final ArrayList<String> nonhcimPlayers = new ArrayList<String>(0);
    private Map<String, Integer> hcimPlayers = new HashMap<>();
    private final Set<String> lookupInProgress = ConcurrentHashMap.newKeySet();


    private int totalChecked = 0;
    private int lookupCounter;

    // TODO: add HCIM LIST to the panel with total level visible
    // TODO: check if HCIM belongs to a clan chat (WOM Integration)
    // TODO: prevent client player and clan members from being looked up in hiscores



    @Override
    protected void startUp()
    {
        lookupCounter = config.lookupCooldown() - 1;

        hooks.registerRenderableDrawListener(this::shouldDrawPlayer);
        overlayManager.add(modToolsOverlay);

        // 🔥 You can now call your panel:
        modToolsPanel.updateCounts(lookupQueue.size(), totalChecked, hcimPlayers.size());
    }

    @Override
    protected void shutDown()
    {
        hooks.unregisterRenderableDrawListener(this::shouldDrawPlayer);
        overlayManager.remove(modToolsOverlay);
    }


    public void init(ModToolsPanel modToolsPanel)
    {
        this.modToolsPanel = modToolsPanel;
        hooks.registerRenderableDrawListener(drawListener);
        overlayManager.add(modToolsOverlay);
    }

    public void deinit()
    {
        hooks.unregisterRenderableDrawListener(drawListener);
        overlayManager.remove(modToolsOverlay);
        //clearCache();
    }

    private static class cachedLookup
    {
        final HiscoreResult result;
        final Instant fetchedAt;

        cachedLookup(HiscoreResult result, Instant fetchedAt)
        {
            this.result = result;
            this.fetchedAt = fetchedAt;
        }
    }

    public void gameTick()
    {
        if (!config.hcimscoutEnable()) return;
        this.processScout();
        this.findNearbyPlayers();
    }

    public void clearCache()
    {
        lookupQueue.clear();
    }

    private boolean fetchPlayerStatus(String playerName)
    {
        //log.debug("Fetching player {} hiscores", playerName);

        try {
            HiscoreResult hcimHiscoreResult = hiscoreClient.lookup(playerName, HiscoreEndpoint.HARDCORE_IRONMAN);
            HiscoreResult normalHiscoreResult = hiscoreClient.lookup(playerName, HiscoreEndpoint.NORMAL);

            if (hcimHiscoreResult == null) {
                //log.debug("User {} is NOT HCIM!", playerName);
                if (!isPlayerIgnored(playerName))
                {
                    addPlayerIgnore(playerName);
                }
            }
            else {
                int hcimTotal = hcimHiscoreResult.getSkill(HiscoreSkill.OVERALL).getLevel();
                int normalTotal = normalHiscoreResult.getSkill(HiscoreSkill.OVERALL).getLevel();
                if (hcimTotal != normalTotal || hcimTotal == 0)
                {
                    //log.debug("User {} is NOT HCIM! {}|{}", playerName,hcimTotal, normalTotal);
                    if (!isPlayerIgnored(playerName))
                    {
                        addPlayerIgnore(playerName);
                    }
                }
                else{
                    //log.debug("User {} is HCIM! {}|{}", playerName,hcimTotal, normalTotal);
                    if (!isPlayerShowed(playerName))
                    {
                        addPlayerHCIM(playerName, hcimTotal);
                    }
                }
            }
            lookupCache.put(playerName, new cachedLookup(hcimHiscoreResult, Instant.now()));
        }
        catch (IOException e)
        {
            log.warn("Failed to fetch {} hiscores", playerName);
            return false;
        }
        return true;
    }

    public void processScout()
    {
        if (lookupQueue.isEmpty())
        {
            lookupCounter = config.lookupCooldown() - 1;
            return;
        }

        lookupCounter = (lookupCounter + 1) % config.lookupCooldown();

        if (lookupCounter > 0)
        {
            return;
        }

        String playerName = lookupQueue.peek(); // DO NOT remove yet

        if (playerName == null)
        {
            return;
        }

        modToolsPanel.updateCounts(
                lookupQueue.size(),
                totalChecked,
                hcimPlayers.size()
        );

        if (!lookupInProgress.add(playerName))
        {
            // already being processed
            //log.debug("Skipping {} because lookup already running", playerName);
            return;
        }

        //log.debug("Processing player {}", playerName);

        // lookupCache.put(playerName, new cachedLookup(data, Instant.now()));

        executor.submit(() ->
        {
            try
            {
                boolean isDone = fetchPlayerStatus(playerName);

                if (isDone)
                {
                    lookupQueue.poll();
                    modToolsPanel.updateCounts(
                            lookupQueue.size(),
                            totalChecked,
                            hcimPlayers.size()
                    );
                }

            }
            finally
            {
                lookupInProgress.remove(playerName);
            }
        });
    }


    private void findNearbyPlayers(){
        IndexedObjectSet<? extends Player> players = client.getTopLevelWorldView().players();
        for (Player player : players)
        {
            String playerName = player.getName();
            cachedLookup cached = lookupCache.get(playerName);
            if (!lookupQueue.contains(playerName) && cached == null && client.getClanSettings().findMember(playerName) == null)
            {
                lookupQueue.offer(playerName);
                //log.debug("Added {} to lookup queue", playerName);
            }
        }
    }

//    @VisibleForTesting
    boolean shouldDrawPlayer(Renderable renderable, boolean drawingUI){
        if (!config.hcimscoutEnable()) return true;
        if (renderable instanceof Player)
        {
            Player player = (Player) renderable;
            Player local = client.getLocalPlayer();

            if (player.getName() == null)
            {
                return true;
            }

            if (player == local)
            {
                return true;
            }

            if (isPlayerShowed(player.getName()))
            {
                return true;
            }

            return !isPlayerIgnored(player.getName()) || !config.hcimscoutHide();
        }
        return true;
    }

    private void addPlayerIgnore(String playerName)
    {
        nonhcimPlayers.add(playerName);
        totalChecked++;
        modToolsPanel.updateCounts(
                lookupQueue.size(),
                totalChecked,
                hcimPlayers.size()
        );
    }

    boolean isPlayerIgnored(String playerName)
    {
        return nonhcimPlayers.contains(playerName);
    }

    private void addPlayerHCIM(String playerName, int hcimTotal)
    {
        hcimPlayers.put(playerName, hcimTotal);
        modToolsPanel.setList(hcimPlayers);
        totalChecked++;
        modToolsPanel.updateCounts(
                lookupQueue.size(),
                totalChecked,
                hcimPlayers.size()
        );
//        modToolsPanel.addToList(playerName, hcimTotal);
    }

    public static class HcimEntry
    {
        public final String name;
        public final int totalLevel;

        public HcimEntry(String name, int totalLevel)
        {
            this.name = name;
            this.totalLevel = totalLevel;
        }
    }


    boolean isPlayerShowed(String playerName)
    {
        return hcimPlayers.get(playerName) != null;
    }

    @Nullable
    int getHCIMTotal(String playerName)
    {
        return hcimPlayers.get(playerName);
    }


    void forEachPlayer(final BiConsumer<Player, Integer> consumer)
    {
        for (Player player : client.getPlayers())
        {
            if (player == null || player.getName() == null)
            {
                continue;
            }
            if (isPlayerShowed(player.getName()))
            {
                consumer.accept(player, getHCIMTotal(player.getName()));
            }
        }
    }
}
