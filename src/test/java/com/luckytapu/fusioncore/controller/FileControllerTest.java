package com.luckytapu.fusioncore.controller;

import com.luckytapu.fusioncore.entity.FileEntity;
import com.luckytapu.fusioncore.model.ChunkingReport;
import com.luckytapu.fusioncore.model.DocumentProcessingResult;
import com.luckytapu.fusioncore.model.FileStatus;
import com.luckytapu.fusioncore.model.SearchResult;
import com.luckytapu.fusioncore.service.FileService;
import com.luckytapu.fusioncore.service.VectorSearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class FileControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FileService fileService;

    @Mock
    private VectorSearchService vectorSearchService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(new FileController(fileService, vectorSearchService)).build();
    }

    @Test
    void uploadFile_Success() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "test content".getBytes());
        FileEntity expectedEntity = FileEntity.builder()
                .fileId(UUID.randomUUID())
                .fileName("test.txt")
                .fileType("text/plain")
                .status(FileStatus.UPLOADED)
                .build();

        when(fileService.saveFile(any())).thenReturn(expectedEntity);

        // When & Then
        mockMvc.perform(multipart("/api/files/upload")
                .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("test.txt"))
                .andExpect(jsonPath("$.fileType").value("text/plain"))
                .andExpect(jsonPath("$.status").value("UPLOADED"));
    }

    @Test
    void analyzeDocument_Success() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "test content".getBytes());
        ChunkingReport report = ChunkingReport.builder()
                .fileName("test.txt")
                .totalCharacters(12)
                .strategies(List.of())
                .build();

        when(fileService.processAndAnalyzeDocument(any())).thenReturn(report);

        // When & Then
        mockMvc.perform(multipart("/api/files/analyze")
                .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("test.txt"))
                .andExpect(jsonPath("$.totalCharacters").value(12));
    }

    @Test
    void processDocument_Success() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "test content".getBytes());
        DocumentProcessingResult result = DocumentProcessingResult.builder()
                .processingStatus("SUCCESS")
                .embeddingsGenerated(true)
                .processingTimeMs(1000L)
                .build();

        when(fileService.processDocument(any(), eq(true))).thenReturn(result);

        // When & Then
        mockMvc.perform(multipart("/api/files/process")
                .file(file)
                .param("generateEmbeddings", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processingStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.embeddingsGenerated").value(true))
                .andExpect(jsonPath("$.processingTimeMs").value(1000));
    }

    @Test
    void processDocument_WithoutEmbeddings() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "test content".getBytes());
        DocumentProcessingResult result = DocumentProcessingResult.builder()
                .processingStatus("SUCCESS")
                .embeddingsGenerated(false)
                .processingTimeMs(500L)
                .build();

        when(fileService.processDocument(any(), eq(false))).thenReturn(result);

        // When & Then
        mockMvc.perform(multipart("/api/files/process")
                .file(file)
                .param("generateEmbeddings", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processingStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.embeddingsGenerated").value(false));
    }

    @Test
    void searchDocuments_Success() throws Exception {
        // Given
        List<SearchResult> searchResults = List.of(
                SearchResult.builder()
                        .fileId(UUID.randomUUID())
                        .fileName("test.txt")
                        .chunkText("relevant content")
                        .similarityScore(0.85)
                        .chunkIndex(0)
                        .build()
        );

        when(fileService.searchDocuments("test query", 10)).thenReturn(searchResults);

        // When & Then
        mockMvc.perform(get("/api/files/search")
                .param("query", "test query")
                .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fileName").value("test.txt"))
                .andExpect(jsonPath("$[0].chunkText").value("relevant content"))
                .andExpect(jsonPath("$[0].similarityScore").value(0.85))
                .andExpect(jsonPath("$[0].chunkIndex").value(0));
    }

    @Test
    void searchDocuments_DefaultLimit() throws Exception {
        // Given
        when(fileService.searchDocuments("test", 10)).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/files/search")
                .param("query", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getDocumentChunks_Success() throws Exception {
        // Given
        UUID fileId = UUID.randomUUID();
        when(vectorSearchService.getDocumentChunks(fileId, null)).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/files/{fileId}/chunks", fileId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getDocumentChunks_WithStrategy() throws Exception {
        // Given
        UUID fileId = UUID.randomUUID();
        when(vectorSearchService.getDocumentChunks(fileId, "character")).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/files/{fileId}/chunks", fileId)
                .param("strategy", "character"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void processDocumentAsync_Success() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "test content".getBytes());
        DocumentProcessingResult result = DocumentProcessingResult.builder()
                .processingStatus("SUCCESS")
                .embeddingsGenerated(true)
                .processingTimeMs(1000L)
                .build();

        when(fileService.processDocumentAsync(any(), eq(true)))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(result));

        // When & Then
        mockMvc.perform(multipart("/api/files/process-async")
                .file(file)
                .param("generateEmbeddings", "true"))
                .andExpect(status().isOk());
    }

    @Test
    void processBatch_Success() throws Exception {
        // Given
        MockMultipartFile file1 = new MockMultipartFile("files", "test1.txt", "text/plain", "content1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("files", "test2.txt", "text/plain", "content2".getBytes());
        
        List<DocumentProcessingResult> results = List.of(
                DocumentProcessingResult.builder()
                        .processingStatus("SUCCESS")
                        .embeddingsGenerated(true)
                        .build(),
                DocumentProcessingResult.builder()
                        .processingStatus("SUCCESS")
                        .embeddingsGenerated(true)
                        .build()
        );

        when(fileService.processBatch(any(), eq(true))).thenReturn(results);

        // When & Then
        mockMvc.perform(multipart("/api/files/batch-process")
                .file(file1)
                .file(file2)
                .param("generateEmbeddings", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void uploadFile_EmptyFile_Success() throws Exception {
        // Given
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);
        FileEntity expectedEntity = FileEntity.builder()
                .fileId(UUID.randomUUID())
                .fileName("empty.txt")
                .fileType("text/plain")
                .status(FileStatus.UPLOADED)
                .build();

        when(fileService.saveFile(any())).thenReturn(expectedEntity);

        // When & Then
        mockMvc.perform(multipart("/api/files/upload")
                .file(emptyFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("empty.txt"));
    }

    @Test
    void searchDocuments_EmptyQuery_Success() throws Exception {
        // Given
        when(fileService.searchDocuments("", 10)).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/files/search")
                .param("query", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}