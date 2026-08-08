# Auth and User Management Architecture

This project keeps authentication separate from game, progress, and AI logic.

## Keycloak Mode

The backend uses Keycloak/OIDC as its only authentication mechanism.

The application should still work with the same internal `app_user` table:

```text
external_subject -> OIDC sub claim or Keycloak user id
username         -> preferred_username
display_name     -> name
school_class     -> class/group claim or parsed from distinguishedName
role             -> mapped from custom_roles, groups, realm roles, or client roles
```

Herr Aberger's LDAP demo shows that the school LDAP data is exposed through
Keycloak. The backend must not talk to LDAP directly. It receives a normal
Bearer token from the frontend and reads claims such as:

```text
sub
preferred_username
name
distinguishedName
custom_roles
```

For local development start the backend with:

```bash
./mvnw quarkus:dev \
  -Dquarkus.console.enabled=false \
  -Dquarkus.analytics.disabled=true \
  -Ddebug=false
```

The current default values are based on the LDAP demo:

```text
OIDC_AUTH_SERVER_URL=https://auth.htl-leonding.ac.at/realms/2526_5bhitm
OIDC_CLIENT_ID=frontend
```

If the final project gets its own realm or client, only these environment
variables have to change.

Game and AI modules must not parse tokens directly. They should receive the
internal `AppUser.id` from controllers/services and store their data against
that internal ID.

## Boundary for Other Team Members

Games and AI integrations should call:

```text
GET /api/auth/me
GET /api/users/me
```

Future game/progress/AI endpoints should accept the current user from the auth
layer and persist only the internal user id. That keeps the login technology
replaceable.
