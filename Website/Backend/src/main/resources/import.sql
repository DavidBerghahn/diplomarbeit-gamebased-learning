insert into games (id, game_type, teacher, name, description, subject, created_at, played_rounds) values ('quizbattle-001', 'Quizbattle', 'Martin Berger', 'Programmieren Grundlagen', 'Ein Quiz zu den Grundlagen der Programmierung und wichtigen Begriffen aus der Softwareentwicklung.', 'Programmieren', date '2026-02-12', 47);
insert into games (id, game_type, teacher, name, description, subject, created_at, played_rounds) values ('quizbattle-002', 'Quizbattle', 'Anna Wagner', 'Netzwerke & Cyber Security', 'Fragen zu Netzwerken, Protokollen, IT-Sicherheit und grundlegenden Sicherheitskonzepten.', 'Netzwerktechnik', date '2026-03-05', 31);
insert into games (id, game_type, teacher, name, description, subject, created_at, played_rounds) values ('quizbattle-003', 'Quizbattle', 'Thomas Leitner', 'Medientechnik Basics', 'Einsteigerquiz zu Bild, Ton, Video, Webentwicklung und digitalen Medien.', 'Medientechnik', date '2026-01-28', 26);
insert into games (id, game_type, teacher, name, description, subject, created_at, played_rounds) values ('quizbattle-004', 'Quizbattle', 'Julia Moser', 'Mathematik fuer Informatik', 'Mathematikquiz mit Schwerpunkt auf logischem Denken, Funktionen und mathematischen Grundlagen der Informatik.', 'Angewandte Mathematik', date '2026-02-21', 58);
insert into games (id, game_type, teacher, name, description, subject, created_at, played_rounds) values ('quizbattle-005', 'Quizbattle', 'Test Lehrer', 'Unser Solarsystem', 'Ein Testspiel mit Wahr-oder-Falsch-Aussagen ueber unser Sonnensystem.', 'Astronomie', date '2026-08-15', 0);
insert into games (id, game_type, teacher, name, description, subject, created_at, played_rounds) values ('duell-um-die-welt-001', 'DuellUmDieWelt', 'Michael Hofer', 'Technikreise um die Welt', 'Ein geografisches Duell mit Fragen zu technischen Innovationen, Unternehmen und Erfindungen aus aller Welt.', 'Geografie und Wirtschaft', date '2026-03-14', 19);
insert into games (id, game_type, teacher, name, description, subject, created_at, played_rounds) values ('duell-um-die-welt-002', 'DuellUmDieWelt', 'Sarah Gruber', 'IT Around the World', 'Eine Reise durch die Welt der Informatik mit Fragen zu Technologie, bekannten Firmen und wichtigen Entwicklungen.', 'Englisch', date '2026-04-02', 34);
insert into games (id, game_type, teacher, name, description, subject, created_at, played_rounds) values ('duell-um-die-welt-003', 'DuellUmDieWelt', 'Daniel Huber', 'Medizinische Entdeckungen', 'Eine Reise durch verschiedene Laender und Epochen der Medizin und Medizintechnik.', 'Biomedizinische Technik', date '2026-03-27', 22);
insert into games (id, game_type, teacher, name, description, subject, created_at, played_rounds) values ('duell-um-die-welt-004', 'DuellUmDieWelt', 'Lisa Steiner', 'Elektronik Around the World', 'Fragen zu Elektronik, technischen Erfindungen und bekannten Entwicklungen aus verschiedenen Laendern.', 'Elektronik', date '2026-04-10', 15);

