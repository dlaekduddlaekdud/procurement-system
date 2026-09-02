package com.dayoung.procurement.common.api;

import com.dayoung.procurement.purchase.exception.InactivePurchaseUserException;
import com.dayoung.procurement.purchase.exception.InvalidPurchaseRequestStateException;
import com.dayoung.procurement.purchase.exception.PurchaseItemNotFoundException;
import com.dayoung.procurement.purchase.exception.PurchaseRequestAccessDeniedException;
import com.dayoung.procurement.purchase.exception.PurchaseRequestLineNotFoundException;
import com.dayoung.procurement.purchase.exception.PurchaseRequestNotFoundException;
import com.dayoung.procurement.purchase.exception.PurchaseRoleRequiredException;
import com.dayoung.procurement.purchase.exception.SelfApprovalNotAllowedException;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(PurchaseRequestNotFoundException.class)
	public ResponseEntity<ApiResponse<Void>> handlePurchaseRequestNotFound(PurchaseRequestNotFoundException exception) {
		return error(HttpStatus.NOT_FOUND, "PURCHASE_REQUEST_NOT_FOUND", exception.getMessage());
	}

	@ExceptionHandler(PurchaseRequestLineNotFoundException.class)
	public ResponseEntity<ApiResponse<Void>> handlePurchaseRequestLineNotFound(
			PurchaseRequestLineNotFoundException exception
	) {
		return error(HttpStatus.NOT_FOUND, "PURCHASE_REQUEST_LINE_NOT_FOUND", exception.getMessage());
	}

	@ExceptionHandler(PurchaseItemNotFoundException.class)
	public ResponseEntity<ApiResponse<Void>> handlePurchaseItemNotFound(PurchaseItemNotFoundException exception) {
		return error(HttpStatus.NOT_FOUND, "PURCHASE_ITEM_NOT_FOUND", exception.getMessage());
	}

	@ExceptionHandler(InactivePurchaseUserException.class)
	public ResponseEntity<ApiResponse<Void>> handleInactivePurchaseUser(InactivePurchaseUserException exception) {
		return error(HttpStatus.NOT_FOUND, "ACTIVE_USER_NOT_FOUND", exception.getMessage());
	}

	@ExceptionHandler({
			PurchaseRequestAccessDeniedException.class,
			PurchaseRoleRequiredException.class,
			SelfApprovalNotAllowedException.class
	})
	public ResponseEntity<ApiResponse<Void>> handleForbidden(RuntimeException exception) {
		return error(HttpStatus.FORBIDDEN, "FORBIDDEN", exception.getMessage());
	}

	@ExceptionHandler(InvalidPurchaseRequestStateException.class)
	public ResponseEntity<ApiResponse<Void>> handleInvalidPurchaseRequestState(
			InvalidPurchaseRequestStateException exception
	) {
		return error(HttpStatus.CONFLICT, "INVALID_PURCHASE_REQUEST_STATE", exception.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
		List<FieldViolation> fields = exception.getBindingResult().getFieldErrors().stream()
				.map(fieldError -> new FieldViolation(fieldError.getField(), fieldError.getDefaultMessage()))
				.toList();
		return validationError(fields);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException exception) {
		List<FieldViolation> fields = exception.getConstraintViolations().stream()
				.map(violation -> new FieldViolation(
						violation.getPropertyPath().toString(),
						violation.getMessage()
				))
				.toList();
		return validationError(fields);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException exception) {
		return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", exception.getMessage());
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation() {
		return error(HttpStatus.CONFLICT, "DATA_CONFLICT", "이미 존재하거나 다른 데이터에서 사용 중인 값입니다.");
	}

	private ResponseEntity<ApiResponse<Void>> validationError(List<FieldViolation> fields) {
		ApiError apiError = new ApiError("VALIDATION_FAILED", "입력값을 확인해 주세요.", fields);
		return ResponseEntity.badRequest().body(ApiResponse.failure(apiError));
	}

	private ResponseEntity<ApiResponse<Void>> error(HttpStatus status, String code, String message) {
		return ResponseEntity.status(status).body(ApiResponse.failure(code, message));
	}
}
