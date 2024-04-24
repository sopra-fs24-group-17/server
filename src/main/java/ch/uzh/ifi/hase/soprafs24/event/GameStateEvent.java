package ch.uzh.ifi.hase.soprafs24.event;

import ch.uzh.ifi.hase.soprafs24.entity.Card;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

import java.util.Map;

@Setter
@Getter
public class GameStateEvent extends ApplicationEvent {
    private Long gameId;
    private Card topMostCardPlayPile;
    private Map<String, Integer> remainingCardStats;

    public GameStateEvent(Object source, Long gameId, Card topMostCardPlayPile, Map<String, Integer> remainingCardStats) {
        super(source);
        this.gameId = gameId;
        this.topMostCardPlayPile = topMostCardPlayPile;
        this.remainingCardStats = remainingCardStats;
    }
}