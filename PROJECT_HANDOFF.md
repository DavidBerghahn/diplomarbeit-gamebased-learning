# Projektübergabe: Diplomarbeit Game-based Learning

Stand: 23. September 2026

## Ziel und nächster Einstieg

Das Projekt ist eine KI-gestützte Spieleplattform für Game-based Learning an der HTL Leonding. Davids Schwerpunkt umfasst Backend, Schul-Keycloak-Login, Benutzer- und Rollenverwaltung, PostgreSQL, APIs sowie das Deployment auf LeoCloud. Die Teamkollegen arbeiten vor allem an Spielen und KI-Inhalten.

Der nächste sinnvolle Einstieg ist:

1. Den geplanten OpenSpec-Change [`game-visibility-and-ownership`](openspec/changes/game-visibility-and-ownership/proposal.md) gemeinsam durchsehen. Er regelt private/öffentliche Spiele und Eigentümerrechte, ist aber **noch nicht implementiert**. Erst nach einem neuen Umsetzungsauftrag mit `$openspec-apply-change` beginnen.
2. Vor einer späteren Produktivschaltung die Datenmigration und den Rollback-Schutz aus dem [Design](openspec/changes/game-visibility-and-ownership/design.md) prüfen. Ein alter Backend-Stand würde private Spiele offenlegen.
3. Den echten Lehrerlogin mit einem Lehreraccount end-to-end prüfen. Danach weitere Datenmodellteile wie Kopien, Sessions, Ergebnisse und Fortschritt in getrennten Changes angehen.

## Fortsetzung am 23. September 2026

- Die Rollen- und Autorisierungsänderungen sowie der fachliche [Datenmodell-Entwurf Version 1](docs/datenmodell-v1.md) sind mit `c9f3859` committet. Der Entwurf ist noch nicht als vollständiges Persistenzmodell umgesetzt.
- OpenSpec und die Projekt-Skills wurden mit `17e3d12` eingerichtet. Proposal, Spezifikation, Design und Aufgabenliste des ersten Changes wurden mit `173b655` committet; `openspec validate game-visibility-and-ownership --strict` war erfolgreich. Anwendungscode wurde dafür nicht geändert.
- Fünf Codex-Worktrees wurden mit dem lokalen `main` synchronisiert und waren danach sauber; `openspec doctor` war dort erfolgreich. Die vorherigen, unversionierten OpenSpec-Kopien wurden zur Sicherheit in fünf Git-Stashes abgelegt. Den HEAD der Worktrees bei Fortsetzung erneut prüfen.
- Für Supervisor, Researcher, Coder, Writer und Reviewer wurden zuvor separate Codex-Aufgaben angestoßen, aber am 23. September war in der Aufgabenliste nur der ursprüngliche Projektchat nachweisbar. Die Worktrees allein belegen keine einsatzbereiten Chats. Keine doppelte Anlage ohne Statusklärung.
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
