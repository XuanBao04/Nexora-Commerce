package com.nexoracommerce.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private Boolean success;
    private LocalDateTime timestamp;
    private Integer status;
    private String message;
    private T data;
    private PaginationInfo pagination;

    public static <T> ApiResponse<T> success(T data, String message, Integer status) {
        return ApiResponse.<T>builder()
                .success(status != null && status >= 200 && status < 300)
                .timestamp(LocalDateTime.now())
                .status(status)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(T data) {
        return success(data, "Success", 200);
    }

    public static <T> ApiResponse<T> created(T data) {
        return success(data, "Resource created successfully", 201);
    }

    public static <T> ApiResponse<T> ok(T data) {
        return success(data, "Success", 200);
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return success(data, message, 200);
    }

    public static <T> ApiResponse<T> failure(String message, Integer status) {
        return ApiResponse.<T>builder()
                .success(false)
                .timestamp(LocalDateTime.now())
                .status(status)
                .message(message)
                .build();
    }

    public static <T> ApiResponse<T> okWithPagination(T data, PaginationInfo pagination) {
        ApiResponse<T> response = success(data);
        response.setPagination(pagination);
        return response;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaginationInfo {
        private Integer page;
        private Integer pageSize;
        private Long totalElements;
        private Integer totalPages;
        private Boolean hasNext;
        private Boolean hasPrevious;

        public static PaginationInfo from(org.springframework.data.domain.Page<?> page) {
            return PaginationInfo.builder()
                    .page(page.getNumber())
                    .pageSize(page.getSize())
                    .totalElements(page.getTotalElements())
                    .totalPages(page.getTotalPages())
                    .hasNext(page.hasNext())
                    .hasPrevious(page.hasPrevious())
                    .build();
        }
    }
}
