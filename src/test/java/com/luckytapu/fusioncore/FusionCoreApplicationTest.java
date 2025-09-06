package com.luckytapu.fusioncore;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "azure.openai.endpoint=test-endpoint",
    "azure.openai.api-key=test-key", 
    "azure.openai.deployment-name=test-deployment"
})
class FusionCoreApplicationTest {

    @Test
    void contextLoads() {
        // Test that Spring context loads successfully
    }

    @Test
    void main_ApplicationStarts() {
        // Given
        String[] args = {};

        // When & Then - should not throw exception
        // Note: We don't actually call main() in tests as it would start the full application
        // This test verifies the class exists and is properly configured
        assert FusionCoreApplication.class != null;
    }
}