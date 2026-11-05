# Projektübergabe: Diplomarbeit Game-based Learning

Stand: 5. November 2026

## Klassenstatistik am 5. November

Ausgangspunkt dieses Folgeauftrags ist main b22af9a mit den unten beschriebenen
uncommitteten Startkorrekturen. Die Klassenansicht unter /statistics gruppiert
alle gespeicherten Schüler nach aktueller Klasse, zeigt ihre Punkte und deren
Summe pro Klasse und berücksichtigt den Datumsfilter auch bei den Summen.
Schüler ohne Spiele erscheinen mit null Punkten. Gemeinsame Sessions zählen
bei der Anzahl der Klassenspiele einmal; Teamwerte zählen je Schüler.

Nach ausdrücklicher Klarstellung des Nutzers zählen auch die Projektmitglieder
mit Rolle ADMIN als Schüler in ihrer Klasse. Die Statistik berücksichtigt daher
STUDENT und ADMIN, während TEACHER aus der Schülerwertung ausgeschlossen bleibt.
Fehlende Klassenzuordnungen lassen sich ebenfalls für diese Admin-Konten ergänzen.
Dabei bleiben Rollen und Berechtigungen unverändert.

Klassen aus dem Schulkonto werden normalisiert gespeichert. Ein fehlender Claim
löscht keine vorhandene Klasse mehr. Ohne bekannte Zuordnung erscheint ein Schüler
unter „Ohne Klasse“; Lehrer/Admins können diese über die Oberfläche ergänzen.
Der neue PUT-Endpunkt /api/users/{id}/school-class prüft aktive Lehrer-/Adminrechte
und erlaubt ausschließlich die Klasse eines Schülers. Eine spätere nichtleere
Klasse aus Keycloak hat Vorrang. Klassenwechsel beziehen sich auf die aktuelle
Gruppierung inklusive bisheriger Schülerpunkte, nicht auf historische Klassen.
Keine neue Migration und kein Datenreset. Der Nutzer hat anschließend ausdrücklich
Commit und GitHub-Push des gesamten lokalen Stands beauftragt; LeoCloud bleibt
ausgenommen. Der GitHub-Workflow baut/veröffentlicht ein Image, führt aber keinen
Kubernetes-Rollout aus.

Prüfung vor dem beauftragten Push: 74 Frontendtests und Produktionsbuild erfolgreich;
66 Backendtests insgesamt nach der Admin-Korrektur, davon 59 erfolgreich ausgeführt und sieben
PostgreSQL-Migrationstests mangels Testinstanz übersprungen. Lokales Backend mit
Datenbank meldet UP, /statistics liefert HTTP 200; Statistik- und Zuordnungs-API
weisen Aufrufe ohne Login mit 401 ab. OpenSpec-Validierung und git diff --check
erfolgreich. Kein neuer Nachweis eines echten Schul-Logins oder Cloudbetriebs.

## Lokaler Startfehler am 5. November

Der alte Entwicklungsdatensatz quizbattle-001 enthält nur zwei Single-Choice-Antworten
und ist kein vollständiges gespeichertes Smart10-Spiel. Er bleibt unverändert erhalten,
wird aber über die additive API-Angabe spielbar=false/startHinweis nicht mehr als startbereit
angeboten. Dev/Test ergänzen idempotent vollständige gespeicherte Programmierbeispiele
sample-smart10-programming (zwölf Karten) und sample-just-one-programming (fünf Begriffe).
Prod legt standardmäßig keine Beispiele an. Die Oberfläche zeigt API-Fehlermeldungen
ohne JSON-Hülle; Solo/Lobby verhindern den Start unvollständiger Inhalte.
Kein Datenbankreset und keine Änderung vorhandener Migrationen.

## Neuester Einstieg: Classroom-Todos angebunden

Die Umsetzung dieses Teammitglieds baut auf main 9301ae3 auf. Der Nutzer hat
ausdrücklich alle Classroom-Todos zur lokalen Umsetzung sowie Commit/GitHub-Push
freigegeben und nach Rückfrage die Arbeit ausschließlich im Supervisor-Chat gewählt.
Es wurden keine Spezialisten-Chats beauftragt. Kein LeoCloud-Rollout und keine
Änderung an der Cloud-Datenbank sind beauftragt oder durchgeführt.

Implementiert sind der ticketgebundene Angular-Multiplayertransport, gespeicherte
Smart10-Soloabläufe mit Commitbestätigung/Retry, Spielerstellung/Kopie/Status/Meine
Spiele mit Besitzschutz, Smart10-Zeitlimit 5–120 Sekunden, serverseitige Timeouts,
atomare Codes und 24-Stunden-Ablauf, Host-Mitspielen einschließlich Lehrer,
gleichmäßige Zufallszuordnung, freie Teamwahl/Host-Umverteilung/Bestätigung,
zufällige Teamleiter und Abschlussanzeige. Just One läuft über denselben Raumdienst
mit getrennten Nachrichten, Nummernwahl ohne Zielwort für den Ratenden, Hinweisen
und Hostbewertung. Korrekturen gespeicherter Versuche nehmen Punkte genau einmal
zurück und sind über den gehosteten Verlauf auch nach Raumende erreichbar.
CSV-Vorlagen und Import für beide Spiele sowie Lehrerstatistik aller Klassen,
Schüler, Häufigkeiten, Punkte und Mitspieler sind angeschlossen.

V1–V3 bleiben unverändert; V4 ergänzt Sichtbarkeit, Host-Mitspielen und
Just-One-Versuche. Dev-H2 speichert lokal unter dem ignorierten .local-data/-Pfad.
Optionales local-pg-Profil und DB-Healthcheck sind ergänzt. Das vorbereitete
Kubernetes-Manifest verwendet weiterhin eine Replica, jetzt mit Recreate.
Die GitHub-CI prüft H2, PostgreSQL und das Frontend. Prod-Migration und
SMART10_PERSISTENCE_ENABLED bleiben standardmäßig aus und müssen beim späteren
manuellen Rollout bewusst freigeschaltet werden.

