package ch.uzh.ifi.hase.soprafs24.eventlistener;

import static org.mockito.Mockito.*;

import ch.uzh.ifi.hase.soprafs24.event.*;
import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.slf4j.Logger;

public class GameEngineEventListenerTest {

    @Mock
    private WebSocketService webSocketService;

    @InjectMocks
    private GameEngineEventListener listener;

    @Mock
    private Logger logger;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testOnDrawCards() {
        DrawCardsEvent event = new DrawCardsEvent("gameId", "playerUsername", 3);
        listener.onDrawCards(event);
        verify(webSocketService).sendMessageCardsDrawn("gameId", "playerUsername", 3);
    }

    @Test
    public void testOnShuffling() {
        ShufflingEvent event = new ShufflingEvent("gameId", "playerUsername");
        listener.onShuffling(event);
        verify(webSocketService).sendMessageShuffling("gameId", "playerUsername");
    }

    @Test
    public void testOnPeekIntoDeck() {
        PeekIntoDeckEvent event = new PeekIntoDeckEvent("gameId", "playerUsername", "userId", new int[]{1, 2, 3});
        listener.onPeekIntoDeck(event);
        verify(webSocketService).sendMessagePeekIntoDeck("gameId", "playerUsername", "userId", new int[]{1, 2, 3});
    }

    @Test
    public void testReturnExplosionToDeck() {
        ExplosionReturnedToDeckEvent event = new ExplosionReturnedToDeckEvent("gameId", "playerUsername");
        listener.returnExplosionToDeck(event);
        verify(webSocketService).sendMessageExplosionReturnedToDeck("gameId", "playerUsername");
    }

    @Test
    public void testStartGame() {
        GameStartEvent event = new GameStartEvent("gameId", "userId");
        listener.startGame(event);
        verify(webSocketService).sendMessageGameStarted("gameId", "userId");
    }

    @Test
    public void testYourTurn() {
        YourTurnEvent event = new YourTurnEvent("userId", "gameId");
        listener.yourTurn(event);
        verify(webSocketService).sendMessageYourTurn("userId", "gameId");
    }

    @Test
    public void testEndTurn() {
        EndTurnEvent event = new EndTurnEvent("gameId", "userName");
        listener.endTurn(event);
        verify(webSocketService).setSendMessageEndTurn("gameId", "userName");
    }

    @Test
    public void testEndGame() {
        EndGameEvent event = new EndGameEvent("gameId", "userName");
        listener.endGame(event);
        verify(webSocketService).sendMessageEndGame("gameId", "userName");
    }

    @Test
    public void testPlayerCard() {
        PlayerCardEvent event = new PlayerCardEvent("gameId", "userId", new String[]{"card1", "card2"});
        listener.playerCard(event);
        verify(webSocketService).sendMessagePlayerCards("gameId", "userId", new String[]{"card1", "card2"});
    }

    @Test
    public void testPlayedCard() {
        CardPlayedEvent event = new CardPlayedEvent("gameId", "playerUsername", "internalCode");
        listener.playedCard(event);
        verify(webSocketService).sendMessageCardPlayed("gameId", "playerUsername", "internalCode");
    }

    // Add tests for other event methods similarly...

    @Test
    public void testStolenCard() {
        StealCardEvent event = new StealCardEvent("gameId", "userId", new String[]{"card1"});
        listener.stolenCard(event);
        verify(webSocketService).sendMessageStolenCard("gameId", "userId", new String[]{"card1"});
    }

    @Test
    public void testDefuseActivated() {
        DefuseEvent event = new DefuseEvent("gameId", "userId", new String[]{"card1"});
        listener.defuseActivated(event);
        verify(webSocketService).sendMessageDefuseCardPlayed("gameId", "userId", new String[]{"card1"});
    }

    @Test
    public void testExplosionTriggered() {
        ExplosionEvent event = new Explosion
