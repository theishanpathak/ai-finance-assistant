# 💰 FinAssist AI - Finance Tutor Chatbot

An AI-powered finance tutor that explains concepts through real-time conversations. Built to learn streaming, conversation memory, and token management before diving into RAG systems.


## 🚀 Live Demo

**Frontend:** [https://finassist-ai.netlify.app/](https://finassist-ai.netlify.app/)  
**Backend:** Deployed on Heroku (PostgreSQL)

---

## ✨ Features

### Real-Time Streaming
- Responses appear word-by-word (like ChatGPT)
- Server-Sent Events (SSE) for live streaming
- Proper buffering for smooth text rendering

### Conversation Memory
- Multi-turn conversations with context
- PostgreSQL stores full chat history
- Session restoration on page refresh
- No login required (UUID-based sessions)

### Smart Token Management
- Counts tokens per message (jtokkit library)
- Auto-truncates old messages when limit reached
- Prevents context window overflow
- Tracks token usage in database

### Finance-Focused
- Specialized tutor for finance topics only
- Politely redirects off-topic questions
- Markdown formatting (bold, lists, tables)
- Concise 300-word responses

---

## 🛠️ Tech Stack

**Frontend:**
- React 18
- react-markdown (remark-gfm)
- Server-Sent Events
- LocalStorage sessions
- Deployed on Netlify

**Backend:**
- Spring Boot 3 (Java 17)
- Spring WebFlux (reactive HTTP)
- PostgreSQL + Spring Data JPA
- OpenAI API (gpt-4o-mini)
- jtokkit (token counting)
- Deployed on Heroku via GitHub Actions

**Infrastructure:**
- Docker Compose (local dev)
- GitHub Actions (CI/CD)
- Heroku Postgres

---

## 🏗️ Architecture
```
React Frontend (Netlify)
    ↓ SSE
Spring Boot API (Heroku)
    ↓
OpenAI API + PostgreSQL
```

**Database Schema:**
```
conversations (id, session_id, created_at)
messages (id, conversation_id, role, content, tokens, created_at)
```

---

## 🚀 Running Locally

### Prerequisites
- Java 17+
- Node.js 18+
- Docker (for PostgreSQL)
- OpenAI API key

### Backend Setup
```bash
# 1. Start PostgreSQL
docker-compose up -d

# 2. Set environment variables
export OPENAI_API_KEY=sk-your-key-here

# 3. Run Spring Boot
cd backend
./mvnw spring-boot:run
```

Backend runs on `http://localhost:8080`

### Frontend Setup
```bash
cd frontend
npm install
npm start
```

Frontend runs on `http://localhost:3000`

---

## 📦 Deployment

### Backend (Heroku + GitHub Actions)

**Took 13 attempts to get right** (learned a lot about Heroku quirks)

`.github/workflows/deploy.yml`:
```yaml
name: Deploy to Heroku
on:
  push:
    branches: [main]
jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: akhileshns/heroku-deploy@v3.12.14
        with:
          heroku_api_key: ${{secrets.HEROKU_API_KEY}}
          heroku_app_name: "your-app-name"
          heroku_email: "your-email"
```

**Environment Variables (Heroku):**
- `OPENAI_API_KEY`
- `DATABASE_URL` (auto-set by Heroku Postgres)

### Frontend (Netlify)

1. Connect GitHub repo to Netlify
2. Build command: `npm run build`
3. Publish directory: `build`


---

## 🎓 What I Learned

Built this to prepare for RAG systems. Key concepts mastered:

### 1. Streaming Responses (SSE)
- Server-Sent Events protocol
- Buffering incomplete chunks
- Reactive programming with Flux

### 2. Conversation Memory
- Session management without auth
- Context window limitations
- Message history persistence

### 3. Token Management
- Token counting (not just character count)
- Context truncation strategies
- Cost optimization

### 4. Error Handling
- Retry logic with exponential backoff
- Network failure graceful degradation

### 5. CI/CD
- GitHub Actions workflows
- Heroku deployment (13 failed attempts taught me patience)
- Environment variable management

---

## 🐛 Challenges & Solutions

**Challenge:** SSE chunks arriving incomplete  
**Solution:** Buffer management - accumulate until `\n\n` delimiter

**Challenge:** Context window overflow (4096 tokens)  
**Solution:** Token counting + automatic truncation of oldest messages

**Challenge:** 13 failed deployments to Heroku  
**Solution:** Learned Procfile, buildpacks, PORT binding ($PORT not 8080)

---

## 🎯 Project Goals

This isn't production-ready finance advice software. It's a **learning project** where I:

- ✅ Learned real-time streaming (SSE)
- ✅ Built conversation memory from scratch
- ✅ Understood token management
- ✅ Practiced error handling
- ✅ Deployed full-stack app with CI/CD
- ✅ Prepared for RAG/vector search (Month 2)

**Next:** Building vector search from scratch + RAG systems

---

## 🤔 FAQ

**Q: Why finance-only?**  
A: Narrowing scope helps AI give better answers. General chatbot = shallow. Focused tutor = actually helpful.

**Q: Why no authentication?**  
A: Learning project focused on AI integration, not auth systems. Sessions work fine for demo purposes.

**Q: 13 deployment attempts??**  
A: Heroku's `PORT` binding, buildpack config, Procfile syntax... each failure taught me something.

**Q: Why not use LangChain?**  
A: Built everything manually to understand how streaming, memory, and token management actually work.

---

## 📝 License

MIT - Clone it, learn from it, build your own version

---

## 🙏 Credits

Built with guidance from Claude AI for architecture decisions and learning explanations.

Special thanks to:
- OpenAI for the API
- The 13 failed Heroku deployments (you taught me patience)
- Stack Overflow (as always)

---


## 🔗 Connect With Me

Building in public and sharing my AI learning journey:
- **Portfolio**: [@theishanpathak](https://theishanpathak.com)
- **LinkedIn**: [Connect with me](https://www.linkedin.com/in/ishan-pathak333/)

---
