package at.htlleonding.gamebasedlearning.games;

import at.htlleonding.gamebasedlearning.users.AppUser;
import at.htlleonding.gamebasedlearning.users.UserRole;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class GameRepository {
    @Inject
    EntityManager entityManager;

    public List<Game> findAll() {
        return entityManager.createQuery("select g from Game g where g.deletedAt is null order by g.gameType, g.name", Game.class)
                .getResultList().stream().map(this::currentSnapshot).toList();
    }

    public List<Game> findByType(String gameType) {
        return entityManager.createQuery("select g from Game g where g.deletedAt is null and g.gameType = :type order by g.name", Game.class)
                .setParameter("type", gameType).getResultList().stream().map(this::currentSnapshot).toList();
    }

    public Optional<Game> findById(String id) {
        List<Game> games = entityManager.createQuery(
                "select g from Game g where g.id = :id and g.deletedAt is null", Game.class)
                .setParameter("id", id).getResultList();
        return games.isEmpty() ? Optional.empty() : Optional.of(currentSnapshot(games.getFirst()));
    }

    @Transactional
    public Game create(Game input, AppUser creator) {
        input.prepareCollections();
        if (input.id == null || input.id.isBlank()) throw new IllegalArgumentException("Game ID required");
        if (entityManager.find(Game.class, input.id) != null) throw new IllegalArgumentException("Game ID already exists");
        Game game = new Game();
        game.id = input.id;
        game.visibility = input.visibility;
        game.createdBy = creator;
        game.teacher = isBlank(input.teacher) && creator != null ? creator.displayName : input.teacher;
        game.creationDate = input.creationDate == null ? LocalDate.now() : input.creationDate;
        game.playedRounds = input.playedRounds;
        game.subject = input.subject;
        game.gameType = input.gameType;
        game.name = input.name;
        game.description = input.description;
        game.copiedFromGameId = input.copiedFromGameId;
        game.familyId = input.familyId == null ? UUID.randomUUID() : input.familyId;
        if (input.familyId == null) {
            entityManager.createNativeQuery("insert into game_families (id) values (?1)")
                    .setParameter(1, game.familyId).executeUpdate();
        }
        entityManager.persist(game);
        entityManager.flush();
        writeSnapshot(game, input, 1);
        return currentSnapshot(game);
    }

    @Transactional
    public Optional<Game> update(String id, Game changed) {
        Game game = entityManager.find(Game.class, id);
        if (game == null || game.deletedAt != null) return Optional.empty();
        Game previous = currentSnapshot(game);
        boolean tagsOmitted = changed.tags == null;
        List<Boolean> cardsOmitted = changed.questions == null ? List.of() : changed.questions.stream()
                .map(question -> question.smart10Cards == null).toList();
        changed.prepareCollections();
        if (tagsOmitted) changed.tags = new ArrayList<>(previous.tags);
        reconcileQuestions(previous.questions, changed.questions, cardsOmitted);
        int next = ((Number) entityManager.createNativeQuery(
                "select max(version_number) from game_versions where game_id = ?1")
                .setParameter(1, id).getSingleResult()).intValue() + 1;
        writeSnapshot(game, changed, next);
        game.gameType = changed.gameType;
        game.teacher = changed.teacher;
        game.name = changed.name;
        game.description = changed.description;
        game.subject = changed.subject;
        game.visibility = changed.visibility;
        game.creationDate = changed.creationDate;
        game.playedRounds = changed.playedRounds;
        return Optional.of(currentSnapshot(game));
    }

    private void reconcileQuestions(List<Question> previous, List<Question> incoming, List<Boolean> cardsOmitted) {
        Set<Long> previousIds = new java.util.HashSet<>();
        for (Question question : previous) if (question.id != null) previousIds.add(question.id);
        Set<Long> assigned = new java.util.HashSet<>();
        for (Question question : incoming) {
            if (question.id != null && (!previousIds.contains(question.id) || !assigned.add(question.id)))
                throw new IllegalArgumentException("Question ID is not unique in the current game");
        }
        // Older clients omit question IDs. Match their unchanged content before considering an edit.
        for (Question question : incoming) {
            if (question.id != null) continue;
            List<Question> matches = previous.stream()
                    .filter(old -> old.id != null && !assigned.contains(old.id) && sameVisibleQuestion(old, question))
                    .toList();
            if (matches.isEmpty()) continue;
            Question samePosition = incoming.indexOf(question) < previous.size()
                    ? previous.get(incoming.indexOf(question)) : null;
            Question chosen = samePosition != null && matches.stream().anyMatch(old -> old.id.equals(samePosition.id))
                    ? samePosition : matches.getFirst();
            question.id = chosen.id;
            assigned.add(chosen.id);
        }
        List<Question> unassignedOld = previous.stream()
                .filter(old -> old.id != null && !assigned.contains(old.id)).toList();
        List<Question> unassignedNew = incoming.stream().filter(question -> question.id == null).toList();
        if (!unassignedOld.isEmpty() && !unassignedNew.isEmpty()) {
            if (unassignedOld.size() != 1 || unassignedNew.size() != 1
                    || previous.size() != incoming.size())
                throw new IllegalArgumentException("Ambiguous idless question edit; submit question IDs");
            unassignedNew.getFirst().id = unassignedOld.getFirst().id;
        }
        for (int i = 0; i < incoming.size(); i++) {
            Question question = incoming.get(i);
            if (question.id == null) continue;
            Question old = previous.stream().filter(candidate -> question.id.equals(candidate.id)).findFirst()
                    .orElseThrow();
            if (cardsOmitted.get(i)) question.smart10Cards = new ArrayList<>(old.smart10Cards);
            // Legacy JSON does not carry the internal solution fields.
            if (question.trueFalseCorrect == null) question.trueFalseCorrect = old.trueFalseCorrect;
            if (question.orderingItems.isEmpty()) question.orderingItems = new ArrayList<>(old.orderingItems);
            if (question.acceptedTextAnswers.isEmpty())
                question.acceptedTextAnswers = new ArrayList<>(old.acceptedTextAnswers);
        }
    }

    private boolean sameVisibleQuestion(Question old, Question incoming) {
        if (!java.util.Objects.equals(old.type, incoming.type)
                || !java.util.Objects.equals(old.text, incoming.text)
                || old.answerOptions.size() != incoming.answerOptions.size()) return false;
        for (int i = 0; i < old.answerOptions.size(); i++) {
            AnswerOption a = old.answerOptions.get(i);
            AnswerOption b = incoming.answerOptions.get(i);
            if (!java.util.Objects.equals(a.text, b.text) || a.correct != b.correct) return false;
        }
        return true;
    }

    @Transactional
    public Game copyGame(String sourceId, String newId, AppUser creator) {
        Game source = findById(sourceId).orElseThrow(() -> new IllegalArgumentException("Source game not found"));
        Game copy = new Game();
        copy.id = newId;
        copy.visibility = source.visibility;
        copy.familyId = source.familyId;
        copy.copiedFromGameId = sourceId;
        copy.gameType = source.gameType;
        copy.teacher = creator == null ? source.teacher : creator.displayName;
        copy.name = source.name;
        copy.description = source.description;
        copy.subject = source.subject;
        copy.creationDate = LocalDate.now();
        copy.branches = new ArrayList<>(source.branches);
        copy.tags = new ArrayList<>(source.tags);
        copy.questions = new ArrayList<>();
        for (Question old : source.questions) {
            Question question = new Question();
            question.type = old.type;
            question.text = old.text;
            question.trueFalseCorrect = old.trueFalseCorrect;
            question.orderingItems = new ArrayList<>(old.orderingItems);
            question.acceptedTextAnswers = new ArrayList<>(old.acceptedTextAnswers);
            question.answerOptions = new ArrayList<>();
            question.smart10Cards = new ArrayList<>();
            for (Smart10Card card : old.smart10Cards)
                question.smart10Cards.add(new Smart10Card(null, card.position(), card.text(), card.solution()));
            for (AnswerOption oldOption : old.answerOptions) {
                AnswerOption option = new AnswerOption();
                option.text = oldOption.text;
                option.correct = oldOption.correct;
                question.answerOptions.add(option);
            }
            copy.questions.add(question);
        }
        return create(copy, creator);
    }

    @Transactional
    public boolean delete(String id) {
        Game game = entityManager.find(Game.class, id);
        if (game == null || game.deletedAt != null) return false;
        game.deletedAt = Instant.now();
        return true;
    }

    @Transactional
    public UUID reportQuestion(String gameId, long questionId, AppUser reporter, String comment) {
        if (reporter == null || reporter.id == null || !reporter.active
                || (reporter.role != UserRole.TEACHER && reporter.role != UserRole.ADMIN)
                || isBlank(comment)) throw new IllegalArgumentException("Verified reporter and comment required");
        Game game = entityManager.find(Game.class, gameId);
        if (game == null || game.deletedAt != null) throw new IllegalArgumentException("Active game required");
        Question question = entityManager.find(Question.class, questionId);
        if (question == null || !gameId.equals(question.gameId)
                || ((Number) entityManager.createNativeQuery("select count(*) from game_version_questions gvq "
                        + "join question_versions qv on qv.id = gvq.question_version_id "
                        + "where gvq.game_version_id = ?1 and qv.question_id = ?2")
                        .setParameter(1, game.currentVersionId).setParameter(2, questionId).getSingleResult()).intValue() == 0)
            throw new IllegalArgumentException("Active question required");
        UUID id = UUID.randomUUID();
        entityManager.createNativeQuery("insert into question_reports "
                        + "(id, question_version_id, reporter_user_id, comment, created_at) values (?1, ?2, ?3, ?4, ?5)")
                .setParameter(1, id).setParameter(2, question.currentVersionId)
                .setParameter(3, reporter.id).setParameter(4, comment)
                .setParameter(5, Instant.now()).executeUpdate();
        return id;
    }

    @Transactional
    public boolean deleteQuestion(String gameId, long questionId) {
        Game current = findById(gameId).orElse(null);
        Question identity = entityManager.find(Question.class, questionId);
        if (current == null || identity == null || !gameId.equals(identity.gameId)) return false;
        Game changed = currentSnapshot(entityManager.find(Game.class, gameId));
        changed.questions.removeIf(question -> question.id != null && question.id == questionId);
        update(gameId, changed);
        entityManager.createNativeQuery("delete from question_reports where question_version_id in "
                + "(select id from question_versions where question_id = ?1)")
                .setParameter(1, questionId).executeUpdate();
        entityManager.createNativeQuery("update question_versions set question_id = null where question_id = ?1")
                .setParameter(1, questionId).executeUpdate();
        entityManager.createNativeQuery("update answer_options set question_id = null where question_id = ?1")
                .setParameter(1, questionId).executeUpdate();
        entityManager.remove(identity);
        return true;
    }

    private void writeSnapshot(Game game, Game input, int versionNumber) {
        UUID versionId = UUID.randomUUID();
        java.util.Set<Long> activeQuestionIds = new java.util.HashSet<>();
        entityManager.createNativeQuery("insert into game_versions "
                        + "(id, game_id, version_number, title, description, game_type, created_at) "
                        + "values (?1, ?2, ?3, ?4, ?5, ?6, ?7)")
                .setParameter(1, versionId).setParameter(2, game.id).setParameter(3, versionNumber)
                .setParameter(4, input.name).setParameter(5, input.description)
                .setParameter(6, input.gameType).setParameter(7, Instant.now()).executeUpdate();
        for (int i = 0; i < input.branches.size(); i++) {
            entityManager.createNativeQuery("insert into game_version_branches (game_version_id, position, branch) values (?1, ?2, ?3)")
                    .setParameter(1, versionId).setParameter(2, i).setParameter(3, input.branches.get(i)).executeUpdate();
        }
        for (int i = 0; i < input.questions.size(); i++) {
            Question incoming = input.questions.get(i);
            UUID questionVersionId = writeQuestion(game.id, incoming, i, "Smart10".equals(input.gameType));
            activeQuestionIds.add(incoming.id);
            entityManager.createNativeQuery("insert into game_version_questions "
                            + "(game_version_id, question_version_id, position) values (?1, ?2, ?3)")
                    .setParameter(1, versionId).setParameter(2, questionVersionId).setParameter(3, i).executeUpdate();
        }
        java.util.LinkedHashSet<String> tags = new java.util.LinkedHashSet<>(input.tags);
        if (!isBlank(input.subject)) tags.add(input.subject);
        for (String tag : tags) {
            if (isBlank(tag)) throw new IllegalArgumentException("Blank tag");
            addTag(versionId, tag);
        }
        for (Question previous : entityManager.createQuery(
                "select q from Question q where q.gameId = :gameId", Question.class)
                .setParameter("gameId", game.id).getResultList()) {
            if (!activeQuestionIds.contains(previous.id)) {
                previous.gameId = null;
                previous.questionOrder = null;
            }
        }
        entityManager.createNativeQuery("delete from game_branches where game_id = ?1")
                .setParameter(1, game.id).executeUpdate();
        for (int i = 0; i < input.branches.size(); i++) {
            entityManager.createNativeQuery("insert into game_branches (game_id, branch_order, branch) values (?1, ?2, ?3)")
                    .setParameter(1, game.id).setParameter(2, i)
                    .setParameter(3, input.branches.get(i)).executeUpdate();
        }
        game.currentVersionId = versionId;
        entityManager.flush();
    }

    private UUID writeQuestion(String gameId, Question incoming, int position, boolean smart10) {
        incoming.prepareCollections();
        if (smart10) Smart10Cards.validate(incoming);
        else validateSolution(incoming);
        Question identity = incoming.id == null ? null : entityManager.find(Question.class, incoming.id);
        if (incoming.id != null && (identity == null || !gameId.equals(identity.gameId))) {
            throw new IllegalArgumentException("Question does not belong to game");
        }
        if (identity == null) {
            identity = new Question();
            identity.gameId = gameId;
            identity.questionOrder = position;
            identity.type = incoming.type;
            identity.text = incoming.text;
            entityManager.persist(identity);
            entityManager.flush();
        } else if (sameQuestion(identity.currentVersionId, incoming)) {
            identity.questionOrder = position;
            return identity.currentVersionId;
        }
        incoming.id = identity.id;
        int number = ((Number) entityManager.createNativeQuery(
                "select count(*) from question_versions where question_id = ?1")
                .setParameter(1, identity.id).getSingleResult()).intValue() + 1;
        UUID versionId = UUID.randomUUID();
        entityManager.createNativeQuery("insert into question_versions "
                        + "(id, question_id, version_number, type, text, created_at) values (?1, ?2, ?3, ?4, ?5, ?6)")
                .setParameter(1, versionId).setParameter(2, identity.id).setParameter(3, number)
                .setParameter(4, incoming.type).setParameter(5, incoming.text)
                .setParameter(6, Instant.now()).executeUpdate();
        entityManager.createNativeQuery("update answer_options set question_id = null where question_id = ?1")
                .setParameter(1, identity.id).executeUpdate();
        for (int i = 0; i < incoming.answerOptions.size(); i++) {
            AnswerOption incomingOption = incoming.answerOptions.get(i);
            AnswerOption option = new AnswerOption();
            option.questionId = identity.id;
            option.questionVersionId = versionId;
            option.answerOrder = i;
            option.text = incomingOption.text;
            option.correct = incomingOption.correct;
            entityManager.persist(option);
        }
        if (smart10) {
            for (Smart10Card card : incoming.smart10Cards) {
                UUID cardId = UUID.randomUUID();
                Object solution = card.solution();
                entityManager.createNativeQuery("insert into smart10_cards "
                                + "(id, question_version_id, position, text, solution_boolean, solution_text, solution_rank) "
                                + "values (?1, ?2, ?3, ?4, ?5, ?6, ?7)")
                        .setParameter(1, cardId).setParameter(2, versionId).setParameter(3, card.position())
                        .setParameter(4, card.text())
                        .setParameter(5, solution instanceof Boolean ? solution : null)
                        .setParameter(6, solution instanceof String ? solution : null)
                        .setParameter(7, solution instanceof Integer ? solution : null).executeUpdate();
            }
        } else if ("TRUE_FALSE".equals(incoming.type)) {
            entityManager.createNativeQuery("insert into true_false_configs (question_version_id, correct_value) values (?1, ?2)")
                    .setParameter(1, versionId).setParameter(2, incoming.trueFalseCorrect).executeUpdate();
        } else if ("ORDERING".equals(incoming.type)) {
            for (int i = 0; i < incoming.orderingItems.size(); i++) {
                entityManager.createNativeQuery("insert into ordering_items (id, question_version_id, text, correct_position) values (?1, ?2, ?3, ?4)")
                        .setParameter(1, UUID.randomUUID()).setParameter(2, versionId)
                        .setParameter(3, incoming.orderingItems.get(i)).setParameter(4, i).executeUpdate();
            }
        } else if ("FREE_TEXT".equals(incoming.type)) {
            for (String accepted : incoming.acceptedTextAnswers) {
                entityManager.createNativeQuery("insert into accepted_text_answers (id, question_version_id, answer) values (?1, ?2, ?3)")
                        .setParameter(1, UUID.randomUUID()).setParameter(2, versionId)
                        .setParameter(3, accepted).executeUpdate();
            }
        }
        identity.currentVersionId = versionId;
        identity.questionOrder = position;
        identity.type = incoming.type;
        identity.text = incoming.text;
        entityManager.flush();
        return versionId;
    }

    private boolean sameQuestion(UUID versionId, Question incoming) {
        if (versionId == null) return false;
        Object[] version = (Object[]) entityManager.createNativeQuery(
                "select type, text from question_versions where id = ?1")
                .setParameter(1, versionId).getSingleResult();
        if (!java.util.Objects.equals(version[0], incoming.type)
                || !java.util.Objects.equals(version[1], incoming.text)) return false;
        List<AnswerOption> options = entityManager.createQuery(
                "select o from AnswerOption o where o.questionVersionId = :version order by o.answerOrder", AnswerOption.class)
                .setParameter("version", versionId).getResultList();
        if (options.size() != incoming.answerOptions.size()) return false;
        for (int i = 0; i < options.size(); i++) {
            if (!java.util.Objects.equals(options.get(i).text, incoming.answerOptions.get(i).text)
                    || options.get(i).correct != incoming.answerOptions.get(i).correct) return false;
        }
        if (!loadCards(versionId).stream().map(card -> new Smart10Card(null, card.position(), card.text(), card.solution())).toList()
                .equals(incoming.smart10Cards.stream()
                        .map(card -> new Smart10Card(null, card.position(), card.text(), card.solution())).toList())) return false;
        return java.util.Objects.equals(loadTrueFalse(versionId), incoming.trueFalseCorrect)
                && loadStrings("select text from ordering_items where question_version_id = ?1 order by correct_position", versionId)
                        .equals(incoming.orderingItems)
                && loadStrings("select answer from accepted_text_answers where question_version_id = ?1 order by answer", versionId)
                        .equals(incoming.acceptedTextAnswers.stream().sorted().toList());
    }

    private void validateSolution(Question question) {
        switch (question.type) {
            case "TRUE_FALSE" -> {
                if (question.trueFalseCorrect == null) throw new IllegalArgumentException("TRUE_FALSE needs a result");
            }
            case "ORDERING" -> {
                if (question.orderingItems.size() < 2 || question.orderingItems.stream().anyMatch(this::isBlank))
                    throw new IllegalArgumentException("ORDERING needs at least two items");
            }
            case "FREE_TEXT" -> {
                if (question.acceptedTextAnswers.isEmpty() || question.acceptedTextAnswers.stream().anyMatch(this::isBlank))
                    throw new IllegalArgumentException("FREE_TEXT needs accepted answers");
            }
            default -> { /* Preserve legacy and unknown types without reinterpretation. */ }
        }
    }

    private Boolean loadTrueFalse(UUID versionId) {
        List<?> values = entityManager.createNativeQuery("select correct_value from true_false_configs where question_version_id = ?1")
                .setParameter(1, versionId).getResultList();
        return values.isEmpty() ? null : (Boolean) values.getFirst();
    }

    private List<String> loadStrings(String query, UUID versionId) {
        return entityManager.createNativeQuery(query, String.class).setParameter(1, versionId).getResultList();
    }

    private List<Smart10Card> loadCards(UUID versionId) {
        List<Object[]> rows = entityManager.createNativeQuery("select id, position, text, solution_boolean, "
                        + "solution_text, solution_rank from smart10_cards where question_version_id = ?1 order by position")
                .setParameter(1, versionId).getResultList();
        return rows.stream().map(row -> new Smart10Card(asUuid(row[0]), ((Number) row[1]).intValue(),
                (String) row[2], row[3] != null ? row[3] : row[4] != null ? row[4]
                : row[5] == null ? null : ((Number) row[5]).intValue())).toList();
    }

    private void addTag(UUID versionId, String name) {
        List<?> ids = entityManager.createNativeQuery("select id from tags where name = ?1")
                .setParameter(1, name).getResultList();
        UUID tagId;
        if (ids.isEmpty()) {
            tagId = UUID.randomUUID();
            entityManager.createNativeQuery("insert into tags (id, name) values (?1, ?2)")
                    .setParameter(1, tagId).setParameter(2, name).executeUpdate();
        } else {
            tagId = asUuid(ids.getFirst());
        }
        entityManager.createNativeQuery("insert into game_version_tags (game_version_id, tag_id) values (?1, ?2)")
                .setParameter(1, versionId).setParameter(2, tagId).executeUpdate();
    }

    private Game currentSnapshot(Game identity) {
        Game result = new Game();
        result.visibility = identity.visibility;
        result.createdBy = identity.createdBy;
        result.id = identity.id;
        result.familyId = identity.familyId;
        result.copiedFromGameId = identity.copiedFromGameId;
        result.currentVersionId = identity.currentVersionId;
        result.createdBy = identity.createdBy;
        result.teacher = identity.teacher;
        result.subject = identity.subject;
        result.creationDate = identity.creationDate;
        result.playedRounds = identity.playedRounds;
        Object[] version = (Object[]) entityManager.createNativeQuery(
                "select title, description, game_type from game_versions where id = ?1")
                .setParameter(1, identity.currentVersionId).getSingleResult();
        result.name = (String) version[0];
        result.description = (String) version[1];
        result.gameType = (String) version[2];
        result.branches = entityManager.createNativeQuery(
                "select branch from game_version_branches where game_version_id = ?1 order by position", String.class)
                .setParameter(1, identity.currentVersionId).getResultList();
        result.tags = entityManager.createNativeQuery("select t.name from game_version_tags gvt "
                        + "join tags t on t.id = gvt.tag_id where gvt.game_version_id = ?1 order by t.name", String.class)
                .setParameter(1, identity.currentVersionId).getResultList();
        List<Object[]> rows = entityManager.createNativeQuery(
                "select qv.id, qv.question_id, qv.type, qv.text from game_version_questions gvq "
                        + "join question_versions qv on qv.id = gvq.question_version_id "
                        + "where gvq.game_version_id = ?1 order by gvq.position")
                .setParameter(1, identity.currentVersionId).getResultList();
        result.questions = new ArrayList<>();
        for (Object[] row : rows) {
            Question question = new Question();
            question.id = row[1] == null ? null : ((Number) row[1]).longValue();
            question.type = (String) row[2];
            question.text = (String) row[3];
            List<AnswerOption> storedOptions = entityManager.createQuery(
                    "select o from AnswerOption o where o.questionVersionId = :version order by o.answerOrder", AnswerOption.class)
                    .setParameter("version", asUuid(row[0])).getResultList();
            question.answerOptions = new ArrayList<>();
            for (AnswerOption stored : storedOptions) {
                AnswerOption projected = new AnswerOption();
                projected.id = stored.id;
                projected.text = stored.text;
                projected.correct = stored.correct;
                question.answerOptions.add(projected);
            }
            question.trueFalseCorrect = loadTrueFalse(asUuid(row[0]));
            question.orderingItems = loadStrings(
                    "select text from ordering_items where question_version_id = ?1 order by correct_position", asUuid(row[0]));
            question.acceptedTextAnswers = loadStrings(
                    "select answer from accepted_text_answers where question_version_id = ?1 order by answer", asUuid(row[0]));
            question.currentVersionId = asUuid(row[0]);
            question.smart10Cards = new ArrayList<>(loadCards(question.currentVersionId));
            result.questions.add(question);
        }
        return result;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
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
