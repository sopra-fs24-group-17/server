package ch.uzh.ifi.hase.soprafs24.eventlistener;

import ch.uzh.ifi.hase.soprafs24.event.FriendshipRequestAcceptanceEvent;
import ch.uzh.ifi.hase.soprafs24.event.FriendshipRequestSendEvent;
import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;

import static org.mockito.Mockito.*;

public class FriendshipRequestEventListenerTest {

    @Mock
    private WebSocketService webSocketService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private FriendshipRequestEventListener eventListener;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testOnFriendshipRequestAcceptance() {
        String friendName = "testFriend";
        Long userId = 123L;
        FriendshipRequestAcceptanceEvent event = new FriendshipRequestAcceptanceEvent(this, friendName, userId);

        eventListener.onFriendshipRequestAcceptance(event);

        verify(webSocketService).sendMessageFriendshipRequestAccepted(friendName, userId);
    }

    @Test
    public void testOnFriendshipRequestSend() {
        String friendName = "testFriend";
        Long userId = 456L;
        FriendshipRequestSendEvent event = new FriendshipRequestSendEvent(this, friendName, userId);

        eventListener.onFriendshipRequestSend(event);

        verify(webSocketService).sendMessageFriendshipRequestReceived(friendName, userId);
    }
}

