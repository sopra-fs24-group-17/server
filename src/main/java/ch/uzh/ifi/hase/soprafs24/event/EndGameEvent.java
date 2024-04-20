package ch.uzh.ifi.hase.soprafs24.event;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

@Setter
@Getter
public class EndGameEvent extends ApplicationEvent {

    private String userName;

    private Long gameId;

    public EndGameEvent(Object source, String userName, Long gameId) {
        super(source);
        this.userName = userName;
        this.gameId = gameId;
    }
}