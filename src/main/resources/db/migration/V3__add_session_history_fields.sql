-- Migration: V3__add_session_history_fields.sql
-- Adds startedAt + userId to review_session, and rating to review_session_word_result.
-- Backward-compatible: unknown_count column is NOT dropped.

-- ── review_session: add started_at and user_id ──────────────────────────────

ALTER TABLE review_session
    ADD COLUMN started_at TIMESTAMPTZ,
    ADD COLUMN user_id    UUID;

-- Backfill: use completed_at as started_at for legacy rows
UPDATE review_session SET started_at = completed_at WHERE started_at IS NULL;

-- Backfill: assign nil UUID to legacy rows (single-user app)
UPDATE review_session SET user_id = '00000000-0000-0000-0000-000000000000' WHERE user_id IS NULL;

-- Enforce NOT NULL after backfill
ALTER TABLE review_session
    ALTER COLUMN started_at SET NOT NULL,
    ALTER COLUMN user_id    SET NOT NULL;

-- ── review_session_word_result: add rating ──────────────────────────────────

ALTER TABLE review_session_word_result
    ADD COLUMN rating VARCHAR(10);

-- Backfill: unknown_count = 0 → EASY, = 1 → GOOD, >= 2 → AGAIN
UPDATE review_session_word_result
    SET rating = CASE
        WHEN unknown_count = 0 THEN 'EASY'
        WHEN unknown_count = 1 THEN 'GOOD'
        ELSE 'AGAIN'
    END
    WHERE rating IS NULL;

ALTER TABLE review_session_word_result
    ALTER COLUMN rating SET NOT NULL;

-- ── Index: speed up history queries per user ────────────────────────────────

CREATE INDEX idx_review_session_user_completed
    ON review_session (user_id, completed_at DESC);
