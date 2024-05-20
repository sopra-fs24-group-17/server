package ch.uzh.ifi.hase.soprafs24.eventlistener;

import ch.uzh.ifi.hase.soprafs24.event.LoginEvent;
import ch.uzh.ifi.hase.soprafs24.event.LogoutEvent;
import ch.uzh.ifi.hase.soprafs24.eventlistener.UserEventListener;
import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ActiveProfiles;

import static org.mockito.Mockito.verify;

@ActiveProfiles("dev")
public class UserEventListenerTest {

    @Mock
    private WebSocketService webSocketService;

    @InjectMocks
    private UserEventListener userEventListener;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testOnLoginOfFriend() {
        String userName = "testUser";
        Long userId = 123L;
        LoginEvent event = new LoginEvent(this, userName, userId);

        userEventListener.onLoginOfFriend(event);

        verify(webSocketService).sendMessageFriendLogin(userName, userId);
    }

    @Test
    public void testOnLogoutOfFriend() {
        String userName = "testUser";
        Long userId = 456L;
        LogoutEvent event = new LogoutEvent(this, userName, userId);

        userEventListener.onLogoutOfFriend(event);

        verify(webSocketService).sendMessageFriendLogout(userName, userId);
    }
}

