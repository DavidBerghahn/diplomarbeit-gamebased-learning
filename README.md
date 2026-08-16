# diplomarbeit-gamebased-learning

KI-gestuetzte Spieleplattform fuer Game-based Learning.

## Backend

Das Backend ist als Quarkus-Anwendung vorbereitet und verwendet den
Schul-Keycloak der HTL Leonding. Die LDAP-Daten der Schule werden nicht direkt
aus der Anwendung gelesen, sondern kommen ueber Claims im Keycloak Token.

Wichtige Endpoints:

```text
GET /api/health
GET /api/auth/me
GET /api/users/me
PUT /api/users/me
GET /api/users          # TEACHER oder ADMIN
PATCH /api/users/{id}   # ADMIN
GET /api/docs
```

Die Keycloak-Anbindung laeuft ueber den gleichen `AuthProvider`. Die fachliche
Datenbank fuer Userprofile, Fortschritt, Spiele und KI-Daten bleibt dadurch
stabil. Siehe `docs/auth-architecture.md`.

Eine einfache deutsche Erklaerung zu Login, Userverwaltung, Docker, GHCR,
Kubernetes und LeoCloud steht in `docs/projektstand-einfach-erklaert.md`.

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
Keycloak-Client `frontend` von einem Admin fuer die lokale URL und die LeoCloud-
URL freigeschaltet werden.

Im Dev-Modus verwendet das Backend H2 im Speicher. Das gebaute Docker-/LeoCloud-
Artefakt verwendet im `prod`-Profil PostgreSQL.

## LeoCloud

Dieses Projekt ist fuer ein LeoCloud-Deployment vorbereitet.

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
