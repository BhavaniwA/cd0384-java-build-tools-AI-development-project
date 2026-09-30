package com.cosmochain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cosmochain.support.HttpGateway;
import com.cosmochain.support.Launch;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProviderLaunchApiClientTest {

    @Test
    void parsesLaunchAndReturnsProviderName() throws Exception {
        HttpGateway http = mock(HttpGateway.class);

        when(http.get("https://example.com/launches?limit=1"))
                .thenReturn("""
                    [
                      {
                        "id": "id-1",
                        "name": "Test Mission",
                        "date_utc": "2026-07-01T10:20:30Z",
                        "flight_number": 52,
                        "details": "Mission details"
                      }
                    ]
                    """);

        ProviderLaunchApiClient client =
                new ProviderLaunchApiClient(
                        "TestProvider",
                        http,
                        "https://example.com/launches");

        List<Launch> launches = client.fetchUpcomingLaunches(1);

        assertEquals("TestProvider", client.getProviderName());
        assertEquals(1, launches.size());
        assertEquals("id-1", launches.get(0).id);
        assertEquals("Test Mission", launches.get(0).name);
        assertEquals("52", launches.get(0).flightNumber);
    }
}
