package at.htlleonding.gamebasedlearning.games;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.websockets.next.WebSocketConnection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@ApplicationScoped
public class MultiplayerGameService {
    private static final int TURN_SECONDS = 20;

    @Inject
    ObjectMapper objectMapper;
    @Inject Smart10SnapshotLoader loader;
    @Inject Smart10Tickets tickets;
    @Inject Smart10Feature feature;
    @Inject CompletedRoundStore store;

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, WebSocketConnection> connections = new ConcurrentHashMap<>();

    public Map<String, Object> createRoom(WebSocketConnection connection, JsonNode request) {
        Smart10Tickets.Actor actor = tickets.actor(connection.id());
        String gameId = request.path("gameId").asText();
        boolean demo = gameId.startsWith("demo-") && request.has("game");
        Smart10Snapshot pinned = null;
        JsonNode game;
        if (demo) game = request.path("game");
        else {
            feature.requireEnabled();
            pinned = loader.load(gameId);
            game = serverGame(pinned);
        }
        String code = uniqueCode();
        Room room = new Room(code, gameId, game, pinned, connection.id(), actor.userId(),
                clamp(request.path("teamCount").asInt(2), 1, 6),
                "random".equals(request.path("assignmentMode").asText()) ? "random" : "self");
        register(connection);
        rooms.put(code, room);
        return snapshot(room);
    }

    public Map<String, Object> joinRoom(WebSocketConnection connection, JsonNode request) {
        Room room = room(request);
        if (room.phase != Phase.LOBBY) throw new IllegalStateException("Das Spiel wurde bereits gestartet.");
        Smart10Tickets.Actor actor = tickets.actor(connection.id());
        if (room.pinned != null && actor.role() == at.htlleonding.gamebasedlearning.users.UserRole.TEACHER)
            throw new IllegalArgumentException("Teacher participation is not enabled");
        if (room.hostUserId.equals(actor.userId()) || room.players.values().stream()
                .anyMatch(existing -> existing.userId.equals(actor.userId())))
            throw new IllegalArgumentException("User already in room");
        Player player = new Player(connection.id(), actor.userId(), actor.displayName());
        register(connection);
        room.players.put(player.id, player);
        assignPlayer(room, player);
        return snapshotAndBroadcast(room);
    }

    public Map<String, Object> startRoom(WebSocketConnection connection, JsonNode request) {
        Room room = room(request);
        if (!room.hostId.equals(connection.id())) throw new IllegalStateException("Nur der Host darf das Spiel starten.");
        if (room.phase != Phase.LOBBY) throw new IllegalStateException("Round already started");
        if (room.players.isEmpty()) throw new IllegalStateException("Mindestens ein Spieler wird benötigt.");
        if (room.teams.values().stream().anyMatch(team -> team.members.isEmpty()))
            throw new IllegalStateException("Teams must contain active players");
        if (room.pinned != null) {
            for (Player player : room.players.values()) {
                Smart10Tickets.Actor actor = tickets.actor(player.id);
                if (!actor.userId().equals(player.userId)
                        || actor.role() == at.htlleonding.gamebasedlearning.users.UserRole.TEACHER)
                    throw new IllegalArgumentException("Active participant identity required");
            }
        }
        room.phase = Phase.PLAYING;
        room.startedAt = Instant.now();
        room.currentTeam = randomActiveTeam(room);
        room.turnDeadline = Instant.now().plusSeconds(TURN_SECONDS);
        return snapshotAndBroadcast(room);
    }

    public Map<String, Object> selectCard(WebSocketConnection connection, JsonNode request) {
        Room room = room(request);
        if (expireTurn(room)) return snapshotAndBroadcast(room);
        Team team = currentTeam(room);
        requireLeader(room, team, connection);
        String cardId = request.path("cardId").asText();
        if (room.selectedCard != null) throw new IllegalStateException("Für diesen Zug wurde bereits eine Karte gewählt.");
        if (room.resolvedCards.containsKey(cardId)) {
            throw new IllegalStateException("Diese Karte ist bereits aufgelöst.");
        }
        room.selectedCard = cardId;
        room.lastAnswer = null;
        return snapshotAndBroadcast(room);
    }

