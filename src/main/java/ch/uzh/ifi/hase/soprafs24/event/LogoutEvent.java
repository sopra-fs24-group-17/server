package ch.uzh.ifi.hase.soprafs24.event;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

@Setter
@Getter
public class LogoutEvent extends ApplicationEvent {
    private String userName;
    private Long userId;

    public LogoutEvent(Object source, String userName, Long userId) {
        super(source);
        this.userName = userName;
        this.userId = userId;
    }
}
