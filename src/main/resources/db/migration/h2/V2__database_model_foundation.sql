-- Additive foundation. Legacy tables and IDs remain available during the transition.
CREATE TABLE game_families (id UUID PRIMARY KEY);
ALTER TABLE games ADD COLUMN family_id UUID;
ALTER TABLE games ADD COLUMN copied_from_game_id VARCHAR(255);
ALTER TABLE games ADD COLUMN current_version_id UUID;
ALTER TABLE games ADD COLUMN deleted_at TIMESTAMP(6) WITH TIME ZONE;
UPDATE games SET family_id = RANDOM_UUID();
INSERT INTO game_families (id) SELECT family_id FROM games;
ALTER TABLE games ALTER COLUMN family_id SET NOT NULL;
ALTER TABLE games ADD CONSTRAINT fk_games_family FOREIGN KEY (family_id) REFERENCES game_families(id);
ALTER TABLE games ADD CONSTRAINT fk_games_copy FOREIGN KEY (copied_from_game_id) REFERENCES games(id);

CREATE TABLE question_versions (
    id UUID PRIMARY KEY,
    question_id BIGINT,
    version_number INTEGER NOT NULL,
    type VARCHAR(80) NOT NULL,
    text VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_question_version_identity FOREIGN KEY (question_id) REFERENCES questions(id),
    CONSTRAINT uq_question_version_number UNIQUE (question_id, version_number)
);
ALTER TABLE questions ADD COLUMN current_version_id UUID;
ALTER TABLE questions ADD CONSTRAINT fk_question_current_version FOREIGN KEY (current_version_id) REFERENCES question_versions(id);
INSERT INTO question_versions (id, question_id, version_number, type, text, created_at)
SELECT RANDOM_UUID(), id, 1, type, text, CURRENT_TIMESTAMP FROM questions;
UPDATE questions q SET current_version_id = (SELECT v.id FROM question_versions v WHERE v.question_id = q.id);

CREATE TABLE game_versions (
    id UUID PRIMARY KEY,
    game_id VARCHAR(255) NOT NULL,
    version_number INTEGER NOT NULL,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(2000),
    game_type VARCHAR(80) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_game_version_identity FOREIGN KEY (game_id) REFERENCES games(id),
    CONSTRAINT uq_game_version_number UNIQUE (game_id, version_number),
    CONSTRAINT uq_game_version_game_id UNIQUE (game_id, id)
);
INSERT INTO game_versions (id, game_id, version_number, title, description, game_type, created_at)
SELECT RANDOM_UUID(), id, 1, name, description, game_type, CURRENT_TIMESTAMP FROM games;
UPDATE games g SET current_version_id = (SELECT v.id FROM game_versions v WHERE v.game_id = g.id);
ALTER TABLE games ADD CONSTRAINT fk_game_current_version FOREIGN KEY (id, current_version_id) REFERENCES game_versions(game_id, id);

CREATE TABLE game_version_questions (
    game_version_id UUID NOT NULL,
    question_version_id UUID NOT NULL,
    position INTEGER NOT NULL,
    PRIMARY KEY (game_version_id, position),
    CONSTRAINT uq_gvq_question UNIQUE (game_version_id, question_version_id),
    CONSTRAINT fk_gvq_game_version FOREIGN KEY (game_version_id) REFERENCES game_versions(id),
    CONSTRAINT fk_gvq_question_version FOREIGN KEY (question_version_id) REFERENCES question_versions(id)
);
INSERT INTO game_version_questions (game_version_id, question_version_id, position)
SELECT g.current_version_id, q.current_version_id,
       CAST(ROW_NUMBER() OVER (PARTITION BY q.game_id ORDER BY q.question_order NULLS LAST, q.id) - 1 AS INTEGER)
FROM questions q JOIN games g ON g.id = q.game_id;

CREATE TABLE game_version_branches (
    game_version_id UUID NOT NULL,
    position INTEGER NOT NULL,
    branch VARCHAR(200),
    PRIMARY KEY (game_version_id, position),
    CONSTRAINT fk_gvb_game_version FOREIGN KEY (game_version_id) REFERENCES game_versions(id)
);
INSERT INTO game_version_branches (game_version_id, position, branch)
SELECT g.current_version_id, b.branch_order, b.branch
FROM game_branches b JOIN games g ON g.id = b.game_id;

ALTER TABLE answer_options ADD COLUMN question_version_id UUID;
UPDATE answer_options a SET question_version_id = (SELECT q.current_version_id FROM questions q WHERE q.id = a.question_id);
ALTER TABLE answer_options ADD CONSTRAINT fk_option_question_version
    FOREIGN KEY (question_version_id) REFERENCES question_versions(id);

