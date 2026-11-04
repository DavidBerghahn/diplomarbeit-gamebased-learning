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

    @JsonIgnore
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
