package ch.uzh.ifi.hase.soprafs24.eventlistener;

import ch.uzh.ifi.hase.soprafs24.event.*;
import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class UserEventListener {

    private final WebSocketService webSocketService;

    @Autowired
    public UserEventListener(WebSocketService webSocketService) {
        this.webSocketService = webSocketService;
    }

    @EventListener
    public void onLoginOfFriend(LoginEvent event) {
        webSocketService.sendMessageFriendLogin(event.getUserName(), event.getUserId());
    }

    @EventListener
    public void onLogoutOfFriend(LogoutEvent event) {
        webSocketService.sendMessageFriendLogout(event.getUserName(), event.getUserId());
    }

}
