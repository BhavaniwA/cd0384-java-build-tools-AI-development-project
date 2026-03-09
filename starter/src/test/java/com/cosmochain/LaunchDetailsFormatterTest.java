package com.cosmochain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cosmochain.support.Launch;
import org.junit.jupiter.api.Test;

class LaunchDetailsFormatterTest {
    /**
     * Verifies SpaceX formatting for UTC timestamps and blank details.
     */
    @Test
    void formatsSpaceXLaunchWithIsoZuluAndBlankDetailsSafely() {
        Launch launch = new Launch("id-1", "Starlink", "2026-07-01T10:20:30Z", "52", "   ");

        String output = LaunchDetailsFormatter.formatLaunchDetails("SpaceX", launch);
        // TODO assert the important SpaceX formatting fields
    }

    /**
     * Verifies NASA formatting for explicit timezone offsets.
     */
    @Test
    void formatsNasaLaunchWithOffsetTimezone() {
        Launch launch = new Launch("id-2", "Artemis", "2026-08-01T13:14:15+02:30", null, "Moon mission");

        String output = LaunchDetailsFormatter.formatLaunchDetails("NASA", launch);
        // TODO assert the important NASA formatting fields
    }

    /**
     * Verifies generic fallback formatting for unknown providers.
     */
    @Test
    void formatsUnknownProviderUsingFallbackLayout() {
        Launch launch = new Launch("id-3", "Generic Mission", "raw-date", null, null);

        String output = LaunchDetailsFormatter.formatLaunchDetails("Other", launch);
        // TODO assert the fallback layout
    }
}
