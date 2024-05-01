package ch.uzh.ifi.hase.soprafs24.eventlistener;

import ch.uzh.ifi.hase.soprafs24.constant.MessageType;
import ch.uzh.ifi.hase.soprafs24.chat.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final SimpMessageSendingOperations messageTemplate;

    private static final Set<String> activeUsers = Collections.synchronizedSet(new HashSet<>());

    @EventListener
    public void handleWebSocketDisconnectedListener(
            SessionDisconnectEvent event
    ) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = (String) headerAccessor.getSessionAttributes().get("username");

        if (username != null && activeUsers.remove(username)) {
            logger.info("User disconnected: {}", username);
            broadcastActiveUsers();
            var chatMessage = ChatMessage.builder()
                    .type(MessageType.LEAVE)
                    .sender(username)
                    .build();
            messageTemplate.convertAndSend("/topic/public", chatMessage);
        }
    }

    public void addUser(String username) {
        activeUsers.add(username);
        broadcastActiveUsers();
    }

    public void removeUser(String username) {
        if (activeUsers.remove(username)) {
            broadcastActiveUsers();
        }
    }

    private void broadcastActiveUsers() {
        ChatMessage userUpdateMsg = ChatMessage.builder()
                .sender("Server")
                .type(MessageType.STATE)
                .content(String.join(",", activeUsers))
                .build();
        messageTemplate.convertAndSend("/topic/public", userUpdateMsg);
    }

}
