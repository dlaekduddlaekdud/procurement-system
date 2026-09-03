package com.dayoung.procurement.common.api;

import com.dayoung.procurement.closing.exception.ClosedPeriodException;
import com.dayoung.procurement.closing.exception.OpenPeriodReversalNotAllowedException;
import com.dayoung.procurement.invoice.exception.DuplicateInvoiceException;
import com.dayoung.procurement.invoice.exception.InvalidInvoiceStateException;
import com.dayoung.procurement.invoice.exception.InvoiceNotFoundException;
import com.dayoung.procurement.ledger.exception.AccrualEntryNotFoundException;
import com.dayoung.procurement.ledger.exception.DuplicateAccrualReversalException;
import com.dayoung.procurement.ledger.exception.InvalidAccrualReversalTargetException;
import com.dayoung.procurement.purchase.exception.InactivePurchaseUserException;
import com.dayoung.procurement.purchase.exception.InvalidPurchaseOrderStateException;
import com.dayoung.procurement.purchase.exception.InvalidPurchaseRequestStateException;
import com.dayoung.procurement.purchase.exception.PurchaseItemNotFoundException;
import com.dayoung.procurement.purchase.exception.PurchaseOrderAccessDeniedException;
import com.dayoung.procurement.purchase.exception.PurchaseOrderAlreadyExistsException;
import com.dayoung.procurement.purchase.exception.PurchaseOrderLineNotFoundException;
import com.dayoung.procurement.purchase.exception.PurchaseOrderNotFoundException;
import com.dayoung.procurement.purchase.exception.PurchaseRequestAccessDeniedException;
import com.dayoung.procurement.purchase.exception.PurchaseRequestLineNotFoundException;
import com.dayoung.procurement.purchase.exception.PurchaseRequestNotFoundException;
import com.dayoung.procurement.purchase.exception.PurchaseRoleRequiredException;
import com.dayoung.procurement.purchase.exception.PurchaseVendorNotFoundException;
import com.dayoung.procurement.purchase.exception.PurchaseWarehouseNotFoundException;
import com.dayoung.procurement.purchase.exception.SelfApprovalNotAllowedException;
import com.dayoung.procurement.receipt.exception.GoodsReceiptNotFoundException;
import com.dayoung.procurement.receipt.exception.GoodsReceiptCancellationBlockedException;
import com.dayoung.procurement.receipt.exception.InvalidGoodsReceiptStateException;
import com.dayoung.procurement.receipt.exception.PurchaseOrderQuantityExceededException;
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

	@ExceptionHandler(ClosedPeriodException.class)
	public ResponseEntity<ApiResponse<Void>> handleClosedPeriod(ClosedPeriodException exception) {
		return error(HttpStatus.CONFLICT, "CLOSED_PERIOD", exception.getMessage());
	}

	@ExceptionHandler(OpenPeriodReversalNotAllowedException.class)
	public ResponseEntity<ApiResponse<Void>> handleOpenPeriodReversalNotAllowed(
			OpenPeriodReversalNotAllowedException exception
	) {
		return error(HttpStatus.CONFLICT, "OPEN_PERIOD_REVERSAL_NOT_ALLOWED", exception.getMessage());
	}

	@ExceptionHandler(AccrualEntryNotFoundException.class)
	public ResponseEntity<ApiResponse<Void>> handleAccrualEntryNotFound(AccrualEntryNotFoundException exception) {
		return error(HttpStatus.NOT_FOUND, "ACCRUAL_ENTRY_NOT_FOUND", exception.getMessage());
	}

	@ExceptionHandler({
			DuplicateAccrualReversalException.class,
			InvalidAccrualReversalTargetException.class
	})
	public ResponseEntity<ApiResponse<Void>> handleAccrualReversalConflict(RuntimeException exception) {
		return error(HttpStatus.CONFLICT, "ACCRUAL_REVERSAL_CONFLICT", exception.getMessage());
	}

	@ExceptionHandler(DuplicateInvoiceException.class)
	public ResponseEntity<ApiResponse<Void>> handleDuplicateInvoice(DuplicateInvoiceException exception) {
		return error(HttpStatus.CONFLICT, "DUPLICATE_INVOICE", exception.getMessage());
	}

	@ExceptionHandler(InvoiceNotFoundException.class)
	public ResponseEntity<ApiResponse<Void>> handleInvoiceNotFound(InvoiceNotFoundException exception) {
		return error(HttpStatus.NOT_FOUND, "INVOICE_NOT_FOUND", exception.getMessage());
	}

	@ExceptionHandler(GoodsReceiptNotFoundException.class)
	public ResponseEntity<ApiResponse<Void>> handleGoodsReceiptNotFound(GoodsReceiptNotFoundException exception) {
		return error(HttpStatus.NOT_FOUND, "GOODS_RECEIPT_NOT_FOUND", exception.getMessage());
	}

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

	@ExceptionHandler(PurchaseVendorNotFoundException.class)
	public ResponseEntity<ApiResponse<Void>> handlePurchaseVendorNotFound(PurchaseVendorNotFoundException exception) {
		return error(HttpStatus.NOT_FOUND, "PURCHASE_VENDOR_NOT_FOUND", exception.getMessage());
	}

	@ExceptionHandler(PurchaseWarehouseNotFoundException.class)
	public ResponseEntity<ApiResponse<Void>> handlePurchaseWarehouseNotFound(
			PurchaseWarehouseNotFoundException exception
	) {
		return error(HttpStatus.NOT_FOUND, "PURCHASE_WAREHOUSE_NOT_FOUND", exception.getMessage());
	}

	@ExceptionHandler(PurchaseOrderAlreadyExistsException.class)
	public ResponseEntity<ApiResponse<Void>> handlePurchaseOrderAlreadyExists(
			PurchaseOrderAlreadyExistsException exception
	) {
		return error(HttpStatus.CONFLICT, "PURCHASE_ORDER_ALREADY_EXISTS", exception.getMessage());
	}

	@ExceptionHandler(PurchaseOrderNotFoundException.class)
	public ResponseEntity<ApiResponse<Void>> handlePurchaseOrderNotFound(PurchaseOrderNotFoundException exception) {
		return error(HttpStatus.NOT_FOUND, "PURCHASE_ORDER_NOT_FOUND", exception.getMessage());
	}

	@ExceptionHandler(PurchaseOrderLineNotFoundException.class)
	public ResponseEntity<ApiResponse<Void>> handlePurchaseOrderLineNotFound(
			PurchaseOrderLineNotFoundException exception
	) {
		return error(HttpStatus.NOT_FOUND, "PURCHASE_ORDER_LINE_NOT_FOUND", exception.getMessage());
	}

	@ExceptionHandler(InactivePurchaseUserException.class)
	public ResponseEntity<ApiResponse<Void>> handleInactivePurchaseUser(InactivePurchaseUserException exception) {
		return error(HttpStatus.NOT_FOUND, "ACTIVE_USER_NOT_FOUND", exception.getMessage());
	}

	@ExceptionHandler({
			PurchaseOrderAccessDeniedException.class,
			PurchaseRequestAccessDeniedException.class,
			PurchaseRoleRequiredException.class,
			SelfApprovalNotAllowedException.class
	})
	public ResponseEntity<ApiResponse<Void>> handleForbidden(RuntimeException exception) {
		return error(HttpStatus.FORBIDDEN, "FORBIDDEN", exception.getMessage());
	}

	@ExceptionHandler(InvalidPurchaseOrderStateException.class)
	public ResponseEntity<ApiResponse<Void>> handleInvalidPurchaseOrderState(
			InvalidPurchaseOrderStateException exception
	) {
		return error(HttpStatus.CONFLICT, "INVALID_PURCHASE_ORDER_STATE", exception.getMessage());
	}

	@ExceptionHandler(PurchaseOrderQuantityExceededException.class)
	public ResponseEntity<ApiResponse<Void>> handlePurchaseOrderQuantityExceeded(
			PurchaseOrderQuantityExceededException exception
	) {
		return error(HttpStatus.CONFLICT, "PURCHASE_ORDER_QUANTITY_EXCEEDED", exception.getMessage());
	}

	@ExceptionHandler(InvalidGoodsReceiptStateException.class)
	public ResponseEntity<ApiResponse<Void>> handleInvalidGoodsReceiptState(
			InvalidGoodsReceiptStateException exception
	) {
		return error(HttpStatus.CONFLICT, "INVALID_GOODS_RECEIPT_STATE", exception.getMessage());
	}

	@ExceptionHandler(InvalidInvoiceStateException.class)
	public ResponseEntity<ApiResponse<Void>> handleInvalidInvoiceState(InvalidInvoiceStateException exception) {
		return error(HttpStatus.CONFLICT, "INVALID_INVOICE_STATE", exception.getMessage());
	}

	@ExceptionHandler(GoodsReceiptCancellationBlockedException.class)
	public ResponseEntity<ApiResponse<Void>> handleGoodsReceiptCancellationBlocked(
			GoodsReceiptCancellationBlockedException exception
	) {
		return error(HttpStatus.CONFLICT, "GOODS_RECEIPT_CANCELLATION_BLOCKED", exception.getMessage());
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