Die Abnahmematrix und die lokale/Cloud-Anleitung stehen in
[docs/classroom-todos-implementation.md](docs/classroom-todos-implementation.md).
Öffentliche Kataloge und WebSocket-Lesewege geben keine privaten Spiele frei;
private Schüler-Einladungen bleiben ausgeschlossen. Der ursprüngliche Access-
und Foundation-Rollout-Change ist damit nicht automatisch vollständig abgenommen
oder archiviert. Nicht nachgewiesene ältere Smart10-Detailabnahmen bleiben in
der Aufgabenliste offen; das ist kein Hinweis, dass die neue Todo-Umsetzung
unimplementiert sei.

Aktuelle Prüfungen: 65 Frontendtests erfolgreich und Produktionsbuild erfolgreich.
Die vollständigen Backendläufe wurden getrennt mit H2 und PostgreSQL ausgeführt:
jeweils 58 Tests, keine Fehler und keine übersprungenen Tests. Der Backend-Paketbau
mit ausgelassenen Tests war ebenfalls erfolgreich. Ein echter
WebSocket-Integrationstest prüft Ticket, Lobby-Broadcast und Datenbankabschluss.
Ein lokaler pg_dump/pg_restore-Test erhielt 92 Spiele, 40 Sessions, 55 Teilnehmer
und vier Just-One-Versuche unverändert in einer zweiten lokalen Datenbank.
Keine Prüfung eines Cloud-Abzugs und kein echter Schul-Login im Browser.
Die bekannten H2/Flyway-Versionswarnungen wurden nicht durch einen Versionssprung
überdeckt. Details der vorherigen Entwicklungsstufen folgen als historische Angaben.

Die isolierte PostgreSQL-Testinstanz wurde danach beendet. Ihre Dateien liegen
noch unter dem lokalen TEMP-Unterordner diplomarbeit-pg-d7bce349, da die automatische
Sicherheitsprüfung das rekursive Aufräumen blockierte. Kein Testport ist ein dauerhafter
Projektzugang; für weitere Tests eine neue lokale Testinstanz starten.

## Team-Einstieg: veröffentlichter Arbeitsstand vom 4. November

**Zielbranch: `main`. Dies ist ein unvollständiger Entwicklungsstand, keine fertige Smart10-Anbindung und keine Rollout-Freigabe.** David hat ausdrücklich beauftragt, den zunächst unter `codex/smart10-db-integration-wip` veröffentlichten Commit `eb3c4d5` auf `main` zu übernehmen, damit das Team ihn direkt abrufen kann. Ausgangspunkt war `106209b`. Enthalten sind die Datenmodell-/Flyway-Foundation, der geprüfte Smart10-Plan sowie die bis zum Nutzungslimit entstandene Backend-Teilimplementierung. Der Push auf `main` löst den vorhandenen Build-/Image-Workflow aus; ein erfolgreicher Workflow oder LeoCloud-Rollout ist damit nicht nachgewiesen. Die nachfolgenden Abschnitte dokumentieren frühere Meilensteine; diese Team-Zusammenfassung ist der aktuelle Einstieg.

### Einstieg für den Codex-Chat eines Teamkollegen

1. Eigenen Arbeitsstand sichern und prüfen, dann `git fetch origin` und den aktuellen Stand von `origin/main` in einem geeigneten Checkout übernehmen. Bei einem unveränderten lokalen `main` genügt `git pull --ff-only origin main`. Keine eigenen Änderungen überschreiben. `git status --short --branch` und `git log -1` prüfen.
2. `AGENTS.md`, diese Übergabe, `docs/datenmodell-entscheidungen.md` und alle Artefakte in `openspec/changes/smart10-session-persistence/` lesen. Die persönlichen Rollen-Chats und lokalen Pfade von David sind auf anderen Rechnern nicht automatisch verfügbar; siehe `docs/codex-team-setup.md`.
3. Erst den tatsächlichen Zwischenstand abgleichen, dann den freigegebenen Smart10-Change fortsetzen. Keine produktiven Daten verwenden, keine V1/V2-Migrationsdateien umschreiben. Ein Push ist keine Freigabe zum Merge oder LeoCloud-Rollout.

### Enthalten und noch offen

