package com.cosmochain;

import java.util.List;
import java.util.Objects;

import com.cosmochain.support.Launch;
import com.cosmochain.support.LaunchRepository;
import com.cosmochain.support.Notifier;
import com.cosmochain.support.RateLimitException;
import com.cosmochain.support.UpcomingLaunchClient;

public class LaunchUpdateService {
    private final List<UpcomingLaunchClient> providers;
    private final LaunchRepository repo;
    private final Notifier notifier;

    public LaunchUpdateService(
            List<UpcomingLaunchClient> providers,
            LaunchRepository repo,
            Notifier notifier) {
        this.providers = List.copyOf(providers);
        this.repo = repo;
        this.notifier = notifier;
    }

    public void checkForUpdatesAcrossProviders() {
        for (UpcomingLaunchClient provider : providers) {
            checkForUpdate(provider);
        }
    }

    void checkForUpdate(UpcomingLaunchClient client) {
        String provider = resolveProviderName(client);
        if (provider == null) {
            return;
        }

        try {
            Launch next = client.fetchNextLaunch();
            Launch last = repo.getLastSeen(provider);

            if (last == null) {
                repo.setLastSeen(provider, next);
                notifier.notify(provider, "NEW", next);
            } else if (!Objects.equals(last.id, next.id)) {
                repo.setLastSeen(provider, next);
                notifier.notify(provider, "NEW", next);
            } else if (!Objects.equals(last.dateUtc, next.dateUtc)) {
                repo.setLastSeen(provider, next);
                notifier.notify(provider, "DATE_CHANGED", next);
            }

        } catch (RateLimitException e) {
            System.out.printf(
                "Skipping %s: API throttled. Retry in %d seconds.%n",
                provider,
                e.getRetryAfterSeconds()
            );
        } catch (Exception e) {
            System.out.println(
                "Error checking updates for " + provider + ": " + e.getMessage()
            );
        }
    }

    private String resolveProviderName(UpcomingLaunchClient client) {
        try {
            return client.getProviderName();
        } catch (Exception e) {
            System.out.println(
                "Error checking updates for provider metadata: " + e.getMessage()
            );
            return null;
        }
    }
}
