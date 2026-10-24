package com.darkspirit69.jadebridge.update;

import java.util.List;

/** Immutable result of a release lookup. */
public record UpdateResult(String latestVersion, List<String> releaseVersions) {

    public static UpdateResult empty() {
        return new UpdateResult(null, List.of());
    }

    public boolean hasRelease() {
        return latestVersion != null;
    }
}
