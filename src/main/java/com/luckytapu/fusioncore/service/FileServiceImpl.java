package com.luckytapu.fusioncore.service;

import com.luckytapu.fusioncore.entity.FileEntity;
import com.luckytapu.fusioncore.model.ChunkingReport;
import com.luckytapu.fusioncore.model.FileStatus;
import com.luckytapu.fusioncore.repository.FileRepository;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentByCharacterSplitter;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.data.embedding.Embedding;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import com.luckytapu.fusioncore.model.DocumentProcessingResult;
import com.luckytapu.fusioncore.model.SearchResult;
import com.luckytapu.fusioncore.entity.ChunkEntity;
import com.luckytapu.fusioncore.repository.ChunkRepository;
import com.luckytapu.fusioncore.service.VectorSearchService;

@Service
@Slf4j
public class FileServiceImpl implements FileService {
    private final FileRepository fileRepository;
    private final EmbeddingModel embeddingModel;
    private final ChunkRepository chunkRepository;
    private final VectorSearchService vectorSearchService;
    private final ChunkService chunkService;
    
    @Value("${fusioncore.embedding.test-strategies:true}")
    private boolean testEmbeddingStrategies;
    
    @Value("${fusioncore.processing.default-strategy:character}")
    private String defaultStrategy;
    
    public FileServiceImpl(FileRepository fileRepository, EmbeddingModel embeddingModel, 
                          ChunkRepository chunkRepository, VectorSearchService vectorSearchService,
                          ChunkService chunkService) {
        this.fileRepository = fileRepository;
        this.embeddingModel = embeddingModel;
        this.chunkRepository = chunkRepository;
        this.vectorSearchService = vectorSearchService;
        this.chunkService = chunkService;
        if (embeddingModel == null) {
            log.warn("EmbeddingModel is null - embedding functionality will be disabled");
        }
    }

    @Override
    public FileEntity saveFile(MultipartFile file) throws IOException {
        byte[] fileBytes = file.getBytes();
        log.info("Saving file: {}, size: {}", file.getOriginalFilename(), fileBytes.length);

        FileEntity fileEntity = FileEntity.builder()
                .fileName(file.getOriginalFilename())
                .fileType(file.getContentType())
                .data(fileBytes)
                .status(FileStatus.UPLOADED)
                .build();

        return fileRepository.save(fileEntity);
    }

    @Override
    public ChunkingReport processAndAnalyzeDocument(MultipartFile file) throws IOException {
        log.info("Processing document: {}", file.getOriginalFilename());
        
        FileEntity savedFile = saveFile(file);
        
        try {
            // Skip PROCESSING status to avoid database constraint violation
            
            Document document = loadDocument(file);
            String text = document.text();
            
            List<ChunkingReport.ChunkingStrategy> strategies = new ArrayList<>();
            strategies.add(testCharacterSplitter(document));
            strategies.add(testSentenceSplitter(document));
            strategies.add(testParagraphSplitter(document));
            
            ChunkingReport.ChunkingStrategy bestStrategy = findBestStrategy(strategies);
            if (bestStrategy.isEmbeddingTestPassed()) {
                byte[] vectorData = generateAndStoreEmbeddings(document);
                savedFile.setVectorData(vectorData);
                updateFileStatus(savedFile.getFileId(), FileStatus.PROCESSED);
            } else {
                updateFileStatus(savedFile.getFileId(), FileStatus.PROCESSED);
            }
            
            return ChunkingReport.builder()
                    .fileName(file.getOriginalFilename())
                    .totalCharacters(text.length())
                    .strategies(strategies)
                    .build();
                    
        } catch (Exception e) {
            log.error("Document processing failed: {}", e.getMessage());
            updateFileStatus(savedFile.getFileId(), FileStatus.FAILED);
            throw new IOException("Document processing failed: " + e.getMessage(), e);
        }
    }

    @Override
    public FileEntity updateFileStatus(UUID fileId, FileStatus status) {
        FileEntity fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found: " + fileId));
        fileEntity.setStatus(status);
        return fileRepository.save(fileEntity);
    }

    private Document loadDocument(MultipartFile file) throws IOException {
        byte[] fileBytes = file.getBytes();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(fileBytes);
        
        DocumentParser parser = file.getOriginalFilename().toLowerCase().endsWith(".pdf") 
            ? new ApachePdfBoxDocumentParser() 
            : new ApacheTikaDocumentParser();
        
        return parser.parse(inputStream);
    }

