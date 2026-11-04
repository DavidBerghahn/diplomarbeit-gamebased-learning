package at.htlleonding.gamebasedlearning.games;

import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Internal persistence boundary for already finalized, trusted game-service results.
 * No HTTP or WebSocket route accepts this payload.
 */
@ApplicationScoped
public class CompletedRoundStore {
    @Inject
    EntityManager entityManager;

    public enum State { COMPLETED, RUNNING, ABORTED }

    public record Answer(UUID questionVersionId, UUID smart10CardId, String answerData,
                         boolean correct, Instant answeredAt) {
        public Answer(UUID questionVersionId, String answerData, boolean correct, Instant answeredAt) {
            this(questionVersionId, null, answerData, correct, answeredAt);
        }
    }
    public record Participant(UUID userId, String teamName, int score, Integer placement,
                              Instant joinedAt, List<Answer> answers) { }
    public record Team(String name, int score, Integer placement) { }
    public record Round(UUID completionKey, State state, String gameId, UUID gameVersionId,
                        UUID hostUserId, String mode, Instant startedAt, Instant finishedAt,
                        List<Team> teams, List<Participant> participants) { }

    public UUID save(Round round) {
        return saveInternal(round, false);
    }

    UUID saveSmart10(Round round) {
        return saveInternal(round, true);
    }

    private UUID saveInternal(Round round, boolean trustedSmart10) {
        validateShape(round, trustedSmart10);
        String hash = fingerprint(round);
        UUID existing = QuarkusTransaction.requiringNew().call(() -> readMatching(round.completionKey(), hash));
        if (existing != null) return existing;
        try {
            return QuarkusTransaction.requiringNew().call(() -> insert(round, hash, trustedSmart10));
        } catch (RuntimeException original) {
            // A unique-key winner is committed only after the failed transaction has rolled back.
            UUID winner = QuarkusTransaction.requiringNew().call(() -> readMatching(round.completionKey(), hash));
            if (winner != null) return winner;
            throw original;
        }
    }

    public long totalPoints(UUID userId) {
        return QuarkusTransaction.requiringNew().call(() -> ((Number) entityManager.createNativeQuery(
                "select coalesce(sum(score), 0) from session_participants where user_id = ?1")
                .setParameter(1, userId).getSingleResult()).longValue());
    }

    public List<UUID> winnerUsers(UUID sessionId) {
        return QuarkusTransaction.requiringNew().call(() -> {
            List<?> ids = entityManager.createNativeQuery(
                    "select p.user_id from session_participants p left join teams t on t.id = p.team_id "
                    + "where p.session_id = ?1 and ((p.team_id is null and p.placement = 1) "
                    + "or (p.team_id is not null and t.placement = 1)) order by p.user_id")
                    .setParameter(1, sessionId).getResultList();
            return ids.stream().map(this::asUuid).toList();
        });
    }

    private UUID readMatching(UUID key, String hash) {
        List<Object[]> rows = entityManager.createNativeQuery(
                "select id, payload_hash from game_sessions where completion_key = ?1")
                .setParameter(1, key).getResultList();
        if (rows.isEmpty()) return null;
        Object[] row = rows.getFirst();
        if (!hash.equals(row[1])) throw new IllegalArgumentException("Completion key belongs to different result");
        return asUuid(row[0]);
    }

