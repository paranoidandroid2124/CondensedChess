-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Users: Identity & Tier
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    tier VARCHAR(50) NOT NULL DEFAULT 'FREE', -- 'FREE', 'PRO'
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Subscriptions: Billing limits
CREATE TABLE subscriptions (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE', -- 'ACTIVE', 'CANCELED', 'PAST_DUE'
    current_period_end TIMESTAMP WITH TIME ZONE,
    monthly_analysis_usage INT DEFAULT 0
);

-- 3. Games: Metadata & S3 Pointer
CREATE TABLE games (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    
    -- Chess Metadata (Searchable)
    white_player VARCHAR(100),
    black_player VARCHAR(100),
    result VARCHAR(10), -- '1-0', '0-1', '1/2-1/2'
    date_played DATE,
    eco VARCHAR(10),
    
    -- The Pointer to Heavy Data
    pgn_headers JSONB DEFAULT '{}', -- Extra headers
    analysis_s3_key VARCHAR(255),   -- e.g. "games/{uuid}/analysis.json"
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Index for "My Games" page
CREATE INDEX idx_games_user_id ON games(user_id);

-- 4. Analysis Jobs: Worker Queue State
CREATE TABLE analysis_jobs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    game_id UUID REFERENCES games(id) ON DELETE CASCADE,
    
    status VARCHAR(50) NOT NULL DEFAULT 'QUEUED', -- 'QUEUED', 'PROCESSING', 'COMPLETED', 'FAILED'
    progress INT DEFAULT 0,
    error_message TEXT,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
