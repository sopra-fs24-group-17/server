package ch.uzh.ifi.hase.soprafs24.eventlistener;

import ch.uzh.ifi.hase.soprafs24.event.GameCreationEvent;
import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GameCreationEventListener {

    private final WebSocketService webSocketService;

    @Autowired
    public GameCreationEventListener(WebSocketService webSocketService) {this.webSocketService = webSocketService;}

    @EventListener
    public void onGameCreation(GameCreationEvent event) {
        log.info("GameCreationEvent triggered for game ID: {}", event.getGameId());
        webSocketService.sendMessageGameCreated(event.getGameId());
    }
}
