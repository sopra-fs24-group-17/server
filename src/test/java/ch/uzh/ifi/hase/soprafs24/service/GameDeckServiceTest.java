package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.GameState;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.GameDeck;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.GameDeckRepository;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePostDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.internal.matchers.Null;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@ExtendWith(MockitoExtension.class)
public class GameDeckServiceTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GameDeckRepository gameDeckRepository;

    @Mock ObjectMapper objectMapper;

    @Mock
    private UserService userService;

    @InjectMocks
    private GameService gameService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private GameEngineService gameEngineService;

    @Mock
    private GameDeckService gameDeckService;

    @Mock
    HttpClient httpClient;

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
        game.setState(GameState.ONGOING);
        Set<User> players = new HashSet<>();
        players.add(mockUser);
        game.setPlayers(players);
    }

    // fetch deck
    @Test
    public void testFetchDeck_success() throws IOException, InterruptedException {
        /*
        String url = "https://deckofcardsapi.com/api/deck/new/shuffle/?deck_count=1&jokers_enabled=true";
        String expectedResponse = "{\"deckId\": 1123, \"remaining\": 54}";

        HttpResponse<String> httpResponse = Mockito.mock(HttpResponse.class);
        Mockito.when(httpResponse.body()).thenReturn(expectedResponse);

        HttpClient httpClientMock = Mockito.mock(HttpClient.class);
        try(MockedStatic<HttpClient> httpClientMockedStatic = Mockito.mockStatic(HttpClient.class)) {
            httpClientMockedStatic.when(HttpClient::newHttpClient).thenReturn(httpClientMock);
            Mockito.when(httpClientMock.send(Mockito.any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                    .thenReturn(httpResponse);

            String actualResponse = MyClass.myMethod(url);

            Assertions.assertEquals(expectedResponse, actualResponse);
        }


        String jsonResponse = "{\"deck_id\": \"test_deck_id\", \"remaining\": 52}";

        // Mocking the HTTP client behavior
        when(httpClient.send(any(HttpRequest.class)))
                .thenReturn(mockResponse);

         */

        // Mocking the HTTP response
        /*
        String jsonResponse = "{\"deck_id\": \"test_deck_id\", \"remaining\": 54}";
        HttpResponse<String> mockResponse = Mockito.mock(HttpResponse.class);
        when(mockResponse.body()).thenReturn(jsonResponse);

        // Mocking the HTTP client behavior
        HttpClient httpClientMock = Mockito.mock(HttpClient.class);
        when(httpClientMock.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        when(gameDeckRepository.saveAndFlush(any(GameDeck.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Create a Deck
        GameDeck deck = gameDeckService.fetchDeck(game, false);
        assertNotNull(deck);
        assertEquals(54, deck.getRemainingCards());
        assertNotNull(deck.getDeckID());
        assertNotNull(deck.getGame());
*/

        String jsonResponse = "{\"deck_id\": \"test_deck_id\", \"remaining\": 54}";
        HttpResponse<String> mockResponse = Mockito.mock(HttpResponse.class);
        when(mockResponse.body()).thenReturn(jsonResponse);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenAnswer(invocation -> {
                    HttpRequest request = invocation.getArgument(0);
                    // If the request URI matches, return the mock response
                    if (request.uri().toString().equals("https://deckofcardsapi.com/api/deck/new/shuffle/?deck_count=1&jokers_enabled=true")) {
                        return mockResponse;
                    }
                    return mockResponse;
                });


        String newDeckUri = "https://deckofcardsapi.com/api/deck/new/shuffle/?deck_count=1&jokers_enabled=true";

        HttpRequest newDeckRequest = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(newDeckUri))
                .build();


        HttpResponse<String> newDeckResponse = httpClient.send(newDeckRequest, HttpResponse.BodyHandlers.ofString());

        // Conversion of response to format suitable for extraction
        //JsonNode rootNode = objectMapper.readTree(newDeckResponse.body());
        GameDeck gameDeck = new GameDeck();

        // Extraction of necessary variables
        //gameDeck.setDeckID(rootNode.get("deck_id").asText());
        //gameDeck.setRemainingCards(rootNode.get("remaining").asInt());
        gameDeck.setGame(game);
        gameDeck = gameDeckRepository.saveAndFlush(gameDeck);
    }
}