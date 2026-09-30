# Datenmodell Version 1 – Mermaid-Entwurf

Stand: 30. September 2026. Fachlicher Entwurf, keine bereits implementierte
Datenbank. Die Tabellen zu Sessions, Teams, Teilnehmern und Antworten zeigen
ausschließlich dauerhaft gespeicherte, vollständig abgeschlossene Runden.
Lobby, laufende und abgebrochene Runden haben hier keinen Datensatz.

```mermaid
erDiagram

    users {
        uuid id PK
        varchar external_subject UK
        varchar username UK
        varchar display_name
        varchar role "STUDENT, TEACHER oder ADMIN"
        varchar current_class_code "nullable"
        boolean active
        timestamp created_at
        timestamp updated_at
    }

    subjects {
        uuid id PK
        varchar name UK
        varchar short_name "nullable"
        boolean active
    }

    game_families {
        uuid id PK
        timestamp created_at
    }

    games {
        uuid id PK
        uuid family_id FK
        uuid copied_from_game_id FK "nullable"
        uuid creator_user_id FK "nur fuer besitzerlose Altspiele nullable"
        varchar visibility "PRIVATE oder PUBLIC"
        uuid current_version_id FK
        timestamp deleted_at "nullable"
        timestamp created_at
        timestamp updated_at
    }

    game_versions {
        uuid id PK
        uuid game_id FK "UQ mit version_number"
        integer version_number "UQ mit game_id"
        varchar title
        text description
        varchar game_mode
        uuid subject_id FK
        timestamp created_at
    }

    questions {
        uuid id PK
        uuid game_id FK
        uuid copied_from_question_id FK "nullable"
        uuid current_version_id FK
        timestamp deleted_at "nullable"
        timestamp created_at
    }

    question_versions {
        uuid id PK
        uuid question_id FK "UQ mit version_number"
        integer version_number "UQ mit question_id"
        varchar type "TRUE_FALSE, ORDERING oder FREE_TEXT"
        text text
        timestamp created_at
    }

    game_version_questions {
        uuid game_version_id PK, FK
        uuid question_version_id PK, FK
        integer position "UQ mit game_version_id"
    }

    true_false_configs {
        uuid question_version_id PK, FK
        boolean correct_value
    }

    ordering_items {
        uuid id PK
        uuid question_version_id FK
        varchar text
        integer correct_position "UQ mit question_version_id"
    }

    accepted_text_answers {
        uuid id PK
        uuid question_version_id FK
        varchar answer
    }

    game_sessions {
        uuid id PK
        uuid game_id FK
        uuid game_version_id FK
        uuid host_user_id FK "aktives Solo-Spiel nullable"
        varchar mode "SINGLEPLAYER oder MULTIPLAYER"
        timestamp started_at
        timestamp finished_at
        timestamp created_at
    }

    teams {
        uuid id PK
        uuid session_id FK
        varchar name "UQ mit session_id"
        varchar color "nullable"
        integer score
        integer placement "nullable"
    }

    session_participants {
        uuid id PK
        uuid session_id FK "UQ mit user_id"
        uuid user_id FK "UQ mit session_id"
        uuid team_id FK "nullable"
        integer score
        integer placement "nullable"
        boolean winner
        timestamp joined_at
    }

    player_answers {
        uuid id PK
        uuid participant_id FK "UQ mit question_version_id"
        uuid question_version_id FK "UQ mit participant_id"
        text answer_data "JSON oder TEXT"
        boolean correct
        integer score
        bigint response_time_ms "nullable"
        timestamp answered_at
    }

    question_reports {
        uuid id PK
        uuid question_id FK
        uuid question_version_id FK
        uuid reporter_user_id FK
        uuid resolved_by_user_id FK "nullable"
        text comment
        varchar status "Workflow-Vorschlag"
        text resolution_comment "nullable"
        timestamp created_at
        timestamp resolved_at "nullable"
    }


    users |o--o{ games : erstellt

    subjects ||--o{ game_versions : kategorisiert

    game_families ||--|{ games : gruppiert

    games |o--o{ games : "kopiert von"

    games ||--|{ game_versions : versioniert

    games |o--|| game_versions : "aktuelle Version"


    games ||--o{ questions : enthaelt

    questions |o--o{ questions : "kopiert von"

    questions ||--|{ question_versions : versioniert

    questions |o--|| question_versions : "aktuelle Version"

    game_versions ||--|{ game_version_questions : ordnet

    question_versions ||--o{ game_version_questions : verwendet

    question_versions ||--o| true_false_configs : Wahr_Falsch

    question_versions ||--o{ ordering_items : Reihenfolge

    question_versions ||--o{ accepted_text_answers : Freitext


    games ||--o{ game_sessions : wird_gespielt

    game_versions ||--o{ game_sessions : Snapshot

    users |o--o{ game_sessions : hostet

    game_sessions ||--o{ teams : bildet

    game_sessions ||--|{ session_participants : hat

    users ||--o{ session_participants : spielt

    teams |o--o{ session_participants : gruppiert

    session_participants ||--o{ player_answers : beantwortet

    question_versions ||--o{ player_answers : bewertet


    questions ||--o{ question_reports : betrifft

    question_versions ||--o{ question_reports : gemeldete_Version

    users ||--o{ question_reports : meldet

    users |o--o{ question_reports : bearbeitet
```

Modellannahme: Beim aktiven Solo-Spiel ist `host_user_id` leer; die allein spielende
Person steht als Teilnehmer in `session_participants`. In einer gehosteten
Mehrspieler-Runde darf der Host nicht zugleich Teilnehmer sein. Nur aktive
Teilnehmer erhalten Fortschritt: Schüler, Lehrkräfte im Singleplayer und
mitspielende Administratoren. Alle Teilnehmer einer abgeschlossenen Runde
erhalten Punkte; Plätze 1 bis 3 deutlich mehr. Punkteformel und Bonus bei einem
Gleichstand an der Grenze zu Platz 3 sind offen. Ein Lobbycode gehört nur zum
flüchtigen Rundenzustand. Ob Lehrkräfte im Mehrspieler-Modus aktiv teilnehmen
dürfen, ist weiterhin offen. Ebenso offen ist, ob ein privates Spiel ohne
Mitspieler gehostet und als Runde abgeschlossen werden kann. Das gezeigte
Ergebnismodell setzt mindestens einen aktiven Teilnehmer voraus; der Host
würde auch in einer reinen Host-Runde nicht selbst mitspielen.

Neue Spiele sind standardmäßig privat. Vorhandene Spiele werden bei der
Migration öffentlich, auch wenn `creator_user_id` mangels belegbarem Ersteller
leer bleibt. Die feste Prüfung der drei Schul-Keycloak-IT-Benutzernamen
`it220269`, `it220240` und `it220265` für `ADMIN` ist geplant; das genaue
Token-Feld ist technisch noch zu verifizieren. Es gibt keine separate
Admin-Anmeldung oder Datenbankvergabe.

Die Sichtbarkeit von Meldungsdetails ist offen. Bestätigt ist nur, dass
Ersteller und Administratoren die Frage ändern dürfen. Status,
Bearbeitungsweg und Empfänger der Meldung sind Vorschläge. Beim Löschen einer
gemeldeten Frage wird auch ihre Meldung gelöscht; wie dabei historische
Antworten trotz `question_versions.question_id` erhalten bleiben, ist technisch
noch zu klären. `questions.deleted_at` ist dafür nur ein Modellvorschlag.
