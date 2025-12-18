package com.example.sis.dto.knowledge;

import com.example.sis.entity.MessageSource;
import lombok.Data;
import lombok.Builder;

import java.util.List;

@Data
@Builder
public class RAGTestResultDTO {
    private String query;
    private List<MessageSource> sources;
    private Integer retrievalMs;
}
