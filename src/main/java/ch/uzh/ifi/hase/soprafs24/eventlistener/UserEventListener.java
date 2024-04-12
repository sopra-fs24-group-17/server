package ch.uzh.ifi.hase.soprafs24.eventlistener;

import ch.uzh.ifi.hase.soprafs24.event.*;
import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class UserEventListener {

    private static final Logger logger = LoggerFactory.getLogger(UserEventListener.class);

    private final WebSocketService webSocketService;

    @Autowired
    public UserEventListener(WebSocketService webSocketService) {
        this.webSocketService = webSocketService;
    }

    @EventListener
    public void onLoginOfFriend(LoginEvent event) {
        logger.info("LoginEvent triggered for user: {}", event.getUserName());
        webSocketService.sendMessageFriendLogin(event.getUserName(), event.getUserId());
    }

    @EventListener
    public void onLogoutOfFriend(LogoutEvent event) {
        logger.info("LogoutEvent triggered for user: {}", event.getUserName());
        webSocketService.sendMessageFriendLogout(event.getUserName(), event.getUserId());
    }

}
