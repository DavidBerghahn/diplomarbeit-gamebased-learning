package at.htlleonding.gamebasedlearning.games;

import at.htlleonding.gamebasedlearning.users.AppUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class GameRepository {
    @Inject
    EntityManager entityManager;

    public List<Game> findAll() {
        return entityManager.createQuery("select g from Game g order by g.gameType, g.name", Game.class)
                .getResultList();
    }

    public List<Game> findByType(String gameType) {
        return entityManager.createQuery("select g from Game g where g.gameType = :gameType order by g.name", Game.class)
                .setParameter("gameType", gameType)
                .getResultList();
    }

    public Optional<Game> findById(String id) {
        return Optional.ofNullable(entityManager.find(Game.class, id));
    }

    @Transactional
    public Game create(Game game, AppUser creator) {
        game.prepareCollections();
        game.createdBy = creator;
        if (isBlank(game.teacher)) {
            game.teacher = creator.displayName;
        }
        if (game.creationDate == null) {
            game.creationDate = LocalDate.now();
        }
        entityManager.persist(game);
        return game;
    }

    @Transactional
    public Optional<Game> update(String id, Game changedGame) {
        Game game = entityManager.find(Game.class, id);
        if (game == null) {
            return Optional.empty();
        }

        game.gameType = changedGame.gameType;
        game.teacher = changedGame.teacher;
        game.name = changedGame.name;
        game.description = changedGame.description;
        game.subject = changedGame.subject;
        game.creationDate = changedGame.creationDate;
        game.playedRounds = changedGame.playedRounds;

        game.branches.clear();
        if (changedGame.branches != null) {
            game.branches.addAll(changedGame.branches);
        }

        game.questions.clear();
        if (changedGame.questions != null) {
            game.questions.addAll(changedGame.questions);
        }
        game.prepareCollections();

        return Optional.of(game);
    }

    @Transactional
    public boolean delete(String id) {
        Game game = entityManager.find(Game.class, id);
        if (game == null) {
            return false;
        }
        entityManager.remove(game);
        return true;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
