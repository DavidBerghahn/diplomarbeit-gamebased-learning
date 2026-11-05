# Tasks

## 1. Additive V3-Migration

- [ ] 1.1 `smart10_cards` mit UUID-Karten-ID, Frageversionsbezug, Position, Text und typisierten Lösungsfeldern in H2 und PostgreSQL ab V3 anlegen; Schema-Tests prüfen UNIQUE, FK, zeilenbezogene Lösungs-CHECKs und den `solution_rank`-Bereich 1–12 (Rang 0/13 abgewiesen), während V1/V2-Dateien unverändert bleiben.
- [ ] 1.2 `player_answers` um nullable `smart10_card_id` und nicht-null `answer_slot_key` erweitern, V2-Bestand mit `question` backfillen und die alte UNIQUE durch Slot-UNIQUE, CHECK und zusammengesetzten Karten-/Frageversions-FK ersetzen; Migrationstest prüft alte IDs, Antworten, Richtigkeiten und Zeitpunkte sowie zwei verschiedene Karten derselben Frage.
- [ ] 1.3 V3 auf leeren und mit generischen Antworten befüllten V2-Datenbanken in H2 und echter Wegwerf-PostgreSQL-Datenbank ausführen; Tests prüfen Wiederholung, Fremdkarte, Doppelkarte, unveränderte Nicht-Smart10-Ein-Antwort-Regel und Vor-/Nachzählungen.

## 2. Gespeicherte Smart10-Inhalte

- [ ] 2.1 Den versionierten Smart10-Adapter für genau zwölf geordnete Karten und die Modi `TRUE_FALSE`, `FREE_TEXT`, `ORDERING` bauen; Schreib-/Lesetests prüfen passende Boolean-, Text- und Ranglösungen, bei `ORDERING` exakt die Permutation 1–12 sowie abgewiesene Fehlzahl, falschen Lösungstyp, Rang 0/13, doppelte und fehlende Ränge.
- [ ] 2.2 Beim Bearbeiten und Kopieren neue Frageversionen und unabhängige Kartenidentitäten erzeugen und historisch genutzte Karten bei Löschung erhalten; Integrationstests prüfen alte Antworten gegen den alten Snapshot nach Änderung, Kopie und Löschung.
- [ ] 2.3 Einen Smart10-Startdienst aus Spiel-ID und aktueller gespeicherter Version erstellen, der fehlende oder unvollständige Karten ausdrücklich ablehnt und keine Legacy-Optionen stillschweigend umdeutet; Tests prüfen zwölf Karten, Typ, bei `ORDERING` die vollständige Permutation 1–12 mit Ablehnung von Rang 0/13, Duplikaten und Lücken sowie unveränderte Version nach parallel erfolgter Bearbeitung.
- [ ] 2.4 Die lokale Demo eindeutig von gespeicherten Smart10-Versionen trennen und produktive Demo-Saat ausschließen; Test/Build prüfen Demo-Kennzeichnung, keinen Store-Aufruf für Demo und keine automatisch angelegten Produktiv-Demospiele.

## 3. Identität und Transport

- [ ] 3.1 Ein kurzlebiges, opakes, einmaliges WebSocket-Ticket nur per authentifiziertem HTTP über `AuthProvider`/`UserService` für den aktuellen `AppUser` ausstellen; Tests prüfen fehlenden Login, Ablauf, atomaren Verbrauch und dass weder URL noch Log einen Bearer-Token enthalten.
- [ ] 3.2 Ticket mit der ersten WebSocket-Nachricht an die Verbindung binden und persistierende `mp_`-Aktionen vor Bindung, anonym, nach Ablauf oder nach Replay ablehnen; Tests prüfen unveränderten Raumzustand und dass `connection.id`/Anzeigename keine Benutzeridentität liefern.
- [x] 3.3 Im Angular-Multiplayer-Transport vor Verbindungsaufbau bei Bedarf den bestehenden Login erneuern, ein Ticket abrufen und es als erste Nachricht einlösen; Frontend-Tests prüfen den neuen Ablauf ohne Bearer-Token in der WebSocket-URL.
- [ ] 3.4 Neue persistente Smart10-Start-/Speicherwege serverseitig hinter einem in Prod standardmäßig ausgeschalteten Flag halten; Konfigurationstest prüft Prod-Sperre und explizite Dev-/Test-Aktivierung ohne Freigabe privater Inhalte.

