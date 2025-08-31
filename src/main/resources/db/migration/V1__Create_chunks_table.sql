-- Create chunks table for storing document chunks with embeddings
CREATE TABLE IF NOT EXISTS chunks (
    chunk_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    file_id UUID NOT NULL,
    text TEXT NOT NULL,
    embedding BYTEA,
    chunk_index INTEGER NOT NULL,
    strategy VARCHAR(50) NOT NULL,
    text_length INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Add foreign key constraint separately to handle potential issues
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fk_chunks_file_id' 
        AND table_name = 'chunks'
    ) THEN
        ALTER TABLE chunks ADD CONSTRAINT fk_chunks_file_id 
        FOREIGN KEY (file_id) REFERENCES file_entity(file_id) ON DELETE CASCADE;
    END IF;
END $$;

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_chunks_file_id ON chunks(file_id);
CREATE INDEX IF NOT EXISTS idx_chunks_strategy ON chunks(strategy);
CREATE INDEX IF NOT EXISTS idx_chunks_file_strategy ON chunks(file_id, strategy);

-- Add comment for documentation
COMMENT ON TABLE chunks IS 'Stores document chunks with embeddings for vector search';
COMMENT ON COLUMN chunks.embedding IS 'Vector embedding stored as byte array';
COMMENT ON COLUMN chunks.strategy IS 'Chunking strategy used: character, sentence, or paragraph';