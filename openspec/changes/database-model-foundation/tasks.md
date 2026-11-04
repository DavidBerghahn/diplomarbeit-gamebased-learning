# Tasks

Stand des Auftrags vom 3. November 2026: Die lokale Umsetzung des angepassten
Fachmodells ist freigegeben. Die noch offenen Aufgaben werden aufbauend auf dem
abgeschlossenen Flyway-Teilauftrag umgesetzt und erst nach konkretem Nachweis
abgehakt. Ein produktiver Rollout bleibt außerhalb dieses Auftrags.

## 0. Freigegebener Flyway-Teilauftrag vom 30. Oktober 2026

Dieser Teilauftrag integriert nur die Migrationsbasis. Die bisherigen 20 Aufgaben
des Fachmodells bleiben unverändert und dürfen dadurch nicht als erledigt gelten.
Eine produktive Datenbankübernahme oder ein Deployment ist nicht beauftragt.

- [x] 0.1 Flyway samt PostgreSQL-Unterstützung einbinden, das vorhandene Schema als initiale H2-/PostgreSQL-Migration abbilden und automatische Strukturänderungen durch Hibernate durch Validierung ersetzen; produktive Migration, automatisches Baseline, Clean und Repair standardmäßig deaktivieren.
- [x] 0.2 Leere Datenbank, wiederholte Migration ohne neue Änderungen, Bestandserhalt bei ausdrücklich gesetzter Baseline, Ablehnung einer unbekannten nichtleeren Datenbank und Prüfsummenfehler testen; bestehende Backendtests sowie echte lokale PostgreSQL-Prüfungen ausführen.
- [x] 0.3 Lokalen Start, Migrationskonvention, sicheren Prod-Opt-in und spätere Bestandsübernahme dokumentieren; Stand und tatsächlich ausgeführte Prüfungen in der vorhandenen Projektübergabe festhalten.

## 1. Migrationsbasis und Abstimmung

- [x] 1.1 Das tatsächliche Schema und die Bestandszahlen von `app_user`, `games`, `game_branches`, `questions` und `answer_options` auf einer nicht produktiven PostgreSQL-Kopie erfassen; IDs, Typwerte, Branch-Werte samt `branch_order`, Optionsreihenfolge, `subject`, `created_by_id` und eine eventuell vorhandene `visibility` dokumentieren und die Erhebung durch wiederholbare Abfragen prüfen.
- [x] 1.2 Mit dem Supervisor einen gemeinsamen, versionierten Migrationseigner für diesen und den offenen `game-visibility-and-ownership`-Change sowie ihre Reihenfolge festlegen; die Entscheidung und den Abgleich beider Migrationspläne dokumentieren und prüfen, dass vorhandene `PRIVATE`-Werte in keinem Backfill überschrieben werden.
- [x] 1.3 Eine Sicherung und Wiederherstellung der nicht produktiven Kopie sowie eine zur Ist-Struktur passende Migrationsbasis vorbereiten; Restore und erneuten Migrationslauf auf einer Wegwerf-PostgreSQL-Datenbank erfolgreich nachweisen.

## 2. Additives Schema und Datenübernahme

- [x] 2.1 Versionierte Migrationen für Spielfamilien, `games.family_id`, optionale Kopiervorlage, aktuelle Version und Löschmarkierung sowie für unveränderliche Spiel- und Frageversionen mit geordneten Fragen und Branches anlegen; Schemaaufbau und Fremdschlüssel ohne Cascade-Löschung referenzierter Vorlagen auf H2 und PostgreSQL prüfen, ohne bestehende IDs oder Spalten zu entfernen.
- [x] 2.2 Vorhandene Spiele, geordnete Branches, Fragen und Antwortoptionen in initiale Versionen überführen; Vor-/Nachzählungen und Inhaltsvergleiche für String-/Long-IDs, Spiel- und Fragetypen, Branch-Werte und `branch_order`, Optionswerte, Richtigkeitswerte und Reihenfolge auf einer befüllten Wegwerf-Datenbank prüfen; fehlende `created_by_id`-Werte leer lassen.
- [x] 2.3 Tags mit eindeutiger Identität und m:n-Zuordnung zu Spielversionen ergänzen und jeden nichtleeren `games.subject`-Wert unverändert übernehmen; Mehrfach-Tags, gemeinsame Tag-Identität und Erhalt von `subject`, Branches und Spieltyp durch Migrationstests prüfen.
- [x] 2.4 Datenstrukturen für bestehende geordnete Boolean-Antwortoptionen sowie Wahr/Falsch-, Reihenfolge- und Freitextlösungen ergänzen; Tests für Legacy-`single_choice` neben den neuen Typen und für unveränderte unbekannte Bestandswerte ausführen.
- [x] 2.5 Tabellen und Constraints für Fragenmeldungen, abgeschlossene Sessions mit eindeutigem `completion_key`, Teams, aktive Teilnehmer und Einzelantworten ergänzen; per Schema-/Repository-Tests `UNIQUE(session_id, user_id)`, Team-/Session- und Spiel-/Version-FKs prüfen sowie nachweisen, dass Meldungen nur `question_version_id` referenzieren und Antworten weder Score noch Antwortdauer speichern.

