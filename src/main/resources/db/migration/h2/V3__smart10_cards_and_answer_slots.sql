CREATE TABLE smart10_cards (
    id UUID PRIMARY KEY,
    question_version_id UUID NOT NULL,
    position INTEGER NOT NULL,
    text VARCHAR(1000) NOT NULL,
    solution_boolean BOOLEAN,
    solution_text VARCHAR(1000),
    solution_rank INTEGER,
    CONSTRAINT fk_smart10_card_question FOREIGN KEY (question_version_id) REFERENCES question_versions(id),
    CONSTRAINT uq_smart10_card_position UNIQUE (question_version_id, position),
    CONSTRAINT uq_smart10_card_pair UNIQUE (question_version_id, id),
    CONSTRAINT uq_smart10_solution_rank UNIQUE (question_version_id, solution_rank),
    CONSTRAINT ck_smart10_card_position CHECK (position BETWEEN 1 AND 12),
    CONSTRAINT ck_smart10_solution_rank CHECK (solution_rank IS NULL OR solution_rank BETWEEN 1 AND 12),
    CONSTRAINT ck_smart10_solution_one CHECK (
        (CASE WHEN solution_boolean IS NULL THEN 0 ELSE 1 END)
      + (CASE WHEN solution_text IS NULL THEN 0 ELSE 1 END)
      + (CASE WHEN solution_rank IS NULL THEN 0 ELSE 1 END) = 1)
);

ALTER TABLE player_answers ADD COLUMN smart10_card_id UUID;
ALTER TABLE player_answers ADD COLUMN answer_slot_key VARCHAR(64);
UPDATE player_answers SET answer_slot_key = 'question';
ALTER TABLE player_answers ALTER COLUMN answer_slot_key SET NOT NULL;
ALTER TABLE player_answers DROP CONSTRAINT uq_answer_question;
ALTER TABLE player_answers ADD CONSTRAINT uq_answer_slot
    UNIQUE (participant_id, question_version_id, answer_slot_key);
ALTER TABLE player_answers ADD CONSTRAINT ck_answer_slot_kind CHECK (
    (smart10_card_id IS NULL AND answer_slot_key = 'question') OR
    (smart10_card_id IS NOT NULL AND answer_slot_key LIKE 'card:%')
);
ALTER TABLE player_answers ADD CONSTRAINT fk_answer_smart10_card
    FOREIGN KEY (question_version_id, smart10_card_id)
    REFERENCES smart10_cards(question_version_id, id);
