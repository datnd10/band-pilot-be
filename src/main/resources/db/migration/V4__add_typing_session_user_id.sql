-- V4: Add user_id to typing_session for per-user streak queries
-- Step 1: Add column as nullable first
ALTER TABLE typing_session
  ADD COLUMN user_id UUID;

-- Step 2: Backfill nil UUID for existing rows
UPDATE typing_session
  SET user_id = '00000000-0000-0000-0000-000000000000'
  WHERE user_id IS NULL;

-- Step 3: Enforce NOT NULL constraint after backfill
ALTER TABLE typing_session
  ALTER COLUMN user_id SET NOT NULL;

-- Step 4: Index to speed up streak queries
CREATE INDEX idx_typing_session_user_completed
  ON typing_session (user_id, completed_at DESC);
