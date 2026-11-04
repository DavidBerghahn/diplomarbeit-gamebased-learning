# Datenmodell Version 1 – Mermaid-Entwurf

Stand: 3. November 2026. Aktualisierter fachlicher Entwurf; eine fertige
Datenbank ist damit nicht nachgewiesen. Die Tabellen zu Sessions, Teams, Teilnehmern und Antworten zeigen
ausschließlich dauerhaft gespeicherte, vollständig abgeschlossene Runden.
Lobby, laufende und abgebrochene Runden haben hier keinen Datensatz.

```mermaid
erDiagram

    app_user {
        uuid id PK
        varchar external_subject UK
        varchar username
        varchar display_name
        varchar role "STUDENT, TEACHER oder ADMIN"
        varchar school_class "nullable"
        boolean active
        timestamp created_at
        timestamp updated_at
    }

    tags {
        uuid id PK
        varchar name UK
    }

    game_families {
        uuid id PK
        timestamp created_at
    }

    games {
        varchar id PK "bestehende String-ID"
        uuid family_id FK
        varchar copied_from_game_id FK "nullable"
        uuid created_by_id FK "bei besitzerlosen Altspielen nullable"
        varchar visibility "PRIVATE oder PUBLIC"
        uuid current_version_id FK
        varchar game_type "Spieltyp; Bestandswerte behalten"
        varchar subject "Legacy-Fach, nullable"
        timestamp deleted_at "nullable"
        timestamp created_at
        timestamp updated_at
    }

    game_versions {
        uuid id PK
        varchar game_id FK "UQ mit version_number"
        integer version_number "UQ mit game_id"
        varchar title
        text description
        varchar game_type
        timestamp created_at
    }

    game_version_tags {
        uuid game_version_id PK, FK
        uuid tag_id PK, FK
    }

    game_version_branches {
        uuid game_version_id PK, FK
        integer position PK
        varchar branch
    }

    questions {
        bigint id PK "bestehende Long-ID"
        varchar game_id FK
        uuid current_version_id FK
        timestamp created_at
    }

    question_versions {
        uuid id PK
        bigint question_id FK "nullable fuer historische Version"
        integer version_number "UQ mit question_id"
        varchar type "single_choice; TRUE_FALSE, ORDERING, FREE_TEXT"
        text text
        timestamp created_at
    }

    game_version_questions {
        uuid game_version_id PK, FK
        uuid question_version_id PK, FK
        integer position "UQ mit game_version_id"
    }

    answer_options {
        bigint id PK "bestehende Long-ID"
        uuid question_version_id FK
        bigint question_id "Legacy, nullable"
        varchar text
        boolean is_correct
        integer answer_order
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
        uuid completion_key UK "stabiler serverseitiger Abschluss"
        varchar game_id FK
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
        timestamp joined_at
    }

    player_answers {
        uuid id PK
        uuid participant_id FK "UQ mit question_version_id"
        uuid question_version_id FK "UQ mit participant_id"
        text answer_data "JSON oder TEXT"
        boolean correct
        timestamp answered_at
    }

    question_reports {
        uuid id PK
        uuid question_version_id FK
        uuid reporter_user_id FK
        text comment
        timestamp created_at
    }


    app_user |o--o{ games : erstellt

    tags ||--o{ game_version_tags : zugeordnet

    game_versions ||--o{ game_version_tags : traegt

    game_families ||--|{ games : gruppiert

    games |o--o{ games : "kopiert von"

    games ||--|{ game_versions : versioniert

    games |o--|| game_versions : "aktuelle Version"

    game_versions ||--o{ game_version_branches : Branches


    games ||--o{ questions : enthaelt

    questions o|--|{ question_versions : versioniert

    questions |o--|| question_versions : "aktuelle Version"

    game_versions ||--o{ game_version_questions : ordnet

    question_versions ||--o{ game_version_questions : verwendet

    question_versions ||--o{ answer_options : Optionen

    question_versions ||--o| true_false_configs : Wahr_Falsch

    question_versions ||--o{ ordering_items : Reihenfolge

    question_versions ||--o{ accepted_text_answers : Freitext


    games ||--o{ game_sessions : wird_gespielt

    game_versions ||--o{ game_sessions : Snapshot

    app_user |o--o{ game_sessions : hostet

    game_sessions ||--o{ teams : bildet

    game_sessions ||--|{ session_participants : hat

    app_user ||--o{ session_participants : spielt

    teams |o--o{ session_participants : gruppiert

    session_participants ||--o{ player_answers : beantwortet

    question_versions ||--o{ player_answers : bewertet


    question_versions ||--o{ question_reports : gemeldete_Version

    app_user ||--o{ question_reports : meldet
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

`completion_key` wird serverseitig stabil pro logischer Runde vergeben. Ein
identischer Abschluss-Retry liefert dasselbe Ergebnis, ein abweichender wird
abgewiesen. Finale Teilnehmer- und Teamwerte bleiben gespeichert; Siegerstatus
wird aus Einzel- beziehungsweise Teamplatzierung abgeleitet. Antwortpunkte,
Antwortdauer und eine separate Fortschrittstabelle entfallen.

Neue Spiele sind standardmäßig privat. Vorhandene Spiele werden bei der
Migration öffentlich, auch wenn `created_by_id` mangels belegbarem Ersteller
leer bleibt. Die feste Prüfung der drei Schul-Keycloak-IT-Benutzernamen
`it220269`, `it220240` und `it220265` für `ADMIN` ist geplant; das genaue
Token-Feld ist technisch noch zu verifizieren. Es gibt keine separate
Admin-Anmeldung oder Datenbankvergabe.

Die Sichtbarkeit von Meldungsdetails, Status, Bearbeitungsweg und Empfänger
bleiben offen. Ersteller und Administratoren dürfen die Frage ändern. Beim
Löschen einer gemeldeten Frage verschwinden die aktive Frage und die Meldung;
historische `question_versions` bleiben mit entkoppelbarem `question_id` für
damalige Antworten erhalten. Eine bereits kopierte Spielvorlage bleibt auch
ohne abgeschlossene Session logisch erhalten, damit ihre Herkunftsreferenz
gültig bleibt. Kopierte Fragen benötigen keine Herkunftskette.

| Entity | Aufgabe und Zusammenhang |
|---|---|
| `app_user` | Bestehende UUID-Benutzeridentität und Rolle; Schul-Keycloak bleibt Quelle der Anmeldung und der Rollenregeln. |
| `tags` | Frei benannte Tags, einschließlich übernommener Fachnamen; ein Tag ist weder Spieltyp noch Branch. |
| `game_families` | Fasst ein ursprüngliches Spiel und alle daraus kopierten Varianten zusammen. |
| `games` | Bestehende String-Spielidentität mit Familie, konkretem Spieltyp, Ersteller und aktueller Version. `copied_from_game_id` hält die direkte Vorlage; bei vorhandener Kopie bleibt sie nach Löschung logisch erhalten. |
| `game_versions` | Unveränderlicher Bearbeitungsstand eines Spiels. Eine abgeschlossene Runde verweist auf den gespielten Stand. |
| `game_version_tags` | Ordnet einer Spielversion mehrere Tags und einem Tag mehrere Spielversionen zu. |
| `game_version_branches` | Bewahrt Branch-Werte und ihre Reihenfolge je Spielversion. |
| `questions` | Bestehende Long-Frageidentität innerhalb eines Spiels. Eine Kopie erhält eine unabhängige Identität ohne Herkunfts-Selbstreferenz. |
| `question_versions` | Unveränderlicher Fragetext und Typ; bei Löschung der aktiven Frage bleiben benötigte historische Versionen entkoppelt erhalten. |
| `game_version_questions` | Verbindet Spielversionen mit den verwendeten Frageversionen und legt ihre Reihenfolge fest. |
| `answer_options` | Bestehende Long-Optionen mit Text, Boolean-Richtigkeit und Reihenfolge, einer Frageversion zugeordnet. |
| `true_false_configs` | Ergänzt eine Wahr/Falsch-Frageversion um die richtige Antwort. |
| `ordering_items` | Speichert die Elemente und ihre richtige Reihenfolge für eine Reihenfolge-Frageversion. |
| `accepted_text_answers` | Speichert die akzeptierten Lösungen einer Freitext-Frageversion. |
| `game_sessions` | Vollständig abgeschlossene Runde mit gespielter Version und stabilem `completion_key`; abgebrochene Runden bleiben flüchtig und Retries erzeugen kein zweites Ergebnis. |
| `teams` | Speichert Teams einer abgeschlossenen Runde mit Punkten und Platzierung. Teilnehmer können einem Team zugeordnet sein. |
| `session_participants` | Aktive Spieler mit finalen Punkten und Platzierung als Fortschrittsquelle. Der Host nimmt nicht teil; Siegerstatus ergibt sich aus Einzel- oder Teamplatzierung. |
| `player_answers` | Antwortinhalt, Boolean-Richtigkeit und Antwortzeitpunkt zu einer Frageversion; ohne eigenen Score oder Antwortdauer. |
| `question_reports` | Meldung mit Kommentar und meldender Person an genau einer `question_version_id`; Status und Detailsichtbarkeit bleiben offen. |
