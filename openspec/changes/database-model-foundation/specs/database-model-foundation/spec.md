# Spec Delta

## Purpose

Diese Capability stellt eine migrationssichere, historisch nachvollziehbare Persistenzgrundlage für Spielinhalte und vollständig abgeschlossene Runden bereit, ohne bestehende IDs, Fragen oder API-Daten zu verlieren.

## ADDED Requirements

### Requirement: Erhalt des bestehenden Datenbestands
Die Umstellung SHALL bestehende Benutzer, Spiele, Fragen, Antwortoptionen, Spieltypen und ihre Identitäten erhalten. Sie MUST auch die heute vorhandenen Fragetypen und Antwortoptionen abbilden können, selbst wenn diese nicht zu den im V1-Entwurf zusätzlich vorgesehenen Fragetypen gehören. Ein fehlender belegbarer Ersteller SHALL nicht durch eine automatische Eigentümerzuordnung ersetzt werden.

#### Scenario: Bestehendes Quiz mit Antwortoptionen
- **WHEN** ein bestehendes Spiel mit String-ID, einer `single_choice`-Frage und geordneten Antwortoptionen umgestellt wird
- **THEN** bleiben Spiel-ID, Fragetext, Fragetyp, Optionsreihenfolge und Richtigkeitswerte nach der Umstellung abrufbar

#### Scenario: Besitzerloses Altspiel
- **WHEN** ein bestehendes Spiel keinen gespeicherten `created_by_id`-Wert besitzt
- **THEN** bleibt dieser Wert ohne unbelegte Zuordnung leer und das Spiel geht durch die Umstellung nicht verloren

### Requirement: Spielfamilien und unabhängige Spielkopien
Eine Spielfamilie SHALL ein Original und seine eigenständigen Spielkopien gruppieren. Eine Spielkopie SHALL eine eigene Spielidentität und eigene bearbeitbare Fragen besitzen; eine Spielfamilie SHALL keinen Spieltyp ersetzen. Eine Frage SHALL für ihre Unabhängigkeit keine Herkunfts-Selbstreferenz benötigen. Beim Löschen einer Vorlage mit vorhandener Kopie SHALL deren Herkunft und Familienbeziehung erhalten bleiben, auch wenn noch keine Runde abgeschlossen wurde.

#### Scenario: Spielvariante aus einer Vorlage
- **WHEN** eine Spielkopie aus einem vorhandenen Spiel angelegt wird
- **THEN** besitzt sie eine neue Spiel-ID und unabhängige Fragen, bleibt aber derselben Spielfamilie wie die Vorlage zugeordnet

#### Scenario: Verschiedene Spieltypen
- **WHEN** Spiele des Typs `Smart10` und `JustOne` gespeichert werden
- **THEN** bleiben dies Spieltypen; eine gemeinsame Spielfamilie entsteht dadurch allein nicht

#### Scenario: Vorlage einer Kopie löschen
- **WHEN** eine Spielvorlage ohne abgeschlossene Runde gelöscht wird, von der eine Kopie besteht
- **THEN** bleibt die Vorlage als logisch gelöschter Herkunftsdatensatz mit ihrer Familienzuordnung bestehen, die Kopie bleibt unabhängig nutzbar und ihre Herkunftsreferenz gültig

### Requirement: Unveränderliche Spiel- und Frageversionen
Eine inhaltliche Bearbeitung SHALL einen neuen vollständigen Spiel-Snapshot einschließlich der Branch-Werte und ihrer Reihenfolge erzeugen. Bereits verwendete Spiel- und Frageversionen samt ihren Branches SHALL unverändert und für historische Ergebnisse referenzierbar bleiben. Die sichtbare aktuelle Version SHALL erst nach erfolgreicher vollständiger Erstellung des neuen Snapshots wechseln.

#### Scenario: Frage in einem Spiel ändern
- **WHEN** eine Frage in einem bestehenden Spiel bearbeitet wird
- **THEN** zeigt das Spiel für neue Runden auf eine neue Version und frühere abgeschlossene Runden behalten ihren ursprünglichen Fragenstand

#### Scenario: Fehler beim Versionswechsel
- **WHEN** das Speichern einer neuen Frageversion oder Fragenreihenfolge fehlschlägt
- **THEN** bleibt die bisherige aktuelle Spielversion unverändert und es wird kein gemischter Snapshot sichtbar

#### Scenario: Branches eines Spiels ändern
- **WHEN** die geordneten Branches nach einer gespielten Version bearbeitet werden
- **THEN** liefert die aktuelle Version die neuen Branch-Werte in ihrer Reihenfolge und die frühere Version weiterhin die damaligen Werte in der damaligen Reihenfolge

