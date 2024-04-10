package ch.uzh.ifi.hase.soprafs24.eventlistener;

import ch.uzh.ifi.hase.soprafs24.event.GameJoinEvent;
import ch.uzh.ifi.hase.soprafs24.eventlistener.GameEventListener;
import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.verify;

public class GameEventListenerTest {

    @Mock
    private WebSocketService webSocketService;

    @InjectMocks
    private GameEventListener gameEventListener;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testOnGameJoinEvent() {
        String userName = "testUser";
        Long gameId = 123L;
        GameJoinEvent event = new GameJoinEvent(this, userName, gameId);

        gameEventListener.onGameJoinEvent(event);

        verify(webSocketService).sendMessageJoinedUser(userName, gameId);
    }

    // Placeholder for onGameLeaveEvent test
    // TO DO -- to be implemented
}
