-- Test-only private values must survive either migration ordering.
ALTER TABLE games ADD COLUMN IF NOT EXISTS visibility VARCHAR(16) DEFAULT 'PUBLIC';
UPDATE games SET visibility = 'PRIVATE' WHERE id = 'legacy-game';
