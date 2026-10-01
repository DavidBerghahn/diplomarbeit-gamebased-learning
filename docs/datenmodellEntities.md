| Entity | Aufgabe und Zusammenhang |
|---|---|
| `users` | Speichert die Benutzer und ihre Rolle. Ein Benutzer kann Spiele erstellen, Runden hosten, aktiv an Runden teilnehmen oder Fragen melden. Die aktuelle Schulklasse eines Schülers steht ebenfalls hier. |
| `subjects` | Speichert Schulfächer. Jede Spielversion ist genau einem Fach zugeordnet. |
| `game_families` | Fasst ein ursprüngliches Spiel und alle daraus kopierten Varianten zusammen. |
| `games` | Ist die dauerhafte Identität eines Spiels. Es gehört zu einer Spielfamilie, hat einen Ersteller und ist öffentlich oder privat. Es verweist auf seine aktuell gültige Version; eine Kopie kann zusätzlich auf ihr Ursprungsspiel verweisen. |
| `game_versions` | Speichert einen unveränderlichen Stand des Spiels, etwa Titel, Beschreibung, Modus und Fach. Ein Spiel kann mehrere Versionen haben; eine abgeschlossene Runde verweist auf die Version, mit der gespielt wurde. |
| `questions` | Ist die dauerhafte Identität einer Frage innerhalb eines Spiels. Sie verweist auf ihre aktuelle Version. Eine kopierte Frage wird danach eigenständig. |
| `question_versions` | Speichert einen bestimmten Stand von Fragetext und Fragetyp. Frühere Versionen helfen, abgeschlossene Runden später nachzuvollziehen. |
| `game_version_questions` | Verbindet Spielversionen mit den verwendeten Frageversionen und legt ihre Reihenfolge fest. |
| `true_false_configs` | Ergänzt eine Wahr/Falsch-Frageversion um die richtige Antwort. |
| `ordering_items` | Speichert die Elemente und ihre richtige Reihenfolge für eine Reihenfolge-Frageversion. |
| `accepted_text_answers` | Speichert die akzeptierten Lösungen einer Freitext-Frageversion. |
| `game_sessions` | Speichert eine **vollständig abgeschlossene** Spielrunde mit Spielversion, Spielart, Zeiten und gegebenenfalls Host. Abgebrochene Runden werden nicht dauerhaft gespeichert. |
| `teams` | Speichert Teams einer abgeschlossenen Runde mit Punkten und Platzierung. Teilnehmer können einem Team zugeordnet sein. |
| `session_participants` | Verbindet einen aktiv spielenden Benutzer mit einer abgeschlossenen Runde. Hier stehen seine endgültigen Punkte und seine Platzierung – daraus wird sein Fortschritt berechnet. Der Host ist kein Teilnehmer derselben Runde. |
| `player_answers` | Speichert, was ein Teilnehmer auf eine bestimmte Frageversion geantwortet hat und wie diese Antwort damals bewertet wurde. |
| `question_reports` | Speichert eine Meldung zu einer konkreten Frage und ihrer damaligen Version, einschließlich meldender Person und Kommentar. Wie der Bearbeitungsablauf genau aussieht, ist noch offen. |