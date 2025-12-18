package com.example.sis.dto.knowledge;

import lombok.Data;
import java.util.Map;

@Data
public class KnowledgeDocumentRequest {
    private String title;
    private String content;
    private String docType; // FAQ, SYLLABUS, TUTORIAL, REFERENCE
    private String sourceUrl;
    private String relatedEntityType; // CLASS, MODULE, LESSON
    private Integer relatedEntityId;
    private Map<String, Object> metadata;
}
