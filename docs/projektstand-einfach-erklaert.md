# Projektstand einfach erklaert

Diese Datei fasst zwei wichtige technische Fragen zum aktuellen Projektstand
kurz und logisch zusammen.

## 1. Wie funktioniert der Login mit Schul-Keycloak?

Unsere Anwendung speichert keine Schulpasswoerter und prueft den Login auch
nicht selbst. Dafuer verwenden wir den Schul-Keycloak der HTL Leonding.

Der Ablauf ist:

1. Der Benutzer oeffnet unsere Seite.
2. Unsere Seite leitet ihn zur HTL-Loginseite weiter.
3. Dort meldet er sich mit seinem Schulaccount an.
4. Bei erfolgreichem Login leitet Keycloak den Browser wieder zu unserer App
   zurueck.
5. Der Browser bekommt dabei einen Token.
6. Der Browser schickt diesen Token bei API-Anfragen an unser Backend.
7. Das Backend prueft, ob der Token echt, gueltig und nicht abgelaufen ist.
8. Wenn alles passt, liest das Backend die Userdaten aus dem Token.
9. Danach wird in unserer PostgreSQL-Datenbank ein eigener App-User gesucht
   oder neu angelegt.

Der Token ist nicht einfach nur eine Zahl. Normalerweise ist er ein JWT. Das ist
ein signierter Text mit Informationen ueber den angemeldeten Benutzer.

Typische Informationen im Token sind:

```text
sub                -> eindeutige technische Keycloak-User-ID
preferred_username -> Schulusername, z. B. it220269
name               -> Anzeigename, z. B. David Berghahn
distinguishedName  -> LDAP-/Schulstruktur, eventuell mit Klasse
roles/groups       -> Rollen oder Gruppen
```

Wichtig ist: Der Token ist zwar lesbar, aber wir vertrauen ihm nur, wenn das
Backend ihn erfolgreich gegen Keycloak validieren kann.

## Warum legen wir trotzdem eigene User in PostgreSQL an?

Keycloak beantwortet nur die Frage:

```text
Wer ist eingeloggt?
```

Unsere Anwendung braucht aber eigene fachliche Daten, zum Beispiel:

- Spielfortschritt
- Punkte
- geloeste Aufgaben
- KI-Ergebnisse
- App-Rollen
- Einstellungen oder Profilinformationen

Darum gibt es in unserer Datenbank eine eigene User-Tabelle. Dort wird der
Schuluser mit einem internen App-User verbunden.

Beispiel:

```text
externalSubject: Keycloak-ID
username: it220269
displayName: David Berghahn
schoolClass: 5BHITM
role: STUDENT
active: true
```

Das Passwort liegt dabei nie in unserer Datenbank. Der Login bleibt bei
Keycloak, unsere App speichert nur die Daten, die sie fuer ihren eigenen
Fachbereich braucht.

## 2. Wie kommt die Seite in die LeoCloud?

Die LeoCloud ist ein Kubernetes-Cluster. Das bedeutet: Wir starten dort nicht
einfach direkt unseren Quellcode, sondern einen Container.

Der Ablauf ist:

1. Unser normales Projekt liegt in GitHub.
2. GitHub Actions baut aus dem Projekt ein Docker-Image.
3. Dieses Docker-Image wird in GHCR gespeichert.
4. In den Kubernetes-YAML-Dateien steht, welches Image gestartet werden soll.
5. LeoCloud/Kubernetes zieht dieses Image aus GHCR.
6. Kubernetes startet daraus einen Pod bzw. Container.
7. Der Container laeuft dauerhaft im Cluster.
8. Ein Ingress macht die Anwendung ueber eine URL erreichbar.

Wichtig: GHCR baut das Image nicht selbst. GHCR ist die Registry, also der
Speicherort fuer das fertige Docker-Image.

Die Kette sieht so aus:

```text
Code
-> GitHub
-> GitHub Actions baut Docker-Image
-> GHCR speichert Docker-Image
-> Kubernetes/LeoCloud zieht Docker-Image
-> Container laeuft dauerhaft
-> Ingress macht die URL erreichbar
```

Unser Image heisst aktuell:

```text
ghcr.io/davidberghahn/diplomarbeit-gamebased-learning:latest
```

Unsere LeoCloud-URL ist:

```text
https://it220269.cloud.htl-leonding.ac.at/
```

## Welche Dateien braucht LeoCloud?

LeoCloud bekommt nicht den Quellcode direkt, sondern Kubernetes-Dateien. Diese
liegen bei uns im Ordner `k8s`.

Die wichtigsten Dateien sind:

```text
k8s/postgres.yaml
k8s/backend.yaml
k8s/backend-ingress.yaml
```

Ihre Aufgaben:

```text
postgres.yaml        -> startet PostgreSQL im Cluster
backend.yaml         -> startet unser Quarkus-Backend mit dem Docker-Image
backend-ingress.yaml -> macht die Anwendung ueber die LeoCloud-URL erreichbar
```

## Was passiert bei einem neuen Stand?

Wenn wir Code aendern, passiert der neue Stand nicht automatisch in der
LeoCloud. Der typische Ablauf ist:

1. Code committen und nach GitHub pushen.
2. GitHub Actions baut ein neues Docker-Image.
3. Das neue Image wird nach GHCR gepusht.
4. Danach wird das Deployment in LeoCloud neu gestartet.
5. Kubernetes zieht dann das neue `latest`-Image und startet den neuen Stand.

Typische Befehle:

```bash
git push
kubectl rollout restart deployment/gamebased-learning-api -n student-it220269
kubectl rollout status deployment/gamebased-learning-api -n student-it220269
```

Danach sollte die App weiterhin unter der LeoCloud-URL erreichbar sein.

## Aktueller Stand

Aktuell ist vorbereitet:

- Backend mit Quarkus
- Schul-Keycloak-Login
- interne Userverwaltung in PostgreSQL
- Dockerfile
- GitHub Actions Build fuer das Docker-Image
- GHCR als Image Registry
- Kubernetes-Dateien fuer LeoCloud
- LeoCloud-Deployment unter `student-it220269`

Noch offen ist die endgueltige Klaerung, warum der Keycloak-Login lokal
vollstaendig funktioniert, die LeoCloud-Version aber vom Backend-Pod aus
Keycloak nicht sauber erreichen kann. Die App selbst ist deployed, der offene
Punkt betrifft die Verbindung vom LeoCloud-Backend zum Schul-Keycloak.