    private ChunkingReport.ChunkingStrategy testCharacterSplitter(Document document) {
        DocumentByCharacterSplitter splitter = new DocumentByCharacterSplitter(500, 50);
        List<TextSegment> chunks = splitter.split(document);
        return analyzeChunks("Character Splitter", "Splits by character count with overlap", chunks);
    }

    private ChunkingReport.ChunkingStrategy testSentenceSplitter(Document document) {
        DocumentBySentenceSplitter splitter = new DocumentBySentenceSplitter(300, 30);
        List<TextSegment> chunks = splitter.split(document);
        return analyzeChunks("Sentence Splitter", "Splits by sentences", chunks);
    }

    private ChunkingReport.ChunkingStrategy testParagraphSplitter(Document document) {
        DocumentByParagraphSplitter splitter = new DocumentByParagraphSplitter(800, 100);
        List<TextSegment> chunks = splitter.split(document);
        return analyzeChunks("Paragraph Splitter", "Splits by paragraphs", chunks);
    }

    private ChunkingReport.ChunkingStrategy analyzeChunks(String strategyName, String description, List<TextSegment> chunks) {
        if (chunks.isEmpty()) {
            return ChunkingReport.ChunkingStrategy.builder()
                    .strategyName(strategyName)
                    .description(description)
                    .chunkCount(0)
                    .averageChunkSize(0)
                    .largestChunkSize(0)
                    .smallestChunkSize(0)
                    .medianChunkSize(0)
                    .p85ChunkSize(0)
                    .p95ChunkSize(0)
                    .standardDeviation(0)
                    .coefficientOfVariation(0)
                    .qualityScore(0)
                    .embeddingTestPassed(false)
                    .embeddingTestResult("No chunks created")
                    .build();
        }

        // Calculate chunk sizes and sort for percentile calculations
        List<Integer> chunkSizes = chunks.stream()
                .mapToInt(chunk -> chunk.text().length())
                .sorted()
                .boxed()
                .collect(Collectors.toList());

        // Basic metrics
        int totalSize = chunkSizes.stream().mapToInt(Integer::intValue).sum();
        double averageSize = (double) totalSize / chunkSizes.size();
        int largestSize = Collections.max(chunkSizes);
        int smallestSize = Collections.min(chunkSizes);

        // Percentile calculations
        double median = calculatePercentile(chunkSizes, 50);
        double p85 = calculatePercentile(chunkSizes, 85);
        double p95 = calculatePercentile(chunkSizes, 95);

        // Consistency metrics
        double stdDev = calculateStandardDeviation(chunkSizes, averageSize);
        double coefficientOfVariation = averageSize > 0 ? stdDev / averageSize : 0;

        // Embedding test (configurable for cost optimization)
        boolean embeddingTestPassed = false;
        String embeddingTestResult = "";
        
        if (testEmbeddingStrategies) {
            try {
                if (embeddingModel != null) {
                    Response<Embedding> response = embeddingModel.embed(chunks.get(0));
                    embeddingTestPassed = response.content() != null;
                    embeddingTestResult = embeddingTestPassed ? "Success" : "Failed";
                } else {
                    embeddingTestResult = "EmbeddingModel not available";
                }
            } catch (Exception e) {
                embeddingTestResult = "Error: " + e.getMessage();
                log.warn("Embedding test failed: {}", e.getMessage());
            }
        } else {
            // Skip embedding test for cost optimization
            embeddingTestPassed = embeddingModel != null;
            embeddingTestResult = "Skipped for cost optimization";
        }

        ChunkingReport.ChunkingStrategy strategy = ChunkingReport.ChunkingStrategy.builder()
                .strategyName(strategyName)
                .description(description)
                .chunkCount(chunks.size())
                .averageChunkSize(averageSize)
                .largestChunkSize(largestSize)
                .smallestChunkSize(smallestSize)
                .medianChunkSize(median)
                .p85ChunkSize(p85)
                .p95ChunkSize(p95)
                .standardDeviation(stdDev)
                .coefficientOfVariation(coefficientOfVariation)
                .embeddingTestPassed(embeddingTestPassed)
                .embeddingTestResult(embeddingTestResult)
                .build();

        // Calculate quality score
        double qualityScore = calculateQualityScore(strategy);
        strategy.setQualityScore(qualityScore);

        return strategy;
    }

