CREATE TABLE IF NOT EXISTS user_profiles (
    id BIGINT PRIMARY KEY CHECK (id = 1),
    name VARCHAR(120) NOT NULL,
    preferred_nickname VARCHAR(120),
    age INTEGER,
    gender VARCHAR(120),
    bio TEXT,
    interests TEXT,
    favorite_topics TEXT,
    communication_preferences TEXT,
    things_to_know TEXT,
    topics_to_avoid TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_user_profile_age CHECK (age IS NULL OR age BETWEEN 1 AND 130)
);

CREATE TABLE IF NOT EXISTS personal_facts (
    id BIGSERIAL PRIMARY KEY,
    content TEXT NOT NULL,
    status VARCHAR(16) NOT NULL,
    source VARCHAR(24) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_personal_fact_status
        CHECK (status IN ('PROPOSED', 'CONFIRMED', 'REJECTED'))
);

CREATE INDEX IF NOT EXISTS idx_personal_facts_status_created
    ON personal_facts (status, created_at DESC);
