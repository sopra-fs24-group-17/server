package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.GameState;
import ch.uzh.ifi.hase.soprafs24.entity.Card;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.GameDeck;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.event.DrawCardsEvent;
import ch.uzh.ifi.hase.soprafs24.event.ExplosionReturnedToDeckEvent;
import ch.uzh.ifi.hase.soprafs24.event.PeekIntoDeckEvent;
import ch.uzh.ifi.hase.soprafs24.event.PlayerCardEvent;
import ch.uzh.ifi.hase.soprafs24.repository.CardRepository;
import ch.uzh.ifi.hase.soprafs24.repository.GameDeckRepository;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePostDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import org.checkerframework.checker.units.qual.C;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
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
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
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

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private GameEngineService gameEngineService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private HttpClient httpClient;

    @Mock
    HttpResponse<InputStream> mockResponse;

    @InjectMocks
    private GameDeckService gameDeckService;

    private GameDeckService spyGameDeckService;

    private User mockUser;
    private GamePostDTO gamePostDTO;

    private Game game;

    private GameDeck testDeck;

    private Long gameId = 1L;

    @BeforeEach
    public void setup() {
        mockUser = new User();
        mockUser.setUsername("TestUser");
        mockUser.setId(1L);
        gamePostDTO = new GamePostDTO();

        game = new Game();
        game.setGameId(gameId);
        game.setState(GameState.ONGOING);
        List<User> players = new ArrayList<>();
        players.add(mockUser);
        game.setPlayers(players);

        testDeck = new GameDeck();
        testDeck.setDeckID("testId");

        game.setGameDeck(testDeck);
        testDeck.setGame(game);

        // Manually create the spy object
        gameDeckService = new GameDeckService(gameDeckRepository, cardRepository, userService, eventPublisher, httpClient);
        spyGameDeckService = spy(gameDeckService);
    }

    // fetch deck
    @Test
    public void testFetchDeck_success() throws IOException, InterruptedException {
        String jsonRes = "{\"deck_id\":\"testId\", \"remaining\": 54}";
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);
        when(mockResponse.body()).thenReturn(jsonRes);
        when(gameDeckRepository.saveAndFlush(any(GameDeck.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GameDeck deck = gameDeckService.fetchDeck(game, false);
        assertNotNull(deck);
        assertEquals(54, deck.getRemainingCardsDeck());
        assertNotNull(deck.getDeckID());
        assertNotNull(deck.getGame());
    }

    @Test
    public void testSaveCards_success(){
        List<Card> mockCards = new ArrayList<>();
        Card card1 = new Card();
        mockCards.add(card1);

        // Mocks
        when(cardRepository.saveAll(any(List.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<Card> savedCards = gameDeckService.saveCards(mockCards);

        assertNotNull(savedCards);
        assertEquals(mockCards.size(), savedCards.size());
    }

    // TODO: Solve issue when mocking save cards

    @Test
    public void drawCardsFromDeckTest_success() throws IOException, InterruptedException {
        String responseBody = "{\n" +
                "  \"deck_id\": \"testDeckId\",\n" +
                "  \"cards\": [\n" +
                "    {\"code\": \"AS\", \"suit\": \"SPADES\", \"image\": \"http://image1.com\"},\n" +
                "    {\"code\": \"AC\", \"suit\": \"CLUBS\", \"image\": \"http://image2.com\"}\n" +
                "  ]\n" +
                "}";

        // Mock the saveCards method to return the list of cards
        doAnswer(invocation -> invocation.getArgument(0)).when(spyGameDeckService).saveCards(any(List.class));
        // Mock the HTTP response
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.body()).thenReturn(responseBody);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        // Perform the method under test
        List<Card> result = spyGameDeckService.drawCardsFromDeck(testDeck);

        // Verify interactions
        verify(httpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        verify(gameDeckRepository).saveAndFlush(testDeck);

        // Assertions
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("AS", result.get(0).getCode());
        assertEquals("SPADES", result.get(0).getSuit());
        assertEquals("http://image1.com", result.get(0).getImage());
        assertEquals("testDeckId", result.get(0).getDeckId());

        assertEquals("AC", result.get(1).getCode());
        assertEquals("CLUBS", result.get(1).getSuit());
        assertEquals("http://image2.com", result.get(1).getImage());
        assertEquals("testDeckId", result.get(1).getDeckId());

    }

    @Test
    public void drawCardsFromDealerPileTest_Success() throws IOException, InterruptedException {
        List<Card> mockCards = new ArrayList<>();
        Card card1 = new Card();
        card1.setCode("AS");
        mockCards.add(card1);
        Card card2 = new Card();
        card2.setCode("AC");
        mockCards.add(card2);

        String responseBody = "{\n" +
                "  \"deck_id\": \"testDeckId\",\n" +
                "  \"cards\": [\n" +
                "    {\"code\": \"AS\", \"suit\": \"SPADES\", \"image\": \"http://image1.com\"},\n" +
                "    {\"code\": \"AC\", \"suit\": \"CLUBS\", \"image\": \"http://image2.com\"}\n" +
                "  ]\n" +
                "}";

        Map<String, Integer> mockRemaining = new HashMap<>();
        mockRemaining.put("dealer", 55);
        game.setCurrentTurn(mockUser);

        // Mock the saveCards method to return the list of cards
        doReturn(responseBody).when(spyGameDeckService).getRemainingPileStats(any(GameDeck.class), any(Long.class));
        doReturn(mockRemaining).when(spyGameDeckService).parsePileCardCounts(any(String.class));
        // Mock the HTTP response
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.body()).thenReturn(responseBody);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);
        // Mock event publishing to avoid NullPointerException
        doNothing().when(eventPublisher).publishEvent(any());
        doReturn(mockCards).when(spyGameDeckService).parseCards(any(GameDeck.class), any(String.class), any(String.class),anyList(), anyList());

        // Perform the method under test
        List<Card> result = spyGameDeckService.drawCardsFromDealerPile(testDeck,2);

        // Verify interactions
        verify(httpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        //verify(gameDeckRepository).saveAndFlush(testDeck);

        // Assertions
        assertNotNull(result);
        assertEquals(2, result.size());
    }


    @Test
    public void testShuffleCardsInDealerPileTest_Success() throws IOException, InterruptedException {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        testDeck.setRemainingCardsDealerStack(20);
        game.setCurrentTurn(mockUser);
        testDeck.setGame(game);

        // Mock event publishing to avoid NullPointerException
        doNothing().when(eventPublisher).publishEvent(any());

        gameDeckService.shuffleCardsInDealerPile(testDeck);

        verify(httpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    public void testCreatePlayerPile_success() throws IOException, InterruptedException {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        gameDeckService.createPlayerPile(testDeck, mockUser.getId(), "AS");

        verify(httpClient).send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class));
    }

    @Test
    public void testCreateDealerPile_success() throws IOException, InterruptedException {
        List<Card> mockCards = new ArrayList<>();
        Card card1 = new Card();
        card1.setCode("AS");
        mockCards.add(card1);

        // Mock HTTP response for fetching deck
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        String jsonRes = "{\"deck_id\":\"testId\", \"remaining\": 54}";
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        // Mock repository save and flush
        when(gameDeckRepository.saveAndFlush(any(GameDeck.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Mock drawCardsFromDeck to return mock cards
        doReturn(mockCards).when(spyGameDeckService).drawCardsFromDeck(any(GameDeck.class));

        // Ensure game deck is not null
        assertNotNull(game.getGameDeck());

        // Call the method under test
        spyGameDeckService.createDealerPile(game);

        // Verify interactions
        verify(httpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    public void testPeekIntoDealerPile_success() throws IOException, InterruptedException {
        List<Card> mockCards = new ArrayList<>();
        Card card1 = new Card();
        card1.setCode("AS");
        mockCards.add(card1);
        Card card2 = new Card();
        card2.setCode("AC");
        mockCards.add(card2);
        Card card3 = new Card();
        card3.setCode("AD");
        mockCards.add(card3);

        // Mock HTTP response for fetching deck
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        String jsonRes = "{\"deck_id\":\"testId\", \"remaining\": 54}";
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        // Mock repository save and flush
        when(gameDeckRepository.saveAndFlush(any(GameDeck.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Ensure the method `drawCardsFromDealerPile` is properly mocked
        doReturn(mockCards).when(spyGameDeckService).drawCardsFromDealerPile(any(GameDeck.class), eq(3));

        // Mock event publishing to avoid NullPointerException
        doNothing().when(eventPublisher).publishEvent(any());

        // Call the method under test
        testDeck.setRemainingCardsDealerStack(55);
        game.setCurrentTurn(mockUser);
        List<Card> topCards = spyGameDeckService.peekIntoDealerPile(game);

        // Verify the interactions
        verify(spyGameDeckService).drawCardsFromDealerPile(any(GameDeck.class), eq(3));
        verify(spyGameDeckService).returnCardsToPile(any(GameDeck.class), eq("dealer"), anyString());
        verify(eventPublisher).publishEvent(any(PeekIntoDeckEvent.class));

        // Assertions
        assertNotNull(topCards);
        assertEquals(3, topCards.size());
        assertEquals("AS", topCards.get(0).getCode());
        assertEquals("AC", topCards.get(1).getCode());
        assertEquals("AD", topCards.get(2).getCode());
    }


    @Test
    public void testReturnCardsToPile_success() throws IOException, InterruptedException {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        testDeck.setRemainingCardsDealerStack(55);

        gameDeckService.returnCardsToPile(testDeck, mockUser.getUsername(), "AS");

        verify(httpClient).send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class));
    }
    @Test
    public void testRemoveCardsFromPlayerPile_success() throws IOException, InterruptedException {
        String jsonRes = "{\"success\":\"true\", \"deck_id\":\"testId\", \"remaining\": 54}";
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);
        when(mockResponse.body()).thenReturn(jsonRes);

        gameDeckService.removeCardsFromPlayerPile(game, mockUser.getId(), "AS");

        verify(httpClient).send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class));
    }

    @Test
    public void testPlaceCardsToPlayPile_success() throws IOException, InterruptedException {
        List<Card> mockCards = new ArrayList<>();
        Card card1 = new Card();
        card1.setCode("AS");
        mockCards.add(card1);
        Card card2 = new Card();
        card2.setCode("AC");
        mockCards.add(card2);
        Card card3 = new Card();
        card3.setCode("AD");
        mockCards.add(card3);

        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);
        when(userService.getUserById(any(Long.class))).thenReturn(mockUser);

        gameDeckService.placeCardsToPlayPile(game, mockUser.getId(), mockCards, "AS,AC,AD");

        verify(httpClient).send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class));
        verify(userService).getUserById(any(Long.class));
    }



    @Test
    public void testGetRemainingPileStats_success() throws IOException, InterruptedException {
        String jsonRes = "{\"deck_id\":\"testId\", \"remaining\": 54}";
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);
        when(mockResponse.body()).thenReturn(jsonRes);

        String response = gameDeckService.getRemainingPileStats(testDeck, mockUser.getId());

        assertNotNull(response);
        verify(httpClient).send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class));
    }

    @Test
    public void testParsePileCardCounts_success() throws IOException {
        String jsonRes = "{\"success\":\"true\", \"deck_id\":\"testId\", \"remaining\": 54, \"piles\": {\"discard\":{\"remaining\":2}}}";

        Map<String, Integer> response = gameDeckService.parsePileCardCounts(jsonRes);

        assertNotNull(response);
    }

    @Test
    public void testParsePileCardCounts_noPiles() throws IOException {
        String jsonRes = "{\"success\":\"true\"}";

        assertThrows(IllegalStateException.class, () -> {
            Map<String, Integer> response = gameDeckService.parsePileCardCounts(jsonRes);
        });
    }

    @Test
    public void testParseCards_withAdditionalPileInfo() throws IOException {
        String jsonResponse = "{ \"deck_id\": \"testDeckId\", \"cards\": [{\"code\": \"AS\", \"suit\": \"SPADES\", \"image\": \"http://image1.com\"}, {\"code\": \"AC\", \"suit\": \"CLUBS\", \"image\": \"http://image2.com\"}], \"pile\": { \"remaining\": 10 } }";
        List<String> cardsKeyPath = List.of("cards");
        List<String> additionalPileInfo = List.of("pile");

        // Mock repository save and flush
        when(gameDeckRepository.saveAndFlush(any(GameDeck.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doAnswer(invocation -> invocation.getArgument(0)).when(spyGameDeckService).saveCards(anyList());


        List<Card> result = spyGameDeckService.parseCards(testDeck, jsonResponse, "deck_id", cardsKeyPath, additionalPileInfo);

        // Verify the repository interaction
        verify(gameDeckRepository).saveAndFlush(any(GameDeck.class));

        // Assertions
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("AS", result.get(0).getCode());
        assertEquals("SPADES", result.get(0).getSuit());
        assertEquals("http://image1.com", result.get(0).getImage());
        assertEquals("testDeckId", result.get(0).getDeckId());

        assertEquals("AC", result.get(1).getCode());
        assertEquals("CLUBS", result.get(1).getSuit());
        assertEquals("http://image2.com", result.get(1).getImage());
        assertEquals("testDeckId", result.get(1).getDeckId());

        assertEquals(10, testDeck.getRemainingCardsDealerStack());
    }

    @Test
    public void testParseCards_withoutAdditionalPileInfo() throws IOException {
        String jsonResponse = "{ \"deck_id\": \"testDeckId\", \"cards\": [{\"code\": \"AS\", \"suit\": \"SPADES\", \"image\": \"http://image1.com\"}, {\"code\": \"AC\", \"suit\": \"CLUBS\", \"image\": \"http://image2.com\"}] }";
        List<String> cardsKeyPath = List.of("cards");

        doAnswer(invocation -> invocation.getArgument(0)).when(spyGameDeckService).saveCards(anyList());

        List<Card> result = spyGameDeckService.parseCards(testDeck, jsonResponse, "deck_id", cardsKeyPath, null);

        // Verify no repository interaction
        verify(gameDeckRepository, never()).saveAndFlush(any(GameDeck.class));

        // Assertions
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("AS", result.get(0).getCode());
        assertEquals("SPADES", result.get(0).getSuit());
        assertEquals("http://image1.com", result.get(0).getImage());
        assertEquals("testDeckId", result.get(0).getDeckId());

        assertEquals("AC", result.get(1).getCode());
        assertEquals("CLUBS", result.get(1).getSuit());
        assertEquals("http://image2.com", result.get(1).getImage());
        assertEquals("testDeckId", result.get(1).getDeckId());
    }

    @Test
    public void testSaveCards_emptyList(){
        List<Card> cards = new ArrayList<>();

        List<Card> savedCards = spyGameDeckService.saveCards(cards);

        assertNotNull(savedCards);
        assertEquals(savedCards.size(), 0);
    }

    @Test
    public void testDrawCardsFromDealerPile_excesiveCards() throws IOException, InterruptedException {
        String responseBody = "{\n" +
                "  \"deck_id\": \"testDeckId\",\n" +
                "  \"cards\": [\n" +
                "    {\"code\": \"AS\", \"suit\": \"SPADES\", \"image\": \"http://image1.com\"},\n" +
                "    {\"code\": \"AC\", \"suit\": \"CLUBS\", \"image\": \"http://image2.com\"}\n" +
                "  ]\n" +
                "}";

        Map<String, Integer> mockRemaining = new HashMap<>();
        mockRemaining.put("dealer", 0);
        game.setCurrentTurn(mockUser);

        // Mock the saveCards method to return the list of cards
        doReturn(responseBody).when(spyGameDeckService).getRemainingPileStats(any(GameDeck.class), any(Long.class));
        doReturn(mockRemaining).when(spyGameDeckService).parsePileCardCounts(any(String.class));

        // Perform the method under test
        assertThrows(ResponseStatusException.class, () -> {
            spyGameDeckService.drawCardsFromDealerPile(testDeck,2);
        });
    }

    @Test
    public void testShuffleCardsInDealerPile_excesiveCards() throws IOException, InterruptedException {
        testDeck.setRemainingCardsDealerStack(0);
        assertThrows(ResponseStatusException.class, () -> {
            gameDeckService.shuffleCardsInDealerPile(testDeck);
        });
    }

    @Test
    public void testExploreTopCardPlayPile_success() throws IOException, InterruptedException {
        List<Card> mockCards = new ArrayList<>();
        Card card1 = new Card();
        card1.setCode("AS");
        mockCards.add(card1);

        String responseBody = "{\n" +
                "  \"deck_id\": \"testDeckId\",\n" +
                "  \"cards\": [\n" +
                "    {\"code\": \"AS\", \"suit\": \"SPADES\", \"image\": \"http://image1.com\"},\n" +
                "  ]\n" +
                "}";
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);
        when(mockResponse.body()).thenReturn(responseBody);
        doReturn(mockCards).when(spyGameDeckService).parseCards(any(GameDeck.class), eq(responseBody), eq("deck_id"),eq(Arrays.asList("piles", "play","cards")), eq(null));
        doAnswer(invocation -> invocation.getArgument(0)).when(spyGameDeckService).saveCards(anyList());

        List<Card> topCard = spyGameDeckService.exploreTopCardPlayPile(testDeck);

        assertNotNull(topCard);
        assertNotNull(topCard.get(0));
        verify(httpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        verify(spyGameDeckService).parseCards(any(GameDeck.class), eq(responseBody), eq("deck_id"), eq(Arrays.asList("piles", "play", "cards")), eq(null));
    }

    @Test
    public void testExploreTopCardPlayPile_noTopCard() throws IOException, InterruptedException {
        List<Card> mockCards = new ArrayList<>();

        String responseBody = "{\n" +
                "  \"deck_id\": \"testDeckId\",\n" +
                "  \"cards\": [\n" +
                "    {\"code\": \"AS\", \"suit\": \"SPADES\", \"image\": \"http://image1.com\"},\n" +
                "  ]\n" +
                "}";
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);
        when(mockResponse.body()).thenReturn(responseBody);
        doReturn(mockCards).when(spyGameDeckService).parseCards(any(GameDeck.class), eq(responseBody), eq("deck_id"),eq(Arrays.asList("piles", "play","cards")), eq(null));

        List<Card> topCard = spyGameDeckService.exploreTopCardPlayPile(testDeck);

        assertNull(topCard);
        verify(httpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        verify(spyGameDeckService).parseCards(any(GameDeck.class), eq(responseBody), eq("deck_id"), eq(Arrays.asList("piles", "play", "cards")), eq(null));
    }

    @Test
    public void testExploreDefuseCardInPlayerPile_noDefuse() throws IOException, InterruptedException {
        List<Card> mockCards = new ArrayList<>();
        Card card1 = new Card();
        card1.setCode("AS");
        mockCards.add(card1);

        String responseBody = "{\n" +
                "  \"deck_id\": \"testDeckId\",\n" +
                "  \"cards\": [\n" +
                "    {\"code\": \"AS\", \"suit\": \"SPADES\", \"image\": \"http://image1.com\"},\n" +
                "  ]\n" +
                "}";
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);
        when(mockResponse.body()).thenReturn(responseBody);
        doReturn(mockCards).when(spyGameDeckService).parseCards(any(GameDeck.class), eq(responseBody), eq("deck_id"),eq(Arrays.asList("piles", mockUser.getId().toString(),"cards")), eq(null));

        String defuseCode = spyGameDeckService.exploreDefuseCardInPlayerPile(testDeck, mockUser.getId());

        assertNull(defuseCode);
        verify(httpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        verify(spyGameDeckService).parseCards(any(GameDeck.class), eq(responseBody), eq("deck_id"), eq(Arrays.asList("piles", mockUser.getId().toString(), "cards")), eq(null));
    }

    @Test
    public void testExploreDefuseCardInPlayerPile_success() throws IOException, InterruptedException {
        List<Card> mockCards = new ArrayList<>();
        Card card1 = new Card();
        card1.setCode("KS");
        card1.setInternalCode("defuse");
        mockCards.add(card1);

        String responseBody = "{\n" +
                "  \"deck_id\": \"testDeckId\",\n" +
                "  \"cards\": [\n" +
                "    {\"code\": \"KS\", \"suit\": \"SPADES\", \"image\": \"http://image1.com\"},\n" +
                "  ]\n" +
                "}";
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);
        when(mockResponse.body()).thenReturn(responseBody);
        doReturn(mockCards).when(spyGameDeckService).parseCards(any(GameDeck.class), eq(responseBody), eq("deck_id"),eq(Arrays.asList("piles", mockUser.getId().toString(),"cards")), eq(null));

        String defuseCode = spyGameDeckService.exploreDefuseCardInPlayerPile(testDeck, mockUser.getId());

        assertNotNull(defuseCode);
        verify(httpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        verify(spyGameDeckService).parseCards(any(GameDeck.class), eq(responseBody), eq("deck_id"), eq(Arrays.asList("piles", mockUser.getId().toString(), "cards")), eq(null));
    }

    @Test
    public void testDrawRandomCardDealerPile() throws IOException, InterruptedException {
        List<Card> mockCards = new ArrayList<>();
        Card card1 = new Card();
        card1.setCode("KS");
        card1.setInternalCode("defuse");
        mockCards.add(card1);

        String responseBody = "{\n" +
                "  \"deck_id\": \"testDeckId\",\n" +
                "  \"cards\": [\n" +
                "    {\"code\": \"KS\", \"suit\": \"SPADES\", \"image\": \"http://image1.com\"},\n" +
                "  ]\n" +
                "}";
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);
        when(mockResponse.body()).thenReturn(responseBody);
        doReturn(mockCards).when(spyGameDeckService).parseCards(any(GameDeck.class), eq(responseBody), eq("deck_id"),eq(Arrays.asList("cards")), eq(null));

        Card randomCard = spyGameDeckService.drawRandomCardDealerPile(testDeck);

        assertNotNull(randomCard);
        verify(httpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        verify(spyGameDeckService).parseCards(any(GameDeck.class), eq(responseBody), eq("deck_id"), eq(Arrays.asList("cards")), eq(null));
    }

    @Test
    public void testDrawCardFromPlayerPile_randomCard() throws IOException, InterruptedException {
        List<Card> mockCards = new ArrayList<>();
        Card card1 = new Card();
        card1.setCode("KS");
        card1.setInternalCode("defuse");
        mockCards.add(card1);

        String responseBody = "{\n" +
                "  \"deck_id\": \"testDeckId\",\n" +
                "  \"cards\": [\n" +
                "    {\"code\": \"KS\", \"suit\": \"SPADES\", \"image\": \"http://image1.com\"},\n" +
                "  ]\n" +
                "}";
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);
        when(mockResponse.body()).thenReturn(responseBody);
        doReturn(mockCards).when(spyGameDeckService).parseCards(any(GameDeck.class), eq(responseBody), eq("deck_id"),eq(Arrays.asList("cards")), eq(null));

        Card randomCard = spyGameDeckService.drawCardFromPlayerPile(testDeck, mockUser.getId(), null);

        assertNotNull(randomCard);
        verify(httpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        verify(spyGameDeckService).parseCards(any(GameDeck.class), eq(responseBody), eq("deck_id"), eq(Arrays.asList("cards")), eq(null));
    }

    @Test
    public void testDrawCardFromPlayerPile_specificCard() throws IOException, InterruptedException {
        List<Card> mockCards = new ArrayList<>();
        Card card1 = new Card();
        card1.setCode("KS");
        card1.setInternalCode("defuse");
        mockCards.add(card1);

        String responseBody = "{\n" +
                "  \"deck_id\": \"testDeckId\",\n" +
                "  \"cards\": [\n" +
                "    {\"code\": \"KS\", \"suit\": \"SPADES\", \"image\": \"http://image1.com\"},\n" +
                "  ]\n" +
                "}";
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);
        when(mockResponse.body()).thenReturn(responseBody);
        doReturn(mockCards).when(spyGameDeckService).parseCards(any(GameDeck.class), eq(responseBody), eq("deck_id"),eq(Arrays.asList("cards")), eq(null));

        Card randomCard = spyGameDeckService.drawCardFromPlayerPile(testDeck, mockUser.getId(), "KS");

        assertNotNull(randomCard);
        verify(httpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        verify(spyGameDeckService).parseCards(any(GameDeck.class), eq(responseBody), eq("deck_id"), eq(Arrays.asList("cards")), eq(null));
    }

    @Test
    public void testRemoveSpecificCardsFromPile() throws IOException, InterruptedException {
        List<Card> mockCards = new ArrayList<>();
        Card card1 = new Card();
        card1.setCode("KS");
        card1.setInternalCode("defuse");
        mockCards.add(card1);

        String responseBody = "{\n" +
                "  \"deck_id\": \"testDeckId\",\n" +
                "  \"cards\": [\n" +
                "    {\"code\": \"KS\", \"suit\": \"SPADES\", \"image\": \"http://image1.com\"},\n" +
                "  ]\n" +
                "}";
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);
        when(mockResponse.body()).thenReturn(responseBody);
        doReturn(mockCards).when(spyGameDeckService).parseCards(any(GameDeck.class), eq(responseBody), eq("deck_id"),eq(Arrays.asList("cards")), eq(Arrays.asList("piles", testDeck.getDealerPileId())));

        List<Card> removedCard = spyGameDeckService.removeSpecificCardsFromPile(testDeck, "KS", testDeck.getDealerPileId());

        assertNotNull(removedCard);
        verify(httpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        verify(spyGameDeckService).parseCards(any(GameDeck.class), eq(responseBody), eq("deck_id"), eq(Arrays.asList("cards")), eq(Arrays.asList("piles", testDeck.getDealerPileId())));

    }

    @Test
    public void testReturnExplosionCardToDealerPile_random() throws IOException, InterruptedException {
        Card returnedCard = new Card();
        returnedCard.setCode("AS");
        returnedCard.setInternalCode("bomb");

        String responseBody = "{\n" +
                "  \"deck_id\": \"testDeckId\",\n" +
                "  \"cards\": [\n" +
                "    {\"code\": \"AS\", \"suit\": \"SPADES\", \"image\": \"http://image1.com\"},\n" +
                "    {\"code\": \"AC\", \"suit\": \"CLUBS\", \"image\": \"http://image2.com\"}\n" +
                "  ]\n" +
                "}";

        Map<String, Integer> mockRemaining = new HashMap<>();
        mockRemaining.put("dealer", 55);
        game.setCurrentTurn(mockUser);

        // Mock the saveCards method to return the list of cards
        doReturn(responseBody).when(spyGameDeckService).getRemainingPileStats(any(GameDeck.class), any(Long.class));
        doReturn(mockRemaining).when(spyGameDeckService).parsePileCardCounts(any(String.class));
        doNothing().when(spyGameDeckService).returnCardsToPile(testDeck, "dealer", returnedCard.getCode());
        doNothing().when(spyGameDeckService).shuffleCardsInDealerPile(testDeck);
        doNothing().when(eventPublisher).publishEvent(any());

        spyGameDeckService.returnExplosionCardToDealerPile(game, -10, returnedCard);;

        verify(spyGameDeckService).returnCardsToPile(testDeck, "dealer", returnedCard.getCode());
        verify(spyGameDeckService).shuffleCardsInDealerPile(testDeck);
        verify(eventPublisher).publishEvent(any(ExplosionReturnedToDeckEvent.class));
    }

    @Test
    public void testReturnExplosionCardToDealerPile_topOfPile() throws IOException, InterruptedException {
        Card returnedCard = new Card();
        returnedCard.setCode("AS");
        returnedCard.setInternalCode("bomb");

        String responseBody = "{\n" +
                "  \"deck_id\": \"testDeckId\",\n" +
                "  \"cards\": [\n" +
                "    {\"code\": \"AS\", \"suit\": \"SPADES\", \"image\": \"http://image1.com\"},\n" +
                "    {\"code\": \"AC\", \"suit\": \"CLUBS\", \"image\": \"http://image2.com\"}\n" +
                "  ]\n" +
                "}";

        Map<String, Integer> mockRemaining = new HashMap<>();
        mockRemaining.put("dealer", 55);
        game.setCurrentTurn(mockUser);

        // Mock the saveCards method to return the list of cards
        doReturn(responseBody).when(spyGameDeckService).getRemainingPileStats(any(GameDeck.class), any(Long.class));
        doReturn(mockRemaining).when(spyGameDeckService).parsePileCardCounts(any(String.class));
        doNothing().when(spyGameDeckService).returnCardsToPile(testDeck, "dealer", returnedCard.getCode());
        doNothing().when(eventPublisher).publishEvent(any());

        spyGameDeckService.returnExplosionCardToDealerPile(game, 0, returnedCard);;

        verify(spyGameDeckService).returnCardsToPile(testDeck, "dealer", returnedCard.getCode());
        verify(eventPublisher).publishEvent(any(ExplosionReturnedToDeckEvent.class));
    }

    @Test
    public void testReturnExplosionCardToDealerPile_bottomOfPile() throws IOException, InterruptedException {
        Card returnedCard = new Card();
        returnedCard.setCode("AS");
        returnedCard.setInternalCode("bomb");

        Card pileCard = new Card();
        pileCard.setCode("5S");
        pileCard.setInternalCode("shuffle");
        List<Card> mockCards = new ArrayList<>();

        mockCards.add(pileCard);

        String responseBody = "{\n" +
                "  \"deck_id\": \"testDeckId\",\n" +
                "  \"cards\": [\n" +
                "    {\"code\": \"AS\", \"suit\": \"SPADES\", \"image\": \"http://image1.com\"},\n" +
                "    {\"code\": \"AC\", \"suit\": \"CLUBS\", \"image\": \"http://image2.com\"}\n" +
                "  ]\n" +
                "}";

        Map<String, Integer> mockRemaining = new HashMap<>();
        mockRemaining.put("dealer", 1);
        game.setCurrentTurn(mockUser);

        // Mock the saveCards method to return the list of cards
        doReturn(responseBody).when(spyGameDeckService).getRemainingPileStats(any(GameDeck.class), any(Long.class));
        doReturn(mockRemaining).when(spyGameDeckService).parsePileCardCounts(any(String.class));
        doReturn(mockCards).when(spyGameDeckService).drawCardsFromDealerPile(testDeck, 1);
        doNothing().when(spyGameDeckService).returnCardsToPile(testDeck, "dealer", returnedCard.getCode()+","+pileCard.getCode());
        doNothing().when(eventPublisher).publishEvent(any());

        spyGameDeckService.returnExplosionCardToDealerPile(game, -1, returnedCard);;

        verify(spyGameDeckService).returnCardsToPile(testDeck, "dealer", returnedCard.getCode()+","+pileCard.getCode());
        verify(spyGameDeckService).drawCardsFromDealerPile(testDeck, 1);
        verify(eventPublisher).publishEvent(any(ExplosionReturnedToDeckEvent.class));
    }

    @Test
    public void testReturnExplosionCardToDealerPile_else() throws IOException, InterruptedException {
        Card returnedCard = new Card();
        returnedCard.setCode("AS");
        returnedCard.setInternalCode("bomb");

        Card pileCard = new Card();
        pileCard.setCode("5S");
        pileCard.setInternalCode("shuffle");
        List<Card> mockCards = new ArrayList<>();

        mockCards.add(pileCard);

        String responseBody = "{\n" +
                "  \"deck_id\": \"testDeckId\",\n" +
                "  \"cards\": [\n" +
                "    {\"code\": \"AS\", \"suit\": \"SPADES\", \"image\": \"http://image1.com\"},\n" +
                "    {\"code\": \"AC\", \"suit\": \"CLUBS\", \"image\": \"http://image2.com\"}\n" +
                "  ]\n" +
                "}";

        Map<String, Integer> mockRemaining = new HashMap<>();
        mockRemaining.put("dealer", 5);
        game.setCurrentTurn(mockUser);

        // Mock the saveCards method to return the list of cards
        doReturn(responseBody).when(spyGameDeckService).getRemainingPileStats(any(GameDeck.class), any(Long.class));
        doReturn(mockRemaining).when(spyGameDeckService).parsePileCardCounts(any(String.class));
        doReturn(mockCards).when(spyGameDeckService).drawCardsFromDealerPile(testDeck, 2);
        doNothing().when(spyGameDeckService).returnCardsToPile(testDeck, "dealer", returnedCard.getCode()+","+pileCard.getCode());
        doNothing().when(eventPublisher).publishEvent(any());

        spyGameDeckService.returnExplosionCardToDealerPile(game, 2, returnedCard);;

        verify(spyGameDeckService).returnCardsToPile(testDeck, "dealer", returnedCard.getCode()+","+pileCard.getCode());
        verify(spyGameDeckService).drawCardsFromDealerPile(testDeck, 2);
        verify(eventPublisher).publishEvent(any(ExplosionReturnedToDeckEvent.class));
    }

    @Test
    public void testRemoveCardsFromPlayerPile_apiFailure() throws IOException, InterruptedException {
        String jsonRes = "{\"success\":false, \"error\":\"User doesn't possess the card(s)\"}";
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);
        when(mockResponse.body()).thenReturn(jsonRes);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            gameDeckService.removeCardsFromPlayerPile(game, mockUser.getId(), "AS");
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Move is invalid, user doesn't poses the card(s) played", exception.getReason());
    }

    @Test
    public void testRemoveCardsFromPlayerPile_ioException() throws IOException, InterruptedException {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("Network error"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            gameDeckService.removeCardsFromPlayerPile(game, mockUser.getId(), "AS");
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Move is invalid, user doesn't poses the card(s) played", exception.getReason());
    }
}