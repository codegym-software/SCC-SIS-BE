package com.example.sis.config;

import com.example.sis.service.vector.QdrantService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AppConfig {
    
    // TODO: Move Qdrant init to QdrantService @PostConstruct to avoid circular dependency
    // @Autowired
    // private QdrantService qdrantService;
    
    /**
     * Initialize Qdrant collection on startup - DISABLED to avoid circular dependency
     * Collection should be initialized manually via API or on first use
     */
    // @PostConstruct
    // public void initQdrant() {
    //     try {
    //         qdrantService.initializeCollection();
    //     } catch (Exception e) {
    //         // Log error but don't fail startup
    //         System.err.println("Failed to initialize Qdrant: " + e.getMessage());
    //     }
    // }
    
    /**
     * WebClient for OpenAI API and Qdrant calls
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
    
    /**
     * Thread pool for async tasks (embedding generation)
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-task-");
        executor.initialize();
        return executor;
    }
}
