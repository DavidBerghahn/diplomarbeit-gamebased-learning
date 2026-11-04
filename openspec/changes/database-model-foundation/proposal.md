# Proposal

## Why

Das Backend speichert derzeit Spiele, Fragen und Antwortoptionen in einem veränderlichen Schema. Der fachlich geprüfte V1-Entwurf verlangt dagegen unveränderliche Spiel- und Frageversionen, Tags, unabhängige Spielkopien, Fragenmeldungen und historische Ergebnisse vollständig abgeschlossener Runden. Vor weiteren Spiel- und Statistikfunktionen braucht es dafür eine migrationssichere Persistenzgrundlage, die vorhandene Spiele und API-Daten erhält.

## What Changes

- Die vorhandenen `app_user`-, `games`-, `questions`- und `answer_options`-Datensätze und ihre IDs werden in ein anschlussfähiges V1-Modell überführt. Spielkopien bleiben über eine Spielfamilie verbunden; auch eine später gelöschte Vorlage bleibt für ihre Kopien als Herkunft erhalten. Bearbeitungsstände desselben Spiels erhalten unveränderliche Versionen einschließlich geordneter Branches. Spielfamilien sind keine Spieltypen.
- Fächer werden durch frei benannte Tags mit einer m:n-Zuordnung zu versionierten Spielinhalten ersetzt. Vorhandene `games.subject`-Werte werden ohne Verlust in Tags überführt; die bisherigen Spieltypen und Fragen samt Antwortoptionen bleiben lesbar, bis ihre fachliche Abbildung geklärt ist.
- Für unabhängige Fragenkopien, gemeldete Frageversionen sowie abgeschlossene Einzel- und Mehrspieler-Ergebnisse werden persistente Beziehungen ergänzt. Unfertige und abgebrochene Runden erhalten keine dauerhaften Session-, Teilnehmer- oder Antwortdatensätze. Wiederholte Speicherung derselben abgeschlossenen Runde erzeugt kein zweites Ergebnis und keinen doppelten Fortschritt. Finale Teilnehmer- und Teamwerte bleiben erhalten, ohne redundante Antwortpunkte, Antwortdauer oder gespeichertes Gewinnerflag.
- **BREAKING (interne Persistenz):** Der bisherige Austausch der Fragenliste mit `orphanRemoval` darf verwendete Inhalte nicht mehr zerstören. Aktualisierungen müssen neue unveränderliche Snapshots erzeugen. Bestehende REST-/WebSocket-Verträge und IDs dürfen bei der Umstellung nicht unbemerkt brechen.
- Eine versionierte PostgreSQL-Migration mit Bestandssicherung, Vor-/Nachprüfung und Rückfallplan wird vorgesehen. David hat Flyway am 30. Oktober 2026 und die lokale Umsetzung des angepassten Fachmodells am 3. November 2026 freigegeben. Die bereits geprüfte Migrationsbasis wird dafür um neue Migrationen erweitert; eine produktive Migration oder ein Deployment ist weiterhin nicht beauftragt.

## Capabilities

### New Capabilities

- `database-model-foundation`: Dauerhafte, kompatible V1-Struktur für Spielefamilien, Spiel-/Frageversionen, Tags, Fragenmeldungen sowie vollständig abgeschlossene Sessions mit Teams, Teilnehmern und Antworten.

### Modified Capabilities

Keine; im Hauptspec-Verzeichnis bestehen derzeit keine Capabilities. Der parallele Change `game-visibility-and-ownership` ist geplant und bleibt unverändert.

## Impact

Betroffen wären Quarkus/Panache-Entities, Repositories, Schema-Migration und Tests für H2 und PostgreSQL sowie die Persistenzwege hinter der bestehenden Spiele-API. Die derzeitigen String-Spiel-IDs und Long-Frage-/Antwortoption-IDs bleiben stabil. Der Change schafft Datenschema und anschließbare Schreib-/Lesewege; er baut keine neue Spieleengine, Lobby-, Frontend-, Leaderboard- oder KI-Funktion. Sichtbarkeit, Eigentümerschaft und ihre Migration bleiben Gegenstand von `game-visibility-and-ownership`; dessen 13 Aufgaben sind derzeit offen. Migrationen beider Changes müssen vor einer Umsetzung in einer festgelegten Reihenfolge zusammengeführt werden.
