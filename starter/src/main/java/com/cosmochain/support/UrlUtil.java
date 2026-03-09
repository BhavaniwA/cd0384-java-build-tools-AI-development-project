package com.cosmochain.support;

/**
 * URL helpers for query-parameter manipulation.
 */
public final class UrlUtil {
    /**
     * Utility class constructor.
     */
    private UrlUtil() {
    }

    /**
     * Adds or replaces the "limit" query parameter in a URL.
     *
     * @param inputUrl input URL.
     * @param limit desired limit value.
     * @return URL containing the requested limit.
     */
    public static String withLimit(String inputUrl, int limit) {
        if (inputUrl == null || inputUrl.isBlank() || limit <= 0) {
            return inputUrl;
        }

        String replacement = "$1" + limit;
        if (inputUrl.matches(".*([?&])limit=\\d+.*")) {
            return inputUrl.replaceFirst("([?&]limit=)\\d+", replacement);
        }
        if (inputUrl.contains("?")) {
            return inputUrl + "&limit=" + limit;
        }
        return inputUrl + "?limit=" + limit;
    }
}
