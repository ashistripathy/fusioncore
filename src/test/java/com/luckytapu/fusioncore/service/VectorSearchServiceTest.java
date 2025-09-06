package com.luckytapu.fusioncore.service;

import com.luckytapu.fusioncore.entity.ChunkEntity;
import com.luckytapu.fusioncore.entity.FileEntity;
import com.luckytapu.fusioncore.model.SearchResult;
import com.luckytapu.fusioncore.repository.ChunkRepository;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VectorSearchServiceTest {

    @Mock
    private ChunkRepository chunkRepository;
    
    @Mock
    private EmbeddingModel embeddingModel;

    private VectorSearchService vectorSearchService;

    @BeforeEach
    void setUp() {
        vectorSearchService = new VectorSearchService(chunkRepository, embeddingModel);
    }

    @Test
    void searchSimilarChunks_Success() {
        // Given
        String query = "test query";
        int limit = 5;
        UUID fileId = UUID.randomUUID();
        
        FileEntity fileEntity = FileEntity.builder()
                .fileId(fileId)
                .fileName("test.txt")
                .build();
        
        ChunkEntity chunkEntity = ChunkEntity.builder()
                .chunkId(UUID.randomUUID())
                .fileId(fileId)
                .text("test chunk content")
                .chunkIndex(0)
                .file(fileEntity)
                .build();
        
        Object[] searchResult = new Object[]{chunkEntity, new BigDecimal("0.85")};
        java.util.List<Object[]> mockResults = new java.util.ArrayList<>();
        mockResults.add(searchResult);
        
        when(embeddingModel.embed(query)).thenReturn(Response.from(Embedding.from(new float[]{0.1f, 0.2f})));
        when(chunkRepository.findSimilarChunks(any(byte[].class), eq(limit))).thenReturn(mockResults);

        // When
        List<SearchResult> results = vectorSearchService.searchSimilarChunks(query, limit);

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        SearchResult result = results.get(0);
        assertEquals(fileId, result.getFileId());
        assertEquals("test.txt", result.getFileName());
        assertEquals("test chunk content", result.getChunkText());
        assertEquals(0.85, result.getSimilarityScore());
        assertEquals(0, result.getChunkIndex());
        
        verify(embeddingModel).embed(query);
        verify(chunkRepository).findSimilarChunks(any(byte[].class), eq(limit));
    }

    @Test
    void searchSimilarChunks_NoEmbeddingModel() {
        // Given
        VectorSearchService serviceWithoutModel = new VectorSearchService(chunkRepository, null);

        // When
        List<SearchResult> results = serviceWithoutModel.searchSimilarChunks("test", 5);

        // Then
        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(chunkRepository, never()).findSimilarChunks(any(), anyInt());
    }

    @Test
    void searchSimilarChunks_EmbeddingFailure() {
        // Given
        when(embeddingModel.embed(anyString())).thenThrow(new RuntimeException("Embedding failed"));

        // When
        List<SearchResult> results = vectorSearchService.searchSimilarChunks("test", 5);

        // Then
        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(chunkRepository, never()).findSimilarChunks(any(), anyInt());
    }

    @Test
    void getDocumentChunks_WithStrategy() {
        // Given
        UUID fileId = UUID.randomUUID();
        String strategy = "character";
        List<ChunkEntity> mockChunks = List.of(
                ChunkEntity.builder().chunkId(UUID.randomUUID()).fileId(fileId).build()
        );
        
        when(chunkRepository.findByFileIdAndStrategy(fileId, strategy)).thenReturn(mockChunks);

        // When
        List<ChunkEntity> results = vectorSearchService.getDocumentChunks(fileId, strategy);

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        verify(chunkRepository).findByFileIdAndStrategy(fileId, strategy);
        verify(chunkRepository, never()).findByFileIdOrderByChunkIndex(any());
    }

    @Test
    void getDocumentChunks_WithoutStrategy() {
        // Given
        UUID fileId = UUID.randomUUID();
        List<ChunkEntity> mockChunks = List.of(
                ChunkEntity.builder().chunkId(UUID.randomUUID()).fileId(fileId).build()
        );
        
        when(chunkRepository.findByFileIdOrderByChunkIndex(fileId)).thenReturn(mockChunks);

        // When
        List<ChunkEntity> results = vectorSearchService.getDocumentChunks(fileId, null);

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        verify(chunkRepository).findByFileIdOrderByChunkIndex(fileId);
        verify(chunkRepository, never()).findByFileIdAndStrategy(any(), any());
    }
}