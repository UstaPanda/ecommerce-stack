-- V3: Chat session and message tables

CREATE TABLE chat_sessions (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE chat_messages (
    id                 BIGSERIAL PRIMARY KEY,
    session_id         BIGINT    NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    role               VARCHAR(20) NOT NULL,
    content            TEXT      NOT NULL,
    sql_query          TEXT,
    visualization_code TEXT,
    created_at         TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_chat_sessions_user_id    ON chat_sessions(user_id);
CREATE INDEX idx_chat_messages_session_id ON chat_messages(session_id);
