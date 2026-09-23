# Spec Delta

## Purpose

Diese Fähigkeit regelt, welche Spiele in der Bibliothek sichtbar sind und wer sie erstellen, veröffentlichen, bearbeiten oder löschen darf, ohne private Inhalte preiszugeben.

## ADDED Requirements

### Requirement: Sichtbarkeit eines Spiels
Jedes Spiel SHALL eine Sichtbarkeit `PRIVATE` oder `PUBLIC` besitzen. Ein neu erstelltes Spiel SHALL ohne ausdrücklich angegebene Sichtbarkeit `PRIVATE` sein. Die erstellende Lehrkraft und ein Administrator SHALL die Sichtbarkeit ändern können.

#### Scenario: Neues Spiel ohne Sichtbarkeitsangabe
- **WHEN** eine Lehrkraft ein Spiel ohne Sichtbarkeitsangabe erstellt
- **THEN** ist das Spiel privat und nur für diese Lehrkraft sowie aktive Administratoren sichtbar

#### Scenario: Privates Spiel in der Spieleliste
- **WHEN** die erstellende Lehrkraft ihr privates Spiel in der Spieleliste sieht
- **THEN** ist es als privat gekennzeichnet und kann von ihr zum Hosten ausgewählt werden

#### Scenario: Veröffentlichung durch den Ersteller
- **WHEN** die erstellende Lehrkraft die Sichtbarkeit ihres Spiels auf `PUBLIC` setzt
- **THEN** erscheint das Spiel in öffentlichen Listen und ist öffentlich abrufbar

#### Scenario: Ungültige Sichtbarkeit
- **WHEN** ein Erstellungs- oder Änderungsauftrag einen anderen Sichtbarkeitswert enthält
- **THEN** wird der Auftrag zurückgewiesen und das Spiel bleibt unverändert

### Requirement: Sichtbare Spiele über REST
Öffentliche Spiele SHALL über REST-Listen und Detailabrufe nur angemeldeten, aktiven Nutzern sichtbar sein. Eine aktive Lehrkraft SHALL zusätzlich ihre eigenen privaten Spiele sehen. Ein aktiver Administrator SHALL alle öffentlichen und privaten Spiele sehen. Private Spiele anderer Schüler oder Lehrkräfte SHALL in Listen fehlen und bei direktem Abruf als nicht vorhanden behandelt werden. Ohne Anmeldung oder mit ungültigem Token SHALL keine Spieleinformation geliefert und ein Authentifizierungsfehler zurückgegeben werden. Ein angemeldetes, aber inaktives Konto SHALL keine Spieldaten erhalten.

#### Scenario: Öffentliche Spiele nach Anmeldung
- **WHEN** ein angemeldeter, aktiver Nutzer die Spieleliste oder ein öffentliches Spiel abruft
- **THEN** erhält er die öffentlichen Spiele beziehungsweise das angeforderte öffentliche Spiel

#### Scenario: Fehlende oder ungültige Anmeldung
- **WHEN** ein Client ohne Token oder mit ungültigem Token eine Spieleliste oder ein Spiel abruft
- **THEN** erhält er keine Spieldaten und einen Authentifizierungsfehler

#### Scenario: Inaktives Konto
- **WHEN** ein angemeldeter, aber inaktiver Nutzer eine Spieleliste oder ein Spiel abruft
- **THEN** erhält er keine Spieldaten und eine Autorisierungsablehnung

#### Scenario: Eigene private Spiele
- **WHEN** eine aktive Lehrkraft eine Spieleliste oder ihr privates Spiel über REST abruft
- **THEN** sind ihre privaten Spiele zusätzlich zu öffentlichen Spielen sichtbar

#### Scenario: Fremdes privates Spiel
- **WHEN** ein Schüler oder eine andere Lehrkraft ein fremdes privates Spiel direkt abruft
- **THEN** erhält der Client keine Spieldaten und eine Nicht-gefunden-Antwort

#### Scenario: Administrator liest private Spiele
- **WHEN** ein aktiver Administrator die Spieleliste oder ein fremdes privates Spiel abruft
- **THEN** sind auch diese privaten Spiele für ihn sichtbar

#### Scenario: Gruppen- und Typfilter
- **WHEN** ein Client die gruppierte Liste oder die Liste eines Spieltyps abruft
- **THEN** gelten dieselben Sichtbarkeitsregeln wie für die allgemeine Spieleliste

### Requirement: Bisheriger WebSocket-Katalog
Die bestehenden WebSocket-Befehle zur Auflistung und zum Abruf von Spielen SHALL ohne gültig geprüfte Anmeldung keinerlei Spieldaten liefern. Da dieser Change keine WebSocket-Authentifizierung einführt, SHALL der Spielekatalog über den authentifizierten REST-Leseweg verwendet werden. Ein direkter WebSocket-Abruf SHALL für jede Spiel-ID dieselbe ablehnende Antwort geben, unabhängig davon, ob das Spiel privat, öffentlich oder unbekannt ist.

