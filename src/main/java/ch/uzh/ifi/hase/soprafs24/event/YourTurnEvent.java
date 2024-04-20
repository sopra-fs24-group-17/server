package ch.uzh.ifi.hase.soprafs24.event;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

@Setter
@Getter
public class YourTurnEvent extends ApplicationEvent {

    private Long userId;

    private Long gameId;

    public YourTurnEvent(Object source, Long userId, Long gameId) {
        super(source);
        this.userId = userId;
        this.gameId = gameId;
    }
}
