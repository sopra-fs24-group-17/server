package ch.uzh.ifi.hase.soprafs24.eventlistener;

import static org.mockito.Mockito.*;

import ch.uzh.ifi.hase.soprafs24.entity.Card;
import ch.uzh.ifi.hase.soprafs24.event.*;
import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ActiveProfiles("dev")
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
        Long gameId = 12345L;
        String userName = "user";
        DrawCardsEvent event = new DrawCardsEvent(this, 3, gameId, userName);
        listener.onDrawCards(event);
        verify(webSocketService).sendMessageCardsDrawn(gameId, userName, 3);
    }

    @Test
    public void testOnShuffling() {
        Long gameId = 12345L;
        String userName = "user";
        ShufflingEvent event = new ShufflingEvent(this, gameId, userName);
        listener.onShuffling(event);
        verify(webSocketService).sendMessageShuffling(gameId, userName);
    }

    @Test
    public void testOnPeekIntoDeck() {
        Long gameId = 12345L;
        String userName = "user";
        List<Card> futureCards = new ArrayList<>();
        Card card1 = new Card();
        card1.setCode("AB");
        card1.setSuit("suit");
        card1.setImage("image");
        card1.setInternalCode("internalCode");
        futureCards.add(card1);

        Card card2 = new Card();
        card2.setCode("CD");
        card2.setSuit("suit");
        card2.setImage("image");
        card2.setInternalCode("internalCode");
        futureCards.add(card2);

        Card card3 = new Card();
        card3.setCode("EF");
        card3.setSuit("suit");
        card3.setImage("image");
        card3.setInternalCode("internalCode");
        futureCards.add(card3);

        PeekIntoDeckEvent event = new PeekIntoDeckEvent(this, gameId, userName, 1L, futureCards);
        listener.onPeekIntoDeck(event);
        verify(webSocketService).sendMessagePeekIntoDeck(gameId, userName, 1L, futureCards);
    }

    @Test
    public void testReturnExplosionToDeck() {
        Long gameId = 12345L;
        String userName = "user";
        ExplosionReturnedToDeckEvent event = new ExplosionReturnedToDeckEvent(this, gameId, userName);
        listener.returnExplosionToDeck(event);
        verify(webSocketService).sendMessageExplosionReturnedToDeck(gameId, userName);
    }

    @Test
    public void testStartGame() {
        Long gameId = 12345L;
        String userName = "user";
        GameStartEvent event = new GameStartEvent(this, gameId, 1L);
        listener.startGame(event);
        verify(webSocketService).sendMessageGameStarted(gameId, 1L);
    }

    @Test
    public void testYourTurn() {
        Long gameId = 12345L;
        String userName = "user";
        YourTurnEvent event = new YourTurnEvent(this, 1L, gameId, userName);
        listener.yourTurn(event);
        verify(webSocketService).sendMessageYourTurn(1L, gameId, userName);
    }

    @Test
    public void testEndTurn() {
        Long gameId = 12345L;
        String userName = "user";
        Long userId = 1L;
        EndTurnEvent event = new EndTurnEvent(this, userName, gameId, userId);
        listener.endTurn(event);
        verify(webSocketService).setSendMessageEndTurn(userId, gameId, userName);
    }

    @Test
    public void testEndGame() {
        Long gameId = 12345L;
        String userName = "user";
        List<String> leaderboard = new ArrayList<>();
        leaderboard.add((userName));
        EndGameEvent event = new EndGameEvent(this, userName, gameId, leaderboard);
        listener.endGame(event);
        verify(webSocketService).sendMessageEndGame(gameId, userName, leaderboard);
    }

    @Test
    public void testPlayerCard() {
        Long gameId = 12345L;
        String userName = "user";

        List<Card> playedCards = new ArrayList<>();
        Card card = new Card();
        card.setCode("AB");
        card.setSuit("suit");
        card.setImage("image");
        card.setInternalCode("internalCode");
        playedCards.add(card);

        PlayerCardEvent event = new PlayerCardEvent(this, 1L, gameId, playedCards);
        listener.playerCard(event);
        verify(webSocketService).sendMessagePlayerCards(gameId, 1L, playedCards);
    }

    @Test
    public void testPlayedCard() {
        Long gameId = 12345L;
        String userName = "user";
        CardPlayedEvent event = new CardPlayedEvent(this, "explosion", gameId, userName, "AB");
        listener.playedCard(event);
        verify(webSocketService).sendMessageCardPlayed(gameId, userName, "explosion", "AB");
    }

    @Test
    public void testStolenCard() {
        Long gameId = 12345L;
        String userName = "user";
        List<Card> stolenCard = new ArrayList<>();
        Card card = new Card();
        card.setCode("AB");
        card.setSuit("suit");
        card.setImage("image");
        card.setInternalCode("internalCode");
        stolenCard.add(card);

        StealCardEvent event = new StealCardEvent(this, 1L, gameId, stolenCard);
        listener.stolenCard(event);
        verify(webSocketService).sendMessageStolenCard(gameId, 1L, stolenCard);
    }

    @Test
    public void testDefuseActivated() {
        Long gameId = 12345L;
        String userName = "user";

        List<Card> playedCard = new ArrayList<>();
        Card card = new Card();
        card.setCode("AB");
        card.setSuit("suit");
        card.setImage("image");
        card.setInternalCode("internalCode");
        playedCard.add(card);

        DefuseEvent event = new DefuseEvent(this, 1L, gameId, playedCard);
        listener.defuseActivated(event);
        verify(webSocketService).sendMessageDefuseCardPlayed(gameId, 1L, playedCard);
    }

    @Test
    public void testExplosionTriggered() {
        Long gameId = 12345L;
        String userName = "user";
        ExplosionEvent event = new ExplosionEvent(this, gameId, userName);
        listener.explosionTriggered(event);
        verify(webSocketService).sendMessageExplosion(gameId, userName);
    }

    @Test
    public void testDownOneUser() {
        Long gameId = 12345L;
        String userName = "user";
        LossEvent event = new LossEvent(this, gameId, userName);
        listener.downOneUser(event);
        verify(webSocketService).lossEvent(gameId, userName);
    }

    @Test
    public void testProvideGameStats() {
        Long gameId = 12345L;
        Card topCard = new Card();
        topCard.setCode("KH");
        topCard.setInternalCode("explosion");
        topCard.setSuit("suit");
        topCard.setImage("image");
        Map<String, Integer> remainingCardStats = new HashMap<>();
        remainingCardStats.put("deck", 40);
        remainingCardStats.put("discard", 5);
        Integer numberOfPlayers = 5;

        List<String> usernames = new ArrayList<>();
        usernames.add("user1");
        usernames.add("user2");

        List<Long> playerIds = new ArrayList<>();
        playerIds.add(1L);
        playerIds.add(2L);

        List<String> playerAvatars = new ArrayList<>();
        playerAvatars.add("avatar1");
        playerAvatars.add("avatar2");

        GameStateEvent event = new GameStateEvent(this, gameId, topCard, remainingCardStats, numberOfPlayers, usernames, playerIds, playerAvatars, "user2");
        listener.provideGameStats(event);
        verify(webSocketService).sendGameState(eq(gameId), eq(topCard), eq(remainingCardStats), eq(numberOfPlayers), eq(usernames), eq(playerIds), eq(playerAvatars), eq("user2"));
    }

    @Test
    public void testExplosionIndividual() {
        Long gameId = 12345L;
        String userName = "user";
        ExplosionEventIndividual event = new ExplosionEventIndividual(this, gameId, 1L);
        listener.explosionIndividual(event);
        verify(webSocketService).sendMessageExplosionIndividual(gameId, 1L);
    }

    @Test
    public void testSkipCardPlayed() {
        Long gameId = 12345L;
        String userName = "user";
        SkipEvent event = new SkipEvent(this, gameId, userName);
        listener.skipCardPlayed(event);
        verifyNoInteractions(webSocketService);
    }

    @Test
    public void testAttackCardPlayed() {
        Long gameId = 12345L;
        String userName = "user";
        String targetName = "userTarget";
        AttackEvent event = new AttackEvent(this, gameId, userName, targetName);
        listener.attackCardPlayed(event);
        verifyNoInteractions(webSocketService);
    }

    @Test
    public void testPlacementRequest() {
        Long gameId = 12345L;
        Long userId = 1L;

        PlacementEvent placementEvent = new PlacementEvent(this, gameId, userId);
        listener.placementRequest(placementEvent);
        verify(webSocketService).sendPlacementRequest(gameId, userId);
    }

    @Test
    public void testGetLucky() {
        Long gameId = 12345L;
        Long userId = 1L;

        Card randomCard = new Card();
        randomCard.setInternalCode("lucky");
        randomCard.setCode("X1");

        LuckyEvent luckyEvent = new LuckyEvent(this, userId, gameId, randomCard);
        listener.getLucky(luckyEvent);
        verify(webSocketService).sendMessageGetLucky(gameId, userId, randomCard);
    }

}
