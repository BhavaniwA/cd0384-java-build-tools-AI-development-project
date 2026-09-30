package com.cosmochain.support;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.Test;

class ConsoleNotifierTest {

    @Test
    void printsNotificationWithProviderChangeTypeAndLaunchDetails() {
        ConsoleNotifier notifier = new ConsoleNotifier();
        Launch launch = new Launch(
                "id-1",
                "Test Mission",
                "2026-07-01T10:20:30Z",
                "52",
                "Mission details"
        );

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;

        try {
            System.setOut(new PrintStream(output));

            notifier.notify("SpaceX", "NEW", launch);

            String text = output.toString();

            assertTrue(text.contains("[SpaceX]"));
            assertTrue(text.contains("NEW"));
            assertTrue(text.contains("Mission: Test Mission"));
            assertTrue(text.contains("Flight Number: 52"));
            assertTrue(text.contains("Details: Mission details"));
        } finally {
            System.setOut(originalOut);
        }
    }
}
