# Spec Delta

## Purpose

Diese Fähigkeit regelt, welche Spiele in der Bibliothek sichtbar sind und wer sie erstellen, veröffentlichen, bearbeiten oder löschen darf, ohne private Inhalte preiszugeben.

## ADDED Requirements

### Requirement: Sichtbarkeit eines Spiels
Jedes Spiel SHALL eine Sichtbarkeit `PRIVATE` oder `PUBLIC` besitzen. Ein neu erstelltes Spiel SHALL ohne ausdrücklich angegebene Sichtbarkeit `PRIVATE` sein. Nur die erstellende Lehrkraft SHALL die Sichtbarkeit ihres Spiels ändern können.

#### Scenario: Neues Spiel ohne Sichtbarkeitsangabe
- **WHEN** eine Lehrkraft ein Spiel ohne Sichtbarkeitsangabe erstellt
- **THEN** ist das Spiel privat und nur für diese Lehrkraft sichtbar

#### Scenario: Privates Spiel in der Spieleliste
- **WHEN** die erstellende Lehrkraft ihr privates Spiel in der Spieleliste sieht
- **THEN** ist es als privat gekennzeichnet und kann dort nicht zum Hosten ausgewählt werden

#### Scenario: Veröffentlichung durch den Ersteller
- **WHEN** die erstellende Lehrkraft die Sichtbarkeit ihres Spiels auf `PUBLIC` setzt
- **THEN** erscheint das Spiel in öffentlichen Listen und ist öffentlich abrufbar

#### Scenario: Ungültige Sichtbarkeit
- **WHEN** ein Erstellungs- oder Änderungsauftrag einen anderen Sichtbarkeitswert enthält
- **THEN** wird der Auftrag zurückgewiesen und das Spiel bleibt unverändert

### Requirement: Sichtbare Spiele über REST
Öffentliche Spiele SHALL über die bestehenden REST-Listen und Detailabrufe sichtbar bleiben. Eine authentifizierte, aktive Lehrkraft SHALL dort zusätzlich ihre eigenen privaten Spiele sehen. Private Spiele anderer Nutzer SHALL in Listen fehlen und bei direktem Abruf als nicht vorhanden behandelt werden. Ohne gültige Identität SHALL ausschließlich öffentliche Spiele sichtbar sein.

#### Scenario: Öffentliche Spiele ohne Anmeldung
- **WHEN** ein nicht angemeldeter Client die Spieleliste oder ein öffentliches Spiel abruft
- **THEN** erhält er nur öffentliche Spiele beziehungsweise das angeforderte öffentliche Spiel

#### Scenario: Eigene private Spiele
- **WHEN** eine aktive Lehrkraft eine Spieleliste oder ihr privates Spiel über REST abruft
- **THEN** sind ihre privaten Spiele zusätzlich zu öffentlichen Spielen sichtbar

#### Scenario: Fremdes privates Spiel
- **WHEN** ein Schüler oder eine andere Lehrkraft ein fremdes privates Spiel direkt abruft
- **THEN** erhält der Client keine Spieldaten und eine Nicht-gefunden-Antwort

#### Scenario: Gruppen- und Typfilter
- **WHEN** ein Client die gruppierte Liste oder die Liste eines Spieltyps abruft
- **THEN** gelten dieselben Sichtbarkeitsregeln wie für die allgemeine Spieleliste

### Requirement: Öffentlicher WebSocket-Katalog
Die bestehenden WebSocket-Befehle zur Auflistung und zum Abruf von Spielen SHALL ausschließlich öffentliche Spiele liefern. Ein privates Spiel SHALL auch bei direktem Abruf per Spiel-ID nicht offengelegt werden.

#### Scenario: WebSocket-Liste
- **WHEN** ein Client `get_games` oder `get_games_by_type` sendet
- **THEN** enthält die Antwort ausschließlich öffentliche Spiele

#### Scenario: Private Spiel-ID über WebSocket
- **WHEN** ein Client `get_game` mit der ID eines privaten Spiels sendet
- **THEN** erhält er keine Spieldaten und dieselbe Fehlermeldung wie für eine unbekannte ID

### Requirement: Lehrkraft und Eigentümerschaft
Nur eine aktive Lehrkraft SHALL ein Spiel erstellen können. Das System SHALL den Ersteller aus der geprüften Identität setzen und eine vom Client übermittelte Eigentümerangabe ignorieren. Ausschließlich dieser gespeicherte Ersteller SHALL das Spiel ändern oder löschen können. Die Rolle `ADMIN` SHALL ohne eigene fachliche Berechtigung keine Ausnahme von diesen Regeln erhalten.

#### Scenario: Lehrkraft erstellt Spiel
- **WHEN** eine aktive Lehrkraft ein Spiel erstellt
- **THEN** wird sie als Ersteller gespeichert, unabhängig von einer Eigentümerangabe im Auftrag

#### Scenario: Schüler oder Administrator erstellt Spiel
- **WHEN** ein Schüler oder ein Administrator ohne Lehrkraftrolle ein Spiel zu erstellen versucht
- **THEN** wird der Auftrag zurückgewiesen

#### Scenario: Andere Lehrkraft ändert oder löscht Spiel
- **WHEN** eine Lehrkraft ein fremdes öffentliches Spiel ändern oder löschen will
- **THEN** wird der Auftrag zurückgewiesen und das Spiel bleibt unverändert

#### Scenario: Ersteller ändert eigenes Spiel
- **WHEN** die aktive erstellende Lehrkraft ihr eigenes Spiel ändert oder löscht
- **THEN** wird die Aktion ausgeführt

#### Scenario: Inaktiver Ersteller
- **WHEN** ein inaktiver Ersteller sein Spiel ändern oder löschen will
- **THEN** wird die Aktion zurückgewiesen

### Requirement: Bestehende Spiele bei Umstellung
Bereits gespeicherte Spiele SHALL bei Einführung der Sichtbarkeit weiterhin öffentlich lesbar sein. Ein Spiel ohne zuordenbaren Ersteller SHALL nicht automatisch einem beliebigen Nutzer gehören und SHALL bis zu einer ausdrücklich geklärten Zuordnung nicht bearbeitet oder gelöscht werden können.

#### Scenario: Bestehendes Spiel ohne Sichtbarkeitswert
- **WHEN** ein vor der Umstellung gespeichertes Spiel gelesen wird
- **THEN** ist es als öffentliches Spiel sichtbar

#### Scenario: Bestehendes Spiel ohne Ersteller
- **WHEN** ein Nutzer ein bestehendes Spiel ohne gespeicherten Ersteller ändern oder löschen will
- **THEN** wird die Aktion zurückgewiesen und das Spiel bleibt erhalten
