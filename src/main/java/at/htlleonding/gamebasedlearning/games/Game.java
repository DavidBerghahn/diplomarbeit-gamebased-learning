package at.htlleonding.gamebasedlearning.games;

import at.htlleonding.gamebasedlearning.users.AppUser;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    public AppUser createdBy;

    @Column(nullable = false, length = 160)
    public String name;

    @JsonProperty("beschreibung")
    @Column(length = 2000)
    public String description;

    @JsonProperty("fach")
    @Column(length = 120)
    public String subject;

    @JsonProperty("zweige")
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "game_branches", joinColumns = @JoinColumn(name = "game_id"))
    @Column(name = "branch", length = 200)
    @OrderColumn(name = "branch_order")
    public List<String> branches = new ArrayList<>();

    @JsonProperty("erstellungsdatum")
    @Column(name = "created_at")
    public LocalDate creationDate;

    @JsonProperty("gespielte_runden")
    @Column(name = "played_rounds")
    public int playedRounds;

    @JsonProperty("fragen")
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "game_id")
    @OrderColumn(name = "question_order")
    public List<Question> questions = new ArrayList<>();

    public void prepareCollections() {
        if (branches == null) {
            branches = new ArrayList<>();
        }
        if (questions == null) {
            questions = new ArrayList<>();
        }
        questions.forEach(Question::prepareCollections);
    }
}
