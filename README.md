# IntelliStudy — Advanced AI-Powered Learning Ecosystem 🧠🚀

IntelliStudy is a state-of-the-art **Contextual Enrichment Framework (CEF)** designed for academic and professional document interaction. Built with **Java 21** and **Spring Boot 3.5**, it integrates **Graph-Augmented RAG (Retrieval-Augmented Generation)** to provide deep semantic insights, automated assessments, and linguistic bridging.

---

## 🛠️ Performance & Technology Stack (Why & What?)

The technical choices behind IntelliStudy are mission-driven, prioritizing high-precision retrieval and secure scalability.

| Technology | Implementation | Why this was chosen? |
| :--- | :--- | :--- |
| **Java 21** | Backend Core | Modern syntax, Pattern Matching, and Virtual Threads readiness for high throughput. |
| **Spring Boot 3.5** | Framework | Industry-standard for robust, maintainable, and highly decoupled micro-services. |
| **Gemini 2.5 Flash** | Core LLM Engine | Superior speed-to-intelligence ratio and massive context window for long docs. |
| **DuckDB** | Vector DB | In-process analytical database allowing blazingly fast local vector search (768d). |
| **Redis** | Metadata Cache | Eliminates repetitive AI calls by caching RAG contexts and session history. |
| **JGraphT** | Semantic Graph | Enables BFS-based context expansion, solving the "lost-in-the-middle" problem. |
| **JWT + Rotation** | Security | Ensures statelessness while maintaining high security via DB-backed token rotation. |
| **Docker** | Orchestration | Guaranteeing "Works on My Machine" (WOMM) parity across environments. |

---

## 🏗️ The Engineering Architecture

At its core, IntelliStudy implements a **Clean Layered Architecture** powered by our proprietary **CEF** engine:

### 1. Mcp (Model Context Protocol) Routing Layer
The `McpAgentLayer` acts as the "Brain" of the system. Instead of simple prompting, it uses **Intent Classification** to route queries to specialized agents:
- **Intelligent Dispatcher**: Uses `gemini-2.5-flash` to classify user intent into discrete Tools (RAG_ASK, SUMMARIZE, QUIZ, TRANSLATE).
- **Session-Awareness**: Maintains context within the CEF layer for multi-turn document sessions.

### 2. The RAG Pipeline (Graph + Vector)
IntelliStudy uses a hybrid retrieval strategy combining semantic vector search with graph-based semantic traversal:
- **Vector Search (DuckDB)**: Text is split into chunks (1000 chars, 200 overlap) and embedded via `text-embedding-004`.
- **GraphRAG (JGraphT)**: Maps semantic relationships (Nodes/Edges) between extracted entities.
- **Graph-Boosted Scoring**: During retrieval, if a chunk contains a neighbor node (BFS Depth=2) of the query concept, it receives a **+0.15 score boost**.

$$FinalScore = VectorRankScore + \sum(GraphMatchBoost)$$

### 3. Identity & Persistence Layer
- **JWT + Refresh Token Rotation**: Secure stateless sessions with hashed MySQL-backed refresh identifiers.
- **Google OAuth2**: Seamless integration for Social Identity Providers.

---

## ✨ Features Walkthrough

- **🧠 Intelligent Q&A**: Graph-boosted RAG ensures answers are contextually accurate and topologically linked.
- **📄 Pro Summarization**: Multi-stage extraction designed for high-density academic papers (12k char window).
- **🇧🇩 English-Bangla Bridge**: High-fidelity translation maintaining domain-specific terminology.
- **📝 MCQ Generator**: 
  - **Dynamic Topic Mode**: Generates quizzes from a single keyword using semantic knowledge.
  - **Contextual Text Mode**: Extracts specific "factoids" from your uploaded PDF using AI.
- **🎓 Interactive Assessment**: Built-in score evaluation with semantic feedback and correct answer reviews.

---

## 🚀 Deployment Guide (Dockerized)

1. **Clone the repository**:
   ```bash
   git clone https://github.com/naim79992/IntelliStudy.git
   cd IntelliStudy
   ```
2. **Configure your environment**:
   - Create a `.env` file and add your `GEMINI_API_KEY`.
3. **Run the stack**:
   ```bash
   docker-compose up --build -d
   ```
4. **Access the dashboard**:
   `http://localhost:8080`

---

## 🖼️ Gallery & UI Documentation

### 🏠 System Home
<img width="1912" alt="Home Screen" src="https://github.com/user-attachments/assets/15ca0f26-b4cd-4460-9d0c-81efe71fc4c8" /> 

### 🧬 AI-Driven Quiz (Topic Specific)
<img width="1895" alt="Quiz by Topic" src="https://github.com/user-attachments/assets/b35b0227-1e40-4472-a565-88da38d1913a" />
<img width="1892" alt="Attempt Quiz" src="https://github.com/user-attachments/assets/5bdd2d44-0c63-47e1-960c-d8f65a7214e5" /> 

### 📊 Assessment Architecture (Text Based)
<img width="1896" alt="Quiz by Text" src="https://github.com/user-attachments/assets/c85cf651-846e-493d-99fc-a953e589e216" />
<img width="1900" alt="Quiz Result" src="https://github.com/user-attachments/assets/d68af7f1-8b97-4e85-a3eb-fef29808b9fa" />

### 📑 Contextual Intelligence (RAG)
<img width="1918" alt="Summarize" src="https://github.com/user-attachments/assets/082e466d-ce92-4660-a212-79b74c172701" /> 
<img width="1918" alt="Translate" src="https://github.com/user-attachments/assets/179190df-2968-48d6-9383-e54493d3613d" />

---
*Architected for Intelligence. Built for Learning.*  
**Developed by Naim .**