- **Foundation:** Flyway V1/V2 für H2/PostgreSQL, versionierte Inhalte, Tags und atomarer interner Ergebnisspeicher. Dieser frühere Stand wurde mit 42 Tests je Datenbank geprüft und unabhängig nachgebessert; diese Zahlen gelten nicht automatisch für den jetzigen Smart10-Zwischenstand. `database-model-foundation` bleibt bei 22/23 Aufgaben; die Rollout-/Rollback-Aufgabe 5.3 ist offen.
- **Smart10-Backend begonnen:** additive `V3__smart10_cards_and_answer_slots.sql`, typisierte zwölf Karten, Snapshot-Adapter, Kartenbezug im Ergebnisspeicher, `Smart10SoloService`/`Smart10SoloResource`, `Smart10Tickets`/`Smart10TicketResource` und Änderungen an `GameSocket`/`MultiplayerGameService`. Vor Fortsetzung besonders Autorisierung, Protokollkompatibilität, Abbruch, Nullpunkte, Abschluss und Retry gegen den Plan prüfen. Für den neuen Code liegt noch kein unabhängiges abschließendes Review vor.
- **WebSockets beachten:** Katalog-/Spieldaten laufen über `GameWebSocketService`, Multiplayer über `MultiplayerWebSocketService`, beide auf `/user-socket`. Das Backend hat bereits eine Ticketbindung ergänzt; die Angular-Dienste sind in diesem Zwischenstand noch nicht angepasst. Der bisherige Client und das neue Backend sind deshalb noch nicht als zusammen funktionierender Spielablauf bestätigt.
- **Frontend noch offen:** Solo-/Lobby-/Socket-Anbindung, Speicherbestätigung und Retry-Oberfläche, ausdrückliche ungespeicherte Demo sowie die tatsächliche Speicherung aus `CreateGame.submit()`. Die vorhandene Erstelloberfläche bereitet bislang nur lokal vor. Keine neue vollständige Fragenbibliothek nebenbei implementieren.
- **Beschlüsse:** Host spielt nicht mit und erhält keinen Fortschritt. Nur regulär abgeschlossene Runden speichern. Jedes Smart10-Teammitglied erhält den tatsächlichen Team-Endwert einschließlich 0; kein erfundener Top-3-Bonus. Fehler/Zugtimeout setzt ein Team auf 0 und eliminiert es für die restliche Runde; Fragenwechsel belebt es nicht wieder. Transportabbruch ist vom regulären Zugtimeout zu unterscheiden. Solo-Formel bleibt `correctCards + completedQuestions * 10 + bestStreak * 2`, künftig serverseitig.
- **Freischaltung:** `gamebased.smart10.persistence.enabled` ist für Dev/Test eingeschaltet und für Prod standardmäßig aus (`SMART10_PERSISTENCE_ENABLED=false`). `game-visibility-and-ownership` steht weiterhin bei 0/13; private Einladungen und aktive Lehrer-Teilnahme im Multiplayer sind nicht fachlich freigegeben. Der neue persistente Pfad soll Lehrer-Teilnahme bis zum Beschluss serverseitig ablehnen, Lehrer-Solo und Hosting zulassen.

### Prüfstand und nächster Auftrag

Der Supervisor hat den aktuellen Backend-Paketbau mit `./mvnw package -DskipTests` erfolgreich ausgeführt; das führt keine Tests aus. `git diff --check` war unauffällig. Der Coder meldete gezielte erfolgreiche Foundation- und Smart10-Inhaltstests; ein vollständiger H2-/PostgreSQL-/Frontend-Nachweis für diesen Zwischenstand fehlt. Der bekannte Frontend-Testfehler `just-one.spec.ts` importiert weiterhin `JustOne` statt `JustOneComponent`. Die 28 Smart10-Aufgaben sind noch nicht als erledigt markiert. Coder und zuletzt auch Writer wurden vom Nutzungslimit gestoppt; die zweite Diagramm-/Modelldokumentationsrunde wurde nicht ausgeführt.

**Nächster konkreter Auftrag:** Backend-Teilimplementierung prüfen, vorhandene Angular-WebSocket-/Solo-/Erstellenwege anschließen, fehlende Migration-/Engine-/Identitäts-/Retrytests ergänzen, dann vollständige Tests auf H2 und einer neu angelegten lokalen Wegwerf-PostgreSQL-Datenbank sowie Frontend-Tests und Build ausführen. Danach einen eindeutig festgelegten Stand unabhängig reviewen und Aufgaben nur anhand des Nachweises abhaken. Modelldokument und Diagramme um `smart10_cards` ergänzen; Entity-Kurzfassung und Flyway-Anleitung sind bereits ergänzt. Kein lokaler Testcontainer-Port ist ein dauerhafter Projektzugang. Kein LeoCloud-Rollout wurde durchgeführt.

## Aktueller Auftrag: Smart10-Anbindung in Umsetzung

- David bestätigte am 4. November für Smart10-Mehrspieler: Jedes aktive Teammitglied erhält den tatsächlichen finalen Teamwert, einschließlich null; ein zusätzlicher Top-3-Bonus folgt erst nach einer späteren Entscheidung. Der datierte Nachtrag in [docs/datenmodell-entscheidungen.md](docs/datenmodell-entscheidungen.md) hält diese Präzisierung fest.
- Der sichtbare Writer-Chat hat den Folgechange [`smart10-session-persistence`](openspec/changes/smart10-session-persistence/proposal.md) mit Proposal, Design, Spezifikation und 28 offenen Aufgaben erstellt. Die Planung umfasst zwölf versionierte Karten je Gruppenfrage, serverseitige Bewertung und Identität, Host-/Teilnehmer-Trennung sowie atomare Speicherung regulärer Solo- und Mehrspieler-Abschlüsse. Eliminierte Teams bleiben bis zum Rundenende ausgeschieden; ein normaler Zugtimeout ist vom Transportabbruch zu unterscheiden.
- David hat die Umsetzung am 4. November ausdrücklich beauftragt und auf die bestehenden WebSocket-Ladewege hingewiesen. Der sichtbare Coder-Chat arbeitet im Hauptcheckout an V3, Kartenadapter, Ergebnisspeicher und Frontend-/Transportanbindung. Der aktuelle Zwischenstand ist noch kein vollständig geprüftes Smart10-Ergebnis. V1/V2 bleiben unverändert; kein Commit, Push oder LeoCloud-Rollout ist beauftragt. Der Access-Change und die Foundation-Rollout-Aufgabe bleiben getrennte Voraussetzungen für die produktive Freischaltung. Private Schüler-Einladungen und aktive Lehrer-Teilnahme im Multiplayer bleiben offen.
- Der sichtbare Reviewer-Chat hat den Plan unabhängig geprüft. Nach einer Korrekturrunde sind beide Befunde behoben: `ORDERING` verlangt die vollständige Permutation 1–12 bei Schreib- und Startvalidierung; aktive `TEACHER`-Teilnahme wird auf dem neuen persistenten Mehrspielerpfad bis zum fachlichen Beschluss serverseitig abgewiesen, während Lehrer-Solo und Hosting möglich bleiben. Die Nachprüfung meldet keinen Restbefund im Umfang. Writer- und Reviewer-Delegation haben tatsächlich funktioniert. `openspec validate smart10-session-persistence --strict` und `git diff --check` waren erfolgreich; dies ist kein Nachweis einer Smart10-Implementierung oder bestandener Anwendungstests.
- Für die Abnahme: Smart10-Change vollständig gegen Code und Tests prüfen und einen festgelegten Stand unabhängig vom bestehenden Reviewer-Chat prüfen lassen. Der Writer ergänzt ausschließlich `docs/flyway.md` und `docs/datenmodellEntities.md`; der Supervisor pflegt diese Übergabe. Im Hauptcheckout arbeiten; alte Rollen-Worktrees enthalten die uncommittete Foundation nicht automatisch. Die aktuelle lokale Wegwerf-PostgreSQL-Instanz und ihr Port sind keine dauerhaften Projektzugänge und werden nach der Prüfung entfernt.

