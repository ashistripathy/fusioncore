package com.luckytapu.fusioncore.integration;

import com.luckytapu.fusioncore.entity.FileEntity;
import com.luckytapu.fusioncore.model.DocumentProcessingResult;
import com.luckytapu.fusioncore.model.FileStatus;
import com.luckytapu.fusioncore.repository.FileRepository;
import com.luckytapu.fusioncore.service.FileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FileProcessingIntegrationTest {

    @Autowired
    private FileService fileService;

    @Autowired
    private FileRepository fileRepository;

    @Test
    void processDocument_EndToEnd_WithoutEmbeddings() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file", 
                "test-document.txt", 
                "text/plain", 
                "This is a comprehensive test document with multiple sentences. It contains enough content to test the chunking strategies effectively. The document should be processed and analyzed properly.".getBytes()
        );

        // When
        DocumentProcessingResult result = fileService.processDocument(file, false);

        // Then
        assertNotNull(result);
        assertEquals("SUCCESS", result.getProcessingStatus());
        assertFalse(result.isEmbeddingsGenerated());
        assertNotNull(result.getFileEntity());
        assertNotNull(result.getChunkingReport());
        assertTrue(result.getProcessingTimeMs() > 0);

        // Verify file is saved in database
        FileEntity savedFile = fileRepository.findById(result.getFileEntity().getFileId()).orElse(null);
        assertNotNull(savedFile);
        assertEquals("test-document.txt", savedFile.getFileName());
        assertEquals(FileStatus.PROCESSED, savedFile.getStatus());

        // Verify chunking report
        assertEquals("test-document.txt", result.getChunkingReport().getFileName());
        assertTrue(result.getChunkingReport().getTotalCharacters() > 0);
        assertFalse(result.getChunkingReport().getStrategies().isEmpty());
    }

    @Test
    void saveFile_Integration() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file", 
                "simple-test.txt", 
                "text/plain", 
                "Simple test content".getBytes()
        );

        // When
        FileEntity result = fileService.saveFile(file);

        // Then
        assertNotNull(result);
        assertNotNull(result.getFileId());
        assertEquals("simple-test.txt", result.getFileName());
        assertEquals("text/plain", result.getFileType());
        assertEquals(FileStatus.UPLOADED, result.getStatus());

        // Verify in database
        FileEntity fromDb = fileRepository.findById(result.getFileId()).orElse(null);
        assertNotNull(fromDb);
        assertEquals(result.getFileName(), fromDb.getFileName());
        assertArrayEquals("Simple test content".getBytes(), fromDb.getData());
    }

    @Test
    void updateFileStatus_Integration() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes());
        FileEntity savedFile = fileService.saveFile(file);
        assertEquals(FileStatus.UPLOADED, savedFile.getStatus());

        // When
        FileEntity updatedFile = fileService.updateFileStatus(savedFile.getFileId(), FileStatus.PROCESSED);

        // Then
        assertNotNull(updatedFile);
        assertEquals(FileStatus.PROCESSED, updatedFile.getStatus());

        // Verify in database
        FileEntity fromDb = fileRepository.findById(savedFile.getFileId()).orElse(null);
        assertNotNull(fromDb);
        assertEquals(FileStatus.PROCESSED, fromDb.getStatus());
    }
}