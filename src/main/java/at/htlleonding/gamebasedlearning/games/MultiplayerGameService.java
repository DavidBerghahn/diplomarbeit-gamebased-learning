package at.htlleonding.gamebasedlearning.games;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, WebSocketConnection> connections = new ConcurrentHashMap<>();

    public Map<String, Object> createRoom(WebSocketConnection connection, JsonNode request) {
        register(connection);
        String code = uniqueCode();
        Room room = new Room(code, request.path("gameId").asText(), request.path("game"),
                clamp(request.path("teamCount").asInt(2), 1, 6),
                "random".equals(request.path("assignmentMode").asText()) ? "random" : "self");
        Player host = new Player(connection.id(), displayName(request), true);
        room.players.put(host.id, host);
        rooms.put(code, room);
        assignPlayer(room, host);
        return snapshot(room);
    }

    public Map<String, Object> joinRoom(WebSocketConnection connection, JsonNode request) {
        register(connection);
        Room room = room(request);
        if (room.phase != Phase.LOBBY) throw new IllegalStateException("Das Spiel wurde bereits gestartet.");
        Player player = new Player(connection.id(), displayName(request), false);
        room.players.put(player.id, player);
        assignPlayer(room, player);
        return snapshotAndBroadcast(room);
    }

    public Map<String, Object> startRoom(WebSocketConnection connection, JsonNode request) {
        Room room = room(request);
        Player host = room.players.get(connection.id());
        if (host == null || !host.host) throw new IllegalStateException("Nur der Host darf das Spiel starten.");
        if (room.players.isEmpty()) throw new IllegalStateException("Mindestens ein Spieler wird benötigt.");
        room.phase = Phase.PLAYING;
        room.currentTeam = randomActiveTeam(room);
        room.turnDeadline = Instant.now().plusSeconds(TURN_SECONDS);
        return snapshotAndBroadcast(room);
    }

    public Map<String, Object> selectCard(WebSocketConnection connection, JsonNode request) {
        Room room = room(request);
        expireTurn(room);
        Team team = currentTeam(room);
        requireLeader(room, team, connection);
        int cardId = request.path("cardId").asInt(-1);
        if (room.selectedCard != null) throw new IllegalStateException("Für diesen Zug wurde bereits eine Karte gewählt.");
        if (card(room, cardId).path("state").asText("open").equals("resolved")) {
            throw new IllegalStateException("Diese Karte ist bereits aufgelöst.");
        }
        room.selectedCard = cardId;
        room.lastAnswer = null;
        return snapshotAndBroadcast(room);
    }

    public Map<String, Object> answer(WebSocketConnection connection, JsonNode request) {
        Room room = room(request);
        expireTurn(room);
        Team team = currentTeam(room);
        requireLeader(room, team, connection);
        if (room.selectedCard == null) throw new IllegalStateException("Wähle zuerst eine Karte.");
        JsonNode question = question(room);
        JsonNode selected = card(room, room.selectedCard);
        boolean correct = isCorrect(question.path("modus").asText("TRUE_FALSE"), selected, request.get("answer"));
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
        expireTurn(room);
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

    private String displayName(JsonNode request) {
        String displayName = request.path("displayName").asText().trim();
        return displayName.isBlank() ? "Spieler" : displayName;
    }

    private void assignPlayer(Room room, Player player) {
        int team = 0;
        if ("random".equals(room.assignmentMode)) {
            team = ThreadLocalRandom.current().nextInt(room.teamCount);
        } else {
            int smallest = Integer.MAX_VALUE;
            for (int index = 0; index < room.teamCount; index++) {
                int size = room.teams.computeIfAbsent(index, Team::new).members.size();
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
                return;
            }
        }
        room.currentTeam = nextActiveTeam(room);
        room.turnDeadline = Instant.now().plusSeconds(TURN_SECONDS);
    }

    private void expireTurn(Room room) {
        if (room.phase != Phase.PLAYING || room.turnDeadline == null || Instant.now().isBefore(room.turnDeadline)) return;
        Team team = currentTeam(room);
        room.selectedCard = null;
        team.passed = true;
        advance(room);
    }

    private int nextActiveTeam(Room room) {
        List<Integer> active = activeTeams(room);
        if (active.isEmpty()) return room.currentTeam;
        int currentIndex = active.indexOf(room.currentTeam);
        return active.get((currentIndex + 1) % active.size());
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
    }

    private JsonNode question(Room room) {
        JsonNode questions = questions(room);
        if (!questions.isArray() || room.questionIndex >= questions.size()) throw new IllegalStateException("Keine Frage verfügbar.");
        return questions.get(room.questionIndex);
    }

    private JsonNode questions(Room room) { return room.game.path("fragen"); }
    private JsonNode options(Room room) { return question(room).path("antwortmoeglichkeiten"); }
    private JsonNode card(Room room, int id) {
        int index = 0;
        for (JsonNode option : options(room)) {
            index++;
            if (option.path("id").asInt(-1) == id || option.path("id").asInt(-1) == -1 && id == index) return option;
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
        snapshot.put("hostId", room.players.values().stream().filter(player -> player.host).map(player -> player.id).findFirst().orElse(null));
        snapshot.put("players", room.players.values().stream().map(Player::asMap).toList());
        snapshot.put("teams", room.teams.values().stream().map(Team::asMap).toList());
        snapshot.put("currentQuestion", questionSnapshot(room));
        snapshot.put("questionIndex", room.questionIndex);
        snapshot.put("questionsTotal", questions(room).size());
        snapshot.put("currentTeam", room.currentTeam);
        snapshot.put("selectedCard", room.selectedCard);
        snapshot.put("turnDeadline", room.turnDeadline == null ? null : room.turnDeadline.toEpochMilli());
        snapshot.put("lastAnswer", room.lastAnswer);
        return snapshot;
    }

    private Map<String, Object> answerFeedback(Room room, Team team, JsonNode card, int cardId, boolean correct, String playerId) {
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
            int id = option.path("id").asInt(++index);
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
            room.players.keySet().stream().map(connections::get).filter(connection -> connection != null && connection.isOpen()).forEach(connection -> connection.sendTextAndAwait(message));
        } catch (Exception ignored) { }
    }

    private int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }

    private enum Phase { LOBBY, PLAYING, FINISHED }

    private static final class Room {
        final String code;
        final String gameId;
        final JsonNode game;
        final int teamCount;
        final String assignmentMode;
        final Map<String, Player> players = new LinkedHashMap<>();
        final Map<Integer, Team> teams = new LinkedHashMap<>();
        final Map<Integer, Boolean> resolvedCards = new LinkedHashMap<>();
        Phase phase = Phase.LOBBY;
        int questionIndex;
        int currentTeam;
        Integer selectedCard;
        Instant turnDeadline;
        Map<String, Object> lastAnswer;

        Room(String code, String gameId, JsonNode game, int teamCount, String assignmentMode) {
            this.code = code; this.gameId = gameId; this.game = game; this.teamCount = teamCount; this.assignmentMode = assignmentMode;
            for (int index = 0; index < teamCount; index++) teams.put(index, new Team(index));
        }
    }

    private static final class Player {
        final String id;
        final String name;
        final boolean host;
        int team;

        Player(String id, String name, boolean host) { this.id = id; this.name = name; this.host = host; }
        Map<String, Object> asMap() { return Map.of("id", id, "name", name, "host", host, "team", team); }
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
