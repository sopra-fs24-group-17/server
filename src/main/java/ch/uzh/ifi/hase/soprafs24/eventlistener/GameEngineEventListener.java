package ch.uzh.ifi.hase.soprafs24.eventlistener;

import ch.uzh.ifi.hase.soprafs24.event.*;
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
        webSocketService.sendMessageCardsDrawn(event.getGameId(), event.getInvokingPlayerUserName(), event.getNumberOfCards());
    }

    @EventListener
    public void onShuffling(ShufflingEvent event) {
        logger.info("ShufflingEvent triggered for game ID: {} by user {}", event.getGameId(), event.getInvokingPlayerUserName());
        webSocketService.sendMessageShuffling(event.getGameId(), event.getInvokingPlayerUserName());
    }

    @EventListener
    public void onPeekIntoDeck(PeekIntoDeckEvent event) {
        logger.info("PeekIntoDeckEvent triggered for game ID: {} by user {}", event.getGameId(), event.getInvokingPlayerUserName());
        webSocketService.sendMessagePeekIntoDeck(event.getGameId(), event.getInvokingPlayerUserName());
    }

    @EventListener
    public void returnExplosionToDeck(ExplosionReturnedToDeckEvent event) {
        logger.info("ExplosionReturnedToDeckEvent triggered for game ID: {} by user {}", event.getGameId(), event.getInvokingPlayerUserName());
        webSocketService.sendMessageExplosionReturnedToDeck(event.getGameId(), event.getInvokingPlayerUserName());
    }
}
