package iuh.fit.se.shared.util;

import iuh.fit.se.shared.exception.DomainException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Converts date strings from multiple formats to {@link LocalDate}.
 * <p>Supported formats (tried in order):
 * <ol>
 *   <li>{@code dd-MM-yyyy} (e.g. 30-04-2025)</li>
 *   <li>{@code dd/MM/yyyy} (e.g. 30/04/2025)</li>
 *   <li>{@code yyyy-MM-dd} (ISO 8601, e.g. 2025-04-30)</li>
 * </ol>
 * Throws {@link DomainException} (HTTP 400) on invalid format or future date.
 */
@Component
public class FlexibleLocalDateConverter implements Converter<String, LocalDate> {

    private static final List<DateTimeFormatter> FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ISO_LOCAL_DATE
    );

    @Override
    @NonNull
    public LocalDate convert(@NonNull String source) {
        String trimmed = source.trim();
        if (trimmed.isEmpty()) {
            throw new DomainException("Date parameter must not be blank");
        }

        LocalDate parsed = tryParse(trimmed);
        if (parsed == null) {
            throw new DomainException(
                    "Invalid date format '" + trimmed + "'. Accepted formats: dd-MM-yyyy, dd/MM/yyyy, yyyy-MM-dd"
            );
        }

        if (parsed.isAfter(LocalDate.now())) {
            throw new DomainException(
                    "Date '" + trimmed + "' is in the future. Please provide a past or current date."
            );
        }

        return parsed;
    }

    private LocalDate tryParse(String value) {
        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // try next format
            }
        }
        return null;
    }
}
