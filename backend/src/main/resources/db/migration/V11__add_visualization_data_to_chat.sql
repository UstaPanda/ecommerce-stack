-- Add visualization_data and is_out_of_scope to chat_messages
ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS visualization_data TEXT;
ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS is_out_of_scope BOOLEAN NOT NULL DEFAULT FALSE;

-- Optional: Rename visualization_code to something else if needed, but keeping it for now
