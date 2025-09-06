package com.luckytapu.fusioncore.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DirectAzureEmbeddingModelTest {

    private DirectAzureEmbeddingModel embeddingModel;

    @BeforeEach
    void setUp() {
        embeddingModel = new DirectAzureEmbeddingModel("test-endpoint", "test-key", "test-deployment");
    }

    @Test
    void embedAll_WithTextSegments_ReturnsResponse() {
        // Given
        List<TextSegment> segments = List.of(
                TextSegment.from("test text 1"),
                TextSegment.from("test text 2")
        );

        // When
        Response<List<Embedding>> result = embeddingModel.embedAll(segments);

        // Then
        assertNotNull(result);
        assertNotNull(result.content());
        assertEquals(1, result.content().size());
    }

    @Test
    void embed_WithTextSegment_ReturnsResponse() {
        // Given
        TextSegment segment = TextSegment.from("test text");

        // When
        Response<Embedding> result = embeddingModel.embed(segment);

        // Then
        assertNotNull(result);
        assertNotNull(result.content());
    }

    @Test
    void embed_WithString_ReturnsResponse() {
        // Given
        String text = "test text";

        // When
        Response<Embedding> result = embeddingModel.embed(text);

        // Then
        assertNotNull(result);
        assertNotNull(result.content());
    }

    @Test
    void constructor_InitializesCorrectly() {
        // When & Then
        assertNotNull(embeddingModel);
        // Constructor test - verifies object creation
    }
}