-- Test-only additive stand-in for the separate access change.
ALTER TABLE games ADD COLUMN visibility VARCHAR(16) DEFAULT 'PUBLIC';
UPDATE games SET visibility = 'PRIVATE' WHERE id = 'legacy-game';
