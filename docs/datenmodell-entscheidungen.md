# Datenmodell V1: Fragerunden und fachliche Entscheidungen

Stand: 30. September 2026

Quelle: Die Runden 1 bis 3 stammen aus der Codex-Aufgabe „Diplomarbeit aus Übergabe fortsetzen“ (`01a0c99d-71e7-7be0-b644-25e8234f662a`). Die Antworten zu Runde 4 und Runde 5 stammen aus dem Supervisor-Chat vom 23. September 2026. Der Stand vom 30. September bezeichnet die Überarbeitung dieses Dokuments, nicht das Datum der Antworten. Die Fragen sind für die Übergabe sinngemäß gekürzt, die Antworten inhaltlich wiedergegeben. Nur der bei Runde 5.5 gekennzeichnete Satz ist wörtlich. Dies ist **kein** vollständiges Chatprotokoll und **keine** Aussage über bereits implementierten Code.

Die spätere Antwort hat bei einem Widerspruch Vorrang. Der daraus entstandene technische Entwurf steht in [datenmodell-v1.md](datenmodell-v1.md). Bei einer erneuten Bearbeitung sollen Supervisor und Spezialisten zuerst diese Entscheidungen prüfen, Abweichungen zum Entwurf benennen und offene Punkte nicht stillschweigend entscheiden.

## Runde 1: Grundstruktur

1. **Wie kommen Schüler und ihre Klasse ins System?** David: Die Schulklasse wird über LeoCloud übergeben; Schüler werden automatisch übernommen. Der bestehende technische Login verwendet Schul-Keycloak. Ob „LeoCloud“ hier genau diesen Identitätsweg meint, ist aus der Antwort allein nicht ableitbar.
2. **Sind Lehrkräfte bestimmten Klassen zugeordnet?** David: Jede Lehrkraft darf alle Klassen einsehen und für alle Klassen Spiele erstellen.
3. **Braucht ein Spiel neben dem Fach ein strukturiertes Thema?** David: Nein. Es soll nur nach Fach gefiltert werden; Unterthemen stehen in Titel oder Beschreibung und werden per Textsuche gefunden.
4. **Wer sieht, bearbeitet und verwendet Spiele?** David: Spiele können privat oder für alle freigegeben sein. Nur der Ersteller darf sie bearbeiten. Ein Spiel kann kopiert und angepasst werden; die Variante bleibt dem vorherigen Spiel untergeordnet, damit ähnliche Spiele in der Bibliothek zusammengefasst werden und alle verfügbaren Varianten spielbar sind.
5. **Gibt es einen gemeinsamen Fragenpool oder eigene Fragen je Spiel?** David: Jedes Spiel hat eigene Fragen. Beim Übernehmen in ein anderes Spiel wird eine neue Frage gespeichert, die verändert werden kann.
6. **Welche Fragetypen gehören in V1?** David: Wahr/Falsch, Reihenfolge und Freitext.
7. **Wird jeder Spieldurchlauf gespeichert?** David: Ja, jede Session.
8. **Einzelspieler, Teams und individuelle Ergebnisse?** David: Je nach Spiel einzeln oder meist im Team; Ergebnisse pro Spieler speichern. Zunächst sollten auch Lehrkräfte mitspielen und Fortschritt erhalten. **Diese letzte Aussage wurde in Runde 2 zunächst durch „Lehrer spielen grundsätzlich nicht mit“ ersetzt. Runde 4 bestätigt Lehrkräfte wieder als fortschrittsrelevante Singleplayer; ihre Mehrspieler-Teilnahme bleibt offen.**
9. **Wer berechnet die Punkte?** David: Der jeweilige Spielmodus automatisch.
10. **Was umfasst der Fortschritt?** David: Single- und Multiplayer getrennt auswerten: gespielte Spiele, Multiplayer-Siege und Punkte je Modus; zusätzlich ein Levelsystem. Zunächst sollten Punkte pro Spiel nur die ersten Male vergeben werden. **Die Begrenzung wurde in Runde 2 ausdrücklich verworfen.**
11. **Wer meldet fehlerhafte Fragen, und wo erscheint die Meldung?** David: Nur Lehrkräfte dürfen mit Kommentar melden; gemeldete Fragen sollen als solche gelistet werden. Zunächst sollten Meldungen auch bei übernommenen Fragen erscheinen. **Die Übernahme auf Kopien wurde in Runde 2 ausdrücklich verworfen.**
12. **Was passiert mit alten Ergebnissen nach einer Frageänderung?** David: Die alte Version soll gespeichert bleiben.

