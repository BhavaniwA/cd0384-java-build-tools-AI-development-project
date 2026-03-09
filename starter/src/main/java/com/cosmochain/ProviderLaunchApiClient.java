package com.cosmochain;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

import com.cosmochain.support.HttpGateway;
import com.cosmochain.support.Launch;
import com.cosmochain.support.UpcomingLaunchClient;
import com.cosmochain.support.UrlUtil;
import org.json.JSONArray;
import org.json.JSONObject;

public class ProviderLaunchApiClient implements UpcomingLaunchClient {
    private final String providerName;
    private final HttpGateway http;
    private final String url;

    /** Makes the provider client. */
    public ProviderLaunchApiClient(String providerName, HttpGateway http, String url) {
        this.providerName = providerName;
        this.http = http;
        this.url = url;
    }

    /** Gets upcoming launches. */
    @Override
    public List<Launch> fetchUpcomingLaunches(int limit) throws Exception {
        String json = http.get(UrlUtil.withLimit(url, limit));
        List<Launch> launches = new ArrayList<>();
        for (JSONObject object : extractLaunchObjects(json, limit)) {
            Launch parsed = parseLaunchObject(object);
            if (parsed != null) {
                launches.add(parsed);
            }
        }
        return launches;
    }

    /**
     * Returns the configured provider label.
     *
     * @return provider name.
     */
    @Override
    public String getProviderName() {
        return providerName;
    }

    /** Finds launch objects in the payload. */
    private List<JSONObject> extractLaunchObjects(String json, int limit) {
        List<JSONObject> objects = new ArrayList<>();
        if (json == null || json.isBlank() || limit <= 0) {
            return objects;
        }

        String trimmed = json.trim();
        if (trimmed.startsWith("[")) {
            JSONArray array = new JSONArray(trimmed);
            for (int i = 0; i < array.length() && objects.size() < limit; i++) {
                JSONObject object = array.optJSONObject(i);
                if (object != null) {
                    objects.add(object);
                }
            }
            return objects;
        }

        JSONObject root = new JSONObject(trimmed);
        JSONArray results = root.optJSONArray("results");
        if (results != null) {
            for (int i = 0; i < results.length() && objects.size() < limit; i++) {
                JSONObject object = results.optJSONObject(i);
                if (object != null) {
                    objects.add(object);
                }
            }
            return objects;
        }

        objects.add(root);
        return objects;
    }

    /** Turns one JSON object into a launch. */
    private Launch parseLaunchObject(JSONObject objectJson) {
        String id = objectJson.optString("id", null);
        String name = objectJson.optString("name", null);
        String dateUtc = firstNonBlank(
            objectJson.optString("date_utc", null),
            objectJson.optString("window_start", null),
            objectJson.optString("net", null)
        );
        String flightNumber = objectJson.has("flight_number") && !objectJson.isNull("flight_number")
            ? String.valueOf(objectJson.get("flight_number"))
            : null;
        String details = firstNonBlank(
            extractMissionDescription(objectJson),
            objectJson.optString("details", null),
            objectJson.optString("description", null)
        );

        if (id == null || name == null || dateUtc == null) {
            return null;
        }

        return new Launch(id, name, dateUtc, flightNumber, sanitizeDetails(details));
    }

    /** Gets nested mission details if they exist. */
    private String extractMissionDescription(JSONObject objectJson) {
        JSONObject mission = objectJson.optJSONObject("mission");
        if (mission == null) {
            return null;
        }
        return mission.optString("description", null);
    }

    /** Picks the first usable value. */
    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    /** Cleans up mission details a bit. */
    private String sanitizeDetails(String details) {
        if (details == null || details.isBlank()) {
            return details;
        }

        String cleaned = details
            .replace("\\r\\n", "\n")
            .replace("\\n", "\n")
            .replace("\\r", "\n")
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace("\\t", " ")
            .replace("\\\"", "\"")
            .replace('?', ' ');

        cleaned = Normalizer.normalize(cleaned, Normalizer.Form.NFKC)
            .replace('\uFFFD', ' ')
            .replace('\u2019', '\'')
            .replace('\u2018', '\'')
            .replace('\u201C', '"')
            .replace('\u201D', '"')
            .replace('\u2013', '-')
            .replace('\u2014', '-')
            .replace('\u00A0', ' ');

        cleaned = cleaned.replaceAll("[^\\x20-\\x7E\\n]", " ");
        cleaned = cleaned.replaceAll("[ ]{2,}", " ");
        cleaned = cleaned.replaceAll("\\n{3,}", "\n\n");

        String[] lines = cleaned.split("\\n");
        StringBuilder normalized = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            normalized.append(lines[i].trim());
            if (i < lines.length - 1) {
                normalized.append('\n');
            }
        }

        return normalized.toString().trim();
    }
}
