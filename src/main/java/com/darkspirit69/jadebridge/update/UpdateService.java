package com.darkspirit69.jadebridge.update;

import com.darkspirit69.jadebridge.JadeBridge;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Coordinates asynchronous update checks and returns results on the main server thread. */
public final class UpdateService {

    private static final String ADMIN_PERMISSION = "jadebridge.admin";
    private static final long UPDATE_CACHE_MILLIS = java.util.concurrent.TimeUnit.HOURS.toMillis(6);
    private static final long EMPTY_UPDATE_CACHE_MILLIS = java.util.concurrent.TimeUnit.MINUTES.toMillis(15);

    private final JadeBridge plugin;
    private final ModrinthReleaseClient client;
    private final VersionComparator versionComparator;
    private CompletableFuture<UpdateResult> inFlightCheck;
    private UpdateResult cachedResult = UpdateResult.empty();
    private long cachedResultAt;

    public UpdateService(JadeBridge plugin) {
        this.plugin = plugin;
        this.versionComparator = new VersionComparator();
        this.client = new ModrinthReleaseClient(plugin, versionComparator);
    }

    public void checkForPlayer(Player player, Consumer<UpdateResult> callback) {
        if (!player.hasPermission(ADMIN_PERMISSION)) {
            return;
        }
        fetchAsync(result -> {
            if (!player.isOnline() || !player.hasPermission(ADMIN_PERMISSION)) {
                return;
            }
            callback.accept(result);
        });
    }

    public void checkForSender(CommandSender sender, Consumer<UpdateResult> callback) {
        fetchAsync(result -> {
            if (sender instanceof Player player && (!player.isOnline() || !player.hasPermission(ADMIN_PERMISSION))) {
                return;
            }
            callback.accept(result);
        });
    }

    public boolean isNewer(String candidate, String current) {
        return versionComparator.isNewer(candidate, current);
    }

    public int countVersionsBehind(UpdateResult result, String current) {
        return versionComparator.countVersionsBehind(result.releaseVersions(), current, result.latestVersion());
    }

    private void fetchAsync(Consumer<UpdateResult> callback) {
        CompletableFuture<UpdateResult> check;
        synchronized (this) {
            long now = System.currentTimeMillis();
            long cacheLifetime = cachedResult.hasRelease() ? UPDATE_CACHE_MILLIS : EMPTY_UPDATE_CACHE_MILLIS;
            if (cachedResultAt > 0 && now - cachedResultAt < cacheLifetime) {
                check = CompletableFuture.completedFuture(cachedResult);
            } else if (inFlightCheck != null && !inFlightCheck.isDone()) {
                check = inFlightCheck;
            } else {
                check = CompletableFuture.supplyAsync(client::fetchLatestRelease);
                inFlightCheck = check;
            }
        }

        check.whenComplete((result, throwable) -> {
            UpdateResult finalResult = result;
            if (throwable != null) {
                finalResult = UpdateResult.empty();
            } else if (result != null) {
                synchronized (UpdateService.this) {
                    cachedResult = result;
                    cachedResultAt = System.currentTimeMillis();
                }
            }

            UpdateResult delivered = finalResult == null ? UpdateResult.empty() : finalResult;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (plugin.isEnabled()) {
                    callback.accept(delivered);
                }
            });
        });
    }
}
