# Tasks

## 1. Datenbestand und Sichtbarkeit

- [ ] 1.1 Eine kontrollierte, wiederholt prüfbare PostgreSQL-Migration samt Ausführungsanweisung für `games.visibility` (`PUBLIC` nur bei `NULL`, erlaubte Werte, `NOT NULL`, kein dauerhafter `PUBLIC`-Default) erstellen; Vor-/Nachzählungen und ein Wiederholungstest mit bereits privatem Spiel auf einer wegwerfbaren Datenbank müssen erfolgreich sein.
- [ ] 1.2 `Game` um `PRIVATE`/`PUBLIC` ergänzen, neue REST-Spiele standardmäßig privat und Demo-Spiele ausdrücklich öffentlich anlegen; Entity- und Ressourcentests müssen Altbestand, neue Defaults und die Erstellung durch Lehrkräfte sowie Administratoren abdecken.

## 2. Serverseitige Zugriffsregeln

- [ ] 2.1 Sichtbare Repository-Abfragen für angemeldete Nutzer, eigene private Spiele und alle Spiele für Administratoren bei Liste, Typfilter und Detailabruf einführen; Tests müssen öffentliche, eigene private, fremde private und Administrator-Sicht unterscheiden.
- [ ] 2.2 Alle REST-Lesewege an eine gültig geprüfte Keycloak-Identität und ein aktives Konto binden; Integrationstests müssen fehlende und ungültige Token mit Authentifizierungsfehler ohne Spieldaten, inaktive Konten mit Autorisierungsablehnung ohne Spieldaten, eigene private Spiele, 404 für fremde private IDs, Administrator-Zugriff sowie gruppierte/Typ-Listen abdecken.
- [ ] 2.3 `GameSocket`-Katalogbefehle schließen; WebSocket-Tests müssen für `get_games`, `get_games_by_type` und `get_game` ohne gültige Anmeldung fehlende Spieldaten sowie die identische Ablehnung für private, öffentliche und unbekannte IDs prüfen.
- [ ] 2.4 Erstellen auf aktive Lehrkräfte und Administratoren begrenzen und den Ersteller ausschließlich serverseitig setzen; Autorisierungstests müssen Schüler, aktive Administratoren, inaktive Konten und eine manipulierte Eigentümerangabe abdecken.
- [ ] 2.5 Bearbeiten, Sichtbarkeitswechsel und Löschen transaktional auf den gespeicherten aktiven Ersteller oder einen aktiven Administrator begrenzen; Tests müssen fremde öffentliche und private Spiele, besitzerlose Altspiele, Administrator-Ausnahme, unveränderte Daten nach Ablehnung, ungültige Sichtbarkeitswerte und manipulierte IDs/Eigentümerfelder bei `PUT` abdecken.
- [ ] 2.6 Den authentifizierten REST-Detailabruf für die Lobby so absichern, dass die erstellende Lehrkraft und Administratoren ein privates Spiel laden dürfen, andere Lehrkräfte und Schüler über die Spiel-ID aber keine Spieldaten erhalten; Tests müssen diese Fälle abdecken. Der Schüler-Beitritt zu einer privaten Session bleibt einem eigenen Change vorbehalten.

## 3. Angular-Anbindung

- [ ] 3.1 Authentifizierte REST-Lesewege für Spieleliste und Spieldetail im vorhandenen `AuthService`-Umfeld bereitstellen; Service-Tests müssen Bearer-Header, fehlendes Token und Fehlerantworten prüfen.
- [ ] 3.2 Spieleliste und Lobby-Detailansicht auf diese Lesewege umstellen und private Spiele kenntlich machen; Komponenten-Tests müssen öffentliche und eigene private Einträge, die Host-Aktion für Schüler bei öffentlichen Spielen sowie für die erstellende Lehrkraft und Administratoren bei privaten Spielen, die fehlende Anzeige und Host-Aktion für fremde private Spiele bei anderen Lehrkräften und Schülern sowie den geschützten Lobby-Detailabruf abdecken.

## 4. Übergreifende Prüfung

- [ ] 4.1 Backend mit `./mvnw test` und Angular mit `npm test -- --watch=false` sowie `npm run build` prüfen; Ergebnisse und verbleibende Abweichungen dokumentieren.
- [ ] 4.2 Die koordinierte Backend-/Frontend-Rollout-Reihenfolge und die Rollback-Sperre nach Anlage privater Spiele gegen den tatsächlich implementierten Stand prüfen; ein kurzer manueller REST-/WebSocket-Test muss bestätigen, dass ohne Login keine Spieldaten erscheinen und fremde private Spiele außerhalb von Administratorzugriffen verborgen bleiben.
- [ ] 4.3 Für echte private Gruppenrunden einen eigenen Session-Change mit authentifiziertem Hosting, Schüler-Beitritt und auf die aktive Runde begrenztem Zugriff auf private Inhalte vorbereiten; bis zu dessen Umsetzung die jetzige Lobby nicht als funktionsfähiges privates Gruppenspiel darstellen.
