package com.dayoung.procurement.ledger.web;

import com.dayoung.procurement.common.api.ApiResponse;
import com.dayoung.procurement.ledger.application.LedgerCorrectionService;
import com.dayoung.procurement.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accrual-entries")
public class LedgerCorrectionController {

	private final LedgerCorrectionService ledgerCorrectionService;

	public LedgerCorrectionController(LedgerCorrectionService ledgerCorrectionService) {
		this.ledgerCorrectionService = ledgerCorrectionService;
	}

	@PostMapping("/{entryId}/reverse")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('ADMIN')")
	public ApiResponse<CreatedAccrualEntryResponse> reverse(
			@AuthenticationPrincipal AuthenticatedUser user,
			@PathVariable Long entryId,
			@Valid @RequestBody ReverseAccrualEntryRequest request
	) {
		Long reversalId = ledgerCorrectionService.reverse(entryId, user.getUserId(), request.toCommand());
		return ApiResponse.success(new CreatedAccrualEntryResponse(reversalId));
	}

	@PostMapping("/{reversalId}/repost")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('ADMIN')")
	public ApiResponse<CreatedAccrualEntryResponse> repost(
			@AuthenticationPrincipal AuthenticatedUser user,
			@PathVariable Long reversalId,
			@Valid @RequestBody RepostAccrualEntryRequest request
	) {
		Long correctionId = ledgerCorrectionService.repost(reversalId, user.getUserId(), request.toCommand());
		return ApiResponse.success(new CreatedAccrualEntryResponse(correctionId));
	}
}
