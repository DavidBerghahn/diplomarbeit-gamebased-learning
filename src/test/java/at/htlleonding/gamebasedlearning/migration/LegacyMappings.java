package at.htlleonding.gamebasedlearning.migration;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Frozen pre-V2 entity mappings. Used only by the isolated migration fixture. */
final class LegacyMappings {
    private LegacyMappings() { }

    enum Role { STUDENT, TEACHER, ADMIN }

    @Entity(name = "LegacyAppUser")
    @Table(name = "app_user")
    static class User {
        @Id @GeneratedValue(strategy = GenerationType.UUID)
        UUID id;
        @Column(name = "external_subject", nullable = false, unique = true, length = 160)
        String externalSubject;
        @Column(nullable = false, length = 80)
        String username;
        @Column(name = "display_name", nullable = false, length = 160)
        String displayName;
        @Column(name = "school_class", length = 40)
        String schoolClass;
        @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24)
        Role role;
        @Column(nullable = false)
        boolean active;
        @Column(name = "created_at", nullable = false)
        Instant createdAt;
        @Column(name = "updated_at", nullable = false)
        Instant updatedAt;
    }

    @Entity(name = "LegacyGame")
    @Table(name = "games")
    static class Game {
        @Id String id;
        @Column(name = "game_type", nullable = false, length = 80)
        String gameType;
        @Column(name = "teacher_name", nullable = false, length = 160)
        String teacher;
        @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by_id")
        User createdBy;
        @Column(nullable = false, length = 160)
        String name;
        @Column(length = 2000)
        String description;
        @Column(length = 120)
        String subject;
        @ElementCollection(fetch = FetchType.EAGER)
        @CollectionTable(name = "game_branches", joinColumns = @JoinColumn(name = "game_id"))
        @Column(name = "branch", length = 200)
        @OrderColumn(name = "branch_order")
        List<String> branches;
        @Column(name = "created_at")
        LocalDate creationDate;
        @Column(name = "played_rounds")
        int playedRounds;
        @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
        @JoinColumn(name = "game_id") @OrderColumn(name = "question_order")
        List<Question> questions;
    }

    @Entity(name = "LegacyQuestion")
    @Table(name = "questions")
    static class Question {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
        Long id;
        @Column(nullable = false, length = 80)
        String type;
        @Column(nullable = false, length = 2000)
        String text;
        @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
        @JoinColumn(name = "question_id") @OrderColumn(name = "answer_order")
        List<Option> answerOptions;
    }

    @Entity(name = "LegacyOption")
    @Table(name = "answer_options")
    static class Option {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
        Long id;
        @Column(nullable = false, length = 1000)
        String text;
        @Column(name = "is_correct")
        boolean correct;
    }
}
