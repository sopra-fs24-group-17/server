package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePostDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

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

    private User mockUser;
    private GamePostDTO gamePostDTO;

    @BeforeEach
    public void setup() {
        mockUser = new User();
        mockUser.setUsername("TestUser");
        gamePostDTO = new GamePostDTO();
    }

    @Test
    public void testCreateNewGame_Success() {
        when(userService.getUserByToken(anyString())).thenReturn(mockUser);
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Game createdGame = gameService.createNewGame("validToken", gamePostDTO);

        assertNotNull(createdGame.getGameId());
        assertTrue(createdGame.getPlayers().contains(mockUser));
        verify(gameRepository).save(any(Game.class));
    }

    @Test
    public void testCreateNewGame_UniqueGameIdGeneration() {
        when(userService.getUserByToken(anyString())).thenReturn(mockUser);
        when(gameRepository.findByGameId(anyLong())).thenReturn(Optional.empty());
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Game createdGame = gameService.createNewGame("validToken", gamePostDTO);

        assertNotNull(createdGame.getGameId());
        verify(gameRepository, atLeastOnce()).findByGameId(anyLong());
        verify(gameRepository).save(any(Game.class));
    }
}