    public Map<String, Object> answer(WebSocketConnection connection, JsonNode request) {
        Room room = room(request);
        if (expireTurn(room)) return snapshotAndBroadcast(room);
        Team team = currentTeam(room);
        requireLeader(room, team, connection);
        if (room.selectedCard == null) throw new IllegalStateException("Wähle zuerst eine Karte.");
        JsonNode question = question(room);
        JsonNode selected = card(room, room.selectedCard);
        boolean correct = room.pinned == null
                ? isCorrect(question.path("modus").asText("TRUE_FALSE"), selected, request.get("answer"))
                : Smart10Cards.correct(room.pinned.question(room.questionIndex).mode(),
                    room.pinned.card(room.questionIndex, UUID.fromString(room.selectedCard)), request.get("answer"));
        if (room.pinned != null) {
            Smart10Snapshot.Group group = room.pinned.question(room.questionIndex);
            UUID cardId = UUID.fromString(room.selectedCard);
            room.answers.computeIfAbsent(connection.id(), ignored -> new ArrayList<>()).add(
                    new CompletedRoundStore.Answer(group.questionVersionId(), cardId,
                            request.get("answer") == null ? "null" : request.get("answer").toString(),
                            correct, Instant.now()));
        }
        room.lastAnswer = answerFeedback(room, team, selected, room.selectedCard, correct, connection.id());
        room.resolvedCards.put(room.selectedCard, correct);
        room.selectedCard = null;
        if (correct) {
            team.points++;
        } else {
            team.points = 0;
            team.eliminated = true;
        }
        advance(room);
        return snapshotAndBroadcast(room);
    }

    public Map<String, Object> pass(WebSocketConnection connection, JsonNode request) {
        Room room = room(request);
        if (expireTurn(room)) return snapshotAndBroadcast(room);
        Team team = currentTeam(room);
        requireLeader(room, team, connection);
        room.selectedCard = null;
        room.lastAnswer = null;
        team.passed = true;
        advance(room);
        return snapshotAndBroadcast(room);
    }

    public void leave(WebSocketConnection connection) {
        connections.remove(connection.id());
        rooms.values().forEach(room -> {
            if (room.hostId.equals(connection.id()) || room.players.containsKey(connection.id())) {
                if (room.phase != Phase.FINISHED) {
                    rooms.remove(room.code);
                    return;
                }
            }
            Player player = room.players.remove(connection.id());
            if (player != null) {
                Team team = room.teams.get(player.team);
                if (team != null) team.members.remove(player.id);
                if (room.players.isEmpty()) rooms.remove(room.code);
                else broadcast(room);
            }
        });
    }

    private void register(WebSocketConnection connection) {
        connections.put(connection.id(), connection);
    }

    private String uniqueCode() {
        String code;
        do {
            code = String.valueOf(ThreadLocalRandom.current().nextInt(1000, 10000));
        } while (rooms.containsKey(code));
        return code;
    }

    private Room room(JsonNode request) {
        String code = request.path("code").asText();
        Room room = rooms.get(code);
        if (room == null) throw new IllegalStateException("Spielraum nicht gefunden.");
        return room;
    }

    private void assignPlayer(Room room, Player player) {
        int team = 0;
        if ("random".equals(room.assignmentMode)) {
            team = ThreadLocalRandom.current().nextInt(room.teamCount);
        } else {
            int smallest = Integer.MAX_VALUE;
            for (int index = 0; index < room.teamCount; index++) {
                int size = room.teams.containsKey(index) ? room.teams.get(index).members.size() : 0;
                if (size < smallest) { smallest = size; team = index; }
            }
        }
        Team selected = room.teams.computeIfAbsent(team, Team::new);
        player.team = team;
        selected.members.add(player.id);
        if (selected.leader == null) selected.leader = player.id;
    }

    private int randomActiveTeam(Room room) {
        List<Integer> active = activeTeams(room);
        return active.get(ThreadLocalRandom.current().nextInt(active.size()));
    }

