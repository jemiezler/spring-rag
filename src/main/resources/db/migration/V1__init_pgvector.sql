-- 1. Enable the vector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- 2. Create the table to store documents and their embeddings
CREATE TABLE IF NOT EXISTS document_embedding (
    id uuid PRIMARY KEY,
    content text,
    metadata jsonb,
    embedding vector(768) -- Matches nomic-embed-text
);

-- 3. Create an index for faster RAG retrieval
CREATE INDEX ON document_embedding USING hnsw (embedding vector_cosine_ops);