insert into game_branches (game_id, branch_order, branch) values ('quizbattle-001', 0, 'SSE - School of Software Engineering');
insert into game_branches (game_id, branch_order, branch) values ('quizbattle-001', 1, 'DDP - Design of Digital Products');
insert into game_branches (game_id, branch_order, branch) values ('quizbattle-001', 2, 'SEC - Smart Cyber Systems');
insert into game_branches (game_id, branch_order, branch) values ('quizbattle-002', 0, 'SSE - School of Software Engineering');
insert into game_branches (game_id, branch_order, branch) values ('quizbattle-002', 1, 'SEC - Smart Cyber Systems');
insert into game_branches (game_id, branch_order, branch) values ('quizbattle-002', 2, 'ELT - Elektronik und technische Informatik');
insert into game_branches (game_id, branch_order, branch) values ('quizbattle-003', 0, 'MEDT - IT-Medientechnik');
insert into game_branches (game_id, branch_order, branch) values ('quizbattle-004', 0, 'SSE - School of Software Engineering');
insert into game_branches (game_id, branch_order, branch) values ('quizbattle-004', 1, 'CSI - Computer Science International');
insert into game_branches (game_id, branch_order, branch) values ('quizbattle-004', 2, 'DDP - Design of Digital Products');
insert into game_branches (game_id, branch_order, branch) values ('quizbattle-005', 0, 'SSE - School of Software Engineering');
insert into game_branches (game_id, branch_order, branch) values ('quizbattle-005', 1, 'DDP - Design of Digital Products');
insert into game_branches (game_id, branch_order, branch) values ('quizbattle-005', 2, 'SEC - Smart Cyber Systems');
insert into game_branches (game_id, branch_order, branch) values ('duell-um-die-welt-001', 0, 'SSE - School of Software Engineering');
insert into game_branches (game_id, branch_order, branch) values ('duell-um-die-welt-001', 1, 'ELT - Elektronik und technische Informatik');
insert into game_branches (game_id, branch_order, branch) values ('duell-um-die-welt-001', 2, 'MEDZ - Biomedizin- und Gesundheitstechnik');
insert into game_branches (game_id, branch_order, branch) values ('duell-um-die-welt-002', 0, 'CSI - Computer Science International');
insert into game_branches (game_id, branch_order, branch) values ('duell-um-die-welt-002', 1, 'SSE - School of Software Engineering');
insert into game_branches (game_id, branch_order, branch) values ('duell-um-die-welt-003', 0, 'MEDZ - Biomedizin- und Gesundheitstechnik');
insert into game_branches (game_id, branch_order, branch) values ('duell-um-die-welt-004', 0, 'ELT - Elektronik und technische Informatik');
insert into game_branches (game_id, branch_order, branch) values ('duell-um-die-welt-004', 1, 'FS - Fachschule Elektronik und technische Informatik');

insert into questions (id, game_id, question_order, type, text) values (1, 'quizbattle-005', 0, 'wahr_oder_falsch', 'Welche Aussagen ueber das Sonnensystem sind wahr?');

insert into answer_options (id, question_id, answer_order, text, is_correct) values (1, 1, 0, 'Die Sonne ist ein Stern.', true);
insert into answer_options (id, question_id, answer_order, text, is_correct) values (2, 1, 1, 'Die Erde ist der dritte Planet von der Sonne aus.', true);
insert into answer_options (id, question_id, answer_order, text, is_correct) values (3, 1, 2, 'Jupiter ist der groesste Planet unseres Sonnensystems.', true);
insert into answer_options (id, question_id, answer_order, text, is_correct) values (4, 1, 3, 'Der Mond ist ein Planet.', false);
insert into answer_options (id, question_id, answer_order, text, is_correct) values (5, 1, 4, 'Mars wird oft als roter Planet bezeichnet.', true);
insert into answer_options (id, question_id, answer_order, text, is_correct) values (6, 1, 5, 'Saturn ist der Sonne am naechsten.', false);
insert into answer_options (id, question_id, answer_order, text, is_correct) values (7, 1, 6, 'Neptun ist weiter von der Sonne entfernt als Uranus.', true);
insert into answer_options (id, question_id, answer_order, text, is_correct) values (8, 1, 7, 'Venus hat eine dichtere Atmosphaere als die Erde.', true);
insert into answer_options (id, question_id, answer_order, text, is_correct) values (9, 1, 8, 'Merkur ist der groesste Planet unseres Sonnensystems.', false);
insert into answer_options (id, question_id, answer_order, text, is_correct) values (10, 1, 9, 'Zwischen Mars und Jupiter befindet sich der Asteroidenguertel.', true);
insert into answer_options (id, question_id, answer_order, text, is_correct) values (11, 1, 10, 'Pluto gilt heute als Zwergplanet.', true);
insert into answer_options (id, question_id, answer_order, text, is_correct) values (12, 1, 11, 'Alle Planeten unseres Sonnensystems haben Ringe.', false);

alter table questions alter column id restart with 2;
alter table answer_options alter column id restart with 13;
