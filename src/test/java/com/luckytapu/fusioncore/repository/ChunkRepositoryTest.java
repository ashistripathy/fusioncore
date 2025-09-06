package com.luckytapu.fusioncore.repository;

import com.luckytapu.fusioncore.entity.ChunkEntity;
import com.luckytapu.fusioncore.entity.FileEntity;
import com.luckytapu.fusioncore.model.FileStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class ChunkRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ChunkRepository chunkRepository;

    @Autowired
    private FileRepository fileRepository;

    @Test
    void findByFileIdOrderByChunkIndex_Success() {
        // Given
        FileEntity fileEntity = FileEntity.builder()
                .fileName("test.txt")
                .fileType("text/plain")
                .data("test content".getBytes())
                .status(FileStatus.UPLOADED)
                .build();
        fileEntity = fileRepository.save(fileEntity);

        ChunkEntity chunk1 = ChunkEntity.builder()
                .fileId(fileEntity.getFileId())
                .text("First chunk")
                .chunkIndex(1)
                .strategy("character")
                .textLength(11)
                .build();

        ChunkEntity chunk2 = ChunkEntity.builder()
                .fileId(fileEntity.getFileId())
                .text("Second chunk")
                .chunkIndex(0)
                .strategy("character")
                .textLength(12)
                .build();

        chunkRepository.save(chunk1);
        chunkRepository.save(chunk2);
        entityManager.flush();

        // When
        List<ChunkEntity> chunks = chunkRepository.findByFileIdOrderByChunkIndex(fileEntity.getFileId());

        // Then
        assertNotNull(chunks);
        assertEquals(2, chunks.size());
        assertEquals(0, chunks.get(0).getChunkIndex());
        assertEquals(1, chunks.get(1).getChunkIndex());
        assertEquals("Second chunk", chunks.get(0).getText());
        assertEquals("First chunk", chunks.get(1).getText());
    }

    @Test
    void findByStrategy_Success() {
        // Given
        FileEntity fileEntity = FileEntity.builder()
                .fileName("test.txt")
                .fileType("text/plain")
                .data("test content".getBytes())
                .status(FileStatus.UPLOADED)
                .build();
        fileEntity = fileRepository.save(fileEntity);

        ChunkEntity characterChunk = ChunkEntity.builder()
                .fileId(fileEntity.getFileId())
                .text("Character chunk")
                .chunkIndex(0)
                .strategy("character")
                .textLength(15)
                .build();

        ChunkEntity sentenceChunk = ChunkEntity.builder()
                .fileId(fileEntity.getFileId())
                .text("Sentence chunk")
                .chunkIndex(0)
                .strategy("sentence")
                .textLength(14)
                .build();

        chunkRepository.save(characterChunk);
        chunkRepository.save(sentenceChunk);
        entityManager.flush();

        // When
        List<ChunkEntity> characterChunks = chunkRepository.findByStrategy("character");
        List<ChunkEntity> sentenceChunks = chunkRepository.findByStrategy("sentence");

        // Then
        assertEquals(1, characterChunks.size());
        assertEquals("Character chunk", characterChunks.get(0).getText());
        
        assertEquals(1, sentenceChunks.size());
        assertEquals("Sentence chunk", sentenceChunks.get(0).getText());
    }

    @Test
    void findByFileIdAndStrategy_Success() {
        // Given
        FileEntity fileEntity = FileEntity.builder()
                .fileName("test.txt")
                .fileType("text/plain")
                .data("test content".getBytes())
                .status(FileStatus.UPLOADED)
                .build();
        fileEntity = fileRepository.save(fileEntity);

        ChunkEntity chunk1 = ChunkEntity.builder()
                .fileId(fileEntity.getFileId())
                .text("Character chunk 1")
                .chunkIndex(0)
                .strategy("character")
                .textLength(17)
                .build();

        ChunkEntity chunk2 = ChunkEntity.builder()
                .fileId(fileEntity.getFileId())
                .text("Character chunk 2")
                .chunkIndex(1)
                .strategy("character")
                .textLength(17)
                .build();

        ChunkEntity sentenceChunk = ChunkEntity.builder()
                .fileId(fileEntity.getFileId())
                .text("Sentence chunk")
                .chunkIndex(0)
                .strategy("sentence")
                .textLength(14)
                .build();

        chunkRepository.save(chunk1);
        chunkRepository.save(chunk2);
        chunkRepository.save(sentenceChunk);
        entityManager.flush();

        // When
        List<ChunkEntity> characterChunks = chunkRepository.findByFileIdAndStrategy(fileEntity.getFileId(), "character");

        // Then
        assertEquals(2, characterChunks.size());
        assertEquals(0, characterChunks.get(0).getChunkIndex());
        assertEquals(1, characterChunks.get(1).getChunkIndex());
    }

    @Test
    void deleteByFileId_Success() {
        // Given
        FileEntity fileEntity = FileEntity.builder()
                .fileName("test.txt")
                .fileType("text/plain")
                .data("test content".getBytes())
                .status(FileStatus.UPLOADED)
                .build();
        fileEntity = fileRepository.save(fileEntity);

        ChunkEntity chunk = ChunkEntity.builder()
                .fileId(fileEntity.getFileId())
                .text("Test chunk")
                .chunkIndex(0)
                .strategy("character")
                .textLength(10)
                .build();

        chunkRepository.save(chunk);
        entityManager.flush();

        // Verify chunk exists
        List<ChunkEntity> chunksBeforeDelete = chunkRepository.findByFileIdOrderByChunkIndex(fileEntity.getFileId());
        assertEquals(1, chunksBeforeDelete.size());

        // When
        chunkRepository.deleteByFileId(fileEntity.getFileId());
        entityManager.flush();

        // Then
        List<ChunkEntity> chunksAfterDelete = chunkRepository.findByFileIdOrderByChunkIndex(fileEntity.getFileId());
        assertTrue(chunksAfterDelete.isEmpty());
    }
}