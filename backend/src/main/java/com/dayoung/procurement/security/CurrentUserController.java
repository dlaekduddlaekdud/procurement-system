package com.dayoung.procurement.security;

import com.dayoung.procurement.common.api.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class CurrentUserController {

	@GetMapping("/me")
	public ApiResponse<CurrentUserResponse> me(@AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse.success(CurrentUserResponse.from(user));
	}
}
