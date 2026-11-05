# Proposal

## Accepted scope update

Der Nutzerauftrag vom 4. November 2026 erweitert diesen Change: Host-Mitspielen ist für alle Rollen per Toggle erlaubt, einschließlich Lehrer; ausschließlich beobachtende Hosts erhalten weiterhin keine Punkte. Diese ausdrückliche Freigabe ersetzt die frühere Lehrer-Sperre und den generellen Host-Ausschluss in den historischen Abschnitten unten. Zusätzlich gehören Raumlaufzeit, Teamverwaltung, Just-One-Multiplayer, CSV-Import, die tatsächlich angebundene Spielerstellung und Klassenstatistik zum Auftrag. Die Umsetzung erfolgt im Supervisor-Chat auf ausdrücklichen Wunsch des Nutzers. Commit und GitHub-Push sind beauftragt, LeoCloud-Rollout und Cloud-Datenänderungen nicht.

## Why

Ergänzung vom 5. November 2026: Die Klassenstatistik erhält einen Klassenfilter, Schülerpunkte und die Summe dieser Punkte pro Klasse, einschließlich Schülern mit Adminrechten. Die Klasse stammt aus dem Schulkonto; Lehrer/Admins können fehlende Zuordnungen dauerhaft ergänzen. Ein Token ohne Klassenclaim darf eine vorhandene Zuordnung nicht löschen. Der Nutzer hat anschließend Commit und GitHub-Push des gesamten lokalen Stands ausdrücklich freigegeben; ein Cloud-Rollout bleibt ausgeschlossen.

Smart10 spielt Solo und Multiplayer derzeit überwiegend mit clientseitigen Lösungen und flüchtigem Rundenzustand; ein angezeigtes Ende erzeugt kein verlässlich zugeordnetes, dauerhaftes Ergebnis. Die lokale Datenmodell-Foundation bietet versionierte Spiele und einen Dienst für abgeschlossene Runden, benötigt für Smart10 aber noch einzelne versionierte Karten, geprüfte Identitäten und eine sichere Anbindung der Spielabläufe.

## What Changes

- Jede Smart10-Gruppenfrage erhält genau zwölf versionierte Einzelkarten mit Modus und typisierter Lösung. Im Modus `ORDERING` bilden die Lösungsränge exakt die Permutation 1–12. Antworten können auf die konkrete Karte verweisen; bisherige Fragen und Ergebnisse bleiben lesbar.
- Solo und Multiplayer starten aus einer gespeicherten Spielversion. Der Server bewertet Antworten und berechnet die Endwerte aus dem gepinnten Stand; Clientspiele, Clientlösungen und Clientscores sind keine Speicherquelle. Die lokale Demo bleibt als ungespeicherte Demo erkennbar.
- Die bestehende Schul-Keycloak-Identität wird für Smart10-Spielaktionen bis zur WebSocket-Verbindung durchgereicht. In Mehrspieler-Runden ist der Host kein aktiver Spieler; Teams enthalten nur echte Teilnehmer. Bis zur offenen fachlichen Entscheidung wird eine aktive Teilnahme mit Lehrerrolle auf dem persistenten Mehrspielerpfad serverseitig abgewiesen.
- Nur regulär beendete Runden werden mit einem stabilen serverseitigen Abschluss-Schlüssel genau einmal gespeichert. Transportabbruch beendet eine Runde ohne Ergebnis; ein regulärer Zugtimeout folgt weiter der Spielregel und kann einen Teamwert von null ergeben. Jedes Teammitglied erhält den tatsächlichen finalen Teamwert, einschließlich null. Der bisherige Solo-Endwert bleibt nach derselben Formel erhalten, wird aber serverseitig berechnet.
- **BREAKING für den persistenten Smart10-Pfad:** Die bisherige `mp_create`-Übergabe eines vollständigen Spiels samt Lösungen und die lokale Solo-Bewertung dürfen keine autoritative Runde mehr erzeugen. Frontend und Backend müssen gemeinsam auf Spiel-ID, gespeicherten Snapshot, serverseitige Bewertung und bestätigte Speicherung umgestellt werden.
- Die produktive persistente Smart10-Anbindung bleibt standardmäßig deaktiviert, bis der separate Change `game-visibility-and-ownership` und die Rollout-Voraussetzungen umgesetzt und geprüft sind. Lokale Dev-/Test-Prüfung ist möglich; private Einladungen und Lehrer-Teilnahme als aktive Mehrspieler bleiben offen.

## Capabilities

### New Capabilities

- `smart10-session-persistence`: Versionierte Smart10-Karten, vertrauenswürdige Solo-/Mehrspieler-Runden und einmalige Speicherung abgeschlossener Ergebnisse.

### Modified Capabilities

Keine; im Hauptspec-Verzeichnis existieren noch keine Capabilities. Die offenen Changes `database-model-foundation` und `game-visibility-and-ownership` bleiben eigenständig.

## Impact

Betroffen sind spätere H2-/PostgreSQL-Migrationen ab V3, Smart10-Snapshot-Adapter und Ergebnisdienst im Quarkus-Backend, authentifizierte HTTP-/WebSocket-Spielpfade, Angular-Solo-/Lobby-/Multiplayer-Flows sowie ihre Tests. V1/V2-Migrationen, allgemeine Berechtigungsregeln und produktive Daten werden durch diese Planung nicht geändert. Die Foundation ist lokal weitgehend umgesetzt (22/23 Aufgaben), aber noch kein abgeschlossener oder ausgerollter Smart10-Nachweis; der Access-Change steht bei 0/13.
