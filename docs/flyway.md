# Flyway für das bestehende Datenbankschema

## Stand und Geltungsbereich

Die Migration `V1__current_legacy_schema.sql` bildet die fünf ursprünglichen Tabellen (`app_user`, `games`, `game_branches`, `questions`, `answer_options`) ab. `V2__database_model_foundation.sql` ergänzt Familien, unveränderliche Versionen, Tags, Meldungen und abgeschlossene Ergebnisse und übernimmt die vorhandenen Spiele, Fragen, Optionen und Branches in erste Versionen. Beide Migrationen liegen für PostgreSQL und H2 mit demselben fachlichen Stand und datenbankspezifischer SQL vor. Die Sichtbarkeits- und Eigentumsregeln des separaten OpenSpec-Changes sind darin nicht umgesetzt.

Die additive `V3__smart10_cards_and_answer_slots.sql` liegt ebenfalls für H2 und PostgreSQL vor. Sie ergänzt `smart10_cards` für einzelne, einer Frageversion zugeordnete Smart10-Karten mit Position und typisierter Lösung. `player_answers` erhält eine optionale Kartenreferenz und einen Antwort-Slot: Vorhandene Antworten werden auf `question` gesetzt; Smart10-Kartenantworten verwenden künftig je Karte einen eigenen Slot. Die bisherige Ein-Antwort-pro-Frage-Regel für andere Spieltypen bleibt damit erhalten. Die SQL-Regeln begrenzen Positionen und `solution_rank` auf 1–12; dass eine `ORDERING`-Frage genau zwölf Karten und die vollständige Rang-Permutation 1–12 besitzt, müssen zusätzlich Schreibadapter und Startdienst prüfen. Deren Anbindung ist noch in Umsetzung; das Vorliegen der Migration belegt weder bestandene Smart10-Tests noch einen Rollout.

Flyway legt in der lokalen H2-Datenbank und in Tests das leere Schema beim Start an. Hibernate prüft es mit `validate`; `update` ist deaktiviert. Im Produktionsprofil bleibt die automatische Flyway-Migration standardmäßig aus (`FLYWAY_PROD_MIGRATE=false`). Auch dort läuft nur Hibernate `validate`. Ein Start gegen eine leere Produktionsdatenbank kann deshalb scheitern, bis die Migration bewusst freigegeben und ausgeführt wurde.

`baseline-on-migrate`, `baseline-at-start`, `repair-at-start` und Flyway `clean` sind deaktiviert. Ein unbekanntes, nicht leeres Schema ohne Flyway-Historie wird bei eingeschalteter Migration nicht automatisch übernommen. Ein Checksum-Fehler stoppt weitere Migrationen; eine bereits angewendete Migration darf nicht nachträglich editiert werden.

## Vor einer ersten Migration einer bestehenden PostgreSQL-Datenbank

1. Wartungsfenster, getestetes Backup und Wiederherstellungsweg festlegen. Die Ziel-Datenbank, das Schema und die laufende Anwendung eindeutig identifizieren.
2. Das **tatsächliche** Schema inklusive Spaltentypen, Nullbarkeit, Primär- und Fremdschlüsseln, Identität/Sequenzen sowie vorhandene Daten gegen `src/main/resources/db/migration/postgresql/V1__current_legacy_schema.sql` prüfen. Besonders `app_user.role`, Zeichenlängen und die bestehenden String-IDs von `games` beachten. Hibernate `validate` allein ist kein vollständiger Schema- oder Datenvergleich.
3. Weicht das Schema ab, zuerst eine geprüfte Migrationsentscheidung treffen. `V1` nicht passend schreiben oder blind als erledigt markieren.
4. Nur wenn das bestehende Schema nachweislich `V1` entspricht, eine **einmalige, ausdrückliche Baseline auf Version 1** mit demselben Datenbankbenutzer und Schema ausführen. Dazu kann ein zur Projekt-Flyway-Version passender Flyway-Client mit sicher bereitgestellter Verbindung verwendet werden (`flyway -baselineVersion=1 baseline`). Die Verbindung und das Schema müssen vor Ausführung geprüft werden; Zugangsdaten gehören nicht in Kommandozeilen-Argumente, Logs oder Git. Die Baseline erzeugt nur die Flyway-Historie und ist keine Schemaänderung.
5. Danach `flyway validate` ausführen und die Version sowie den Erfolg in `flyway_schema_history` kontrollieren. Die Baseline markiert ausschließlich V1; **V2 und die inzwischen vorhandene V3 werden bei einer freigegebenen Migration anschließend tatsächlich und in Reihenfolge ausgeführt**, statt V3 nur als vorhanden oder als Baseline zu behandeln. Vor dieser Ausführung sind Vor-/Nachzählungen, Wiederherstellung, Schreibzugriffs-Umschaltung und der separat geplante Zugriffsschutz abzugleichen. `FLYWAY_PROD_MIGRATE=true` darf erst im ausdrücklich freigegebenen Rollout gesetzt werden. Erst dann startet die Anwendung mit Flyway-Migration und anschließender Hibernate-Prüfung.

