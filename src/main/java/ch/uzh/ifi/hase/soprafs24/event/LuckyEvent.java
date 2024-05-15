package ch.uzh.ifi.hase.soprafs24.event;

import ch.uzh.ifi.hase.soprafs24.entity.Card;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Setter
@Getter
public class LuckyEvent extends ApplicationEvent {
    private Long userId;
    private Long gameId;
    private Card randomCard;

    public LuckyEvent(Object source, Long userId, Long gameId, Card randomCard) {
        super(source);
        this.userId = userId;
        this.gameId = gameId;
        this.randomCard = randomCard;
    }
}
