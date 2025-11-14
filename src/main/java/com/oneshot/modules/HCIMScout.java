package com.oneshot.modules;

import com.oneshot.OneShotConfig;

import com.google.common.annotations.VisibleForTesting;

import net.runelite.api.Client;
import net.runelite.api.IndexedObjectSet;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.Renderable;

import net.runelite.client.callback.Hooks;
import net.runelite.client.callback.RenderCallbackManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.hiscore.HiscoreClient;
import net.runelite.client.hiscore.HiscoreEndpoint;
import net.runelite.client.hiscore.HiscoreResult;

import java.awt.*;
import java.io.IOException;
import java.time.Instant;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import javax.inject.Inject;
import javax.inject.Singleton;

import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class HCIMScout extends Plugin {

    @Inject
    private Client client;

    @Inject
    private HiscoreClient hiscoreClient;

    @Inject
    private ConfigManager configManager;

    @Inject
    private Hooks hooks;

    @Inject
    private OverlayManager overlayManager;

//    private HCIMScoutOverlay hcimScoutOverlay;
//    private TestOverlay testOverlay;

    private static final Logger log = LoggerFactory.getLogger(HCIMScout.class);

    private final OneShotConfig config;
    private final Map<String, cachedLookup> lookupCache = new ConcurrentHashMap<>();
    private final Queue<String> lookupQueue = new ConcurrentLinkedQueue<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Hooks.RenderableDrawListener drawListener = this::shouldDrawPlayer;
    private final ArrayList<String> hidePlayers = new ArrayList<String>(0);
    private final ArrayList<String> showPlayers = new ArrayList<String>(0);

    private static final int ACTOR_OVERHEAD_TEXT_MARGIN = 25;
    private static final int ACTOR_HORIZONTAL_TEXT_MARGIN = 10;

    private int lookupCounter;


    @Inject
    private HCIMScout(OneShotConfig config){
        this.config = config;
        this.lookupCounter = config.lookupCooldown() - 1;
    }

    public void init()
    {
        hooks.registerRenderableDrawListener(drawListener);
        //overlayManager.add(testOverlay);
        //overlayManager.add(hcimScoutOverlay);
    }

    public void deinit()
    {
        hooks.unregisterRenderableDrawListener(drawListener);
        //overlayManager.remove(testOverlay);
        //overlayManager.remove(hcimScoutOverlay);
    }

    private static class cachedLookup
    {
        final Map<HiscoreEndpoint, Integer> map;
        final Instant fetchedAt;

        cachedLookup(Map<HiscoreEndpoint, Integer> map, Instant fetchedAt)
        {
            this.map = map;
            this.fetchedAt = fetchedAt;
        }
    }

    public void gameTick()
    {
        this.processScout();
        this.findNearbyPlayers();
    }

    public void clearCache()
    {
        //lookupCache.clear();
        lookupQueue.clear();
    }

    private Map<HiscoreEndpoint, Integer> fetchPlayerStatus(String playerName)
    {
        log.debug("Fetching player {} hiscores", playerName);
        Map<HiscoreEndpoint, Integer> results = new HashMap<>();

        try {
            HiscoreResult result = hiscoreClient.lookup(playerName, HiscoreEndpoint.HARDCORE_IRONMAN);
            HiscoreResult check = hiscoreClient.lookup(playerName, HiscoreEndpoint.NORMAL);

            if (result == null) {
                log.debug("User {} is NOT HCIM!", playerName);
                if (!isPlayerIgnored(playerName))
                {
                    addPlayerIgnore(playerName);
                }
                log.debug("Number of players in queue: {}", lookupQueue.size());
                return results;
            }

            int hcimTotal = result.getSkill(HiscoreSkill.OVERALL).getLevel();
            int normalTotal = check.getSkill(HiscoreSkill.OVERALL).getLevel();
            if (hcimTotal != normalTotal || hcimTotal == 0)
            {
                log.debug("User {} is NOT HCIM! {}|{}", playerName,hcimTotal, normalTotal);
                if (!isPlayerIgnored(playerName))
                {
                    addPlayerIgnore(playerName);
                }
            }
            else{
                log.debug("User {} is HCIM! {}|{}", playerName,hcimTotal, normalTotal);
                if (!isPlayerShowed(playerName))
                {
                    addPlayerHCIM(playerName);
                }
            }
        }
        catch (IOException e)
        {
            log.warn("Failed to fetch {} hiscores", playerName);
        }
        log.debug("Number of players in queue: {}", lookupQueue.size());
        return results;
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

        String playerName = lookupQueue.poll();
        Map<HiscoreEndpoint, Integer> data = lookupCache.get(playerName) != null ? lookupCache.get(playerName).map : null;
        // Update the cache with current time so we don't re-add players that we are currently fetching
        lookupCache.put(playerName, new cachedLookup(data, Instant.now()));

        executor.submit(() ->
        {
            Map<HiscoreEndpoint, Integer> map = fetchPlayerStatus(playerName);
            // If the lookup failed, use the previous data and set the fetchedAt to 2 minutes less than our cache duration
            if (map.isEmpty()) {
                lookupCache.put(playerName, new cachedLookup(data, Instant.now().minus(Duration.ofMinutes((long)config.cacheDuration() - 2))));
            } else {
                lookupCache.put(playerName, new cachedLookup(map, Instant.now()));
            }
        });

    }

    private void findNearbyPlayers(){
        IndexedObjectSet<? extends Player> players = client.getWorldView(-1).players();
        for (Player player : players)
        {
            String playerName = player.getName();
            cachedLookup cached = lookupCache.get(playerName);
            if (!lookupQueue.contains(playerName) && cached == null)
            {
                lookupQueue.offer(playerName);
                log.debug("Added {} to lookup queue", playerName);
            }
        }
    }

    @VisibleForTesting
    boolean shouldDrawPlayer(Renderable renderable, boolean drawingUI){
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

            if (isPlayerIgnored(player.getName()) && config.hcimscoutHide())
            {
                return false;
            }
        }
        return true;
    }

    private void addPlayerIgnore(String playerName)
    {
        hidePlayers.add(playerName);
    }

    boolean isPlayerIgnored(String playerName)
    {
        return hidePlayers.contains(playerName);
    }

    private void addPlayerHCIM(String playerName)
    {
        showPlayers.add(playerName);
    }

    boolean isPlayerShowed(String playerName)
    {
        return showPlayers.contains(playerName);
    }

    void forEachPlayer(final BiConsumer<Player, Color> consumer)
    {
        for (Player player : client.getPlayers())
        {
            if (player == null || player.getName() == null)
            {
                continue;
            }
            if (isPlayerShowed(player.getName()))
            {
                consumer.accept(player, Color.RED);
            }
        }
    }
}
