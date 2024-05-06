import ch.uzh.ifi.hase.soprafs24.constant.GameState;
import ch.uzh.ifi.hase.soprafs24.event.AttackEvent;
import ch.uzh.ifi.hase.soprafs24.event.GameStateEvent;
import ch.uzh.ifi.hase.soprafs24.event.LossEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import ch.uzh.ifi.hase.soprafs24.repository.*;
import ch.uzh.ifi.hase.soprafs24.service.*;
import ch.uzh.ifi.hase.soprafs24.entity.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.*;

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
        mockGame.setPlayers(new HashSet<>(Arrays.asList(user1, user2)));
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
        Set<User> players = new HashSet<>(Arrays.asList(new User(), new User()));
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
        mockGame.setPlayers(new HashSet<>(Collections.singletonList(user1)));

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
        mockGame.setPlayers(new HashSet<>(Collections.singletonList(user)));

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
        Set<User> players = new HashSet<>(Arrays.asList(user, user1));
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
}
