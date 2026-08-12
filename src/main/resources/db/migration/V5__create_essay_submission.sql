-- V5: Create essay_submission table to store IELTS Writing Task 2 practice history

CREATE TABLE essay_submission (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL,
    question            TEXT NOT NULL,
    essay               TEXT NOT NULL,
    task_achievement    NUMERIC(3,1) NOT NULL,
    coherence_cohesion  NUMERIC(3,1) NOT NULL,
    lexical_resource    NUMERIC(3,1) NOT NULL,
    grammatical_range   NUMERIC(3,1) NOT NULL,
    overall_band        NUMERIC(3,1) NOT NULL,
    strengths           TEXT[],
    improvements        TEXT[],
    improved_version    TEXT,
    encouragement       TEXT,
    submitted_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_essay_submission_user_submitted
    ON essay_submission (user_id, submitted_at DESC);
