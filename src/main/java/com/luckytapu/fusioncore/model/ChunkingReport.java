package com.luckytapu.fusioncore.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChunkingReport {
    private String fileName;
    private int totalCharacters;
    private List<ChunkingStrategy> strategies;

    @Data
    @Builder
    public static class ChunkingStrategy {
        private String strategyName;
        private String description;
        private int chunkCount;
        private double averageChunkSize;
        private int largestChunkSize;
        private int smallestChunkSize;
        private boolean embeddingTestPassed;
        private String embeddingTestResult;
        
        // Enhanced metrics for better decision making
        private double medianChunkSize;      // P50
        private double p85ChunkSize;         // 85th percentile
        private double p95ChunkSize;         // 95th percentile
        private double standardDeviation;    // Consistency measure
        private double coefficientOfVariation; // Normalized consistency
        private double qualityScore;         // Overall strategy score
    }
}