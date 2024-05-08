package ch.uzh.ifi.hase.soprafs24.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import ch.uzh.ifi.hase.soprafs24.constant.MessageType;
import ch.uzh.ifi.hase.soprafs24.eventlistener.WebSocketEventListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

import java.util.HashMap;
import java.util.Map;

public class ChatControllerTest {

    @InjectMocks
    private ChatController chatController;

    @Mock
    private WebSocketEventListener webSocketEventListener;

    @Mock
    private SimpMessageSendingOperations messagingTemplate;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Tests sending a message to a chatroom.
     */
    @Test
    public void testSendMessage() {
        String roomId = "1";
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setSender("user1");
        chatMessage.setContent("Hello World");
        chatMessage.setType(MessageType.CHAT);

        chatController.sendMessage(roomId, chatMessage);

        verify(messagingTemplate).convertAndSend(eq("/topic/" + roomId), eq(chatMessage));
    }

    /**
     * Tests the behavior when a user switches from one room to another.
     */
    @Test
    public void testAddUser_SwitchRoom() {
        String currentRoomId = "1";
        String newRoomId = "2";
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create();
        ChatMessage joinMessage = new ChatMessage();
        joinMessage.setSender("user3");
        joinMessage.setType(MessageType.JOIN);

        Map<String, Object> sessionAttributes = new HashMap<>();
        sessionAttributes.put("room_id", currentRoomId);
        sessionAttributes.put("username", "user3");
        headerAccessor.setSessionAttributes(sessionAttributes);

        chatController.addUser(newRoomId, joinMessage, headerAccessor);

        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(messagingTemplate, times(2)).convertAndSend(anyString(), messageCaptor.capture());

        ChatMessage leaveMessage = messageCaptor.getAllValues().get(0);
        assertEquals(MessageType.LEAVE, leaveMessage.getType());
        assertEquals("user3", leaveMessage.getSender());

        ChatMessage sentJoinMessage = messageCaptor.getAllValues().get(1);
        assertEquals(MessageType.JOIN, sentJoinMessage.getType());
        assertEquals("user3", sentJoinMessage.getSender());

        verify(webSocketEventListener).addUser("user3", newRoomId);
        verify(webSocketEventListener).broadcastActiveUsers(currentRoomId);
        verify(webSocketEventListener).broadcastActiveUsers(newRoomId);
        assertEquals(newRoomId, headerAccessor.getSessionAttributes().get("room_id"));
    }

    /**
     * Tests adding a user to a new chatroom (user currently not in any room).
     */
    @Test
    public void testAddUser_NewRoom() {
        String roomId = "2";
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create();
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setSender("user2");
        chatMessage.setType(MessageType.JOIN);

        Map<String, Object> sessionAttributes = new HashMap<>();

        headerAccessor.setSessionAttributes(sessionAttributes);

        chatController.addUser(roomId, chatMessage, headerAccessor);

        verify(messagingTemplate).convertAndSend(eq("/topic/" + roomId), eq(chatMessage));
        verify(webSocketEventListener).addUser("user2", roomId);
        verify(webSocketEventListener).broadcastActiveUsers(roomId);
        verifyNoMoreInteractions(messagingTemplate);
    }
}