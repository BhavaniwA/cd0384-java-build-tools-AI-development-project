package com.cosmochain.support;

import com.cosmochain.LaunchDetailsFormatter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Notifier implementation that prints launch updates to the console.
 */
public class ConsoleNotifier implements Notifier {
    /**
     * Prints a formatted notification message with timestamp, provider, and launch details.
     *
     * @param provider provider name.
     * @param changeType change type such as NEW or DATE_CHANGED.
     * @param launch launch payload used in the message.
     */
    @Override
    public void notify(String provider, String changeType, Launch launch) {
        String msg = String.format(
            "[%s] [%s] %s%n%s",
            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            provider,
            changeType,
            LaunchDetailsFormatter.formatLaunchDetails(provider, launch)
        );
        System.out.println(msg);
    }
}
