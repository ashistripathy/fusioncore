package com.luckytapu.fusioncore.controller;

import com.luckytapu.fusioncore.entity.FileEntity;
import com.luckytapu.fusioncore.model.ChunkingReport;
import com.luckytapu.fusioncore.model.DocumentProcessingResult;
import com.luckytapu.fusioncore.model.SearchResult;
import com.luckytapu.fusioncore.service.FileService;
import com.luckytapu.fusioncore.service.VectorSearchService;
import com.luckytapu.fusioncore.entity.ChunkEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;
    private final VectorSearchService vectorSearchService;

    // Legacy endpoints (kept for backward compatibility)
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    @Operation(description = "Upload a file", summary = "Uploads a file and stores it in the database")
    public ResponseEntity<FileEntity> uploadFile(@Parameter(description = "File to upload") @RequestParam("file") MultipartFile file) throws IOException {
        FileEntity savedFile = fileService.saveFile(file);
        return ResponseEntity.ok(savedFile);
    }

    @PostMapping(value = "/analyze", consumes = "multipart/form-data")
    @Operation(description = "Analyze document chunking strategies", summary = "Uploads and analyzes a document for optimal chunking strategy")
    public ResponseEntity<ChunkingReport> analyzeDocument(@Parameter(description = "Document file to analyze (PDF, DOCX supported)") @RequestParam("file") MultipartFile file) throws IOException {
        ChunkingReport report = fileService.processAndAnalyzeDocument(file);
        return ResponseEntity.ok(report);
    }
    
    // Enhanced endpoints
    @PostMapping(value = "/process", consumes = "multipart/form-data")
    @Operation(description = "Process document with optional embeddings", summary = "Streamlined endpoint for complete document processing")
    public ResponseEntity<DocumentProcessingResult> processDocument(
            @Parameter(description = "Document file to process") @RequestParam("file") MultipartFile file,
            @Parameter(description = "Generate embeddings") @RequestParam(defaultValue = "true") boolean generateEmbeddings) throws IOException {
        DocumentProcessingResult result = fileService.processDocument(file, generateEmbeddings);
        return ResponseEntity.ok(result);
    }
    
    @PostMapping(value = "/process-async", consumes = "multipart/form-data")
    @Operation(description = "Process document asynchronously", summary = "Async processing for large documents")
    public CompletableFuture<ResponseEntity<DocumentProcessingResult>> processDocumentAsync(
            @Parameter(description = "Document file to process") @RequestParam("file") MultipartFile file,
            @Parameter(description = "Generate embeddings") @RequestParam(defaultValue = "true") boolean generateEmbeddings) {
        return fileService.processDocumentAsync(file, generateEmbeddings)
                .thenApply(ResponseEntity::ok);
    }
    
    @PostMapping(value = "/batch-process", consumes = "multipart/form-data")
    @Operation(description = "Process multiple documents", summary = "Batch processing for multiple files")
    public ResponseEntity<List<DocumentProcessingResult>> processBatch(
            @Parameter(description = "Files to process") @RequestParam("files") List<MultipartFile> files,
            @Parameter(description = "Generate embeddings") @RequestParam(defaultValue = "true") boolean generateEmbeddings) {
        List<DocumentProcessingResult> results = fileService.processBatch(files, generateEmbeddings);
        return ResponseEntity.ok(results);
    }
    
    @GetMapping("/search")
    @Operation(description = "Search documents by semantic similarity", summary = "Vector-based document search")
    public ResponseEntity<List<SearchResult>> searchDocuments(
            @Parameter(description = "Search query") @RequestParam String query,
            @Parameter(description = "Maximum results") @RequestParam(defaultValue = "10") int limit) {
        List<SearchResult> results = fileService.searchDocuments(query, limit);
        return ResponseEntity.ok(results);
    }
    
    @GetMapping("/{fileId}/chunks")
    @Operation(description = "Get chunks for a document", summary = "Retrieve stored chunks for a specific document")
    public ResponseEntity<List<ChunkEntity>> getDocumentChunks(
            @Parameter(description = "File ID") @PathVariable UUID fileId,
            @Parameter(description = "Chunking strategy filter") @RequestParam(required = false) String strategy) {
        List<ChunkEntity> chunks = vectorSearchService.getDocumentChunks(fileId, strategy);
        return ResponseEntity.ok(chunks);
    }
}
