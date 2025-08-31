package com.luckytapu.fusioncore.service;

import com.luckytapu.fusioncore.entity.ChunkEntity;
import com.luckytapu.fusioncore.model.SearchResult;
import com.luckytapu.fusioncore.repository.ChunkRepository;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class VectorSearchService {
    
    private final ChunkRepository chunkRepository;
    private final EmbeddingModel embeddingModel;
    
    public List<SearchResult> searchSimilarChunks(String query, int limit) {
        if (embeddingModel == null) {
            log.warn("EmbeddingModel not available for search");
            return List.of();
        }
        
        try {
            // Generate embedding for query
            Response<Embedding> queryEmbeddingResponse = embeddingModel.embed(query);
            if (queryEmbeddingResponse.content() == null) {
                log.warn("Failed to generate embedding for query");
                return List.of();
            }
            
            byte[] queryEmbedding = convertToBytes(queryEmbeddingResponse.content().vector());
            
            // Search similar chunks
            List<Object[]> results = chunkRepository.findSimilarChunks(queryEmbedding, limit);
            
            List<SearchResult> searchResults = new ArrayList<>();
            for (Object[] result : results) {
                ChunkEntity chunk = (ChunkEntity) result[0];
                BigDecimal similarity = (BigDecimal) result[1];
                
                SearchResult searchResult = SearchResult.builder()
                        .fileId(chunk.getFileId())
                        .fileName(chunk.getFile() != null ? chunk.getFile().getFileName() : "Unknown")
                        .chunkText(chunk.getText())
                        .similarityScore(similarity.doubleValue())
                        .chunkIndex(chunk.getChunkIndex())
                        .build();
                
                searchResults.add(searchResult);
            }
            
            return searchResults;
            
        } catch (Exception e) {
            log.error("Vector search failed: {}", e.getMessage(), e);
            return List.of();
        }
    }
    
    public List<ChunkEntity> getDocumentChunks(UUID fileId, String strategy) {
        if (strategy != null) {
            return chunkRepository.findByFileIdAndStrategy(fileId, strategy);
        }
        return chunkRepository.findByFileIdOrderByChunkIndex(fileId);
    }
    
    private byte[] convertToBytes(float[] vector) {
        byte[] bytes = new byte[vector.length * 4];
        for (int i = 0; i < vector.length; i++) {
            int bits = Float.floatToIntBits(vector[i]);
            bytes[i * 4] = (byte) (bits >> 24);
            bytes[i * 4 + 1] = (byte) (bits >> 16);
            bytes[i * 4 + 2] = (byte) (bits >> 8);
            bytes[i * 4 + 3] = (byte) bits;
        }
        return bytes;
    }
}