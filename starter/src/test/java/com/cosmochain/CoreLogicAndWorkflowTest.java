package com.cosmochain;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.cosmochain.support.Launch;
import com.cosmochain.support.LaunchRepository;
import com.cosmochain.support.Notifier;
import com.cosmochain.support.RateLimitException;
import com.cosmochain.support.UpcomingLaunchClient;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CoreLogicAndWorkflowTest {

    @Mock
    private UpcomingLaunchClient client;

    @Mock
    private UpcomingLaunchClient failingProvider;

    @Mock
    private UpcomingLaunchClient healthyProvider;

    @Mock
    private LaunchRepository repo;

    @Mock
    private Notifier notifier;

    @Test
    @DisplayName("First seen launch is persisted and notified as NEW")
    void firstSeenLaunchPersistsAndNotifiesNew() throws Exception {
        Launch next = launch("id-1", "2026-04-01T12:00:00Z");

        when(client.getProviderName()).thenReturn("ProviderA");
        when(client.fetchNextLaunch()).thenReturn(next);
        when(repo.getLastSeen("ProviderA")).thenReturn(null);

        new LaunchUpdateService(List.of(client), repo, notifier)
                .checkForUpdate(client);

        InOrder order = inOrder(repo, notifier);
        order.verify(repo).setLastSeen("ProviderA", next);
        order.verify(notifier).notify("ProviderA", "NEW", next);
    }

    @Test
    @DisplayName("Changed launch date is notified as DATE_CHANGED and persisted")
    void dateChangeNotifiesDateChangedAndPersists() throws Exception {
        Launch last = launch("id-1", "2026-04-01T12:00:00Z");
        Launch next = launch("id-1", "2026-04-01T15:00:00Z");

        when(client.getProviderName()).thenReturn("ProviderA");
        when(client.fetchNextLaunch()).thenReturn(next);
        when(repo.getLastSeen("ProviderA")).thenReturn(last);

        new LaunchUpdateService(List.of(client), repo, notifier)
                .checkForUpdate(client);

        InOrder order = inOrder(repo, notifier);
        order.verify(repo).setLastSeen("ProviderA", next);
        order.verify(notifier).notify("ProviderA", "DATE_CHANGED", next);
    }

    @Test
    @DisplayName("Different launch ID is notified as NEW and persisted")
    void differentLaunchIdNotifiesNewAndPersists() throws Exception {
        Launch last = launch("id-1", "2026-04-01T12:00:00Z");
        Launch next = launch("id-2", "2026-04-01T12:00:00Z");

        when(client.getProviderName()).thenReturn("ProviderA");
        when(client.fetchNextLaunch()).thenReturn(next);
        when(repo.getLastSeen("ProviderA")).thenReturn(last);

        new LaunchUpdateService(List.of(client), repo, notifier)
                .checkForUpdate(client);

        InOrder order = inOrder(repo, notifier);
        order.verify(repo).setLastSeen("ProviderA", next);
        order.verify(notifier).notify("ProviderA", "NEW", next);
    }

    @Test
    @DisplayName("Unchanged launch causes no notification or repository write")
    void unchangedLaunchDoesNotNotifyOrWrite() throws Exception {
        Launch unchanged = launch("id-1", "2026-04-01T12:00:00Z");

        when(client.getProviderName()).thenReturn("ProviderA");
        when(client.fetchNextLaunch()).thenReturn(unchanged);
        when(repo.getLastSeen("ProviderA")).thenReturn(unchanged);

        new LaunchUpdateService(List.of(client), repo, notifier)
                .checkForUpdate(client);

        verify(repo).getLastSeen("ProviderA");
        verifyNoInteractions(notifier);
        verifyNoMoreInteractions(repo);
    }

    @Test
    @DisplayName("Rate limit exception causes no repository write or notification")
    void rateLimitExceptionProducesNoRepoWriteOrNotify() throws Exception {
        RateLimitException exception = mock(RateLimitException.class);

        when(client.getProviderName()).thenReturn("ProviderA");
        when(client.fetchNextLaunch()).thenThrow(exception);

        new LaunchUpdateService(List.of(client), repo, notifier)
                .checkForUpdate(client);

        verify(client).fetchNextLaunch();
        verifyNoInteractions(repo, notifier);
    }

    @Test
    @DisplayName("Empty upcoming list has no side effects")
    void emptyUpcomingListThroughDefaultFetchNextLaunchPathHasNoSideEffects() {
        UpcomingLaunchClient emptyProvider = new UpcomingLaunchClient() {
            @Override
            public List<Launch> fetchUpcomingLaunches(int limit) {
                return List.of();
            }

            @Override
            public String getProviderName() {
                return "EmptyProvider";
            }
        };

        new LaunchUpdateService(List.of(emptyProvider), repo, notifier)
                .checkForUpdate(emptyProvider);

        verifyNoInteractions(repo, notifier);
    }

    @Test
    @DisplayName("Provider exception is isolated without side effects")
    void providerExceptionDuringFetchIsIsolatedWithoutSideEffects() throws Exception {
        when(client.getProviderName()).thenReturn("BrokenProvider");
        when(client.fetchNextLaunch()).thenThrow(new IOException("Provider failed"));

        new LaunchUpdateService(List.of(client), repo, notifier)
                .checkForUpdate(client);

        verify(client).fetchNextLaunch();
        verifyNoInteractions(repo, notifier);
    }

    @Test
    @DisplayName("Workflow continues when one provider fails")
    void workflowContinuesWhenOneProviderFails() throws Exception {
        Launch next = launch("ok-1", "2026-04-20T00:00:00Z");

        when(failingProvider.getProviderName()).thenReturn("BrokenProvider");
        when(failingProvider.fetchNextLaunch())
                .thenThrow(new IOException("Provider failed"));

        when(healthyProvider.getProviderName()).thenReturn("HealthyProvider");
        when(healthyProvider.fetchNextLaunch()).thenReturn(next);
        when(repo.getLastSeen("HealthyProvider")).thenReturn(null);

        new LaunchUpdateService(
                List.of(failingProvider, healthyProvider),
                repo,
                notifier
        ).checkForUpdatesAcrossProviders();

        verify(failingProvider).fetchNextLaunch();
        verify(healthyProvider).fetchNextLaunch();

        verify(repo, never()).getLastSeen("BrokenProvider");
        verify(repo).getLastSeen("HealthyProvider");
        verify(repo).setLastSeen("HealthyProvider", next);

        verify(notifier, never())
                .notify(eq("BrokenProvider"), any(), any());

        verify(notifier)
                .notify("HealthyProvider", "NEW", next);
    }

    @Test
    @DisplayName("RateLimitException stores retry delay")
    void rateLimitExceptionStoresRetryDelay() {
        RateLimitException exception =
                new RateLimitException("Too many requests", 30);

        assertTrue(exception.getMessage().contains("Too many requests"));
        assertTrue(exception.getRetryAfterSeconds() == 30);
    }

    private static Launch launch(String id, String dateUtc) {
        return new Launch(id, "Mission", dateUtc, "7", "details");
}
}
