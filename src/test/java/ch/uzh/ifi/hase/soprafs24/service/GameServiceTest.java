package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.GameMode;
import ch.uzh.ifi.hase.soprafs24.constant.GameState;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.GameDeck;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePostDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.*;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class GameServiceTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private GameService gameService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private GameDeckService gameDeckService;

    private User mockUser;
    private GamePostDTO gamePostDTO;

    private Game game;

    private Long gameId = 1L;

    @BeforeEach
    public void setup() {
        mockUser = new User();
        mockUser.setUsername("TestUser");
        gamePostDTO = new GamePostDTO();

        game = new Game();
        game.setGameId(gameId);
        game.setState(GameState.PREPARING);
        Set<User> players = new HashSet<>();
        players.add(mockUser);
        game.setPlayers(players);

    }

    @Test
    public void testCreateNewGame_Success() throws IOException, InterruptedException {
        when(userService.verifyUserByToken(anyString())).thenReturn(mockUser);
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(gameDeckService.fetchDeck(any(Game.class), eq(true))).thenReturn(new GameDeck());
        Game createdGame = gameService.createNewGame("validToken", gamePostDTO);

        assertNotNull(createdGame.getGameId());
        assertTrue(createdGame.getPlayers().contains(mockUser));
        verify(gameRepository).save(any(Game.class));
    }

    @Test
    public void testCreateNewGame_UniqueGameIdGeneration() throws IOException, InterruptedException {
        when(userService.verifyUserByToken(anyString())).thenReturn(mockUser);
        when(gameDeckService.fetchDeck(any(Game.class), eq(true))).thenReturn(new GameDeck());
        when(gameRepository.findByGameId(anyLong())).thenReturn(Optional.empty());
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Game createdGame = gameService.createNewGame("validToken", gamePostDTO);

        assertNotNull(createdGame.getGameId());
        verify(gameRepository, atLeastOnce()).findByGameId(anyLong());
        verify(gameRepository).save(any(Game.class));
    }

    @Test
    public void testLeaveGame_Success() {

        User anotherUser = new User();
        anotherUser.setUsername("AnotherUser");

        Set<User> players = new HashSet<>();
        players.add(mockUser);
        players.add(anotherUser);
        game.setPlayers(players);

        when(userService.verifyUserByToken(anyString())).thenReturn(mockUser);
        when(gameRepository.findByGameId(anyLong())).thenReturn(Optional.of(game));

        Game updatedGame = gameService.leaveGame("validToken", gameId);

        assertFalse(updatedGame.getPlayers().contains(mockUser));
        verify(eventPublisher).publishEvent(any());
        assertEquals(GameState.PREPARING, updatedGame.getState());
    }

    @Test
    public void testLeaveGame_UserNotInGame() {
        game.getPlayers().remove(mockUser);

        when(userService.verifyUserByToken(anyString())).thenReturn(mockUser);
        when(gameRepository.findByGameId(anyLong())).thenReturn(Optional.of(game));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            gameService.leaveGame("validToken", gameId);
        });

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    public void testLeaveGame_InvalidGameId() {
        when(userService.verifyUserByToken(anyString())).thenReturn(mockUser);
        when(gameRepository.findByGameId(anyLong())).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            gameService.leaveGame("validToken", gameId);
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    public void testLeaveGame_GameStateNotPreparing() {
        game.setState(GameState.ONGOING);

        when(userService.verifyUserByToken(anyString())).thenReturn(mockUser);
        when(gameRepository.findByGameId(anyLong())).thenReturn(Optional.of(game));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            gameService.leaveGame("validToken", gameId);
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    public void testLeaveGame_LastPlayerLeaves() {
        when(userService.verifyUserByToken(anyString())).thenReturn(mockUser);
        when(gameRepository.findByGameId(anyLong())).thenReturn(Optional.of(game));

        game.getPlayers().removeIf(user -> !user.equals(mockUser));

        Game updatedGame = gameService.leaveGame("validToken", gameId);

        assertTrue(updatedGame.getPlayers().isEmpty());
        assertEquals(GameState.ABORTED, updatedGame.getState());
    }

    @Test
    public void testJoinGame_Success() {
        game.setMaxPlayers(10);
        when(userService.verifyUserByToken("validToken")).thenReturn(mockUser);
        when(gameRepository.findByGameId(gameId)).thenReturn(Optional.of(game));

        User anotherUser = new User();
        anotherUser.setUsername("anotherUser");

        game.getPlayers().remove(mockUser);
        game.getPlayers().add(anotherUser);

        Game updatedGame = gameService.joinGame("validToken", gameId);

        assertTrue(updatedGame.getPlayers().contains(mockUser));
        assertEquals(2, updatedGame.getPlayers().size());
        verify(gameRepository).save(updatedGame);
    }

    @Test
    public void testJoinGame_GameNotFound() {
        when(gameRepository.findByGameId(gameId)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> gameService.joinGame("validToken", gameId));
    }

    @Test
    public void testJoinGame_NotPreparingState() {
        game.setState(GameState.ONGOING);  // Set the game state to active
        when(gameRepository.findByGameId(gameId)).thenReturn(Optional.of(game));

        assertThrows(ResponseStatusException.class, () -> gameService.joinGame("validToken", gameId));
    }

    @Test
    public void testJoinGame_UserAlreadyInGame() {
        when(userService.verifyUserByToken("validToken")).thenReturn(mockUser);
        when(gameRepository.findByGameId(gameId)).thenReturn(Optional.of(game));

        assertThrows(ResponseStatusException.class, () -> gameService.joinGame("validToken", gameId));
    }

    @Test
    public void testJoinGame_UnauthorizedUser() {
        when(userService.verifyUserByToken("invalidToken")).thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        assertThrows(ResponseStatusException.class, () -> gameService.joinGame("invalidToken", gameId));
    }

    @Test
    public void testGetGames() {

        User verifiedUser = new User();
        verifiedUser.setId(1L);
        verifiedUser.setUsername("TestUser");

        game = new Game();
        game.setGameId(1L);
        game.setState(GameState.PREPARING);
        game.setMode(GameMode.PUBLIC);
        game.setCreationdate(new Date());

        String token = "validToken";
        when(userService.verifyUserByToken(token)).thenReturn(verifiedUser);
        when(userService.getUsersFriends(verifiedUser.getId())).thenReturn(Collections.singletonList(verifiedUser));

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR_OF_DAY, -2);
        Date oneHourAgo = cal.getTime();

        List<Game> publicGames = Collections.singletonList(game);
        List<Game> privateUserGames = Collections.singletonList(game);

        when(gameRepository.findByStateAndModeAndCreationdateAfter(eq(GameState.PREPARING), eq(GameMode.PUBLIC), any(Date.class)))
                .thenReturn(publicGames);
        when(gameRepository.findByInitiatingUserAndStateAndModeAndCreationdateAfter(eq(verifiedUser), eq(GameState.PREPARING), eq(GameMode.PRIVATE), any(Date.class)))
                .thenReturn(privateUserGames);

        List<Game> result = gameService.getGames(token);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(game));
    }
}

