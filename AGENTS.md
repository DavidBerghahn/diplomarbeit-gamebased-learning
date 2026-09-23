# Projektregeln: Diplomarbeit Game-based Learning

Diese Hinweise ergänzen Davids globale Codex-Regeln. Sie gelten für das Repository und seine Worktrees; ein Chatverlauf allein ist keine verlässliche Projektübergabe.

## Einstieg und Quellen

- Prüfe vor Änderungen `git status --short --branch`, den aktuellen Commit und die betroffenen Dateien. Teamänderungen in anderen Worktrees oder auf `origin/main` nicht überschreiben.
- Wenn du die Arbeit nach einem Chatwechsel fortsetzt, lies [PROJECT_HANDOFF.md](PROJECT_HANDOFF.md) und vergleiche dessen datierte Aussagen mit Git und Code. Die Übergabe ist keine automatische Bestätigung des heutigen Live-Zustands.
- Für fachliche Regeln zu Spielen, Fragen, Sessions und Fortschritt nutze [docs/datenmodell-v1.md](docs/datenmodell-v1.md). Es ist ein Entwurf, keine bereits implementierte Datenbankstruktur.
- Für den aktuellen Change zu Spielsichtbarkeit und Eigentümerschaft nutze [openspec/changes/game-visibility-and-ownership/](openspec/changes/game-visibility-and-ownership/). Seine Planung ist noch keine Implementierung.

## Struktur und Arbeitsweise

- Backend: Java/Quarkus unter `src/main/java/`; Tests unter `src/test/java/`. Frontend: Angular unter `Website/Frontend/`. Deployment: `.github/workflows/` und `k8s/`.
- Bei größeren Änderungen OpenSpec gezielt verwenden: `$openspec-explore` → `$openspec-propose` → nach Freigabe `$openspec-apply-change` → `$openspec-verify-change` → `$openspec-archive-change`. Ein geplanter Change darf nicht als erledigte Funktion beschrieben werden.
- Prüfe passende Backend-Änderungen mit `./mvnw test`; im Frontend mit `npm test -- --watch=false` und `npm run build` aus `Website/Frontend/`. Dokumentiere, welche Prüfungen wirklich gelaufen sind.
- Schul-Keycloak ist die Quelle der Identität. Rollen- und Besitzrechte serverseitig prüfen; keine vollständigen Tokens, Passwörter oder personenbezogenen Claims in Git oder Dokumentation übernehmen.
- `git push`, LeoCloud-Rollout und Änderungen an produktiven Daten nur im Rahmen eines entsprechenden Auftrags durchführen. Lokaler Commit, CI-Build und erfolgreicher Live-Rollout sind verschiedene Zustände.
