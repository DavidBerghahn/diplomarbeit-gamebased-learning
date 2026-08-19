package at.htl.repositories;

import at.htl.entities.Game;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class GameRepository {

    @Inject
    EntityManager em;

    public List<Game> findAll() {
        return em.createQuery("select g from Game g order by g.gameType, g.name", Game.class)
                .getResultList();
    }

    public List<Game> findByType(String gameType) {
        return em.createQuery("select g from Game g where g.gameType = :gameType order by g.name", Game.class)
                .setParameter("gameType", gameType)
                .getResultList();
    }

    public Optional<Game> findById(String id) {
        return Optional.ofNullable(em.find(Game.class, id));
    }

    @Transactional
    public Game create(Game game) {
        game.prepareCollections();
        em.persist(game);
        return game;
    }

    @Transactional
    public Optional<Game> update(String id, Game changedGame) {
        Game game = em.find(Game.class, id);

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
        Game game = em.find(Game.class, id);

        if (game == null) {
            return false;
        }

        em.remove(game);
        return true;
    }
}
