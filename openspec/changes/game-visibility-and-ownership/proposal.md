# Proposal

## Why

Der fachliche Datenmodell-Entwurf unterscheidet öffentliche und private Spiele. Die aktuelle Implementierung kennt noch keine Sichtbarkeit: REST und WebSocket liefern sämtliche Spiele, und jede Lehrkraft kann fremde Spiele ändern oder löschen. David hat inzwischen bestätigt, dass öffentliche Spiele nur nach Anmeldung lesbar sind, der Ersteller sein privates Spiel hosten darf und Administratoren im Bereich der Spielrechte auch fremde Spiele verwalten dürfen. Vor weiteren Bibliotheks- und Spielfunktionen braucht es dafür eine verlässliche Zugriffsgrenze.

## What Changes

- Spiele erhalten eine Sichtbarkeit `PRIVATE` oder `PUBLIC`; neue Spiele sind ohne ausdrückliche Veröffentlichung privat.
- REST-Lesezugriffe setzen eine gültige Anmeldung voraus. Angemeldete Nutzer sehen öffentliche Spiele, erstellende Lehrkräfte zusätzlich ihre eigenen privaten Spiele und Administratoren alle Spiele. Private Spiele anderer Nutzer fehlen in Listen und Detailabrufen. Die bisherigen WebSocket-Katalogbefehle ohne Authentifizierung dürfen keine Spieldaten mehr liefern.
- Lehrkräfte dürfen Spiele erstellen und nur eigene Spiele veröffentlichen, bearbeiten oder löschen. Administratoren haben im Bereich der Spielrechte eine ausdrückliche Ausnahme: Sie dürfen auch fremde und private Spiele lesen, erstellen, bearbeiten, veröffentlichen und löschen.
- Die erstellende Lehrkraft darf ein eigenes privates Spiel alleine und mit Schülern hosten; Administratoren dürfen Spiele ebenfalls hosten. Ein privates Spiel wird dadurch nicht zum allgemein sichtbaren Katalogeintrag. Die sichere technische Umsetzung des Schüler-Beitritts zu privaten Spielrunden gehört zu einem gesonderten Session-Change.
- **BREAKING**: Anonyme REST- und WebSocket-Katalogzugriffe liefern keine Spieldaten mehr. Änderungen an fremden Spielen durch Lehrkräfte werden zurückgewiesen; die bisherige Administrator-Berechtigung wird fachlich ausdrücklich bestätigt.
- Bestehende Spiele bleiben bei der Umstellung öffentlich. Spiele ohne zuordenbaren Ersteller werden nicht automatisch einer Lehrkraft zugeschrieben; bis zur geklärten Zuordnung können nur Administratoren sie bearbeiten oder löschen.

## Capabilities

### New Capabilities

- `game-access`: Sichtbarkeit, Eigentümerschaft und rollenabhängige Lese- und Schreibrechte für Spiele.

### Modified Capabilities

Keine; bisher gibt es keine OpenSpec-Hauptspezifikation.

## Impact

Betroffen sind die Spielentität und Datenbankumstellung, `GameRepository`, `GameResource`, `GameSocket`, die Spieleliste und der Lobby-Detailabruf im Angular-Frontend sowie Autorisierungs- und Integrationstests. REST verwendet die bestehende Schul-Keycloak-Identität; eine neue WebSocket-Authentifizierung ist nicht Teil dieses Changes. Kopieren, Spielfamilien, persistente Sessions, der Schüler-Beitritt zu privaten Runden und eine neue Bibliotheksoberfläche bleiben eigene Folgearbeiten. Die Entscheidung zum privaten Hosting ist hier als Berechtigungsregel festgehalten, nicht als bereits vollständig umgesetzte Session-Funktion.
