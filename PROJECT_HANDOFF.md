# Projektübergabe: Diplomarbeit Game-based Learning

Stand: 22. September 2026

## Ziel und nächster Einstieg

Das Projekt ist eine KI-gestützte Spieleplattform für Game-based Learning an der HTL Leonding. Davids Schwerpunkt umfasst Backend, Schul-Keycloak-Login, Benutzer- und Rollenverwaltung, PostgreSQL, APIs sowie das Deployment auf LeoCloud. Die Teamkollegen arbeiten vor allem an Spielen und KI-Inhalten.

Der nächste sinnvolle Einstieg ist:

1. Lehrerlogin mit einem echten Lehreraccount erneut testen und in `Login-Details` sowie `GET /api/auth/me` verifizieren, dass `role: TEACHER` geliefert wird.
2. Die noch uncommitteten Rollen- und Autorisierungsänderungen gemeinsam prüfen, committen und anschließend deployen.
3. Danach Datenmodelle und APIs für von Lehrkräften erstellte Spiele, Fragen, Ergebnisse und Lernfortschritt konkretisieren.

## Fortsetzung am 22. September 2026

- Ein echter Login wurde live erneut durchgeführt. Das verwendete Konto wurde von `GET /api/auth/me` korrekt als `STUDENT` erkannt; ein echter Lehreraccount wurde dabei nicht verwendet. Der Nachweis für `role: TEACHER` bleibt daher offen.
- Die Frontend-Routen unterscheiden jetzt zwischen allgemeinen angemeldeten Benutzern und `TEACHER`/`ADMIN`. `Meine Spiele`, `Spiel erstellen` und die Host-Lobby sind für Schüler weder als Aktion sichtbar noch direkt per URL erreichbar.
- Der OIDC-PKCE-Flow bewahrt ein geprüftes internes Rücksprungziel. Externe oder unbekannte Ziele werden auf `/home` zurückgesetzt.
- Inaktive Konten werden bei schreibenden Spieleaktionen und allen Endpunkten der Benutzerverwaltung auch serverseitig mit `403` abgewiesen.
- Die Änderungen sind lokal implementiert und geprüft, aber noch nicht committet, gepusht oder deployt.

## Verifizierter Projektzugang

- Lokaler Pfad: `/Users/davidberghahn/Documents/diplomarbeit-gamebased-learning`
- Repository: `https://github.com/DavidBerghahn/diplomarbeit-gamebased-learning`
- Branch: `main`
- Aktueller Commit lokal und auf `origin/main`: `e37c583` (`Recognize teachers from LDAP distinguished name`)
- Lokal existieren uncommittete Änderungen für Rollen-Guards, rollenabhängige Lehreraktionen, sichere Login-Rücksprünge, serverseitige Aktivitätsprüfungen und die zugehörigen Tests. `PROJECT_HANDOFF.md` ist weiterhin eine neue, noch nicht committete Datei.
- LeoCloud-Namespace: `student-it220269`
- Live-Adresse: `https://it220269.cloud.htl-leonding.ac.at/`
- Container-Image: `ghcr.io/davidberghahn/diplomarbeit-gamebased-learning:latest`

## Heute verifizierter Live-Zustand

Am 22. September 2026 wurden folgende Punkte read-only geprüft:

- `GET /api/health` antwortet mit `status: UP`.
- Das API-Deployment hat `1/1` verfügbare Replica.
- Der PostgreSQL-Pod ist `1/1 Running`.
- Service und Ingress sind vorhanden; der Ingress zeigt auf `it220269.cloud.htl-leonding.ac.at`.

Das belegt die Erreichbarkeit und den Kubernetes-Zustand an diesem Datum. Ein vollständiger End-to-End-Login mit einem Lehreraccount wurde heute nicht erneut durchgeführt.

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

Zuletzt nachgewiesen am 22. September 2026:

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
2. Das Frontend besitzt noch keine konsequente Route-Guard-/Rollenstruktur für Schüler- und Lehreransichten.
3. Lehrer sollen später Spielmodi auswählen und eigene Fragen/Antworten verwalten. Dafür fehlen noch vollständige Besitz-, Veröffentlichungs- und Bearbeitungsregeln im Datenmodell.
4. Ergebnisse, Fortschritt, Achievements und KI-Daten sind fachlich vorgesehen, aber noch nicht als vollständige persistente Domänenmodelle umgesetzt.
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
