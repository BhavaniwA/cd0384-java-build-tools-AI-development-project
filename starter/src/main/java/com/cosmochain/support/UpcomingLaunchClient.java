package com.cosmochain.support;

import java.io.IOException;
import java.util.List;

/**
 * Contract for provider clients that return upcoming launch data.
 */
public interface UpcomingLaunchClient {
    /**
     * Returns the next upcoming launch using a single-item request.
     *
     * @return next launch item.
     * @throws Exception when retrieval fails or no launch is available.
     */
    default Launch fetchNextLaunch() throws Exception {
        List<Launch> launches = fetchUpcomingLaunches(1);
        if (launches.isEmpty()) {
            throw new IOException(getProviderName() + ": No upcoming launches found");
        }
        return launches.get(0);
    }

    /**
     * Returns upcoming launches from a provider.
     *
     * @param limit maximum number of launches requested.
     * @return list of upcoming launches.
     * @throws Exception when retrieval fails.
     */
    List<Launch> fetchUpcomingLaunches(int limit) throws Exception;

    /**
     * Returns the provider label.
     *
     * @return provider name.
     */
    String getProviderName();
}
