package at.htlleonding.gamebasedlearning.games;

import at.htlleonding.gamebasedlearning.users.AppUser;
import at.htlleonding.gamebasedlearning.users.UserRole;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class Smart10Tickets {
    private static final long TICKET_SECONDS = 45;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Ticket> pending = new ConcurrentHashMap<>();
    private final Map<String, UUID> bound = new ConcurrentHashMap<>();

    @Inject EntityManager entityManager;
    @Inject Smart10Feature feature;

    public record Actor(UUID userId, UserRole role, String displayName) { }
    private record Ticket(UUID userId, Instant expiresAt) { }

    public String issue(AppUser user) {
        feature.requireEnabled();
        if (user == null || user.id == null || !user.active)
            throw new IllegalArgumentException("Verified active user required");
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        pending.put(value, new Ticket(user.id, Instant.now().plusSeconds(TICKET_SECONDS)));
        return value;
    }

    public Actor bind(String connectionId, String value) {
        feature.requireEnabled();
        if (connectionId == null || value == null || bound.containsKey(connectionId))
            throw new IllegalArgumentException("Invalid ticket binding");
        Ticket ticket = pending.remove(value);
        if (ticket == null || !Instant.now().isBefore(ticket.expiresAt()))
            throw new IllegalArgumentException("Ticket expired or already used");
        Actor actor = load(ticket.userId());
        if (bound.putIfAbsent(connectionId, actor.userId()) != null)
            throw new IllegalArgumentException("Connection already bound");
        return actor;
    }

    public Actor actor(String connectionId) {
        feature.requireEnabled();
        UUID userId = bound.get(connectionId);
        if (userId == null) throw new IllegalArgumentException("WebSocket identity required");
        return load(userId);
    }

    public void unbind(String connectionId) { if (connectionId != null) bound.remove(connectionId); }

    private Actor load(UUID id) {
        AppUser user = entityManager.find(AppUser.class, id);
        if (user == null || !user.active) throw new IllegalArgumentException("Active account required");
        return new Actor(user.id, user.role, user.displayName);
    }
}