#### Scenario: WebSocket-Liste
- **WHEN** ein Client `get_games` oder `get_games_by_type` sendet
- **THEN** enthält die Antwort keine Spieldaten und weist auf die erforderliche Anmeldung hin

#### Scenario: Spiel-ID über WebSocket
- **WHEN** ein Client `get_game` mit der ID eines privaten, öffentlichen oder unbekannten Spiels sendet
- **THEN** erhält er keine Spieldaten und jeweils dieselbe Fehlermeldung

### Requirement: Lehrkraft und Eigentümerschaft
Nur eine aktive Lehrkraft oder ein aktiver Administrator SHALL ein Spiel erstellen können. Das System SHALL den Ersteller aus der geprüften Identität setzen und eine vom Client übermittelte Eigentümerangabe ignorieren. Eine aktive Lehrkraft SHALL nur eigene Spiele ändern oder löschen können. Ein aktiver Administrator SHALL jedes Spiel unabhängig vom gespeicherten Ersteller ändern oder löschen können, auch wenn es privat ist oder keinen zuordenbaren Ersteller besitzt. Serverseitig verwaltete IDs und Eigentümerfelder SHALL bei Änderungen nicht aus Clientdaten übernommen werden.

#### Scenario: Lehrkraft erstellt Spiel
- **WHEN** eine aktive Lehrkraft ein Spiel erstellt
- **THEN** wird sie als Ersteller gespeichert, unabhängig von einer Eigentümerangabe im Auftrag

#### Scenario: Schüler erstellt Spiel
- **WHEN** ein Schüler ein Spiel zu erstellen versucht
- **THEN** wird der Auftrag zurückgewiesen

#### Scenario: Administrator erstellt Spiel
- **WHEN** ein aktiver Administrator ein Spiel erstellt
- **THEN** wird er als Ersteller gespeichert, unabhängig von einer Eigentümerangabe im Auftrag

#### Scenario: Andere Lehrkraft ändert oder löscht Spiel
- **WHEN** eine Lehrkraft ein fremdes öffentliches Spiel ändern oder löschen will
- **THEN** wird der Auftrag zurückgewiesen und das Spiel bleibt unverändert

#### Scenario: Ersteller ändert eigenes Spiel
- **WHEN** die aktive erstellende Lehrkraft ihr eigenes Spiel ändert oder löscht
- **THEN** wird die Aktion ausgeführt

#### Scenario: Administrator verwaltet fremdes Spiel
- **WHEN** ein aktiver Administrator ein fremdes oder besitzerloses Spiel ändert, veröffentlicht oder löscht
- **THEN** wird die Aktion ausgeführt, ohne den gespeicherten Ersteller stillschweigend zu ersetzen

#### Scenario: Inaktiver Ersteller
- **WHEN** ein inaktiver Ersteller sein Spiel ändern oder löschen will
- **THEN** wird die Aktion zurückgewiesen

### Requirement: Berechtigung zum privaten Hosting
Eine aktive Lehrkraft SHALL ihr eigenes privates Spiel für das Hosting laden dürfen; ein aktiver Administrator SHALL auch fremde private Spiele dafür laden dürfen. Diese Berechtigung SHALL unabhängig davon gelten, ob die Lehrkraft später allein oder mit Schülern spielt. Das Laden für das Hosting SHALL ein privates Spiel nicht in öffentlichen oder fremden Katalogabfragen sichtbar machen. Die tatsächliche Session, der Schüler-Beitritt und der zeitlich begrenzte Zugriff auf private Spielinhalte sind Teil eines eigenen Folge-Changes.

#### Scenario: Ersteller öffnet private Lobby
- **WHEN** die aktive erstellende Lehrkraft für ihr privates Spiel eine Lobby vorbereitet
- **THEN** darf sie den Spielinhalt über einen authentifizierten, auf Eigentum geprüften Leseweg laden

#### Scenario: Fremde Lehrkraft öffnet private Lobby
- **WHEN** eine andere Lehrkraft die private Lobby-Detailansicht über die Spiel-ID aufruft
- **THEN** erhält sie keine Spieldaten

### Requirement: Bestehende Spiele bei Umstellung
Bereits gespeicherte Spiele SHALL bei Einführung der Sichtbarkeit für angemeldete Nutzer weiterhin öffentlich lesbar sein. Ein Spiel ohne zuordenbaren Ersteller SHALL nicht automatisch einem beliebigen Nutzer gehören. Nur ein aktiver Administrator SHALL ein solches Spiel bis zu einer ausdrücklich geklärten Zuordnung bearbeiten oder löschen können.

#### Scenario: Bestehendes Spiel ohne Sichtbarkeitswert
- **WHEN** ein vor der Umstellung gespeichertes Spiel gelesen wird
- **THEN** ist es als öffentliches Spiel sichtbar

#### Scenario: Bestehendes Spiel ohne Ersteller
- **WHEN** eine Lehrkraft ohne Administratorrolle ein bestehendes Spiel ohne gespeicherten Ersteller ändern oder löschen will
- **THEN** wird die Aktion zurückgewiesen und das Spiel bleibt erhalten
