package com.cosmochain.support;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * HTTP gateway that performs real network requests with rate-limit handling.
 */
public class RealHttpGateway implements HttpGateway {
    private static final Map<String, Long> URL_THROTTLED_UNTIL_MS = new HashMap<>();
    private static final Pattern RETRY_AFTER_BODY_PATTERN =
        Pattern.compile("Expected available in (\\d+) seconds");

    private final boolean allowInsecureSsl;
    private final SSLSocketFactory insecureSocketFactory;
    private final HostnameVerifier insecureHostnameVerifier;

    /**
     * Creates a real HTTP gateway with optional insecure-SSL fallback support.
     */
    public RealHttpGateway() {
        this.allowInsecureSsl = isInsecureSslEnabled();
        this.insecureHostnameVerifier = (hostname, session) -> true;
        this.insecureSocketFactory = buildInsecureSocketFactory();
    }

    /**
     * Executes a GET request with optional TLS fallback and throttle awareness.
     *
     * @param url target URL.
     * @return response body.
     * @throws IOException when request execution fails.
     */
    @Override
    public String get(String url) throws IOException {
        try {
            return executeRequest(url, allowInsecureSsl);
        } catch (SSLHandshakeException e) {
            if (!allowInsecureSsl) {
                System.out.println(
                    "WARNING: TLS handshake failed. Retrying once with insecure SSL for local testing."
                );
                try {
                    return executeRequest(url, true);
                } catch (SSLHandshakeException retryFailure) {
                    throw new IOException(
                        "TLS handshake still failed after insecure retry. Root cause: "
                            + retryFailure.getMessage()
                            + ". This often means an outdated Java runtime/TLS stack.",
                        retryFailure
                    );
                }
            }
            throw new IOException(
                "TLS handshake failed. Install your CA cert in the JDK truststore, or set ALLOW_INSECURE_SSL=true for local testing.",
                e
            );
        }
    }

    /**
     * Returns whether insecure SSL mode is enabled via environment variable or system property.
     *
     * @return true when insecure SSL mode is enabled.
     */
    public static boolean isInsecureSslEnabled() {
        String envValue = System.getenv("ALLOW_INSECURE_SSL");
        String sysPropUpper = System.getProperty("ALLOW_INSECURE_SSL");
        String sysPropLower = System.getProperty("allow.insecure.ssl");
        return parseFlexibleBoolean(envValue)
            || parseFlexibleBoolean(sysPropUpper)
            || parseFlexibleBoolean(sysPropLower);
    }

    /**
     * Performs the underlying HTTP request and handles status-specific behaviors.
     *
     * @param url target URL.
     * @param useInsecureSsl whether to apply insecure SSL trust settings.
     * @return response body.
     * @throws IOException when request execution fails.
     */
    private String executeRequest(String url, boolean useInsecureSsl) throws IOException {
        long remaining = getThrottleRemainingSeconds(url);
        if (remaining > 0) {
            throw new RateLimitException(
                "Request was throttled. Expected available in " + remaining + " seconds.",
                remaining
            );
        }

        HttpURLConnection conn = openConnection(url, useInsecureSsl);
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        try {
            int status = conn.getResponseCode();
            InputStream stream = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
            String content = readResponseBody(stream);

            if (status == 429) {
                long retryAfterSeconds = extractRetryAfterSeconds(conn, content);
                setThrottle(url, retryAfterSeconds);
                throw new RateLimitException(
                    "Request was throttled. Expected available in " + retryAfterSeconds + " seconds.",
                    retryAfterSeconds
                );
            }

            if (status < 200 || status >= 300) {
                throw new IOException("HTTP error: " + status + " - " + content);
            }
            clearThrottle(url);
            return content;
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Opens an HTTP connection and applies insecure SSL settings when requested.
     *
     * @param url target URL.
     * @param useInsecureSsl whether to apply insecure SSL trust settings.
     * @return open connection.
     * @throws IOException when opening the connection fails.
     */
    private HttpURLConnection openConnection(String url, boolean useInsecureSsl) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) java.net.URI.create(url).toURL().openConnection();
        if (useInsecureSsl && conn instanceof HttpsURLConnection) {
            HttpsURLConnection httpsConn = (HttpsURLConnection) conn;
            httpsConn.setSSLSocketFactory(insecureSocketFactory);
            httpsConn.setHostnameVerifier(insecureHostnameVerifier);
        }
        return conn;
    }

    /**
     * Builds an SSL socket factory that trusts all certificates.
     *
     * @return insecure SSL socket factory.
     */
    private static SSLSocketFactory buildInsecureSocketFactory() {
        try {
            TrustManager[] trustAll = new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }

                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    }
                }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAll, new SecureRandom());
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize insecure SSL context", e);
        }
    }

    /**
     * Parses common truthy string representations.
     *
     * @param value input string.
     * @return true for true/1/yes values.
     */
    private static boolean parseFlexibleBoolean(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim().toLowerCase();
        return "true".equals(normalized) || "1".equals(normalized) || "yes".equals(normalized);
    }

    /**
     * Extracts retry-after seconds from headers or response body.
     *
     * @param conn HTTP connection used for the request.
     * @param body response body text.
     * @return retry delay in seconds.
     */
    private static long extractRetryAfterSeconds(HttpURLConnection conn, String body) {
        String retryAfterHeader = conn.getHeaderField("Retry-After");
        if (retryAfterHeader != null && retryAfterHeader.matches("\\d+")) {
            return Long.parseLong(retryAfterHeader);
        }

        Matcher matcher = RETRY_AFTER_BODY_PATTERN.matcher(body == null ? "" : body);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }
        return 60L;
    }

    /**
     * Reads a response stream into a UTF-8 string.
     *
     * @param stream response stream, which may be null for empty error bodies.
     * @return response body text, or an empty string when no body is present.
     * @throws IOException when reading the stream fails.
     */
    private static String readResponseBody(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        try (InputStream responseStream = stream) {
            return new String(responseStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Returns remaining local throttle duration for a URL.
     *
     * @param url target URL.
     * @return remaining seconds, or zero when not throttled.
     */
    private static long getThrottleRemainingSeconds(String url) {
        synchronized (URL_THROTTLED_UNTIL_MS) {
            Long until = URL_THROTTLED_UNTIL_MS.get(url);
            if (until == null) {
                return 0L;
            }
            long remainingMs = until - System.currentTimeMillis();
            if (remainingMs <= 0) {
                URL_THROTTLED_UNTIL_MS.remove(url);
                return 0L;
            }
            return (remainingMs + 999) / 1000;
        }
    }

    /**
     * Stores local throttle expiration for a URL.
     *
     * @param url target URL.
     * @param retryAfterSeconds retry delay in seconds.
     */
    private static void setThrottle(String url, long retryAfterSeconds) {
        long safeRetrySeconds = Math.max(1L, retryAfterSeconds);
        synchronized (URL_THROTTLED_UNTIL_MS) {
            URL_THROTTLED_UNTIL_MS.put(url, System.currentTimeMillis() + safeRetrySeconds * 1000);
        }
    }

    /**
     * Clears local throttle state for a URL.
     *
     * @param url target URL.
     */
    private static void clearThrottle(String url) {
        synchronized (URL_THROTTLED_UNTIL_MS) {
            URL_THROTTLED_UNTIL_MS.remove(url);
        }
    }
}
