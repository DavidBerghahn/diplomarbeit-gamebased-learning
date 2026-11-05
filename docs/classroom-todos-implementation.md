# Umsetzung der Classroom-Todos

Stand: 4. November 2026. Ausgangspunkt: main, 9301ae3.
Der Nutzer hat die Umsetzung im Supervisor-Chat sowie Commit und GitHub-Push freigegeben.
LeoCloud-Rollout und Änderungen an der Cloud-Datenbank sind ausdrücklich ausgenommen.

## Abgleich der angefragten Punkte

| Punkt | Umsetzung | Nachweis |
|---|---|---|
| Einstellbarer Timer | 5–120 Sekunden, Standard 20, Server führt Timeout aus | ClassroomIntegrationTest: Timeout ohne Clientbefehl; Lobby-Build |
| Eindeutiger Code | Vergabe und Reservierung synchronisiert, eine aktive Instanz | Test paralleler Erstellung; Recreate-Strategie im vorbereiteten Manifest |
| Ablauf nach 24 Stunden | Ab Erstellung; auch laufende Spiele werden geschlossen | Raumablauftest |
| Host-Mitspielen | Toggle für alle Rollen einschließlich Lehrer; ohne Toggle nur Beobachter | Host- und echter WebSocket-Test; host_participates in V4 |
| Teamleiter im Ergebnis | Zufällig beim Start bestimmt und im Team-/Ergebnisbereich angezeigt | Teammitgliedschaftstest und Frontendbuild |
| Navigation | Startseite, Spieleübersicht, Raum verlassen; Verlassen bricht aktive Runde ab | Router-/Komponententests, Build |
| Gleichmäßige Teams | Zufallswahl unter den jeweils kleinsten Teams | Verteilungstest mit sieben Spielern und drei Teams |
| Freie Teamwahl | Selbstwechsel, Host-Umverteilung; Unterschied über eins nur nach Bestätigung; keine leeren Teams | Team-/Starttests |
| Just One: Ratender sieht nur Zahlen | Gemeinsamer Raum, getrennte Nachrichten, Zielwort erst bei Auflösung | Test prüft tatsächlichen Nachrichteninhalt |
| Just One: ungültige Versuche löschen | Ursprünglicher Host kann Versuche auch nach Abschluss korrigieren; maximal ein Punkt je vergebenem Versuch, keine doppelte Rücknahme | Speicher-/Idempotenz-/Fremdhosttests |
| Datenbankanbindung | Spielerstellung, Kopien, Status, Katalog, Solo und Multiplayer an gespeicherte Versionen angeschlossen; bestätigter Commit und Retry | API-Vertrags-, Solo-, Speicher- und echter WebSocket-Test; H2 und PostgreSQL |
| CSV beider Spiele | Vorlagen für drei Smart10-Modi und Just One; Vorschau, Quotes/Zeilenumbrüche, Kartenanzahl-/Rangprüfung; serverseitige Prüfung beim Speichern | CSV-Tests und Erstellungstest |
| Statistik ohne Antwortdetails | Statistik-DTO enthält nur Spiele, Modus, Datum, Punkte und Mitspieler | DTO-Test ohne Antwortinhalte |
| Alle Klassen und Schüler | Lehrer/Admin sehen alle Klassen und Schüler sowie Spielhäufigkeit und Mitspieler; Schüler erhalten 403 | Klassen-/Berechtigungstest |

Die lokale Just-One-Demo bleibt als Quelldatei erhalten, ist aber nicht mehr der normale Spielweg.
Die Route /justOne führt zur Spieleauswahl. Gespeicherte Just-One-Spiele werden über
Alle Spiele → Just One → Lobby erstellen gestartet. Just One benötigt mindestens
zwei Mitglieder je Team.

Private Schüler-Einladungen bleiben außerhalb des Auftrags: Ein Code umgeht die
Besitzer-/Admin-Prüfung privater Spielinhalte nicht. Für eine Klassenrunde mit
Schülern ist das Spiel zunächst öffentlich zu stellen.

## Ausgeführte Prüfungen

