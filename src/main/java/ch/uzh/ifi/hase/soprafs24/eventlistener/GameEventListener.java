package ch.uzh.ifi.hase.soprafs24.eventlistener;

import ch.uzh.ifi.hase.soprafs24.event.GameJoinEvent;
import ch.uzh.ifi.hase.soprafs24.event.GameLeaveEvent;
import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GameEventListener {

    private final WebSocketService webSocketService;

    @Autowired
    public GameEventListener(WebSocketService webSocketService) {
        this.webSocketService = webSocketService;
    }

    @EventListener
    public void onGameJoinEvent(GameJoinEvent event) {
        log.info("GameJoinEvent triggered for game ID: {}", event.getGameId());
        webSocketService.sendMessageJoinedUser(event.getUserName(), event.getGameId(), event.getMaxPlayerCount(), event.getCurrentPlayerCount());
    }

    @EventListener
    public void onGameLeaveEvent(GameLeaveEvent event) {
        log.info("GameLeaveEvent triggered for game ID: {}", event.getGameId());
        webSocketService.sendMessageLeftUser(event.getUserName(), event.getGameId(), event.getMaxPlayerCount(), event.getCurrentPlayerCount());
    }

}
