package ch.uzh.ifi.hase.soprafs24.event;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Setter
@Getter
public class EndGameEvent extends ApplicationEvent {

    private String userName;

    private Long gameId;

    private List<String> leaderboard;

    public EndGameEvent(Object source, String userName, Long gameId, List<String> leaderboard) {
        super(source);
        this.userName = userName;
        this.gameId = gameId;
        this.leaderboard = leaderboard;
    }
}