package com.luckytapu.fusioncore.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChunkingReportTest {

    @Test
    void chunkingReport_Builder_Success() {
        // Given
        List<ChunkingReport.ChunkingStrategy> strategies = List.of(
                ChunkingReport.ChunkingStrategy.builder()
                        .strategyName("Character Splitter")
                        .description("Test description")
                        .chunkCount(5)
                        .averageChunkSize(100.0)
                        .largestChunkSize(150)
                        .smallestChunkSize(50)
                        .medianChunkSize(100.0)
                        .p85ChunkSize(140.0)
                        .p95ChunkSize(145.0)
                        .standardDeviation(25.0)
                        .coefficientOfVariation(0.25)
                        .qualityScore(15.0)
                        .embeddingTestPassed(true)
                        .embeddingTestResult("Success")
                        .build()
        );

        // When
        ChunkingReport report = ChunkingReport.builder()
                .fileName("test.txt")
                .totalCharacters(500)
                .strategies(strategies)
                .build();

        // Then
        assertNotNull(report);
        assertEquals("test.txt", report.getFileName());
        assertEquals(500, report.getTotalCharacters());
        assertEquals(1, report.getStrategies().size());

        ChunkingReport.ChunkingStrategy strategy = report.getStrategies().get(0);
        assertEquals("Character Splitter", strategy.getStrategyName());
        assertEquals("Test description", strategy.getDescription());
        assertEquals(5, strategy.getChunkCount());
        assertEquals(100.0, strategy.getAverageChunkSize());
        assertEquals(150, strategy.getLargestChunkSize());
        assertEquals(50, strategy.getSmallestChunkSize());
        assertEquals(100.0, strategy.getMedianChunkSize());
        assertEquals(140.0, strategy.getP85ChunkSize());
        assertEquals(145.0, strategy.getP95ChunkSize());
        assertEquals(25.0, strategy.getStandardDeviation());
        assertEquals(0.25, strategy.getCoefficientOfVariation());
        assertEquals(15.0, strategy.getQualityScore());
        assertTrue(strategy.isEmbeddingTestPassed());
        assertEquals("Success", strategy.getEmbeddingTestResult());
    }

    @Test
    void chunkingStrategy_DefaultValues() {
        // When
        ChunkingReport.ChunkingStrategy strategy = ChunkingReport.ChunkingStrategy.builder()
                .strategyName("Test Strategy")
                .build();

        // Then
        assertNotNull(strategy);
        assertEquals("Test Strategy", strategy.getStrategyName());
        assertEquals(0, strategy.getChunkCount());
        assertEquals(0.0, strategy.getAverageChunkSize());
        assertFalse(strategy.isEmbeddingTestPassed());
    }

    @Test
    void chunkingReport_EmptyStrategies() {
        // When
        ChunkingReport report = ChunkingReport.builder()
                .fileName("empty.txt")
                .totalCharacters(0)
                .strategies(List.of())
                .build();

        // Then
        assertNotNull(report);
        assertEquals("empty.txt", report.getFileName());
        assertEquals(0, report.getTotalCharacters());
        assertTrue(report.getStrategies().isEmpty());
    }
}