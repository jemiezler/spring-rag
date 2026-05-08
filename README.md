# Spring RAG

A Spring Boot application implementing **Retrieval Augmented Generation (RAG)** using Spring AI, PostgreSQL pgvector, and Ollama for local LLM inference.

## Overview

Spring RAG enables you to:
- **Ingest Documents**: Upload and process PDF files, splitting them into manageable chunks
- **Generate Embeddings**: Convert document chunks into vector embeddings using Ollama
- **Store Vectors**: Persist embeddings in PostgreSQL using pgvector for efficient similarity search
- **Query with Context**: Ask questions and receive answers grounded in your ingested documents via semantic search

This application provides a complete RAG pipeline running locally without external API dependencies.

## Tech Stack

- **Framework**: Spring Boot 4.0.6
- **AI Framework**: Spring AI 2.0.0-M5
- **Language**: Java 25
- **Database**: PostgreSQL 16 with pgvector extension
- **LLM**: Ollama (local inference)
- **Build Tool**: Maven
- **Container**: Docker & Docker Compose

### Key Dependencies

- **spring-ai-starter-model-ollama**: Ollama integration for chat and embeddings
- **spring-ai-pdf-document-reader**: PDF document parsing
- **pgvector**: Vector similarity search in PostgreSQL
- **Spring Data JPA**: Data access layer
- **Lombok**: Boilerplate reduction
- **Spring Validation**: Input validation

## Prerequisites

Before running the application, ensure you have:

