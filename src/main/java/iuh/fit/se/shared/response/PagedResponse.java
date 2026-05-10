package iuh.fit.se.shared.response;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Lightweight pagination envelope for paginated REST endpoints.
 *
 * <p>Use {@link #from(Page)} to convert a Spring Data {@link Page} into a
 * stable JSON shape: {@code { content, page, size, totalElements, totalPages }}.
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