### Requirement: Tags statt Fachzuordnung
Spielversionen SHALL beliebig viele Tags über eine m:n-Beziehung tragen können. Die Umstellung SHALL jeden vorhandenen, nichtleeren `games.subject`-Wert als Tag am entsprechenden migrierten Spielinhalt erhalten. Bestehende, getrennte Branch- und Typangaben SHALL dabei erhalten bleiben.

#### Scenario: Fach-Backfill
- **WHEN** ein Altspiel das Fach `Programmieren` besitzt
- **THEN** trägt seine migrierte aktuelle Spielversion das Tag `Programmieren`, ohne das Fach als Pflichtfeld eines neuen Spiels vorauszusetzen

#### Scenario: Mehrere Tags
- **WHEN** eine Spielversion den Tags `Programmieren` und `Grundlagen` zugeordnet wird
- **THEN** lassen sich beide Zuordnungen ohne doppelte Spielversion und ohne doppelte Tag-Identität speichern

### Requirement: Kompatible Frageninhalte
Das Modell SHALL die bisherigen geordneten Antwortoptionen samt Boolean-Richtigkeitswerten und die künftig vorgesehenen Wahr/Falsch-, Reihenfolge- und Freitextdaten verlustfrei speichern können. Unbekannte oder noch nicht fachlich zugeordnete Bestandswerte SHALL bis zu einer gesonderten Typentscheidung erhalten bleiben und nicht automatisch umgedeutet werden.

#### Scenario: Legacy- und V1-Fragen nebeneinander
- **WHEN** eine `single_choice`-Bestandsfrage und eine neue `TRUE_FALSE`-Frage gespeichert werden
- **THEN** bleiben die ursprünglichen Typwerte und ihre jeweiligen Lösungsdaten getrennt und lesbar

### Requirement: Meldungen an einer Frageversion
Eine Fragenmeldung SHALL genau die gemeldete Frageversion referenzieren und einen Kommentar sowie die meldende Person festhalten. Sie SHALL keine redundante direkte Frage-ID benötigen und SHALL bei einer unabhängigen Fragenkopie nicht mitkopiert werden. Beim Löschen einer gemeldeten Frage SHALL auch deren Meldung entfernt werden; bereits abgeschlossene Runden und Antworten MUST dabei historisch nachvollziehbar bleiben.

#### Scenario: Frageversion melden
- **WHEN** eine berechtigte Person eine konkrete Frageversion mit Kommentar meldet
- **THEN** verweist die gespeicherte Meldung auf diese Version und nicht zusätzlich auf eine separate Frage-ID

#### Scenario: Gemeldete Frage kopieren
- **WHEN** eine Frage mit Meldung in ein unabhängiges Spiel kopiert wird
- **THEN** enthält die neue Frage keine übernommene Meldung

#### Scenario: Gemeldete Frage löschen
- **WHEN** eine gemeldete Frage gelöscht wird, die in einer abgeschlossenen Runde verwendet wurde
- **THEN** ist die Meldung entfernt und die historische Antwort weiterhin dem damals gespielten Inhalt zuordenbar

### Requirement: Nur vollständige Runden dauerhaft speichern
Das System SHALL dauerhaft ausschließlich vollständig abgeschlossene Runden zusammen mit ihren Teams, aktiven Teilnehmern und Ergebnissen speichern. Lobby-, laufende und abgebrochene Runden sowie deren Zwischenantworten MUST ohne dauerhaftes Ergebnis bleiben. Die dauerhafte Anlage aller Bestandteile einer abgeschlossenen Runde SHALL atomar erfolgen. Eine serverseitig stabile Identität SHALL dieselbe logische abgeschlossene Runde bei wiederholten oder parallelen Speicheraufrufen eindeutig kennzeichnen. Ein inhaltlich identischer Wiederholungsaufruf SHALL dasselbe Ergebnis zurückgeben; ein abweichender Aufruf mit derselben Identität MUST abgewiesen werden, ohne das gespeicherte Ergebnis oder den daraus abgeleiteten Fortschritt zu verändern.

#### Scenario: Vollständig abgeschlossene Runde
- **WHEN** eine Runde vollständig beendet und ihr Ergebnis zur Speicherung übergeben wird
- **THEN** werden Session, Teams, Teilnehmer und Antworten als zusammengehöriger Stand gespeichert

