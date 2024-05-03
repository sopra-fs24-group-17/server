package ch.uzh.ifi.hase.soprafs24.eventlistener;

import ch.uzh.ifi.hase.soprafs24.event.*;
import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GameEngineEventListener {

    private final WebSocketService webSocketService;

    @Autowired
    public GameEngineEventListener(WebSocketService webSocketService) {
        this.webSocketService = webSocketService;
    }

    @EventListener
    public void onDrawCards(DrawCardsEvent event) {
        log.info("DrawCardsEvent triggered for game ID: {}, user {} drew {} card(s)", event.getGameId(), event.getInvokingPlayerUserName(), event.getNumberOfCards());
        webSocketService.sendMessageCardsDrawn(event.getGameId(), event.getInvokingPlayerUserName(), event.getNumberOfCards());
    }

    @EventListener
    public void onShuffling(ShufflingEvent event) {
        log.info("ShufflingEvent triggered for game ID: {} by user {}", event.getGameId(), event.getInvokingPlayerUserName());
        webSocketService.sendMessageShuffling(event.getGameId(), event.getInvokingPlayerUserName());
    }

    @EventListener
    public void onPeekIntoDeck(PeekIntoDeckEvent event) {
        log.info("PeekIntoDeckEvent triggered for game ID: {} by user {}", event.getGameId(), event.getInvokingPlayerUserName());
        webSocketService.sendMessagePeekIntoDeck(event.getGameId(), event.getInvokingPlayerUserName(), event.getUserId() ,event.getFutureCards());
    }

    @EventListener
    public void returnExplosionToDeck(ExplosionReturnedToDeckEvent event) {
        log.info("ExplosionReturnedToDeckEvent triggered for game ID: {} by user {}", event.getGameId(), event.getInvokingPlayerUserName());
        webSocketService.sendMessageExplosionReturnedToDeck(event.getGameId(), event.getInvokingPlayerUserName());
    }

    @EventListener
    public void startGame(GameStartEvent event) {
        log.info("Game: {}, Successfully started for user {}", event.getGameId(), event.getUserId());
        webSocketService.sendMessageGameStarted(event.getGameId(), event.getUserId());
    }

    @EventListener
    public void yourTurn(YourTurnEvent event) {
        log.info("Player {}: Your turn", event.getUserId());
        webSocketService.sendMessageYourTurn(event.getUserId(), event.getGameId(), event.getUserName());
    }

    @EventListener
    public void endTurn(EndTurnEvent event) {
        log.info("Player {} terminated his turn", event.getUserName());
        webSocketService.setSendMessageEndTurn(event.getUserId(), event.getGameId(), event.getUserName());
    }

    @EventListener
    public void endGame(EndGameEvent event) {
        log.info("Player {} won the game {}",event.getUserName(), event.getGameId());
        webSocketService.sendMessageEndGame(event.getGameId(), event.getUserName());
    }

    @EventListener
    public void playerCard(PlayerCardEvent event) {
        log.info("Player {} cards dispatched", event.getUserId());
        webSocketService.sendMessagePlayerCards(event.getGameId(), event.getUserId(), event.getPlayerCards());
    }

    @EventListener
    public void playedCard(CardPlayedEvent event) {
        log.info("Player {} played card {}", event.getInvokingPlayerUserName(), event.getInternalCode());
        webSocketService.sendMessageCardPlayed(event.getGameId(), event.getInvokingPlayerUserName(), event.getInternalCode());
    }

    @EventListener
    public void skipCardPlayed(SkipEvent event) {
        log.info("Player {} played a skip card in game {}", event.getInvokingPlayerUserName(), event.getGameId());
        // No message triggered
    }

    @EventListener
    public void attackCardPlayed(AttackEvent event) {
        log.info("Player {} played an attack card in game {} to attack player {}", event.getInvokingPlayerUserName(), event.getGameId(), event.getTargetUsername());
        // No message triggered
    }

    @EventListener
    public void stolenCard(StealCardEvent event) {
        log.info("A card was stolen from user {} in game {}", event.getUserId(), event.getGameId());
        webSocketService.sendMessageStolenCard(event.getGameId(), event.getUserId(), event.getStolenCards());
    }

    @EventListener
    public void defuseActivated(DefuseEvent event) {
        log.info("User {} played a defuse card in game {}", event.getUserId(), event.getGameId());
        webSocketService.sendMessageDefuseCardPlayed(event.getGameId(), event.getUserId(), event.getPlayerCards());
    }

    @EventListener
    public void explosionTriggered(ExplosionEvent event) {
        log.info("User {} exploded, can he react with a defuse card?", event.getInvokingPlayerUserName());
        webSocketService.sendMessageExplosion(event.getGameId(), event.getInvokingPlayerUserName());
    }

    @EventListener
    public void downOneUser(LossEvent event) {
        log.info("User {} lost in game {}", event.getLooserPlayer(), event.getGameId());
        webSocketService.lossEvent(event.getGameId(), event.getLooserPlayer());
    }

    @EventListener
    public void provideGameStats(GameStateEvent event) {
        log.info("Game Stats for {} dispatched", event.getGameId());
        webSocketService.sendGameState(event.getGameId(), event.getTopMostCardPlayPile(), event.getRemainingCardStats(), event.getNumberOfPlayers());
    }

    @EventListener
    public void explosionIndividual(ExplosionEventIndividual event) {
        webSocketService.sendMessageExplosionIndividual(event.getGameId(), event.getUserId());
    }
}