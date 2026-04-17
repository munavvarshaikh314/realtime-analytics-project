package com.analytics.app.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Accepts both ISO-8601 timestamps used in the API docs and the legacy
 * "yyyy-MM-dd HH:mm:ss" payloads already used by the simulator/tests.
 */
public class FlexibleLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    private static final List<DateTimeFormatter> LOCAL_DATE_TIME_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    );

    @Override
    public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String value = parser.getValueAsString();
        if (value == null || value.isBlank()) {
            return null;
        }

        String timestamp = value.trim();

        try {
            return OffsetDateTime.parse(timestamp, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            // Fall through to additional formats.
        }

        try {
            return Instant.parse(timestamp).atOffset(ZoneOffset.UTC).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            // Fall through to local date-time formats.
        }

        for (DateTimeFormatter formatter : LOCAL_DATE_TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(timestamp, formatter);
            } catch (DateTimeParseException ignored) {
                // Continue trying supported formats.
            }
        }

        throw context.weirdStringException(
                timestamp,
                LocalDateTime.class,
                "Expected ISO-8601 date-time or yyyy-MM-dd HH:mm:ss"
        );
    }
}
