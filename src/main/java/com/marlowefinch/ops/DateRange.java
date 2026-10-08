package com.marlowefinch.ops;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A closed date range for the query endpoints.
 *
 * Both bounds default to "the last 30 days ending today". {@link #resolve} validates the
 * raw query parameters (TODO-232); the record itself stays permissive so repositories can
 * be called with any range.
 */
public record DateRange(LocalDate from, LocalDate to) {

    public static final int DEFAULT_DAYS = 30;
    public static final int MAX_DAYS = 366;

    private static final Pattern ISO_DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    /** Resolves and validates the range, throwing {@link InvalidQueryException} on bad input. */
    public static DateRange resolve(String from, String to, Clock clock) {
        QueryParams params = new QueryParams();
        DateRange range = params.dateRange(from, to, clock);
        params.validate();
        return range;
    }

    /**
     * Resolves the range, appending a message to {@code errors} for each problem found.
     * Returns {@code null} if the range could not be resolved.
     */
    static DateRange resolve(String from, String to, Clock clock, List<String> errors) {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = isMissing(from) ? today.minusDays(DEFAULT_DAYS) : parse("from", from, errors);
        LocalDate end = isMissing(to) ? today : parse("to", to, errors);
        if (start == null || end == null) {
            return null;
        }
        if (start.isAfter(end)) {
            errors.add("from must be on or before to");
            return null;
        }
        if (ChronoUnit.DAYS.between(start, end) > MAX_DAYS) {
            errors.add("range must span at most " + MAX_DAYS + " days");
            return null;
        }
        return new DateRange(start, end);
    }

    private static boolean isMissing(String value) {
        return value == null || value.isBlank();
    }

    private static LocalDate parse(String name, String value, List<String> errors) {
        String trimmed = value.trim();
        if (ISO_DATE.matcher(trimmed).matches()) {
            try {
                return LocalDate.parse(trimmed);
            } catch (DateTimeParseException ignored) {
                // falls through to the error below (e.g. 2026-02-30)
            }
        }
        errors.add(name + " must be an ISO date (YYYY-MM-DD)");
        return null;
    }
}
