package com.oneshot.modules;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.oneshot.OneShotPlugin;
import okhttp3.*;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * This thread-safe singleton holds a mapping of item names to their item id, using the RuneLite API.
 * <p>
 * Unlike {@link net.runelite.client.game.ItemManager#search(String)}, this mapping supports untradable items.
 */
@Singleton
public class ItemSearcher {
    private static final Logger log = LoggerFactory.getLogger(OneShotPlugin.class);

    private final Map<String, Integer> itemIdByName = Collections.synchronizedMap(new HashMap<>(16384));
    private @Inject OkHttpClient httpClient;
    private @Inject Gson gson;

    /**
     * @param name the exact in-game name of an item
     * @return the id associated with the item name, or null if not found
     */
    @Nullable
    public Integer findItemId(String name) {
        return itemIdByName.get(name);
    }

    /**
     * Begins the initialization process for {@link #itemIdByName}
     * by querying item names and noted item ids from the RuneLite API,
     * before passing them to {@link #populate(Map, Set)}
     *
     * @implNote This operation does not block the current thread,
     * by utilizing OkHttp's thread pool and Java's Fork-Join common pool.
     */
    @Inject
    void init() {
        queryNamesById()
                .thenAcceptBothAsync(
                        queryNotedItemIds().exceptionally(e -> {
                            log.error("Failed to read noted items", e);
                            return Collections.emptySet();
                        }),
                        this::populate
                )
                .exceptionally(e -> {
                    log.error("Failed to read item names", e);
                    return null;
                });
    }

    /**
     * Populates {@link #itemIdByName} with the inverted mappings of {@code namesById},
     * while skipping noted items specified in {@code notedIds}.
     *
     * @param namesById a mapping of item id's to the corresponding in-game name
     * @param notedIds  the id's of noted items
     * @implNote When multiple non-noted item id's have the same in-game name, only the earliest id is saved
     */

    void populate(Map<Integer, String> namesById, Set<Integer> notedIds) {
        namesById.forEach((id, name) -> {
            if (!notedIds.contains(id))
                itemIdByName.putIfAbsent(name, id);
        });

        log.debug("Completed initialization of item cache with {} entries", itemIdByName.size());
    }

    /**
     * @return a mapping of item ids to their in-game names, provided by the RuneLite API
     */
    private CompletableFuture<Map<Integer, String>> queryNamesById() {
        return queryCache("names.json", new TypeToken<Map<Integer, String>>() {});
    }

    /**
     * @return a set of id's of noted items, provided by the RuneLite API
     */
    private CompletableFuture<Set<Integer>> queryNotedItemIds() {
        return queryCache("notes.json", new TypeToken<Map<Integer, Integer>>() {})
                .thenApply(Map::keySet);
    }

    /**
     * @param fileName the name of the file to query from RuneLite's cache
     * @param type     a type token that indicates how the json response should be parsed
     * @return the transformed cache response, wrapped in a future
     */
    private <T> CompletableFuture<T> queryCache(String fileName, TypeToken<T> type) {
        return readJson(httpClient, gson, "https://static.runelite.net/cache/item/" + fileName, type);
    }

    public <T> CompletableFuture<T> readJson(OkHttpClient httpClient, Gson gson, String url, TypeToken<T> type) {
        return readUrl(httpClient, url, reader -> gson.fromJson(reader, type.getType()));
    }

    public <T> CompletableFuture<T> readUrl( OkHttpClient httpClient, String url, Function<Reader, T> transformer) {
        CompletableFuture<T> future = new CompletableFuture<>();
        Request request = new Request.Builder().url(url).build();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(Call call, Response response) {
                assert response.body() != null;
                try (Reader reader = response.body().charStream()) {
                    future.complete(transformer.apply(reader));
                } catch (Exception e) {
                    future.completeExceptionally(e);
                } finally {
                    response.close();
                }
            }
        });
        return future;
    }
}