## Runde 2: Grenzfälle und Korrekturen

1. **Benötigt die Veröffentlichung einer kopierten Variante die Zustimmung des Originalerstellers?** David: Nein. Sobald ein Spiel veröffentlicht ist, darf es kopiert werden.
2. **Darf jemand ein fremdes oder veröffentlichtes Spiel bearbeiten?** David: Nur der Ersteller darf sein Spiel bearbeiten. Andere Nutzer dürfen es grundsätzlich nicht bearbeiten; Kopieren ist nur Lehrkräften erlaubt. **Runde 4 ergänzt Administratoren als Ausnahme mit uneingeschränkten Spielrechten.**
3. **Wandert eine Meldung mit einer kopierten Frage mit?** David: Nein, die Meldung wird nicht übernommen.
4. **Wie wird Freitext bewertet?** David: Durch exakte Übereinstimmung mit internen Regeln, die etwas Spielraum wie unterschiedliche Groß-/Kleinschreibung lassen. Weitere Toleranzen wurden nicht festgelegt.
5. **Wie genau wird wiederholtes Spielen bei Punkten begrenzt?** David: Gar nicht; die vorherige Begrenzung entfällt. Es soll jedes Mal dieselbe Punkteberechnung gelten.
6. **Wer gewinnt und wie funktionieren Gleichstände?** David: Jedes Mitglied des Siegerteams erhält die Punkte. Lehrkräfte spielen grundsätzlich nicht mit. Bei Gleichstand teilen sich die Teams den besseren Platz. **Der pauschale Ausschluss von Lehrkräften wurde in Runde 4 für Singleplayer ersetzt; Mehrspieler bleibt offen.**
7. **Bleiben Ergebnisse einer früheren Klasse historisch dieser Klasse zugeordnet?** David: Nein. Der Fortschritt gehört dem Schüler; Klassenstatistiken werden nur aus den aktuell zugehörigen Schülern berechnet. Wer wegfällt oder die Klasse wechselt, zählt in der bisherigen Klasse nicht mehr mit.
8. **Sehen andere Lehrkräfte private Spiele?** David: Nein, nur der Ersteller sieht sie. **Runde 4 ergänzt Administratoren als Ausnahme.**
9. **Dürfen Fragen gelöscht werden?** David: Ja, eine Frage soll einfach gelöscht werden können. Wie historische Sessiondaten dabei technisch erhalten bleiben, ist eine Umsetzungsfrage. **Runde 4 präzisiert für eine gemeldete Frage, dass Frage und Meldung gelöscht werden.**
10. **Ist Einzelspieler frei zugänglich oder nur nach Zuweisung?** David: Jeder kann alle freigegebenen Spiele alleine spielen oder für eine Gruppe hosten. Das Wort „jeder“ war mit der Aussage „Lehrkräfte spielen grundsätzlich nicht mit“ aus Punkt 6 abzugleichen. **Runde 4 löst diesen Widerspruch für Singleplayer: Lehrkräfte dürfen alleine spielen und erhalten Fortschritt.**

## Runde 3: Letzte Präzisierungen

1. **Dürfen Schüler tatsächlich Gruppen hosten?** David: Ja. Lehrkräfte dürfen Spiele erstellen, kopieren, hosten, melden und eigene Spiele bearbeiten und löschen. Schüler dürfen öffentliche Spiele hosten und alleine spielen; beim Hosting macht die Rolle keinen Unterschied.
2. **Ein gemeinsames oder getrennte Level?** David: Alle Punkte werden zu einem Gesamtlevel zusammengezählt. Single- und Multiplayer-Statistiken bleiben getrennt.
3. **Wann werden Änderungen an einem Spiel wirksam?** David: Änderungen gehen sofort in die aktuelle Version ein. Bereits aktive Sessions spielen mit ihrer alten Version fertig.

## Runde 4: Sichtbarkeit, Rollen und Löschung

