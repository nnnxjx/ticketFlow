package com.ticketflow.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {
    private final String code;
    private final String message;
    private final T data;
    private final String requestId;

    public static <T> ApiResponse<T> ok(T data, String requestId) {
        return new ApiResponse<>("OK", "success", data, requestId);
    }
}
