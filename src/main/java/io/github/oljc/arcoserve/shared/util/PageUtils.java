package io.github.oljc.arcoserve.shared.util;

import io.github.oljc.arcoserve.shared.response.ApiResponse;
import io.github.oljc.arcoserve.shared.response.PageData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

/**
 * 分页工具类
 */
public interface PageUtils {

    /**
     * 构建分页响应
     */
    static <T> ApiResponse<PageData<T>> buildPageResponse(Page<T> page) {
        return ApiResponse.success(PageData.of(page));
    }

    /**
     * 构建分页响应 - 自定义消息
     */
    static <T> ApiResponse<PageData<T>> buildPageResponse(Page<T> page, String message) {
        return ApiResponse.success(PageData.of(page), message);
    }

    /**
     * 构建空分页响应
     */
    static <T> ApiResponse<PageData<T>> buildEmptyPageResponse(Integer current, Integer size) {
        return ApiResponse.success(PageData.empty(current, size));
    }

    /**
     * 构建空分页响应 - 自定义消息
     */
    static <T> ApiResponse<PageData<T>> buildEmptyPageResponse(Integer current, Integer size, String message) {
        return ApiResponse.success(PageData.empty(current, size), message);
    }

    /**
     * 构建分页请求对象
     */
    static Pageable buildPageRequest(Integer current, Integer size) {
        return PageRequest.of(current - 1, size); // 转换为从0开始的页码
    }

    /**
     * 构建分页请求对象 - 带排序
     */
    static Pageable buildPageRequest(Integer current, Integer size, Sort sort) {
        return PageRequest.of(current - 1, size, sort);
    }

    /**
     * 构建分页请求对象 - 简单排序
     */
    static Pageable buildPageRequest(Integer current, Integer size, String sortField, String sortOrder) {
        Sort sort = "desc".equalsIgnoreCase(sortOrder) ?
                Sort.by(Sort.Direction.DESC, sortField) :
                Sort.by(Sort.Direction.ASC, sortField);
        return PageRequest.of(current - 1, size, sort);
    }

    /**
     * 验证分页参数
     */
    static void validatePageParams(Integer current, Integer size) {
        if (current == null || current < 1) {
            throw new IllegalArgumentException("页码必须大于0");
        }
        if (size == null || size < 1) {
            throw new IllegalArgumentException("每页数量必须大于0");
        }
        if (size > 100) {
            throw new IllegalArgumentException("每页数量不能超过100");
        }
    }
}
