package com.cosmochain.support;

/**
 * Storage contract for last-seen launch state per provider.
 */
public interface LaunchRepository {
    /**
     * Returns the last-seen launch for a provider.
     *
     * @param provider provider key.
     * @return last-seen launch or null when none exists.
     */
    Launch getLastSeen(String provider);

    /**
     * Stores the last-seen launch for a provider.
     *
     * @param provider provider key.
     * @param launch launch to persist.
     */
    void setLastSeen(String provider, Launch launch);
}