- Vollständiger Backendlauf mit H2 und zusätzlichen echten PostgreSQL-Migrationstests;
  vollständiger zweiter Lauf mit PostgreSQL 16 als tatsächlicher Anwendungsdatenbank.
  Ergebnisse und genaue Testzahlen stehen im aktuellen Einstieg von PROJECT_HANDOFF.md.
- Frontend: 65 Tests erfolgreich, npm run build erfolgreich.
- Der Integrationstest öffnet zwei echte WebSocket-Verbindungen gegen Quarkus,
  bindet Tickets, prüft den Lobby-Broadcast und einen gespeicherten Spielabschluss.
- Lokaler PostgreSQL-Backup-/Restore-Test mit pg_dump/pg_restore: Vorher und nachher
  92 Spiele, 40 abgeschlossene Sessions, 55 Teilnehmer und vier Just-One-Versuche.
  Das sind ausschließlich synthetische Testdaten, kein Cloud-Abzug.
- OpenSpec-Validierung und Git-Diff-Prüfung. Kein unabhängiger Rollen-Chat-Review:
  Der Nutzer hat ausdrücklich die Durchführung im Supervisor-Chat gewählt.
- Kein echter Schul-Login im Browser und kein LeoCloud-Livetest durchgeführt.
  Die bekannte H2/Flyway-Versionswarnung bleibt; die geprüften Tests bestehen.

## Datenfluss

### Ergänzung der Klassenstatistik am 5. November 2026

Unter `/statistics` kann ein Lehrer/Admin alle Klassen oder eine einzelne Klasse
auswählen. Angezeigt werden die gespeicherten Punkte je Schüler, die Summe pro
Klasse und der Spielverlauf. Ein Datumsfilter gilt einheitlich für alle Werte.
Schüler ohne Spiele zählen mit null Punkten. Die Klassensumme zählt die Punkte
jedes Schülers, auch wenn mehrere Schüler denselben Teamwert erhalten haben.
Die Zahl abgeschlossener Spiele zählt dieselbe Session pro Klasse dagegen nur einmal.
Die Projektmitglieder mit Adminrechten zählen ebenfalls als Schüler, inklusive
ihrer Klasse und Punkte. Lehrerkonten bleiben außerhalb der Schülerwertung.
Fehlende Klassen können auch für Schüler mit Adminrechten ergänzt werden, ohne
ihre Rolle zu verändern.

Die Klasse stammt aus den Klassenclaims beziehungsweise dem LDAP-DN des
Schulkontos. Sie wird getrimmt und in Großbuchstaben gespeichert. Fehlt die
Klasse bei einem späteren Login, bleibt eine vorhandene Zuordnung erhalten.
Fehlende Angaben erscheinen unter „Ohne Klasse“ und können dort von Lehrern/Admins
ergänzt werden. Ohne bekannte Klasse wird keine Zuordnung erfunden. Ein späterer
nichtleerer Schulclaim aktualisiert die Klasse wieder. Für die Anzeige gilt die
aktuelle Klasse einschließlich aller bisherigen Punkte; historische Ergebnisse
werden nicht geändert.

Der Nutzer hat anschließend Commit und GitHub-Push des lokalen Folgeauftrags
freigegeben; ein LeoCloud-Rollout bleibt ausdrücklich ausgeschlossen. Geprüft wurden
74 Frontendtests, der Frontendbuild und nach der Admin-Korrektur 59 Backendtests; sieben zusätzliche
PostgreSQL-Migrationstests wurden mangels laufender Testinstanz übersprungen.

Frontend → authentifizierte HTTP-API / WebSocket-Ticket → Quarkus → H2 oder PostgreSQL.
Der Browser verbindet sich nie direkt mit PostgreSQL. Die bestehende Schul-Keycloak-Anbindung bleibt die Identitätsquelle.
Ein WebSocket-Ticket wird über authentifiziertes HTTP geladen, vor der ersten Spielaktion gebunden
und nicht als URL-Parameter versendet. Gespeicherte Smart10-Runden laden Inhalte und Lösungen
serverseitig aus der aktuellen unveränderlichen Version.

Nur abgeschlossene Runden erzeugen Sessions. Demos und abgebrochene Räume zählen nicht zur
Klassenstatistik. Just-One-Versuche speichern nur Team, Punktvergabe und Ungültig-Markierung,
keine Begriffe, Hinweise oder geratene Antworten. Historische Smart10-Kartenantworten bleiben
entsprechend dem bestehenden Modell erhalten, werden aber nicht in der Statistik ausgeliefert.
Korrekturen sind über Startseite → Just-One-Spielverlauf auch nach Raumende erreichbar.

