# Proposal

## Why

Der fachliche Datenmodell-Entwurf unterscheidet öffentliche und private Spiele und erlaubt Änderungen nur durch die erstellende Lehrkraft. Die aktuelle Implementierung kennt noch keine Sichtbarkeit: REST und WebSocket liefern sämtliche Spiele, und jede Lehrkraft kann fremde Spiele ändern oder löschen. Vor weiteren Bibliotheks- und Spielfunktionen braucht es dafür eine verlässliche Zugriffsgrenze.

## What Changes

- Spiele erhalten eine Sichtbarkeit `PRIVATE` oder `PUBLIC`; neue Spiele sind ohne ausdrückliche Veröffentlichung privat.
- REST-Lesezugriffe liefern öffentliche Spiele sowie bei authentifizierten Lehrkräften die eigenen privaten Spiele. Eigene private Spiele sind in der Liste als nicht hostbar erkennbar. Der bestehende WebSocket ohne Nutzeranmeldung liefert ausschließlich öffentliche Spiele; private Spiele sind darüber nicht abrufbar.
- Erstellen und Ändern der Sichtbarkeit bleiben Lehrkräften vorbehalten; Bearbeiten und Löschen sind nur für den gespeicherten Ersteller möglich. Die technische Rolle `ADMIN` erhält ohne Fachentscheidung keine Sonderrechte.
- **BREAKING**: Bisher mögliche Änderungen an fremden Spielen sowie Administrator-Änderungen werden zurückgewiesen. Private Spiele verschwinden aus fremden Listen und Detailabrufen.
- Bestehende Spiele bleiben bei der Umstellung sichtbar; Spiele ohne zuordenbaren Ersteller werden nicht automatisch einer Lehrkraft zugeschrieben und bleiben bis zur geklärten Zuordnung schreibgeschützt.

## Capabilities

### New Capabilities

- `game-access`: Sichtbarkeit, Eigentümerschaft und rollenabhängige Lese- und Schreibrechte für Spiele.

### Modified Capabilities

Keine; bisher gibt es keine OpenSpec-Hauptspezifikation.

## Impact

Betroffen sind die Spielentität und Datenbankumstellung, `GameRepository`, `GameResource`, `GameSocket`, die Spieleliste im Angular-Frontend sowie Autorisierungs- und Integrationstests. Für private REST-Zugriffe wird die bestehende Schul-Keycloak-Identität verwendet; eine neue WebSocket-Authentifizierung ist nicht Teil dieses Changes. Kopieren, Spielfamilien, Sessions und eine neue Bibliotheksoberfläche sind ebenfalls nicht enthalten.
