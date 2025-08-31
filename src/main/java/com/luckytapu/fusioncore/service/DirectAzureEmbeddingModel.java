package com.luckytapu.fusioncore.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
public class DirectAzureEmbeddingModel implements EmbeddingModel {
    private final String apiKey;
    private final String endpoint;
    private final String deploymentName;
    private final RestTemplate restTemplate;
    
    public DirectAzureEmbeddingModel(String apiKey, String endpoint, String deploymentName) {
        this.apiKey = apiKey;
        this.endpoint = endpoint;
        this.deploymentName = deploymentName;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
        // For simplicity, just embed the first segment
        if (textSegments.isEmpty()) {
            return Response.from(List.of());
        }
        
        Response<Embedding> singleResponse = embed(textSegments.get(0));
        return Response.from(List.of(singleResponse.content()));
    }

    @Override
    public Response<Embedding> embed(TextSegment textSegment) {
        return embed(textSegment.text());
    }

    @Override
    public Response<Embedding> embed(String text) {
        try {
            String url = endpoint + "/openai/deployments/" + deploymentName + "/embeddings?api-version=2023-05-15";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", apiKey);
            
            String body = "{\"input\": \"" + text.replace("\"", "\\\"") + "\"}";
            HttpEntity<String> request = new HttpEntity<>(body, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> data = (List<Map<String, Object>>) responseBody.get("data");
                if (data != null && !data.isEmpty()) {
                    List<Double> embeddingList = (List<Double>) data.get(0).get("embedding");
                    float[] vector = new float[embeddingList.size()];
                    for (int i = 0; i < embeddingList.size(); i++) {
                        vector[i] = embeddingList.get(i).floatValue();
                    }
                    return Response.from(Embedding.from(vector));
                }
            }
            
            log.error("Failed to get embedding from Azure OpenAI");
            return Response.from(Embedding.from(new float[0]));
            
        } catch (Exception e) {
            log.error("Error calling Azure OpenAI embedding API: {}", e.getMessage());
            return Response.from(Embedding.from(new float[0]));
        }
    }
}