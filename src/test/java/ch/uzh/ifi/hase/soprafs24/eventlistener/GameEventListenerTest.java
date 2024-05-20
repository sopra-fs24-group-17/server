package ch.uzh.ifi.hase.soprafs24.eventlistener;

import ch.uzh.ifi.hase.soprafs24.event.GameJoinEvent;
import ch.uzh.ifi.hase.soprafs24.event.GameLeaveEvent;
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
        Integer maxPlayers = 4;
        Integer currentPlayers = 2;
        GameJoinEvent event = new GameJoinEvent(this, userName, gameId, maxPlayers, currentPlayers);

        gameEventListener.onGameJoinEvent(event);
        verify(webSocketService).sendMessageJoinedUser(userName, gameId, maxPlayers, currentPlayers);
    }

    @Test
    public void testOnGameLeaveEvent() {
        String userName = "testUser";
        Long gameId = 123L;
        Integer maxPlayers = 3;
        Integer currentPlayers = 2;

        GameLeaveEvent event = new GameLeaveEvent(this, userName, gameId, maxPlayers, currentPlayers);

        gameEventListener.onGameLeaveEvent(event);
        verify(webSocketService).sendMessageLeftUser(userName, gameId, maxPlayers, currentPlayers);
    }
}
