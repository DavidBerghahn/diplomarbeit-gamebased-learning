package at.htlleonding.gamebasedlearning.games;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "questions")
public class Question extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    public Long id;

    @JsonProperty("typ")
    @Column(nullable = false, length = 80)
    public String type;

    @JsonProperty("frage")
    @Column(nullable = false, length = 2000)
    public String text;

    @JsonProperty("antwortmoeglichkeiten")
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "question_id")
    @OrderColumn(name = "answer_order")
    public List<AnswerOption> answerOptions = new ArrayList<>();

    public void prepareCollections() {
        if (answerOptions == null) {
            answerOptions = new ArrayList<>();
        }
    }
}
