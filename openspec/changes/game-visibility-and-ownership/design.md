# Design

## Context

Siehe `proposal.md` für den Anlass und `specs/game-access/spec.md` für das geforderte Verhalten. `Game` hat bereits ein optionales `createdBy`-Feld, aber keine Sichtbarkeit. `GameResource` listet und liest ohne Sichtbarkeitsprüfung; `GameSocket` verwendet dieselben ungeschützten Repository-Zugriffe. `GameResource` prüft für Schreibzugriffe nur die Rolle `TEACHER` oder `ADMIN`, nicht den Ersteller. Die Angular-Spieleliste und die Lobby-Detailansicht verwenden den WebSocket ohne Authentifizierungsdaten. Der Lobbycode ist derzeit nur ein Platzhalter; eine echte Session mit Schüler-Beitritt existiert noch nicht. Das Backend verwendet Hibernate `database.generation=update` und hat derzeit kein versioniertes Migrationswerkzeug.

## Goals / Non-Goals

**Goals:**

- Eine einzige serverseitige Sichtbarkeitsregel für alle Spiel-Lesewege; Spielekatalog und Detaildaten nur für gültig angemeldete Nutzer.
- Eigentümerprüfung vor jeder Änderung und Löschung, einschließlich Sichtbarkeitswechsel, mit bestätigter Administrator-Ausnahme.
- Bestehende Spiele ohne unbelegte Eigentümerzuschreibung erhalten und eine sichere Datenumstellung vorbereiten.
- Eigene private Spiele für die erstellende Lehrkraft als hostbar behandeln, ohne sie anderen Nutzern im Katalog offenzulegen.

**Non-Goals:**

- WebSocket-Authentifizierung oder Übertragung von Spieldaten über den bisher anonymen WebSocket-Katalog.
- Versionsverwaltung und laufende Sessions; das bisherige Update-Verhalten für Spielinhalte wird durch diesen Change nicht gelöst.
- Echte Lobby-Sessions, Schüler-Beitritt per Code und begrenzter Teilnehmerzugriff auf private Spielinhalte; dies braucht einen eigenen Session-Change.
- Kopierfunktion, Spielfamilien, Klassenrechte oder Administratorrechte außerhalb der Spieleverwaltung.

## Decisions

### Sichtbarkeit im Datenmodell und Migration

`Game.visibility` wird als String-Enum `PRIVATE`/`PUBLIC` gespeichert. Die REST-Erstellung setzt bei fehlender Angabe ausdrücklich `PRIVATE`; Demo-Spiele werden ausdrücklich `PUBLIC`. Bestehende Daten erhalten in einer kontrollierten SQL-Umstellung `PUBLIC`, bevor eine `NOT NULL`-Regel gilt. `created_by_id` bleibt für Altspiele vorerst nullable. Ein `teacher`-Anzeigename genügt nicht zur sicheren Zuordnung. Alternative, alle Altspiele automatisch einer Lehrkraft zuzuweisen, wäre eine unbelegte Rechteausweitung und wird verworfen.

Da noch kein Migrationsframework existiert, wird eine idempotent prüfbare PostgreSQL-Migration samt Ausführungsanweisung vorbereitet. Der Backfill setzt nur `visibility IS NULL` auf `PUBLIC`; bereits gespeicherte `PRIVATE`-Werte dürfen bei Wiederholung keinesfalls überschrieben werden. Ein dauerhafter Datenbankdefault `PUBLIC` wird nicht gesetzt, damit spätere Einfügungen nicht versehentlich veröffentlicht werden. Hibernate `update` allein darf nicht als Backfill betrachtet werden. Die Anwendung darf erst nach Prüfung des Backfills private Spiele anlegen. Eine spätere Einführung von Flyway ist ein separater Architekturentscheid.

### Gemeinsame Zugriffslogik für REST und WebSocket

Repository-Abfragen erhalten explizite Varianten für öffentliche Spiele, öffentliche plus eigene private Spiele und alle Spiele für Administratoren; Detailabrufe prüfen dieselbe Regel. Das REST-Leseprofil stammt aus einer gültig geprüften Schul-Keycloak-Identität. Fehlender oder ungültiger Token führt zu einem Authentifizierungsfehler statt zu Spieldaten. Inaktive Konten erhalten als technische Zugriffsschranke überhaupt keine Spieldaten; diese Regel ist nicht als gesonderte fachliche Antwort Davids zu verstehen. Alle bestehenden WebSocket-Katalogbefehle werden für Spielauflistung und Detailabruf geschlossen und liefern ohne neue Authentifizierung keine Spieldaten, auch nicht zu öffentlichen IDs. Der Client wechselt für Katalog und Lobby-Detailansicht auf authentifizierte REST-Abfragen. Ein Token wird nicht in WebSocket-URL oder Nachricht übertragen; die technische WebSocket-Authentifizierung wäre ein eigener Entscheid.

### Schreibrechte und Clientdaten