## Aktueller Einstieg: Datenmodell-Foundation lokal umgesetzt

- David hat die lokale Umsetzung des angepassten Fachmodells am 3. November freigegeben. Im Hauptcheckout auf `main`, weiterhin ausgehend von `106209b`, liegen die Änderungen uncommittet vor. Die frühere Flyway-Grundlage ist um V2-Migrationen, versionierte Inhalte und einen internen Ergebnis-Speicherdienst erweitert. Es gab keinen Push, kein Deployment und keine produktive Bestandsübernahme.
- Die sichtbaren Chats „Diplomarbeit · Coder“ und „Diplomarbeit · Writer“ haben den ersten Implementierungs- und Dokumentationsstand geliefert. Der Reviewer hat die fünf Modelldokumente nach einer Kardinalitätskorrektur ohne Restbefund im geprüften Umfang bestätigt. Ein visuelles PlantUML-Rendering ist nicht nachgewiesen.
- Der erste Backendstand bestand am 3. November 39 Tests je Datenbankkonfiguration. Nach Korrekturrunde 1 haben Coder und Supervisor den aktuellen Stand am 4. November erneut vollständig geprüft: **42 Tests mit H2 und 42 mit PostgreSQL, jeweils ohne Fehler und ohne übersprungene Tests**. Die Quarkus-Tests verwenden PostgreSQL beziehungsweise H2 als tatsächliche Backend-Datenbank; zusätzliche Migrationstests prüfen jeweils beide Dialekte.
- Die drei Code-Review-Befunde sind korrigiert und unabhängig nachgeprüft: stabile Frage-IDs auch im REST-Rundlauf, Erhalt zusätzlicher Tags und interner Lösungsdaten beim normalen PUT sowie ausschließlich `SINGLEPLAYER`/`MULTIPLAYER` mit geprüfter Einzelspielerform. Die Frage-ID ist eine additive JSON-Ergänzung; eindeutige ältere Anfragen ohne IDs bleiben unterstützt, mehrdeutige oder fremde Zuordnungen werden abgewiesen. Der Reviewer meldet keinen Restbefund in den geprüften Korrekturpfaden. Die sichtbare Delegation an Coder, Writer und Reviewer hat tatsächlich stattgefunden.
- Der Supervisor hat auch den aktuellen Backend-Paketbau mit `./mvnw package -DskipTests` erfolgreich geprüft. Die lokale Testinstanz `codex-foundation-test-20261104` wurde nach den Prüfungen beendet und automatisch entfernt; Port 49484 ist kein dauerhaft verfügbarer Projektzugang. Für eine Fortsetzung mit PostgreSQL eine neue Wegwerf-Instanz verwenden.
- Unabhängige Migrationsprüfung am 3. November: künstlicher V1-Bestand mit einem Benutzer, zwei Spielen (davon eines ohne Ersteller), zwei Branches, einer Frage und zwei Antwortoptionen. `pg_dump`/`pg_restore`, ausdrückliche Baseline 1 und Migration der finalen PostgreSQL-V2 auf einer lokalen Wegwerf-Datenbank waren erfolgreich; ein zweiter Migrationslauf führte keine Änderung aus. Der Fingerprint aller ursprünglichen Datenwerte blieb `40457f6db7a0f25a14725c8aab35784e`. Danach bestanden zwei Familien, zwei Spielversionen, eine Frageversion, ein gemeinsamer Tag und zwei Tag-Zuordnungen; es wurden keine Sessions erfunden. Dies ist **keine** Prüfung eines LeoCloud-Datenbankabzugs.
- Die PostgreSQL-V2 dieser Prüfung hat SHA-256 `35c46a325b838dfb95cbc852f07de83df9c5141d4a13fd26a07bf1bf4972ef4f`. V1 bleibt unverändert. Flyway ist der gemeinsame Migrationseigner: Foundation zuerst, die spätere Access-Migration im selben Versionsraum. Die Tests mit Sichtbarkeits-Fixtures prüfen beide Reihenfolgen und den Erhalt vorhandener `PRIVATE`-Werte, implementieren aber den Access-Change nicht.
- Der unveränderte Frontend-Testbefehl scheitert an `just-one.spec.ts:3`: Import `JustOne`, obwohl die Komponente `JustOneComponent` exportiert. `npm run build` war erfolgreich. Die DB-Änderung hat diesen Testfehler nicht eingeführt; Frontend-Dateien wurden nicht geändert.
- Weiter außerhalb des Auftrags: LeoCloud-Rollout, produktive Baseline, neue Spieleengine-/Frontend-Anbindung, Punkteformel, Leaderboards und KI. Die neuen Ergebnisdaten werden nur über einen internen, vertrauenswürdigen Speicherweg angenommen; bestehende flüchtige Spiele sind noch nicht automatisch daran angeschlossen. Den Testcontainer und Port bei Fortsetzung neu prüfen, nicht aus diesem Text als laufend voraussetzen.

