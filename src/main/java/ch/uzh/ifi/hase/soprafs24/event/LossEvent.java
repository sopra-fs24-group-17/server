package ch.uzh.ifi.hase.soprafs24.event;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

@Setter
@Getter
public class LossEvent extends ApplicationEvent {
    private Long gameId;
    private String looserPlayer;

    public LossEvent(Object source, Long gameId, String looserPlayer) {
        super(source);
        this.gameId = gameId;
        this.looserPlayer = looserPlayer;
    }
}