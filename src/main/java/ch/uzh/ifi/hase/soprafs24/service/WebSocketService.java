package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.eventlistener.GameCreationEventListener;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WebSocketService {

    @Autowired
    protected SimpMessagingTemplate sendMessage;

    public void sendMessageToClients(String destination, Object dto) {
        this.sendMessage.convertAndSend(destination, dto);
    }

    public void sendMessageToClients(String destination, String message) {
        this.sendMessage.convertAndSend(destination, message);
    }

    public void sendMessageJoinedUser(String userName, Long gameId) {

        JSONObject message = new JSONObject();
        message.put("type", "join");
        message.put("userName", userName);
        message.put("gameId", gameId);

        this.sendMessage.convertAndSend("/game/" + gameId, message.toString());
    }

    public void sendMessageLeftUser(String userName, Long gameId) {

        JSONObject message = new JSONObject();
        message.put("type", "leave");
        message.put("userName", userName);
        message.put("gameId", gameId);

        this.sendMessage.convertAndSend("/game/" + gameId, message.toString());
    }

    public void sendMessageFriendLogin(String userName, Long userId) {
        this.sendMessage.convertAndSend("/login", userName);
    }

    public void sendMessageFriendLogout(String userName, Long userId) {
        this.sendMessage.convertAndSend("/logout", userName);
    }

    public void sendMessageFriendshipRequestAccepted(String userName, Long userId) {
        this.sendMessage.convertAndSend("/friendshiprequest/acceptance/" + userId, userName);
    }

    public void sendMessageFriendshipRequestReceived(String userName, Long userId) {
        this.sendMessage.convertAndSend("/friendshiprequest/received/" + userId, userName);
    }

    public void sendMessageCardsDrawn(Long gameId, String invokingPlayerUserName, Integer numberOfCards) {
        JSONObject message = new JSONObject();
        message.put("type", "drawing");
        message.put("gameId", gameId);
        message.put("user", invokingPlayerUserName);
        message.put("numberOfCards", numberOfCards);

        this.sendMessage.convertAndSend("/game/" + gameId, message.toString());
    }

    public void sendMessageShuffling(Long gameId, String invokingPlayerUserName) {
        JSONObject message = new JSONObject();
        message.put("type", "shuffling");
        message.put("gameId", gameId);
        message.put("user", invokingPlayerUserName);

        this.sendMessage.convertAndSend("/game/" + gameId, message.toString());
    }

    public void sendMessagePeekIntoDeck(Long gameId, String invokingPlayerUserName) {
        JSONObject message = new JSONObject();
        message.put("type", "peekIntoDeck");
        message.put("gameId", gameId);
        message.put("user", invokingPlayerUserName);

        this.sendMessage.convertAndSend("/game/" + gameId, message.toString());
    }

    public void sendMessageExplosionReturnedToDeck(Long gameId, String invokingPlayerUserName) {
        JSONObject message = new JSONObject();
        message.put("type", "placedBackToDeck");
        message.put("gameId", gameId);
        message.put("user", invokingPlayerUserName);

        this.sendMessage.convertAndSend("/game/" + gameId, message.toString());
    }

    public void sendMessageGameCreated(Long gameId) {
        this.sendMessage.convertAndSend("/game/new", gameId);
    }
}