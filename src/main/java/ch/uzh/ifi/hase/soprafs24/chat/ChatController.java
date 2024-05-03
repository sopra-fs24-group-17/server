package ch.uzh.ifi.hase.soprafs24.chat;

import ch.uzh.ifi.hase.soprafs24.constant.MessageType;
import ch.uzh.ifi.hase.soprafs24.eventlistener.WebSocketEventListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

@Slf4j
@Controller
public class ChatController {

    @Autowired
    private WebSocketEventListener webSocketEventListener;

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    /**
     * Websocket endpoint to send real-time messages within a particular chatroom
     * @param roomId of the chatroom (given by the gameId)
     * @param chatMessage content of the message to be forwarded
     */
    @MessageMapping("/chat/{roomId}/sendMessage")
    public void sendMessage(@DestinationVariable String roomId,
                            @Payload ChatMessage chatMessage) {
        chatMessage.setType(MessageType.CHAT);
        log.info("Sending message to room {}: {}", roomId, chatMessage.getContent());
        messagingTemplate.convertAndSend(String.format("/topic/%s", roomId), chatMessage);
    }

    /**
     * Websocket endpoint to add users to a particular chatroom
     * @param roomId of the chatroom (given by the gameId)
     * @param chatMessage containing the join meessage
     * @param headerAccessor containing room_id and username of the user who wants to join the chatroom
     */
    @MessageMapping("/chat/{roomId}/addUser")
    public void addUser(@DestinationVariable String roomId,
                        @Payload ChatMessage chatMessage,
                        SimpMessageHeaderAccessor headerAccessor) {
        log.info("Added user {} to chatroom", chatMessage.getSender());
        String currentRoomId = (String) headerAccessor.getSessionAttributes().put("room_id", roomId);
        headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
        webSocketEventListener.addUser(chatMessage.getSender(), roomId);

        if (currentRoomId != null && !currentRoomId.equals(roomId)) {
            ChatMessage leaveMessage = new ChatMessage();
            leaveMessage.setType(MessageType.LEAVE);
            leaveMessage.setSender(chatMessage.getSender());
            log.info("User {} left room {}", chatMessage.getSender(), currentRoomId);
            messagingTemplate.convertAndSend(String.format("/topic/%s", currentRoomId), leaveMessage);
            webSocketEventListener.broadcastActiveUsers(currentRoomId);
        }

        chatMessage.setType(MessageType.JOIN);
        log.info("User {} joined room {}", chatMessage.getSender(), roomId);
        messagingTemplate.convertAndSend(String.format("/topic/%s", roomId), chatMessage);
        webSocketEventListener.broadcastActiveUsers(roomId);
    }
}