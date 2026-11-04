package at.htlleonding.gamebasedlearning.games;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "questions")
public class Question extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty("id")
    public Long id;

    @JsonIgnore
    @Column(name = "game_id")
    public String gameId;

    @JsonIgnore
    @Column(name = "question_order")
    public Integer questionOrder;

    @JsonIgnore
    @Column(name = "current_version_id")
    public UUID currentVersionId;

    @JsonProperty("typ")
    @Column(nullable = false, length = 80)
    public String type;

    @JsonProperty("frage")
    @Column(nullable = false, length = 2000)
    public String text;

    @JsonProperty("antwortmoeglichkeiten")
    @Transient
    public List<AnswerOption> answerOptions = new ArrayList<>();

    @JsonProperty(value = "karten", access = Access.WRITE_ONLY)
    @Transient
    public List<Smart10Card> smart10Cards;

    @JsonIgnore
    @Transient
    public Boolean trueFalseCorrect;

    @JsonIgnore
    @Transient
    public List<String> orderingItems = new ArrayList<>();

    @JsonIgnore
    @Transient
    public List<String> acceptedTextAnswers = new ArrayList<>();

    public void prepareCollections() {
        if (answerOptions == null) {
            answerOptions = new ArrayList<>();
        }
        if (smart10Cards == null) smart10Cards = new ArrayList<>();
        if (orderingItems == null) orderingItems = new ArrayList<>();
        if (acceptedTextAnswers == null) acceptedTextAnswers = new ArrayList<>();
    }
}
