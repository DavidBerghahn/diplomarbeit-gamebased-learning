# diplomarbeit-gamebased-learning

KI-gestützte Spieleplattform für Game-based Learning.

## Backend

Das Backend ist als Quarkus-Anwendung vorbereitet und verwendet den
Schul-Keycloak der HTL Leonding. Die LDAP-Daten der Schule werden nicht direkt
aus der Anwendung gelesen, sondern kommen über Claims im Keycloak Token.

Wichtige Endpoints:

```text
GET /api/health
GET /api/auth/me
GET /api/users/me
PUT /api/users/me
GET /api/users          # TEACHER oder ADMIN
PATCH /api/users/{id}   # ADMIN
GET /api/games
GET /api/games/grouped
GET /api/games/type/{gameType}
GET /api/games/{id}
POST /api/games         # TEACHER oder ADMIN
PUT /api/games/{id}     # TEACHER oder ADMIN
DELETE /api/games/{id}  # TEACHER oder ADMIN
WS  /user-socket
GET /api/docs
```

Die Keycloak-Anbindung läuft über den gleichen `AuthProvider`. Die fachliche
Datenbank für Userprofile, Fortschritt, Spiele und KI-Daten bleibt dadurch
stabil. Siehe `docs/auth-architecture.md`.

Eine einfache deutsche Erklärung zu Login, Userverwaltung, Docker, GHCR,
Kubernetes und LeoCloud steht in `docs/projektstand-einfach-erklärt.md`.

Lokal testen:

```bash
./mvnw test
```

Backend mit Keycloak-Login lokal starten:

```bash
./mvnw quarkus:dev \
  -Dquarkus.console.enabled=false \
  -Dquarkus.analytics.disabled=true \
  -Ddebug=false
```

Das Frontend sendet nach dem Schul-Login einen Bearer Token mit:

```text
Authorization: Bearer <keycloak-token>
```

Die integrierte Test-Loginseite ist danach unter `http://localhost:8080/`
erreichbar. Wenn Keycloak eine `invalid redirect_uri` Meldung zeigt, muss der
Keycloak-Client `frontend` von einem Admin für die lokale URL und die LeoCloud-
URL freigeschaltet werden.

Im Dev-Modus verwendet das Backend H2 im Speicher. Das gebaute Docker-/LeoCloud-
Artefakt verwendet im `prod`-Profil PostgreSQL.

## Frontend und Spiele

Der Ordner `Website/Frontend` enthält das Angular-Frontend der Spieleplattform.
Im Docker-Build wird dieses Frontend gebaut und anschließend vom Quarkus-
Backend als statische Web-App ausgeliefert.

Das frühere separate Demo-Backend unter `Website/Backend` dient nur noch als
Quelle für die Spielidee. Die fachlich relevanten Teile daraus sind im
Hauptbackend integriert:

```text
Game
Question
AnswerOption
GET /api/games
WS /user-socket
```

Damit laufen Login, Userverwaltung, Spiele-API und PostgreSQL über dasselbe
Backend.

## LeoCloud

Dieses Projekt ist für ein LeoCloud-Deployment vorbereitet.

- Namespace: `student-it220269`
- Host: `it220269.cloud.htl-leonding.ac.at`
- Image: `ghcr.io/davidberghahn/diplomarbeit-gamebased-learning:latest`
- Datenbank-Service: `gamebased-learning-postgres`

Deployment:

```bash
kubectl config use-context leocloud
kubectl apply -n student-it220269 -f k8s/postgres.yaml
kubectl apply -n student-it220269 -f k8s/backend.yaml
kubectl apply -n student-it220269 -f k8s/backend-ingress.yaml
kubectl rollout status deployment/gamebased-learning-api -n student-it220269
```

Danach sollte die Seite erreichbar sein unter:

```text
https://it220269.cloud.htl-leonding.ac.at/
```

Healthcheck:

```bash
curl https://it220269.cloud.htl-leonding.ac.at/api/health
```
