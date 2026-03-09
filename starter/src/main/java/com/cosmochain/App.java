package com.cosmochain;

import java.util.List;
import java.util.Scanner;

import com.cosmochain.support.ConsoleNotifier;
import com.cosmochain.support.HttpGateway;
import com.cosmochain.support.InMemoryLaunchRepository;
import com.cosmochain.support.Launch;
import com.cosmochain.support.LaunchRepository;
import com.cosmochain.support.Notifier;
import com.cosmochain.support.RateLimitException;
import com.cosmochain.support.RealHttpGateway;
import com.cosmochain.support.UpcomingLaunchClient;

public class App {
    private static final String DEFAULT_SPACEX_URL =
        "https://ll.thespacedevs.com/2.2.0/launch/upcoming/?lsp__name=SpaceX&limit=3&ordering=net&format=json";
    private static final String DEFAULT_NASA_URL =
        "https://ll.thespacedevs.com/2.2.0/launch/upcoming/?lsp__name=NASA&limit=3&ordering=net&format=json";

    private final List<UpcomingLaunchClient> providers;
    private final LaunchUpdateService launchService;

    /** Sets up the app pieces. */
    private App(String spacexUrl, String nasaUrl) {
        HttpGateway http = new RealHttpGateway();
        Notifier notifier = new ConsoleNotifier();
        LaunchRepository repo = new InMemoryLaunchRepository();
        this.providers = List.of(
            new ProviderLaunchApiClient("SpaceX", http, spacexUrl),
            new ProviderLaunchApiClient("NASA", http, nasaUrl)
        );
        this.launchService = new LaunchUpdateService(providers, repo, notifier);
    }

    /**
     * JVM entry point.
     *
     * @param args command-line arguments.
     */
    public static void main(String[] args) {
        start();
    }

    /** Starts things. */
    public static void start() {
        System.out.println("CosmoChain Notification Service");
        if (RealHttpGateway.isInsecureSslEnabled()) {
            System.out.println("WARNING: Insecure SSL mode is enabled for local testing.");
        }

        String spacexUrl = System.getenv().getOrDefault("SPACEX_NEXT_URL", DEFAULT_SPACEX_URL);
        String nasaUrl = DEFAULT_NASA_URL;

        App app = new App(spacexUrl, nasaUrl);
        try (Scanner scanner = new Scanner(System.in)) {
            app.run(scanner);
        }
    }

    /** Runs the menu. */
    private void run(Scanner scanner) {
        while (true) {
            System.out.println("\nMenu:");
            System.out.println("1) View next 3 upcoming launches (ALL providers)");
            System.out.println("2) Check for updates (ALL providers)");
            System.out.println("3) Exit");
            System.out.print("Select option: ");

            String input = scanner.nextLine();
            switch (input) {
                case "1":
                    forEachProvider(client -> printUpcomingLaunches(client, 3));
                    break;
                case "2":
                    launchService.checkForUpdatesAcrossProviders();
                    break;
                case "3":
                    System.out.println("Goodbye.");
                    return;
                default:
                    System.out.println("Invalid option. Please try again.");
                    break;
            }
        }
    }

    /** Gets launches and prints them. */
    private void printUpcomingLaunches(UpcomingLaunchClient client, int count) {
        System.out.printf("%s - Next %d Upcoming Launches%n", client.getProviderName(), count);
        try {
            List<Launch> launches = client.fetchUpcomingLaunches(count);
            if (launches.isEmpty()) {
                System.out.println("No upcoming launches found.");
                return;
            }
            for (int i = 0; i < launches.size(); i++) {
                System.out.printf(
                    "Launch %d%n%s%n",
                    i + 1,
                    LaunchDetailsFormatter.formatLaunchDetails(client.getProviderName(), launches.get(i))
                );
                if (i < launches.size() - 1) {
                    System.out.println();
                }
            }
        } catch (RateLimitException e) {
            System.out.printf(
                "Skipping %s: API throttled. Retry in %d seconds.%n",
                client.getProviderName(),
                e.getRetryAfterSeconds()
            );
        } catch (Exception e) {
            System.out.println("Error fetching upcoming launches: " + e.getMessage());
        }
    }

    /** Runs something for each provider. */
    private void forEachProvider(ProviderAction action) {
        for (int i = 0; i < providers.size(); i++) {
            action.run(providers.get(i));
            if (i < providers.size() - 1) {
                System.out.println();
            }
        }
    }

    @FunctionalInterface
    private interface ProviderAction {
        /** Does provider work. */
        void run(UpcomingLaunchClient client);
    }
}
