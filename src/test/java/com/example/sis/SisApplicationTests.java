package com.example.sis;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Basic Spring Boot application context test
 * Verifies that the application context loads successfully
 */
@SpringBootTest
@ActiveProfiles("test")
class SisApplicationTests {
	
	@Test
	void contextLoads() {
		// This test will pass if the application context loads successfully
		// It's a smoke test to ensure basic configuration is correct
	}
}
