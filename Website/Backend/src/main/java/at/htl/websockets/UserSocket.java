package at.htl.websockets;

import io.quarkus.websockets.next.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
@WebSocket(path="/user-socket")
public class UserSocket {
    @Inject
    WebSocketConnection connection;
    @Inject
    OpenConnections connections;

    @OnOpen
    public void onOpen() {
        System.out.println("WebSocket verbunden: " + connection.id());
    }

    @OnClose
    public void onClose() {
        System.out.println("WebSocket geschlossen: " + connection.id());
    }

    public void broadcast(String text) {
        System.out.println(text);
        connections.listAll().forEach(connection -> {
            connection.sendTextAndAwait(text);
        });
    }
}
