package at.htlleonding.gamebasedlearning.users;

import at.htlleonding.gamebasedlearning.auth.AuthenticatedUser;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class UserService {
    @Transactional
    public synchronized AppUser getOrCreateFromIdentity(AuthenticatedUser identity) {
        AppUser user = AppUser.findByExternalSubject(identity.externalSubject())
                .orElseGet(() -> createUser(identity));

        user.username = identity.username();
        user.displayName = identity.displayName();
        user.schoolClass = identity.schoolClass();
        user.role = mergedRole(user.role, identity.role());
        return user;
    }

    public List<AppUser> listUsers() {
        return AppUser.listAll(Sort.by("username").and("displayName"));
    }

    public AppUser getById(UUID id) {
        return (AppUser) AppUser.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Transactional
    public AppUser updateOwnProfile(AppUser user, UpdateOwnProfileRequest request) {
        if (request.displayName != null && !request.displayName.isBlank()) {
            user.displayName = request.displayName.trim();
        }
        if (request.schoolClass != null) {
            user.schoolClass = blankToNull(request.schoolClass);
        }
        return user;
    }

    @Transactional
    public AppUser updateUser(UUID id, UpdateUserRequest request) {
        AppUser user = getById(id);
        if (request.displayName != null && !request.displayName.isBlank()) {
            user.displayName = request.displayName.trim();
        }
        if (request.schoolClass != null) {
            user.schoolClass = blankToNull(request.schoolClass);
        }
        if (request.role != null) {
            user.role = request.role;
        }
        if (request.active != null) {
            user.active = request.active;
        }
        return user;
    }

    private AppUser createUser(AuthenticatedUser identity) {
        AppUser user = new AppUser();
        user.externalSubject = identity.externalSubject();
        user.username = identity.username();
        user.displayName = identity.displayName();
        user.schoolClass = identity.schoolClass();
        user.role = identity.role();
        user.active = true;
        user.persist();
        return user;
    }

    private UserRole mergedRole(UserRole currentRole, UserRole identityRole) {
        if (currentRole == UserRole.ADMIN) {
            return UserRole.ADMIN;
        }
        if (currentRole == UserRole.TEACHER && identityRole == UserRole.STUDENT) {
            return UserRole.TEACHER;
        }
        return identityRole;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
