# AI Research Platform - Architecture

## System Overview
```
┌─────────────────────────────────────────────────────────────────┐
│                    SYSTEM ARCHITECTURE                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────┐         ┌──────────────┐         ┌──────────┐     │
│  │   n8n    │────────▶│  Spring Boot │────────▶│ ChromaDB│     │
│  │ Workflows│  HTTP   │   REST API   │  HTTP   │  Vector  │     │
│  └──────────┘         └──────────────┘         │   Store  │     │
│                              │                 └──────────┘     │
│                              │                                  │
│                              ▼                                  │
│                       ┌─────────────┐                           │
│                       │  Gemini AI  │                           │ 
│                       │  -Embeddings│                           │
│                       │  -Generation│                           │
│                       └─────────────┘                           │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## Data Flow - Document Addition
```
1. User/n8n sends document
        ↓
2. Webhook receives request
        ↓
3. Input validation
        ↓
4. Document chunking (500 char chunks, 50 char overlap)
        ↓
5. Generate embeddings (Gemini text-embedding-004)
        ↓
6. Store in ChromaDB (with metadata)
        ↓
7. Return success response
```

## Data Flow - RAG Query
```
1. User asks question
        ↓
2. Generate query embedding
        ↓
3. Search ChromaDB (cosine similarity)
        ↓
4. Retrieve top-k relevant chunks
        ↓
5. Construct prompt with context
        ↓
6. Generate answer (Gemini 2.0 Flash)
        ↓
7. Return answer + sources
```

## Technology Stack

- **Backend**: Spring Boot 4.0, Java 17
- **AI/ML**: Google Gemini AI (2.0 Flash, text-embedding-004)
- **Vector DB**: ChromaDB 1.3.1
- **Automation**: n8n
- **Containerization**: Docker
- **API Docs**: Swagger/OpenAPI

## Key Design Decisions

### Why ChromaDB?
- Open-source and free
- Built specifically for AI applications
- Supports metadata filtering
- Easy Docker deployment
- Production-ready

### Why Gemini?
- Free tier with generous limits
- High-quality embeddings (768 dimensions)
- Fast response times
- Good text generation quality

### Why Chunking?
- Token limits in AI models
- Better semantic precision
- Improved retrieval quality
- Handles large documents

## Scalability Considerations

- **Current**: Handles ~10K documents
- **Bottleneck**: In-memory rate limiting
- **Future**: Redis for distributed rate limiting
- **Future**: Load balancer for multiple instances