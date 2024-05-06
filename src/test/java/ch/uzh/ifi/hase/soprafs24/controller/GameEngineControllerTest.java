import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

import ch.uzh.ifi.hase.soprafs24.controller.GameEngineController;
import ch.uzh.ifi.hase.soprafs24.entity.Card;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.service.GameDeckService;
import ch.uzh.ifi.hase.soprafs24.service.GameEngineService;
import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.CardMoveRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class GameEngineControllerTest {

    @Mock
    private GameDeckService gameDeckService;

    @Mock
    private GameEngineService gameEngineService;

    @Mock
    private WebSocketService webSocketService;

    @InjectMocks
    private GameEngineController gameEngineController;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testHandleCardMove_success() throws IOException, InterruptedException {
        Long gameId = 1L;
        Long userId = 2L;
        Long targetUserId = 3L;

        CardMoveRequest cardMoveRequest = new CardMoveRequest();
        Card card1 = new Card();
        card1.setInternalCode("explosion");
        card1.setCode("AB");
        card1.setImage("image");
        card1.setSuit("suit");

        Card card2 = new Card();
        card2.setInternalCode("defuse");
        card2.setCode("CD");
        card2.setImage("image");
        card2.setSuit("suit");

        List<String> cardIds = new ArrayList<>();
        cardIds.add(card1.getCode());
        cardIds.add(card2.getCode());

        cardMoveRequest.setCardIds(cardIds);
        cardMoveRequest.setTargetUserId(targetUserId);

        List<Card> transformedCards = new ArrayList<>();
        transformedCards.add(card1);
        transformedCards.add(card2);
        Game game = new Game();

        when(gameEngineService.transformCardsToInternalRepresentation(anyList())).thenReturn(transformedCards);
        when(gameEngineService.findGameById(gameId)).thenReturn(game);
        doNothing().when(gameDeckService).removeCardsFromPlayerPile(any(Game.class), anyLong(), anyString());
        doNothing().when(gameDeckService).placeCardsToPlayPile(any(Game.class), anyLong(), anyList(), anyString());

        gameEngineController.handleCardMove(gameId, userId, cardMoveRequest);

        verify(gameDeckService, times(1)).removeCardsFromPlayerPile(game, userId, "AB,CD");
        verify(gameDeckService, times(1)).placeCardsToPlayPile(game, userId, transformedCards, "AB,CD");
        verify(gameEngineService, times(1)).dispatchGameState(gameId, userId);
    }

    @Test
    void testHandleStartGame() throws Exception {
        Long gameId = 1L;
        Game game = new Game();
        when(gameEngineService.startGame(gameId)).thenReturn(game);

        gameEngineController.handleStartGame(gameId);

        verify(gameEngineService, times(1)).startGame(gameId);
    }

}
