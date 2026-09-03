package com.dayoung.procurement.closing.web;

import com.dayoung.procurement.closing.application.CloseService;
import com.dayoung.procurement.common.api.ApiResponse;
import com.dayoung.procurement.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/close-periods")
public class ClosePeriodController {

	private final CloseService closeService;

	public ClosePeriodController(CloseService closeService) {
		this.closeService = closeService;
	}

	@PostMapping("/{period}/close")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('ADMIN')")
	public ApiResponse<CreatedCloseRunResponse> close(
			@AuthenticationPrincipal AuthenticatedUser user,
			@PathVariable String period
	) {
		Long closeRunId = closeService.closeManually(period, user.getUserId());
		return ApiResponse.success(new CreatedCloseRunResponse(closeRunId));
	}
}
