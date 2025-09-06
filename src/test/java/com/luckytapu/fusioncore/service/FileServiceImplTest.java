package com.luckytapu.fusioncore.service;

import com.luckytapu.fusioncore.entity.FileEntity;
import com.luckytapu.fusioncore.model.ChunkingReport;
import com.luckytapu.fusioncore.model.DocumentProcessingResult;
import com.luckytapu.fusioncore.model.FileStatus;
import com.luckytapu.fusioncore.repository.ChunkRepository;
import com.luckytapu.fusioncore.repository.FileRepository;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {

    @Mock
    private FileRepository fileRepository;
    
    @Mock
    private EmbeddingModel embeddingModel;
    
    @Mock
    private ChunkRepository chunkRepository;
    
    @Mock
    private VectorSearchService vectorSearchService;
    
    @Mock
    private ChunkService chunkService;

    private FileServiceImpl fileService;

    @BeforeEach
    void setUp() {
        fileService = new FileServiceImpl(fileRepository, embeddingModel, chunkRepository, vectorSearchService, chunkService);
        ReflectionTestUtils.setField(fileService, "testEmbeddingStrategies", false);
        ReflectionTestUtils.setField(fileService, "defaultStrategy", "character");
    }

    @Test
    void saveFile_Success() throws IOException {
        // Given
        MockMultipartFile file = new MockMultipartFile("test", "test.txt", "text/plain", "test content".getBytes());
        FileEntity expectedEntity = FileEntity.builder()
                .fileName("test.txt")
                .fileType("text/plain")
                .data("test content".getBytes())
                .status(FileStatus.UPLOADED)
                .build();
        
        when(fileRepository.save(any(FileEntity.class))).thenReturn(expectedEntity);

        // When
        FileEntity result = fileService.saveFile(file);

        // Then
        assertNotNull(result);
        assertEquals("test.txt", result.getFileName());
        assertEquals("text/plain", result.getFileType());
        assertEquals(FileStatus.UPLOADED, result.getStatus());
        verify(fileRepository).save(any(FileEntity.class));
    }

    @Test
    void updateFileStatus_Success() {
        // Given
        UUID fileId = UUID.randomUUID();
        FileEntity fileEntity = FileEntity.builder()
                .fileId(fileId)
                .fileName("test.txt")
                .status(FileStatus.UPLOADED)
                .build();
        
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(fileEntity));
        when(fileRepository.save(any(FileEntity.class))).thenReturn(fileEntity);

        // When
        FileEntity result = fileService.updateFileStatus(fileId, FileStatus.PROCESSED);

        // Then
        assertNotNull(result);
        assertEquals(FileStatus.PROCESSED, result.getStatus());
        verify(fileRepository).findById(fileId);
        verify(fileRepository).save(fileEntity);
    }

    @Test
    void updateFileStatus_FileNotFound() {
        // Given
        UUID fileId = UUID.randomUUID();
        when(fileRepository.findById(fileId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> fileService.updateFileStatus(fileId, FileStatus.PROCESSED));
        verify(fileRepository).findById(fileId);
        verify(fileRepository, never()).save(any());
    }

    @Test
    void processDocument_WithEmbeddings_Success() throws IOException {
        // Given
        MockMultipartFile file = new MockMultipartFile("test", "test.txt", "text/plain", "test content for processing".getBytes());
        UUID fileId = UUID.randomUUID();
        FileEntity savedFile = FileEntity.builder()
                .fileId(fileId)
                .fileName("test.txt")
                .status(FileStatus.UPLOADED)
                .build();
        
        when(fileRepository.save(any(FileEntity.class))).thenReturn(savedFile);
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(savedFile));
        when(chunkService.generateAndStoreChunkEmbeddings(any(), any(), any())).thenReturn(true);
        when(embeddingModel.embed(any(dev.langchain4j.data.segment.TextSegment.class))).thenReturn(Response.from(Embedding.from(new float[]{0.1f, 0.2f})));

        // When
        DocumentProcessingResult result = fileService.processDocument(file, true);

        // Then
        assertNotNull(result);
        assertEquals("SUCCESS", result.getProcessingStatus());
        assertTrue(result.isEmbeddingsGenerated());
        assertNotNull(result.getChunkingReport());
        verify(fileRepository, atLeastOnce()).save(any(FileEntity.class));
        verify(chunkService).generateAndStoreChunkEmbeddings(any(), any(), any());
    }

    @Test
    void processDocument_WithoutEmbeddings_Success() throws IOException {
        // Given
        MockMultipartFile file = new MockMultipartFile("test", "test.txt", "text/plain", "test content".getBytes());
        UUID fileId = UUID.randomUUID();
        FileEntity savedFile = FileEntity.builder()
                .fileId(fileId)
                .fileName("test.txt")
                .status(FileStatus.UPLOADED)
                .build();
        
        when(fileRepository.save(any(FileEntity.class))).thenReturn(savedFile);
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(savedFile));

        // When
        DocumentProcessingResult result = fileService.processDocument(file, false);

        // Then
        assertNotNull(result);
        assertEquals("SUCCESS", result.getProcessingStatus());
        assertFalse(result.isEmbeddingsGenerated());
        verify(fileRepository, atLeastOnce()).save(any(FileEntity.class));
        verify(chunkService, never()).generateAndStoreChunkEmbeddings(any(), any(), any());
    }

    @Test
    void processDocument_Exception_ReturnsFailedResult() throws IOException {
        // Given
        MockMultipartFile file = new MockMultipartFile("test", "test.txt", "text/plain", "test content".getBytes());
        when(fileRepository.save(any(FileEntity.class))).thenThrow(new RuntimeException("Database error"));

        // When
        DocumentProcessingResult result = fileService.processDocument(file, true);

        // Then
        assertNotNull(result);
        assertTrue(result.getProcessingStatus().startsWith("FAILED:"));
        assertFalse(result.isEmbeddingsGenerated());
    }
}