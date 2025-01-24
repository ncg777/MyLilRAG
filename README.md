# MyLilRAG

MyLilRAG is a Java application that utilizes retrieval-augmented generation (RAG) techniques to assist users by retrieving relevant information from both internal knowledge bases (embedding store). 

## Features

- User-friendly GUI for interaction
- Real-time web search capabilities using the Tavily API
- Interaction with an internal embedding store (Neo4j)
- Elegant output display separating user messages from AI responses
- Document ingestion facility to update the knowledge base
- Threaded input processing to ensure a responsive UI

## Getting Started

### Prerequisites

Before running the application, ensure you have the following installed:

- [Java](https://www.java.com/)
- local ollama running nomic-embed-text for the embeddings.
- Neo4j database instance for the embedding store running in a Docker with NEO4J_AUTH set to none.

```bash
docker run \
    --restart always \
    --publish=7474:7474 --publish=7687:7687 \
    --env NEO4J_AUTH=none \
    --volume=/path/to/your/data:/data \
    neo4j:5.26.1
```
- OpenAI and Groq API keys set as OPENAI_API_KEY GROQ_API_KEY environment variables.

### Clone the Repository

```bash
git clone https://github.com/ncg777/MyLilRAG.git
cd MyLilRAG
