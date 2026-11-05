package at.htlleonding.gamebasedlearning.games;

import at.htlleonding.gamebasedlearning.users.AppUser;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.LocalDate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "games")
public class Game extends PanacheEntityBase {
    @Id
    public String id;

    @JsonProperty("spiel_typ")
    @Column(name = "game_type", nullable = false, length = 80)
    public String gameType;

    @JsonProperty("lehrer")
    @Column(name = "teacher_name", nullable = false, length = 160)
    public String teacher;

    @JsonIgnore
    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "created_by_id")
    public AppUser createdBy;

    @JsonIgnore
    @Column(name = "family_id", nullable = false)
    public UUID familyId;

    @JsonIgnore
    @Column(name = "copied_from_game_id")
    public String copiedFromGameId;

    @JsonIgnore
    @Column(name = "current_version_id")
    public UUID currentVersionId;

    @JsonIgnore
    @Column(name = "deleted_at")
    public Instant deletedAt;

    @Column(nullable = false, length = 160)
    public String name;

    @JsonProperty("status")
    @Column(name = "visibility", nullable = false, length = 16)
    public String visibility = "PUBLIC";

    @JsonProperty("besitzerId")
    public UUID ownerId() { return createdBy == null ? null : createdBy.id; }

    @JsonProperty(value = "spielbar", access = JsonProperty.Access.READ_ONLY)
    public boolean playable() { return unavailableReason() == null; }

    @JsonProperty(value = "startHinweis", access = JsonProperty.Access.READ_ONLY)
    public String unavailableReason() {
        if (!"Smart10".equals(gameType) && !"JustOne".equals(gameType))
            return "Dieses Spiel hat noch kein vollständiges Smart10-Fragenset. Bitte wähle ein anderes Spiel.";
        if (questions == null || questions.isEmpty()) return "Dieses Spiel enthält noch keine Fragen.";
        for (Question question : questions) {
            if (question == null || question.text == null || question.text.isBlank())
                return "Dieses Spiel enthält noch eine unvollständige Frage.";
            if ("Smart10".equals(gameType)) {
                try { Smart10Cards.validate(question); }
                catch (IllegalArgumentException invalid) {
                    return "Jede Smart10-Frage benötigt zwölf vollständige Karten mit passenden Lösungen.";
                }
            } else if (question.answerOptions == null || question.answerOptions.size() != 5
                    || question.answerOptions.stream().anyMatch(option -> option == null || option.text == null || option.text.isBlank()))
                return "Jede Just-One-Karte benötigt fünf vollständige Begriffe.";
        }
        return null;
    }

    @JsonProperty("beschreibung")
    @Column(length = 2000)
    public String description;

    @JsonProperty("fach")
    @Column(length = 120)
    public String subject;

    @JsonProperty("zweige")
    @Transient
    public List<String> branches = new ArrayList<>();

    @JsonProperty("erstellungsdatum")
    @Column(name = "created_at")
    public LocalDate creationDate;

    @JsonProperty("gespielte_runden")
    @Column(name = "played_rounds")
    public int playedRounds;

    @JsonProperty("fragen")
    @Transient
    public List<Question> questions = new ArrayList<>();

    @JsonProperty("tags")
    @Transient
    public List<String> tags;

    public void prepareCollections() {
        if (branches == null) {
            branches = new ArrayList<>();
        }
        if (questions == null) {
            questions = new ArrayList<>();
        }
        if (tags == null) {
            tags = new ArrayList<>();
        }
        questions.forEach(Question::prepareCollections);
    }
}