## 4. Solo-Runden

- [ ] 4.1 Authentifizierte Solo-Start-/Antwort-/Finish-Aufrufe mit flüchtiger Runde, gepinntem Snapshot und serverseitigem `completion_key` ergänzen; Tests prüfen verifizierte AppUser-ID, Versionstreue und Ablehnung von Clientspiel, Clientlösung und Clientscore.
- [ ] 4.2 Alle drei Kartenmodi serverseitig bewerten und `correctCards`, `completedQuestions`, `bestStreak` aus echten Antworten führen; Tests prüfen die bestehende Formel `correctCards + completedQuestions * 10 + bestStreak * 2`, Duplikate und manipulierte Antworten.
- [ ] 4.3 Nur den regulären Solo-Abschluss mit Kartenantworten und berechnetem Endwert über den Store speichern; Tests prüfen Abbruch ohne Daten, Fehler mit unverändertem finalem Payload/Schlüssel und identischen Retry mit derselben Session-ID.
- [x] 4.4 Angular-Solo auf den gespeicherten Serverablauf umstellen und die lokale Demo als ungespeichert kenntlich halten; Komponententests prüfen Fortschritt/Feedback, fehlenden Demo-Store-Aufruf und „gespeichert“ erst nach bestätigter Session-ID.

## 5. Mehrspieler-Runden

- [ ] 5.1 `mp_create` für persistente Runden aus gespeicherter Spiel-ID und gepinnter Version statt aus dem übermittelten Spielobjekt starten; WebSocket-Tests prüfen abweichende Clientlösungen/Clientscores und fehlende Freigabe von Lösungen vor Kartenauflösung.
- [ ] 5.2 Host standardmäßig beobachten lassen und per Toggle für alle Rollen einschließlich Lehrer mitspielen lassen; reale Teilnehmer über `app_user.id` deduplizieren, leere Teams ablehnen und ausschließlich aktive Mitglieder speichern. Tests prüfen beobachtenden Host ohne Punkte, mitspielenden Lehrer-Host, Doppelverbindung und besetzte Teams.
- [ ] 5.3 Bestehende Zugregeln beibehalten: +1 bei richtig, null plus dauerhafte Elimination bei falsch oder regulärem Zugtimeout, kein Punkte- oder Eliminationsreset beim Fragenwechsel und reguläres Ende bei vollständig eliminierten Teams; Zustands- und Regressionstests prüfen alle Übergänge.
- [ ] 5.4 Kartenantworten für alle drei Modi anhand des serverseitigen Snapshots bewerten und mit konkreter Karten-ID erfassen; Tests prüfen Fremd-/Doppelkarte, Antwortdaten und Boolean-Richtigkeit ohne akzeptierten Clientscore.
- [ ] 5.5 Beim regulären Ende Teams nach Endwert mit Competition Ranking platzieren und jedem Mitglied exakt den Teamwert einschließlich null zuordnen; Tests prüfen `1, 1, 3`, Nullteams, keine Boni und keine künstlichen Mindestpunkte.
- [ ] 5.6 Transportabbruch vor regulärem Ende und Neustart als Rundenabbruch ohne Store-Aufruf behandeln, Zugtimeout dagegen als Spielregel; Tests prüfen beide Fälle und unveränderte Nullpunkt-Endwerte nach regulärem Abschluss.

## 6. Atomare Speicherung und Rückmeldung

- [ ] 6.1 Nullpunkte und Host-Mitspielen ausschließlich für intern verifizierte Smart10-/Just-One-Runden erlauben; Negativtests prüfen generische Nullwerte, negative Werte, fremde Versionen und frei behauptete Modi.

