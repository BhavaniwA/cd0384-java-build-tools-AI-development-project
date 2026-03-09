package com.cosmochain.support;

/**
 * Immutable launch data model used across parsing, workflow, and notifications.
 */
public class Launch {
    public final String id;
    public final String name;
    public final String dateUtc;
    public final String flightNumber;
    public final String details;

    /**
     * Creates a launch value object.
     *
     * @param id provider launch id.
     * @param name launch mission name.
     * @param dateUtc launch date-time text.
     * @param flightNumber provider flight number when available.
     * @param details launch details text when available.
     */
    public Launch(String id, String name, String dateUtc, String flightNumber, String details) {
        this.id = id;
        this.name = name;
        this.dateUtc = dateUtc;
        this.flightNumber = flightNumber;
        this.details = details;
    }
}
