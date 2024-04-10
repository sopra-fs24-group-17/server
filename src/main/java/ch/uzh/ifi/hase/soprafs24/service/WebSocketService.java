package ch.uzh.ifi.hase.soprafs24.service;

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

    public void sendMessageJoinedUser(String userName, Long gameId) {
        this.sendMessage.convertAndSend("/game/" + gameId, userName);
    }

    public void sendMessageFriendLogin(String userName, Long userId) {
        this.sendMessage.convertAndSend("/login/" + userId, userName);
    }

    public void sendMessageFriendLogout(String userName, Long userId) {
        this.sendMessage.convertAndSend("/logout/" + userId, userName);
    }

    public void sendMessageFriendshipRequestAccepted(String userName, Long userId) {
        this.sendMessage.convertAndSend("/friendshiprequest/acceptance/" + userId, userName);
    }

    public void sendMessageFriendshipRequestReceived(String userName, Long userId) {
        this.sendMessage.convertAndSend("/friendshiprequest/received/" + userId, userName);
    }

}