    private ChunkingReport.ChunkingStrategy findBestStrategy(List<ChunkingReport.ChunkingStrategy> strategies) {
        return strategies.stream()
                .filter(ChunkingReport.ChunkingStrategy::isEmbeddingTestPassed)
                .max(Comparator.comparingDouble(ChunkingReport.ChunkingStrategy::getQualityScore))
                .orElse(strategies.stream()
                        .max(Comparator.comparingDouble(ChunkingReport.ChunkingStrategy::getQualityScore))
                        .orElse(strategies.get(0)));
    }

    private double calculatePercentile(List<Integer> sortedSizes, int percentile) {
        if (sortedSizes.isEmpty()) return 0;
        int index = (int) Math.ceil(percentile / 100.0 * sortedSizes.size()) - 1;
        return sortedSizes.get(Math.max(0, Math.min(index, sortedSizes.size() - 1)));
    }

    private double calculateStandardDeviation(List<Integer> chunkSizes, double mean) {
        if (chunkSizes.size() <= 1) return 0;
        
        double sumSquaredDiffs = chunkSizes.stream()
                .mapToDouble(size -> Math.pow(size - mean, 2))
                .sum();
        
        return Math.sqrt(sumSquaredDiffs / (chunkSizes.size() - 1));
    }

    private double calculateQualityScore(ChunkingReport.ChunkingStrategy strategy) {
        double score = 0;
        
        // P85 in optimal range (200-800) - Most chunks should be well-sized
        if (strategy.getP85ChunkSize() >= 200 && strategy.getP85ChunkSize() <= 800) {
            score += 5;
        } else if (strategy.getP85ChunkSize() >= 100 && strategy.getP85ChunkSize() <= 1000) {
            score += 2;
        }
        
        // P95 not too large (< 1200) - Avoid extremely large chunks
        if (strategy.getP95ChunkSize() < 1200) {
            score += 3;
        } else if (strategy.getP95ChunkSize() < 1500) {
            score += 1;
        }
        
        // Low coefficient of variation (< 0.5) - Consistent chunk sizes
        if (strategy.getCoefficientOfVariation() < 0.3) {
            score += 4;
        } else if (strategy.getCoefficientOfVariation() < 0.6) {
            score += 2;
        }
        
        // Reasonable chunk count
        int chunkCount = strategy.getChunkCount();
        if (chunkCount >= 5 && chunkCount <= 50) {
            score += 2;
        } else if (chunkCount >= 2 && chunkCount <= 100) {
            score += 1;
        }
        
        // Median in good range
        if (strategy.getMedianChunkSize() >= 200 && strategy.getMedianChunkSize() <= 600) {
            score += 3;
        } else if (strategy.getMedianChunkSize() >= 100 && strategy.getMedianChunkSize() <= 800) {
            score += 1;
        }
        
        // Bonus for embedding compatibility
        if (strategy.isEmbeddingTestPassed()) {
            score += 10; // High weight for embedding compatibility
        }
        
        log.debug("Strategy '{}' scored: {} (P85: {}, P95: {}, CV: {}, Count: {}, Embedding: {})", 
                strategy.getStrategyName(), score, strategy.getP85ChunkSize(), 
                strategy.getP95ChunkSize(), strategy.getCoefficientOfVariation(), 
                strategy.getChunkCount(), strategy.isEmbeddingTestPassed());
        
        return score;
    }

    // Enhanced methods implementation
    @Override
    public DocumentProcessingResult processDocument(MultipartFile file, boolean generateEmbeddings) throws IOException {
        long startTime = System.currentTimeMillis();
        log.info("Processing document: {} with embeddings: {}", file.getOriginalFilename(), generateEmbeddings);
        
        try {
            FileEntity savedFile = saveFile(file);
            Document document = loadDocument(file);
            
            // Use default strategy or run analysis based on configuration
            ChunkingReport report;
            if (testEmbeddingStrategies) {
                report = analyzeAllStrategies(document, file.getOriginalFilename());
            } else {
                report = analyzeDefaultStrategy(document, file.getOriginalFilename());
            }
            
            boolean embeddingsGenerated = false;
            if (generateEmbeddings && embeddingModel != null) {
                ChunkingReport.ChunkingStrategy bestStrategy = findBestStrategy(report.getStrategies());
                embeddingsGenerated = chunkService.generateAndStoreChunkEmbeddings(document, savedFile.getFileId(), bestStrategy.getStrategyName());
                
                // Also store document-level embedding for backward compatibility
                if (embeddingsGenerated) {
                    byte[] vectorData = generateAndStoreEmbeddings(document);
                    if (vectorData != null) {
                        savedFile.setVectorData(vectorData);
                    }
                }
            }
            
            updateFileStatus(savedFile.getFileId(), FileStatus.PROCESSED);
            long processingTime = System.currentTimeMillis() - startTime;
            
            return DocumentProcessingResult.builder()
                    .fileEntity(savedFile)
                    .chunkingReport(report)
                    .embeddingsGenerated(embeddingsGenerated)
                    .processingStatus("SUCCESS")
                    .processingTimeMs(processingTime)
                    .build();
                    
        } catch (Exception e) {
            log.error("Document processing failed: {}", e.getMessage());
            long processingTime = System.currentTimeMillis() - startTime;
            
            return DocumentProcessingResult.builder()
                    .processingStatus("FAILED: " + e.getMessage())
                    .processingTimeMs(processingTime)
                    .embeddingsGenerated(false)
                    .build();
        }
    }
    
