package ch.uzh.ifi.hase.soprafs24.event;

import org.springframework.context.ApplicationEvent;

public class GameCreationEvent extends ApplicationEvent {
    private final Long gameId;
    private final String initiatingUsername;

    public GameCreationEvent(Object source, Long gameId, String initiatingUsername) {
        super(source);
        this.gameId = gameId;
        this.initiatingUsername = initiatingUsername;
    }

    public Long getGameId() {
        return gameId;
    }

    public String getInitiatingUsername() {
        return initiatingUsername;
    }
}
