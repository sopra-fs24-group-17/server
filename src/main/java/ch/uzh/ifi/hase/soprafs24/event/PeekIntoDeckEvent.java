package ch.uzh.ifi.hase.soprafs24.event;

import ch.uzh.ifi.hase.soprafs24.entity.Card;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Setter
@Getter
public class PeekIntoDeckEvent extends ApplicationEvent {
    private Long gameId;
    private String invokingPlayerUserName;
    private Long userId;
    private List<Card> futureCards;

    public PeekIntoDeckEvent(Object source, Long gameId, String invokingPlayerUserName, Long userId, List<Card> futureCards) {
        super(source);
        this.gameId = gameId;
        this.invokingPlayerUserName = invokingPlayerUserName;
        this.userId = userId;
        this.futureCards = futureCards;
    }
}
