package at.htlleonding.gamebasedlearning.games;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "answer_options")
public class AnswerOption extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    public Long id;

    @JsonIgnore
    @Column(name = "question_id")
    public Long questionId;

    @JsonIgnore
    @Column(name = "answer_order")
    public Integer answerOrder;

    @JsonIgnore
    @Column(name = "question_version_id")
    public UUID questionVersionId;

    @Column(nullable = false, length = 1000)
    public String text;

    @JsonProperty("ist_richtig")
    @Column(name = "is_correct")
    public boolean correct;
}
