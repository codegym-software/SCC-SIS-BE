-- ==================== CHAT TABLES ====================

-- Chat Sessions
CREATE TABLE chat_sessions (
    session_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    title VARCHAR(255),
    context JSON,  -- Store academic context: { "classId": 15, "moduleId": 3, "lessonId": null }
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_chat_session_user 
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    
    INDEX idx_chat_sessions_user_updated (user_id, updated_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Chat Messages
CREATE TABLE chat_messages (
    message_id INT AUTO_INCREMENT PRIMARY KEY,
    session_id INT NOT NULL,
    role ENUM('user', 'assistant', 'system') NOT NULL,
    content TEXT NOT NULL,
    
    -- Performance tracking
    completion_ms INT,
    model VARCHAR(50),
    
    -- Cost tracking
    tokens_used INT,
    cost_usd DECIMAL(10, 6),
    
    -- Sources from RAG (JSON array)
    sources JSON,  -- [{ "docId": 42, "chunkIndex": 0, "title": "...", "similarity": 0.89, "excerpt": "..." }]
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_chat_message_session 
        FOREIGN KEY (session_id) REFERENCES chat_sessions(session_id) ON DELETE CASCADE,
    
    INDEX idx_chat_messages_session_created (session_id, created_at ASC),
    INDEX idx_chat_messages_model (model) -- For analytics
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== KNOWLEDGE BASE ====================

-- Documents table (lightweight, no embeddings)
CREATE TABLE knowledge_documents (
    doc_id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    doc_type VARCHAR(50) NOT NULL,  -- 'policy', 'guide', 'faq', 'lesson_material'
    source_url VARCHAR(500),
    
    -- Related entity for filtering
    related_entity_type VARCHAR(50),  -- 'module', 'class', 'center', 'lesson'
    related_entity_id INT,
    
    -- Metadata (author, version, tags, etc.)
    metadata JSON,
    
    created_by INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_knowledge_doc_creator 
        FOREIGN KEY (created_by) REFERENCES users(user_id) ON DELETE SET NULL,
    
    INDEX idx_knowledge_documents_type (doc_type),
    INDEX idx_knowledge_documents_entity (related_entity_type, related_entity_id),
    INDEX idx_knowledge_documents_created (created_at DESC),
    
    -- Full-text search index
    FULLTEXT INDEX idx_knowledge_documents_fts (title, content)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Embeddings table (metadata only, vectors stored in Qdrant)
-- Purpose: Track chunk metadata and link to Qdrant vector points
-- Note: Actual embedding vectors (1536 dimensions) are stored in Qdrant for performance
CREATE TABLE knowledge_embeddings (
    embedding_id INT AUTO_INCREMENT PRIMARY KEY,
    doc_id INT NOT NULL,
    chunk_index INT NOT NULL DEFAULT 0,
    chunk_text TEXT NOT NULL,
    
    -- Qdrant point ID (UUID format: "doc123_chunk0")
    qdrant_point_id VARCHAR(255) NOT NULL,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_knowledge_embedding_doc 
        FOREIGN KEY (doc_id) REFERENCES knowledge_documents(doc_id) ON DELETE CASCADE,
    
    UNIQUE KEY uk_doc_chunk (doc_id, chunk_index),
    UNIQUE KEY uk_qdrant_point (qdrant_point_id),
    INDEX idx_knowledge_embeddings_doc (doc_id),
    INDEX idx_qdrant_point (qdrant_point_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== ANALYTICS TRACKING ====================

-- Chat analytics aggregate table (for faster queries)
CREATE TABLE chat_analytics (
    analytics_id INT AUTO_INCREMENT PRIMARY KEY,
    date DATE NOT NULL,
    user_id INT,
    
    -- Metrics
    total_messages INT DEFAULT 0,
    total_sessions INT DEFAULT 0,
    total_tokens INT DEFAULT 0,
    total_cost_usd DECIMAL(10, 4) DEFAULT 0,
    avg_completion_ms INT DEFAULT 0,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_analytics_date_user (date, user_id),
    INDEX idx_chat_analytics_date (date DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
