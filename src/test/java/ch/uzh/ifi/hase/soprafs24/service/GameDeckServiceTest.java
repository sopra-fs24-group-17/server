package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.GameState;
import ch.uzh.ifi.hase.soprafs24.entity.Card;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.GameDeck;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.event.DrawCardsEvent;
import ch.uzh.ifi.hase.soprafs24.repository.CardRepository;
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
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.stream.Collectors;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@ExtendWith(MockitoExtension.class)
public class GameDeckServiceTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GameDeckRepository gameDeckRepository;

    @Mock
    private CardRepository cardRepository;

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
    private ObjectMapper objectMapper;

    @Spy
    HttpClient httpClient;

    @Mock
    HttpResponse<InputStream> mockResponse;

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
        ObjectMapper testMapper = new ObjectMapper();
        String jsonRes = "{\"deck_id\":\"testId\", \"remaining\": 54}";
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);
        when(mockResponse.body()).thenReturn(jsonRes);
        when(objectMapper.readTree(any(String.class))).thenReturn(testMapper.readTree(jsonRes));
        when(gameDeckRepository.saveAndFlush(any(GameDeck.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String newDeckUri = "https://deckofcardsapi.com/api/deck/new/shuffle/?deck_count=1&jokers_enabled=true";

        HttpRequest newDeckRequest = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(newDeckUri))
                .build();

        HttpResponse<String> newDeckResponse = httpClient.send(newDeckRequest, HttpResponse.BodyHandlers.ofString());

        String test = (newDeckResponse.body());

        JsonNode rootNode = objectMapper.readTree(newDeckResponse.body());
        GameDeck deck = new GameDeck();

        // Extraction of necessary variables
        deck.setDeckID(rootNode.get("deck_id").asText());
        deck.setRemainingCards(rootNode.get("remaining").asInt());
        deck.setGame(game);
        deck = gameDeckRepository.saveAndFlush(deck);

        //GameDeck deck = gameDeckService.fetchDeck(game, false);
        assertNotNull(deck);
        assertEquals(54, deck.getRemainingCards());
        assertNotNull(deck.getDeckID());
        assertNotNull(deck.getGame());
    }

    @Test
    public void drawCardsFromDeckTest_success() throws IOException, InterruptedException {
        int N = 1;
        List<Card> mockCards = new ArrayList<>(Collections.nCopies(N, new Card()));

        when(gameDeckService.saveCards(any(List.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(gameDeckService.parseCards(any(String.class), any(GameDeck.class))).thenReturn(mockCards);
        String jsonRes = "{\"success\":true}";
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);
        when(mockResponse.body()).thenReturn(jsonRes);
        when(gameDeckService.fetchDeck(any(Game.class), eq(false))).thenReturn(new GameDeck());

        // Create a new Deck
        GameDeck deck = gameDeckService.fetchDeck(game, false);
        deck.setRemainingCards(5);
        // draw cards
        //List<Card> drawnCards = gameDeckService.drawCardsFromDeck(deck, N);

        if (N > deck.getRemainingCards()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Number of cards to be drawn exceeds available cards");
        }

        String drawCardsFromDeckUri = String.format("https://deckofcardsapi.com/api/deck/%s/draw/?count=%s", deck.getDeckID(), deck);

        HttpRequest drawCardsFromDeckRequest = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(drawCardsFromDeckUri))
                .build();

        HttpResponse<String> drawCardsFromDeckResponse = httpClient.send(drawCardsFromDeckRequest, HttpResponse.BodyHandlers.ofString());

        List<Card> drawnCards = gameDeckService.parseCards(drawCardsFromDeckResponse.body(), deck);

        drawnCards = gameDeckService.saveCards(drawnCards);

        // assert
        //verify(gameDeckService).parseCards(any(String.class), any(GameDeck.class));
        assertNotNull(drawnCards);
        assertEquals(drawnCards.size(), N);
    }

    @Test
    public void drawCardsFromDealerPileTest_Success() throws IOException, InterruptedException {
        int N = 1;
        List<Card> mockCards = new ArrayList<>(Collections.nCopies(N, new Card()));

        when(gameDeckService.saveCards(any(List.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(gameDeckService.parseCardsDealer(any(String.class), any(GameDeck.class))).thenReturn(mockCards);
        String jsonRes = "{\"success\":true}";
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);
        when(mockResponse.body()).thenReturn(jsonRes);
        when(gameDeckService.fetchDeck(any(Game.class), eq(false))).thenReturn(new GameDeck());

        game.setCurrentTurn(mockUser);

        // Create a new Deck
        GameDeck deck = gameDeckService.fetchDeck(game, false);
        deck.setRemainingCards(5);
        deck.setRemainingCardsDealerStack(100);
        deck.setDeckID("player1");
        deck.setGame(game);
        // draw cards
        //List<Card> savedCards = gameDeckService.drawCardsFromDealerPile(deck, N);


        if (N > deck.getRemainingCardsDealerStack()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Number of cards to be drawn exceeds available cards");
        }

        String drawCardsFromPileUri = String.format("https://www.deckofcardsapi.com/api/deck/%s/pile/%s/draw/?count=%s", deck.getDeckID(), deck.getDealerPileId(), N);

        HttpRequest drawCardsFromPileRequest = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(drawCardsFromPileUri))
                .build();

        HttpResponse<String> drawCardsFromPileResponse = httpClient.send(drawCardsFromPileRequest, HttpResponse.BodyHandlers.ofString());

        List<Card> cards = gameDeckService.parseCardsDealer(drawCardsFromPileResponse.body(), deck);

        List<Card> savedCards = gameDeckService.saveCards(cards);

        String playerName = deck.getGame().getCurrentTurn().getUsername();



        // assert
        //verify(gameDeckService).parseCards(any(String.class), any(GameDeck.class));
        assertNotNull(savedCards);
        assertEquals(savedCards.size(), N);
        assertNotNull(playerName);
    }

    /*
    @Test
    public void shuffleCardsInDealerPileTest_Success(){

    }
     */
}