- **Java 25 JDK** - [Eclipse Temurin](https://adoptium.net/) or similar
- **Maven 3.8+** - Build tool
- **Docker & Docker Compose** - For PostgreSQL and Ollama
- **Ollama** - Local LLM runtime (see [ollama.ai](https://ollama.ai))
- **Git** - Version control

### Required Models

You'll need to pull these Ollama models:

```bash
ollama pull nomic-embed-text   # For embeddings (768 dimensions)
ollama pull gemma4             # For chat/reasoning (~7B parameter model)
```

> **Note**: Adjust the model names in `application.yml` if you prefer different models. Ensure embedding models output the configured dimension size (768).

## Quick Start

### 1. Start Infrastructure

Start PostgreSQL with pgvector:

```bash
docker-compose up -d postgres
```

Verify PostgreSQL is running:
```bash
docker-compose ps
```

### 2. Start Ollama

Start Ollama (either as a service or container):

**Option A: Mac/Windows/Linux Desktop**
```bash
ollama serve
```

**Option B: Docker**
```bash
# Add to docker-compose.yml and uncomment the ollama service, then run:
docker-compose up -d ollama
```

Verify Ollama is accessible:
```bash
curl http://localhost:11434/api/tags
```

### 3. Build & Run Application

```bash
# Build the project
./mvnw clean package

# Run the application
./mvnw spring-boot:run
```

The application starts on `http://localhost:8080`

### 4. Verify Setup

Check the logs for successful database migration and connection:
```
o.springframework.boot.StartupInfoLogger : Started SpringRagApplication
```

## API Endpoints

### Document Ingestion

**Upload and ingest a PDF document:**

```bash
curl -X POST http://localhost:8080/api/documents/ingest \
  -F "file=@path/to/document.pdf"
```

**Response:**
```json
{
  "source": "document.pdf",
  "chunksStored": 42,
  "message": "Successfully ingested 42 chunks from 'document.pdf'"
}
```

**Parameters:**
- `file` (required): PDF file to ingest (multipart form data)

### RAG Query

**Ask a question about ingested documents:**

```bash
curl "http://localhost:8080/api/rag/ask?query=What%20is%20the%20main%20topic?"
```

**Response:**
```
Based on the documents, the main topic is... [answer with context from documents]
```

**Parameters:**
- `query` (required): Natural language question

## Configuration

Edit `src/main/resources/application.yml` to customize:

```yaml
spring:
  ai:
    vectorstore:
      pgvector:
        initialize-schema: true      # Auto-create tables
        table-name: document_embedding
        index-type: HNSW            # HNSW or IVFFlat
        distance-type: COSINE_DISTANCE
        dimensions: 768              # Must match embedding model output
    
    ollama:
      base-url: http://localhost:11434
      embedding:
        options:
          model: nomic-embed-text
      chat:
        options:
          model: gemma4
          temperature: 0.7           # 0 = deterministic, 1 = creative
          num-predict: 2048          # Max response tokens

  datasource:
    url: jdbc:postgresql://localhost:5432/spring_ai_rag
    username: postgres
    password: postgres
```

### Key Tuning Parameters

- **chunk-size** (DocumentIngestionService): Controls document chunk size (500 tokens default)
- **topK**: Number of similar documents to retrieve (default: 5)
- **temperature**: LLM creativity (0.0–1.0)
- **model**: Change embedding or chat models
- **distance-type**: COSINE_DISTANCE (recommended) or other pgvector metrics

## Project Structure

```
src/main/java/com/jemiezler/spring_rag/
├── config/
│   ├── ChatClientConfig.java          # ChatClient bean setup
│   └── ManualIngestor.java            # Utility for manual document ingestion
├── controller/
│   ├── ChatController.java            # /api/rag endpoints
│   └── DocumentController.java        # /api/documents endpoints
├── service/
│   ├── RagService.java                # Query & context retrieval logic
│   └── DocumentIngestionService.java  # PDF parsing & embedding
├── dto/
│   └── DocIngestionResponse.java      # API response DTO
├── exception/
│   └── RagAppException.java           # Custom exception handling
└── SpringRagApplication.java          # Entry point

src/main/resources/
├── application.yml                    # Configuration
├── db/migration/
│   └── V1__init_pgvector.sql         # Database initialization
└── prompts/
    └── rag-user-prompt.st            # RAG system prompt template
```

## Database

### Schema

The application automatically creates the required schema on startup:

**document_embedding table:**
- `id`: Unique identifier
- `document_id`: Reference to source document
- `chunk_content`: Text chunk
- `embedding`: Vector (768 dimensions with HNSW index)
- `metadata`: Document source/page info
- `created_at`: Ingestion timestamp

### Migrations

Flyway-managed migrations in `src/main/resources/db/migration/`:
- `V1__init_pgvector.sql`: Initialize pgvector extension and schema

## Docker Deployment

### Build Image

```bash
./mvnw clean package
docker build -t spring-rag:latest .
```

### Run Container

```bash
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/spring_ai_rag \
  -e SPRING_AI_OLLAMA_BASE_URL=http://ollama:11434 \
  --network spring-rag-environment \
  spring-rag:latest
```

### Full Stack with Compose

```bash
# Update docker-compose.yml to include the RAG service, then:
docker-compose up
```

## Troubleshooting

### Connection Issues

**PostgreSQL won't connect:**
```bash
# Check if container is running
docker-compose ps postgres

# View logs
docker-compose logs postgres

# Test connection
psql -h localhost -U postgres -d spring_ai_rag
```

**Ollama not responding:**
```bash
# Check if Ollama is running
curl http://localhost:11434/api/tags

# View logs
ollama logs
```

### Ingestion Errors

**PDF parsing fails:**
- Verify PDF is not corrupted
- Check file size limits (adjust if needed)
- Ensure UTF-8 encoding

**Vector dimension mismatch:**
- Verify embedding model: `ollama list`
- Update `dimensions` in `application.yml` to match model output
- Regenerate embeddings

### Query Returns No Results

- Verify documents were ingested: Check `document_embedding` table row count
- Ensure similarity threshold isn't too high
- Try broader query terms

## Development

### Local Development

```bash
# Start services
docker-compose up -d postgres
ollama serve

# Run tests
./mvnw test

# Run with hot reload
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.devtools.restart.enabled=true"
```

### Building

```bash
# Build without tests
./mvnw clean package -DskipTests

# Build with all checks
./mvnw clean verify
```

## Performance Considerations

- **Chunk Size**: 400–800 tokens optimal for RAG (configured: 500)
- **Embedding Model**: nomic-embed-text is 768-dim, fast, and accurate
- **Vector Index**: HNSW for fast similarity search on large datasets
- **Database Pooling**: Configured via Spring DataSource settings
- **Caching**: Consider caching embeddings for frequently queried documents

## Security Notes

⚠️ **Current Configuration is for Development Only**

Before production deployment:
- [ ] Change PostgreSQL default credentials
- [ ] Enable database SSL/TLS
- [ ] Restrict API access with authentication/authorization
- [ ] Use environment variables for sensitive config
- [ ] Enable input validation and sanitization
- [ ] Add rate limiting to API endpoints
- [ ] Use secrets management (AWS Secrets Manager, HashiCorp Vault, etc.)

## Known Limitations

- **Local Models Only**: Designed for local Ollama models (7-13B parameters recommended)
- **Single-User**: No built-in multi-tenancy or user isolation
- **Synchronous Processing**: Document ingestion is blocking (consider async for large files)
- **Memory Usage**: Larger models and batch operations require significant RAM

## Future Enhancements

- [ ] Async document processing with job queues
- [ ] Web UI for document management
- [ ] Multi-model support (routing to different models)
- [ ] Persistent chat history and conversation tracking
- [ ] Fine-tuning workflows for domain-specific models
- [ ] Batch query optimization
- [ ] Document metadata indexing and filtering

## Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is open source and available under the MIT License.

## Support & Resources

- **Spring AI Docs**: https://docs.spring.io/spring-ai/
- **Ollama**: https://ollama.ai
- **pgvector**: https://github.com/pgvector/pgvector
- **Spring Boot**: https://spring.io/projects/spring-boot

## Author

Created by `jemiezler`

---

**Last Updated**: May 2026 | **Java**: 25 | **Spring Boot**: 4.0.6