- [ ] 6.2 Karten-ID und abgeleiteten `answer_slot_key` in Store-Validierung und kanonischem `payload_hash` berücksichtigen; Tests prüfen zwei Karten derselben Frage, falschen Slot, fremde Karte, kartenlose Smart10-Antwort, Kartenantwort zu anderem Spieltyp und abweichenden Retry mit anderer Karte.
- [ ] 6.3 Finale Mehrspieler-Payloads bei regulärem Ende mit stabilem Schlüssel atomar speichern und bei Fehlern für denselben Retry in der laufenden Instanz halten; Tests prüfen Teilfehler-Rollback, parallele/identische/abweichende Retries, genau eine Session und einmalig abgeleiteten Fortschritt.
- [ ] 6.4 Frontend und WebSocket-Antwort zwischen Spielende und bestätigtem Datenbank-Commit unterscheiden; Tests prüfen, dass eine Session-ID nur nach erfolgreicher Speicherung erscheint und Speicherfehler keine Erfolgsmeldung erzeugt.

## 7. Gesamtprüfung und Freischaltgrenze

- [ ] 7.1 Backend-Integration für alle drei Modi, zwölf Karten, Versionwechsel, Host-/Identitätsgrenzen, Nullwerte, Timeout/Abbruch und Store-Retry auf H2 und echter Wegwerf-PostgreSQL-Datenbank mit `./mvnw test` prüfen; Ergebnisse und nicht ausgeführte Fälle dokumentieren.
- [x] 7.2 `npm test -- --watch=false` und `npm run build` in `Website/Frontend/` ausführen; falls der bekannte Import `JustOne` statt `JustOneComponent` in `just-one.spec.ts` den Testlauf blockiert, ausschließlich diesen Import als getrennt sichtbare Testkorrektur beheben und den Test erneut ausführen, ohne Just-One-Funktion zu ändern.
- [ ] 7.3 Vor einer gesonderten produktiven Freischaltung den abgeschlossenen Access-Change, Migrations-/Restore-Nachweis, Prod-Flag, Ticket-/Instanzrouting und serverseitige Zugriffstests als prüfbare Freigabebedingungen dokumentieren; eine Konfigurations- und Integrationsprüfung weist nach, dass der persistente Smart10-Pfad bis dahin in Prod geschlossen bleibt.

## 8. Bestätigte Ergänzungen des Teammitglieds

- [x] 8.1 Einstellbare Smart10-Zeit, atomare Codevergabe, Ablauf nach 24 Stunden und serverseitigen Takt implementieren; Timeout-, Ablauf- und parallele Erstellungstests ausführen.
- [x] 8.2 Gleichverteilung, freie Teamwahl, Host-Umverteilung, Ungleichverteilungsbestätigung, Zufallsleiter und Abschlussanzeige implementieren; Teamtests und Frontendbuild prüfen.
- [x] 8.3 Just-One-Multiplayer mit personalisierten Nachrichten, Zahlenwahl, Hinweisen, Bewertung und idempotenter nachträglicher Punktekorrektur implementieren; Rollen-/Leakage-/Speichertests ausführen.
- [x] 8.4 CSV-Vorlagen, Quote-/Zeilenparser, Vorschau, serverseitige Inhaltsprüfung und dauerhafte Erstellung beider Spiele anbinden; CSV- und API-Vertragstests ausführen.
- [x] 8.5 Lehrerstatistik aller Klassen und Schüler mit Spielhäufigkeiten, Punkten und Mitspielern implementieren; 403 für Schüler und fehlende Antwortinhalte testen.
- [x] 8.6 Lokale dauerhafte Datenbank, PostgreSQL-Profil, DB-Healthcheck und manuelle LeoCloud-Anleitung bereitstellen; H2-/PostgreSQL-Prüfung und Frontendtests/Build dokumentieren. Kein Cloud-Rollout.
- [x] 8.7 Klassen aus der Schulidentität dauerhaft zuordnen, fehlende Klassen durch Lehrer/Admins ergänzen und Klassenfilter mit Schülerpunkten und Klassensummen inklusive Schülern mit Adminrechten implementieren; Persistenz-, Rechte-, Summen- und Datumsfiltertests sowie Frontendbuild prüfen (Folgeauftrag 5. November 2026).
- [x] 8.8 Die zwei ausdrücklich freigegebenen gespeicherten Programmier-Beispiele getrennt von alten Entwicklungs-Demos für den Produktionsstart bereitstellen; frische Anlage, wiederholten Start ohne Überschreiben und gespeicherten Spielabschluss testen. Rollout bleibt beim Teampartner; Commit und GitHub-Push sind beauftragt.