    private void advance(Room room) {
        if (allCardsResolved(room) || activeTeams(room).isEmpty()) {
            if (room.phase == Phase.PLAYING && room.questionIndex + 1 < questions(room).size() && !allTeamsEliminated(room)) {
                room.questionIndex++;
                room.resolvedCards.clear();
                room.lastAnswer = null;
                room.teams.values().forEach(team -> team.passed = false);
            } else {
                room.phase = Phase.FINISHED;
                room.turnDeadline = null;
                finish(room);
                return;
            }
        }
        room.currentTeam = nextActiveTeam(room);
        room.turnDeadline = Instant.now().plusSeconds(TURN_SECONDS);
    }

    private boolean expireTurn(Room room) {
        if (room.phase != Phase.PLAYING || room.turnDeadline == null || Instant.now().isBefore(room.turnDeadline)) return false;
        Team team = currentTeam(room);
        room.selectedCard = null;
        team.points = 0;
        team.eliminated = true;
        advance(room);
        return true;
    }

    private int nextActiveTeam(Room room) {
        List<Integer> active = activeTeams(room);
        if (active.isEmpty()) return room.currentTeam;
        return active.stream()
                .filter(teamNumber -> teamNumber > room.currentTeam)
                .findFirst()
                .orElse(active.getFirst());
    }

    private List<Integer> activeTeams(Room room) {
        List<Integer> active = new ArrayList<>();
        room.teams.forEach((index, team) -> { if (!team.eliminated && !team.passed) active.add(index); });
        Collections.sort(active);
        return active;
    }

    private boolean allTeamsEliminated(Room room) {
        return room.teams.values().stream().allMatch(team -> team.eliminated);
    }

    private boolean allCardsResolved(Room room) {
        return room.resolvedCards.size() >= options(room).size();
    }

    private Team currentTeam(Room room) {
        Team team = room.teams.get(room.currentTeam);
        if (team == null) throw new IllegalStateException("Kein aktives Team vorhanden.");
        return team;
    }

    private void requireLeader(Room room, Team team, WebSocketConnection connection) {
        if (!team.leader.equals(connection.id())) throw new IllegalStateException("Nur der Teamleader darf auswählen und antworten.");
        if (room.pinned != null) {
            Smart10Tickets.Actor actor = tickets.actor(connection.id());
            Player player = room.players.get(connection.id());
            if (player == null || !actor.userId().equals(player.userId)
                    || actor.role() == at.htlleonding.gamebasedlearning.users.UserRole.TEACHER)
                throw new IllegalArgumentException("Active participant identity required");
        }
    }

    private JsonNode question(Room room) {
        JsonNode questions = questions(room);
        if (!questions.isArray() || room.questionIndex >= questions.size()) throw new IllegalStateException("Keine Frage verfügbar.");
        return questions.get(room.questionIndex);
    }

    private JsonNode questions(Room room) { return room.game.path("fragen"); }
    private JsonNode options(Room room) { return question(room).path("antwortmoeglichkeiten"); }
    private JsonNode card(Room room, String id) {
        int index = 0;
        for (JsonNode option : options(room)) {
            index++;
            if (option.path("id").asText(String.valueOf(index)).equals(id)) return option;
        }
        throw new IllegalStateException("Antwortkarte nicht gefunden.");
    }

    private boolean isCorrect(String mode, JsonNode card, JsonNode answer) {
        JsonNode solution = card.get("loesung");
        if (solution == null) solution = card.get("ist_richtig");
        if (solution == null || answer == null) return false;
        if ("ORDERING".equals(mode)) return solution.asInt() == answer.asInt();
        if ("TRUE_FALSE".equals(mode)) return solution.asBoolean() == answer.asBoolean();
        return solution.asText().trim().equalsIgnoreCase(answer.asText().trim());
    }

    private Map<String, Object> snapshotAndBroadcast(Room room) {
        Map<String, Object> snapshot = snapshot(room);
        broadcast(room, snapshot);
        return snapshot;
    }

