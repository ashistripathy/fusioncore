package com.luckytapu.fusioncore.config;

import com.luckytapu.fusioncore.service.DirectAzureEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class LangChain4jConfig {

    @Value("${embedding.azureOpenAIApiKey}")
    private String apiKey;

    @Value("${embedding.azureOpenAIEndpoint}")
    private String endpoint;

    @Value("${embedding.azureOpenAIEmbeddingDeploymentName}")
    private String deploymentName;

    @Bean
    public EmbeddingModel embeddingModel() {
        log.info("Creating Direct Azure OpenAI Embedding Model");
        return new DirectAzureEmbeddingModel(apiKey, endpoint, deploymentName);
    }
}