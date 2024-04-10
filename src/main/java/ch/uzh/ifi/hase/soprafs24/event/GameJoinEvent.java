package ch.uzh.ifi.hase.soprafs24.event;

import org.springframework.context.ApplicationEvent;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class GameJoinEvent extends ApplicationEvent {
    private String userName;
    private Long gameId;

    public GameJoinEvent(Object source, String userName, Long gameId) {
        super(source);
        this.userName = userName;
        this.gameId = gameId;
    }
}
