# AIMailExchangeSimulator

AIMailExchangeSimulator is a Java application that lets the user simulate email
exchanges between different fictitious personas defined in a json file.

The personas are defined by 3 strings in an array ["NAME", "EMAILADDRESS","BIO"].
Those arrays are just stored in a json array in a json file named personas.json.

You may select a persona to embody and a persona to exchange with that will be 
handled by AI. You can simulate a round of exchange by clicking AI Followup;
that will generate 2 mail interactions, leaving it back to your turn.

Generated mail are saved in an archive folder and also saved in the neo4j vector
database so that when AI writes the mail, it may consult the entire history of
mail exchanges in the system.

You may also ingest documents in the vector database by putting them in the
toIngest folder; they will be ingested upon the startup of the program.


## Features

- User-friendly GUI for interaction
- Interaction with an internal embedding store (Neo4j)
- Elegant output display separating user messages from AI responses
- Document ingestion facility to update the knowledge base
- Threaded input processing to ensure a responsive UI

## Getting Started

### Prerequisites

Before running the application, ensure you have the following installed:

- [Java](https://www.java.com/)
- OpenAI and Groq API keys set as OPENAI_API_KEY GROQ_API_KEY environment variables.
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

### Clone the Repository

```bash
git clone https://github.com/ncg777/AIMailExchangeSimulator.git
cd AIMailExchangeSimulator