### Prüfbericht `database-model-foundation` vom 4. November

| Dimension | Ergebnis |
|---|---|
| Vollständigkeit | 22 von 23 Aufgaben erledigt; 5.3 bleibt offen |
| Korrektheit | 10 Foundation-Anforderungen gegen Code und Tests abgeglichen; aktuelle Backendtests mit beiden DB-Konfigurationen erfolgreich |
| Kohärenz | Modelldokumente und gezielte Codekorrekturen unabhängig geprüft; ergänzte REST-Regeln im Design und in der Spezifikation festgehalten |

- **Vor Archivierung offen:** Aufgabe 5.3 verlangt die vollständige spätere Rollout-/Rollback-Anweisung einschließlich Sicherheitsprobe für einen alten Backendstand. Backup/Restore des künstlichen Altbestands ist geprüft; daraus folgt noch keine sichere Rollout- oder Rückfallfreigabe für LeoCloud. Den Change deshalb nicht als vollständig abgeschlossen archivieren. Vor produktiver Übernahme sind außerdem das tatsächliche Ziel-Schema, ein aktuelles Backup und der separate Zugriffsschutz abzugleichen.
- **Bekannte Warnung:** Der unveränderte Angular-Testlauf scheitert an `just-one.spec.ts:3`, während der Frontend-Build besteht. Aufgabe 5.2 ist als tatsächlich ausgeführte und dokumentierte Prüfung abgehakt, **nicht** als vollständig grüner Frontend-Testnachweis.
- **Bekannte Versionswarnung:** Flyway meldet die H2-Version als neuer als seine getestete Unterstützung. Die geprüften Tests bestehen; es erfolgte kein eigenmächtiger Versionssprung außerhalb der Quarkus-BOM.

## Rückblick auf die Flyway-Grundlage vom 1. November 2026

- Im Hauptcheckout auf `main`, ausgehend von `106209b`, ist Flyway lokal integriert. Die Änderungen sind noch nicht committet, gepusht oder auf LeoCloud ausgerollt. Vor Fortsetzung den tatsächlichen Git-Stand prüfen; die nachfolgenden September-Abschnitte sind historisch.
- Die neuen H2- und PostgreSQL-Migrationen `V1__current_legacy_schema.sql` bilden nur die fünf bestehenden Tabellen ab. Die Migrationsnummer `V1` bedeutet **nicht**, dass der fachliche V1-Entwurf mit Versionen, Tags, Meldungen und Ergebnissen bereits implementiert ist. Im Change `database-model-foundation` sind dafür die ursprünglichen 20 Aufgaben weiterhin offen; der gesonderte Flyway-Teilauftrag ist umgesetzt.
- Lokal und im Test legt Flyway ein leeres Schema an; Hibernate verwendet `validate` statt `update`. Automatisches Baseline, Clean und Repair sind deaktiviert. Auf Prod bleibt die automatische Migration ohne ausdrücklichen Opt-in `FLYWAY_PROD_MIGRATE=true` ausgeschaltet. Eine unbekannte nichtleere Datenbank wird bei einem Migrationsversuch nicht automatisch übernommen.
- Der Supervisor hat `./mvnw test -Dflyway.test.pg-url=jdbc:postgresql://127.0.0.1:53721/flyway_test` gegen eine ausschließlich lokale Wegwerf-PostgreSQL-Instanz wiederholt: **26 Tests, keine Fehler, keine übersprungenen Tests**. Darin sind je vier H2-/PostgreSQL-Migrationstests für Anlage/Wiederholung/Datenerhalt, unbekannte Schemas, ausdrückliche Baseline und Prüfsummenfehler enthalten. Der Port gehört nur zu diesem temporären Testlauf. Der Coder hat zusätzlich einen Quarkus-Starttest mit PostgreSQL und Hibernate-Validierung erfolgreich ausgeführt.
- Nach dem unabhängigen Review wurde die Baseline-Testlücke korrigiert: Hibernate erzeugt den repräsentativen Altbestand direkt aus den bestehenden Entities, ohne vorher Flyway-V1 auszuführen. Der Supervisor hat den vollständigen Lauf am 1. November erneut mit allen 26 Tests erfolgreich wiederholt. Der Backend-Build `./mvnw package -DskipTests` war ebenfalls erfolgreich; dieses Kommando selbst führt keine Tests aus.
- Die sichtbaren Chats „Diplomarbeit · Coder“ und „Diplomarbeit · Reviewer“ haben Umsetzung und Nachprüfung abgeschlossen. Der Reviewer bestätigt die Korrektur ohne Restbefund in diesem Umfang. Der ausschließlich für diese Prüfung angelegte PostgreSQL-Container wurde anschließend beendet und automatisch entfernt; für erneute PostgreSQL-Tests ist eine neue lokale Testinstanz nötig.
- Es bleibt eine Versionswarnung der BOM-Abhängigkeiten: Flyway meldet H2 2.3.230 als neuer als seine getestete H2-Unterstützung. Die genannten Tests bestehen; daraus wird keine generelle Kompatibilitätsgarantie abgeleitet. Abhängigkeiten wurden nicht außerhalb der Quarkus-BOM aktualisiert.
- Start, neue SQL-Migrationen und der sichere spätere Bestandsübergang sind in [docs/flyway.md](docs/flyway.md) beschrieben. Vor einer produktiven Baseline sind ein getestetes Backup und der Abgleich des tatsächlichen PostgreSQL-Schemas nötig. Produktive Daten und Berechtigungen wurden nicht geprüft oder verändert. Ein echter Lehrerlogin, Frontendtests und ein Deployment sind durch diesen Backend-Teilauftrag nicht nachgewiesen.
- Nächster Schritt für das Fachmodell: die noch offenen Aufgaben des DB-Changes in abgegrenzten Schritten umsetzen. Die Sichtbarkeits-/Eigentümerregeln bleiben im separaten offenen Change; ein späterer Rollout muss dessen Migration und Zugriffsschutz berücksichtigen.

