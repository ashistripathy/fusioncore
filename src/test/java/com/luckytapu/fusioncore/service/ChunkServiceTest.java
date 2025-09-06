package com.luckytapu.fusioncore.service;

import com.luckytapu.fusioncore.entity.ChunkEntity;
import com.luckytapu.fusioncore.repository.ChunkRepository;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChunkServiceTest {

    @Mock
    private ChunkRepository chunkRepository;
    
    @Mock
    private EmbeddingModel embeddingModel;

    private ChunkService chunkService;

    @BeforeEach
    void setUp() {
        chunkService = new ChunkService(chunkRepository, embeddingModel);
    }

    @Test
    void generateAndStoreChunkEmbeddings_Success() {
        // Given
        Document document = Document.from("This is a test document with some content for chunking.");
        UUID fileId = UUID.randomUUID();
        String strategy = "Character Splitter";
        
        when(embeddingModel.embed(any(dev.langchain4j.data.segment.TextSegment.class))).thenReturn(Response.from(Embedding.from(new float[]{0.1f, 0.2f, 0.3f})));
        when(chunkRepository.save(any(ChunkEntity.class))).thenReturn(new ChunkEntity());

        // When
        boolean result = chunkService.generateAndStoreChunkEmbeddings(document, fileId, strategy);

        // Then
        assertTrue(result);
        verify(chunkRepository).deleteByFileId(fileId);
        verify(chunkRepository, atLeastOnce()).save(any(ChunkEntity.class));
        verify(embeddingModel, atLeastOnce()).embed(any(dev.langchain4j.data.segment.TextSegment.class));
    }

    @Test
    void generateAndStoreChunkEmbeddings_NoEmbeddingModel() {
        // Given
        ChunkService serviceWithoutModel = new ChunkService(chunkRepository, null);
        Document document = Document.from("Test content");
        UUID fileId = UUID.randomUUID();

        // When
        boolean result = serviceWithoutModel.generateAndStoreChunkEmbeddings(document, fileId, "character");

        // Then
        assertFalse(result);
        verify(chunkRepository, never()).save(any());
    }

    @Test
    void generateAndStoreChunkEmbeddings_EmbeddingFailure() {
        // Given
        Document document = Document.from("Test content");
        UUID fileId = UUID.randomUUID();
        
        when(embeddingModel.embed(any(dev.langchain4j.data.segment.TextSegment.class))).thenThrow(new RuntimeException("Embedding failed"));

        // When
        boolean result = chunkService.generateAndStoreChunkEmbeddings(document, fileId, "character");

        // Then
        assertTrue(result); // Should still return true as it continues processing other chunks
        verify(chunkRepository).deleteByFileId(fileId);
    }

    @Test
    void generateAndStoreChunkEmbeddings_EmptyDocument() {
        // Given
        Document document = Document.from("short");
        UUID fileId = UUID.randomUUID();
        
        when(embeddingModel.embed(any(dev.langchain4j.data.segment.TextSegment.class))).thenReturn(Response.from(Embedding.from(new float[]{0.1f})));
        when(chunkRepository.save(any(ChunkEntity.class))).thenReturn(new ChunkEntity());

        // When
        boolean result = chunkService.generateAndStoreChunkEmbeddings(document, fileId, "character");

        // Then
        assertTrue(result);
        verify(chunkRepository).deleteByFileId(fileId);
        verify(chunkRepository, atLeastOnce()).save(any(ChunkEntity.class));
    }
}