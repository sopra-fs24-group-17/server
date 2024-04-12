package ch.uzh.ifi.hase.soprafs24.eventlistener;

import ch.uzh.ifi.hase.soprafs24.event.GameCreationEvent;
import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class GameCreationEventListener {

    private static final Logger logger = LoggerFactory.getLogger(GameCreationEventListener.class);

    private final WebSocketService webSocketService;

    @Autowired
    public GameCreationEventListener(WebSocketService webSocketService) {this.webSocketService = webSocketService;}

    @EventListener
    public void onGameCreation(GameCreationEvent event) {
        logger.info("GameCreationEvent triggered for game ID: {}", event.getGameId());
        webSocketService.sendMessageToClients("/game/new", "New game created with ID: " + event.getGameId());
    }
}
