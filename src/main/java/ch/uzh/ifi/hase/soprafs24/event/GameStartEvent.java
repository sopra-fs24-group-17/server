package ch.uzh.ifi.hase.soprafs24.event;

import ch.uzh.ifi.hase.soprafs24.websocket.dto.CardGetDTO;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Setter
@Getter
public class GameStartEvent extends ApplicationEvent {

    private Long gameId;

    private Long userId;

    private List<CardGetDTO> playerCards;

    public GameStartEvent (Object source,
                           Long gameId,
                           Long userId) {
        super(source);
        this.gameId = gameId;
        this.userId = userId;
    }
}
