# AIMailExchangeSimulator

AIMailExchangeSimulator is a Java application that lets the user simulate email
exchanges between different personas defined in a json file.

The personas are defined by 3 strings in an array ["NAME", "EMAILADDRESS","BIO"].
Those arrays are just stored in a json array in a json file named personas.json.

You may select a persona to embody, the user persona, and a persona to exchange 
with, the agent persona, handled by AI. You can also simulate a round of 
exchanges by clicking AI Follow-up; this will generate 2 mail interactions, 
leaving it back to your turn.

You may see the list of mails in the system on the left, and the currently
selected mail as plaintext on the right.

You may input a message to send to the agent persona and the selected message
will be sent along as context.

Generated mail are saved in as strings in a JSON file and also 
in a local neo4j vector database so that when AI writes the mail, it may 
retrieve the entire history of mail exchanges in the system by vectors.

You may also just ingest documents in the vector database by putting them in the
toIngest folder; they will be ingested and archived upon the startup of the program.

The Agent also has a tool to fetch any URL that will convert HTML files to 
Markdown. Downloaded files will be saved in the archive folder.

## Features

- User-friendly GUI for interaction
- Interaction with an internal embedding store (Neo4j)
- Elegant output display separating user messages from AI responses
- Document ingestion facility to update the knowledge base
- Threaded input processing to ensure a responsive UI

## Getting Started

### Prerequisites

Before running the application, ensure you have the following installed/setup:

- [Java](https://www.java.com/)
- local ollama running nomic-embed-text for the embeddings.
- Neo4j database instance for the embedding store running in a Docker with NEO4J_AUTH set to none.
- Optionally OpenAI and/or Groq API keys set as OPENAI_API_KEY and GROQ_API_KEY environment variables.

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
