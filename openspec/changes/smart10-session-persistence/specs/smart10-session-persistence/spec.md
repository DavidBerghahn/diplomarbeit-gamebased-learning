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

### Requirement: Verifizierte Identität für persistierte Spielaktionen
Persistierbare Smart10-Runden SHALL Host und aktive Teilnehmer an ihre validierte Schul-Keycloak-Identität und vorhandene Anwendungsbenutzer binden. Eine WebSocket-Verbindung SHALL vor Spielaktionen eine kurzlebige, einmalige serverseitig ausgestellte Berechtigung annehmen; abgelaufene, wiederverwendete oder anonyme Zugriffe MUST abgewiesen werden. Verbindungskennungen, Anzeigenamen und Client-Claims SHALL keine Benutzeridentität ersetzen. Die bestehende Erneuerung des Logins SHALL vor einer neuen Verbindung genutzt werden können, ohne Bearer-Tokens in WebSocket-URLs offenzulegen.

#### Scenario: Zwei Verbindungen derselben Person
- **WHEN** dieselbe verifizierte Person eine weitere Verbindung zur selben Runde öffnet
- **THEN** entsteht dadurch kein zweiter aktiver Teilnehmer und kein doppeltes Ergebnis

#### Scenario: Ticket-Replay oder anonyme Aktion
- **WHEN** ein Ticket abgelaufen oder bereits benutzt ist oder ohne verifizierte Identität eine Spielaktion gesendet wird
- **THEN** wird die Aktion ohne Zustandsänderung abgewiesen

### Requirement: Host und echte Mitspieler trennen
Ein Mehrspieler-Host SHALL weder Teammitglied noch aktiver Teilnehmer derselben Runde sein und SHALL daraus keine Punkte erhalten. Eine persistierbare Mehrspieler-Runde SHALL mindestens eine andere verifizierte aktive Person besitzen; doppelte Benutzer und leere Teams MUST ausgeschlossen werden. Bis zur offenen fachlichen Entscheidung über aktive Lehrer-Teilnahme SHALL der Server auf dem persistenten Mehrspielerpfad `TEACHER` bei `mp_join` und bei jeder Teilnehmerbindung abweisen, auch mit gültigem Ticket. `STUDENT` und `ADMIN` können gemäß den übrigen geltenden Regeln teilnehmen; Solo und Hosting für Lehrkräfte bleiben möglich. Diese technische Begrenzung trifft keine dauerhafte fachliche Entscheidung über Lehrer-Teilnahme. Private Einladungen bleiben außerhalb dieser Capability.

#### Scenario: Host startet mit Mitspieler
- **WHEN** ein Host eine zulässige Smart10-Runde mit mindestens einem anderen verifizierten Spieler startet
- **THEN** werden nur echte Mitspieler Teams und späteren Teilnehmerergebnissen zugeordnet

#### Scenario: Host allein oder leeres Team
- **WHEN** nur der Host anwesend ist oder ein vorgesehenes Team keinen aktiven Spieler besitzt
- **THEN** kann daraus kein gespeichertes Mehrspieler-Ergebnis entstehen

#### Scenario: Direkter Lehrer-Beitritt mit gültigem Ticket
- **WHEN** eine verifizierte `TEACHER`-Identität mit gültigem Ticket `mp_join` oder eine andere Teilnehmerbindung für eine persistente Mehrspieler-Runde ausführt
- **THEN** weist der Server die aktive Teilnahme ohne Zustandsänderung ab; das Ticket allein berechtigt nicht zur Teilnahme

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
Die persistente Smart10-Anbindung SHALL in der Produktion standardmäßig deaktiviert bleiben, bis die geplanten Spielsichtbarkeits- und Eigentümerregeln sowie die Migrations- und Rollout-Prüfungen umgesetzt sind. Diese Capability SHALL weder einen allgemeinen Zugang zu privaten Spielen noch eine aktive Lehrer-Mehrspielerrolle einführen. Lokale Dev-/Test-Runden SHALL getrennt prüfbar sein.

#### Scenario: Noch offener Access-Change
- **WHEN** die neue Smart10-Persistenz lokal vorhanden ist, die geplanten Sichtbarkeitsregeln aber noch nicht umgesetzt und geprüft sind
- **THEN** kann sie in Produktion nicht als freigegebener persistenter Spielpfad genutzt werden
