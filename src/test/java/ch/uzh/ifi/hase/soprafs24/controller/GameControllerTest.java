package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.GameDTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opencensus.trace.Link;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Mock
    private GameDTOMapper gameDTOMapper;

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

        Set<User> testPlayers = new LinkedHashSet<>();
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

        Set<User> testPlayers = new LinkedHashSet<>();
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

        Set<User> testPlayers = new LinkedHashSet<>();
        testPlayers.add(testUser);
        game.setPlayers(testPlayers);

        given(gameService.joinGame(token, gameId)).willReturn(game);
        mockMvc.perform(put("/dashboard/games/join/{gameId}", gameId)
                        .header("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId").value(gameId));
    }
}
