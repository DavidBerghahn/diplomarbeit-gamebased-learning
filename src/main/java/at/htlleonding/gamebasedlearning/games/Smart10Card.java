package at.htlleonding.gamebasedlearning.games;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record Smart10Card(UUID id, int position, String text, @JsonProperty("loesung") Object solution) { }
