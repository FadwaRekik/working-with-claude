package com.marlowefinch.ops;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

/**
 * Collects validation errors for the raw query parameters of one request (TODO-232), so a
 * request with several problems reports all of them. Call {@link #validate()} once every
 * parameter has been read.
 */
public final class QueryParams {

    public static final int DEFAULT_LIMIT = 20;
    public static final int MAX_LIMIT = 500;

    private final List<String> errors = new ArrayList<>();

    public DateRange dateRange(String from, String to, Clock clock) {
        return DateRange.resolve(from, to, clock, errors);
    }

    /** Parses {@code limit}: missing or blank means the default, otherwise an integer 1..500. */
    public int limit(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT_LIMIT;
        }
        try {
            int limit = Integer.parseInt(raw.trim());
            if (limit >= 1 && limit <= MAX_LIMIT) {
                return limit;
            }
        } catch (NumberFormatException ignored) {
            // reported below
        }
        errors.add("limit must be an integer between 1 and " + MAX_LIMIT);
        return DEFAULT_LIMIT;
    }

    /** Throws {@link InvalidQueryException} if any parameter was invalid. */
    public void validate() {
        if (!errors.isEmpty()) {
            throw new InvalidQueryException(errors);
        }
    }
}
