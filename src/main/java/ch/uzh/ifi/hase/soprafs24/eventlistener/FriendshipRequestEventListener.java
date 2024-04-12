package ch.uzh.ifi.hase.soprafs24.eventlistener;

import ch.uzh.ifi.hase.soprafs24.event.FriendshipRequestAcceptanceEvent;
import ch.uzh.ifi.hase.soprafs24.event.FriendshipRequestSendEvent;
import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class FriendshipRequestEventListener {

    private static final Logger logger = LoggerFactory.getLogger(FriendshipRequestEventListener.class);

    private final WebSocketService webSocketService;

    @Autowired
    public FriendshipRequestEventListener(WebSocketService webSocketService) {
        this.webSocketService = webSocketService;
    }

    @EventListener
    public void onFriendshipRequestAcceptance(FriendshipRequestAcceptanceEvent event) {
        logger.info("FriendshipRequestAcceptanceEvent triggered for userID: {} request by user: {}", event.getUserId(), event.getFriendName());
        webSocketService.sendMessageFriendshipRequestAccepted(event.getFriendName(), event.getUserId());
    }

    @EventListener
    public void onFriendshipRequestSend(FriendshipRequestSendEvent event) {
        logger.info("FriendshipRequestSendEvent triggered for userID: {} by user: {}", event.getUserId(), event.getFriendName());
        webSocketService.sendMessageFriendshipRequestReceived(event.getFriendName(), event.getUserId());
    }

}
