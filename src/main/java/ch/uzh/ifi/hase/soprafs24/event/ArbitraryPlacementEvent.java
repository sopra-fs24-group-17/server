package ch.uzh.ifi.hase.soprafs24.event;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

@Setter
@Getter
public class ArbitraryPlacementEvent extends ApplicationEvent {
    private Long gameId;
    private String invokingPlayerUserName;

    public ArbitraryPlacementEvent(Object source, Long gameId, String invokingPlayerUserName) {
        super(source);
        this.gameId = gameId;
        this.invokingPlayerUserName = invokingPlayerUserName;
    }
}