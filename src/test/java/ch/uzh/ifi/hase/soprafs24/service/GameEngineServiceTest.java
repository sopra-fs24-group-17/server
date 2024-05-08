package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.GameState;
import ch.uzh.ifi.hase.soprafs24.entity.*;
import ch.uzh.ifi.hase.soprafs24.event.AttackEvent;
import ch.uzh.ifi.hase.soprafs24.event.GameStateEvent;
import ch.uzh.ifi.hase.soprafs24.event.PlayerCardEvent;
import ch.uzh.ifi.hase.soprafs24.event.SkipEvent;
import ch.uzh.ifi.hase.soprafs24.repository.CardRepository;
import ch.uzh.ifi.hase.soprafs24.repository.GameDeckRepository;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.postgresql.hostchooser.HostRequirement.any;

@ActiveProfiles("dev")
@ExtendWith(MockitoExtension.class)
public class GameEngineServiceTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GameDeckRepository gameDeckRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private GameDeckService gameDeckService;

    @Mock
    private UserService userService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private GameEngineService gameEngineService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testFindGameById_GameNotFound() {
        when(gameRepository.findByGameId(1L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> {
            gameEngineService.findGameById(1L);
        });
    }

    @Test
    public void testStartGame_Success() throws Exception {
        UserStats userStats = new UserStats();
        userStats.setGamesPlayed(0);

        User user1 = new User();
        user1.setId(1L);
        user1.setUserStats(userStats);

        User user2 = new User();
        user2.setId(2L);
        user2.setUserStats(userStats);

        Game mockGame = new Game();
        mockGame.setState(GameState.PREPARING);
        mockGame.setPlayers(Arrays.asList(user1, user2));
        when(gameRepository.findByGameId(1L)).thenReturn(Optional.of(mockGame));

        Game startedGame = gameEngineService.startGame(1L);

        assertEquals(GameState.ONGOING, startedGame.getState());
        verify(gameRepository, times(1)).saveAndFlush(mockGame);

        assertEquals(2, user1.getUserStats().getGamesPlayed());
        assertEquals(2, user2.getUserStats().getGamesPlayed());
    }

    @Test
    public void testTurnValidation_NotUsersTurn() {
        Game mockGame = new Game();
        User user = new User();
        user.setId(2L);
        mockGame.setCurrentTurn(user);

        when(gameRepository.findByGameId(1L)).thenReturn(Optional.of(mockGame));
        when(userService.getUserById(3L)).thenReturn(new User());

        assertThrows(ResponseStatusException.class, () -> {
            gameEngineService.turnValidation(1L, 3L);
        });
    }

    @Test
    public void testTerminatingGame_InvalidTermination() {
        Game mockGame = new Game();
        List<User> players = new ArrayList<>(Arrays.asList(new User(), new User()));
        mockGame.setPlayers(players);

        when(gameRepository.findByGameId(1L)).thenReturn(Optional.of(mockGame));

        assertThrows(ResponseStatusException.class, () -> {
            gameEngineService.terminatingGame(1L);
        });
    }

    @Test
    public void testRemoveUserFromGame_Failure_UserNotInGame() {
        Game mockGame = new Game();
        User fakeUser = new User();
        fakeUser.setId(99L);

        when(gameRepository.findByGameId(1L)).thenReturn(Optional.of(mockGame));
        when(userService.getUserById(99L)).thenReturn(fakeUser);

        assertThrows(ResponseStatusException.class, () -> gameEngineService.removeUserFromGame(1L, 99L));
    }

    @Test
    public void testTurnValidation_Failure_NotUsersTurn() {
        Game mockGame = new Game();
        User currentUser = new User();
        currentUser.setId(1L);
        User anotherUser = new User();
        anotherUser.setId(2L);
        mockGame.setCurrentTurn(anotherUser);

        when(gameRepository.findByGameId(1L)).thenReturn(Optional.of(mockGame));

        assertThrows(ResponseStatusException.class, () -> gameEngineService.turnValidation(1L, 1L));
    }

    @Test
    public void testStartGame_Failure_NotInPreparingState() {
        User user1 = new User();
        user1.setId(1L);

        Game mockGame = new Game();
        mockGame.setState(GameState.ONGOING);
        mockGame.setPlayers(new ArrayList<>(Collections.singletonList(user1)));

        when(gameRepository.findByGameId(1L)).thenReturn(Optional.of(mockGame));

        assertThrows(ResponseStatusException.class, () -> gameEngineService.startGame(1L));
    }

    @Test
    public void testTerminatingGame_Success() {
        UserStats userStats = new UserStats();
        userStats.setGamesPlayed(0);
        userStats.setGamesWon(0);

        User user = new User();
        user.setId(1L);
        user.setUserStats(userStats);

        Game mockGame = new Game();
        mockGame.setPlayers(new ArrayList<>(Collections.singletonList(user)));

        when(gameRepository.findByGameId(1L)).thenReturn(Optional.of(mockGame));

        gameEngineService.terminatingGame(1L);

        assertEquals(GameState.FINISHED, mockGame.getState());
        verify(gameRepository, times(1)).saveAndFlush(mockGame);
    }

    @Test
    public void testRemoveUserFromGame_Success_UsersTurn() throws Exception {
        UserStats userStats = new UserStats();
        userStats.setGamesPlayed(0);
        userStats.setGamesWon(0);

        User user = new User();
        user.setId(1L);
        user.setUserStats(userStats);

        User user1 = new User();
        user1.setId(2L);
        UserStats user1Stats = new UserStats();
        user1Stats.setGamesPlayed(0);
        user1Stats.setGamesWon(0);
        user1.setUserStats(user1Stats);

        Game mockGame = new Game();
        mockGame.setState(GameState.ONGOING);
        mockGame.setCurrentTurn(user);
        List<User> players = new ArrayList<>(Arrays.asList(user, user1));
        mockGame.setPlayers(players);

        when(gameRepository.findByGameId(1L)).thenReturn(Optional.of(mockGame));
        when(userService.getUserById(1L)).thenReturn(user);

        gameEngineService.removeUserFromGame(1L, 1L);

        assertFalse(mockGame.getPlayers().isEmpty());
        assertEquals(mockGame.getState(), GameState.FINISHED);
        verify(gameRepository, times(3)).saveAndFlush(mockGame);
    }

    @Test
    public void testDrawCardMoveTermination_NoSkip() throws Exception {
        Game mockGame = new Game();
        User user = new User();
        user.setId(1L);

        Card drawnCard = new Card();
        drawnCard.setCode("example_card");
        drawnCard.setInternalCode("explosion");

        when(gameRepository.findByGameId(1L)).thenReturn(Optional.of(mockGame));
        when(gameDeckService.drawCardsFromDealerPile(any(), eq(1))).thenReturn(Collections.singletonList(drawnCard));

        String result = gameEngineService.drawCardMoveTermination(1L, 1L);

        assertEquals("example_card", result);
    }

    @Test
    public void testTransformCardsToInternalRepresentation() {
        List<String> cardIds = Arrays.asList("card1", "card2");
        Card card1 = new Card();
        card1.setCode("card1");
        Card card2 = new Card();
        card2.setCode("card2");

        when(cardRepository.findByCode("card1")).thenReturn(card1);
        when(cardRepository.findByCode("card2")).thenReturn(card2);

        List<Card> result = gameEngineService.transformCardsToInternalRepresentation(cardIds);

        assertEquals(2, result.size());
        assertTrue(result.contains(card1));
        assertTrue(result.contains(card2));
        verify(cardRepository, times(1)).findByCode("card1");
        verify(cardRepository, times(1)).findByCode("card2");
    }

    @Test
    public void testHandleShuffleCard() throws IOException, InterruptedException {
        Game game = mock(Game.class);
        GameDeck deck = mock(GameDeck.class);
        when(game.getGameDeck()).thenReturn(deck);

        gameEngineService.handleShuffleCard(game, 1L);

        verify(gameDeckService, times(1)).shuffleCardsInDealerPile(deck);
    }

    @Test
    public void testDispatchGameState() throws IOException, InterruptedException {
        // Setup
        Long gameId = 1L;
        Long userId = 1L;

        Game mockGame = new Game();
        GameDeck mockDeck = new GameDeck();
        mockGame.setGameDeck(mockDeck);

        Card topCard = new Card();
        topCard.setCode("AH");
        topCard.setInternalCode("Ace of Hearts");

        List<Card> topCards = Arrays.asList(topCard);

        String jsonResponse = "{\"dealer\": {\"remaining\": 52}}";
        Map<String, Integer> pileCardCounts = Map.of("dealer", 52);

        when(gameRepository.findByGameId(gameId)).thenReturn(Optional.of(mockGame));
        when(gameDeckService.getRemainingPileStats(mockDeck, userId)).thenReturn(jsonResponse);
        when(gameDeckService.parsePileCardCounts(jsonResponse)).thenReturn(pileCardCounts);
        when(gameDeckService.exploreTopCardPlayPile(mockDeck)).thenReturn(topCards);

        gameEngineService.dispatchGameState(gameId, userId);

        verify(gameRepository).findByGameId(gameId);
        verify(gameDeckService).getRemainingPileStats(mockDeck, userId);
        verify(gameDeckService).parsePileCardCounts(jsonResponse);
        verify(gameDeckService).exploreTopCardPlayPile(mockDeck);

        ArgumentCaptor<GameStateEvent> eventCaptor = ArgumentCaptor.forClass(GameStateEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        GameStateEvent capturedEvent = eventCaptor.getValue();

        assertNotNull(capturedEvent);
        assertEquals(gameId, capturedEvent.getGameId());
    }

    @Test
    public void testHandleFutureCard() throws IOException, InterruptedException {
        Game game = mock(Game.class);
        Long userId = 1L;

        gameEngineService.handleFutureCard(game, userId);
        verify(gameDeckService, times(1)).peekIntoDealerPile(game);
    }

    @Test
    public void testHandleAttackCard_Success() throws IOException, InterruptedException {
        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUsername("currentUser");

        User nextUser = new User();
        nextUser.setId(2L);
        nextUser.setUsername("nextUser");

        List<User> players = Arrays.asList(currentUser, nextUser);
        Game mockGame = new Game();
        mockGame.setGameId(1L);
        mockGame.setPlayers(players);
        mockGame.setCurrentTurn(currentUser);
        mockGame.setAttacked(false);

        when(userRepository.findUserById(1L)).thenReturn(currentUser);
        when(userService.getUserById(1L)).thenReturn(currentUser);
        when(gameRepository.findByGameId(1L)).thenReturn(Optional.of(mockGame));
        when(gameRepository.saveAndFlush(Mockito.any())).thenReturn(mockGame);

        gameEngineService.handleAttackCard(mockGame, 1L);

        assertTrue(mockGame.isRepeatTurn());
    }

    @Test
    public void testHandleSkipCard_PublishesSkipEventAndCallsTurnValidation() throws IOException, InterruptedException {
        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUsername("currentUser");

        User nextUser = new User();
        nextUser.setId(2L);
        nextUser.setUsername("nextUser");

        List<User> players = Arrays.asList(currentUser, nextUser);
        Game mockGame = new Game();
        mockGame.setGameId(1L);
        mockGame.setPlayers(players);
        mockGame.setCurrentTurn(currentUser);

        when(userService.getUserById(1L)).thenReturn(currentUser);
        when(gameRepository.findByGameId(1L)).thenReturn(Optional.of(mockGame));
        when(gameRepository.saveAndFlush(Mockito.any())).thenReturn(mockGame);

        gameEngineService.handleSkipCard(mockGame, 1L);

        verify(eventPublisher, times(1)).publishEvent(any(SkipEvent.class));
    }

    @Test
    public void testDrawCardMoveTermination_PublishesPlayerCardEvent() throws IOException, InterruptedException {
        Game mockGame = new Game();
        mockGame.setGameId(1L);
        GameDeck mockDeck = new GameDeck();
        mockGame.setGameDeck(mockDeck);

        User user = new User();
        user.setId(1L);

        Card drawnCard = new Card();
        drawnCard.setCode("example_card");
        drawnCard.setInternalCode("normal_card");

        when(gameRepository.findByGameId(1L)).thenReturn(Optional.of(mockGame));
        when(gameDeckService.drawCardsFromDealerPile(mockDeck, 1)).thenReturn(Collections.singletonList(drawnCard));

        ArgumentCaptor<PlayerCardEvent> eventCaptor = ArgumentCaptor.forClass(PlayerCardEvent.class);
        doNothing().when(eventPublisher).publishEvent(eventCaptor.capture());

        String result = gameEngineService.drawCardMoveTermination(1L, 1L);

        verify(eventPublisher, times(1)).publishEvent(any(PlayerCardEvent.class));
        PlayerCardEvent capturedEvent = eventCaptor.getValue();
        assertEquals(1L, capturedEvent.getGameId(), "Event should have the correct game ID.");
        assertEquals(1L, capturedEvent.getUserId(), "Event should have the correct user ID.");
        assertEquals(Collections.singletonList(drawnCard), capturedEvent.getPlayerCards(), "Event should contain the correct drawn cards.");

        assertNull(result, "No explosion should have been detected.");
    }

    @Test
    public void testDispatchGameState_PublishesGameStateEvent() throws IOException, InterruptedException {
        // Setup mock game and deck
        Long gameId = 1L;
        Long userId = 1L;

        Game mockGame = new Game();
        GameDeck mockDeck = new GameDeck();
        mockGame.setGameId(gameId);
        mockGame.setGameDeck(mockDeck);

        Card topCard = new Card();
        topCard.setCode("AH");
        topCard.setInternalCode("Ace of Hearts");
        List<Card> topCards = Arrays.asList(topCard);

        String jsonResponse = "{\"dealer\": {\"remaining\": 52}}";
        Map<String, Integer> pileCardCounts = Map.of("dealer", 52);

        User player1 = new User();
        player1.setId(1L);
        player1.setUsername("player1");
        User player2 = new User();
        player2.setId(2L);
        player2.setUsername("player2");
        List<User> players = Arrays.asList(player1, player2);
        mockGame.setPlayers(players);

        when(gameRepository.findByGameId(gameId)).thenReturn(Optional.of(mockGame));
        when(gameDeckService.getRemainingPileStats(mockDeck, userId)).thenReturn(jsonResponse);
        when(gameDeckService.parsePileCardCounts(jsonResponse)).thenReturn(pileCardCounts);
        when(gameDeckService.exploreTopCardPlayPile(mockDeck)).thenReturn(topCards);

        ArgumentCaptor<GameStateEvent> eventCaptor = ArgumentCaptor.forClass(GameStateEvent.class);
        doNothing().when(eventPublisher).publishEvent(eventCaptor.capture());

        gameEngineService.dispatchGameState(gameId, userId);

        verify(eventPublisher, times(1)).publishEvent(any(GameStateEvent.class));
        GameStateEvent capturedEvent = eventCaptor.getValue();
        assertEquals(gameId, capturedEvent.getGameId(), "Event should have the correct game ID.");
        assertEquals(topCard, capturedEvent.getTopMostCardPlayPile(), "Event should contain the correct top card from the play pile.");
    }

    @Test
    public void testDispatchGameState_TopCardIsNull() throws IOException, InterruptedException {
        Long gameId = 1L;
        Long userId = 1L;

        Game mockGame = new Game();
        GameDeck mockDeck = new GameDeck();
        mockGame.setGameId(gameId);
        mockGame.setGameDeck(mockDeck);

        String jsonResponse = "{\"dealer\": {\"remaining\": 52}}";
        Map<String, Integer> pileCardCounts = Map.of("dealer", 52);

        User player1 = new User();
        player1.setId(1L);
        player1.setUsername("player1");
        User player2 = new User();
        player2.setId(2L);
        player2.setUsername("player2");
        List<User> players = Arrays.asList(player1, player2);
        mockGame.setPlayers(players);

        when(gameRepository.findByGameId(gameId)).thenReturn(Optional.of(mockGame));
        when(gameDeckService.getRemainingPileStats(mockDeck, userId)).thenReturn(jsonResponse);
        when(gameDeckService.parsePileCardCounts(jsonResponse)).thenReturn(pileCardCounts);
        when(gameDeckService.exploreTopCardPlayPile(mockDeck)).thenReturn(null);  // Mock to return `null`

        ArgumentCaptor<GameStateEvent> eventCaptor = ArgumentCaptor.forClass(GameStateEvent.class);
        doNothing().when(eventPublisher).publishEvent(eventCaptor.capture());

        gameEngineService.dispatchGameState(gameId, userId);

        verify(eventPublisher, times(1)).publishEvent(any(GameStateEvent.class));
        GameStateEvent capturedEvent = eventCaptor.getValue();
        assertEquals(gameId, capturedEvent.getGameId(), "Event should have the correct game ID.");
        assertEquals("", capturedEvent.getTopMostCardPlayPile().getCode(), "Top card should have an empty code when no top card is found.");
        assertEquals("", capturedEvent.getTopMostCardPlayPile().getInternalCode(), "Top card should have an empty internal code when no top card is found.");
    }

    @Test
    public void testTurnValidation_RepeatTurn() throws IOException, InterruptedException {
        Long gameId = 1L;
        Long userId = 1L;

        User terminatingUser = new User();
        terminatingUser.setId(userId);
        terminatingUser.setUsername("terminatingUser");

        Game mockGame = new Game();
        mockGame.setGameId(gameId);
        mockGame.setCurrentTurn(terminatingUser);
        mockGame.setRepeatTurn(true);

        when(gameRepository.findByGameId(gameId)).thenReturn(Optional.of(mockGame));
        when(userService.getUserById(userId)).thenReturn(terminatingUser);

        GameEngineService spyGameEngineService = Mockito.spy(gameEngineService);
        doReturn("example_card").when(spyGameEngineService).drawCardMoveTermination(gameId, userId);

        spyGameEngineService.turnValidation(gameId, userId);

        verify(spyGameEngineService, times(1)).drawCardMoveTermination(gameId, userId);

        assertFalse(mockGame.isRepeatTurn(), "Repeat turn should be reset to false after turnValidation.");
        assertEquals(terminatingUser, mockGame.getCurrentTurn(), "The current turn should still belong to the terminating user during a repeat turn.");
    }

    @Test
    public void testRemoveUserFromGame_UserNotPartOfGame() throws IOException, InterruptedException {
        Long gameId = 1L;
        Long userIdNotInGame = 99L;
        Long existingUserId = 1L;

        User terminatingUser = new User();
        terminatingUser.setId(userIdNotInGame);
        terminatingUser.setUsername("notInGameUser");

        User existingUser = new User();
        existingUser.setId(existingUserId);
        existingUser.setUsername("existingUser");

        Game mockGame = new Game();
        mockGame.setGameId(gameId);
        mockGame.setState(GameState.ONGOING);
        List<User> players = Collections.singletonList(existingUser);
        mockGame.setPlayers(players);

        when(userService.getUserById(userIdNotInGame)).thenReturn(terminatingUser);
        when(gameRepository.findByGameId(gameId)).thenReturn(Optional.of(mockGame));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            gameEngineService.removeUserFromGame(gameId, userIdNotInGame);
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("User is not part of the game", exception.getReason());
    }

    @Test
    public void testTurnValidation_InitiatesGameTermination() throws IOException, InterruptedException {
        Long gameId = 1L;
        Long userId = 1L;

        User terminatingUser = new User();
        terminatingUser.setId(userId);
        terminatingUser.setUsername("terminatingUser");

        Game mockGame = new Game();
        mockGame.setGameId(gameId);
        mockGame.setCurrentTurn(terminatingUser);
        mockGame.setPlayers(Collections.singletonList(terminatingUser));

        when(gameRepository.findByGameId(gameId)).thenReturn(Optional.of(mockGame));
        when(userService.getUserById(userId)).thenReturn(terminatingUser);

        GameEngineService spyGameEngineService = Mockito.spy(gameEngineService);
        doNothing().when(spyGameEngineService).terminatingGame(gameId);

        spyGameEngineService.turnValidation(gameId, userId);

        verify(spyGameEngineService, times(1)).terminatingGame(gameId);
    }

}