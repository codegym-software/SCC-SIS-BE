package com.example.sis.dto.knowledge;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class KnowledgeDocumentDTO {
    private Integer docId;
    private String title;
    private String content;
    private String docType;
    private String sourceUrl;
    private String relatedEntityType;
    private Integer relatedEntityId;
    private Map<String, Object> metadata;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer chunkCount;
}
