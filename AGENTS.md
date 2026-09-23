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

## Zusammenarbeit der Codex-Aufgaben

- David erteilt den Auftrag im Supervisor-Chat. Für größere, sinnvoll teilbare Arbeiten beauftragt der Supervisor die **bereits vorhandene, separat sichtbare Codex-Aufgabe** der passenden Rolle (Researcher, Coder, Writer oder Reviewer) und führt deren Ergebnis im Supervisor-Chat zusammen. Ein intern gestarteter Unteragent ist **nicht** dieser Rollen-Chat und erfüllt diese Anforderung nicht.
- Vor der Beauftragung die vorhandenen Aufgaben nach Titel und Aufgaben-ID ermitteln. Den passenden Rollen-Chat mit einer konkreten Nachricht beauftragen und dessen Ergebnis abwarten; nicht allein wegen eines Rollennamens oder eines vorhandenen Worktrees annehmen, dass der Chat existiert oder erreichbar ist. Keine neuen nutzereigenen Aufgaben ohne ausdrücklichen Auftrag anlegen.
- Jede Beauftragung enthält Ziel, abgegrenzte Zuständigkeit, relevanten OpenSpec-Change beziehungsweise Dateien, Abnahmekriterien, Freigabegrenzen und den erwarteten Rückbericht. Den beauftragten Aufgaben-Titel und die tatsächliche Antwort im Supervisor-Ergebnis nennen. Behaupte eine Delegation an einen Rollen-Chat nur, wenn genau dort eine Nachricht angekommen ist und ein Ergebnis zurückkam.
- Die Rollen-Chats können in getrennten Worktrees auf unterschiedlichen Commits arbeiten. Vor dem Auftrag Git-Stand und benötigte Änderungen abgleichen; den Ziel-Commit ausdrücklich angeben. Uncommittete Änderungen eines anderen Worktrees sind nicht automatisch sichtbar. Integration und Review erfolgen erst auf einem eindeutig bezeichneten Stand; kein implizites Pushen, Mergen oder Deployen.
- Kurze Fragen und kleine Änderungen darf der Supervisor selbst erledigen. Fehlt für eine größere Aufgabe der passende sichtbare Rollen-Chat oder ist er nicht erreichbar, die Grenze offenlegen und David um Entscheidung bitten, statt unbemerkt einen internen Unteragenten als Ersatz zu starten.

### Verbindlicher Ablauf zwischen den Chats

1. Der Supervisor prüft Auftrag, Git-Stand und vorhandenen OpenSpec-Change. Er zerlegt nur unabhängig bearbeitbare Teile und weist jeder betroffenen Datei beziehungsweise jedem Artefakt genau einen verantwortlichen Chat zu. Abhängige Arbeit folgt nacheinander: erst Recherche oder Planung, dann Umsetzung, dann Review.
2. Der Supervisor sendet den Teilauftrag an die **bestehende sichtbare Aufgabe** der passenden Rolle und wartet auf deren Antwort. Spezialisten starten weder eigene Unteragenten noch beauftragen sie andere Rollen-Chats; Rückfragen, Blockaden und Änderungsbedarf gehen an den Supervisor. Der Supervisor entscheidet über Umplanung und informiert David über wesentliche Abweichungen.
3. Researcher liefert belegte Analyse ohne ungefragte Projektänderungen. Coder ändert nur zugewiesene Bereiche und nennt Tests sowie Commit oder Diff. Writer pflegt zugewiesene Texte oder Planungsartefakte, ohne technische Entscheidungen eigenmächtig zu ändern. Reviewer prüft einen vom Supervisor festgelegten, danach unveränderten Stand unabhängig und verändert keine Dateien.
4. Der Supervisor gleicht die Ergebnisse mit dem beauftragten Ziel ab, integriert Änderungen erst nach Prüfung des jeweiligen Git-Stands und lässt wesentliche Ergebnisse reviewen. Konkrete Review-Mängel gehen an den zuständigen Bearbeiter zurück; höchstens zwei Korrekturrunden. In seiner Antwort an David trennt er Umsetzung, tatsächliche Prüfung und offene Punkte.

### OpenSpec-Verantwortung

- Der Supervisor ist für die Aktualität von OpenSpec verantwortlich. Vor einem größeren Change prüft er Proposal, Design, Specs und Aufgabenliste gegen die Anforderungen; nach der Umsetzung vergleicht er sie mit Code und Tests. Nötige Anpassungen weist er als eigene, abgegrenzte Aufgabe einem passenden bestehenden Spezialisten-Chat zu und prüft dessen Ergebnis.
- Änderungen an OpenSpec und Implementierung müssen demselben fachlichen Beschluss entsprechen. Aufgaben erst nach nachgewiesener Umsetzung als erledigt markieren; vor dem Archivieren den Change verifizieren und relevante Specs synchronisieren. Ein Chat-Bericht allein ist kein Nachweis für fertigen Code, bestandene Tests oder einen abgeschlossenen Change.
