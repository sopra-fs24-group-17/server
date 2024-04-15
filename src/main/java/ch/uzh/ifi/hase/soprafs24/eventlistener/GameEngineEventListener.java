package ch.uzh.ifi.hase.soprafs24.eventlistener;

import ch.uzh.ifi.hase.soprafs24.event.DrawCardsEvent;
import ch.uzh.ifi.hase.soprafs24.event.GameCreationEvent;
import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class GameEngineEventListener {

    private static final Logger logger = LoggerFactory.getLogger(GameEventListener.class);

    private final WebSocketService webSocketService;

    @Autowired
    public GameEngineEventListener(WebSocketService webSocketService) {
        this.webSocketService = webSocketService;
    }

    @EventListener
    public void onDrawCards(DrawCardsEvent event) {
        logger.info("DrawCardsEvent triggered for game ID: {}, user {} drew {} card(s)", event.getGameId(), event.getInvokingPlayerUserName(), event.getNumberOfCards());
        webSocketService.sendMessageGameCreated(event.getGameId());
    }

}
