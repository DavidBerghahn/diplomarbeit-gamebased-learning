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
    @ConfigProperty(name = "gamebased.demo-data.enabled", defaultValue = "false")
    boolean demoDataEnabled;

    @Inject
    GameRepository gameRepository;

    @Transactional
    void seed(@Observes StartupEvent event) {
        if (!demoDataEnabled) return;
        if (gameRepository.findAll().isEmpty()) seedLegacy();
        seedPlayableSamples();
    }

    private void seedLegacy() {
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
        gameRepository.create(quiz, null);

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
        gameRepository.create(duel, null);
    }

    private void seedPlayableSamples() {
        if (Game.count("id", "sample-smart10-programming") == 0) {
            Game smart10 = sample("sample-smart10-programming", "Smart10", "Programmieren: Smart10 zum Ausprobieren");
            Question question = new Question();
            question.type = "TRUE_FALSE";
            question.text = "Welche Aussagen über das Programmieren sind richtig?";
            String[] statements = {
                    "Eine Variable kann einen Wert speichern.", "Eine Schleife kann Anweisungen wiederholen.",
                    "Ein Boolean beschreibt einen Wahrheitswert.", "Eine Funktion kann Parameter erhalten.",
                    "Git ist ein Versionsverwaltungssystem.", "Ein Array kann mehrere Elemente enthalten.",
                    "Java und JavaScript sind dieselbe Sprache.", "HTML ist eine relationale Datenbank.",
                    "Ein Compiler ist ein Bildschirm.", "Ein Kommentar wird immer als Programmcode ausgeführt.",
                    "Eine Endlosschleife endet nach genau einem Durchlauf.", "SQL ist ein Bilddateiformat."
            };
            question.smart10Cards = new java.util.ArrayList<>();
            for (int i=0; i<statements.length; i++)
                question.smart10Cards.add(new Smart10Card(null, i+1, statements[i], i<6));
            smart10.questions = List.of(question);
            gameRepository.create(smart10, null);
        }
        if (Game.count("id", "sample-just-one-programming") == 0) {
            Game justOne = sample("sample-just-one-programming", "JustOne", "Programmieren: Just One zum Ausprobieren");
            Question question = new Question();
            question.type = "JUST_ONE";
            question.text = "Begriffe aus der Programmierung";
            question.answerOptions = java.util.stream.Stream.of("Variable", "Schleife", "Klasse", "Compiler", "Datenbank")
                    .map(text -> { AnswerOption option = new AnswerOption(); option.text = text; return option; }).toList();
            justOne.questions = List.of(question);
            gameRepository.create(justOne, null);
        }
    }

    private Game sample(String id, String type, String name) {
        Game game = new Game();
        game.id=id; game.gameType=type; game.name=name; game.teacher="Beispielspiel";
        game.description="Vollständiges lokales Beispielspiel zum Ausprobieren.";
        game.subject="Programmieren"; game.creationDate=LocalDate.now();
        return game;
    }
}
