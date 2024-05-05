package ch.uzh.ifi.hase.soprafs24.event;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

@Setter
@Getter
public class NopeEvent extends ApplicationEvent {
    private Long gameId;
    private String activePlayer;

    public NopeEvent(Object source, Long gameId, String activePlayer) {
        super(source);
        this.gameId = gameId;
        this.activePlayer = activePlayer;
    }

}