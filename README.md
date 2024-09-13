# MyLilRAG

MyLilRAG is a simple Java application that utilizes retrieval-augmented generation (RAG) techniques to assist users by retrieving relevant information from both internal knowledge bases (embedding store) and real-time web search capabilities. It features a user-friendly graphical interface where users can input queries and receive responses based on a combination of stored knowledge and live web data.

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

- [Java JDK 17](https://www.oracle.com/java/technologies/javase-jdk17-downloads.html)
- [Apache Maven](https://maven.apache.org/download.cgi)
- Neo4j database instance for the embedding store running in a Docker with NEO4J_AUTH set to none.
- Tavily API key for web search capabilities  set as TAVILY_API_KEY in your environment variables
- Groq API key for OpenAI model usage set as GROQ_API_KEY in your environment variables.

### Clone the Repository

```bash
git clone https://github.com/ncg777/MyLilRAG.git
cd MyLilRAG
