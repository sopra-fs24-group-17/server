package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.eventlistener.GameCreationEventListener;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WebSocketService {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketService.class);

    @Autowired
    protected SimpMessagingTemplate sendMessage;
    public void sendMessageToClients(String destination, Object dto) {
        logger.info("Message Dispatched: (DTO)");
        this.sendMessage.convertAndSend(destination, dto);
    }

    public void sendMessageToClients(String destination, String message) {
        logger.info("Message Dispatched: " + message);
        this.sendMessage.convertAndSend(destination, message);
    }

    public void sendMessageJoinedUser(String userName, Long gameId) {
        logger.info("Join Message Dispatched: User: {} joined Game: {}", userName, gameId);

        JSONObject message = new JSONObject();
        message.put("type", "join");
        message.put("userName", userName);
        message.put("gameId", gameId);

        this.sendMessage.convertAndSend("/game/" + gameId, message.toString());
    }


    public void sendMessageLeftUser(String userName, Long gameId) {
        logger.info("Leave Message Dispatched: User: {} left Game: {}", userName, gameId);

        JSONObject message = new JSONObject();
        message.put("type", "leave");
        message.put("userName", userName);
        message.put("gameId", gameId);

        this.sendMessage.convertAndSend("/game/" + gameId, message.toString());
    }



    public void sendMessageFriendLogin(String userName, Long userId) {
        logger.info("Friend Login Message Dispatched: User: {} logged in", userName);
        this.sendMessage.convertAndSend("/login", userName);
    }

    public void sendMessageFriendLogout(String userName, Long userId) {
        logger.info("Friend Logout Message Dispatched: User: {} logged out", userName);
        this.sendMessage.convertAndSend("/logout", userName);
    }

    public void sendMessageFriendshipRequestAccepted(String userName, Long userId) {
        logger.info("Friend Request Acceptance Message Dispatched: User: {}", userName);
        this.sendMessage.convertAndSend("/friendshiprequest/acceptance/" + userId, userName);
    }

    public void sendMessageFriendshipRequestReceived(String userName, Long userId) {
        logger.info("Friend Request Received Message Dispatched: User: {}", userName);
        this.sendMessage.convertAndSend("/friendshiprequest/received/" + userId, userName);
    }

    public void sendMessageGameCreated(Long gameId) {
        logger.info("Game Creation Message Sent: Game: {}", gameId);
        this.sendMessage.convertAndSend("/game/new", gameId);
    }
}