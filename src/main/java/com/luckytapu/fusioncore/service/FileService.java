package com.luckytapu.fusioncore.service;

import com.luckytapu.fusioncore.entity.FileEntity;
import com.luckytapu.fusioncore.model.ChunkingReport;
import com.luckytapu.fusioncore.model.DocumentProcessingResult;
import com.luckytapu.fusioncore.model.SearchResult;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface FileService {
    // Legacy methods (kept for backward compatibility)
    FileEntity saveFile(MultipartFile file) throws IOException;
    ChunkingReport processAndAnalyzeDocument(MultipartFile file) throws IOException;
    FileEntity updateFileStatus(UUID fileId, com.luckytapu.fusioncore.model.FileStatus status);
    
    // Enhanced methods
    DocumentProcessingResult processDocument(MultipartFile file, boolean generateEmbeddings) throws IOException;
    CompletableFuture<DocumentProcessingResult> processDocumentAsync(MultipartFile file, boolean generateEmbeddings);
    List<SearchResult> searchDocuments(String query, int limit);
    List<DocumentProcessingResult> processBatch(List<MultipartFile> files, boolean generateEmbeddings);
}
