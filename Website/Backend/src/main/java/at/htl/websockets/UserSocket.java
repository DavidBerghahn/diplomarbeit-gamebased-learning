package at.htl.websockets;

import at.htl.repositories.GameRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.websockets.next.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.LinkedHashMap;
import java.util.Map;

@ApplicationScoped
@WebSocket(path="/user-socket")
public class UserSocket {
    @Inject
    WebSocketConnection connection;
    @Inject
    OpenConnections connections;
    @Inject
    GameRepository gameRepository;
    @Inject
    ObjectMapper objectMapper;

    @OnOpen
    public void onOpen() {
        System.out.println("WebSocket verbunden: " + connection.id());
    }

    @OnClose
    public void onClose() {
        System.out.println("WebSocket geschlossen: " + connection.id());
    }

    @OnTextMessage
    public String onMessage(String message) throws JsonProcessingException {
        try {
            Map<String, String> request = objectMapper.readValue(message, new TypeReference<>() {});
            String requestId = request.get("requestId");

            return switch (request.get("type")) {
                case "get_games" -> response(requestId, "games", gameRepository.findAll());
                case "get_games_by_type" -> response(requestId, "games", gameRepository.findByType(request.get("gameType")));
                case "get_game" -> gameResponse(requestId, request.get("id"));
                default -> error(requestId, "Unbekannter WebSocket-Befehl: " + request.get("type"));
            };
        } catch (Exception e) {
            return error(null, "WebSocket-Nachricht konnte nicht verarbeitet werden: " + e.getMessage());
        }
    }

    public void broadcast(String text) {
        System.out.println(text);
        connections.listAll().forEach(connection -> {
            connection.sendTextAndAwait(text);
        });
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
