# Spec Delta

## Purpose

Diese Capability verbindet Smart10-Solo- und Mehrspieler-Runden mit einem gespeicherten Spielstand, bewertet Antworten serverseitig und bewahrt nur regulär abgeschlossene Ergebnisse einmalig und historisch nachvollziehbar auf.

## ADDED Requirements

### Requirement: Zwölf versionierte Karten je Smart10-Frage
Eine persistierbare Smart10-Gruppenfrage SHALL in ihrer verwendeten Frageversion genau zwölf unterscheidbare Einzelkarten mit fester Reihenfolge, Fragemodus und zum Modus passender typisierter Lösung besitzen. Bei `ORDERING` SHALL die zwölf `solution_rank`-Werte exakt eine Permutation von 1 bis 12 bilden. Der Schreibadapter und der Startdienst SHALL die vollständige Rangfolge prüfen und ungültige Versionen abweisen. Kartentext und Lösung SHALL bei Bearbeitung als neue unveränderliche Version erhalten bleiben. Eine unabhängige Spielkopie SHALL ihre eigenen Karten erhalten; eine historische Antwort SHALL weiterhin der damals gespielten Karte zuordenbar bleiben.

#### Scenario: Drei Kartenmodi
- **WHEN** gespeicherte Smart10-Fragen im Modus `TRUE_FALSE`, `FREE_TEXT` beziehungsweise `ORDERING` geladen werden
- **THEN** besitzt jede Frage genau zwölf Karten mit Boolean-, Text- beziehungsweise Ranglösung und ihrer ursprünglichen Reihenfolge

#### Scenario: Ungültige Kartenzahl oder Lösung
- **WHEN** eine gespeicherte Smart10-Frage weniger oder mehr als zwölf Karten oder eine nicht zum Modus passende Lösung besitzt
- **THEN** kann damit keine persistierbare Smart10-Runde gestartet werden; der Inhalt wird nicht stillschweigend durch Demokarten ersetzt

#### Scenario: Ungültige ORDERING-Rangfolge
- **WHEN** eine `ORDERING`-Frage einen Rang 0 oder 13, einen doppelten Rang oder dadurch einen fehlenden Rang enthält
- **THEN** weist der Schreibadapter den Inhalt ab und der Startdienst lehnt eine bereits gespeicherte ungültige Version ab, statt die Rangfolge zu ergänzen oder eine Runde zu starten

#### Scenario: Frage nach einer Runde ändern oder kopieren
- **WHEN** eine gespielte Frage bearbeitet oder ein Spiel mit dieser Frage kopiert wird
- **THEN** bleiben die früheren Karten und Antworten der alten Version zugeordnet und die Kopie besitzt unabhängige Kartenidentitäten

### Requirement: Antworten auf konkrete Karten ohne Verlust anderer Fragetypen
Eine Smart10-Antwort SHALL sowohl die gespielte Frageversion als auch genau eine Karte dieser Version identifizieren. Ein Teilnehmer SHALL mehrere verschiedene Karten derselben Gruppenfrage beantworten können, aber dieselbe Karte pro Runde höchstens einmal. Eine kartenlose Antwort zu einer Smart10-Frage und eine Kartenantwort zu einer anderen Spielfrage MUST abgewiesen werden. Antworten anderer Spieltypen SHALL ihre bestehende Ein-Antwort-pro-Frage-Regel und historischen Daten behalten.

#### Scenario: Zwei Karten einer Gruppenfrage
- **WHEN** eine Person in einer abgeschlossenen Smart10-Runde zwei verschiedene Karten derselben Frage beantwortet
- **THEN** sind zwei getrennte Antworten mit ihren Karten, Inhalten, Boolean-Richtigkeiten und Zeitpunkten nachvollziehbar

#### Scenario: Fremde oder doppelte Karte
- **WHEN** eine Antwort eine Karte aus einer anderen Frageversion nennt oder dieselbe Karte erneut beantwortet
- **THEN** wird die Antwort abgewiesen, ohne ein teilweises Ergebnis zu speichern

#### Scenario: Bestehende Nicht-Smart10-Antwort
- **WHEN** ein bestehendes Ergebnis eines anderen Spieltyps gelesen oder geschrieben wird
- **THEN** bleibt seine Frageantwort ohne Smart10-Karte gültig und die bisherige Eindeutigkeit pro Teilnehmer und Frage erhalten

