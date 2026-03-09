package com.cosmochain.support;

import java.io.IOException;

/**
 * Abstraction for HTTP GET requests.
 */
public interface HttpGateway {
    /**
     * Executes an HTTP GET request for the given URL.
     *
     * @param url target URL.
     * @return response body as text.
     * @throws IOException when the request fails.
     */
    String get(String url) throws IOException;
}