REST-Schreibzugriffe verwenden die geprüfte Identität und akzeptieren fachlich `TEACHER` oder `ADMIN`. Vor `PUT` und `DELETE` wird der gespeicherte `createdBy.id` mit der aktiven Lehrkraft verglichen; bei `ADMIN` entfällt dieser Besitzervergleich ausdrücklich. Die Prüfung und Änderung erfolgen transaktional. Ein Spiel ohne gespeicherten Ersteller bleibt für Lehrkräfte schreibgeschützt, kann aber von einem Administrator verwaltet werden, ohne dass ihm die Erstellerschaft zugeschrieben wird. Bei privaten Fremdspielen wird die Existenz gegenüber Schülern und anderen Lehrkräften nicht offengelegt. `createdBy`, IDs und andere serververwaltete Felder werden bei Updates nicht aus dem Client übernommen. Alternative, nur Buttons im Frontend auszublenden, schützt die API nicht.

### Angular-Anbindung

Die Spieleliste und die Lobby-Detailansicht rufen REST-Endpunkte mit dem vorhandenen Bearer-Token ab. Der Client zeigt eigene private Spiele erkennbar als privat an und bietet der erstellenden Lehrkraft dafür die Host-Aktion; Administratoren dürfen ebenfalls hosten. Schüler dürfen öffentliche Spiele hosten. Die Anzeige eines Host-Buttons ist keine Freigabe für fremde private Spiele: Die Lobby lädt ihren Spielinhalt erneut über den serverseitig geprüften Detailabruf. Der derzeitige Lobbycode ist ein Platzhalter; die sichere Weitergabe eines privaten Spiels an beitretende Schüler erfordert einen späteren Session-Change. Eine neue Bearbeitungsoberfläche und ein darauf zugeschnittenes `isOwner`-Feld sind nicht nötig; die Serverprüfung bleibt maßgeblich.

## Risks / Trade-offs

- [Bestehende anonyme API-Clients] → Auch öffentliche Spiele erfordern jetzt Anmeldung. Der bisherige WebSocket-Katalog liefert keine Spiele mehr; Clients müssen auf authentifizierte REST-Abfragen wechseln.
- [Altspiele ohne `createdBy`] → Sie bleiben nach Anmeldung öffentlich lesbar und für Lehrkräfte schreibgeschützt. Administratoren können sie verwalten; eine spätere Zuordnung braucht geprüfte Herkunft und eine eigene Entscheidung.
- [Rollback auf den alten Backend-Code] → Der alte Code würde private Spiele ungeschützt ausgeben. Nach dem ersten privaten Spiel ist ein reiner Code-Rollback unsicher; stattdessen den korrigierten Zugriffspfad wiederherstellen oder den Dienst bis dahin schützen. Eine pauschale Veröffentlichung privater Daten ist kein Rollback.
- [Administratorrechte] → Die bestätigte Ausnahme erlaubt Administratoren auch die Einsicht und Änderung fremder privater Spiele. Die Berechtigung muss serverseitig geprüft werden; die gespeicherte Erstellerschaft wird nicht automatisch geändert.
- [Frontend und Backend laufen mit unterschiedlichem Stand] → Die API-Filterung wird zuerst wirksam; der alte Client kann nach Schließung des WebSocket-Katalogs keine Spieleliste mehr laden. Frontend und Backend müssen deshalb koordiniert aktualisiert werden.
- [Privates Hosting mit Schülern] → Dieser Change ermöglicht dem Ersteller den geschützten Lobby-Detailabruf, aber noch keinen sicheren Schüler-Beitritt. Eine produktive private Gruppenrunde setzt den Folge-Change zu Sessions und Beitrittsrechten voraus.

## Migration Plan

1. Bestand zählen und prüfen: vorhandene Spiele, `created_by_id = NULL`, aktuelle Datenbankstruktur; keine Eigentümer aus `teacher_name` ableiten.
2. PostgreSQL-Schema und Daten kontrolliert erweitern: `visibility` für Bestand auf `PUBLIC` setzen, erlaubte Werte und `NOT NULL` absichern; Migration in einer Transaktion und mit Vor-/Nachprüfung ausführen.
3. Backend mit Anmeldungspflicht, Sichtbarkeitsfiltern und Eigentümer-/Administratorprüfung sowie Angular-Spieleliste und Lobby-Detailansicht mit authentifiziertem REST-Leseweg als koordinierten Anwendungsstand ausrollen. Die alte Liste darf nicht durch vorzeitige Schließung des WebSocket-Katalogs unbenutzbar werden.
4. REST- und gesperrte WebSocket-Katalogbefehle prüfen. Sicherstellen, dass die erstellende Lehrkraft und Administratoren eigene beziehungsweise berechtigte private Spiele als hostbar sehen, Schüler öffentliche Spiele hosten können und fremde private Spiele nicht abrufbar sind. Erst nach diesen Prüfungen neue private Spiele freigeben.

Ein Rollback vor Erstellung privater Spiele kann die Anwendung auf den vorherigen Stand setzen, während die zusätzliche Spalte ungenutzt bleibt. Danach gilt die Rollback-Einschränkung unter „Risks / Trade-offs“.
