package at.htlleonding.gamebasedlearning.games;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.LinkedHashMap;
import java.util.Map;

@ApplicationScoped
@WebSocket(path = "/user-socket")
public class GameSocket {
    @Inject
    GameRepository gameRepository;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    MultiplayerGameService multiplayerGameService;

    @OnTextMessage
    public String onMessage(String message, WebSocketConnection connection) throws JsonProcessingException {
        String requestId = null;
        try {
            com.fasterxml.jackson.databind.JsonNode request = objectMapper.readTree(message);
            requestId = request.path("requestId").isMissingNode() ? null : request.path("requestId").asText();
            String type = request.path("type").asText();

            if (type.startsWith("mp_")) {
                return multiplayerResponse(requestId, type, objectMapper.readTree(message), connection);
            }

            return switch (type) {
                case "get_games" -> response(requestId, "games", gameRepository.findAll());
                case "get_games_by_type" -> response(requestId, "games", gameRepository.findByType(request.path("gameType").asText()));
                case "get_game" -> gameResponse(requestId, request.path("id").asText());
                default -> error(requestId, "Unbekannter WebSocket-Befehl: " + type);
            };
        } catch (Exception e) {
            return error(requestId, "WebSocket-Nachricht konnte nicht verarbeitet werden: " + e.getMessage());
        }
    }

    @OnClose
    public void onClose(WebSocketConnection connection) {
        multiplayerGameService.leave(connection);
    }

    private String multiplayerResponse(String requestId, String type, com.fasterxml.jackson.databind.JsonNode request, WebSocketConnection connection) throws JsonProcessingException {
        Map<String, Object> data = switch (type) {
            case "mp_create" -> multiplayerGameService.createRoom(connection, request);
            case "mp_join" -> multiplayerGameService.joinRoom(connection, request);
            case "mp_start" -> multiplayerGameService.startRoom(connection, request);
            case "mp_select_card" -> multiplayerGameService.selectCard(connection, request);
            case "mp_answer" -> multiplayerGameService.answer(connection, request);
            case "mp_pass" -> multiplayerGameService.pass(connection, request);
            default -> throw new IllegalStateException("Unbekannter Multiplayer-Befehl");
        };
        data.put("selfId", connection.id());
        return response(requestId, "room_state", data);
    }

    private String gameResponse(String requestId, String id) throws JsonProcessingException {
        if (id == null || id.isBlank()) {
            return error(requestId, "Es wurde keine Spiel-ID mitgeschickt.");
        }

        return gameRepository.findById(id)
                .map(game -> {
                    try {
                        return response(requestId, "game", game);
                    } catch (JsonProcessingException e) {
                        throw new IllegalStateException(e);
                    }
                })
                .orElseGet(() -> {
                    try {
                        return error(requestId, "Spiel nicht gefunden: " + id);
                    } catch (JsonProcessingException e) {
                        throw new IllegalStateException(e);
                    }
                });
    }

    private String response(String requestId, String type, Object data) throws JsonProcessingException {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("requestId", requestId);
        response.put("type", type);
        response.put("data", data);
        return objectMapper.writeValueAsString(response);
    }

    private String error(String requestId, String message) throws JsonProcessingException {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("requestId", requestId);
        response.put("type", "error");
        response.put("message", message);
        return objectMapper.writeValueAsString(response);
    }
}
