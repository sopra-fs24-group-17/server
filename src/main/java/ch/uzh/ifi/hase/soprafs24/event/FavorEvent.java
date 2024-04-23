package ch.uzh.ifi.hase.soprafs24.event;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

@Setter
@Getter
public class FavorEvent extends ApplicationEvent {
    private Long gameId;
    private String invokingPlayerUserName;
    private String targetPlayerUserName;
    private String stealedCard;

    public FavorEvent(Object source, Long gameId, String invokingPlayerUserName, String targetPlayerUserName, String stealedCard) {
        super(source);
        this.gameId = gameId;
        this.invokingPlayerUserName = invokingPlayerUserName;
        this.targetPlayerUserName = targetPlayerUserName;
        this.stealedCard = stealedCard;
    }

}