## Historischer Einstieg vom 23. September 2026

Das Projekt ist eine KI-gestützte Spieleplattform für Game-based Learning an der HTL Leonding. Davids Schwerpunkt umfasst Backend, Schul-Keycloak-Login, Benutzer- und Rollenverwaltung, PostgreSQL, APIs sowie das Deployment auf LeoCloud. Die Teamkollegen arbeiten vor allem an Spielen und KI-Inhalten.

Der nächste sinnvolle Einstieg ist:

1. Mit dem Supervisor und passenden Spezialisten das [Entscheidungsprotokoll der drei Datenmodell-Fragerunden](docs/datenmodell-entscheidungen.md) gegen den [V1-Entwurf](docs/datenmodell-v1.md) und bestehende OpenSpec-Changes abgleichen. Die dort markierte Frage zum Singleplayer für Lehrkräfte nicht stillschweigend entscheiden. Dies ist zunächst eine Planungs- und Prüfaufgabe, kein Umsetzungsauftrag.
2. Den geplanten OpenSpec-Change [`game-visibility-and-ownership`](openspec/changes/game-visibility-and-ownership/proposal.md) gemeinsam durchsehen. Er regelt private/öffentliche Spiele und Eigentümerrechte, ist aber **noch nicht implementiert**. Erst nach einem neuen Umsetzungsauftrag mit `$openspec-apply-change` beginnen.
3. Vor einer späteren Produktivschaltung die Datenmigration und den Rollback-Schutz aus dem [Design](openspec/changes/game-visibility-and-ownership/design.md) prüfen. Ein alter Backend-Stand würde private Spiele offenlegen.
4. Den echten Lehrerlogin mit einem Lehreraccount end-to-end prüfen. Danach weitere Datenmodellteile wie Kopien, Sessions, Ergebnisse und Fortschritt in getrennten Changes angehen.

## Fortsetzung am 23. September 2026

- Die Rollen- und Autorisierungsänderungen sowie der fachliche [Datenmodell-Entwurf Version 1](docs/datenmodell-v1.md) sind mit `c9f3859` committet. Der Entwurf ist noch nicht als vollständiges Persistenzmodell umgesetzt.
- Die drei ursprünglichen Fragerunden mit Davids Antworten und späteren Korrekturen sind als [Entscheidungsprotokoll zum Datenmodell](docs/datenmodell-entscheidungen.md) festgehalten. Vor einer neuen Modell- oder OpenSpec-Bearbeitung dieses Protokoll mit dem V1-Entwurf abgleichen. Die Frage, ob Lehrkräfte öffentliche Spiele auch alleine spielen dürfen, bleibt wegen zweier Aussagen ausdrücklich offen.
- OpenSpec und die Projekt-Skills wurden mit `17e3d12` eingerichtet. Proposal, Spezifikation, Design und Aufgabenliste des ersten Changes wurden mit `173b655` committet; `openspec validate game-visibility-and-ownership --strict` war erfolgreich. Anwendungscode wurde dafür nicht geändert.
- Fünf Codex-Worktrees wurden mit dem lokalen `main` synchronisiert und waren danach sauber; `openspec doctor` war dort erfolgreich. Die vorherigen, unversionierten OpenSpec-Kopien wurden zur Sicherheit in fünf Git-Stashes abgelegt. Den HEAD der Worktrees bei Fortsetzung erneut prüfen.
- Für Supervisor, Researcher, Coder, Writer und Reviewer wurden zuvor separate Codex-Aufgaben angestoßen, aber am 23. September war in der Aufgabenliste nur der ursprüngliche Projektchat nachweisbar. Die Worktrees allein belegen keine einsatzbereiten Chats. Keine doppelte Anlage ohne Statusklärung.
- Der erste Supervisor-Test zu `game-visibility-and-ownership` nutzte einen internen Unteragenten für den lesenden Codeabgleich, nicht einen bestehenden, separat sichtbaren Researcher-Chat. Der Befund war nachvollziehbar, testete aber nicht den gewünschten Chat-übergreifenden Ablauf. Die Projektregel in `AGENTS.md` verlangt für größere delegierte Arbeiten deshalb ausdrücklich die Beauftragung und Rückmeldung aus der vorhandenen Rollen-Aufgabe. Ob alle Rollen-Aufgaben aktuell erreichbar sind, muss vor dem nächsten Test geprüft werden.
- Die Zusammenarbeit ist nun in `AGENTS.md` als sichtbare Supervisor-zu-Rollen-Chat-Kette mit klarer Datei-Zuständigkeit, Rückmeldung, Integration und unabhängigem Review beschrieben. Der Supervisor trägt außerdem die Verantwortung, OpenSpec-Artefakte und Aufgabenstand passend zu Anforderungen, Code und Tests halten zu lassen. Die Regel ist noch kein Nachweis, dass die vier Spezialisten-Aufgaben tatsächlich erreichbar sind.
- Der lokale Branch `main` stand vor dieser Dokumentationsaktualisierung auf `173b655`, `origin/main` auf `c9f3859` (zwei lokale Commits voraus). Diese Commits wurden nicht gepusht. Der Git-Stand muss beim nächsten Einstieg erneut geprüft werden.
- LeoCloud, Lehrerlogin, Maven-/Angular-Tests und Deployment wurden am 23. September **nicht** erneut geprüft. Frühere Nachweise unten gelten nur für ihr jeweiliges Datum.

