import static org.mockito.Mockito.*;

import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;
import org.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.json.JSONObject;
import ch.uzh.ifi.hase.soprafs24.entity.Card;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

}