    private Map<String, Object> snapshot(Room room) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("code", room.code);
        snapshot.put("gameId", room.gameId);
        snapshot.put("gameName", room.game.path("name").asText("Smart10"));
        snapshot.put("phase", room.phase.name());
        snapshot.put("hostId", room.hostId);
        snapshot.put("players", room.players.values().stream().map(Player::asMap).toList());
        snapshot.put("teams", room.teams.values().stream().map(Team::asMap).toList());
        snapshot.put("currentQuestion", questionSnapshot(room));
        snapshot.put("questionIndex", room.questionIndex);
        snapshot.put("questionsTotal", questions(room).size());
        snapshot.put("currentTeam", room.currentTeam);
        snapshot.put("selectedCard", room.selectedCard);
        snapshot.put("turnDeadline", room.turnDeadline == null ? null : room.turnDeadline.toEpochMilli());
        snapshot.put("lastAnswer", room.lastAnswer);
        snapshot.put("demo", room.pinned == null);
        snapshot.put("sessionId", room.sessionId);
        snapshot.put("saveError", room.saveError);
        return snapshot;
    }

    private Map<String, Object> answerFeedback(Room room, Team team, JsonNode card, String cardId, boolean correct, String playerId) {
        Map<String, Object> feedback = new LinkedHashMap<>();
        feedback.put("correct", correct);
        feedback.put("team", team.number);
        feedback.put("cardId", cardId);
        JsonNode solution = card.get("loesung");
        if (solution == null) solution = card.get("ist_richtig");
        feedback.put("solution", solution == null || solution.isNull() ? null : objectMapper.convertValue(solution, Object.class));
        feedback.put("answeredBy", room.players.get(playerId) == null ? "Teamleader" : room.players.get(playerId).name);
        return feedback;
    }

    private Map<String, Object> questionSnapshot(Room room) {
        JsonNode question = question(room);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("prompt", question.path("frage").asText());
        result.put("mode", question.path("modus").asText("TRUE_FALSE"));
        List<Map<String, Object>> cards = new ArrayList<>();
        int index = 0;
        for (JsonNode option : question.path("antwortmoeglichkeiten")) {
            String id = option.path("id").asText(String.valueOf(++index));
            Map<String, Object> card = new LinkedHashMap<>();
            card.put("id", id);
            card.put("text", option.path("text").asText());
            card.put("state", room.resolvedCards.containsKey(id) ? "resolved" : "open");
            if (room.resolvedCards.containsKey(id)) card.put("solution", option.path("loesung").isMissingNode() ? option.path("ist_richtig").asText() : option.get("loesung"));
            cards.add(card);
        }
        result.put("cards", cards);
        return result;
    }

    private void broadcast(Room room) { broadcast(room, snapshot(room)); }

    private void broadcast(Room room, Map<String, Object> snapshot) {
        try {
            String message = objectMapper.writeValueAsString(Map.of("type", "room_state", "data", snapshot));
            java.util.stream.Stream.concat(room.players.keySet().stream(), java.util.stream.Stream.of(room.hostId))
                    .distinct().map(connections::get).filter(connection -> connection != null && connection.isOpen())
                    .forEach(connection -> connection.sendTextAndAwait(message));
        } catch (Exception ignored) { }
    }

    public Map<String, Object> retrySave(WebSocketConnection connection, JsonNode request) {
        Room room = room(request);
        if (!room.hostId.equals(connection.id()) || room.phase != Phase.FINISHED || room.finalPayload == null)
            throw new IllegalStateException("No finalized result available for retry");
        persist(room);
        return snapshotAndBroadcast(room);
    }

    private void finish(Room room) {
        if (room.pinned == null || room.finalPayload != null) return;
        List<Team> sorted = room.teams.values().stream()
                .sorted(java.util.Comparator.comparingInt((Team team) -> team.points).reversed()).toList();
        Map<Integer, Integer> places = new LinkedHashMap<>();
        int previousScore = Integer.MIN_VALUE;
        int place = 0;
        for (int i = 0; i < sorted.size(); i++) {
            Team team = sorted.get(i);
            if (team.points != previousScore) place = i + 1;
            places.put(team.number, place);
            previousScore = team.points;
        }
        List<CompletedRoundStore.Team> teams = room.teams.values().stream()
                .map(team -> new CompletedRoundStore.Team(teamName(team.number), team.points,
                        places.get(team.number))).toList();
        List<CompletedRoundStore.Participant> participants = room.players.values().stream().map(player -> {
            Team team = room.teams.get(player.team);
            return new CompletedRoundStore.Participant(player.userId, teamName(team.number), team.points,
                    places.get(team.number), room.startedAt,
                    List.copyOf(room.answers.getOrDefault(player.id, List.of())));
        }).toList();
        room.finalPayload = new CompletedRoundStore.Round(room.completionKey,
                CompletedRoundStore.State.COMPLETED, room.gameId, room.pinned.gameVersionId(),
                room.hostUserId, "MULTIPLAYER", room.startedAt, Instant.now(), teams, participants);
        persist(room);
    }

    private void persist(Room room) {
        try {
            room.sessionId = store.saveSmart10(room.finalPayload);
            room.saveError = null;
        } catch (RuntimeException failure) {
            room.saveError = "Ergebnis konnte nicht gespeichert werden. Der Host kann erneut versuchen.";
        }
    }

    private String teamName(int number) { return "Team " + (number + 1); }

    private JsonNode serverGame(Smart10Snapshot snapshot) {
        ObjectNode game = objectMapper.createObjectNode();
        game.put("name", snapshot.title());
        ArrayNode questions = game.putArray("fragen");
        for (Smart10Snapshot.Group group : snapshot.questions()) {
            ObjectNode question = questions.addObject();
            question.put("frage", group.prompt());
            question.put("modus", group.mode());
            ArrayNode cards = question.putArray("antwortmoeglichkeiten");
            for (Smart10Card card : group.cards()) {
                ObjectNode entry = cards.addObject();
                entry.put("id", card.id().toString());
                entry.put("text", card.text());
                switch (group.mode()) {
                    case "TRUE_FALSE" -> entry.put("loesung", (Boolean) card.solution());
                    case "FREE_TEXT" -> entry.put("loesung", (String) card.solution());
                    case "ORDERING" -> entry.put("loesung", (Integer) card.solution());
                    default -> throw new IllegalArgumentException("Unsupported Smart10 mode");
                }
            }
        }
        return game;
    }

    private int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }

    private enum Phase { LOBBY, PLAYING, FINISHED }

    private static final class Room {
        final String code;
        final String gameId;
        final JsonNode game;
        final Smart10Snapshot pinned;
        final String hostId;
        final UUID hostUserId;
        final UUID completionKey = UUID.randomUUID();
        final Instant createdAt = Instant.now();
        final int teamCount;
        final String assignmentMode;
        final Map<String, Player> players = new LinkedHashMap<>();
        final Map<Integer, Team> teams = new LinkedHashMap<>();
        final Map<String, Boolean> resolvedCards = new LinkedHashMap<>();
        final Map<String, List<CompletedRoundStore.Answer>> answers = new LinkedHashMap<>();
        Phase phase = Phase.LOBBY;
        int questionIndex;
        int currentTeam;
        String selectedCard;
        Instant turnDeadline;
        Instant startedAt;
        Map<String, Object> lastAnswer;
        CompletedRoundStore.Round finalPayload;
        UUID sessionId;
        String saveError;

        Room(String code, String gameId, JsonNode game, Smart10Snapshot pinned,
                String hostId, UUID hostUserId, int teamCount, String assignmentMode) {
            this.code = code; this.gameId = gameId; this.game = game; this.pinned = pinned;
            this.hostId = hostId; this.hostUserId = hostUserId;
            this.teamCount = teamCount; this.assignmentMode = assignmentMode;
        }
    }

    private static final class Player {
        final String id;
        final UUID userId;
        final String name;
        int team;

        Player(String id, UUID userId, String name) { this.id = id; this.userId = userId; this.name = name; }
        Map<String, Object> asMap() { return Map.of("id", id, "name", name, "host", false, "team", team); }
    }

    private static final class Team {
        final int number;
        final List<String> members = new ArrayList<>();
        String leader;
        int points;
        boolean eliminated;
        boolean passed;

        Team(int number) { this.number = number; }
        Map<String, Object> asMap() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("number", number);
            result.put("members", members);
            result.put("leader", leader);
            result.put("points", points);
            result.put("eliminated", eliminated);
            result.put("passed", passed);
            return result;
        }
    }
}
