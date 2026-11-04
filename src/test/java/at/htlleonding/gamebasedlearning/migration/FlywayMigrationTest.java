package at.htlleonding.gamebasedlearning.migration;

import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class FlywayMigrationTest {
    @Test
    void h2FreshMigrationIsRepeatable() throws SQLException {
        verifyFreshMigration(h2());
    }

    @Test
    void h2RefusesUnknownNonEmptySchema() throws SQLException {
        verifyUnknownSchemaIsRejected(h2());
    }

    @Test
    void h2ExplicitBaselinePreservesKnownLegacyRows() throws SQLException {
        verifyExplicitBaseline(h2());
    }

    @Test
    void h2DetectsChecksumChanges() throws SQLException {
        verifyChecksumValidation(h2());
    }

    @Test
    void postgresFreshMigrationIsRepeatable() throws SQLException {
        verifyFreshMigration(postgres());
    }

    @Test
    void postgresRefusesUnknownNonEmptySchema() throws SQLException {
        verifyUnknownSchemaIsRejected(postgres());
    }

    @Test
    void postgresExplicitBaselinePreservesKnownLegacyRows() throws SQLException {
        verifyExplicitBaseline(postgres());
    }

    @Test
    void postgresDetectsChecksumChanges() throws SQLException {
        verifyChecksumValidation(postgres());
    }

    @Test
    void h2PreservesPrivateVisibilityInBothMigrationOrders() throws SQLException {
        verifyVisibilityOrders(this::h2);
    }

    @Test
    void postgresPreservesPrivateVisibilityInBothMigrationOrders() throws SQLException {
        verifyVisibilityOrders(this::postgres);
    }

    @Test
    void h2PreservesUnknownLegacyQuestionType() throws SQLException {
        verifyUnknownType(h2());
    }

    @Test
    void postgresPreservesUnknownLegacyQuestionType() throws SQLException {
        verifyUnknownType(postgres());
    }

    @Test
    void h2EnforcesResultReferences() throws SQLException {
        verifyResultConstraints(h2());
    }

    @Test
    void postgresEnforcesResultReferences() throws SQLException {
        verifyResultConstraints(postgres());
    }

    private void verifyResultConstraints(Fixture fixture) throws SQLException {
        createLegacySchemaWithHibernate(fixture);
        insertLegacyRows(fixture);
        fixture.flyway().baseline();
        fixture.flyway().migrate();
        String games = fixture.table("games");
        String sessions = fixture.table("game_sessions");
        String teams = fixture.table("teams");
        String participants = fixture.table("session_participants");
        String firstSession = "10000000-0000-0000-0000-000000000001";
        String secondSession = "10000000-0000-0000-0000-000000000002";
        String firstTeam = "20000000-0000-0000-0000-000000000001";
        String otherTeam = "20000000-0000-0000-0000-000000000002";
        assertThrows(SQLException.class, () -> execute(fixture, "INSERT INTO " + sessions
                + " (id, completion_key, payload_hash, game_id, game_version_id, mode, started_at, finished_at, created_at) "
                + "SELECT '" + firstSession + "', '30000000-0000-0000-0000-000000000001', 'hash', "
                + "'ownerless-game', current_version_id, 'TEAM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP "
                + "FROM " + games + " WHERE id = 'legacy-game'"));
        execute(fixture, "INSERT INTO " + sessions
                + " (id, completion_key, payload_hash, game_id, game_version_id, mode, started_at, finished_at, created_at) "
                + "SELECT '" + firstSession + "', '30000000-0000-0000-0000-000000000001', 'hash', "
                + "id, current_version_id, 'TEAM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP "
                + "FROM " + games + " WHERE id = 'legacy-game'");
        execute(fixture, "INSERT INTO " + sessions
                + " (id, completion_key, payload_hash, game_id, game_version_id, mode, started_at, finished_at, created_at) "
                + "SELECT '" + secondSession + "', '30000000-0000-0000-0000-000000000002', 'hash', "
                + "id, current_version_id, 'TEAM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP "
                + "FROM " + games + " WHERE id = 'ownerless-game'");
        execute(fixture, "INSERT INTO " + teams + " (id, session_id, name, score) VALUES "
                + "('" + firstTeam + "', '" + firstSession + "', 'A', 1), "
                + "('" + otherTeam + "', '" + secondSession + "', 'B', 1)");
        execute(fixture, "INSERT INTO " + participants
                + " (id, session_id, user_id, team_id, score, joined_at) VALUES "
                + "('40000000-0000-0000-0000-000000000001', '" + firstSession + "', "
                + "'00000000-0000-0000-0000-000000000001', '" + firstTeam + "', 1, CURRENT_TIMESTAMP)");
        assertThrows(SQLException.class, () -> execute(fixture, "INSERT INTO " + participants
                + " (id, session_id, user_id, team_id, score, joined_at) VALUES "
                + "('40000000-0000-0000-0000-000000000002', '" + firstSession + "', "
                + "'00000000-0000-0000-0000-000000000001', '" + firstTeam + "', 1, CURRENT_TIMESTAMP)"));
        execute(fixture, "INSERT INTO " + fixture.table("app_user")
                + " (id, active, created_at, updated_at, username, display_name, external_subject, role) "
                + "VALUES ('00000000-0000-0000-0000-000000000002', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, "
                + "'other', 'Other', 'other-subject', 'STUDENT')");
        assertThrows(SQLException.class, () -> execute(fixture, "INSERT INTO " + participants
                + " (id, session_id, user_id, team_id, score, joined_at) VALUES "
                + "('40000000-0000-0000-0000-000000000003', '" + firstSession + "', "
                + "'00000000-0000-0000-0000-000000000002', '" + otherTeam + "', 1, CURRENT_TIMESTAMP)"));
        assertEquals(1, scalar(fixture, "SELECT COUNT(*) FROM " + participants
                + " WHERE session_id = '" + firstSession + "'"));
    }

    private void verifyUnknownType(Fixture fixture) throws SQLException {
        createLegacySchemaWithHibernate(fixture);
        insertLegacyRows(fixture);
        execute(fixture, "INSERT INTO " + fixture.table("questions")
                + " (id, game_id, question_order, type, text) "
                + "VALUES (42, 'ownerless-game', 0, 'old_custom_type', 'Unbekannte Frage')");
        fixture.flyway().baseline();
        assertEquals(3, fixture.flyway().migrate().migrationsExecuted);
        assertEquals(1, scalar(fixture, "SELECT COUNT(*) FROM " + fixture.table("question_versions")
                + " WHERE question_id = 42 AND type = 'old_custom_type' AND text = 'Unbekannte Frage'"));
        assertEquals(1, scalar(fixture, "SELECT COUNT(*) FROM " + fixture.table("game_version_questions")
                + " WHERE game_version_id = (SELECT current_version_id FROM " + fixture.table("games")
                + " WHERE id = 'ownerless-game')"));
    }

    private void verifyVisibilityOrders(FixtureFactory factory) throws SQLException {
        for (String accessLocation : List.of("access-before", "access-after")) {
            Fixture fixture = factory.create();
            createLegacySchemaWithHibernate(fixture);
            insertLegacyRows(fixture);
            fixture.flyway().baseline();
            Flyway ordered = Flyway.configure()
                    .dataSource(fixture.url(), fixture.username(), fixture.password())
                    .schemas(fixture.schema())
                    .defaultSchema(fixture.schema())
                    .locations(fixture.location(), accessLocation)
                    .baselineVersion("1")
                    .baselineOnMigrate(false)
                    .cleanDisabled(true)
                    .load();
            assertEquals(4, ordered.migrate().migrationsExecuted);
            assertEquals(1, scalar(fixture, "SELECT COUNT(*) FROM " + fixture.table("games")
                    + " WHERE id = 'legacy-game' AND visibility = 'PRIVATE'"));
            assertLegacyRows(fixture);
            assertTrue(ordered.validateWithResult().validationSuccessful);
        }
    }

    @FunctionalInterface
    private interface FixtureFactory {
        Fixture create() throws SQLException;
    }

    private void verifyFreshMigration(Fixture fixture) throws SQLException {
        assertEquals(4, fixture.flyway().migrate().migrationsExecuted);
        assertEquals(0, fixture.flyway().migrate().migrationsExecuted);
        assertTrue(fixture.flyway().validateWithResult().validationSuccessful);
    }

    private void verifyUnknownSchemaIsRejected(Fixture fixture) throws SQLException {
        execute(fixture, "CREATE TABLE " + fixture.table("legacy_marker") + " (id INTEGER PRIMARY KEY)");
        execute(fixture, "INSERT INTO " + fixture.table("legacy_marker") + " (id) VALUES (1)");

        assertThrows(org.flywaydb.core.api.FlywayException.class, () -> fixture.flyway().migrate());
        assertEquals(1, scalar(fixture, "SELECT COUNT(*) FROM " + fixture.table("legacy_marker")));
    }

    private void verifyExplicitBaseline(Fixture fixture) throws SQLException {
        // Produce the pre-V2 schema from frozen legacy mappings, independently of Flyway V1.
        createLegacySchemaWithHibernate(fixture);
        insertLegacyRows(fixture);

        assertThrows(org.flywaydb.core.api.FlywayException.class, () -> fixture.flyway().migrate());
        fixture.flyway().baseline();
        assertEquals(3, fixture.flyway().migrate().migrationsExecuted);
        assertLegacyRows(fixture);
        assertEquals(2, scalar(fixture, "SELECT COUNT(*) FROM " + fixture.table("game_versions")));
        assertEquals(2, scalar(fixture, "SELECT COUNT(*) FROM " + fixture.table("game_families")));
        assertEquals(1, scalar(fixture, "SELECT COUNT(*) FROM " + fixture.table("question_versions")));
        assertEquals(1, scalar(fixture, "SELECT COUNT(*) FROM " + fixture.table("tags")
                + " WHERE name = 'Programmieren'"));
        assertEquals(List.of("SSE", "DDP"), orderedStrings(fixture,
                "SELECT branch FROM " + fixture.table("game_version_branches") + " ORDER BY position"));
        assertEquals(1, scalar(fixture, "SELECT COUNT(*) FROM " + fixture.table("game_versions")
                + " WHERE game_id = 'ownerless-game' AND game_type = 'Smart10' AND title = 'Ohne Besitzer'"));
        assertEquals(1, scalar(fixture, "SELECT COUNT(*) FROM " + fixture.table("question_versions")
                + " WHERE question_id = 41 AND type = 'single_choice' AND text = 'Welche Antwort?'"));
        assertTrue(fixture.flyway().validateWithResult().validationSuccessful);
        String schema = fixture.schema();
        assertEquals(0, scalar(fixture, "SELECT COUNT(*) FROM information_schema.columns WHERE "
                + "LOWER(table_schema) = LOWER('" + schema + "') AND LOWER(table_name) = 'question_reports' "
                + "AND LOWER(column_name) = 'question_id'"));
        assertEquals(0, scalar(fixture, "SELECT COUNT(*) FROM information_schema.columns WHERE "
                + "LOWER(table_schema) = LOWER('" + schema + "') AND LOWER(table_name) = 'player_answers' "
                + "AND LOWER(column_name) IN ('score', 'response_time_ms')"));
        assertEquals(0, scalar(fixture, "SELECT COUNT(*) FROM information_schema.columns WHERE "
                + "LOWER(table_schema) = LOWER('" + schema + "') AND LOWER(table_name) = 'session_participants' "
                + "AND LOWER(column_name) = 'winner'"));
        execute(fixture, "INSERT INTO " + fixture.table("questions")
                + " (game_id, type, text) VALUES ('legacy-game', 'single_choice', 'Neue Frage')");
        execute(fixture, "INSERT INTO " + fixture.table("answer_options")
                + " (question_id, text, is_correct) VALUES (41, 'Neue Option', FALSE)");
        assertEquals(42, scalar(fixture, "SELECT MAX(id) FROM " + fixture.table("questions")));
        assertEquals(75, scalar(fixture, "SELECT MAX(id) FROM " + fixture.table("answer_options")));
    }

    private void createLegacySchemaWithHibernate(Fixture fixture) {
        // Every fixture owns an isolated in-memory database or a unique schema in disposable local PostgreSQL.
        String connectionUrl = fixture.url().startsWith("jdbc:postgresql:")
                ? fixture.url() + "?currentSchema=" + fixture.schema()
                : fixture.url();
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.connection.driver_class", fixture.url().startsWith("jdbc:postgresql:")
                        ? "org.postgresql.Driver" : "org.h2.Driver")
                .applySetting("hibernate.connection.url", connectionUrl)
                .applySetting("hibernate.connection.username", fixture.username())
                .applySetting("hibernate.connection.password", fixture.password())
                .applySetting("hibernate.dialect", fixture.url().startsWith("jdbc:postgresql:")
                        ? "org.hibernate.dialect.PostgreSQLDialect" : "org.hibernate.dialect.H2Dialect")
                .applySetting("hibernate.default_schema", fixture.schema())
                .applySetting("hibernate.hbm2ddl.auto", "none")
                .build();
        try {
            try (SessionFactory sessionFactory = new MetadataSources(registry)
                    .addAnnotatedClass(LegacyMappings.User.class)
                    .addAnnotatedClass(LegacyMappings.Game.class)
                    .addAnnotatedClass(LegacyMappings.Question.class)
                    .addAnnotatedClass(LegacyMappings.Option.class)
                    .buildMetadata()
                    .buildSessionFactory()) {
                sessionFactory.getSchemaManager().exportMappedObjects(false);
                sessionFactory.getSchemaManager().validateMappedObjects();
            }
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    private void verifyChecksumValidation(Fixture fixture) throws SQLException {
        fixture.flyway().migrate();
        execute(fixture, "UPDATE " + fixture.table("flyway_schema_history")
                + " SET \"checksum\" = \"checksum\" + 1 WHERE \"version\" = '1'");

        assertFalse(fixture.flyway().validateWithResult().validationSuccessful);
        assertThrows(org.flywaydb.core.api.FlywayException.class, () -> fixture.flyway().migrate());
    }

    private void insertLegacyRows(Fixture fixture) throws SQLException {
        execute(fixture, "INSERT INTO " + fixture.table("app_user") + " (id, active, created_at, updated_at, "
                + "username, display_name, external_subject, role) VALUES "
                + "('00000000-0000-0000-0000-000000000001', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, "
                + "'legacy-user', 'Legacy User', 'legacy-subject', 'TEACHER')");
        execute(fixture, "INSERT INTO " + fixture.table("games") + " (id, game_type, teacher_name, "
                + "created_by_id, name, subject, description, played_rounds) VALUES "
                + "('legacy-game', 'Quizbattle', 'Legacy Teacher', "
                + "'00000000-0000-0000-0000-000000000001', 'Legacy Game', 'Programmieren', 'Altbestand', 3)");
        execute(fixture, "INSERT INTO " + fixture.table("games") + " (id, game_type, teacher_name, name, subject) "
                + "VALUES ('ownerless-game', 'Smart10', 'Unbekannt', 'Ohne Besitzer', 'Programmieren')");
        execute(fixture, "INSERT INTO " + fixture.table("game_branches") + " (game_id, branch_order, branch) "
                + "VALUES ('legacy-game', 0, 'SSE'), ('legacy-game', 1, 'DDP')");
        execute(fixture, "INSERT INTO " + fixture.table("questions") + " (id, game_id, question_order, type, text) "
                + "VALUES (41, 'legacy-game', 0, 'single_choice', 'Welche Antwort?')");
        execute(fixture, "INSERT INTO " + fixture.table("answer_options")
                + " (id, question_id, answer_order, is_correct, text) VALUES "
                + "(73, 41, 0, TRUE, 'Richtig'), (74, 41, 1, FALSE, 'Falsch')");
    }

    private void assertLegacyRows(Fixture fixture) throws SQLException {
        assertEquals(1, scalar(fixture, "SELECT COUNT(*) FROM " + fixture.table("app_user")));
        assertEquals(2, scalar(fixture, "SELECT COUNT(*) FROM " + fixture.table("games")));
        assertEquals(1, scalar(fixture, "SELECT COUNT(*) FROM " + fixture.table("questions")));
        assertEquals(2, scalar(fixture, "SELECT COUNT(*) FROM " + fixture.table("game_branches")));
        assertEquals(2, scalar(fixture, "SELECT COUNT(*) FROM " + fixture.table("answer_options")));
        assertEquals(1, scalar(fixture, "SELECT COUNT(*) FROM " + fixture.table("games")
                + " WHERE id = 'legacy-game' AND game_type = 'Quizbattle' AND subject = 'Programmieren' "
                + "AND created_by_id = '00000000-0000-0000-0000-000000000001'"));
        assertEquals(1, scalar(fixture, "SELECT COUNT(*) FROM " + fixture.table("games")
                + " WHERE id = 'ownerless-game' AND created_by_id IS NULL"));
        assertEquals(1, scalar(fixture, "SELECT COUNT(*) FROM " + fixture.table("questions")
                + " WHERE id = 41 AND game_id = 'legacy-game' AND question_order = 0 "
                + "AND type = 'single_choice' AND text = 'Welche Antwort?'"));
        assertEquals(List.of("SSE", "DDP"), orderedStrings(fixture,
                "SELECT branch FROM " + fixture.table("game_branches")
                        + " WHERE game_id = 'legacy-game' ORDER BY branch_order"));
        assertEquals(List.of("73:true:Richtig", "74:false:Falsch"), orderedOptions(fixture));
    }

    private List<String> orderedStrings(Fixture fixture, String sql) throws SQLException {
        List<String> values = new ArrayList<>();
        try (Connection connection = fixture.connect();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(sql)) {
            while (rows.next()) {
                values.add(rows.getString(1));
            }
        }
        return values;
    }

    private List<String> orderedOptions(Fixture fixture) throws SQLException {
        List<String> values = new ArrayList<>();
        try (Connection connection = fixture.connect();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT id, is_correct, text FROM "
                     + fixture.table("answer_options") + " WHERE question_id = 41 ORDER BY answer_order")) {
            while (rows.next()) {
                values.add(rows.getLong(1) + ":" + rows.getBoolean(2) + ":" + rows.getString(3));
            }
        }
        return values;
    }

    private int scalar(Fixture fixture, String sql) throws SQLException {
        try (Connection connection = fixture.connect();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(sql)) {
            rows.next();
            return rows.getInt(1);
        }
    }

    private void execute(Fixture fixture, String sql) throws SQLException {
        try (Connection connection = fixture.connect(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private Fixture h2() {
        String url = "jdbc:h2:mem:flyway_" + UUID.randomUUID().toString().replace("-", "")
                + ";DB_CLOSE_DELAY=-1";
        return fixture(url, "sa", "", "PUBLIC", "db/migration/h2");
    }

    private Fixture postgres() throws SQLException {
        String url = System.getProperty("flyway.test.pg-url");
        assumeTrue(url != null && url.matches(
                "^jdbc:postgresql://(?:127\\.0\\.0\\.1|localhost):[0-9]+/flyway_test$"),
                "Only the local disposable flyway_test PostgreSQL database is allowed");
        String schema = "flyway_coder_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection connection = DriverManager.getConnection(url, "flyway_test", "");
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
        }
        return fixture(url, "flyway_test", "", schema, "db/migration/postgresql");
    }

    private Fixture fixture(String url, String username, String password, String schema, String location) {
        Flyway flyway = Flyway.configure()
                .dataSource(url, username, password)
                .schemas(schema)
                .defaultSchema(schema)
                .locations(location)
                .baselineVersion("1")
                .baselineOnMigrate(false)
                .cleanDisabled(true)
                .load();
        return new Fixture(url, username, password, schema, location, flyway);
    }

    private record Fixture(String url, String username, String password, String schema, String location, Flyway flyway) {
        private Connection connect() throws SQLException {
            return DriverManager.getConnection(url, username, password);
        }

        private String table(String name) {
            return schema + "." + ("flyway_schema_history".equals(name) ? "\"flyway_schema_history\"" : name);
        }
    }
}
