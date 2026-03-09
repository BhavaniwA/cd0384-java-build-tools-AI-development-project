package com.cosmochain;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cosmochain.support.InMemoryLaunchRepository;
import com.cosmochain.support.Launch;
import com.cosmochain.support.Notifier;
import com.cosmochain.support.RateLimitException;
import com.cosmochain.support.RealHttpGateway;
import com.cosmochain.support.UpcomingLaunchClient;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

class ApplicationIntegrationIT {
    private HttpServer server;
    private String baseUrl;

    /**
     * Starts an embedded HTTP server with deterministic test endpoints.
     *
     * @throws IOException when server startup fails.
     */
    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);

        server.createContext("/spacex", exchange ->
            sendJson(
                exchange,
                200,
                "{\"results\":[{\"id\":\"spx-1\",\"name\":\"Falcon Integration\",\"date_utc\":\"2026-03-15T12:00:00Z\",\"flight_number\":99,\"details\":\"SpaceX detail\"}]}"
            )
        );

        server.createContext("/nasa-array", exchange ->
            sendJson(
                exchange,
                200,
                "[{\"id\":\"nasa-1\",\"name\":\"Artemis Integration\",\"window_start\":\"2026-06-01T11:00:00+01:00\",\"mission\":{\"description\":\"Line 1\\nLine 2??   detail\"}}]"
            )
        );

        server.createContext("/throttled", exchange -> {
            exchange.getResponseHeaders().add("Retry-After", "8");
            sendText(exchange, 429, "Expected available in 8 seconds");
        });

        server.createContext("/invalid", exchange ->
            sendJson(
                exchange,
                200,
                "{\"results\":[{\"name\":\"Missing Id\",\"date_utc\":\"2026-06-01T11:00:00Z\"},{\"id\":\"missing-date\",\"name\":\"Missing Date\"}]}"
            )
        );

        server.createContext("/server-error-empty", exchange -> sendNoBody(exchange, 500));

        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
    }

    /**
     * Stops the embedded HTTP server after each test.
     */
    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    /**
     * Verifies parsing of results-wrapper payloads through real HTTP calls.
     *
     * @throws Exception when the integration flow fails.
     */
    @Test
    void parsesLaunchFromResultsPayload() throws Exception {
        ProviderLaunchApiClient client = new ProviderLaunchApiClient("SpaceX", new RealHttpGateway(), baseUrl + "/spacex");
        List<Launch> launches = client.fetchUpcomingLaunches(1);
        Launch launch = launches.isEmpty() ? null : launches.get(0);
        // TODO assert the parsed SpaceX launch fields
    }

    /**
     * Verifies parsing of top-level array payloads and field fallback behavior.
     *
     * @throws Exception when the integration flow fails.
     */
    @Test
    void parsesTopLevelArrayPayloadWithFallbackFields() throws Exception {
        ProviderLaunchApiClient client = new ProviderLaunchApiClient("NASA", new RealHttpGateway(), baseUrl + "/nasa-array");
        List<Launch> launches = client.fetchUpcomingLaunches(1);
        Launch launch = launches.isEmpty() ? null : launches.get(0);
        // TODO assert the parsed NASA launch fields
    }

    /**
     * Verifies HTTP 429 responses are mapped to RateLimitException.
     */
    @Test
    void propagatesRateLimitFrom429Response() {
        ProviderLaunchApiClient client = new ProviderLaunchApiClient("SpaceX", new RealHttpGateway(), baseUrl + "/throttled");
        // TODO assert the throttled response behavior
    }

    /**
     * Verifies objects with missing required fields are filtered out.
     *
     * @throws Exception when the integration flow fails.
     */
    @Test
    void ignoresObjectsMissingRequiredLaunchFields() throws Exception {
        ProviderLaunchApiClient client = new ProviderLaunchApiClient("NASA", new RealHttpGateway(), baseUrl + "/invalid");
        List<Launch> launches = client.fetchUpcomingLaunches(3);
        // TODO assert invalid launch objects are ignored
    }

    /**
     * Verifies empty HTTP error responses produce a normal IOException instead of a null-stream failure.
     */
    @Test
    void handlesHttpErrorResponseWithoutBody() {
        ProviderLaunchApiClient client =
            new ProviderLaunchApiClient("NASA", new RealHttpGateway(), baseUrl + "/server-error-empty");
        // TODO assert the empty-body HTTP error behavior
    }

    /**
     * Verifies end-to-end workflow behavior with real components and provider failure isolation.
     */
    @Test
    void workflowContinuesAndPersistsWithRealComponents() {
        InMemoryLaunchRepository repo = new InMemoryLaunchRepository();
        RecordingNotifier notifier = new RecordingNotifier();

        UpcomingLaunchClient brokenProvider = new UpcomingLaunchClient() {
            /**
             * Returns no launches for this simulated provider.
             *
             * @param limit requested launch count.
             * @return empty launch list.
             */
            @Override
            public List<Launch> fetchUpcomingLaunches(int limit) {
                return List.of();
            }

            /**
             * Throws an error to simulate provider metadata failure.
             *
             * @return never returns.
             */
            @Override
            public String getProviderName() {
                throw new RuntimeException("broken metadata");
            }
        };

        UpcomingLaunchClient healthyProvider = new UpcomingLaunchClient() {
            /**
             * Returns a deterministic launch fixture for integration assertions.
             *
             * @param limit requested launch count.
             * @return one launch fixture.
             */
            @Override
            public List<Launch> fetchUpcomingLaunches(int limit) {
                return List.of(new Launch("mission-1", "Mission", "2026-08-01T09:00:00Z", "7", "detail"));
            }

            /**
             * Returns a stable provider name used as repository key.
             *
             * @return provider name.
             */
            @Override
            public String getProviderName() {
                return "IntegrationProvider";
            }
        };

        LaunchUpdateService service = new LaunchUpdateService(List.of(brokenProvider, healthyProvider), repo, notifier);
        service.checkForUpdatesAcrossProviders();
        // TODO assert the workflow stores the launch and records one notification
    }

    /**
     * Sends a JSON response from the embedded server.
     *
     * @param exchange current HTTP exchange.
     * @param status HTTP status code.
     * @param body response body.
     * @throws IOException when writing the response fails.
     */
    private static void sendJson(HttpExchange exchange, int status, String body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        sendBody(exchange, status, body);
    }

    /**
     * Sends a text response from the embedded server.
     *
     * @param exchange current HTTP exchange.
     * @param status HTTP status code.
     * @param body response body.
     * @throws IOException when writing the response fails.
     */
    private static void sendText(HttpExchange exchange, int status, String body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        sendBody(exchange, status, body);
    }

    /**
     * Writes bytes to the HTTP response stream and closes the exchange.
     *
     * @param exchange current HTTP exchange.
     * @param status HTTP status code.
     * @param body response body.
     * @throws IOException when writing the response fails.
     */
    private static void sendBody(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        } finally {
            exchange.close();
        }
    }

    /**
     * Sends a response with no body to exercise empty error-stream handling.
     *
     * @param exchange current HTTP exchange.
     * @param status HTTP status code.
     * @throws IOException when writing the response fails.
     */
    private static void sendNoBody(HttpExchange exchange, int status) throws IOException {
        exchange.sendResponseHeaders(status, -1);
        exchange.close();
    }

    private static final class RecordingNotifier implements Notifier {
        private final List<String> messages = new ArrayList<>();

        /**
         * Records notification data for later assertions.
         *
         * @param provider provider name.
         * @param changeType change classification.
         * @param launch launch payload.
         */
        @Override
        public void notify(String provider, String changeType, Launch launch) {
            messages.add(provider + "|" + changeType + "|" + launch.id);
        }
    }
}
