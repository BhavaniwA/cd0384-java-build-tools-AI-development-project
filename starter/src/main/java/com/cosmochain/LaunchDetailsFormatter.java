package com.cosmochain;

import com.cosmochain.support.Launch;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LaunchDetailsFormatter {
    private static final DateTimeFormatter TIME_OUTPUT_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * Utility class constructor.
     */
    private LaunchDetailsFormatter() {
    }

    /** Formats the launch output. */
    public static String formatLaunchDetails(String provider, Launch launch) {
        if ("SpaceX".equals(provider)) {
            String flight = launch.flightNumber == null ? "N/A" : launch.flightNumber;
            String details = launch.details == null ? "N/A" : launch.details;
            DateTimeParts dateTime = splitUtcDateTime(launch.dateUtc);
            return String.format(
                "Mission: %s%nFlight Number: %s%nDate: %s%nTime: %s%nTime Zone: %s%nDetails: %s",
                launch.name,
                flight,
                dateTime.date,
                dateTime.time,
                dateTime.timezone,
                details
            );
        }

        if ("NASA".equals(provider)) {
            String details = launch.details == null ? "N/A" : launch.details;
            DateTimeParts dateTime = splitUtcDateTime(launch.dateUtc);
            return String.format(
                "Mission: %s%nDate: %s%nTime: %s%nTime Zone: %s%nDetails: %s",
                launch.name,
                dateTime.date,
                dateTime.time,
                dateTime.timezone,
                details
            );
        }

        return String.format("Name: %s%nDate/Time: %s", launch.name, launch.dateUtc);
    }

    /** Breaks date text into pieces. */
    private static DateTimeParts splitUtcDateTime(String dateUtc) {
        if (dateUtc == null || dateUtc.isBlank()) {
            return new DateTimeParts("N/A", "N/A", "N/A");
        }

        try {
            OffsetDateTime parsed = OffsetDateTime.parse(dateUtc);
            return new DateTimeParts(
                parsed.toLocalDate().toString(),
                parsed.toLocalTime().format(TIME_OUTPUT_FORMAT),
                formatTimezone(parsed.getOffset().getId())
            );
        } catch (DateTimeParseException ignored) {
            // Fallback for non-ISO payloads.
        }

        String timezone = extractTimezone(dateUtc);
        String normalized = dateUtc.replace("Z", "");
        int tIndex = normalized.indexOf('T');
        if (tIndex > 0 && tIndex < normalized.length() - 1) {
            String datePart = normalized.substring(0, tIndex);
            String timePart = normalized.substring(tIndex + 1);
            if (timePart.length() > 8) {
                timePart = timePart.substring(0, 8);
            }
            return new DateTimeParts(datePart, timePart, timezone);
        }

        return new DateTimeParts(dateUtc, "N/A", timezone);
    }

    /** Pulls timezone info out. */
    private static String extractTimezone(String rawDateTime) {
        if (rawDateTime == null || rawDateTime.isBlank()) {
            return "N/A";
        }
        if (rawDateTime.endsWith("Z")) {
            return "UTC";
        }

        Matcher tzMatch = Pattern.compile("([+-]\\d{2}:\\d{2})$").matcher(rawDateTime);
        if (tzMatch.find()) {
            return formatTimezone(tzMatch.group(1));
        }
        return "N/A";
    }

    /** Formats the timezone. */
    private static String formatTimezone(String offset) {
        if (offset == null || offset.isBlank()) {
            return "N/A";
        }
        if ("Z".equals(offset) || "+00:00".equals(offset) || "-00:00".equals(offset)) {
            return "UTC";
        }
        if (offset.startsWith("+") || offset.startsWith("-")) {
            return offset;
        }
        return offset;
    }

    private static class DateTimeParts {
        private final String date;
        private final String time;
        private final String timezone;

        /**
         * Holds formatted date/time fields used by provider output templates.
         *
         * @param date output date value.
         * @param time output time value.
         * @param timezone output timezone value.
         */
        private DateTimeParts(String date, String time, String timezone) {
            this.date = date;
            this.time = time;
            this.timezone = timezone;
        }
    }
}



