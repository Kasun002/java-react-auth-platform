package com.org.auth.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Generic paginated response wrapper.
 * Carries page content alongside standard pagination metadata so callers
 * can drive paging controls without parsing Spring's raw {@link Page} JSON.
 */
@Data
@Schema(description = "Paginated result wrapper")
public class PageDto<T> {

    @Schema(description = "Page contents")
    private List<T> content;

    @Schema(description = "Current page number (0-based)", example = "0")
    private int page;

    @Schema(description = "Page size", example = "10")
    private int size;

    @Schema(description = "Total number of elements across all pages", example = "42")
    private long totalElements;

    @Schema(description = "Total number of pages", example = "5")
    private int totalPages;

    @Schema(description = "Whether this is the last page", example = "false")
    private boolean last;

    /** Factory method — maps a Spring {@link Page} to this DTO. */
    public static <T> PageDto<T> from(Page<T> springPage) {
        PageDto<T> dto = new PageDto<>();
        dto.setContent(springPage.getContent());
        dto.setPage(springPage.getNumber());
        dto.setSize(springPage.getSize());
        dto.setTotalElements(springPage.getTotalElements());
        dto.setTotalPages(springPage.getTotalPages());
        dto.setLast(springPage.isLast());
        return dto;
    }
}
