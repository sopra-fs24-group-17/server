package ch.uzh.ifi.hase.soprafs24.eventlistener;

import java.util.HashSet;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import ch.uzh.ifi.hase.soprafs24.constant.MessageType;
import ch.uzh.ifi.hase.soprafs24.chat.ChatMessage;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Slf4j
@Component
public class WebSocketEventListener {

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    private final Map<String, String> userRoomMap = new ConcurrentHashMap<>();

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = (String) headerAccessor.getSessionAttributes().get("username");
        String roomId = (String) headerAccessor.getSessionAttributes().get("room_id");

        if (username != null && roomId != null) {
            log.info("User Disconnected: {} from room {}", username, roomId);
            userRoomMap.remove(username);
            log.debug("userRoomMap after removal: {}", userRoomMap);
            broadcastActiveUsers(roomId);
        }
    }

    public void addUser(String username, String roomId) {
        userRoomMap.put(username, roomId);
        log.debug("Added user {}, room {} to userRoomMap", username, roomId);
        log.debug("userRoomMap after addition: {}", userRoomMap);
        broadcastActiveUsers(roomId);
    }

    public void removeUser(String username) {
        String roomId = userRoomMap.get(username);
        if (roomId != null && userRoomMap.remove(username) != null) {
            log.debug("Removed user {}, room {} from userRoomMap", username, roomId);
            log.debug("userRoomMap after removal: {}", userRoomMap);
            broadcastActiveUsers(roomId);
        }
    }

    public void broadcastActiveUsers(String roomId) {
        Set<String> activeUsers = new HashSet<>();
        for (Map.Entry<String, String> entry : userRoomMap.entrySet()) {
            if (entry.getValue().equals(roomId)) {
                activeUsers.add(entry.getKey());
            }
        }

        ChatMessage userUpdateMsg = new ChatMessage();
        userUpdateMsg.setSender("Server");
        userUpdateMsg.setType(MessageType.STATE);
        userUpdateMsg.setContent(String.join(",", activeUsers));

        log.info("Broadcasting active users in room {}: {}", roomId, String.join(",", activeUsers));
        messagingTemplate.convertAndSend(String.format("/topic/%s", roomId), userUpdateMsg);
    }
}