    private UUID insert(Round round, String hash, boolean trustedSmart10) {
        validateReferences(round, trustedSmart10);
        UUID sessionId = UUID.randomUUID();
        entityManager.createNativeQuery("insert into game_sessions "
                        + "(id, completion_key, payload_hash, game_id, game_version_id, host_user_id, mode, started_at, finished_at, created_at) "
                        + "values (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10)")
                .setParameter(1, sessionId).setParameter(2, round.completionKey()).setParameter(3, hash)
                .setParameter(4, round.gameId()).setParameter(5, round.gameVersionId())
                .setParameter(6, round.hostUserId()).setParameter(7, round.mode())
                .setParameter(8, round.startedAt()).setParameter(9, round.finishedAt())
                .setParameter(10, Instant.now()).executeUpdate();
        entityManager.flush();

        java.util.Map<String, UUID> teamIds = new java.util.HashMap<>();
        for (Team team : round.teams()) {
            UUID id = UUID.randomUUID();
            teamIds.put(team.name(), id);
            entityManager.createNativeQuery("insert into teams (id, session_id, name, score, placement) "
                            + "values (?1, ?2, ?3, ?4, ?5)")
                    .setParameter(1, id).setParameter(2, sessionId).setParameter(3, team.name())
                    .setParameter(4, team.score()).setParameter(5, team.placement()).executeUpdate();
        }
        for (Participant participant : round.participants()) {
            UUID id = UUID.randomUUID();
            entityManager.createNativeQuery("insert into session_participants "
                            + "(id, session_id, user_id, team_id, score, placement, joined_at) "
                            + "values (?1, ?2, ?3, ?4, ?5, ?6, ?7)")
                    .setParameter(1, id).setParameter(2, sessionId).setParameter(3, participant.userId())
                    .setParameter(4, participant.teamName() == null ? null : teamIds.get(participant.teamName()))
                    .setParameter(5, participant.score()).setParameter(6, participant.placement())
                    .setParameter(7, participant.joinedAt()).executeUpdate();
            for (Answer answer : participant.answers()) {
                entityManager.createNativeQuery("insert into player_answers "
                                + "(id, participant_id, question_version_id, smart10_card_id, answer_slot_key, "
                                + "answer_data, correct, answered_at) values (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8)")
                        .setParameter(1, UUID.randomUUID()).setParameter(2, id)
                        .setParameter(3, answer.questionVersionId()).setParameter(4, answer.smart10CardId())
                        .setParameter(5, slot(answer)).setParameter(6, answer.answerData())
                        .setParameter(7, answer.correct()).setParameter(8, answer.answeredAt()).executeUpdate();
            }
        }
        entityManager.flush();
        return sessionId;
    }

    private String slot(Answer answer) {
        return answer.smart10CardId() == null ? "question" : "card:" + answer.smart10CardId();
    }

    private void validateShape(Round round, boolean trustedSmart10) {
        if (round == null || round.state() != State.COMPLETED
                || round.completionKey() == null || round.gameId() == null || round.gameVersionId() == null
                || (!"SINGLEPLAYER".equals(round.mode()) && !"MULTIPLAYER".equals(round.mode()))
                || round.startedAt() == null || round.finishedAt() == null
                || round.finishedAt().isBefore(round.startedAt())
                || round.teams() == null || round.participants() == null || round.participants().isEmpty()) {
            throw new IllegalArgumentException("Only fully completed rounds can be persisted");
        }
        if ("SINGLEPLAYER".equals(round.mode())
                && (round.participants().size() != 1 || round.hostUserId() != null || !round.teams().isEmpty()))
            throw new IllegalArgumentException("Singleplayer requires one participant without host or teams");
        Set<String> teams = new HashSet<>();
        for (Team team : round.teams()) {
            if (team == null || team.name() == null || team.name().isBlank() || !teams.add(team.name())
                    || team.placement() == null || team.placement() <= 0)
                throw new IllegalArgumentException("Invalid team result");
        }
        Set<UUID> users = new HashSet<>();
        for (Participant participant : round.participants()) {
            if (participant == null || participant.userId() == null || !users.add(participant.userId())
                    || participant.userId().equals(round.hostUserId())
                    || participant.score() < (trustedSmart10 ? 0 : 1)
                    || participant.placement() == null || participant.placement() <= 0
                    || participant.joinedAt() == null || participant.answers() == null
                    || (participant.teamName() != null && !teams.contains(participant.teamName()))
                    || (!teams.isEmpty() && participant.teamName() == null)
                    || (teams.isEmpty() && participant.teamName() != null))
                throw new IllegalArgumentException("Invalid participant result");
            Set<String> answered = new HashSet<>();
            for (Answer answer : participant.answers()) {
                if (answer == null || answer.questionVersionId() == null
                        || !answered.add(answer.questionVersionId() + ":" + slot(answer))
                        || answer.answerData() == null || answer.answeredAt() == null)
                    throw new IllegalArgumentException("Invalid answer");
            }
        }
    }

