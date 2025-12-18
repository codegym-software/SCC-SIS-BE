# AI Chatbot with RAG - Backend Implementation

## Tổng quan

Hệ thống AI Chatbot tích hợp vào SIS (Student Information System) với các tính năng:
- **RAG (Retrieval Augmented Generation)**: Tìm kiếm thông tin từ knowledge base
- **Vector Search**: Sử dụng **Qdrant** - chuyên cho vector similarity search
- **Streaming Chat**: Server-Sent Events (SSE) để stream responses từ OpenAI
- **Context Resolution**: Tự động xác định context từ enrollment và lesson progress
- **Security**: Không gửi tokens/sensitive data cho LLM
- **Analytics**: Theo dõi usage, cost, performance

## Kiến trúc

```
User → Frontend (React) → Backend (Spring Boot) → MySQL (SIS core data)
                                                 → Qdrant (Vector search)
                                                 → OpenAI (LLM + embeddings)
```

## Cấu trúc

```
src/main/java/com/example/sis/
├── entity/
│   ├── ChatSession.java           # Chat sessions
│   ├── ChatMessage.java            # Messages with metrics
│   ├── MessageSource.java          # RAG sources
│   ├── KnowledgeDocument.java      # Documents (NO embeddings)
│   └── KnowledgeEmbedding.java     # Separate embeddings table
├── dto/
│   ├── chat/
│   │   ├── SafeChatContext.java    # Public info only
│   │   ├── ChatMessageRequest.java
│   │   ├── ChatResponseChunk.java  # SSE chunks
│   │   └── ChatSessionDTO.java
│   ├── knowledge/
│   │   ├── KnowledgeDocumentDTO.java
│   │   └── RAGTestResultDTO.java
│   └── analytics/
│       ├── ChatAnalyticsDTO.java
│       └── PopularQuestionDTO.java
├── repository/
│   ├── ChatSessionRepository.java
│   ├── ChatMessageRepository.java
│   ├── KnowledgeDocumentRepository.java
│   └── KnowledgeEmbeddingRepository.java  # Vector search queries
├── service/
│   ├── chat/
│   │   ├── ChatService.java               # Main chat logic
│   │   ├── ChatContextResolver.java       # Auto-detect context
│   │   └── SecurityAuditService.java      # Validate no tokens
│   ├── openai/
│   │   └── OpenAIService.java             # OpenAI API wrapper
│   ├── knowledge/
│   │   ├── KnowledgeService.java          # Document CRUD
│   │   └── EmbeddingService.java          # Chunking + embeddings
│   └── analytics/
│       └── ChatAnalyticsService.java      # Metrics
└── controller/
    ├── ChatController.java                 # /api/chat (student)
    ├── AdminKnowledgeController.java       # /api/admin/knowledge
    └── ChatAnalyticsController.java        # /api/admin/chat-analytics
```

## Database Schema

### Bảng `chat_sessions`
- `session_id`: Primary key
- `user_id`: Foreign key -> users
- `title`: Session title
- `context`: JSONB (classId, moduleId, lessonId)
- `created_at`, `updated_at`: Timestamps

### Bảng `chat_messages`
- `message_id`: Primary key
- `session_id`: Foreign key -> chat_sessions
- `role`: ENUM ('user', 'assistant', 'system')
- `content`: TEXT
- `completion_ms`: Response time
- `model`: GPT model used
- `tokens_used`: Token count
- `cost_usd`: Estimated cost
- `sources`: JSONB array of RAG sources
- `created_at`: Timestamp

### Bảng `knowledge_documents`
- `doc_id`: Primary key
- `title`, `content`: Document data
- `doc_type`: FAQ, SYLLABUS, TUTORIAL, etc.
- `related_entity_type`, `related_entity_id`: Context linking
- `metadata`: JSONB
- `created_by`: Foreign key -> users

