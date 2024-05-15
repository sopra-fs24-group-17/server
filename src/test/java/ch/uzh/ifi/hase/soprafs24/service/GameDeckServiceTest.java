//package ch.uzh.ifi.hase.soprafs24.service;
//
//import ch.uzh.ifi.hase.soprafs24.constant.GameState;
//import ch.uzh.ifi.hase.soprafs24.entity.Card;
//import ch.uzh.ifi.hase.soprafs24.entity.Game;
//import ch.uzh.ifi.hase.soprafs24.entity.GameDeck;
//import ch.uzh.ifi.hase.soprafs24.entity.User;
//import ch.uzh.ifi.hase.soprafs24.event.DrawCardsEvent;
//import ch.uzh.ifi.hase.soprafs24.repository.CardRepository;
//import ch.uzh.ifi.hase.soprafs24.repository.GameDeckRepository;
//import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
//import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePostDTO;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.JsonSerializer;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.*;
//import org.mockito.internal.matchers.Null;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.context.ApplicationEventPublisher;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
//import org.springframework.web.server.ResponseStatusException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.net.URI;
//import java.net.http.HttpResponse;
//import java.util.*;
//import java.net.http.HttpClient;
//import java.net.http.HttpRequest;
//import java.net.http.HttpResponse;
//import java.util.stream.Collectors;
//
//import static org.mockito.Mockito.*;
//import static org.junit.jupiter.api.Assertions.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//
//@ExtendWith(MockitoExtension.class)
//public class GameDeckServiceTest {
//
//    @Mock
//    private GameRepository gameRepository;
//
//    @Mock
//    private GameDeckRepository gameDeckRepository;
//
//    @Mock
//    private CardRepository cardRepository;
//
//    @Mock
//    private UserService userService;
//
//    @InjectMocks
//    private GameService gameService;
//
//    @Mock
//    private ApplicationEventPublisher eventPublisher;
//
//    @Mock
//    private GameEngineService gameEngineService;
//
//    private GameDeckService gameDeckService;
//
//    @Mock
//    private ObjectMapper objectMapper;
//
//    @Spy
//    private HttpClient httpClient;
//
//    @Mock
//    HttpResponse<InputStream> mockResponse;
//
//    private User mockUser;
//    private GamePostDTO gamePostDTO;
//
//    private Game game;
//
//    private GameDeck testDeck;
//
//    private Long gameId = 1L;
//
//    @BeforeEach
//    public void setup() {
//        mockUser = new User();
//        mockUser.setUsername("TestUser");
//        mockUser.setId(1L);
//        gamePostDTO = new GamePostDTO();
//
//        game = new Game();
//        game.setGameId(gameId);
//        game.setState(GameState.ONGOING);
//        List<User> players = new ArrayList<>();
//        players.add(mockUser);
//        game.setPlayers(players);
//
//        testDeck = new GameDeck();
//        testDeck.setDeckID("testId");
//
//        game.setGameDeck(testDeck);
//        testDeck.setGame(game);
//
//        // Create an instance of GameDeckService with mocked dependencies
//        gameDeckService = new GameDeckService(gameDeckRepository, cardRepository, userService, eventPublisher, httpClient);
//    }
//
//    // fetch deck
//    @Test
//    public void testFetchDeck_success() throws IOException, InterruptedException {
//        String jsonRes = "{\"deck_id\":\"testId\", \"remaining\": 54}";
//        HttpResponse<String> mockResponse = mock(HttpResponse.class);
//        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
//                .thenReturn(mockResponse);
//        when(mockResponse.body()).thenReturn(jsonRes);
//        when(gameDeckRepository.saveAndFlush(any(GameDeck.class))).thenAnswer(invocation -> invocation.getArgument(0));
//
//        GameDeck deck = gameDeckService.fetchDeck(game, false);
//        assertNotNull(deck);
//        assertEquals(54, deck.getRemainingCardsDeck());
//        assertNotNull(deck.getDeckID());
//        assertNotNull(deck.getGame());
//    }
//
//    @Test
//    public void testSaveCards_success(){
//        List<Card> mockCards = new ArrayList<>();
//        Card card1 = new Card();
//        mockCards.add(card1);
//
//        // Mocks
//        when(cardRepository.saveAll(any(List.class))).thenAnswer(invocation -> invocation.getArgument(0));
//
//        List<Card> savedCards = gameDeckService.saveCards(mockCards);
//
//        assertNotNull(savedCards);
//        assertEquals(mockCards.size(), savedCards.size());
//    }
//
//    // TODO: Solve issue when mocking save cards
//    /*
//    @Test
//    public void drawCardsFromDeckTest_success() throws IOException, InterruptedException {
//        int N = 1;
//        List<Card> mockCards = new ArrayList<>(Collections.nCopies(N, new Card()));
//        List<String> mockArray = new ArrayList<>();
//
//        when(gameDeckService.saveCards(mockCards)).thenAnswer(invocation -> invocation.getArgument(0));
//        String jsonRes = "{\"success\":true, \"deck_id\": \"testId\", \"cards\": [{\"code\": \"AS\", \"image\": \"https://deckofcardsapi.com/static/img/AS.png\", \"images\": {\"svg\": \"https://deckofcardsapi.com/static/img/AS.svg\", \"png\": \"https://deckofcardsapi.com/static/img/AS.png\"}, \"value\": \"ACE\", \"suit\": \"SPADES\"}], \"remaining\": 53}";
//        HttpResponse<String> mockResponse = mock(HttpResponse.class);
//        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
//                .thenReturn(mockResponse);
//        when(mockResponse.body()).thenReturn(jsonRes);
//        when(gameDeckService.parseCards(any(GameDeck.class), eq(jsonRes), any(String.class), eq(mockArray), isNull()).thenReturn(mockCards);
//        //when(gameDeckService.fetchDeck(any(Game.class), eq(false))).thenReturn(new GameDeck());
//
//        testDeck.setRemainingCardsDeck(N);
//        // draw cards
//        List<Card> drawnCards = gameDeckService.drawCardsFromDeck(testDeck);
//
//        // assert
//        //verify(gameDeckService).parseCards(any(String.class), any(GameDeck.class));
//        //assertNotNull(drawnCards);
//        //assertEquals(drawnCards.size(), N);
//    }
//
//    @Test
//    public void drawCardsFromDealerPileTest_Success() throws IOException, InterruptedException {
//        int N = 1;
//        List<Card> mockCards = new ArrayList<>(Collections.nCopies(N, new Card()));
//
//        when(gameDeckService.saveCards(any(List.class))).thenAnswer(invocation -> invocation.getArgument(0));
//        when(gameDeckService.parseCards(any(GameDeck.class), any(String.class), any(String.class), eq(Collections.emptyList()), eq(Collections.emptyList()))).thenReturn(mockCards);
//        String jsonRes = "{\"success\":true}";
//        HttpResponse<String> mockResponse = mock(HttpResponse.class);
//        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
//                .thenReturn(mockResponse);
//        when(mockResponse.body()).thenReturn(jsonRes);
//        when(gameDeckService.fetchDeck(any(Game.class), eq(false))).thenReturn(new GameDeck());
//
//        game.setCurrentTurn(mockUser);
//
//        // Create a new Deck
//        GameDeck deck = gameDeckService.fetchDeck(game, false);
//        deck.setRemainingCardsDealerStack(5);
//        deck.setRemainingCardsDealerStack(100);
//        deck.setDeckID("player1");
//        deck.setGame(game);
//        // draw cards
//        //List<Card> savedCards = gameDeckService.drawCardsFromDealerPile(deck, N);
//
//
//        if (N > deck.getRemainingCardsDealerStack()) {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Number of cards to be drawn exceeds available cards");
//        }
//
//        String drawCardsFromPileUri = String.format("https://www.deckofcardsapi.com/api/deck/%s/pile/%s/draw/?count=%s", deck.getDeckID(), deck.getDealerPileId(), N);
//
//        HttpRequest drawCardsFromPileRequest = HttpRequest.newBuilder()
//                .GET()
//                .uri(URI.create(drawCardsFromPileUri))
//                .build();
//
//        HttpResponse<String> drawCardsFromPileResponse = httpClient.send(drawCardsFromPileRequest, HttpResponse.BodyHandlers.ofString());
//
//        List<Card> cards = gameDeckService.parseCards(deck, drawCardsFromPileResponse.body(), "deckId", new ArrayList<>(), new ArrayList<>());
//
//        List<Card> savedCards = gameDeckService.saveCards(cards);
//
//        String playerName = deck.getGame().getCurrentTurn().getUsername();
//
//
//
//        // assert
//        //verify(gameDeckService).parseCards(any(String.class), any(GameDeck.class));
//        assertNotNull(savedCards);
//        assertEquals(savedCards.size(), N);
//        assertNotNull(playerName);
//    }
//    */
//
//    @Test
//    public void testShuffleCardsInDealerPileTest_Success() throws IOException, InterruptedException {
//        HttpResponse<String> mockResponse = mock(HttpResponse.class);
//        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
//                .thenReturn(mockResponse);
//
//        testDeck.setRemainingCardsDealerStack(20);
//        game.setCurrentTurn(mockUser);
//        testDeck.setGame(game);
//
//        gameDeckService.shuffleCardsInDealerPile(testDeck);
//
//        verify(httpClient).send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class));
//    }
//
//    @Test
//    public void testCreatePlayerPile_success() throws IOException, InterruptedException {
//        HttpResponse<String> mockResponse = mock(HttpResponse.class);
//        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
//                .thenReturn(mockResponse);
//
//        gameDeckService.createPlayerPile(testDeck, mockUser.getId(), "AS");
//
//        verify(httpClient).send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class));
//    }
///*
//    @Test
//    public void testCreateDealerPile_success() throws IOException, InterruptedException {
//        List<Card> mockCards = new ArrayList<>();
//        Card card1 = new Card();
//        card1.setCode("AS");
//        mockCards.add(card1);
//
//        String jsonRes = "{\"deck_id\":\"testId\", \"remaining\": 54}";
//        HttpResponse<String> mockResponse = mock(HttpResponse.class);
//        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
//                .thenReturn(mockResponse);
//        when(mockResponse.body()).thenReturn(jsonRes);
//        when(gameDeckRepository.saveAndFlush(any(GameDeck.class))).thenAnswer(invocation -> invocation.getArgument(0));
//        when(gameDeckService.drawCardsFromDeck(any(GameDeck.class))).thenReturn(mockCards);
//
//        game.setGameDeck(testDeck);
//        testDeck.setGame(game);
//        //String deckId = testDeck.getDeckID();
//
//        assertNotNull(game.getGameDeck());
//
//
//        gameDeckService.createDealerPile(game);
//
//        verify(httpClient).send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class));
//    }
//
// */
///*
//    @Test
//    public void testPeekIntoDealerPile_success() throws IOException, InterruptedException {
//        List<Card> mockCards = new ArrayList<>();
//        Card card1 = new Card();
//        card1.setCode("AS");
//        mockCards.add(card1);
//        Card card2 = new Card();
//        card2.setCode("AC");
//        mockCards.add(card2);
//        Card card3 = new Card();
//        card3.setCode("AD");
//        mockCards.add(card3);
//        Game mockGame = mock(Game.class);
//        GameDeck mockDeck = new GameDeck();
//        mockDeck.setDeckID("mockId");
//
//        String jsonRes = "{\"deck_id\":\"testId\", \"remaining\": 54}";
//        HttpResponse<String> mockResponse = mock(HttpResponse.class);
//        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
//                .thenReturn(mockResponse);
//        when(mockResponse.body()).thenReturn(jsonRes);
//        when(gameDeckRepository.saveAndFlush(any(GameDeck.class))).thenAnswer(invocation -> invocation.getArgument(0));
//        when(mockGame.getGameDeck()).thenReturn(mockDeck);
//        when(gameDeckService.drawCardsFromDealerPile(any(GameDeck.class), eq(3))).thenReturn(mockCards);
//
//
//        gameDeckService.drawCardsFromDealerPile(mockGame.getGameDeck(), 3);
//
//        //List<Card> topCards = gameDeckService.peekIntoDealerPile(game);
//        //assertNotNull(testDeck);
//        //List<Card> topThreeCards = gameDeckService.drawCardsFromDealerPile(testDeck, 3);
//
//        //assertNotNull(topThreeCards);
//        //assertEquals(topCards.size(), 3);
//    }
//
// */
//
//    @Test
//    public void testReturnCardsToPile_success() throws IOException, InterruptedException {
//        HttpResponse<String> mockResponse = mock(HttpResponse.class);
//        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
//                .thenReturn(mockResponse);
//
//        gameDeckService.returnCardsToPile(testDeck, mockUser.getUsername(), "AS");
//
//        verify(httpClient).send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class));
//    }
//    @Test
//    public void testRemoveCardsFromPlayerPile_success() throws IOException, InterruptedException {
//        String jsonRes = "{\"success\":\"true\", \"deck_id\":\"testId\", \"remaining\": 54}";
//        HttpResponse<String> mockResponse = mock(HttpResponse.class);
//        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
//                .thenReturn(mockResponse);
//        when(mockResponse.body()).thenReturn(jsonRes);
//
//        gameDeckService.removeCardsFromPlayerPile(game, mockUser.getId(), "AS");
//
//        verify(httpClient).send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class));
//    }
//
//    @Test
//    public void testPlaceCardsToPlayPile_success() throws IOException, InterruptedException {
//        List<Card> mockCards = new ArrayList<>();
//        Card card1 = new Card();
//        card1.setCode("AS");
//        mockCards.add(card1);
//        Card card2 = new Card();
//        card2.setCode("AC");
//        mockCards.add(card2);
//        Card card3 = new Card();
//        card3.setCode("AD");
//        mockCards.add(card3);
//
//        HttpResponse<String> mockResponse = mock(HttpResponse.class);
//        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
//                .thenReturn(mockResponse);
//        when(userService.getUserById(any(Long.class))).thenReturn(mockUser);
//
//        gameDeckService.placeCardsToPlayPile(game, mockUser.getId(), mockCards, "AS,AC,AD");
//
//        verify(httpClient).send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class));
//        verify(userService).getUserById(any(Long.class));
//    }
//
//    /*
//    @Test
//    public void testRemoveSpecificCardsFromDealerPile_success() throws IOException, InterruptedException {
//        List<Card> mockCards = new ArrayList<>();
//        Card card1 = new Card();
//        card1.setCode("AS");
//        mockCards.add(card1);
//        Card card2 = new Card();
//        card2.setCode("AC");
//        mockCards.add(card2);
//        Card card3 = new Card();
//        card3.setCode("AD");
//        mockCards.add(card3);
//
//        String jsonRes = "{\"deck_id\":\"testId\", \"remaining\": 54}";
//        HttpResponse<String> mockResponse = mock(HttpResponse.class);
//        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
//                .thenReturn(mockResponse);
//        when(mockResponse.body()).thenReturn(jsonRes);
//        when(userService.getUserById(any(Long.class))).thenReturn(mockUser);
//        when(gameDeckService.parseCards(any(GameDeck.class), any(String.class), any(String.class), any(List.class), any(List.class))).thenReturn(mockCards);
//
//        gameDeckService.removeSpecificCardsFromDealerPile(testDeck, "AS");
//
//    }
//
//     */
//
//    @Test
//    public void testGetRemainingPileStats_success() throws IOException, InterruptedException {
//        String jsonRes = "{\"deck_id\":\"testId\", \"remaining\": 54}";
//        HttpResponse<String> mockResponse = mock(HttpResponse.class);
//        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
//                .thenReturn(mockResponse);
//        when(mockResponse.body()).thenReturn(jsonRes);
//
//        String response = gameDeckService.getRemainingPileStats(testDeck, mockUser.getId());
//
//        assertNotNull(response);
//        verify(httpClient).send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class));
//    }
//
//    @Test
//    public void testGetRemainingDealerPileStats_success() throws IOException, InterruptedException {
//        String jsonRes = "{\"deck_id\":\"testId\", \"remaining\": 54}";
//        HttpResponse<String> mockResponse = mock(HttpResponse.class);
//        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
//                .thenReturn(mockResponse);
//        when(mockResponse.body()).thenReturn(jsonRes);
//
//        String response = gameDeckService.getRemainingDealerPileStats(testDeck, mockUser.getUsername());
//
//        assertNotNull(response);
//        verify(httpClient).send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class));
//    }
//
//    @Test
//    public void testParsePileCardCounts_success() throws IOException {
//        String jsonRes = "{\"success\":\"true\", \"deck_id\":\"testId\", \"remaining\": 54, \"piles\": {\"discard\":{\"remaining\":2}}}";
//
//        Map<String, Integer> response = gameDeckService.parsePileCardCounts(jsonRes);
//
//        assertNotNull(response);
//    }
//    /*
//    @Test
//    public void testParseCards_success() throws IOException {
//        List<Card> mockCards = new ArrayList<>();
//        Card card1 = new Card();
//        card1.setCode("AS");
//        card1.setSuit("S");
//        card1.setImage("test");
//        mockCards.add(card1);
//        Card card2 = new Card();
//        card2.setCode("AC");
//        card2.setSuit("C");
//        card2.setImage("test");
//        mockCards.add(card2);
//        Card card3 = new Card();
//        card3.setCode("AD");
//        card3.setSuit("D");
//        card3.setImage("test");
//        mockCards.add(card3);
//
//        when(gameDeckRepository.saveAndFlush(any(GameDeck.class))).thenAnswer(invocation -> invocation.getArgument(0));
//        when(gameDeckService.saveCards(any(List.class))).thenAnswer(invocation -> invocation.getArgument(0));
//
//        String jsonRes = "{\"success\":\"true\", \"deck_id\":\"testId\", \"remaining\": 54, \"piles\": {\"discard\":{\"remaining\":2}}}";
//        List<String> cardsPath = Arrays.asList("piles", "play", "cards");
//
//        List<Card> response = gameDeckService.parseCards(testDeck, jsonRes, testDeck.getDeckID(), cardsPath, null);
//
//        assertNotNull(response);
//    }
//
//     */
//}