CREATE TABLE tags (id UUID PRIMARY KEY, name VARCHAR(120) NOT NULL UNIQUE);
INSERT INTO tags (id, name)
SELECT RANDOM_UUID(), subject FROM (SELECT DISTINCT subject FROM games WHERE subject IS NOT NULL AND TRIM(subject) <> '') existing_subjects;
CREATE TABLE game_version_tags (
    game_version_id UUID NOT NULL,
    tag_id UUID NOT NULL,
    PRIMARY KEY (game_version_id, tag_id),
    CONSTRAINT fk_gvt_game_version FOREIGN KEY (game_version_id) REFERENCES game_versions(id),
    CONSTRAINT fk_gvt_tag FOREIGN KEY (tag_id) REFERENCES tags(id)
);
INSERT INTO game_version_tags (game_version_id, tag_id)
SELECT g.current_version_id, t.id FROM games g JOIN tags t ON t.name = g.subject;

-- Explicit legacy IDs do not advance H2 identity generators by themselves.
ALTER TABLE questions ALTER COLUMN id RESTART WITH
    (SELECT COALESCE(MAX(id), 0) + 1 FROM questions);
ALTER TABLE answer_options ALTER COLUMN id RESTART WITH
    (SELECT COALESCE(MAX(id), 0) + 1 FROM answer_options);

CREATE TABLE true_false_configs (
    question_version_id UUID PRIMARY KEY,
    correct_value BOOLEAN NOT NULL,
    CONSTRAINT fk_true_false_version FOREIGN KEY (question_version_id) REFERENCES question_versions(id)
);
CREATE TABLE ordering_items (
    id UUID PRIMARY KEY,
    question_version_id UUID NOT NULL,
    text VARCHAR(1000) NOT NULL,
    correct_position INTEGER NOT NULL,
    CONSTRAINT fk_ordering_version FOREIGN KEY (question_version_id) REFERENCES question_versions(id),
    CONSTRAINT uq_ordering_position UNIQUE (question_version_id, correct_position)
);
CREATE TABLE accepted_text_answers (
    id UUID PRIMARY KEY,
    question_version_id UUID NOT NULL,
    answer VARCHAR(1000) NOT NULL,
    CONSTRAINT fk_text_answer_version FOREIGN KEY (question_version_id) REFERENCES question_versions(id)
);

CREATE TABLE question_reports (
    id UUID PRIMARY KEY,
    question_version_id UUID NOT NULL,
    reporter_user_id UUID NOT NULL,
    comment VARCHAR(4000) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_report_question_version FOREIGN KEY (question_version_id) REFERENCES question_versions(id),
    CONSTRAINT fk_report_user FOREIGN KEY (reporter_user_id) REFERENCES app_user(id)
);
CREATE TABLE game_sessions (
    id UUID PRIMARY KEY,
    completion_key UUID NOT NULL UNIQUE,
    payload_hash VARCHAR(64) NOT NULL,
    game_id VARCHAR(255) NOT NULL,
    game_version_id UUID NOT NULL,
    host_user_id UUID,
    mode VARCHAR(40) NOT NULL,
    started_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    finished_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_session_game FOREIGN KEY (game_id) REFERENCES games(id),
    CONSTRAINT fk_session_game_version FOREIGN KEY (game_id, game_version_id) REFERENCES game_versions(game_id, id),
    CONSTRAINT fk_session_host FOREIGN KEY (host_user_id) REFERENCES app_user(id)
);
CREATE TABLE teams (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL,
    name VARCHAR(160) NOT NULL,
    score INTEGER NOT NULL,
    placement INTEGER,
    CONSTRAINT fk_team_session FOREIGN KEY (session_id) REFERENCES game_sessions(id),
    CONSTRAINT uq_team_session_id UNIQUE (session_id, id),
    CONSTRAINT uq_team_name UNIQUE (session_id, name)
);
CREATE TABLE session_participants (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL,
    user_id UUID NOT NULL,
    team_id UUID,
    score INTEGER NOT NULL,
    placement INTEGER,
    joined_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_participant_session FOREIGN KEY (session_id) REFERENCES game_sessions(id),
    CONSTRAINT fk_participant_user FOREIGN KEY (user_id) REFERENCES app_user(id),
    CONSTRAINT fk_participant_team FOREIGN KEY (session_id, team_id) REFERENCES teams(session_id, id),
    CONSTRAINT uq_participant_user UNIQUE (session_id, user_id)
);
CREATE TABLE player_answers (
    id UUID PRIMARY KEY,
    participant_id UUID NOT NULL,
    question_version_id UUID NOT NULL,
    answer_data VARCHAR(4000) NOT NULL,
    correct BOOLEAN NOT NULL,
    answered_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_answer_participant FOREIGN KEY (participant_id) REFERENCES session_participants(id),
    CONSTRAINT fk_answer_question_version FOREIGN KEY (question_version_id) REFERENCES question_versions(id),
    CONSTRAINT uq_answer_question UNIQUE (participant_id, question_version_id)
);
