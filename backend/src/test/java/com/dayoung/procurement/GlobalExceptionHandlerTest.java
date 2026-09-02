package com.dayoung.procurement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.dayoung.procurement.common.api.ApiResponse;
import com.dayoung.procurement.common.api.GlobalExceptionHandler;
import com.dayoung.procurement.purchase.domain.PurchaseRequestStatus;
import com.dayoung.procurement.purchase.exception.InvalidPurchaseRequestStateException;
import com.dayoung.procurement.purchase.exception.PurchaseRequestAccessDeniedException;
import com.dayoung.procurement.purchase.exception.PurchaseRequestNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

	private GlobalExceptionHandler exceptionHandler;

	@BeforeEach
	void setUp() {
		exceptionHandler = new GlobalExceptionHandler();
	}

	@Test
	void convertsNotFoundExceptionToNotFoundResponse() {
		ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handlePurchaseRequestNotFound(
				new PurchaseRequestNotFoundException(1L)
		);

		assertError(response, HttpStatus.NOT_FOUND, "PURCHASE_REQUEST_NOT_FOUND");
	}

	@Test
	void convertsAccessDeniedExceptionToForbiddenResponse() {
		ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleForbidden(
				new PurchaseRequestAccessDeniedException(1L)
		);

		assertError(response, HttpStatus.FORBIDDEN, "FORBIDDEN");
	}

	@Test
	void convertsInvalidStateExceptionToConflictResponse() {
		ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleInvalidPurchaseRequestState(
				new InvalidPurchaseRequestStateException(PurchaseRequestStatus.APPROVED, "제출")
		);

		assertError(response, HttpStatus.CONFLICT, "INVALID_PURCHASE_REQUEST_STATE");
	}

	private void assertError(
			ResponseEntity<ApiResponse<Void>> response,
			HttpStatus expectedStatus,
			String expectedCode
	) {
		ApiResponse<Void> body = response.getBody();

		assertEquals(expectedStatus, response.getStatusCode());
		assertFalse(body.success());
		assertNull(body.data());
		assertEquals(expectedCode, body.error().code());
	}
}
