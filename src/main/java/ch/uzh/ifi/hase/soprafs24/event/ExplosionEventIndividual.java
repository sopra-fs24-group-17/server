package ch.uzh.ifi.hase.soprafs24.event;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

@Setter
@Getter
public class ExplosionEventIndividual extends ApplicationEvent {
    private Long gameId;
    private Long userId;

    public ExplosionEventIndividual(Object source, Long gameId, Long userId) {
        super(source);
        this.gameId = gameId;
        this.userId = userId;
    }
}

