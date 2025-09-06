package com.luckytapu.fusioncore.service;

import com.luckytapu.fusioncore.entity.FileEntity;
import com.luckytapu.fusioncore.model.FileStatus;
import com.luckytapu.fusioncore.repository.FileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileServiceImplUnitTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private ChunkService chunkService;

    @Mock
    private VectorSearchService vectorSearchService;

    @InjectMocks
    private FileServiceImpl fileService;

    @Test
    void saveFile_Success() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile("test", "test.txt", "text/plain", "content".getBytes());
        FileEntity expectedEntity = FileEntity.builder()
                .fileId(UUID.randomUUID())
                .fileName("test.txt")
                .fileType("text/plain")
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
    }
}