package com.cosmochain.support;

/**
 * Notification contract for launch update events.
 */
public interface Notifier {
    /**
     * Sends a launch update notification.
     *
     * @param provider provider name.
     * @param changeType change classification.
     * @param launch launch payload for the notification.
     */
    void notify(String provider, String changeType, Launch launch);
}
