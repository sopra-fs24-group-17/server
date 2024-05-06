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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
    public void sendMessageJoinedUserTest() {
        String userName = "testUser";
        Long gameId = 123L;

        JSONObject expectedMessage = new JSONObject();
        expectedMessage.put("type", "join");
        expectedMessage.put("userName", userName);
        expectedMessage.put("gameId", gameId);
        String expectedJson = expectedMessage.toString();

        webSocketService.sendMessageJoinedUser(userName, gameId);
        verify(messagingTemplate).convertAndSend("/game/" + gameId, expectedJson);
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
    public void sendMessageLeftUserTest() {
        String userName = "testUser";
        Long gameId = 234L;

        JSONObject expectedMessage = new JSONObject();
        expectedMessage.put("type", "leave");
        expectedMessage.put("userName", userName);
        expectedMessage.put("gameId", gameId);
        String expectedJson = expectedMessage.toString();

        webSocketService.sendMessageLeftUser(userName, gameId);
        verify(messagingTemplate).convertAndSend("/game/" + gameId, expectedJson);
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
    public void testSendMessageYourTurn() {
        Long gameId = 1L;
        Long userId = 2L;

        JSONObject message = new JSONObject();
        message.put("type", "startTurn");
        message.put("userId", userId);

        webSocketService.sendMessageYourTurn(userId, gameId);
        verify(messagingTemplate).convertAndSend("/game/" + gameId + "/" + userId, message.toString());
    }

    @Test
    public void testSetSendMessageEndTurn() {
        Long gameId = 1L;
        String userName = "user";

        JSONObject message = new JSONObject();
        message.put("type", "endTurn");
        message.put("terminatingUser", userName);

        webSocketService.setSendMessageEndTurn(gameId, userName);
        verify(messagingTemplate).convertAndSend("/game/" + gameId, message.toString());
    }

    @Test
    public void testSendMessageEndGame() {
        Long gameId = 1L;
        String userName = "winner";

        JSONObject message = new JSONObject();
        message.put("type", "endGame");
        message.put("winningUser", userName);

        webSocketService.sendMessageEndGame(gameId, userName);
        verify(messagingTemplate).convertAndSend("/game/" + gameId, message.toString());
    }

    @Test
    public void testSendMessagePlayerCards() {
        Long gameId = 1L;
        Long userId = 2L;
        List<Card> playerCards = new ArrayList<>();

        Card card1 = new Card();
        card1.setInternalCode("explosion");
        card1.setCode("AH");

        Card card2 = new Card();
        card2.setInternalCode("defuse");
        card2.setCode("KH");

        playerCards.add(card1);
        playerCards.add(card2);

        JSONArray cardsArray = new JSONArray();
        for (Card card : playerCards) {
            JSONObject cardJson = new JSONObject();
            cardJson.put("code", card.getCode());
            cardJson.put("internalCode", card.getInternalCode());
            cardsArray.put(cardJson);
        }

        JSONObject message = new JSONObject();
        message.put("type", "cards");
        message.put("cards", cardsArray);

        webSocketService.sendMessagePlayerCards(gameId, userId, playerCards);
        verify(messagingTemplate).convertAndSend("/game/" + gameId + "/" + userId, message.toString());
    }

    @Test
    public void testSendMessageCardPlayed() {
        Long gameId = 1L;
        String userName = "user";
        String internalCode = "explosion";

        JSONObject message = new JSONObject();
        message.put("type", "cardPlayed");
        message.put("userName", userName);
        message.put("cardPlayed", internalCode);

        webSocketService.sendMessageCardPlayed(gameId, userName, internalCode);
        verify(messagingTemplate).convertAndSend("/game/" + gameId, message.toString());
    }

    @Test
    public void testSendMessageStolenCard() {
        Long gameId = 1L;
        Long userId = 2L;
        List<Card> stolenCards = new ArrayList<>();

        Card card1 = new Card();
        card1.setInternalCode("explosion");
        card1.setCode("AH");

        Card card2 = new Card();
        card2.setInternalCode("defuse");
        card2.setCode("KH");

        stolenCards.add(card1);
        stolenCards.add(card2);

        JSONArray cardsArray = new JSONArray();
        for (Card card : stolenCards) {
            JSONObject cardJson = new JSONObject();
            cardJson.put("code", card.getCode());
            cardJson.put("internalCode", card.getInternalCode());
            cardsArray.put(cardJson);
        }

        JSONObject message = new JSONObject();
        message.put("type", "cardStolen");
        message.put("cards", cardsArray);

        webSocketService.sendMessageStolenCard(gameId, userId, stolenCards);
        verify(messagingTemplate).convertAndSend("/game/" + gameId + "/" + userId, message.toString());
    }
    @Test
    public void testSendMessageDefuseCardPlayed() {
        Long gameId = 1L;
        Long userId = 2L;

        List<Card> defuseCards = new ArrayList<>();

        Card card1 = new Card();
        card1.setInternalCode("explosion");
        card1.setCode("AH");

        Card card2 = new Card();
        card2.setInternalCode("defuse");
        card2.setCode("KH");

        defuseCards.add(card1);
        defuseCards.add(card2);

        JSONArray cardsArray = new JSONArray();
        for (Card card : defuseCards) {
            JSONObject cardJson = new JSONObject();
            cardJson.put("code", card.getCode());
            cardJson.put("internalCode", card.getInternalCode());
            cardsArray.put(cardJson);
        }

        JSONObject message = new JSONObject();
        message.put("type", "defuseCard");
        message.put("cards", cardsArray);

        webSocketService.sendMessageDefuseCardPlayed(gameId, userId, defuseCards);
        verify(messagingTemplate).convertAndSend("/game/" + gameId + "/" + userId, message.toString());
    }

    @Test
    public void testSendMessageExplosion() {
        Long gameId = 1L;
        String userName = "user";

        JSONObject message = new JSONObject();
        message.put("type", "explosion");
        message.put("terminatingUser", userName);

        webSocketService.sendMessageExplosion(gameId, userName);
        verify(messagingTemplate).convertAndSend("/game/" + gameId, message.toString());
    }

    @Test
    public void testSendMessageExplosionIndividual() {
        Long gameId = 1L;
        Long userId = 2L;

        JSONObject message = new JSONObject();
        message.put("type", "explosion");

        webSocketService.sendMessageExplosionIndividual(gameId, userId);
        verify(messagingTemplate).convertAndSend("/game/" + gameId + "/" + userId, message.toString());
    }

    @Test
    public void testLossEvent() {
        Long gameId = 1L;
        String userName = "user";

        JSONObject message = new JSONObject();
        message.put("type", "loss");
        message.put("looserUser", userName);

        webSocketService.lossEvent(gameId, userName);
        verify(messagingTemplate).convertAndSend("/game/" + gameId, message.toString());
    }

    @Test
    public void testSendGameState() {
        Long gameId = 1L;
        Card topCard = new Card();
        topCard.setCode("AH");
        topCard.setInternalCode("defuse");
        Map<String, Integer> remainingCardStats = Map.of("1", 5, "Play", 2);

        JSONObject message = new JSONObject();
        message.put("type", "gameState");
        message.put("topCardCode", topCard.getCode());
        message.put("topCardInternalCode", topCard.getInternalCode());

        JSONObject pilesJson = new JSONObject();
        remainingCardStats.forEach(pilesJson::put);
        message.put("piles", pilesJson);

        webSocketService.sendGameState(gameId, topCard, remainingCardStats);
        verify(messagingTemplate).convertAndSend("/game/" + gameId, message.toString());
    }

}
