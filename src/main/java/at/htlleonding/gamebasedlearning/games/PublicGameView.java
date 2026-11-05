package at.htlleonding.gamebasedlearning.games;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PublicGameView {
    @Inject ObjectMapper mapper;

    public ObjectNode of(Game game) {
        // Serialize before redacting so playability reflects the complete stored game.
        ObjectNode view = mapper.valueToTree(game);
        if ("JustOne".equals(game.gameType)) {
            JsonNode questions = view.path("fragen");
            if (questions.isArray()) {
                for (JsonNode question : questions) {
                    if (question instanceof ObjectNode object) {
                        object.set("antwortmoeglichkeiten", mapper.createArrayNode());
                        object.remove("karten");
                    }
                }
            }
        }
        return view;
    }
}
