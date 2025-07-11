package io.github.oljc.arcoserve.shared.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 通用分页响应结构
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PageData<T>(
        List<T> records,
        Integer current,
        Integer size,
        Long total,
        Integer pages,
        Boolean hasNext,
        Boolean hasPrevious
) {

    public static <T> PageData<T> of(Page<T> page) {
        return new PageData<>(
                page.getContent(),
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious()
        );
    }

    public static <T> PageData<T> of(List<T> records, int current, int size, long total) {
        int safeSize = Math.max(size, 1);
        int pages = calculatePages(total, safeSize);

        return new PageData<>(
                records == null ? List.of() : records,
                current,
                safeSize,
                total,
                pages,
                current < pages,
                current > 1
        );
    }

    public static <T> PageData<T> empty() {
        return new PageData<>(
                List.of(),
                1,
                10,
                0L,
                0,
                false,
                false
        );
    }

    public static <T> PageData<T> empty(int current, int size) {
        return new PageData<>(
                List.of(),
                current,
                size,
                0L,
                0,
                false,
                false
        );
    }

    private static int calculatePages(long total, int size) {
        return (int) Math.ceil((double) total / size);
    }
}
