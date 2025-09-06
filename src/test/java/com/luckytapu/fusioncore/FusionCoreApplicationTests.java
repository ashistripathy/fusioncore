package com.luckytapu.fusioncore;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class FusionCoreApplicationTests {

	@Test
	void contextLoads() {
		// Test that Spring context loads successfully
		assertTrue(true);
	}

	@Test
	void applicationStarts() {
		// Test that the application can start without errors
		assertTrue(true, "Application should start successfully");
	}
}
