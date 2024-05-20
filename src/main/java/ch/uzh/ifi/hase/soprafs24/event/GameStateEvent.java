package ch.uzh.ifi.hase.soprafs24.event;

import ch.uzh.ifi.hase.soprafs24.entity.Card;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

import java.util.List;
import java.util.Map;

@Setter
@Getter
public class GameStateEvent extends ApplicationEvent {
    private Long gameId;
    private Card topMostCardPlayPile;
    private Map<String, Integer> remainingCardStats;
    private Integer numberOfPlayers;
    private List<String> playerNames;
    private List<Long> playerIds;
    private List<String> playerAvatars;

    public GameStateEvent(Object source, Long gameId, Card topMostCardPlayPile, Map<String, Integer> remainingCardStats, Integer numberOfPlayers, List<String> playerNames, List<Long> playerIds, List<String> playerAvatars) {
        super(source);
        this.gameId = gameId;
        this.topMostCardPlayPile = topMostCardPlayPile;
        this.remainingCardStats = remainingCardStats;
        this.numberOfPlayers = numberOfPlayers;
        this.playerNames = playerNames;
        this.playerIds = playerIds;
        this.playerAvatars = playerAvatars;
    }
}