#### Scenario: Gemischte Antwortform
- **WHEN** zu einer Smart10-Frage eine kartenlose Antwort oder zu einer Nicht-Smart10-Frage eine Kartenantwort übergeben wird
- **THEN** wird der gesamte Ergebnisversuch abgewiesen

### Requirement: Gespeicherter Snapshot als einzige Spiel- und Lösungsquelle
Eine persistierbare Smart10-Runde SHALL aus einem serverseitig geladenen, berechtigten Spiel und einer festen Spielversion starten. Der Server SHALL Lösungen, Antwortbewertung und Endwerte aus diesem Snapshot ableiten; übermittelte Spielinhalte, Lösungen, Richtigkeiten oder Endwerte eines Clients MUST ignoriert oder abgewiesen werden. Vor Auflösung einer Karte SHALL ihre Lösung über den persistenten Spielkanal nicht mitgeteilt werden. Eine ausdrücklich gewählte lokale Demo SHALL eindeutig als ungespeichert erkennbar bleiben und MUST keine automatische produktive Demo-Saat auslösen.

#### Scenario: Manipuliertes Clientspiel
- **WHEN** ein Client beim Start oder bei der Antwort eine andere Lösung oder einen höheren Score mitschickt
- **THEN** verwendet der Server ausschließlich den gepinnten gespeicherten Stand und den selbst berechneten Endwert

#### Scenario: Änderung während einer Runde
- **WHEN** das Spiel während einer aktiven Runde eine neue Version erhält
- **THEN** spielt die aktive Runde mit der beim Start gewählten Version fertig und ein späterer Start nutzt die dann aktuelle Version

#### Scenario: Lokale Demo
- **WHEN** die Person ausdrücklich den lokalen Demopfad wählt
- **THEN** wird diese Runde ausdrücklich als Demo ohne dauerhaftes Ergebnis angezeigt

### Requirement: Freigegebene gespeicherte Beispiele für den Produktionsstart
Die beiden ausdrücklich freigegebenen Spiele `sample-smart10-programming` und `sample-just-one-programming` SHALL bei aktivem `gamebased.sample-games.enabled` als öffentliche versionierte Spiele angelegt werden. Dieser Schalter SHALL unabhängig von den alten Entwicklungs-Demos sein und in Prod standardmäßig aktiv sein; `SAMPLE_GAMES_ENABLED=false` SHALL die Anlage abschalten können. Existierende IDs einschließlich gelöschter Einträge MUST unverändert bleiben. Ein Neustart MUST weder Duplikate noch neue Versionen vorhandener Beispiele erzeugen. Diese Anlage MUST weder Migrationen noch die produktive Spielfreischaltung automatisch aktivieren.

#### Scenario: Produktionsstart ohne Entwicklungs-Demos
- **WHEN** Beispieldaten aktiviert und alte Entwicklungs-Demos deaktiviert sind
- **THEN** werden ausschließlich die beiden vollständigen gespeicherten Programmier-Beispiele hinzugefügt und können über den normalen öffentlichen Spielekatalog geladen werden

#### Scenario: Beispiel nach Bearbeitung erneut starten
- **WHEN** der Server nach einer Bearbeitung oder nach gespeicherten Ergebnissen neu startet
- **THEN** bleiben Spielinhalt, Versionen und vorhandene Ergebnisse erhalten; eine regulär gespielte Beispielrunde nutzt denselben Ergebnisdienst wie andere gespeicherte Spiele

### Requirement: Verifizierte Identität für persistierte Spielaktionen
Persistierbare Smart10-Runden SHALL Host und aktive Teilnehmer an ihre validierte Schul-Keycloak-Identität und vorhandene Anwendungsbenutzer binden. Eine WebSocket-Verbindung SHALL vor Spielaktionen eine kurzlebige, einmalige serverseitig ausgestellte Berechtigung annehmen; abgelaufene, wiederverwendete oder anonyme Zugriffe MUST abgewiesen werden. Verbindungskennungen, Anzeigenamen und Client-Claims SHALL keine Benutzeridentität ersetzen. Die bestehende Erneuerung des Logins SHALL vor einer neuen Verbindung genutzt werden können, ohne Bearer-Tokens in WebSocket-URLs offenzulegen.

