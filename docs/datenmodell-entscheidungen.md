# Datenmodell V1: Fragerunden und fachliche Entscheidungen

Stand: 23. September 2026

Quelle: drei Fragerunden in der Codex-Aufgabe „Diplomarbeit aus Übergabe fortsetzen“ (`01a0c99d-71e7-7be0-b644-25e8234f662a`). Die Fragen sind für die Übergabe sinngemäß gekürzt, die Antworten inhaltlich wiedergegeben. Dies ist **kein** wörtliches Chatprotokoll und **keine** Aussage über bereits implementierten Code.

Die spätere Antwort hat bei einem Widerspruch Vorrang. Der daraus entstandene technische Entwurf steht in [datenmodell-v1.md](datenmodell-v1.md). Bei einer erneuten Bearbeitung sollen Supervisor und Spezialisten zuerst diese Entscheidungen prüfen, Abweichungen zum Entwurf benennen und offene Punkte nicht stillschweigend entscheiden.

## Runde 1: Grundstruktur

1. **Wie kommen Schüler und ihre Klasse ins System?** David: Die Schulklasse wird über LeoCloud übergeben; Schüler werden automatisch übernommen. Der bestehende technische Login verwendet Schul-Keycloak. Ob „LeoCloud“ hier genau diesen Identitätsweg meint, ist aus der Antwort allein nicht ableitbar.
2. **Sind Lehrkräfte bestimmten Klassen zugeordnet?** David: Jede Lehrkraft darf alle Klassen einsehen und für alle Klassen Spiele erstellen.
3. **Braucht ein Spiel neben dem Fach ein strukturiertes Thema?** David: Nein. Es soll nur nach Fach gefiltert werden; Unterthemen stehen in Titel oder Beschreibung und werden per Textsuche gefunden.
4. **Wer sieht, bearbeitet und verwendet Spiele?** David: Spiele können privat oder für alle freigegeben sein. Nur der Ersteller darf sie bearbeiten. Ein Spiel kann kopiert und angepasst werden; die Variante bleibt dem vorherigen Spiel untergeordnet, damit ähnliche Spiele in der Bibliothek zusammengefasst werden und alle verfügbaren Varianten spielbar sind.
5. **Gibt es einen gemeinsamen Fragenpool oder eigene Fragen je Spiel?** David: Jedes Spiel hat eigene Fragen. Beim Übernehmen in ein anderes Spiel wird eine neue Frage gespeichert, die verändert werden kann.
6. **Welche Fragetypen gehören in V1?** David: Wahr/Falsch, Reihenfolge und Freitext.
7. **Wird jeder Spieldurchlauf gespeichert?** David: Ja, jede Session.
8. **Einzelspieler, Teams und individuelle Ergebnisse?** David: Je nach Spiel einzeln oder meist im Team; Ergebnisse pro Spieler speichern. Zunächst sollten auch Lehrkräfte mitspielen und Fortschritt erhalten. **Diese letzte Aussage wurde in Runde 2 durch „Lehrer spielen grundsätzlich nicht mit“ ersetzt.**
9. **Wer berechnet die Punkte?** David: Der jeweilige Spielmodus automatisch.
10. **Was umfasst der Fortschritt?** David: Single- und Multiplayer getrennt auswerten: gespielte Spiele, Multiplayer-Siege und Punkte je Modus; zusätzlich ein Levelsystem. Zunächst sollten Punkte pro Spiel nur die ersten Male vergeben werden. **Die Begrenzung wurde in Runde 2 ausdrücklich verworfen.**
11. **Wer meldet fehlerhafte Fragen, und wo erscheint die Meldung?** David: Nur Lehrkräfte dürfen mit Kommentar melden; gemeldete Fragen sollen als solche gelistet werden. Zunächst sollten Meldungen auch bei übernommenen Fragen erscheinen. **Die Übernahme auf Kopien wurde in Runde 2 ausdrücklich verworfen.**
12. **Was passiert mit alten Ergebnissen nach einer Frageänderung?** David: Die alte Version soll gespeichert bleiben.

## Runde 2: Grenzfälle und Korrekturen