1. **Dürfen öffentliche Spiele und ihre Details ohne Anmeldung angezeigt werden?** David: Nein. Sowohl die öffentliche Spieleliste als auch die Spieldetails sind nur nach einem Login zugänglich.
2. **Welche Sichtbarkeit hat ein neu erstelltes Spiel?** David: Neue Spiele sind standardmäßig `PRIVATE`.
3. **Welche Sichtbarkeit erhalten bereits vorhandene Spiele bei der Migration?** David: Vorhandene Spiele werden bei der Migration als `PUBLIC` übernommen.
4. **Darf eine Lehrkraft ihr eigenes privates Spiel verwenden?** David: Ja. Die Ersteller-Lehrkraft darf das eigene private Spiel sowohl alleine als auch gemeinsam mit Schülern hosten. Wie eingeladene Schüler eine solche private Runde finden oder betreten, wurde damit nicht festgelegt.
5. **Welche Spielrechte hat ein Administrator?** David: Ein `ADMIN` darf im Kontext der Spielrechte alles, ausdrücklich auch auf fremde und private Spiele zugreifen. Daraus folgen keine automatisch bestätigten Administratorrechte außerhalb des Spielkontexts.
6. **Dürfen Lehrkräfte selbst spielen und Fortschritt erhalten?** David: Lehrkräfte dürfen alleine spielen und erhalten dabei Fortschritt. Ob sie auch als Teilnehmer an Mehrspieler-Sessions teilnehmen und dafür Fortschritt erhalten, wurde nicht entschieden. **Damit ist der pauschale Ausschluss aus Runde 2 ersetzt.**
7. **Was geschieht beim Löschen einer gemeldeten Frage?** David: Die Frage und die zugehörige Meldung werden gelöscht. Der bestehende Modellzweck, historische Sessions und Antworten nachvollziehbar zu erhalten, bleibt bestehen. Wie beide Anforderungen technisch miteinander vereinbart werden, wurde nicht entschieden.
8. **Woher stammt die Schulklasse beim Login?** David: Die Schulklasse stammt aus einem Claim des Schul-Keycloak-Tokens und wird beim Login übernommen. **Diese Antwort präzisiert Runde 1.1 und ersetzt dort die unklare Formulierung „über LeoCloud“.**

## Runde 5: Persistenz, Hosting, Administratoren und Punkte

1. **Welche Spieldurchläufe werden dauerhaft gespeichert?** David: Nur vollständig abgeschlossene Runden samt Resultaten. Unfertige und abgebrochene Runden werden nicht dauerhaft gespeichert. **Diese spätere Antwort ersetzt Runde 1.7 („jede Session“).** Dass eine laufende Runde bis zum Abschluss vorübergehend Zustand benötigt, ist eine technische Folgerung und keine Zusage für persistierte Zwischenstände.
2. **Spielt die hostende Person selbst mit?** David: Nein. Wer hostet, spielt nicht aktiv mit. Die früher im Entwurf vorgesehene gleichzeitige Rolle als Host und Teilnehmer entfällt. Separat ist bestätigt, dass eine Lehrkraft ihr eigenes privates Spiel allein spielen und dabei Fortschritt erhalten darf. Die frühere Formulierung „alleine hosten“ ist dadurch nicht eindeutig aufgelöst: Ob ein privates Spiel ohne Mitspieler gehostet werden kann, bleibt offen. Wie eine allein spielende Person technisch als Teilnehmer ohne Host erfasst wird, ist eine Modellannahme.
3. **Wer sind die Administratoren und dürfen sie Fortschritt erhalten?** David: Die Schul-Keycloak-IT-User `it220269`, `it220240` und `it220265` sind Administratoren. Das Backend soll diese Benutzernamen fest hinterlegt prüfen; es gibt weder eigene Admin-Logins noch eine Oberfläche oder Datenbankfunktion zum Erteilen und Entziehen dieser Rolle. David bezog sich dabei auf den Benutzernamen im `backenduser`-Array eines überprüften Tokens. Die genaue technische Claim-Auswertung und ihre Eignung als verlässliche Identitätsquelle müssen beim Implementieren am tatsächlichen Token geprüft werden. Administratoren dürfen Spiele erstellen, hosten und selbst spielen; beim aktiven Mitspielen erwerben sie Fortschritt. Allein das Hosting erzeugt keinen Fortschritt. **Damit ist die nach Runde 4 noch offene Frage nach Admin-Fortschritt beantwortet.**
4. **Wer erhält Punkte, und wie wirkt sich die Platzierung aus?** David: Alle Teilnehmer einer vollständig abgeschlossenen Runde erhalten Punkte. Die Plätze 1 bis 3 erhalten deutlich mehr Punkte. Der jeweilige Spielmodus berechnet die Punkte; eine konkrete Formel oder Zahlenwerte wurden nicht festgelegt. Wiederholungen zählen weiterhin voll. Die bisherige Gleichstandsregel bleibt bestehen; wie ein Gleichstand über der Grenze zum dritten Platz die Platzierungsboni beeinflusst, ist noch offen.
5. **Wer sieht eine gemeldete Frage und wer darf sie ändern?** Davids wörtliche Antwort: „sie wird allen angezeigt, aber nur ersteller und admins können die frage ändern.“ Sicher bestätigt ist damit: Nur Ersteller und Administratoren dürfen die Frage ändern. Das Pronomen „sie“ kann sich auf die Frage, ihre Markierung oder die Meldung beziehen. Deshalb ist ein allgemeiner Zugriff auf Meldungsdetails aus dieser Antwort **nicht** ableitbar. Die bisherige Aussage „Meldung an den Ersteller“ bleibt ein Entwurfsvorschlag, bis Sichtbarkeit und Empfänger geklärt sind. Die bestätigte Löschung von Frage und Meldung aus Runde 4 bleibt bestehen.