## Lokaler Betrieb

Backend im Repository:

```powershell
.\mvnw.cmd quarkus:dev
```

Frontend in einem zweiten Terminal unter Website/Frontend:

```powershell
npm start
```

Oberfläche: http://localhost:4200. Der Entwicklungsproxy leitet /api und /user-socket
an dasselbe Backend auf Port 8080. Dev-H2 ist dateibasiert unter .local-data/gamebased
und überlebt Backend-Neustarts; dieser Ordner ist ignoriert. Tests verwenden weiterhin
isoliertes H2 im Speicher. Neue Migrationen nicht umschreiben und vorhandene Daten
nicht durch Löschen der Datenbank „reparieren“.

Für eine eigene lokale PostgreSQL-Datenbank kann das Profil local-pg benutzt werden:

```powershell
$env:DATABASE_JDBC_URL = 'jdbc:postgresql://127.0.0.1:5432/gamebased'
$env:DATABASE_USERNAME = 'gamebased'
# DATABASE_PASSWORD lokal bereitstellen, nicht in Git oder Chat.
.\mvnw.cmd quarkus:dev '-Dquarkus.profile=dev,local-pg'
```

Der Healthcheck /api/health prüft jetzt die DB-Verbindung: HTTP 200 mit database=UP,
bei fehlender Datenbankverbindung HTTP 503.

## Späterer manueller LeoCloud-Rollout

Hier wurde kein kubectl-Befehl und kein Zugriff auf die Cloud-Datenbank ausgeführt.
Das bestehende Manifest verwendet DATABASE_JDBC_URL und Secret-Referenzen; sein
PostgreSQL-Service heißt gamebased-learning-postgres. Es ist eine aktive Backendinstanz
vorgesehen. Recreate verhindert zwei gleichzeitig aktive In-Memory-Raumdienste beim
Rollout; laufende Räume gehen beim Neustart verloren.

1. Vor dem Rollout das tatsächliche Schema und bestehende Daten prüfen und sichern.
   „Vermutlich leer“ ist kein Nachweis. Restore vor Freischaltung testen.
2. Neue, leere Datenbanken lassen V1–V4 regulär laufen. Eine bestehende Legacy-Datenbank
   ohne Flyway-Historie nur nach dem Abgleich in docs/flyway.md ausdrücklich baselinen.
   Automatische Baseline, Clean und Repair bleiben ausgeschaltet.
3. Das Image des geprüften Commits verwenden. Frontend und Backend gemeinsam ausrollen,
   weil das Ticketprotokoll und die Kartenkennungen zusammenpassen müssen.
4. Im freigegebenen Wartungsfenster FLYWAY_PROD_MIGRATE=true für die Migration und
   SMART10_PERSISTENCE_ENABLED=true für gespeicherte Spielabläufe ausdrücklich setzen.
   Beide sind standardmäßig false. Ohne diese Freischaltung ist eine neue Prod-DB
   nicht automatisch eingerichtet und der persistente Spielpfad bleibt geschlossen.
5. Nach dem Rollout Flyway-Historie, Readiness und /api/health prüfen; dann mit echten
   Benutzerkonten öffentliches/privates Spiel, Soloabschluss, Mehrspielerabschluss
   und Klassenstatistik kontrollieren.
6. Nach erfolgreicher Migration FLYWAY_PROD_MIGRATE bei Bedarf wieder deaktivieren.
   Kein ungeschütztes Downgrade auf ein altes Backend: dessen öffentliche Lesewege
   kennen die neue Sichtbarkeit nicht. Rückfall nur mit zusammenpassendem geprüftem
   Image und DB-Backup oder durch eine vorwärts gerichtete Korrektur.

Der vollständige produktive Bestands-/Rollback-Nachweis des Foundation-Changes bleibt
eine separate Freischaltaufgabe. Weder lokale Tests noch ein GitHub-Push bestätigen
einen LeoCloud-Rollout oder einen echten Keycloak-Login in der Cloud.
