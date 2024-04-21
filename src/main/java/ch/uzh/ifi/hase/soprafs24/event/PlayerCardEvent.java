package ch.uzh.ifi.hase.soprafs24.event;

import ch.uzh.ifi.hase.soprafs24.entity.Card;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Setter
@Getter
public class PlayerCardEvent extends ApplicationEvent {
    private Long userId;
    private Long gameId;
    private List<Card> playerCards;

    public PlayerCardEvent(Object source, Long userId, Long gameId, List<Card> playerCards) {
        super(source);
        this.userId = userId;
        this.gameId = gameId;
        this.playerCards = playerCards;
    }
}
