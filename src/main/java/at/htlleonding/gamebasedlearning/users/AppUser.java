package at.htlleonding.gamebasedlearning.users;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class AppUser extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name = "external_subject", nullable = false, unique = true, length = 160)
    public String externalSubject;

    @Column(nullable = false, length = 80)
    public String username;

    @Column(name = "display_name", nullable = false, length = 160)
    public String displayName;

    @Column(name = "school_class", length = 40)
    public String schoolClass;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    public UserRole role;

    @Column(nullable = false)
    public boolean active = true;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    public static Optional<AppUser> findByExternalSubject(String externalSubject) {
        return find("externalSubject", externalSubject).firstResultOptional();
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