## Rückblick: Fortsetzung am 22. September 2026

- Ein echter Login wurde live erneut durchgeführt. Das verwendete Konto wurde von `GET /api/auth/me` korrekt als `STUDENT` erkannt; ein echter Lehreraccount wurde dabei nicht verwendet. Der Nachweis für `role: TEACHER` bleibt daher offen.
- Die Frontend-Routen unterscheiden jetzt zwischen allgemeinen angemeldeten Benutzern und `TEACHER`/`ADMIN`. `Meine Spiele`, `Spiel erstellen` und die Host-Lobby sind für Schüler weder als Aktion sichtbar noch direkt per URL erreichbar.
- Der OIDC-PKCE-Flow bewahrt ein geprüftes internes Rücksprungziel. Externe oder unbekannte Ziele werden auf `/home` zurückgesetzt.
- Inaktive Konten werden bei schreibenden Spieleaktionen und allen Endpunkten der Benutzerverwaltung auch serverseitig mit `403` abgewiesen.
- Die Änderungen waren zu diesem Zeitpunkt lokal implementiert und geprüft, aber noch nicht committet, gepusht oder deployt. Der spätere Commit-Stand steht oben.

## Verifizierter Projektzugang

- Lokaler Pfad: `/Users/davidberghahn/Documents/diplomarbeit-gamebased-learning`
- Repository: `https://github.com/DavidBerghahn/diplomarbeit-gamebased-learning`
- Branch: `main`
- Zuletzt vor dieser Dokumentationsaktualisierung verifiziert: lokal `173b655`, `origin/main` `c9f3859`. Lokale Commits sind nicht automatisch veröffentlicht; der Git-Stand kann sich danach ändern.
- LeoCloud-Namespace: `student-it220269`
- Live-Adresse: `https://it220269.cloud.htl-leonding.ac.at/`
- Container-Image: `ghcr.io/davidberghahn/diplomarbeit-gamebased-learning:latest`

## Am 22. September 2026 verifizierter Live-Zustand

Am 22. September 2026 wurden folgende Punkte read-only geprüft:

- `GET /api/health` antwortet mit `status: UP`.
- Das API-Deployment hat `1/1` verfügbare Replica.
- Der PostgreSQL-Pod ist `1/1 Running`.
- Service und Ingress sind vorhanden; der Ingress zeigt auf `it220269.cloud.htl-leonding.ac.at`.

Das belegt die Erreichbarkeit und den Kubernetes-Zustand nur an diesem Datum. Am 23. September wurde kein neuer Live-Check durchgeführt. Ein vollständiger End-to-End-Login mit einem Lehreraccount bleibt offen.

## Architektur

### Backend

- Java 21 und Quarkus 3.15.3
- REST-Endpunkte mit Quarkus REST/Jackson
- Hibernate ORM mit Panache
- H2 im lokalen Dev-/Testbetrieb
- PostgreSQL im `prod`-Profil auf LeoCloud
- OIDC-/JWT-Prüfung über den Schul-Keycloak
- WebSocket-Endpunkt `/user-socket` für die Spieleintegration

Relevante Bereiche:

- Authentifizierung: [`src/main/java/at/htlleonding/gamebasedlearning/auth`](src/main/java/at/htlleonding/gamebasedlearning/auth)
- Benutzerverwaltung: [`src/main/java/at/htlleonding/gamebasedlearning/users`](src/main/java/at/htlleonding/gamebasedlearning/users)
- Spiele-API: [`src/main/java/at/htlleonding/gamebasedlearning/games`](src/main/java/at/htlleonding/gamebasedlearning/games)
- Konfiguration: [`src/main/resources/application.properties`](src/main/resources/application.properties)

### Frontend

- Angular 22 unter [`Website/Frontend`](Website/Frontend)
- Das Frontend wird im Docker-Build gebaut und anschließend vom Quarkus-Backend als statische Web-App ausgeliefert.
- Enthalten sind Start/Login, Home, Spieleübersicht, eigene Spiele, Spiel erstellen, Lobby und ein Quizbattle-Entwurf.
- `AuthService` führt den OIDC Authorization Code Flow mit PKCE aus und speichert die Tokens nur im Browser-`sessionStorage`.
- Auf `/home` können über `Login-Details` das Backendprofil und die dekodierten Claims angezeigt werden. Der rohe Token wird nicht angezeigt.

Relevante Dateien:

- [`Website/Frontend/src/app/auth.service.ts`](Website/Frontend/src/app/auth.service.ts)
- [`Website/Frontend/src/app/app.routes.ts`](Website/Frontend/src/app/app.routes.ts)
- [`Website/Frontend/src/app/home`](Website/Frontend/src/app/home)
- [`Website/Frontend/src/app/game-websocket.service.ts`](Website/Frontend/src/app/game-websocket.service.ts)

### Login und Rollen

Die Anwendung speichert keine Schulpasswörter. Keycloak authentifiziert den Benutzer; das Backend validiert den Bearer-Token und legt anhand der Keycloak-Subject-ID einen eigenen App-User in PostgreSQL an oder aktualisiert ihn.

Bestätigte Rollenlogik:

- Schüler-Token enthalten im `distinguishedName` einen LDAP-Pfad mit `OU=Students` und üblicherweise der Klasse.
- Ein realer Lehrer-Token enthielt `OU=Teachers,OU=HTL,...`.
- Seit Commit `e37c583` wertet `KeycloakAuthProvider` den LDAP-DN strukturiert aus und ordnet `OU=Teachers` der Rolle `TEACHER` zu.
- Ein bestehender App-User wird bei einem erneuten Aufruf von `/api/auth/me` aus der aktuellen Identität aktualisiert.
- Zusätzlich werden Rollen aus `custom_roles`, `groups`, `realm_access.roles` und clientbezogenen Resource-Rollen berücksichtigt.
- Intern gesetzte Rollen `TEACHER` und `ADMIN` werden durch einen späteren Standard-Studentenclaim nicht ungewollt herabgestuft.

