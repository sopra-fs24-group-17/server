package ch.uzh.ifi.hase.soprafs24.event;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

@Setter
@Getter
public class FriendshipRequestAcceptanceEvent extends ApplicationEvent {
    private String friendName;
    private Long userId;

    public FriendshipRequestAcceptanceEvent(Object source, String friendName, Long userId) {
        super(source);
        this.friendName = friendName;
        this.userId = userId;
    }
}