#### Scenario: Zwei Verbindungen derselben Person
- **WHEN** dieselbe verifizierte Person eine weitere Verbindung zur selben Runde öffnet
- **THEN** entsteht dadurch kein zweiter aktiver Teilnehmer und kein doppeltes Ergebnis

#### Scenario: Ticket-Replay oder anonyme Aktion
- **WHEN** ein Ticket abgelaufen oder bereits benutzt ist oder ohne verifizierte Identität eine Spielaktion gesendet wird
- **THEN** wird die Aktion ohne Zustandsänderung abgewiesen

### Requirement: Host und echte Mitspieler trennen
Der Host SHALL bei Erstellung per Toggle entscheiden, ob er aktiv mitspielt. Ein beobachtender Host SHALL aus Teams, Teilnehmerergebnissen und Punkten ausgeschlossen bleiben. Ein mitspielender Host SHALL einschließlich Lehrerrolle wie andere Mitglieder gewertet werden. Doppelte Benutzer und leere Teams MUST ausgeschlossen werden. Private Einladungen bleiben außerhalb dieser Capability.

#### Scenario: Host startet mit Mitspieler
- **WHEN** ein Host eine zulässige Smart10-Runde mit mindestens einem anderen verifizierten Spieler startet
- **THEN** werden nur echte Mitspieler Teams und späteren Teilnehmerergebnissen zugeordnet

#### Scenario: Host allein oder leeres Team
- **WHEN** kein aktiver Spieler anwesend ist oder ein vorgesehenes Team keinen aktiven Spieler besitzt
- **THEN** kann daraus kein gespeichertes Mehrspieler-Ergebnis entstehen; ein allein mitspielender Host in einem Smart10-Team ist dagegen zulässig

#### Scenario: Lehrer als aktiver Mitspieler
- **WHEN** eine aktive verifizierte Lehreridentität mit gültigem Ticket einem zulässigen Raum beitritt oder den Host-Mitspieltoggle aktiviert
- **THEN** ist die Teilnahme nach denselben Spielregeln wie für andere aktive Mitglieder möglich

### Requirement: Solo-Punkte serverseitig nach bestehender Formel
Ein regulär abgeschlossenes Smart10-Solospiel SHALL den Teilnehmer-Endwert serverseitig als `correctCards + completedQuestions * 10 + bestStreak * 2` berechnen. Die bisherigen drei Kartenmodi SHALL mit demselben gespielten Snapshot bewertet werden. Ein vom Client berechneter Wert SHALL die Speicherung nicht beeinflussen.

#### Scenario: Solo-Abschluss
- **WHEN** eine verifizierte Person alle Karten einer gültigen gespeicherten Solo-Runde regulär beendet
- **THEN** wird genau der serverseitig berechnete Wert mit ihren Kartenantworten als abgeschlossenes Ergebnis gespeichert

### Requirement: Mehrspieler-Endwerte einschließlich null
Bei Smart10-Mehrspieler SHALL ein korrekt beantworteter Zug den Teamwert um eins erhöhen; Falschantwort und regulärer Zugtimeout SHALL den Teamwert auf null setzen und das Team für die verbleibende Runde eliminieren. Beim Wechsel zur nächsten Gruppenfrage SHALL weder der Teamwert zurückgesetzt noch ein eliminiertes Team wieder aktiviert werden. Sind alle Teams eliminiert, SHALL die Runde nach der bestehenden Endregel regulär enden, auch wenn weitere Gruppenfragen vorhanden sind. Jedes aktive Teammitglied SHALL den tatsächlichen finalen Teamwert einschließlich null als eigenen Teilnehmer-Endwert erhalten. Teams SHALL absteigend nach Endwert platziert werden; Gleichstände teilen einen Platz und überspringen die folgenden Platznummern. Ein zusätzlicher Bonus oder ein künstlicher Mindestpunktwert MUST unterbleiben.

#### Scenario: Timeout mit null Punkten
- **WHEN** der reguläre Zugtimeout ein Team auf null setzt und die Runde anschließend nach den Spielregeln endet
- **THEN** erhält jedes aktive Mitglied dieses Teams einen finalen Teilnehmerwert von null und die Runde kann gespeichert werden

