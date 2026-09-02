package com.dayoung.procurement;

import org.springframework.boot.SpringApplication;

public class TestProcurementApplication {

	public static void main(String[] args) {
		SpringApplication.from(ProcurementApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
