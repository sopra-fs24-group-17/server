import static org.mockito.Mockito.*;

import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

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
        webSocketService.sendMessageJoinedUser(userName, gameId);
        verify(messagingTemplate).convertAndSend("/game/" + gameId, userName);
    }

    @Test
    public void sendMessageFriendLoginTest() {
        String userName = "testUser";
        Long userId = 456L;
        webSocketService.sendMessageFriendLogin(userName, userId);
        verify(messagingTemplate).convertAndSend("/login/" + userId, userName);
    }

    @Test
    public void sendMessageFriendLogoutTest() {
        String userName = "testUser";
        Long userId = 789L;
        webSocketService.sendMessageFriendLogout(userName, userId);
        verify(messagingTemplate).convertAndSend("/logout/" + userId, userName);
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

}