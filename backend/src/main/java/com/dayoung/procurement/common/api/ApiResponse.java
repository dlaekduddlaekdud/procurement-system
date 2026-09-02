package com.dayoung.procurement.common.api;

public record ApiResponse<T>(
		boolean success,
		T data,
		ApiError error
) {

	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>(true, data, null);
	}

	public static <T> ApiResponse<T> failure(String code, String message) {
		return new ApiResponse<>(false, null, ApiError.of(code, message));
	}

	public static <T> ApiResponse<T> failure(ApiError error) {
		return new ApiResponse<>(false, null, error);
	}
}
