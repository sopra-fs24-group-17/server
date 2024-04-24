package ch.uzh.ifi.hase.soprafs24.event;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

@Setter
@Getter
public class AttackEvent extends ApplicationEvent {
    private Long gameId;
    private String invokingPlayerUserName;
    private String targetUsername;

    public AttackEvent(Object source, Long gameId, String invokingPlayerUserName, String targetUsername) {
        super(source);
        this.gameId = gameId;
        this.invokingPlayerUserName = invokingPlayerUserName;
        this.targetUsername = targetUsername;
    }

}
