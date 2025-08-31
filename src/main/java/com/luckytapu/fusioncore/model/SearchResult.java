package com.luckytapu.fusioncore.model;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class SearchResult {
    private UUID fileId;
    private String fileName;
    private String chunkText;
    private double similarityScore;
    private int chunkIndex;
}