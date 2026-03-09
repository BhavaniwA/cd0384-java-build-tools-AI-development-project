package com.cosmochain.support;

import java.util.HashMap;
import java.util.Map;

/**
 * In-memory implementation of launch state storage keyed by provider name.
 */
public class InMemoryLaunchRepository implements LaunchRepository {
    private final Map<String, Launch> lastSeen = new HashMap<>();

    /**
     * Returns the last-seen launch for a provider.
     *
     * @param provider provider key.
     * @return last-seen launch or null when not found.
     */
    @Override
    public Launch getLastSeen(String provider) {
        return lastSeen.get(provider);
    }

    /**
     * Stores the last-seen launch for a provider.
     *
     * @param provider provider key.
     * @param launch launch value to store.
     */
    @Override
    public void setLastSeen(String provider, Launch launch) {
        lastSeen.put(provider, launch);
    }
}