#### Scenario: Abbruch oder Speicherfehler
- **WHEN** eine Runde abgebrochen wird oder ein Teil der Ergebnisanlage fehlschlägt
- **THEN** entsteht kein teilweiser dauerhafter Session-, Teilnehmer-, Team- oder Antwortstand

#### Scenario: Wiederholter oder paralleler Abschluss
- **WHEN** der vollständige Abschluss derselben logischen Runde nach einem Timeout erneut oder gleichzeitig gespeichert wird
- **THEN** existiert genau ein Ergebnis und daraus höchstens einmal abgeleiteter Fortschritt; ein identischer Aufruf erhält dieses Ergebnis und ein abweichender Aufruf mit derselben Identität wird abgewiesen

### Requirement: Host und Teilnehmer getrennt halten
Ein Host SHALL in derselben Runde kein aktiver Teilnehmer sein und aus bloßem Hosting weder Punkte noch Fortschritt erhalten. Bei aktivem Solo-Spiel SHALL die spielende Person als Teilnehmer mit Ergebnis erfasst werden. Ob ein privates Spiel ohne Mitspieler gehostet werden kann, bleibt außerhalb dieses Persistenzvertrags.

#### Scenario: Gehostete Runde
- **WHEN** eine Lehrkraft eine abgeschlossene Runde hostet, ohne selbst mitzuspielen
- **THEN** verweist die Runde auf sie als Host, enthält für sie keinen Teilnehmerdatensatz und schreibt ihr keinen Fortschritt gut

#### Scenario: Allein gespielte Runde
- **WHEN** eine Lehrkraft oder ein Administrator ein Spiel allein vollständig spielt
- **THEN** enthält das Ergebnis diese Person als aktiven Teilnehmer mit Punkten und Fortschritt, ohne sie zugleich zum Host zu machen

### Requirement: Finale Ergebnisse ohne redundante Felder
Ein abgeschlossenes Ergebnis SHALL den vom Spielmodus finalisierten Teilnehmerwert und bei Teamrunden den finalisierten Teamwert samt Platzierung speichern. Alle aktiven Teilnehmer SHALL Punkte erhalten; die Plätze 1 bis 3 SHALL deutlich höher bewertet werden, ohne hier eine Formel festzulegen. Gewinnerstatus SHALL aus Platzierung und Teamzuordnung ableitbar sein. Einzelantworten SHALL Antwortinhalt und Boolean-Richtigkeit speichern, aber weder eigene Punkte noch Antwortdauer.

#### Scenario: Teamresultat mit Gleichstand
- **WHEN** zwei Teams denselben ersten Platz erreichen
- **THEN** bleiben Teamplätze `1, 1, 3` und individuelle Endwerte erhalten; der Gewinnerstatus lässt sich aus den Plätzen ableiten

#### Scenario: Einzelantwort
- **WHEN** eine abgeschlossene Runde eine beantwortete Frage enthält
- **THEN** sind Antwort und Richtigkeitswert gespeichert, ohne redundanten Antwort-Score oder Antwortdauer

### Requirement: Kompatibilität der vorhandenen Spiele-Schnittstellen
Die Persistenzumstellung SHALL die heute verwendeten Spiel-IDs und die Daten für bestehende REST- und WebSocket-Spielkataloge erhalten. Neue interne Relationen SHALL nicht automatisch neue Endpunkte, ein anderes JSON-Format oder eine Freigabe privater Inhalte bewirken. Die geplanten Zugriffsregeln aus `game-visibility-and-ownership` bleiben ein eigenständiger Change.

#### Scenario: Spiel nach Schemaumstellung laden
- **WHEN** ein bestehender Client ein migriertes Spiel über einen weiterhin berechtigten vorhandenen Leseweg lädt
- **THEN** findet er es unter derselben ID und erhält die bisher benötigten Spiel-, Fragen- und Optionsdaten

#### Scenario: Gelesenes Spiel wieder bearbeiten
- **WHEN** ein bestehender Client ein gelesenes Spiel mit unveränderten, umsortierten oder bearbeiteten Fragen erneut speichert
- **THEN** bleiben eindeutig zuordenbare Fragenidentitäten und Meldungsbezüge erhalten; ohne ausdrückliche Änderung gehen zusätzliche Tags und intern erhaltene Lösungsdaten nicht verloren

#### Scenario: Noch nicht umgesetzter Zugriffs-Change
- **WHEN** nur diese Persistenzgrundlage umgesetzt ist
- **THEN** wird daraus keine Behauptung abgeleitet, dass private Spiele bereits geschützt oder anonyme Katalogzugriffe bereits geschlossen sind
