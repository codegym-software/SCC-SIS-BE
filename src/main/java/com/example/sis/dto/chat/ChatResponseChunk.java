package com.example.sis.dto.chat;

import com.example.sis.entity.MessageSource;
import lombok.Data;
import lombok.Builder;

import java.util.List;

@Data
@Builder
public class ChatResponseChunk {
    private ChunkType type;
    private String content; // For TEXT type
    private Integer sessionId; // For SESSION_CREATED type
    private List<MessageSource> sources; // For SOURCES type
    private ChatMetrics metrics; // For METRICS type
    private String error; // For ERROR type
    
    public enum ChunkType {
        TEXT,           // Streaming text content
        SOURCES,        // RAG sources used
        SESSION_CREATED,// New session ID
        METRICS,        // Completion stats
        DONE,           // End of stream
        ERROR           // Error occurred
    }
    
    @Data
    @Builder
    public static class ChatMetrics {
        private Integer completionMs;
        private Integer tokensUsed;
        private String model;
    }
}
