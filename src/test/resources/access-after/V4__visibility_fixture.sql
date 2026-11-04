-- Same test-only operation ordered after the foundation migration.
ALTER TABLE games ADD COLUMN visibility VARCHAR(16) DEFAULT 'PUBLIC';
UPDATE games SET visibility = 'PRIVATE' WHERE id = 'legacy-game';
