# 🤖 AI Research Platform

A production-grade RAG (Retrieval Augmented Generation) system with vector search, intelligent document chunking, and AI-powered question answering.

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-green)
![License](https://img.shields.io/badge/license-MIT-blue)

## 🌟 Features

- ✅ **RAG Pipeline**: Retrieval Augmented Generation for accurate AI responses  
- ✅ **Vector Search**: Semantic search using 768-dimensional embeddings  
- ✅ **Document Chunking**: Smart text splitting with overlap for context preservation  
- ✅ **Workflow Automation**: n8n integration for scheduled tasks  
- ✅ **Production Ready**: Error handling, logging, rate limiting, health checks  
- ✅ **API Documentation**: Interactive Swagger UI  

## 🏗️ Architecture
User / n8n → Spring Boot API → ChromaDB Vector Store
↓
Gemini AI (Embeddings + Generation)


See [ARCHITECTURE.md](docs/ARCHITECTURE.md) for detailed architecture.

## 🚀 Quick Start

### Prerequisites

- Java 17+
- Docker
- Maven 3.8+
- Gemini API Key

### 1. Start Dependencies

```bash
# Start ChromaDB
docker run -d --name chromadb -p 8000:8000 chromadb/chroma:latest

# Start n8n (optional)
docker run -d --name n8n -p 5678:5678 n8nio/n8n


2. Configure Application

Edit src/main/resources/application.properties:
gemini.api.key=YOUR_GEMINI_API_KEY_HERE
chroma.url=http://localhost:8000

3. Run Application
mvn spring-boot:run

4. Test API
# Health check
curl http://localhost:8080/api/health

# Add a document
curl -X POST http://localhost:8080/api/webhooks/add-document \
  -H "Content-Type: application/json" \
  -d '{"title":"Test","content":"This is a test document"}'

# Ask a question
curl -X POST http://localhost:8080/api/webhooks/ask \
  -H "Content-Type: application/json" \
  -d '{"question":"What is this about?"}'

5. View API Documentation

Open: http://localhost:8080/swagger-ui/index.html

📊 API Endpoints
Method	Endpoint	Description
POST	/api/webhooks/add-document	Add a document
POST	/api/webhooks/ask	Ask a question (RAG)
POST	/api/webhooks/research	Research a topic
GET	/api/health	Health check
GET	/api/health/metrics	System metrics
🧪 Running Tests
mvn test


Current test coverage: 15+ tests across services and controllers

🔧 Configuration
# Document Chunking
chunking.max-chunk-size=500
chunking.overlap-size=50

# Logging
logging.level.com.nethmadtharuka=INFO

📈 Performance

Embedding Generation: ~2 seconds per document

Query Response: ~3–5 seconds

Supported Documents: 100K+ (limited by ChromaDB)

Concurrent Requests: 10 requests/minute (rate limited)

🛠️ Tech Stack

Backend: Spring Boot 4.0, Java 17

AI/ML: Google Gemini AI

text-embedding-004 (768-dim vectors)

Gemini 2.0 Flash (text generation)

Vector DB: ChromaDB 1.3.1

Automation: n8n

Testing: JUnit 5, Mockito

Docs: Swagger/OpenAPI

Containerization: Docker

📚 Documentation

Architecture Overview

API Documentation

n8n Workflows

📄 License

MIT License

👨‍💻 Author

Nethma.D.Tharuka
Undergraduate | Backend & AI Systems Developer





