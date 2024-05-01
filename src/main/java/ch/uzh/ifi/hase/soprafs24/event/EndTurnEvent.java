package ch.uzh.ifi.hase.soprafs24.event;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

@Setter
@Getter
public class EndTurnEvent extends ApplicationEvent {

    private String userName;

    private Long gameId;

    private Long userId;

    public EndTurnEvent(Object source, String userName, Long gameId, Long userId) {
        super(source);
        this.userName = userName;
        this.gameId = gameId;
        this.userId = userId;
    }
}
