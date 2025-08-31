package com.luckytapu.fusioncore.model;

import com.luckytapu.fusioncore.entity.FileEntity;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DocumentProcessingResult {
    private FileEntity fileEntity;
    private ChunkingReport chunkingReport;
    private boolean embeddingsGenerated;
    private String processingStatus;
    private long processingTimeMs;
}