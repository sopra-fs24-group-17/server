package ch.uzh.ifi.hase.soprafs24.eventlistener;

import ch.uzh.ifi.hase.soprafs24.event.GameCreationEvent;
import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class GameCreationEventListenerTest {

    @Mock
    private WebSocketService webSocketService;

    @InjectMocks
    private GameCreationEventListener gameCreationEventListener;

    @Test
    public void testOnGameCreation() {
        Long gameId = 123L;
        GameCreationEvent event = new GameCreationEvent(this, gameId, "TestUser");
        gameCreationEventListener.onGameCreation(event);
        verify(webSocketService).sendMessageToClients(eq("/game/new"), anyString());
    }
}
