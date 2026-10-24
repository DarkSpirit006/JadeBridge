package com.darkspirit69.jadebridge.update;

import com.darkspirit69.jadebridge.JadeBridge;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Fetches stable releases from the JadeBridge Modrinth project. */
public final class ModrinthReleaseClient {

    private static final String VERSIONS_URL = "https://api.modrinth.com/v2/project/jadebridge/version";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

    private final JadeBridge plugin;
    private final VersionComparator versionComparator;
    private final HttpClient httpClient;

    public ModrinthReleaseClient(JadeBridge plugin, VersionComparator versionComparator) {
        this.plugin = plugin;
        this.versionComparator = versionComparator;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public UpdateResult fetchLatestRelease() {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(VERSIONS_URL))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Accept", "application/json")
                    .header("User-Agent", "JadeBridge/" + plugin.getPluginMeta().getVersion())
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return UpdateResult.empty();
            }
            return parseVersions(JsonParser.parseString(response.body()).getAsJsonArray());
        } catch (IOException e) {
            plugin.getSLF4JLogger().warn("Could not check Modrinth for JadeBridge updates: {}", e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            plugin.getSLF4JLogger().warn("Modrinth update check was interrupted");
        } catch (JsonParseException | IllegalStateException e) {
            plugin.getSLF4JLogger().warn("Modrinth returned an unexpected update response: {}", e.getMessage());
        }
        return UpdateResult.empty();
    }

    private UpdateResult parseVersions(JsonArray versions) {
        List<String> releases = new ArrayList<>();
        String latest = null;
        for (JsonElement element : versions) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject version = element.getAsJsonObject();
            if (!"release".equals(versionString(version, "version_type"))) {
                continue;
            }
            String normalized = versionComparator.normalize(versionString(version, "version_number"));
            if (normalized == null || releases.contains(normalized)) {
                continue;
            }
            releases.add(normalized);
            if (latest == null || versionComparator.isNewer(normalized, latest)) {
                latest = normalized;
            }
        }
        return latest == null ? UpdateResult.empty() : new UpdateResult(latest, List.copyOf(releases));
    }

    private String versionString(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? null : element.getAsString();
    }
}
