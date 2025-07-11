package io.github.oljc.arcoserve.shared.response;

import org.springframework.data.domain.Page;

import java.util.Collections;
import java.util.List;

/**
 * 分页数据结构
 *
 * @param <T> 数据类型
 */
public record PageData<T>(
    /**
     * 数据列表
     */
    List<T> records,

    /**
     * 当前页码
     */
    Integer current,

    /**
     * 每页数量
     */
    Integer size,

    /**
     * 总记录数
     */
    Long total,

    /**
     * 总页数
     */
    Integer pages,

    /**
     * 是否有下一页
     */
    Boolean hasNext,

    /**
     * 是否有上一页
     */
    Boolean hasPrevious
) {

    /**
     * 从Spring Data Page构建
     */
    public static <T> PageData<T> of(Page<T> page) {
        return new PageData<>(
                page.getContent(),
                page.getNumber() + 1, // Spring Data JPA从0开始
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious()
        );
    }

    /**
     * 从数据列表构建分页数据
     */
    public static <T> PageData<T> of(List<T> records, Integer current, Integer size, Long total) {
        int pages = (int) Math.ceil((double) total / size);

        return new PageData<>(
                records,
                current,
                size,
                total,
                pages,
                current < pages,
                current > 1
        );
    }

    /**
     * 构建空页面
     */
    public static <T> PageData<T> empty(Integer current, Integer size) {
        return new PageData<>(
                Collections.emptyList(),
                current,
                size,
                0L,
                0,
                false,
                false
        );
    }
}
