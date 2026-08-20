package at.htlleonding.gamebasedlearning.games;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.runtime.StartupEvent;

import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class GameDemoData {
    @ConfigProperty(name = "gamebased.demo-data.enabled", defaultValue = "true")
    boolean demoDataEnabled;

    @Inject
    GameRepository gameRepository;

    @Transactional
    void seed(@Observes StartupEvent event) {
        if (!demoDataEnabled || !gameRepository.findAll().isEmpty()) {
            return;
        }

        Game quiz = new Game();
        quiz.id = "quizbattle-001";
        quiz.gameType = "Quizbattle";
        quiz.teacher = "Demo Lehrer";
        quiz.name = "Programmieren Grundlagen";
        quiz.description = "Ein Quiz zu Grundlagen der Programmierung.";
        quiz.subject = "Programmieren";
        quiz.creationDate = LocalDate.now();
        quiz.branches = List.of("SSE", "DDP");
        quiz.playedRounds = 0;

        Question question = new Question();
        question.type = "single_choice";
        question.text = "Welcher Datentyp speichert Wahrheitswerte?";

        AnswerOption correct = new AnswerOption();
        correct.text = "boolean";
        correct.correct = true;

        AnswerOption wrong = new AnswerOption();
        wrong.text = "String";
        wrong.correct = false;

        question.answerOptions = List.of(correct, wrong);
        quiz.questions = List.of(question);
        quiz.prepareCollections();
        quiz.persist();

        Game duel = new Game();
        duel.id = "duell-um-die-welt-001";
        duel.gameType = "DuellUmDieWelt";
        duel.teacher = "Demo Lehrer";
        duel.name = "IT Around the World";
        duel.description = "Eine Reise durch IT-Fragen aus verschiedenen Ländern.";
        duel.subject = "Informatik";
        duel.creationDate = LocalDate.now();
        duel.branches = List.of("CSI", "SSE");
        duel.playedRounds = 0;
        duel.prepareCollections();
        duel.persist();
    }
}
