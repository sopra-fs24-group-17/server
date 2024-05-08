package ch.uzh.ifi.hase.soprafs24.eventlistener;

import ch.uzh.ifi.hase.soprafs24.chat.ChatMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.*;

class WebSocketEventListenerTest {

    @Mock
    private SimpMessageSendingOperations messagingTemplate;

    @InjectMocks
    private WebSocketEventListener webSocketEventListener;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void addUser_shouldAddUserToMapAndBroadcast() {
        webSocketEventListener.addUser("newUser", "newRoom");
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/newRoom"), any(ChatMessage.class));
    }

    @Test
    void removeUser_withExistingUser_shouldRemoveUserAndBroadcast() {
        webSocketEventListener.addUser("existingUser", "existingRoom");
        webSocketEventListener.removeUser("existingUser");
        verify(messagingTemplate, times(2)).convertAndSend(eq("/topic/existingRoom"), any(ChatMessage.class));
    }

    @Test
    void removeUser_withNonExistingUser_shouldNotBroadcast() {
        webSocketEventListener.removeUser("nonExistingUser");
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(ChatMessage.class));
    }

    @Test
    void handleWebSocketDisconnectListener_withValidSessionAttributes_shouldRemoveUserAndBroadcast() {
        // Prepare mock session attributes
        Map<String, Object> sessionAttributes = new HashMap<>();
        sessionAttributes.put("username", "testUser");
        sessionAttributes.put("room_id", "testRoom");

        Message message = mock(Message.class);
        MessageHeaders messageHeaders = new MessageHeaders(new HashMap<>());
        when(message.getHeaders()).thenReturn(messageHeaders);

        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(message);
        headerAccessor.setSessionAttributes(sessionAttributes);

        when(message.getHeaders()).thenReturn(headerAccessor.toMessageHeaders());

        String sessionId = "testSessionId";
        CloseStatus closeStatus = CloseStatus.NORMAL;

        SessionDisconnectEvent event = new SessionDisconnectEvent(this, message, sessionId, closeStatus);

        webSocketEventListener.addUser("testUser", "testRoom");
        webSocketEventListener.handleWebSocketDisconnectListener(event);
        verify(messagingTemplate, times(2)).convertAndSend(eq("/topic/testRoom"), any(ChatMessage.class));
    }
}
