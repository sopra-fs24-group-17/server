package ch.uzh.ifi.hase.soprafs24.eventlistener;

import ch.uzh.ifi.hase.soprafs24.event.*;
import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UserEventListener {

    private final WebSocketService webSocketService;

    @Autowired
    public UserEventListener(WebSocketService webSocketService) {
        this.webSocketService = webSocketService;
    }

    @EventListener
    public void onLoginOfFriend(LoginEvent event) {
        log.info("LoginEvent triggered for user: {}", event.getUserName());
        webSocketService.sendMessageFriendLogin(event.getUserName(), event.getUserId());
    }

    @EventListener
    public void onLogoutOfFriend(LogoutEvent event) {
        log.info("LogoutEvent triggered for user: {}", event.getUserName());
        webSocketService.sendMessageFriendLogout(event.getUserName(), event.getUserId());
    }

}
