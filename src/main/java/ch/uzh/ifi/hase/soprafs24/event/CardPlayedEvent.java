package ch.uzh.ifi.hase.soprafs24.event;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

@Setter
@Getter
public class CardPlayedEvent extends ApplicationEvent {
    private String internalCode;
    private Long gameId;
    private String invokingPlayerUserName;

    public CardPlayedEvent(Object source, String internalCode, Long gameId, String invokingPlayerUserName) {
        super(source);
        this.internalCode = internalCode;
        this.gameId = gameId;
        this.invokingPlayerUserName = invokingPlayerUserName;
    }
}