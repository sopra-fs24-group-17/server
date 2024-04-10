package ch.uzh.ifi.hase.soprafs24.eventlistener;

import ch.uzh.ifi.hase.soprafs24.event.GameJoinEvent;
import ch.uzh.ifi.hase.soprafs24.event.GameLeaveEvent;
import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class GameEventListener {

    private final WebSocketService webSocketService;

    @Autowired
    public GameEventListener(WebSocketService webSocketService) {
        this.webSocketService = webSocketService;
    }

    @EventListener
    public void onGameJoinEvent(GameJoinEvent event) {
        webSocketService.sendMessageJoinedUser(event.getUserName(), event.getGameId());
    }

    @EventListener
    public void onGameLeaveEvent(GameLeaveEvent event) {
        //To Do -- To be implemented
    }

}