    @Override
    @Async
    public CompletableFuture<DocumentProcessingResult> processDocumentAsync(MultipartFile file, boolean generateEmbeddings) {
        try {
            DocumentProcessingResult result = processDocument(file, generateEmbeddings);
            return CompletableFuture.completedFuture(result);
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
    
    @Override
    public List<SearchResult> searchDocuments(String query, int limit) {
        log.info("Searching documents for query: {} with limit: {}", query, limit);
        return vectorSearchService.searchSimilarChunks(query, limit);
    }
    
    @Override
    public List<DocumentProcessingResult> processBatch(List<MultipartFile> files, boolean generateEmbeddings) {
        log.info("Processing batch of {} files", files.size());
        
        return files.stream()
                .map(file -> {
                    try {
                        return processDocument(file, generateEmbeddings);
                    } catch (IOException e) {
                        log.error("Failed to process file: {}", file.getOriginalFilename(), e);
                        return DocumentProcessingResult.builder()
                                .processingStatus("FAILED: " + e.getMessage())
                                .embeddingsGenerated(false)
                                .build();
                    }
                })
                .collect(Collectors.toList());
    }
    
    private ChunkingReport analyzeAllStrategies(Document document, String fileName) {
        List<ChunkingReport.ChunkingStrategy> strategies = new ArrayList<>();
        strategies.add(testCharacterSplitter(document));
        strategies.add(testSentenceSplitter(document));
        strategies.add(testParagraphSplitter(document));
        
        return ChunkingReport.builder()
                .fileName(fileName)
                .totalCharacters(document.text().length())
                .strategies(strategies)
                .build();
    }
    
    private ChunkingReport analyzeDefaultStrategy(Document document, String fileName) {
        List<ChunkingReport.ChunkingStrategy> strategies = new ArrayList<>();
        
        switch (defaultStrategy.toLowerCase()) {
            case "sentence":
                strategies.add(testSentenceSplitter(document));
                break;
            case "paragraph":
                strategies.add(testParagraphSplitter(document));
                break;
            default:
                strategies.add(testCharacterSplitter(document));
        }
        
        return ChunkingReport.builder()
                .fileName(fileName)
                .totalCharacters(document.text().length())
                .strategies(strategies)
                .build();
    }

    private byte[] generateAndStoreEmbeddings(Document document) {
        try {
            if (embeddingModel == null) {
                log.warn("EmbeddingModel not available for generating embeddings");
                return null;
            }
            
            DocumentByCharacterSplitter splitter = new DocumentByCharacterSplitter(500, 50);
            List<TextSegment> chunks = splitter.split(document);
            
            if (!chunks.isEmpty()) {
                Response<Embedding> response = embeddingModel.embed(chunks.get(0));
                if (response.content() != null) {
                    float[] vector = response.content().vector();
                    byte[] vectorBytes = new byte[vector.length * 4];
                    for (int i = 0; i < vector.length; i++) {
                        int bits = Float.floatToIntBits(vector[i]);
                        vectorBytes[i * 4] = (byte) (bits >> 24);
                        vectorBytes[i * 4 + 1] = (byte) (bits >> 16);
                        vectorBytes[i * 4 + 2] = (byte) (bits >> 8);
                        vectorBytes[i * 4 + 3] = (byte) bits;
                    }
                    return vectorBytes;
                }
            }
        } catch (Exception e) {
            log.error("Failed to generate embeddings: {}", e.getMessage());
        }
        return null;
    }
}