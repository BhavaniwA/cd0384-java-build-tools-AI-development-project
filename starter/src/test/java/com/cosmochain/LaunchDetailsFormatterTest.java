package com.cosmochain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cosmochain.support.Launch;
import org.junit.jupiter.api.Test;

class LaunchDetailsFormatterTest {
    @Test
    void formatsSpaceXLaunchWithIsoZuluAndBlankDetailsSafely() {
        Launch launch = new Launch("id-1", "Starlink", "2026-07-01T10:20:30Z", "52", "   ");

        String output = LaunchDetailsFormatter.formatLaunchDetails("SpaceX", launch);

        assertAll(
            () -> assertTrue(output.contains("Mission: Starlink")),
            () -> assertTrue(output.contains("Flight Number: 52")),
            () -> assertTrue(output.contains("Date: 2026-07-01")),
            () -> assertTrue(output.contains("Time: 10:20:30")),
            () -> assertTrue(output.contains("Time Zone: UTC")),
            () -> assertTrue(output.contains("Details:    "))
        );
    }

    @Test
    void formatsNasaLaunchWithOffsetTimezone() {
        Launch launch = new Launch("id-2", "Artemis", "2026-08-01T13:14:15+02:30", null, "Moon mission");

        String output = LaunchDetailsFormatter.formatLaunchDetails("NASA", launch);

        assertAll(
            () -> assertTrue(output.contains("Mission: Artemis")),
            () -> assertTrue(output.contains("Date: 2026-08-01")),
            () -> assertTrue(output.contains("Time: 13:14:15")),
            () -> assertTrue(output.contains("Time Zone: +02:30")),
            () -> assertTrue(output.contains("Details: Moon mission"))
        );
    }

    @Test
    void formatsUnknownProviderUsingFallbackLayout() {
        Launch launch = new Launch("id-3", "Generic Mission", "raw-date", null, null);

        String output = LaunchDetailsFormatter.formatLaunchDetails("Other", launch);

        assertAll(
            () -> assertTrue(output.contains("Name: Generic Mission")),
            () -> assertTrue(output.contains("Date/Time: raw-date"))
        );
    }

    @Test
    void handlesBlankAndNonIsoDates() {
        Launch blankDate = new Launch(
                "id-4", "Blank Date", "   ", null, null);

        String blankOutput =
                LaunchDetailsFormatter.formatLaunchDetails("SpaceX", blankDate);

        assertTrue(blankOutput.contains("Date: N/A"));
        assertTrue(blankOutput.contains("Time: N/A"));
        assertTrue(blankOutput.contains("Time Zone: N/A"));
        assertTrue(blankOutput.contains("Flight Number: N/A"));
        assertTrue(blankOutput.contains("Details: N/A"));

        Launch nonIsoDate = new Launch(
                "id-5", "Fallback Mission",
                "2026-09-30T12:34:56+05:30", null, null);

        String fallbackOutput =
                LaunchDetailsFormatter.formatLaunchDetails("SpaceX", nonIsoDate);

        assertTrue(fallbackOutput.contains("Date: 2026-09-30"));
        assertTrue(fallbackOutput.contains("Time: 12:34:56"));
        assertTrue(fallbackOutput.contains("Time Zone: +05:30"));
    }
}
