package at.htlleonding.gamebasedlearning.games;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class Smart10Feature {
    @ConfigProperty(name = "gamebased.smart10.persistence.enabled", defaultValue = "false")
    boolean enabled;

    public void requireEnabled() {
        if (!enabled) throw new IllegalStateException("Persistent Smart10 play is disabled");
    }
}