1. **Benötigt die Veröffentlichung einer kopierten Variante die Zustimmung des Originalerstellers?** David: Nein. Sobald ein Spiel veröffentlicht ist, darf es kopiert werden.
2. **Darf jemand ein fremdes oder veröffentlichtes Spiel bearbeiten?** David: Nur der Ersteller darf sein Spiel bearbeiten. Andere Nutzer dürfen es grundsätzlich nicht bearbeiten; Kopieren ist nur Lehrkräften erlaubt.
3. **Wandert eine Meldung mit einer kopierten Frage mit?** David: Nein, die Meldung wird nicht übernommen.
4. **Wie wird Freitext bewertet?** David: Durch exakte Übereinstimmung mit internen Regeln, die etwas Spielraum wie unterschiedliche Groß-/Kleinschreibung lassen. Weitere Toleranzen wurden nicht festgelegt.
5. **Wie genau wird wiederholtes Spielen bei Punkten begrenzt?** David: Gar nicht; die vorherige Begrenzung entfällt. Es soll jedes Mal dieselbe Punkteberechnung gelten.
6. **Wer gewinnt und wie funktionieren Gleichstände?** David: Jedes Mitglied des Siegerteams erhält die Punkte. Lehrkräfte spielen grundsätzlich nicht mit. Bei Gleichstand teilen sich die Teams den besseren Platz.
7. **Bleiben Ergebnisse einer früheren Klasse historisch dieser Klasse zugeordnet?** David: Nein. Der Fortschritt gehört dem Schüler; Klassenstatistiken werden nur aus den aktuell zugehörigen Schülern berechnet. Wer wegfällt oder die Klasse wechselt, zählt in der bisherigen Klasse nicht mehr mit.
8. **Sehen andere Lehrkräfte private Spiele?** David: Nein, nur der Ersteller sieht sie.
9. **Dürfen Fragen gelöscht werden?** David: Ja, eine Frage soll einfach gelöscht werden können. Wie historische Sessiondaten dabei technisch erhalten bleiben, ist eine Umsetzungsfrage.
10. **Ist Einzelspieler frei zugänglich oder nur nach Zuweisung?** David: Jeder kann alle freigegebenen Spiele alleine spielen oder für eine Gruppe hosten. Das Wort „jeder“ ist mit der Aussage „Lehrkräfte spielen grundsätzlich nicht mit“ aus Punkt 6 abzugleichen; siehe offene Klärung unten.

## Runde 3: Letzte Präzisierungen

1. **Dürfen Schüler tatsächlich Gruppen hosten?** David: Ja. Lehrkräfte dürfen Spiele erstellen, kopieren, hosten, melden und eigene Spiele bearbeiten und löschen. Schüler dürfen öffentliche Spiele hosten und alleine spielen; beim Hosting macht die Rolle keinen Unterschied.
2. **Ein gemeinsames oder getrennte Level?** David: Alle Punkte werden zu einem Gesamtlevel zusammengezählt. Single- und Multiplayer-Statistiken bleiben getrennt.
3. **Wann werden Änderungen an einem Spiel wirksam?** David: Änderungen gehen sofort in die aktuelle Version ein. Bereits aktive Sessions spielen mit ihrer alten Version fertig.

## Vorrang und offene Klärung für die nächste Bearbeitung

- **Ersetzt:** Lehrkräfte als fortschrittsrelevante Spieler (R1.8), Anti-Farming-Begrenzung (R1.10) und vererbte Fragenmeldungen (R1.11) sind nach den Antworten aus Runde 2 keine gültigen V1-Regeln mehr.
- **Noch widersprüchlich:** R2.10 sagt „jeder“ dürfe öffentliche Spiele alleine spielen; R2.6 sagt, Lehrkräfte spielten grundsätzlich nicht mit. Der Datenmodell-Entwurf [schließt Lehrkräfte vom Singleplayer aus](datenmodell-v1.md). Vor einer konkreten Singleplayer-Implementierung David fragen, ob Lehrkräfte nur hosten oder auch ohne gespeicherten Fortschritt alleine spielen dürfen.
- **Technische Ausgestaltung, keine neue Nutzerentscheidung:** Der Entwurf nutzt unveränderliche Spiel- und Frageversionen sowie gegebenenfalls logisches Löschen, um R1.12, R2.9 und R3.3 zugleich zu erfüllen. Diese Konstruktion ist ein Vorschlag des Modells, nicht Davids wörtliche Vorgabe.
- **Weiter offen:** konkrete Levelkurve, Fächerliste, fachliche Administratorrechte und weitere Punkte aus [datenmodell-v1.md](datenmodell-v1.md). Keine dieser Lücken durch Annahmen als bestätigt darstellen.