Es wurden keine Token, Passwörter oder personenbezogenen Lehrerdaten in diese Übergabe übernommen.

## Lokaler Start

Backend:

```bash
cd /Users/davidberghahn/Documents/diplomarbeit-gamebased-learning
./mvnw quarkus:dev
```

Frontend in einem zweiten Terminal:

```bash
cd /Users/davidberghahn/Documents/diplomarbeit-gamebased-learning/Website/Frontend
npm start
```

Die gemeinsame lokale Oberfläche unter `http://localhost:4200` verwenden. `localhost:8080` ist das Backend und kann weiterhin eine separate statische Quarkus-Seite ausliefern.

Prüfungen:

```bash
./mvnw test
cd Website/Frontend
npm run build
```

Historisch nachgewiesen am 22. September 2026; nach den späteren Commits hier nicht erneut ausgeführt:

- Maven: 9 Tests, 0 Fehler
- Angular: 29 Tests, 0 Fehler
- Angular-Produktionsbuild erfolgreich

## Deployment

- [`Dockerfile`](Dockerfile) baut zuerst Angular, kopiert das Ergebnis nach `META-INF/resources` und baut anschließend die Quarkus-Anwendung.
- [`.github/workflows/ci.yaml`](.github/workflows/ci.yaml) führt die Maven-Tests aus und veröffentlicht bei Änderungen auf `main` das Image in GHCR.
- [`k8s/backend.yaml`](k8s/backend.yaml) startet das API-Image und enthält wegen der internen Namensauflösung einen `hostAliases`-Eintrag für `auth.htl-leonding.ac.at` auf `10.191.112.13`.
- [`k8s/postgres.yaml`](k8s/postgres.yaml) startet PostgreSQL mit persistentem Volume.
- [`k8s/backend-ingress.yaml`](k8s/backend-ingress.yaml) veröffentlicht die Anwendung über die LeoCloud-URL.

Nach einem erfolgreichen GitHub-Actions-Imagebuild:

```bash
kubectl rollout restart deployment/gamebased-learning-api -n student-it220269
kubectl rollout status deployment/gamebased-learning-api -n student-it220269
curl https://it220269.cloud.htl-leonding.ac.at/api/health
```

Ein `git push` allein aktualisiert das Image in GHCR, startet aber das bestehende Kubernetes-Deployment nicht automatisch neu.

## Bekannte offene Punkte und Altlasten

1. Der Lehrer-Login muss mit dem aktuellen Deployment einmal end-to-end bestätigt werden. Die Backendlogik und Unit-Tests sind vorhanden, der reale Nachweis nach der Änderung fehlt noch.
2. Das Frontend besitzt bereits erste Route-Guards, weicht aber noch vom Fachentwurf ab: Schüler dürfen laut Entwurf öffentliche Spiele hosten; die derzeitige Lobby ist auf `TEACHER`/`ADMIN` beschränkt.
3. Spielsichtbarkeit und Erstellerrechte sind fachlich modelliert und als OpenSpec-Change geplant, aber noch nicht im Code umgesetzt. Kopien, Versionen, Sessions, Ergebnisse und Fortschritt folgen später.
4. Achievements und KI-Daten sind fachlich vorgesehen, aber noch nicht als vollständige persistente Domänenmodelle umgesetzt.
5. Der frühere Ordner `Website/Backend` wurde von Team-Commits wieder eingefügt und enthält unter anderem generierte Angular-Cache- und lokale Datenbankdateien. Er gehört nicht zur aktuellen Hauptarchitektur und sollte nach Abstimmung bereinigt sowie über `.gitignore` ausgeschlossen werden.
6. Das README verweist auf `docs/auth-architecture.md` und `docs/projektstand-einfach-erklärt.md`; diese Pfade existieren im aktuellen Stand nicht. Die vorhandene Erklärung heißt [`docs/leocloud-keycloak-backend-erklärt.md`](docs/leocloud-keycloak-backend-erklärt.md) und enthält am Ende noch eine veraltete Aussage, dass die LeoCloud-Keycloak-Verbindung offen sei. Diese Dokumentation muss aktualisiert werden.
7. Das PostgreSQL-Manifest enthält derzeit einfache, im Repository definierte Zugangswerte. Vor einer echten produktiven Nutzung mit sensiblen Daten sollte das Secret außerhalb des Manifests verwaltet und rotiert werden.
8. Das Frontend hatte zuletzt eine nicht-blockierende Angular-Budgetwarnung für `quizbattle.css`.

## Zusammenarbeit und Grenzen

- Vor Änderungen zuerst den aktuellen Git-Status, neue Team-Commits und die betroffenen Dateien prüfen.
- Änderungen der Teamkollegen nicht überschreiben; mit vorhandenen Änderungen arbeiten.
- Umlaute in deutschen Texten als `ä`, `ö`, `ü` schreiben, nicht als `ae`, `oe`, `ue`.
- David möchte Aufgaben in einem Chat stellen; bei größeren, sinnvoll teilbaren Aufgaben geeignete Spezialisten koordinieren.
- Veröffentlichungen nur als erfolgreich bezeichnen, wenn Build, Image und Live-Rollout jeweils tatsächlich geprüft wurden.
- Keine vollständigen Tokens, Zugangsdaten oder personenbezogenen User-Claims in Git oder Dokumentation speichern.
