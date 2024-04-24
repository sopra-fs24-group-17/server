package ch.uzh.ifi.hase.soprafs24.event;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

@Setter
@Getter
public class DrawCardsEvent extends ApplicationEvent {
    private Integer numberOfCards;
    private Long gameId;
    private String invokingPlayerUserName;

    public DrawCardsEvent(Object source, Integer numberOfCards, Long gameId, String invokingPlayerUserName) {
        super(source);
        this.numberOfCards = numberOfCards;
        this.gameId = gameId;
        this.invokingPlayerUserName = invokingPlayerUserName;
    }
}
