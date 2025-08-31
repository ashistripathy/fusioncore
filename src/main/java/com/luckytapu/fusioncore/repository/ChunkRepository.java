package com.luckytapu.fusioncore.repository;

import com.luckytapu.fusioncore.entity.ChunkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChunkRepository extends JpaRepository<ChunkEntity, UUID> {
    
    List<ChunkEntity> findByFileIdOrderByChunkIndex(UUID fileId);
    
    List<ChunkEntity> findByStrategy(String strategy);
    
    @Query(value = """
        SELECT c.*, 
               (1 - (c.embedding <-> CAST(:queryEmbedding AS bytea))) as similarity_score
        FROM chunks c 
        WHERE c.embedding IS NOT NULL
        ORDER BY c.embedding <-> CAST(:queryEmbedding AS bytea)
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findSimilarChunks(@Param("queryEmbedding") byte[] queryEmbedding, @Param("limit") int limit);
    
    @Query("SELECT c FROM ChunkEntity c WHERE c.fileId = :fileId AND c.strategy = :strategy ORDER BY c.chunkIndex")
    List<ChunkEntity> findByFileIdAndStrategy(@Param("fileId") UUID fileId, @Param("strategy") String strategy);
    
    void deleteByFileId(UUID fileId);
}