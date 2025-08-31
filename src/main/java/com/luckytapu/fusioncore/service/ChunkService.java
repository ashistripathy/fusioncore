package com.luckytapu.fusioncore.service;

import com.luckytapu.fusioncore.entity.ChunkEntity;
import com.luckytapu.fusioncore.repository.ChunkRepository;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.document.splitter.DocumentByCharacterSplitter;
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChunkService {
    
    private final ChunkRepository chunkRepository;
    private final EmbeddingModel embeddingModel;
    
    public boolean generateAndStoreChunkEmbeddings(Document document, UUID fileId, String strategy) {
        try {
            if (embeddingModel == null) {
                log.warn("EmbeddingModel not available for chunk embeddings");
                return false;
            }
            
            List<TextSegment> chunks = getChunksForStrategy(document, strategy);
            if (chunks.isEmpty()) {
                log.warn("No chunks generated for strategy: {}", strategy);
                return false;
            }
            
            chunkRepository.deleteByFileId(fileId);
            
            for (int i = 0; i < chunks.size(); i++) {
                TextSegment chunk = chunks.get(i);
                try {
                    Response<Embedding> response = embeddingModel.embed(chunk);
                    if (response.content() != null) {
                        byte[] embeddingBytes = convertToBytes(response.content().vector());
                        
                        ChunkEntity chunkEntity = ChunkEntity.builder()
                                .fileId(fileId)
                                .text(chunk.text())
                                .embedding(embeddingBytes)
                                .chunkIndex(i)
                                .strategy(strategy)
                                .textLength(chunk.text().length())
                                .build();
                        
                        chunkRepository.save(chunkEntity);
                    }
                } catch (Exception e) {
                    log.warn("Failed to generate embedding for chunk {}: {}", i, e.getMessage());
                }
            }
            
            log.info("Generated and stored embeddings for {} chunks using {} strategy", chunks.size(), strategy);
            return true;
            
        } catch (Exception e) {
            log.error("Failed to generate chunk embeddings: {}", e.getMessage(), e);
            return false;
        }
    }
    
    private List<TextSegment> getChunksForStrategy(Document document, String strategy) {
        switch (strategy.toLowerCase()) {
            case "sentence splitter":
                return new DocumentBySentenceSplitter(300, 30).split(document);
            case "paragraph splitter":
                return new DocumentByParagraphSplitter(800, 100).split(document);
            default:
                return new DocumentByCharacterSplitter(500, 50).split(document);
        }
    }
    
    private byte[] convertToBytes(float[] vector) {
        byte[] bytes = new byte[vector.length * 4];
        for (int i = 0; i < vector.length; i++) {
            int bits = Float.floatToIntBits(vector[i]);
            bytes[i * 4] = (byte) (bits >> 24);
            bytes[i * 4 + 1] = (byte) (bits >> 16);
            bytes[i * 4 + 2] = (byte) (bits >> 8);
            bytes[i * 4 + 3] = (byte) bits;
        }
        return bytes;
    }
}