#### Scenario: Elimination über Fragenwechsel
- **WHEN** ein Team durch Falschantwort oder Zugtimeout eliminiert wird und ein anderes Team die nächste Gruppenfrage erreicht
- **THEN** bleibt das eliminierte Team aus dem weiteren Spiel ausgeschlossen und sein Wert wird beim Fragenwechsel nicht zurückgesetzt

#### Scenario: Alle Teams eliminiert
- **WHEN** alle Teams vor der letzten Gruppenfrage eliminiert sind
- **THEN** endet die Runde nach der bestehenden Spielregel regulär und kann mit ihren finalen Werten gespeichert werden

#### Scenario: Gleichstand
- **WHEN** zwei Teams denselben höchsten Endwert und ein drittes Team einen niedrigeren Endwert haben
- **THEN** lauten ihre Platzierungen `1, 1, 3` und alle Mitglieder erhalten jeweils den Wert ihres Teams

### Requirement: Nur regulären Abschluss einmalig bestätigen
Nur eine nach den Spielregeln regulär abgeschlossene Smart10-Runde SHALL mit Teams, Teilnehmern und Kartenantworten atomar gespeichert werden. Abbruch, Verbindungsverlust oder Serverneustart vor dem Abschluss MUST ohne dauerhaftes Rundenergebnis bleiben; ein regulärer Zugtimeout allein ist kein Abbruch. Der Server SHALL pro logischer Runde einen stabilen Abschluss-Schlüssel verwenden. Gleiche Wiederholungen SHALL dasselbe Ergebnis liefern; abweichende Wiederholungen MUST abgewiesen werden. Die Oberfläche SHALL Speicherung erst nach bestätigtem Commit mit Ergebniskennung anzeigen. Bei einem Speicherfehler SHALL der unveränderte finale Inhalt samt Schlüssel für einen erneuten Versuch verfügbar bleiben, solange die laufende Serverinstanz ihn hält.

#### Scenario: Transportabbruch gegenüber Spieltimeout
- **WHEN** eine Spielverbindung vor dem regulären Ende verloren geht, gegenüber einem regulären Zugtimeout mit späterem Spielende
- **THEN** entsteht im ersten Fall kein Ergebnis und im zweiten Fall kann ein reguläres Ergebnis mit null Punkten entstehen

#### Scenario: Speicherfehler und Retry
- **WHEN** das finale Ergebnis zunächst nicht gespeichert werden kann und derselbe Abschluss erneut versucht wird
- **THEN** bestätigt die Oberfläche erst nach Commit eine Ergebniskennung und es entsteht höchstens ein Ergebnis mit einmaligem Fortschritt

#### Scenario: Abweichender Retry
- **WHEN** derselbe Abschluss-Schlüssel erneut mit verändertem Endwert oder Antworten verwendet wird
- **THEN** wird der neue Inhalt abgewiesen und das zuerst gespeicherte Ergebnis bleibt unverändert

### Requirement: Bestehende Zugriffsgrenzen für produktive Nutzung
Die persistente Smart10-Anbindung SHALL in der Produktion standardmäßig deaktiviert bleiben, bis die Spielsichtbarkeits- und Eigentümerregeln sowie die Migrations- und Rollout-Prüfungen bestätigt sind. Diese Capability SHALL keinen allgemeinen Zugang zu privaten Spielen einführen. Lokale Dev-/Test-Runden SHALL getrennt prüfbar sein.

#### Scenario: Produktive Freischaltung bleibt manuell
- **WHEN** das Backend ohne ausdrückliche produktive Freischaltung startet
- **THEN** bleibt der persistente Spielpfad in Produktion gesperrt

### Requirement: Einstellbare und zeitlich begrenzte Räume
Der Server SHALL Codes innerhalb der einzelnen aktiven Instanz atomar reservieren und nach 24 Stunden ab Erstellung ungültig machen, auch während des Spiels. Neustarts dürfen Räume schließen. Der Host SHALL für Smart10 5–120 Sekunden pro Zug wählen können, Standard 20. Zugtimeouts SHALL serverseitig automatisch zur Elimination und zum nächsten aktiven Team führen.