### Bảng `knowledge_embeddings`
- `embedding_id`: Primary key
- `doc_id`: Foreign key -> knowledge_documents
- `chunk_index`: Chunk number
- `chunk_text`: TEXT
- `qdrant_point_id`: VARCHAR(255) - Link to Qdrant vector point
- **Note**: Actual embeddings (1536 dimensions) stored in **Qdrant**, NOT MySQL

## API Endpoints

### Chat (Tất cả users)

#### `POST /api/chat/stream`
Stream chat response với SSE
```json
Request:
{
  "sessionId": 1,          // null = new session
  "message": "Làm thế nào để cài đặt Java?",
  "classId": 5,            // Optional
  "moduleId": 12,          // Optional
  "lessonId": 45           // Optional
}

Response (SSE chunks):
data: {"type":"SESSION_CREATED","sessionId":10}
data: {"type":"SOURCES","sources":[...]}
data: {"type":"TEXT","content":"Để cài đặt Java..."}
data: {"type":"TEXT","content":" bạn cần..."}
data: {"type":"METRICS","metrics":{"completionMs":1234,"tokensUsed":156}}
data: {"type":"DONE"}
```

#### `GET /api/chat/sessions`
Lấy danh sách sessions của user

#### `GET /api/chat/sessions/{sessionId}/messages`
Lấy messages trong session

#### `DELETE /api/chat/sessions/{sessionId}`
Xóa session

### Admin Knowledge Base

#### `POST /api/admin/knowledge/documents`
Tạo document mới (auto-generate embeddings)

#### `PUT /api/admin/knowledge/documents/{docId}`
Cập nhật document (regenerate embeddings)

#### `DELETE /api/admin/knowledge/documents/{docId}`
Xóa document và embeddings

#### `GET /api/admin/knowledge/documents`
Lấy tất cả documents

#### `GET /api/admin/knowledge/documents/search?query=java`
Tìm kiếm documents

#### `POST /api/admin/knowledge/reindex`
Regenerate tất cả embeddings

#### `GET /api/admin/knowledge/test-rag?query=...&classId=5`
Test RAG retrieval

### Admin Analytics

#### `GET /api/admin/chat-analytics/overview?days=30`
Tổng quan: sessions, messages, tokens, cost

#### `GET /api/admin/chat-analytics/popular-questions?days=30&limit=10`
Top câu hỏi phổ biến

## Setup & Configuration

### 1. Database Migration
```bash
# V48__create_chat_and_knowledge_tables.sql sẽ tự chạy với Flyway
```

### 2. Environment Variables
```properties
# application.properties
openai.api.key=${OPENAI_API_KEY:your-api-key-here}
openai.model=gpt-4o
```

Hoặc set environment variable:
```bash
export OPENAI_API_KEY=sk-xxx...
```

### 3. Dependencies Required
```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

### 4. Start Qdrant with Docker
```bash
# Using docker-compose (recommended)
docker-compose up -d qdrant

# Or standalone
docker run -p 6333:6333 -p 6334:6334 \
    -v $(pwd)/qdrant_storage:/qdrant/storage \
    qdrant/qdrant
```

Verify Qdrant is running:
```bash
### Chat Flow
1. User gửi message qua `POST /api/chat/stream`
2. `ChatContextResolver` auto-detect class/module/lesson từ enrollment/progress
3. `SecurityAuditService` validate không có tokens trong context
4. `ChatService` tạo/lấy session, lưu user message (MySQL)
5. `EmbeddingService` tìm relevant chunks (RAG):
   - Create query embedding với OpenAI
   - **Vector similarity search trong Qdrant** (cosine similarity)
   - Filter by academic context (class/module)
6. `ChatService` build prompt với context + sources
7. `OpenAIService` stream response từ OpenAI
8. Save assistant message với metrics (tokens, cost, completion_ms) vào MySQL

### Knowledge Base Management (Admin uploads document)
1. Admin upload document qua `POST /api/admin/knowledge/documents`
2. `KnowledgeService` lưu document vào **MySQL** (metadata only)
3. `EmbeddingService` (async):
   - **Chunk document** (500 words, 50 overlap)
   - **Create embeddings** cho mỗi chunk với OpenAI
   - **Lưu vectors vào Qdrant** (batch upsert)
   - Lưu metadata vào `knowledge_embeddings` table (MySQL) với `qdrant_point_id`
