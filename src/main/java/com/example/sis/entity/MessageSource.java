package com.example.sis.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageSource {
    private Integer docId;
    private Integer chunkIndex;
    private String title;
    private Double similarity;
    private String excerpt;
}
