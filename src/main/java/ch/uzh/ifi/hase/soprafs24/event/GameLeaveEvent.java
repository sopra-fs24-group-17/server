package ch.uzh.ifi.hase.soprafs24.event;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

@Getter
@Setter
public class GameLeaveEvent extends ApplicationEvent {
    private String userName;
    private Long gameId;
    private Integer maxPlayerCount;
    private Integer currentPlayerCount;

    public GameLeaveEvent(Object source, String userName, Long gameId, Integer maxPlayerCount, Integer currentPlayerCount) {
        super(source);
        this.userName = userName;
        this.gameId = gameId;
        this.maxPlayerCount = maxPlayerCount;
        this.currentPlayerCount = currentPlayerCount;
    }
}