Die Baseline wurde **nicht** auf einer produktiven Datenbank ausgeführt. Diese Anleitung ist kein Beleg für den aktuellen LeoCloud-Datenbankstand oder einen Rollout.

## Lokale Prüfung

Im Projektverzeichnis kann der Backend-Start mit H2 und Flyway sowie der Testlauf so geprüft werden:

```sh
./mvnw quarkus:dev
./mvnw test
```

Die PostgreSQL-Tests laufen zusätzlich gegen ein ausschließlich lokales, wegwerfbares PostgreSQL mit Datenbank und Benutzer `flyway_test`:

```sh
./mvnw test '-Dflyway.test.pg-url=jdbc:postgresql://127.0.0.1:<PORT>/flyway_test'
```

`<PORT>` ist durch den tatsächlichen lokalen Port zu ersetzen. Die Tests legen eigene Schemas an; sie lehnen andere Datenbanknamen und entfernte Hosts ab.

Die bisherigen Foundation-Migrationstests prüfen eine neue Datenbank, einen zweiten unveränderten Lauf, bestehende Datensätze und Reihenfolgen, die Verweigerung eines unbekannten Schemas, eine bewusst aufgerufene Baseline und die Checksum-Prüfung. Für den Baseline-Test erzeugt Hibernate das ursprüngliche Schema aus **eingefrorenen V1-Test-Mappings** in einer eigenen H2-Datenbank beziehungsweise einem eigenen PostgreSQL-Testschema; Flyway legt dieses Schema zuvor nicht an. Nach dem Einfügen repräsentativer Datensätze muss `migrate` ohne Historie scheitern. Die ausdrückliche Baseline auf Version 1 muss anschließend V2 und V3 tatsächlich ausführen. Testmigrationen prüfen außerdem, dass ein vorhandener `PRIVATE`-Wert bei beiden Reihenfolgen einer additiven Sichtbarkeitsmigration erhalten bleibt. Diese Fixture implementiert den separaten Access-Change nicht und ersetzt keine Prüfung des konkreten Zielschemas vor einer produktiven Migration.

Für die Smart10-Migrationsabnahme ist zusätzlich eine **mit generischen Antworten befüllte V2-Datenbank** auf H2 und einer lokalen Wegwerf-PostgreSQL-Datenbank nach V3 zu migrieren. Vorher und nachher sind Antwort-IDs, Frage- und Teilnehmerbezüge, Inhalte, Richtigkeiten, Zeitpunkte und Zeilenzahlen zu vergleichen; alle übernommenen Antworten brauchen danach `answer_slot_key='question'` und keine Kartenreferenz. Zu prüfen sind außerdem die unveränderte Ein-Antwort-Regel anderer Spieltypen, mehrere verschiedene Kartenantworten derselben Smart10-Frage, Fremd- und Doppelkarten sowie die SQL-Grenzen für Position und Rang. Der Smart10-Adapter und der Startdienst müssen `ORDERING` mit fehlendem, doppeltem oder außerhalb 1–12 liegendem Rang ablehnen. Diese Abnahme ist eine Anforderung an die laufende Implementierung; ihre Ergebnisse und ein abschließendes Review werden erst in der Projektübergabe festgehalten.

Neue Änderungen erhalten die nächste fortlaufende Nummer und eine aussagekräftige Bezeichnung, nach der vorhandenen V3 beispielsweise `V4__add_example_column.sql`. Jede Version liegt unter **beiden** Verzeichnissen `db/migration/h2/` und `db/migration/postgresql/`, setzt denselben fachlichen Beschluss um und wird auf beiden Datenbanken geprüft. Bereits angewendete V1/V2-Dateien werden nie umgeschrieben; Korrekturen erfolgen in einer neuen Migration.

Referenz: [Quarkus 3.15 Flyway Guide](https://quarkus.io/version/3.15/guides/flyway/).
