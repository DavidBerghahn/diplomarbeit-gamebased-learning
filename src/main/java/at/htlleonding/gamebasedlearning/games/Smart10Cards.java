package at.htlleonding.gamebasedlearning.games;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class Smart10Cards {
    private Smart10Cards() { }

    static void validate(Question question) {
        if (!List.of("TRUE_FALSE", "FREE_TEXT", "ORDERING").contains(question.type)
                || question.smart10Cards == null || question.smart10Cards.size() != 12)
            throw new IllegalArgumentException("Smart10 requires exactly twelve typed cards");
        Set<Integer> positions = new HashSet<>();
        Set<Integer> ranks = new HashSet<>();
        for (Smart10Card card : question.smart10Cards) {
            if (card == null || card.position() < 1 || card.position() > 12
                    || !positions.add(card.position()) || card.text() == null || card.text().isBlank())
                throw new IllegalArgumentException("Invalid Smart10 card or position");
            Object solution = card.solution();
            switch (question.type) {
                case "TRUE_FALSE" -> {
                    if (!(solution instanceof Boolean)) throw new IllegalArgumentException("Boolean solution required");
                }
                case "FREE_TEXT" -> {
                    if (!(solution instanceof String text) || text.isBlank())
                        throw new IllegalArgumentException("Text solution required");
                }
                case "ORDERING" -> {
                    if (!(solution instanceof Integer rank) || rank < 1 || rank > 12 || !ranks.add(rank))
                        throw new IllegalArgumentException("Ordering solutions must be ranks 1 to 12");
                }
                default -> throw new IllegalArgumentException("Unsupported Smart10 mode");
            }
        }
        if (positions.size() != 12 || ("ORDERING".equals(question.type) && ranks.size() != 12))
            throw new IllegalArgumentException("Smart10 positions and ranks must cover 1 to 12");
    }

    static boolean correct(String mode, Smart10Card card, JsonNode answer) {
        if (answer == null || answer.isNull()) return false;
        return switch (mode) {
            case "TRUE_FALSE" -> answer.isBoolean() && answer.booleanValue() == (Boolean) card.solution();
            case "FREE_TEXT" -> answer.isTextual()
                    && answer.textValue().trim().equalsIgnoreCase(((String) card.solution()).trim());
            case "ORDERING" -> answer.isIntegralNumber() && answer.intValue() == (Integer) card.solution();
            default -> throw new IllegalArgumentException("Unsupported Smart10 mode");
        };
    }
}
