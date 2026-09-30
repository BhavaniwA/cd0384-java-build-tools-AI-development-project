package com.cosmochain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cosmochain.support.UrlUtil;
import org.junit.jupiter.api.Test;

class UrlUtilTest {

    @Test
    void addsLimitToUrls() {
        assertEquals(
                "https://example.com/api?limit=5",
                UrlUtil.withLimit("https://example.com/api", 5));

        assertEquals(
                "https://example.com/api?foo=bar&limit=5",
                UrlUtil.withLimit("https://example.com/api?foo=bar", 5));

        assertEquals(
                "https://example.com/api?limit=5",
                UrlUtil.withLimit("https://example.com/api?limit=2", 5));
    }
}
