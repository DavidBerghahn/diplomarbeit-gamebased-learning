# Codex-Einrichtung für Projektmitglieder

Stand: 23. September 2026

Diese Anleitung ist für andere Mitglieder der Diplomarbeitsgruppe gedacht. Sie
beschreibt, wie ihr im **eigenen Codex-Konto** sichtbare Projekt-Chats mit
gemeinsamen Regeln einrichtet. Eine Markdown-Datei kann keine Chats automatisch
anlegen, keine Berechtigungen vergeben und keine persönliche Codex-Konfiguration
von David auf ein anderes Konto übertragen. Die Einrichtung beginnt erst, wenn
das jeweilige Teammitglied den untenstehenden Auftrag selbst an Codex sendet.

## Was das Repository bereits mitbringt

- [`AGENTS.md`](../AGENTS.md): gemeinsame Projekt- und Git-Regeln. Der dortige
  Supervisor-Ablauf gilt für die **eigenen** Chats jedes Teammitglieds.
- [`PROJECT_HANDOFF.md`](../PROJECT_HANDOFF.md): datierter Projektstand; Angaben
  zu Commit, Implementierung und Deployment vor der Arbeit erneut prüfen.
- [`datenmodell-entscheidungen.md`](datenmodell-entscheidungen.md) und
  [`datenmodell-v1.md`](datenmodell-v1.md): bestätigte Antworten und fachlicher
  Entwurf. Der Entwurf ist nicht automatisch implementiert.
- [`../openspec/`](../openspec/): größere Änderungen als planbare Changes.
  Die projektlokalen OpenSpec-Skills liegen unter `../.agents/skills/`.

**Nicht geteilt** werden Davids persönliche `MEMORY.md`, seine lokalen
Rollendateien in seinem Benutzerverzeichnis, seine Codex-Chats, deren Historie,
Worktrees, Anmeldungen und lokale, noch nicht veröffentlichte Commits. Prüft
deshalb zuerst, welcher Git-Commit auf eurem Rechner und auf dem Remote liegt.
Ein vorhandener Worktree ist noch kein sichtbarer Chat.

## Einmaliger Einrichtungsauftrag

Öffnet das Repository in Codex und sendet **selbst** diesen Auftrag in einem
eigenen Projekt-Chat. Passt Rollen und Anzahl an euren tatsächlichen Bedarf an;
fünf dauerhafte Chats sind kein Muss für kleine Aufgaben.

> Ich arbeite als Mitglied des Diplomarbeitsteams an diesem Repository. Lies
> zuerst `AGENTS.md`, `docs/codex-team-setup.md`, `PROJECT_HANDOFF.md` und den
> aktuellen Git-Stand. Prüfe, welche sichtbaren Codex-Aufgaben und Worktrees in
> **meinem** Konto für dieses Projekt bereits existieren, und vermeide
> Duplikate. Richte für mich, soweit die Codex-Oberfläche es ermöglicht,
> separat sichtbare Projekt-Chats für einen Supervisor und die aktuell
> benötigten Spezialisten Researcher, Coder, Writer und Reviewer ein. Jeder
> neue Chat soll das gleiche Repository verwenden und seine Rolle aus
> `AGENTS.md` sowie dieser Anleitung kennen. Für parallele Codeänderungen
> verwende getrennte Git-Worktrees und nenne den Ausgangs-Commit. Prüfe nach
> dem Erstellen für jeden Chat Titel, Aufgaben-ID, Projektpfad, Git-Stand und
> Erreichbarkeit. Ein interner Unteragent zählt **nicht** als separater Chat.
> Gib mir die tatsächlich eingerichteten Chats und verbleibende Hindernisse
> zurück. Ändere keinen Anwendungscode; nicht pushen oder deployen.

Der Startauftrag jedes erzeugten Rollen-Chats soll seine Rolle ausdrücklich
nennen: „Lies `AGENTS.md` und `docs/codex-team-setup.md`, prüfe deinen
Projektpfad und Git-Stand, arbeite nur an abgegrenzten Aufträgen des
Supervisors und beginne jetzt keine Facharbeit.“ Der Supervisor erhält
stattdessen den Koordinationsauftrag aus `AGENTS.md`. Das bloße Benennen eines
Chats als „Researcher“ oder „Reviewer“ weist ihm noch keine Aufgabe zu.

Falls Codex die Aufgabe nicht selbst anlegen oder erreichen kann, soll es das
konkret melden, statt die Existenz zu behaupten. Ihr könnt die Chats dann in
der App selbst erstellen und mit denselben Rollenhinweisen beginnen.

## Zusammenarbeit nach der Einrichtung

1. Gebt eure Projektaufträge dem **eigenen Supervisor-Chat**. Kleine, eng
   zusammenhängende Aufgaben kann er selbst erledigen.
2. Bei größeren Arbeiten beauftragt er nur die passenden **bestehenden,
   sichtbaren** Spezialisten-Chats. Researcher untersucht, Coder implementiert,
   Writer pflegt zugewiesene Texte und Planungsartefakte, Reviewer prüft einen
   festen Stand unabhängig. Spezialisten beauftragen einander nicht selbst.
3. Jede Beauftragung nennt Ziel, Dateien, Ausgangs-Commit, Abnahmekriterien
   und Freigabegrenzen. Ergebnisse werden mit Commit oder Diff und tatsächlich
   ausgeführten Prüfungen an den Supervisor zurückgemeldet. Änderungen aus
   verschiedenen Worktrees müssen bewusst integriert werden.
4. Der Supervisor hält OpenSpec zum beschlossenen Projektstand passend,
   koordiniert Review und meldet euch getrennt, was geplant, implementiert,
   getestet und veröffentlicht ist. Ein lokaler Commit ist weder ein Push
   noch ein LeoCloud-Rollout.

Das sind **persönliche Arbeits-Chats**, keine gemeinsamen Team-Chats. Für die
Zusammenarbeit zwischen Menschen bleiben das geteilte Git-Repository, die
Projektdateien und überprüfbare Commits beziehungsweise Pull Requests die
gemeinsame Grundlage. Stimmt gleichzeitige Änderungen an denselben Dateien
vorher im Team ab.

## Kurzer Funktionstest

Gebt dem Supervisor nach der Einrichtung einen rein lesenden Auftrag, etwa:

> Lass den bestehenden Researcher-Chat den aktuellen Code mit einem konkreten
> OpenSpec-Change vergleichen. Nenne den beauftragten Chat, seinen Git-Stand,
> zwei belegte Befunde und ob seine Antwort tatsächlich angekommen ist. Keine
> Dateien ändern.

Wenn nur ein intern gestarteter Unteragent erscheint oder kein Ergebnis im
sichtbaren Researcher-Chat liegt, ist die Chat-übergreifende Zusammenarbeit
noch nicht nachgewiesen.

Diese Anleitung wird für andere Rechner erst verfügbar, nachdem der
entsprechende Git-Commit im gemeinsam verwendeten Remote-Branch angekommen
und dort ausgecheckt wurde. Das ist ein eigener, zu prüfender Schritt; aus
dieser Datei folgt keine Erlaubnis zum Pushen.
