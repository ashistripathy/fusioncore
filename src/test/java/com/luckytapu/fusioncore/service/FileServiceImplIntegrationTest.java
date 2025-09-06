package com.luckytapu.fusioncore.service;

import com.luckytapu.fusioncore.model.ChunkingReport;
import com.luckytapu.fusioncore.model.DocumentProcessingResult;
import com.luckytapu.fusioncore.model.SearchResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "azure.openai.endpoint=test-endpoint",
    "azure.openai.api-key=test-key",
    "azure.openai.deployment-name=test-deployment",
    "fusioncore.embedding.test-all-strategies=false",
    "fusioncore.embedding.test-embeddings=false"
})
class FileServiceImplIntegrationTest {

    @Autowired
    private FileService fileService;

    @Test
    void processAndAnalyzeDocument_Success() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.txt", "text/plain", 
            "This is a test document with multiple sentences. It contains enough content for chunking analysis.".getBytes()
        );

        // When
        ChunkingReport report = fileService.processAndAnalyzeDocument(file);

        // Then
        assertNotNull(report);
        assertEquals("test.txt", report.getFileName());
        assertTrue(report.getTotalCharacters() > 0);
        assertFalse(report.getStrategies().isEmpty());
    }

    @Test
    void processDocumentAsync_Success() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
            "file", "async-test.txt", "text/plain", 
            "Async processing test content.".getBytes()
        );

        // When
        CompletableFuture<DocumentProcessingResult> future = fileService.processDocumentAsync(file, false);
        DocumentProcessingResult result = future.get();

        // Then
        assertNotNull(result);
        assertEquals("SUCCESS", result.getProcessingStatus());
        assertFalse(result.isEmbeddingsGenerated());
    }

    @Test
    void processBatch_Success() {
        // Given
        List<org.springframework.web.multipart.MultipartFile> files = List.of(
            new MockMultipartFile("file1", "batch1.txt", "text/plain", "Batch content 1".getBytes()),
            new MockMultipartFile("file2", "batch2.txt", "text/plain", "Batch content 2".getBytes())
        );

        // When
        List<DocumentProcessingResult> results = fileService.processBatch(files, false);

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
        results.forEach(result -> {
            assertEquals("SUCCESS", result.getProcessingStatus());
            assertFalse(result.isEmbeddingsGenerated());
        });
    }

    @Test
    void searchDocuments_ReturnsEmptyList() {
        // When
        List<SearchResult> results = fileService.searchDocuments("nonexistent query", 10);

        // Then
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }
}