## 3. Versionierte Spielinhalte

- [x] 3.1 Spielanlage und -bearbeitung auf vollständige, transaktionale Spiel-Snapshots mit unveränderlichen geordneten Branches und Frageversionen umstellen; Tests für geänderte Branch-Werte/-Reihenfolge gegenüber dem erhaltenen alten Snapshot, neue Versionen, wiederverwendete unveränderte Fragen und Rollback bei Fehlern vor Wechsel von `current_version_id` ausführen.
- [x] 3.2 Die bisherigen REST- und WebSocket-Lesewege aus der aktuellen Version mit geordneten Branches, Fragen und Optionen bedienen; bestehende IDs und JSON-Felder einschließlich `zweige` anhand fokussierter Vertrags- und Integrationstests vergleichen, ohne Zugriffsregeln des anderen Changes vorwegzunehmen.
- [x] 3.3 Eine Spielkopie mit neuer Spiel-ID und unabhängigen Fragen innerhalb derselben Familie anlegen; Tests für getrennte Bearbeitung, unveränderten Spieltyp und nicht übernommene Fragenmeldungen ausführen.
- [x] 3.4 Historisch benötigte Versionen vor dem bisherigen `orphanRemoval` und dem Löschen eines Spiels schützen; bei vorhandener Kopie die Vorlage auch ohne abgeschlossene Session logisch löschen und Tests für erhaltene Familien-/Herkunftsreferenz, nutzbare Kopie sowie historische Referenz nach Bearbeitung oder Löschung ausführen; erforderliche Legacy-Joins bis zu einem gesondert geprüften Contract-Schritt erhalten.
- [x] 3.5 Fragenmeldungen an konkrete Versionen und die Löschung einer gemeldeten aktiven Frage implementieren; FK-/Löschtests auf H2 und PostgreSQL prüfen, dass Meldungen entfernt und historisch referenzierte Versionen, Optionen und Antworten erhalten bleiben.

## 4. Abgeschlossene Ergebnisse

- [x] 4.1 Einen internen Speicherdienst für vollständig finalisierte Einzel- und Teamrunden mit stabilem serverseitigem `completion_key`, Session, Teams, aktiven Teilnehmern und Antworten in einer Transaktion erstellen; Tests für vollständige Speicherung, Rollback bei Teilfehlern, identischen Retry mit demselben Ergebnis, abweichenden Retry mit Ablehnung sowie parallele Retries mit genau einem Ergebnis und ohne doppelten daraus abgeleiteten Fortschritt ausführen.
- [x] 4.2 Unvollständige, laufende und abgebrochene Runden an der Speichergrenze ablehnen; Tests prüfen, dass dafür keine dauerhaften Session-, Team-, Teilnehmer- oder Antwortzeilen entstehen und heutige flüchtige WebSocket-Räume nicht als historischer Bestand übernommen werden.
- [x] 4.3 Verifizierte Benutzerreferenzen, Host-/Teilnehmer-Trennung, Teamzugehörigkeit zur selben Session und Antwort-Frageversionen über `game_version_questions` aus dem Session-Snapshot innerhalb der Speichertransaktion validieren; Negativtests für fremdes Team/Session, Frageversion eines anderen Snapshots, Doppelteilnahme und Host als Teilnehmer sowie Positivtests für gehostete Runden und aktives Solo-Spiel ausführen.
- [x] 4.4 Finalisierte Teilnehmer- und Teamwerte samt Platzierung einschließlich Gleichstand speichern und Gewinner aus Platzierung ableiten; Tests für Plätze `1, 1, 3`, individuelle Endwerte und das Fehlen gespeicherter `winner`-, Antwort-Score- und Antwortdauer-Felder ausführen, ohne eine neue Punkteformel festzulegen.

## 5. Gesamtprüfung und Übergabe

- [x] 5.1 Migrations- und Repository-Tests auf leerer und vorbefüllter H2-Datenbank sowie auf echter Wegwerf-PostgreSQL-Datenbank ausführen; Wiederholung, Teilfehler, Bestandsvergleich einschließlich geordneter historischer Branches, Löschung einer Vorlage mit Kopie ohne Session, parallele Abschlussversuche und beide möglichen Reihenfolgen mit der `visibility`-Migration nachweisbar prüfen.
- [x] 5.2 `./mvnw test` sowie die für bestehende Angular-Verträge passenden `npm test -- --watch=false` und `npm run build` aus `Website/Frontend/` ausführen; Ergebnisse und nicht ausgeführte Prüfungen getrennt dokumentieren.
- [ ] 5.3 Für einen späteren Rollout Sicherung, Readiness-/Schema-Prüfung, Schreibzugriffs-Umschaltung, Vor-/Nachzählung, Abbruchkriterien und geprüften Restore schriftlich festhalten; auf einer Wegwerf-Umgebung prüfen, dass kein altes Backend nach neuen privaten Daten oder neuen Versionsschreibungen ungeschützt wieder gestartet wird.
