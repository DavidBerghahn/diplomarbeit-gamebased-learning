# Auth and User Management Architecture

This project keeps authentication separate from game, progress, and AI logic.

## Current Development Mode

The backend currently uses `MockAuthProvider`.

It reads optional HTTP headers:

```text
X-Mock-User: it220269
X-Mock-Name: David Berghahn
X-Mock-Class: 5BHITM
X-Mock-Role: STUDENT
```

If no headers are provided, the backend creates a default student user.

## Later Keycloak Mode

The future Keycloak/OIDC implementation should replace only the `AuthProvider`.

The application should still work with the same internal `app_user` table:

```text
external_subject -> OIDC sub claim or Keycloak user id
username         -> preferred_username
display_name     -> name
school_class     -> class/group claim when available
role             -> mapped from OIDC roles/groups
```

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
