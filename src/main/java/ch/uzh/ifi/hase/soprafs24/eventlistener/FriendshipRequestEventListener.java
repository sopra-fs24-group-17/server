package ch.uzh.ifi.hase.soprafs24.eventlistener;

import ch.uzh.ifi.hase.soprafs24.event.FriendshipRequestAcceptanceEvent;
import ch.uzh.ifi.hase.soprafs24.event.FriendshipRequestSendEvent;
import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class FriendshipRequestEventListener {

    private final WebSocketService webSocketService;

    @Autowired
    public FriendshipRequestEventListener(WebSocketService webSocketService) {
        this.webSocketService = webSocketService;
    }

    @EventListener
    public void onFriendshipRequestAcceptance(FriendshipRequestAcceptanceEvent event) {
        webSocketService.sendMessageFriendshipRequestAccepted(event.getFriendName(), event.getUserId());
    }

    @EventListener
    public void onFriendshipRequestSend(FriendshipRequestSendEvent event) {
        webSocketService.sendMessageFriendshipRequestReceived(event.getFriendName(), event.getUserId());
    }

}
