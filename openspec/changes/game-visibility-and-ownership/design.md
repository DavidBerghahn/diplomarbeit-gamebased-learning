# Design

## Context

Siehe `proposal.md` für den Anlass und `specs/game-access/spec.md` für das geforderte Verhalten. `Game` hat bereits ein optionales `createdBy`-Feld, aber keine Sichtbarkeit. `GameResource` listet und liest ohne Sichtbarkeitsprüfung; `GameSocket` verwendet dieselben ungeschützten Repository-Zugriffe. `GameResource` prüft für Schreibzugriffe nur die Rolle `TEACHER` oder `ADMIN`, nicht den Ersteller. Die Angular-Spieleliste verwendet den WebSocket ohne Authentifizierungsdaten. Das Backend verwendet Hibernate `database.generation=update` und hat derzeit kein versioniertes Migrationswerkzeug.

## Goals / Non-Goals

**Goals:**

- Eine einzige serverseitige Sichtbarkeitsregel für alle Spiel-Lesewege; keine Sicherheitsentscheidung allein im Frontend.
- Eigentümerprüfung vor jeder Änderung und Löschung, einschließlich Sichtbarkeitswechsel.
- Bestehende Spiele ohne unbelegte Eigentümerzuschreibung erhalten und eine sichere Datenumstellung vorbereiten.

**Non-Goals:**

- WebSocket-Authentifizierung oder Übertragung privater Spiele über den WebSocket.
- Versionsverwaltung und laufende Sessions; das bisherige Update-Verhalten für Spielinhalte wird durch diesen Change nicht gelöst.
- Kopierfunktion, Spielfamilien, Klassenrechte oder allgemeine Admin-Sonderrechte.

## Decisions

### Sichtbarkeit im Datenmodell und Migration

`Game.visibility` wird als String-Enum `PRIVATE`/`PUBLIC` gespeichert. Die REST-Erstellung setzt bei fehlender Angabe ausdrücklich `PRIVATE`; Demo-Spiele werden ausdrücklich `PUBLIC`. Bestehende Daten erhalten in einer kontrollierten SQL-Umstellung `PUBLIC`, bevor eine `NOT NULL`-Regel gilt. `created_by_id` bleibt für Altspiele vorerst nullable. Ein `teacher`-Anzeigename genügt nicht zur sicheren Zuordnung. Alternative, alle Altspiele automatisch einer Lehrkraft zuzuweisen, wäre eine unbelegte Rechteausweitung und wird verworfen.

Da noch kein Migrationsframework existiert, wird eine idempotent prüfbare PostgreSQL-Migration samt Ausführungsanweisung vorbereitet. Der Backfill setzt nur `visibility IS NULL` auf `PUBLIC`; bereits gespeicherte `PRIVATE`-Werte dürfen bei Wiederholung keinesfalls überschrieben werden. Ein dauerhafter Datenbankdefault `PUBLIC` wird nicht gesetzt, damit spätere Einfügungen nicht versehentlich veröffentlicht werden. Hibernate `update` allein darf nicht als Backfill betrachtet werden. Die Anwendung darf erst nach Prüfung des Backfills private Spiele anlegen. Eine spätere Einführung von Flyway ist ein separater Architekturentscheid.

### Gemeinsame Zugriffslogik für REST und WebSocket

Repository-Abfragen erhalten explizite Varianten für öffentliche Spiele und für öffentliche plus eigene private Spiele; Detailabrufe prüfen dieselbe Regel. Das REST-Leseprofil stammt aus einer optionalen, gültig geprüften Schul-Keycloak-Identität. Ohne Anmeldung wird nur öffentlich gelesen; ein ungültiger Authentifizierungsversuch wird nicht als anonymer Nutzer behandelt. Inaktive Konten erhalten keinen Zugriff auf eigene private Spiele. Der WebSocket nutzt ausschließlich öffentliche Repository-Varianten. Ein direkter privater Abruf dort beantwortet wie eine unbekannte ID, damit die Existenz nicht verraten wird. Alternative, den Access Token in WebSocket-URL oder Nachricht zu schicken, würde einen neuen Authentifizierungspfad mit höherem Risiko und größerem Umfang schaffen.