4. Qdrant tự động optimize với HNSW index

### RAG Workflow Details
```
Student asks: "Làm thế nào để cài đặt Java?"
    ↓
1. OpenAI: Tạo embedding cho câu hỏi → [0.1, 0.2, ..., 0.9] (1536 dims)
    ↓
2. Qdrant: Vector search với filter (classId, moduleId)
   → Trả về top 5 chunks similarity > 0.7
    ↓
3. Build prompt:
   System: "Bạn là AI CodeGym, context: Java Module..."
   Sources: [chunk1, chunk2, ...]
   User: "Làm thế nào để cài đặt Java?"
    ↓
4. OpenAI GPT-4o: Stream response
## Performance

### Vector Search Optimization (Qdrant)
- **Qdrant HNSW index**: ~10-100x faster than brute force
- **Separate storage**: MySQL (metadata) + Qdrant (vectors) → cleaner architecture
- **Threshold filtering**: Chỉ lấy similarity > 0.7
- **Batch operations**: Upload nhiều embeddings cùng lúc
- **Filter support**: Tìm theo class/module context trực tiếp trong Qdrant
   - Lưu vào `knowledge_embeddings` table
4. HNSW index tự động optimize vector search

## Security

### Không gửi sensitive data cho LLM
- **SafeChatContext**: CHỈ chứa public info (name, role, class/module/lesson info)
- **SecurityAuditService**: Validate không có JWT tokens
- **NO Keycloak tokens** trong bất kỳ API call nào đến OpenAI

### Role-Based Access
- **STUDENT**: Chat, view own sessions
- **ADMIN**: Manage knowledge base, analytics, reindex

## Performance

### Vector Search Optimization
- **HNSW index**: ~10x faster than brute force
- **Separate embeddings table**: Cleaner data, better caching
- **Threshold filtering**: Chỉ lấy similarity > 0.7

### Async Processing
## Troubleshooting

### 1. Qdrant connection failed
- **Check Qdrant running**: `curl http://localhost:6333/collections`
- **Check docker-compose**: `docker-compose ps qdrant`
- **Check logs**: `docker-compose logs qdrant`
- **Port conflict**: Đảm bảo port 6333 không bị chiếm
- Analytics API để theo dõi chi phí

## Testing

### Test RAG
```bash
curl "http://localhost:7000/api/admin/knowledge/test-rag?query=Làm%20thế%20nào%20để%20cài%20đặt%20Java&classId=5"
```

### Test Chat Stream
```bash
curl -N -X POST http://localhost:7000/api/chat/stream \
  -H "Authorization: Bearer <JWT>" \
  -H "Content-Type: application/json" \
  -d '{"message":"Chào bạn, Java là gì?"}'
```

## Troubleshooting

### 1. Vector search không hoạt động
- **Check pgvector**: `SELECT * FROM pg_extension WHERE extname = 'vector';`
- **Check HNSW index**: `\d+ knowledge_embeddings` trong psql

### 2. OpenAI API errors
### 3. Embeddings không generate
- **Check logs**: `EmbeddingService` async errors
- **Check Qdrant collection**: `GET http://localhost:6333/collections/sis-knowledge-base`
- **Reindex**: `POST /api/admin/knowledge/reindex`
- **Manual check**: Browse Qdrant UI at `http://localhost:6333/dashboard`Service`

### 3. Embeddings không generate
- **Check logs**: `EmbeddingService` async errors
- **Reindex**: `POST /api/admin/knowledge/reindex`

## Roadmap (Optional)

- [ ] Multi-language support (English, Vietnamese)
- [ ] Feedback system (thumbs up/down)
- [ ] Advanced analytics (user satisfaction, topic clustering)
- [ ] External knowledge sources (web scraping, APIs)
- [ ] Fine-tuned embeddings model
- [ ] WebSocket notifications cho real-time updates

## License

Internal tool for CodeGym Vietnam.
