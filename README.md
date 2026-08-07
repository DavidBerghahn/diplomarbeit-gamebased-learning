# diplomarbeit-gamebased-learning

KI-gestuetzte Spieleplattform fuer Game-based Learning.

## Backend

Das Backend ist als Quarkus-Anwendung vorbereitet. Der aktuelle Login ist ein
Mock-Login, damit die Spiel- und KI-Module weiterentwickelt werden koennen,
bevor die echte Schuluser-/Keycloak-Anbindung feststeht.

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

Mock-Login per Header:

```bash
curl http://localhost:8080/api/auth/me \
  -H "X-Mock-User: it220269" \
  -H "X-Mock-Name: David Berghahn" \
  -H "X-Mock-Class: 5BHITM" \
  -H "X-Mock-Role: STUDENT"
```

Die spaetere Keycloak-Anbindung soll nur den `AuthProvider` ersetzen. Die
fachliche Datenbank fuer Userprofile, Fortschritt, Spiele und KI-Daten bleibt
dadurch stabil. Siehe `docs/auth-architecture.md`.

Lokal testen:

```bash
./mvnw test
./mvnw quarkus:dev
```

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
