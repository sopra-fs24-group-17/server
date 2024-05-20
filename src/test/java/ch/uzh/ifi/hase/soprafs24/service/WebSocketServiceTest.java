import static org.mockito.Mockito.*;

import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;
import org.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.json.JSONObject;
import ch.uzh.ifi.hase.soprafs24.entity.Card;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

@ActiveProfiles("dev")
public class WebSocketServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private WebSocketService webSocketService;

    @BeforeEach
    public void setup() {
        messagingTemplate = mock(SimpMessagingTemplate.class);
        webSocketService = new WebSocketService();
        ReflectionTestUtils.setField(webSocketService, "sendMessage", messagingTemplate);
    }

    @Test
    public void sendMessageToClientsTest() {
        String destination = "/test/destination";
        Object dto = new Object();
        webSocketService.sendMessageToClients(destination, dto);
        verify(messagingTemplate).convertAndSend(destination, dto);
    }

    @Test
    public void sendMessageFriendLoginTest() {
        String userName = "testUser";
        Long userId = 456L;
        webSocketService.sendMessageFriendLogin(userName, userId);
        verify(messagingTemplate).convertAndSend("/login" , userName);
    }

    @Test
    public void sendMessageFriendLogoutTest() {
        String userName = "testUser";
        Long userId = 789L;
        webSocketService.sendMessageFriendLogout(userName, userId);
        verify(messagingTemplate).convertAndSend("/logout", userName);
    }

    @Test
    public void sendMessageFriendshipRequestAcceptedTest() {
        String userName = "testUser";
        Long userId = 101112L;
        webSocketService.sendMessageFriendshipRequestAccepted(userName, userId);
        verify(messagingTemplate).convertAndSend("/friendshiprequest/acceptance/" + userId, userName);
    }

    @Test
    public void sendMessageFriendshipRequestReceivedTest() {
        String userName = "testUser";
        Long userId = 131415L;
        webSocketService.sendMessageFriendshipRequestReceived(userName, userId);
        verify(messagingTemplate).convertAndSend("/friendshiprequest/received/" + userId, userName);
    }

    @Test
    public void sendMessageToClients_withStringMessageTest() {
        String destination = "/test/destination";
        String message = "Hello, world!";
        webSocketService.sendMessageToClients(destination, message);
        verify(messagingTemplate).convertAndSend(destination, message);
    }

    @Test
    public void sendMessageGameCreatedTest() {
        Long gameId = 1234L;
        webSocketService.sendMessageGameCreated(gameId);
        verify(messagingTemplate).convertAndSend("/game/new", gameId);
    }

    @Test
    public void testSendMessageCardsDrawn() {
        Long gameId = 1L;
        String userName = "user";
        Integer numberOfCards = 5;
        JSONObject message = new JSONObject();
        message.put("type", "drawing");
        message.put("gameId", gameId);
        message.put("user", userName);
        message.put("numberOfCards", numberOfCards);
        webSocketService.sendMessageCardsDrawn(gameId, userName, numberOfCards);
        verify(messagingTemplate).convertAndSend("/game/" + gameId, message.toString());
    }

    @Test
    public void testSendMessageShuffling() {
        Long gameId = 1L;
        String userName = "user";
        JSONObject message = new JSONObject();
        message.put("type", "shuffling");
        message.put("gameId", gameId);
        message.put("user", userName);
        webSocketService.sendMessageShuffling(gameId, userName);
        verify(messagingTemplate).convertAndSend("/game/" + gameId, message.toString());
    }

    @Test
    public void testSendMessagePeekIntoDeck() {
        Long gameId = 1L;
        String userName = "user";
        Long userId = 2L;
        List<Card> futureCards = new ArrayList<>();

        Card card1 = new Card();
        card1.setCode("AH");
        card1.setInternalCode("explosion");

        Card card2 = new Card();
        card2.setCode("KH");
        card2.setInternalCode("defuse");

        futureCards.add(card1);
        futureCards.add(card2);

        JSONArray cardsArray = new JSONArray();
        for (Card card : futureCards) {
            JSONObject cardJson = new JSONObject();
            cardJson.put("code", card.getCode());
            cardJson.put("internalCode", card.getInternalCode());
            cardsArray.put(cardJson);
        }
        JSONObject message = new JSONObject();
        message.put("type", "peekIntoDeck");
        message.put("gameId", gameId);
        message.put("user", userName);
        message.put("cards", cardsArray);
        webSocketService.sendMessagePeekIntoDeck(gameId, userName, userId, futureCards);
        verify(messagingTemplate).convertAndSend("/game/" + gameId + "/" + userId, message.toString());
    }

    @Test
    public void testSendMessageExplosionReturnedToDeck() {
        Long gameId = 1L;
        String userName = "user";
        JSONObject message = new JSONObject();
        message.put("type", "placedBackToDeck");
        message.put("gameId", gameId);
        message.put("user", userName);
        webSocketService.sendMessageExplosionReturnedToDeck(gameId, userName);
        verify(messagingTemplate).convertAndSend("/game/" + gameId, message.toString());
    }

    @Test
    public void testSendMessageGameStarted() {
        Long gameId = 1L;
        Long userId = 2L;
        JSONObject message = new JSONObject();
        message.put("type", "start");
        message.put("gameId", gameId);
        webSocketService.sendMessageGameStarted(gameId, userId);
        verify(messagingTemplate).convertAndSend("/game/" + gameId + "/" + userId, message.toString());
    }

    @Test
    public void sendMessageJoinedUserTest() {
        String userName = "testUser";
        Long gameId = 123L;
        Integer maxPlayers = 4;
        Integer currentPlayers = 2;
        webSocketService.sendMessageJoinedUser(userName, gameId, maxPlayers, currentPlayers);
        verify(messagingTemplate).convertAndSend(eq("/game/" + gameId), any(String.class));
    }

    @Test
    public void sendMessageLeftUserTest() {
        String userName = "testUser";
        Long gameId = 123L;
        Integer maxPlayers = 4;
        Integer currentPlayers = 2;
        webSocketService.sendMessageLeftUser(userName, gameId, maxPlayers, currentPlayers);
        verify(messagingTemplate).convertAndSend(eq("/game/" + gameId), any(String.class));
    }

    @Test
    public void sendMessageStolenCardTest() {
        Long gameId = 1L;
        Long userId = 2L;
        List<Card> stolenCards = new ArrayList<>();
        Card card = new Card();
        card.setCode("AH");
        card.setInternalCode("explosion");
        stolenCards.add(card);

        webSocketService.sendMessageStolenCard(gameId, userId, stolenCards);
        verify(messagingTemplate).convertAndSend(eq("/game/" + gameId + "/" + userId), any(String.class));
    }

    @Test
    public void sendMessageDefuseCardPlayedTest() {
        Long gameId = 1L;
        Long userId = 2L;
        List<Card> defuseCards = new ArrayList<>();
        Card card = new Card();
        card.setCode("KH");
        card.setInternalCode("defuse");
        defuseCards.add(card);

        webSocketService.sendMessageDefuseCardPlayed(gameId, userId, defuseCards);
        verify(messagingTemplate).convertAndSend(eq("/game/" + gameId + "/" + userId), any(String.class));
    }

    @Test
    public void sendMessageCardPlayedTest() {
        Long gameId = 1L;
        String userName = "user";
        String internalCode = "KH";
        String externalCode = "AH";

        webSocketService.sendMessageCardPlayed(gameId, userName, internalCode, externalCode);
        verify(messagingTemplate).convertAndSend(eq("/game/" + gameId), any(String.class));
    }

    @Test
    public void sendMessageExplosionTest() {
        Long gameId = 1L;
        String userName = "user";

        webSocketService.sendMessageExplosion(gameId, userName);
        verify(messagingTemplate).convertAndSend(eq("/game/" + gameId), any(String.class));
    }

    @Test
    public void sendMessageExplosionIndividualTest() {
        Long gameId = 1L;
        Long userId = 2L;

        webSocketService.sendMessageExplosionIndividual(gameId, userId);
        verify(messagingTemplate).convertAndSend(eq("/game/" + gameId + "/" + userId), any(String.class));
    }

    @Test
    public void sendMessageYourTurnTest() {
        Long userId = 1L;
        Long gameId = 100L;
        String userName = "test";
        webSocketService.sendMessageYourTurn(userId, gameId, userName);
        verify(messagingTemplate).convertAndSend(eq("/game/" + gameId + "/" + userId), any(String.class));
    }

    @Test
    public void setSendMessageEndTurnTest() {
        Long gameId = 100L;
        String userName = "user";
        Long userId = 1L;
        webSocketService.setSendMessageEndTurn(userId, gameId, userName);
        verify(messagingTemplate).convertAndSend(eq("/game/" + gameId + "/" + userId), any(String.class));
    }

    @Test
    public void sendMessageEndGameTest() {
        Long gameId = 100L;
        String userName = "winner";
        List<String> leaderboard = new ArrayList<>();
        leaderboard.add(userName);
        webSocketService.sendMessageEndGame(gameId, userName, leaderboard);
        verify(messagingTemplate).convertAndSend(eq("/game/" + gameId), any(String.class));
    }

    @Test
    public void sendMessagePlayerCardsTest() {
        Long gameId = 1L;
        Long userId = 2L;
        List<Card> playerCards = new ArrayList<>();
        Card card1 = new Card();
        card1.setCode("AH");
        card1.setInternalCode("explosion");
        playerCards.add(card1);

        Card card2 = new Card();
        card2.setCode("KH");
        card2.setInternalCode("defuse");
        playerCards.add(card2);

        webSocketService.sendMessagePlayerCards(gameId, userId, playerCards);

        verify(messagingTemplate).convertAndSend(eq("/game/" + gameId + "/" + userId), any(String.class));
    }

    @Test
    public void lossEventTest() {
        Long gameId = 100L;
        String userName = "testUser";
        webSocketService.lossEvent(gameId, userName);
        verify(messagingTemplate).convertAndSend(eq("/game/" + gameId), any(String.class));
    }

    @Test
    public void sendGameStateTest() {
        Long gameId = 100L;
        Card topCard = new Card();
        topCard.setCode("4H");
        topCard.setInternalCode("high card");

        Map<String, Integer> remainingCardStats = Map.of("totalCards", 40, "explosiveCards", 5);

        Integer numberOfPlayers = 5;

        List<String> playerNames = new ArrayList<>();
        playerNames.add("karl");
        playerNames.add("heinz");
        playerNames.add("peter");
        playerNames.add("heidi");
        playerNames.add("thomas");

        List<Long> playerIds = new ArrayList<>();
        playerIds.add(1L);
        playerIds.add(2L);
        playerIds.add(3L);
        playerIds.add(4L);
        playerIds.add(5L);

        List<String> playerAvatars = new ArrayList<>();
        playerAvatars.add("avatar1");
        playerAvatars.add("avatar2");
        playerAvatars.add("avatar3");
        playerAvatars.add("avatar4");
        playerAvatars.add("avatar5");


        webSocketService.sendGameState(gameId, topCard, remainingCardStats, numberOfPlayers, playerNames, playerIds, playerAvatars, "heinz");
        verify(messagingTemplate).convertAndSend(eq("/game/" + gameId), any(String.class));
    }

    @Test
    public void testSendPlacementRequest() {
        Long gameId = 1L;
        Long userId = 2L;

        webSocketService.sendPlacementRequest(gameId, userId);

        JSONObject expectedMessage = new JSONObject();
        expectedMessage.put("type", "placementRequest");
        expectedMessage.put("userId", userId);

        verify(messagingTemplate).convertAndSend(eq("/game/" + gameId + "/" + userId), eq(expectedMessage.toString()));
    }

    @Test
    public void testSendMessageGetLucky() {
        Long gameId = 1L;
        Long userId = 2L;
        Card randomCard = new Card();
        randomCard.setCode("KC");
        randomCard.setInternalCode("defuse");

        webSocketService.sendMessageGetLucky(gameId, userId, randomCard);

        JSONObject cardJson = new JSONObject();
        cardJson.put("code", randomCard.getCode());
        cardJson.put("internalCode", randomCard.getInternalCode());

        JSONArray cardsArray = new JSONArray();
        cardsArray.put(cardJson);

        JSONObject expectedMessage = new JSONObject();
        expectedMessage.put("type", "cards");
        expectedMessage.put("gameId", gameId);
        expectedMessage.put("user", userId);
        expectedMessage.put("cards", cardsArray);

        verify(messagingTemplate).convertAndSend(eq("/game/" + gameId + "/" + userId), eq(expectedMessage.toString()));
    }
}