### Schreibrechte und Clientdaten

REST-Schreibzugriffe verwenden die geprüfte Identität und akzeptieren fachlich nur `TEACHER`. Vor `PUT` und `DELETE` wird der gespeicherte `createdBy.id` mit der aktiven Lehrkraft verglichen; fehlender Besitzer bedeutet Ablehnung. Diese Prüfung und die Änderung erfolgen transaktional, damit zwischen Prüfung und Änderung kein anderer Zustand entsteht. Bei öffentlichen Fremdspielen wird eine Berechtigungsverweigerung geliefert; bei privaten Fremdspielen wird die Existenz nicht offengelegt. `createdBy`, IDs und andere serververwaltete Felder werden bei Updates nicht aus dem Client übernommen. Die technische Rolle `ADMIN` ist kein impliziter Lehrkraft-Ersatz. Alternative, nur Buttons im Frontend auszublenden, schützt die API nicht.

### Angular-Anbindung

Die Spieleliste ruft für angemeldete Nutzer den REST-Endpunkt mit dem vorhandenen Bearer-Token ab, damit eigene private Spiele sichtbar sind. Der WebSocket bleibt für bestehende öffentliche Katalog-Clients und die öffentliche Lobby-Detailansicht verfügbar. Der Client zeigt private Spiele erkennbar als privat an und bietet für sie keine Host-Aktion. Dies folgt dem fachlichen Entwurf, der Hosting für öffentliche Spiele vorsieht. Eine neue Bearbeitungsoberfläche und ein darauf zugeschnittenes `isOwner`-Feld sind nicht nötig; die Serverprüfung bleibt maßgeblich.

## Risks / Trade-offs

- [Bestehende anonyme API-Clients] → Öffentliche Spiele bleiben über REST und WebSocket ohne Anmeldung lesbar; nur private Inhalte werden ausgefiltert.
- [Altspiele ohne `createdBy`] → Sie bleiben öffentlich lesbar, aber schreibgeschützt. Eine spätere Zuordnung braucht geprüfte Herkunft und eine eigene Entscheidung.
- [Rollback auf den alten Backend-Code] → Der alte Code würde private Spiele ungeschützt ausgeben. Nach dem ersten privaten Spiel ist ein reiner Code-Rollback unsicher; stattdessen den korrigierten Zugriffspfad wiederherstellen oder den Dienst bis dahin schützen. Eine pauschale Veröffentlichung privater Daten ist kein Rollback.
- [Rollenabweichung `ADMIN`] → Die bisherige technische Administrator-Schreibberechtigung entfällt entsprechend dem fachlichen Entwurf; bestehende Admin-Workflows müssen vor einer Produktivschaltung geprüft werden.
- [Frontend und Backend laufen mit unterschiedlichem Stand] → Die API-Filterung wird zuerst wirksam; der alte Client sieht weiterhin alle öffentlichen Spiele, aber noch keine eigenen privaten.

## Migration Plan

1. Bestand zählen und prüfen: vorhandene Spiele, `created_by_id = NULL`, aktuelle Datenbankstruktur; keine Eigentümer aus `teacher_name` ableiten.
2. PostgreSQL-Schema und Daten kontrolliert erweitern: `visibility` für Bestand auf `PUBLIC` setzen, erlaubte Werte und `NOT NULL` absichern; Migration in einer Transaktion und mit Vor-/Nachprüfung ausführen.
3. Backend mit Sichtbarkeitsfiltern und Eigentümerprüfung ausrollen, REST-/WebSocket-Verhalten verifizieren; erst danach private Spiele im Client freigeben.
4. Angular-Spieleliste auf den authentifizierten REST-Leseweg umstellen und prüfen, dass private Einträge sichtbar, aber nicht hostbar sind. Die Lobby-Detailansicht bleibt beim öffentlichen WebSocket.

Ein Rollback vor Erstellung privater Spiele kann die Anwendung auf den vorherigen Stand setzen, während die zusätzliche Spalte ungenutzt bleibt. Danach gilt die Rollback-Einschränkung unter „Risks / Trade-offs“.