#### Scenario: Kein Client meldet den Timeout
- **WHEN** die Zugfrist ohne weiteren Clientbefehl verstreicht
- **THEN** verliert das Team seine Punkte, scheidet aus und alle verbundenen Clients erhalten den Folgezustand

### Requirement: Transparente Teamverwaltung
Zufallszuweisung SHALL die Teamgrößen höchstens um eins unterscheiden lassen. Freie Wahl SHALL Selbstwechsel erlauben; der Host SHALL Spieler umverteilen können und stärkere Ungleichverteilung ausdrücklich bestätigen müssen. Kein Team darf leer starten. Teamleiter SHALL zufällig ausgelost und im Abschluss angezeigt werden.

#### Scenario: Ungleich verteilte freie Teams
- **WHEN** sich besetzte Teams um mehr als eine Person unterscheiden
- **THEN** wird der Start ohne ausdrückliche Hostbestätigung abgewiesen

### Requirement: Just One mit getrennten Spieleransichten
Just One SHALL synchronisierte Räume mit mindestens zwei Personen pro Team und fünf Begriffen pro Karte verwenden. Der Ratende SHALL nur Nummern auswählen und den Zielbegriff bis zur Auflösung nicht über seine Spielverbindung erhalten. Mitspieler SHALL Hinweise senden; doppelte Hinweise SHALL ausgeblendet werden. Der Host SHALL bewerten und ungültige Versuche auch nach Spielende idempotent zurücknehmen können; dafür vergebene Punkte SHALL genau einmal vom Team und allen Mitgliedern abgezogen werden.

#### Scenario: Ratender wählt eine Nummer
- **WHEN** der Ratende eine Nummer auswählt
- **THEN** erhalten nur die anderen Verbindungen den Zielbegriff; der Ratende wartet auf die Hinweise

#### Scenario: Allgemeine Spielabrufe verraten keine Zielwörter
- **WHEN** ein Just-One-Spiel über normale HTTP-Listen, gruppierte Listen, Typfilter oder Details sowie über die entsprechenden WebSocket-Katalogbefehle geladen wird
- **THEN** enthält die Antwort keine Zielwörter oder alternativen Lösungsfelder; Metadaten und die aus vollständigen Inhalten ermittelte Spielbarkeit bleiben erhalten und die gespeicherten Inhalte bleiben unverändert

#### Scenario: Geschützter Autoreninhalt
- **WHEN** der Editor-Vollinhalt eines Just-One-Spiels angefordert wird
- **THEN** erhalten nur der aktive berechtigte Besitzer oder Admin die Zielwörter; nach erlaubtem Kopieren eines öffentlichen Spiels kann der neue Besitzer seine eigene Kopie bearbeiten

#### Scenario: Autorisierte Fragenübernahme
- **WHEN** eine aktive Lehrkraft oder ein Admin ausdrücklich Fragen eines zugänglichen öffentlichen oder eigenen Spiels zur Wiederverwendung lädt
- **THEN** erhält der geschützte Autorenweg die vollständigen benötigten Inhalte ohne automatische dauerhafte Kopie beim bloßen Vorschau-Lesen; anonyme Personen, Schüler und unberechtigte Zugriffe auf fremde private Spiele werden abgewiesen und normale Spielabrufe bleiben redigiert

#### Scenario: Nachträgliche Korrektur
- **WHEN** der ursprüngliche Host einen bereits gespeicherten gültigen Versuch zweimal ungültig macht
- **THEN** sinken Team- und Mitgliederscores nur einmal und Klassenstatistiken zeigen den korrigierten Wert

### Requirement: CSV und dauerhafte Spielanlage
Lehrer und Admins SHALL Smart10 aller drei Modi sowie Just One per CSV-Vorlage importieren, vor dem Speichern prüfen und als öffentliche oder private Spiele speichern können. Der Server SHALL ungültige Kartenzahl, Lösungstypen und Reihenfolgen ablehnen. Öffentliche Kataloge MUST private Inhalte ausschließen.

#### Scenario: Reihenfolge mit doppeltem Rang
- **WHEN** eine importierte Frage einen Rang doppelt enthält
- **THEN** wird der Import mit verständlichem Fehler abgewiesen und nicht gespeichert

