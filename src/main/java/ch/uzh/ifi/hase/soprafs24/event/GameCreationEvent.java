package ch.uzh.ifi.hase.soprafs24.event;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

@Getter
@Setter
public class GameCreationEvent extends ApplicationEvent {
    private final Long gameId;
    private final String initiatingUsername;

    public GameCreationEvent(Object source, Long gameId, String initiatingUsername) {
        super(source);
        this.gameId = gameId;
        this.initiatingUsername = initiatingUsername;
    }
}
