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
| `smart10_cards` | V3 ergänzt für jede Smart10-Gruppenfrage einzelne Karten mit eigener UUID, fester Position, Text und einer zum Fragemodus passenden Boolean-, Text- oder Ranglösung. Die Karten gehören zu einer unveränderlichen Frageversion; für `ORDERING` müssen genau zwölf Karten die Lösungsränge 1–12 jeweils einmal enthalten. |
| `true_false_configs` | Ergänzt eine Wahr/Falsch-Frageversion um die richtige Antwort. |
| `ordering_items` | Speichert die Elemente und ihre richtige Reihenfolge für eine Reihenfolge-Frageversion. |
| `accepted_text_answers` | Speichert die akzeptierten Lösungen einer Freitext-Frageversion. |
| `game_sessions` | Vollständig abgeschlossene Runde mit gespielter Version und stabilem `completion_key`; abgebrochene Runden bleiben flüchtig und Retries erzeugen kein zweites Ergebnis. |
| `teams` | Speichert Teams einer abgeschlossenen Runde mit Punkten und Platzierung. Teilnehmer können einem Team zugeordnet sein. |
| `session_participants` | Aktive Spieler mit finalen Punkten und Platzierung als Fortschrittsquelle. Der Host nimmt nicht teil; Siegerstatus ergibt sich aus Einzel- oder Teamplatzierung. |
| `player_answers` | Antwortinhalt, Boolean-Richtigkeit und Antwortzeitpunkt zu einer Frageversion; ohne eigenen Score oder Antwortdauer. V3 ergänzt eine optionale Referenz auf die konkret gespielte Smart10-Karte und einen Antwort-Slot. Bisherige und andere Spieltypen bleiben beim Slot `question` mit höchstens einer Antwort pro Frage; Smart10 kann mehrere verschiedene Karten derselben Frage beantworten. |
| `question_reports` | Meldung mit Kommentar und meldender Person an genau einer `question_version_id`; Status und Detailsichtbarkeit bleiben offen. |

Die V3-Schemaänderung liegt vor. Die Smart10-Anbindung ist noch in Umsetzung; Tests, unabhängiges Review und eine produktive Freigabe sind dadurch nicht belegt.
