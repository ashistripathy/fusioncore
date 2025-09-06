package com.luckytapu.fusioncore.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationTest {

    @Test
    void cacheConfig_ClassExists() {
        // When & Then
        assertNotNull(CacheConfig.class);
        assertTrue(CacheConfig.class.isAnnotationPresent(org.springframework.context.annotation.Configuration.class));
    }

    @Test
    void asyncConfig_ClassExists() {
        // When & Then
        assertNotNull(com.luckytapu.fusioncore.config.AsyncConfig.class);
        assertTrue(com.luckytapu.fusioncore.config.AsyncConfig.class.isAnnotationPresent(org.springframework.context.annotation.Configuration.class));
    }

    @Test
    void langChain4jConfig_ClassExists() {
        // When & Then
        assertNotNull(com.luckytapu.fusioncore.config.LangChain4jConfig.class);
        assertTrue(com.luckytapu.fusioncore.config.LangChain4jConfig.class.isAnnotationPresent(org.springframework.context.annotation.Configuration.class));
    }
}