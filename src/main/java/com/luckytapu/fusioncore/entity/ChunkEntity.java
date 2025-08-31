package com.luckytapu.fusioncore.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "chunks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID chunkId;
    
    @Column(nullable = false)
    private UUID fileId;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String text;
    
    @Column(columnDefinition = "BYTEA")
    private byte[] embedding;
    
    @Column(nullable = false)
    private Integer chunkIndex;
    
    @Column(nullable = false)
    private String strategy;
    
    @Column
    private Integer textLength;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fileId", insertable = false, updatable = false)
    private FileEntity file;
}