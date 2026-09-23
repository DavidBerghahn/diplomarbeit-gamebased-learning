# Tasks

## 1. Datenbestand und Sichtbarkeit

- [ ] 1.1 Eine kontrollierte, wiederholt prüfbare PostgreSQL-Migration samt Ausführungsanweisung für `games.visibility` (`PUBLIC` nur bei `NULL`, erlaubte Werte, `NOT NULL`, kein dauerhafter `PUBLIC`-Default) erstellen; Vor-/Nachzählungen und ein Wiederholungstest mit bereits privatem Spiel auf einer wegwerfbaren Datenbank müssen erfolgreich sein.
- [ ] 1.2 `Game` um `PRIVATE`/`PUBLIC` ergänzen, neue REST-Spiele standardmäßig privat und Demo-Spiele ausdrücklich öffentlich anlegen; Entity- und Ressourcentests müssen Altbestand und neue Defaults abdecken.

## 2. Serverseitige Zugriffsregeln

- [ ] 2.1 Öffentliche sowie nutzerspezifisch sichtbare Repository-Abfragen für Liste, Typfilter und Detailabruf einführen; Tests müssen öffentliche, eigene private und fremde private Spiele unterscheiden.
- [ ] 2.2 Alle REST-Lesewege an die geprüfte optionale Keycloak-Identität anbinden; Integrationstests müssen anonyme öffentliche Abfragen, eigene private Spiele, 404 für fremde private IDs, gruppierte/Typ-Listen und ungültige Token abdecken.
- [ ] 2.3 `GameSocket` auf ausschließlich öffentliche Abfragen umstellen; WebSocket-Tests müssen alle drei Befehle sowie die identische Fehlerantwort für private und unbekannte IDs prüfen.
- [ ] 2.4 Erstellen auf aktive Lehrkräfte begrenzen und den Eigentümer ausschließlich serverseitig setzen; Autorisierungstests müssen Schüler, Administratoren, inaktive Lehrkräfte und eine manipulierte Eigentümerangabe abdecken.
- [ ] 2.5 Bearbeiten, Sichtbarkeitswechsel und Löschen transaktional auf den gespeicherten aktiven Ersteller begrenzen; Tests müssen Fremdspiele, private IDs, eigentliche Ersteller und Altspiele ohne Ersteller sowie unveränderte Daten nach Ablehnung prüfen.

## 3. Angular-Anbindung

- [ ] 3.1 Einen authentifizierten REST-Leseweg für die Spieleliste im vorhandenen `AuthService`-Umfeld bereitstellen; Service-Tests müssen Bearer-Header, fehlendes Token und Fehlerantworten prüfen.
- [ ] 3.2 Spieleliste auf diesen Leseweg umstellen und private Spiele kenntlich, aber nicht hostbar machen; Komponenten-Tests müssen öffentliche und eigene private Einträge, die fehlende Host-Aktion bei `PRIVATE` und den weiterhin öffentlichen Lobby-Detailabruf abdecken.

## 4. Übergreifende Prüfung

- [ ] 4.1 Backend mit `./mvnw test` und Angular mit `npm test -- --watch=false` sowie `npm run build` prüfen; Ergebnisse und verbleibende Abweichungen dokumentieren.
- [ ] 4.2 Die geplante Rollout-Reihenfolge und die Rollback-Sperre nach Anlage privater Spiele gegen den tatsächlich implementierten Stand prüfen; ein kurzer manueller REST-/WebSocket-Test muss bestätigen, dass private Spieldaten nirgends öffentlich erscheinen.