    private void validateReferences(Round round, boolean trustedSmart10) {
        List<?> types = entityManager.createNativeQuery(
                "select game_type from game_versions where game_id = ?1 and id = ?2")
                .setParameter(1, round.gameId()).setParameter(2, round.gameVersionId()).getResultList();
        if (types.size() != 1)
            throw new IllegalArgumentException("Game version does not belong to game");
        boolean smart10 = "Smart10".equals(types.getFirst());
        if (trustedSmart10 && !smart10) throw new IllegalArgumentException("Verified Smart10 version required");
        if (round.hostUserId() != null) requireActiveUser(round.hostUserId());
        for (Participant participant : round.participants()) {
            requireActiveUser(participant.userId());
            for (Answer answer : participant.answers()) {
                if (((Number) entityManager.createNativeQuery(
                        "select count(*) from game_version_questions where game_version_id = ?1 and question_version_id = ?2")
                        .setParameter(1, round.gameVersionId()).setParameter(2, answer.questionVersionId())
                        .getSingleResult()).intValue() == 0)
                    throw new IllegalArgumentException("Answer question outside session snapshot");
                if (smart10) {
                    if (answer.smart10CardId() == null || ((Number) entityManager.createNativeQuery(
                            "select count(*) from smart10_cards where question_version_id = ?1 and id = ?2")
                            .setParameter(1, answer.questionVersionId()).setParameter(2, answer.smart10CardId())
                            .getSingleResult()).intValue() != 1)
                        throw new IllegalArgumentException("Smart10 answer must reference a card in the snapshot");
                } else if (answer.smart10CardId() != null) {
                    throw new IllegalArgumentException("Non-Smart10 answer cannot reference a card");
                }
            }
        }
    }

    private void requireActiveUser(UUID id) {
        if (((Number) entityManager.createNativeQuery(
                "select count(*) from app_user where id = ?1 and active = true")
                .setParameter(1, id).getSingleResult()).intValue() != 1)
            throw new IllegalArgumentException("Verified active user required");
    }

    private String fingerprint(Round round) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            value(out, round.gameId());
            value(out, round.gameVersionId());
            value(out, round.hostUserId());
            value(out, round.mode());
            value(out, round.startedAt());
            value(out, round.finishedAt());
            List<Team> teams = new ArrayList<>(round.teams());
            teams.sort(Comparator.comparing(Team::name));
            out.writeInt(teams.size());
            for (Team team : teams) {
                value(out, team.name());
                out.writeInt(team.score());
                value(out, team.placement());
            }
            List<Participant> participants = new ArrayList<>(round.participants());
            participants.sort(Comparator.comparing(p -> p.userId().toString()));
            out.writeInt(participants.size());
            for (Participant participant : participants) {
                value(out, participant.userId());
                value(out, participant.teamName());
                out.writeInt(participant.score());
                value(out, participant.placement());
                value(out, participant.joinedAt());
                List<Answer> answers = new ArrayList<>(participant.answers());
                answers.sort(Comparator.comparing((Answer a) -> a.questionVersionId().toString())
                        .thenComparing(this::slot));
                out.writeInt(answers.size());
                for (Answer answer : answers) {
                    value(out, answer.questionVersionId());
                    value(out, answer.smart10CardId());
                    value(out, slot(answer));
                    value(out, answer.answerData());
                    out.writeBoolean(answer.correct());
                    value(out, answer.answeredAt());
                }
            }
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private void value(DataOutputStream out, Object value) throws IOException {
        if (value == null) {
            out.writeInt(-1);
        } else {
            byte[] bytes = value.toString().getBytes(StandardCharsets.UTF_8);
            out.writeInt(bytes.length);
            out.write(bytes);
        }
    }

    private UUID asUuid(Object value) {
        if (value instanceof UUID uuid) return uuid;
        if (value instanceof byte[] bytes) {
            java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(bytes);
            return new UUID(buffer.getLong(), buffer.getLong());
        }
        return UUID.fromString(value.toString());
    }
}
