package com.luckytapu.fusioncore.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ModelTest {

    @Test
    void chunkingReport_BuilderAndGetters() {
        // Given
        ChunkingReport.ChunkingStrategy strategy = ChunkingReport.ChunkingStrategy.builder()
                .strategyName("character")
                .chunkCount(10)
                .averageChunkSize(100.0)
                .largestChunkSize(150)
                .smallestChunkSize(50)
                .medianChunkSize(95.0)
                .p85ChunkSize(130.0)
                .p95ChunkSize(145.0)
                .standardDeviation(25.5)
                .coefficientOfVariation(0.255)
                .qualityScore(0.85)
                .build();

        ChunkingReport report = ChunkingReport.builder()
                .fileName("test.txt")
                .totalCharacters(1000)
                .strategies(List.of(strategy))
                .build();

        // Then
        assertEquals("test.txt", report.getFileName());
        assertEquals(1000, report.getTotalCharacters());
        assertEquals(1, report.getStrategies().size());
        
        ChunkingReport.ChunkingStrategy retrievedStrategy = report.getStrategies().get(0);
        assertEquals("character", retrievedStrategy.getStrategyName());
        assertEquals(10, retrievedStrategy.getChunkCount());
        assertEquals(100.0, retrievedStrategy.getAverageChunkSize());
        assertEquals(0.85, retrievedStrategy.getQualityScore());
    }

    @Test
    void documentProcessingResult_BuilderAndGetters() {
        // Given
        DocumentProcessingResult result = DocumentProcessingResult.builder()
                .processingStatus("SUCCESS")
                .embeddingsGenerated(true)
                .processingTimeMs(1500L)
                .build();

        // Then
        assertEquals("SUCCESS", result.getProcessingStatus());
        assertTrue(result.isEmbeddingsGenerated());
        assertEquals(1500L, result.getProcessingTimeMs());
    }

    @Test
    void searchResult_BuilderAndGetters() {
        // Given
        UUID fileId = UUID.randomUUID();
        SearchResult result = SearchResult.builder()
                .fileId(fileId)
                .fileName("document.pdf")
                .chunkText("This is a relevant chunk of text")
                .similarityScore(0.92)
                .chunkIndex(5)
                .build();

        // Then
        assertEquals(fileId, result.getFileId());
        assertEquals("document.pdf", result.getFileName());
        assertEquals("This is a relevant chunk of text", result.getChunkText());
        assertEquals(0.92, result.getSimilarityScore());
        assertEquals(5, result.getChunkIndex());
    }

    @Test
    void fileStatus_EnumValues() {
        // When & Then
        assertEquals("UPLOADED", FileStatus.UPLOADED.name());
        assertEquals("PROCESSED", FileStatus.PROCESSED.name());
        assertEquals("FAILED", FileStatus.FAILED.name());
    }

    @Test
    void chunkingStrategy_AllFields() {
        // Given
        ChunkingReport.ChunkingStrategy strategy = ChunkingReport.ChunkingStrategy.builder()
                .strategyName("sentence")
                .description("Sentence-based chunking")
                .chunkCount(5)
                .averageChunkSize(200.0)
                .largestChunkSize(300)
                .smallestChunkSize(100)
                .embeddingTestPassed(true)
                .embeddingTestResult("PASSED")
                .medianChunkSize(190.0)
                .p85ChunkSize(250.0)
                .p95ChunkSize(280.0)
                .standardDeviation(50.0)
                .coefficientOfVariation(0.25)
                .qualityScore(0.9)
                .build();

        // Then
        assertEquals("sentence", strategy.getStrategyName());
        assertEquals("Sentence-based chunking", strategy.getDescription());
        assertEquals(5, strategy.getChunkCount());
        assertEquals(200.0, strategy.getAverageChunkSize());
        assertEquals(300, strategy.getLargestChunkSize());
        assertEquals(100, strategy.getSmallestChunkSize());
        assertTrue(strategy.isEmbeddingTestPassed());
        assertEquals("PASSED", strategy.getEmbeddingTestResult());
        assertEquals(190.0, strategy.getMedianChunkSize());
        assertEquals(250.0, strategy.getP85ChunkSize());
        assertEquals(280.0, strategy.getP95ChunkSize());
        assertEquals(50.0, strategy.getStandardDeviation());
        assertEquals(0.25, strategy.getCoefficientOfVariation());
        assertEquals(0.9, strategy.getQualityScore());
    }
}