## Vorrang und offene Klärung für die nächste Bearbeitung

- **Ersetzt oder präzisiert:** Der pauschale Ausschluss von Lehrkräften als Spieler (R2.6) ist durch R4.6 für Singleplayer ersetzt. Die unklare Herkunft der Schulklasse aus R1.1 ist durch R4.8 präzisiert. Die Anti-Farming-Begrenzung (R1.10) und vererbte Fragenmeldungen (R1.11) bleiben durch Runde 2 verworfen.
- **Ersetzt durch Runde 5:** R1.7 über die Speicherung jeder Session gilt nicht mehr; dauerhaft gespeichert werden nur vollständig abgeschlossene Runden. Eine hostende Person nimmt nicht aktiv an derselben Runde teil. Die offene Admin-Fortschrittsfrage ist für aktives Mitspielen beantwortet: Admins erwerben dabei Fortschritt, beim bloßen Hosting nicht.
- **Weiter offen:** Ob Lehrkräfte an Mehrspieler-Sessions teilnehmen und dafür Fortschritt erhalten dürfen. Die bestätigte Rolle als Host entscheidet diese Teilnehmerfrage nicht.
- **Weiter offen:** Ob ein privates Spiel ohne Mitspieler gehostet werden kann. Das bestätigte Solo-Spiel mit Fortschritt ist davon zu unterscheiden; auch bei einer solchen Host-Runde dürfte der Host nicht aktiv mitspielen.
- **Weiter offen:** Die Punkteformel einschließlich des deutlich höheren Bonus für die Plätze 1 bis 3 und der Bonusbehandlung eines Gleichstands an der Grenze zum dritten Platz.
- **Weiter offen:** Was in R5.5 genau „allen angezeigt“ wird und welche Meldungsdetails für wen sichtbar sind. Das Änderungsrecht an der Frage liegt nur bei Ersteller und Administratoren.
- **Weiter offen:** Wie Schüler zu einer von der Ersteller-Lehrkraft gehosteten privaten Runde gelangen und welche Details eines privaten Spiels sie dabei sehen dürfen. R4.4 bestätigt die gemeinsame Runde, aber keinen allgemeinen Bibliothekszugriff auf private Spiele.
- **Technischer Konflikt, noch keine Nutzerentscheidung:** R4.7 verlangt beim Löschen einer gemeldeten Frage die Löschung von Frage und Meldung. Gleichzeitig sollen historische Sessions, Antworten und die damals verwendeten Frageversionen nachvollziehbar bleiben. Ob dafür nur die aktive Frage entfernt, die historische Version entkoppelt oder ein anderes Aufbewahrungsmodell verwendet wird, ist technisch festzulegen.
- **Technische Ausgestaltung, keine neue Nutzerentscheidung:** Der Entwurf nutzt unveränderliche Spiel- und Frageversionen sowie gegebenenfalls logisches Löschen, um R1.12, R2.9 und R3.3 zugleich zu erfüllen. Diese Konstruktion ist ein Vorschlag des Modells, nicht Davids wörtliche Vorgabe.
- **Weiter offen:** konkrete Levelkurve, Fächerliste, Administratorrechte außerhalb des Spielkontexts und weitere Punkte aus [datenmodell-v1.md](datenmodell-v1.md). Keine dieser Lücken durch Annahmen als bestätigt darstellen.
