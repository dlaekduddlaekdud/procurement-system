package com.dayoung.procurement.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	private static final String BASIC_AUTH = "basicAuth";

	@Bean
	OpenAPI procurementOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("Procurement System API")
						.version("v1")
						.description("구매요청부터 입고, 송장, 원장 정정, 월 마감까지 제공하는 API"))
				.addSecurityItem(new SecurityRequirement().addList(BASIC_AUTH))
				.components(new Components().addSecuritySchemes(
						BASIC_AUTH,
						new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")
				));
	}
}