### Requirement: Klassen- und Schülerstatistik
Aktive angemeldete Schüler, Lehrer und Admins SHALL alle Klassen mit Schülern, abgeschlossenen Spielen, Spielhäufigkeit, Punkten und Mitspielern einsehen können. Schüler SHALL dabei ausschließlich lesen können; Änderungen an Klassenzuordnungen MUST weiterhin aktiven Lehrern und Admins vorbehalten bleiben. Nicht angemeldete und inaktive Konten MUST vom Statistikzugriff ausgeschlossen bleiben. Statistikantworten SHALL keine konkreten Begriffe, Hinweise oder Antworten enthalten. Demos und abgebrochene Räume MUST ausgeschlossen bleiben.

#### Scenario: Schüler öffnet Klassenstatistik
- **WHEN** eine aktive angemeldete Schüleridentität die Statistik-API direkt aufruft
- **THEN** erhält sie dieselbe lesende Übersicht aller Klassen wie Lehrer und Admins, einschließlich anderer Klassen

#### Scenario: Schüler verändert eine Klassenzuordnung
- **WHEN** eine Schüleridentität die Zuordnungs-API direkt aufruft
- **THEN** wird die Änderung serverseitig mit 403 abgewiesen; die Statistikoberfläche bietet Schülern keine Zuordnungsformulare oder Verwaltungsaktionen an

#### Scenario: Noch offener Access-Change
- **WHEN** die neue Smart10-Persistenz lokal vorhanden ist, die geplanten Sichtbarkeitsregeln aber noch nicht umgesetzt und geprüft sind
- **THEN** kann sie in Produktion nicht als freigegebener persistenter Spielpfad genutzt werden

### Requirement: Dauerhafte Klassenzuordnung und Klassenpunkte
Die Statistik SHALL Schüler nach ihrer aktuellen gespeicherten Klasse filtern und für jede Klasse die Punkte jedes Schülers sowie die Summe dieser Schülerpunkte anzeigen. Schüler ohne abgeschlossene Spiele SHALL mit null Punkten erscheinen. Klassen- und Schülerwerte SHALL denselben Datumsfilter berücksichtigen. Teamwerte SHALL entsprechend den gespeicherten Teilnehmerpunkten je Schüler zählen. Ein gemeinsames Spiel SHALL in der Anzahl der Klassenspiele nur einmal gezählt werden.

#### Scenario: Schüler mit Adminrechten
- **WHEN** ein Projektmitglied mit Rolle `ADMIN` eine gespeicherte Klasse besitzt
- **THEN** erscheint es mit seinen Teilnehmerpunkten genau einmal in dieser Klasse und deren Summe, auch ohne bisherige Spiele mit null Punkten; seine Adminrechte bleiben unverändert
- **AND** bei fehlender Klasse kann die Zuordnung wie bei anderen Schülern ergänzt werden; Konten mit Rolle `TEACHER` bleiben aus der Schülerwertung ausgeschlossen

#### Scenario: Klasse und Zeitraum auswählen
- **WHEN** ein Lehrer eine Klasse und ein Anfangsdatum auswählt
- **THEN** sieht er nur die Schüler dieser Klasse und ihre seit dem Datum gespeicherten Punkte; die Klassensumme entspricht exakt der Summe der sichtbaren Schülerwerte

#### Scenario: Klasse aus dem Schulkonto oder ergänzter Zuordnung
- **WHEN** ein Schüler sich mit einer Klassenangabe anmeldet
- **THEN** wird die Klasse normalisiert gespeichert; fehlt diese Angabe später, bleibt die gespeicherte Klasse erhalten

#### Scenario: Fehlende Klasse ergänzen
- **WHEN** ein Lehrer oder Admin einen bislang nicht zugeordneten Schüler einer gültigen Klasse zuweist
- **THEN** wird die Klasse dauerhaft gespeichert und dessen bisheriger Punktestand der Klassensumme zugeordnet; ein Schüleraufruf dieses Endpunkts wird mit 403 abgewiesen

#### Scenario: Keine verlässliche Klassenangabe
- **WHEN** weder Schulkonto noch gespeichertes Profil eine Klasse angeben
- **THEN** wird der Schüler mit seinen Punkten als „Ohne Klasse“ angezeigt, bis eine Zuordnung ergänzt wurde; keine Klasse wird automatisch erfunden
