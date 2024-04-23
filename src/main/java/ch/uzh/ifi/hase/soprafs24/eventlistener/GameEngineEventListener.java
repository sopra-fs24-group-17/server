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
        webSocketService.sendMessagePeekIntoDeck(event.getGameId(), event.getInvokingPlayerUserName(), event.getUserId() ,event.getFutureCards());
    }

    @EventListener
    public void returnExplosionToDeck(ExplosionReturnedToDeckEvent event) {
        logger.info("ExplosionReturnedToDeckEvent triggered for game ID: {} by user {}", event.getGameId(), event.getInvokingPlayerUserName());
        webSocketService.sendMessageExplosionReturnedToDeck(event.getGameId(), event.getInvokingPlayerUserName());
    }

    @EventListener
    public void startGame(GameStartEvent event) {
        logger.info("Game: {}, Successfully started for user {}", event.getGameId(), event.getUserId());
        webSocketService.sendMessageGameStarted(event.getGameId(), event.getUserId());
    }

    @EventListener
    public void yourTurn(YourTurnEvent event) {
        logger.info("Player {}: Your turn", event.getUserId());
        webSocketService.sendMessageYourTurn(event.getUserId(), event.getGameId());
    }

    @EventListener
    public void endTurn(EndTurnEvent event) {
        logger.info("Player {} terminated his turn", event.getUserName());
        webSocketService.setSendMessageEndTurn(event.getGameId(), event.getUserName());
    }

    @EventListener
    public void endGame(EndGameEvent event) {
        logger.info("Player {} won the game {}",event.getUserName(), event.getGameId());
        webSocketService.sendMessageEndGame(event.getGameId(), event.getUserName());
    }

    @EventListener
    public void playerCard(PlayerCardEvent event) {
        logger.info("Player {} cards dispatched", event.getUserId());
        webSocketService.sendMessagePlayerCards(event.getGameId(), event.getUserId(), event.getPlayerCards());
    }

    @EventListener
    public void playedCard(CardPlayedEvent event) {
        logger.info("Player {} played card {}", event.getInvokingPlayerUserName(), event.getInternalCode());
        webSocketService.sendMessageCardPlayed(event.getGameId(), event.getInvokingPlayerUserName(), event.getInternalCode());
    }
}