package at.htlleonding.gamebasedlearning.auth;

public record AuthConfigResponse(
        String provider,
        String keycloakUrl,
        String keycloakRealm,
        String keycloakClientId
) {
}
