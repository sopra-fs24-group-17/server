package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.GameDTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.GameEngineService;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.CardMoveRequest;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.ExplosionCardRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GameController.class)
public class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GameService gameService;

    @MockBean
    private GameEngineService gameEngineService;

    @Mock
    private GameDTOMapper gameDTOMapper;

    @InjectMocks
    private GameEngineController gameEngineController;

    @BeforeEach
    public void setup(WebApplicationContext webApplicationContext) {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    private String asJsonString(final Object object) {
        try {
            return new ObjectMapper().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("The request body could not be created.%s", e.toString()));
        }
    }

    @Test
    public void testCreateNewGame_Success() throws Exception {
        GamePostDTO gamePostDTO = new GamePostDTO();
        User testUser = new User();
        testUser.setUsername("test");
        testUser.setId(1L);
        testUser.setPassword("password");

        Game game = new Game();
        game.setMaxPlayers(3);

        List<User> testPlayers = new ArrayList<>();
        testPlayers.add(testUser);

        game.setPlayers(testPlayers);

        given(gameService.createNewGame(Mockito.anyString(), Mockito.any())).willReturn(game);

        mockMvc.perform(post("/dashboard/games/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("token", "someToken")
                        .content(asJsonString(gamePostDTO)))
                .andExpect(status().isCreated());

        verify(gameService).createNewGame(anyString(), any(GamePostDTO.class));
    }

    @Test
    public void testLeaveGame_Success() throws Exception {
        Long gameId = 1L;
        String token = "validToken";
        Game game = new Game();
        game.setGameId(gameId);

        User testUser = new User();
        testUser.setUsername("test");
        testUser.setId(1L);
        testUser.setPassword("password");

        game.setMaxPlayers(3);

        List<User> testPlayers = new ArrayList<>();
        testPlayers.add(testUser);
        game.setPlayers(testPlayers);


        given(gameService.leaveGame(token, gameId)).willReturn(game);

        mockMvc.perform(put("/dashboard/games/leave/{gameId}", gameId)
                        .header("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId").value(gameId));
    }

    @Test
    public void testJoinGame_Success() throws Exception {
        Long gameId = 1L;
        String token = "validToken";
        Game game = new Game();

        game.setGameId(gameId);

        User testUser = new User();
        testUser.setUsername("test");
        testUser.setId(1L);
        testUser.setPassword("password");

        game.setMaxPlayers(3);

        List<User> testPlayers = new ArrayList<>();
        testPlayers.add(testUser);
        game.setPlayers(testPlayers);

        given(gameService.joinGame(token, gameId)).willReturn(game);
        mockMvc.perform(put("/dashboard/games/join/{gameId}", gameId)
                        .header("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId").value(gameId));
    }

    @Test
    void testHandleTerminatingMove_NoExplosion() throws Exception {
        Long gameId = 1L;
        Long userId = 2L;

        // Mock behavior: No explosion card is drawn
        when(gameEngineService.drawCardMoveTermination(gameId, userId)).thenReturn(null);

        gameEngineController.handleTerminatingMove(gameId, userId);

        // Verify that drawCardMoveTermination was called
        verify(gameEngineService, times(1)).drawCardMoveTermination(gameId, userId);
        // Ensure handleExplosionCard was not called
        verify(gameEngineService, never()).handleExplosionCard(anyLong(), anyLong(), anyString(), anyInt());
        // Verify that turnValidation and dispatchGameState were called
        verify(gameEngineService, times(1)).turnValidation(gameId, userId);
        verify(gameEngineService, times(1)).dispatchGameState(gameId, userId);
    }


    @Test
    void testHandleTerminatingMove_WithExplosion() throws Exception {
        Long gameId = 1L;
        Long userId = 2L;
        String explosionCard = "explosion";

        when(gameEngineService.drawCardMoveTermination(gameId, userId)).thenReturn(explosionCard);
        gameEngineController.handleTerminatingMove(gameId, userId);

        verify(gameEngineService, times(1)).drawCardMoveTermination(gameId, userId);
        verify(gameEngineService, times(1)).lookForDefuse(gameId, userId, explosionCard);
        verify(gameEngineService, times(1)).turnValidation(gameId, userId);
        verify(gameEngineService, times(1)).dispatchGameState(gameId, userId);
    }

    @Test
    void testHandleLeavingUser() throws Exception {
        Long gameId = 1L;
        Long userId = 2L;

        gameEngineController.handleLeavingUser(gameId, userId);
        verify(gameEngineService, times(1)).removeUserFromGame(gameId, userId);
    }

    @Test
    public void getAllJoinablePublicGames_returnsGameList() throws Exception {
        // Setup
        String token = "testToken";
        Game game1 = new Game();
        game1.setGameId(1L);
        game1.setMaxPlayers(2);
        GameGetDTO gameGetDTO1 = new GameGetDTO();
        gameGetDTO1.setGameId(1L);

        Game game2 = new Game();
        game2.setGameId(2L);
        game2.setMaxPlayers(2);
        GameGetDTO gameGetDTO2 = new GameGetDTO();
        gameGetDTO2.setGameId(2L);

        List<Game> games = Arrays.asList(game1, game2);
        List<GameGetDTO> dtos = Arrays.asList(gameGetDTO1, gameGetDTO2);

        when(gameService.getGames(token)).thenReturn(games);
        when(gameDTOMapper.convertEntityToGameGetDTO(game1)).thenReturn(gameGetDTO1);
        when(gameDTOMapper.convertEntityToGameGetDTO(game2)).thenReturn(gameGetDTO2);

        mockMvc.perform(MockMvcRequestBuilders.get("/dashboard/games")
                .header("token", token)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
}