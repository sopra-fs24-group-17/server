package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.Card;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.service.GameDeckService;
import ch.uzh.ifi.hase.soprafs24.service.GameEngineService;
import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.CardMoveRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

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
        cardMoveRequest.setTargetUsername("testUser");

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

        User startUser = new User();
        startUser.setId(1L);
        startUser.setUsername("startUser");
        startUser.setEmail("start@user");

        game.setCurrentTurn(startUser);

        when(gameEngineService.startGame(gameId)).thenReturn(game);

        gameEngineController.handleStartGame(gameId);

        verify(gameEngineService, times(1)).startGame(gameId);
    }

    @Test
    void testHandleCardMove_ShuffleCard() throws IOException, InterruptedException {
        Long gameId = 1L;
        Long userId = 2L;
        CardMoveRequest cardMoveRequest = new CardMoveRequest();
        List<String> cardIds = List.of("AB");
        cardMoveRequest.setCardIds(cardIds);
        cardMoveRequest.setTargetUsername("testUser");

        Card card = new Card();
        card.setInternalCode("shuffle");
        card.setCode("AB");

        List<Card> transformedCards = List.of(card);
        Game game = new Game();

        when(gameEngineService.transformCardsToInternalRepresentation(cardIds)).thenReturn(transformedCards);
        when(gameEngineService.findGameById(gameId)).thenReturn(game);
        doNothing().when(gameDeckService).removeCardsFromPlayerPile(any(Game.class), eq(userId), anyString());
        doNothing().when(gameDeckService).placeCardsToPlayPile(any(Game.class), eq(userId), anyList(), anyString());
        doNothing().when(gameEngineService).handleShuffleCard(any(Game.class), eq(userId));

        gameEngineController.handleCardMove(gameId, userId, cardMoveRequest);

        verify(gameEngineService).handleShuffleCard(game, userId);
        verify(gameDeckService).removeCardsFromPlayerPile(game, userId, "AB");
        verify(gameDeckService).placeCardsToPlayPile(game, userId, transformedCards, "AB");
        verify(gameEngineService).dispatchGameState(gameId, userId);
    }

    @Test
    void testHandleCardMove_FutureCard() throws IOException, InterruptedException {
        Long gameId = 1L;
        Long userId = 2L;
        CardMoveRequest cardMoveRequest = new CardMoveRequest();
        List<String> cardIds = List.of("AB");
        cardMoveRequest.setCardIds(cardIds);
        cardMoveRequest.setTargetUsername("targetUser");

        Card card = new Card();
        card.setInternalCode("future");
        card.setCode("AB");

        List<Card> transformedCards = List.of(card);
        Game game = new Game();

        when(gameEngineService.transformCardsToInternalRepresentation(cardIds)).thenReturn(transformedCards);
        when(gameEngineService.findGameById(gameId)).thenReturn(game);
        doNothing().when(gameDeckService).removeCardsFromPlayerPile(any(Game.class), eq(userId), anyString());
        doNothing().when(gameDeckService).placeCardsToPlayPile(any(Game.class), eq(userId), anyList(), anyString());
        doNothing().when(gameEngineService).handleShuffleCard(any(Game.class), eq(userId));

        gameEngineController.handleCardMove(gameId, userId, cardMoveRequest);

        verify(gameEngineService).handleFutureCard(game, userId);
        verify(gameDeckService).removeCardsFromPlayerPile(game, userId, "AB");
        verify(gameDeckService).placeCardsToPlayPile(game, userId, transformedCards, "AB");
        verify(gameEngineService).dispatchGameState(gameId, userId);
    }

    @Test
    void testHandleCardMove_SkipCard() throws IOException, InterruptedException {
        Long gameId = 1L;
        Long userId = 2L;
        CardMoveRequest cardMoveRequest = new CardMoveRequest();
        List<String> cardIds = List.of("AB");
        cardMoveRequest.setCardIds(cardIds);
        cardMoveRequest.setTargetUsername("targetUser");

        Card card = new Card();
        card.setInternalCode("skip");
        card.setCode("AB");

        List<Card> transformedCards = List.of(card);
        Game game = new Game();

        when(gameEngineService.transformCardsToInternalRepresentation(cardIds)).thenReturn(transformedCards);
        when(gameEngineService.findGameById(gameId)).thenReturn(game);
        doNothing().when(gameDeckService).removeCardsFromPlayerPile(any(Game.class), eq(userId), anyString());
        doNothing().when(gameDeckService).placeCardsToPlayPile(any(Game.class), eq(userId), anyList(), anyString());
        doNothing().when(gameEngineService).handleShuffleCard(any(Game.class), eq(userId));

        gameEngineController.handleCardMove(gameId, userId, cardMoveRequest);

        verify(gameEngineService).handleSkipCard(game, userId);
        verify(gameDeckService).removeCardsFromPlayerPile(game, userId, "AB");
        verify(gameDeckService).placeCardsToPlayPile(game, userId, transformedCards, "AB");
        verify(gameEngineService).dispatchGameState(gameId, userId);

    }


    @Test
    void testHandleCardMove_FavorCard() throws IOException, InterruptedException {
        Long gameId = 1L;
        Long userId = 2L;
        CardMoveRequest cardMoveRequest = new CardMoveRequest();
        List<String> cardIds = List.of("AB");
        cardMoveRequest.setCardIds(cardIds);
        cardMoveRequest.setTargetUsername("targetUser");

        User targetUser = new User();
        targetUser.setId(1L);
        targetUser.setUsername("targetUser");

        Card card = new Card();
        card.setInternalCode("favor");
        card.setCode("AB");

        List<Card> transformedCards = List.of(card);
        Game game = new Game();

        when(gameEngineService.transformCardsToInternalRepresentation(cardIds)).thenReturn(transformedCards);
        when(gameEngineService.findGameById(gameId)).thenReturn(game);
        /* Mock user repo: when(userRepository.findByUsername(anyString())).thenReturn(targetUser); */
        doNothing().when(gameDeckService).removeCardsFromPlayerPile(any(Game.class), eq(userId), anyString());
        doNothing().when(gameDeckService).placeCardsToPlayPile(any(Game.class), eq(userId), anyList(), anyString());
        doNothing().when(gameEngineService).handleShuffleCard(any(Game.class), eq(userId));

        gameEngineController.handleCardMove(gameId, userId, cardMoveRequest);

        verify(gameEngineService).handleFavorCard(game, userId, cardMoveRequest.getTargetUsername());
        verify(gameDeckService).removeCardsFromPlayerPile(game, userId, "AB");
        verify(gameDeckService).placeCardsToPlayPile(game, userId, transformedCards, "AB");
        verify(gameEngineService).dispatchGameState(gameId, userId);
    }


    @Test
    void testHandleCardMove_AttackCard() throws IOException, InterruptedException {

        Long gameId = 1L;
        Long userId = 2L;
        CardMoveRequest cardMoveRequest = new CardMoveRequest();
        List<String> cardIds = List.of("AB");
        cardMoveRequest.setCardIds(cardIds);
        cardMoveRequest.setTargetUsername("targetUser");

        Card card = new Card();
        card.setInternalCode("attack");
        card.setCode("AB");

        List<Card> transformedCards = List.of(card);
        Game game = new Game();

        when(gameEngineService.transformCardsToInternalRepresentation(cardIds)).thenReturn(transformedCards);
        when(gameEngineService.findGameById(gameId)).thenReturn(game);
        doNothing().when(gameDeckService).removeCardsFromPlayerPile(any(Game.class), eq(userId), anyString());
        doNothing().when(gameDeckService).placeCardsToPlayPile(any(Game.class), eq(userId), anyList(), anyString());
        doNothing().when(gameEngineService).handleShuffleCard(any(Game.class), eq(userId));

        gameEngineController.handleCardMove(gameId, userId, cardMoveRequest);

        verify(gameEngineService).handleAttackCard(game, userId);
        verify(gameDeckService).removeCardsFromPlayerPile(game, userId, "AB");
        verify(gameDeckService).placeCardsToPlayPile(game, userId, transformedCards, "AB");
        verify(gameEngineService).dispatchGameState(gameId, userId);
    }

    @Test
    void testHandleCardMove_TwoTacocatCards() throws IOException, InterruptedException {
        Long gameId = 1L;
        Long userId = 2L;
        CardMoveRequest cardMoveRequest = new CardMoveRequest();
        List<String> cardIds = List.of("AB", "CD");
        cardMoveRequest.setCardIds(cardIds);
        cardMoveRequest.setTargetUsername("targetUser");

        Card card1 = new Card();
        card1.setInternalCode("tacocat");
        card1.setCode("AB");

        Card card2 = new Card();
        card2.setInternalCode("tacocat");
        card2.setCode("CD");

        List<Card> transformedCards = List.of(card1, card2);
        Game game = new Game();

        when(gameEngineService.transformCardsToInternalRepresentation(cardIds)).thenReturn(transformedCards);
        when(gameEngineService.findGameById(gameId)).thenReturn(game);
        doNothing().when(gameDeckService).removeCardsFromPlayerPile(any(Game.class), eq(userId), anyString());
        doNothing().when(gameDeckService).placeCardsToPlayPile(any(Game.class), eq(userId), anyList(), anyString());
        doNothing().when(gameEngineService).handleFutureCard(any(Game.class), eq(userId));

        gameEngineController.handleCardMove(gameId, userId, cardMoveRequest);

        verify(gameEngineService).handleFutureCard(game, userId);
        verify(gameDeckService).removeCardsFromPlayerPile(game, userId, "AB,CD");
        verify(gameDeckService).placeCardsToPlayPile(game, userId, transformedCards, "AB,CD");
        verify(gameEngineService).dispatchGameState(gameId, userId);
    }

    @Test
    void testHandleCardMove_TwoCattermelonCards() throws IOException, InterruptedException {
        Long gameId = 1L;
        Long userId = 2L;
        CardMoveRequest cardMoveRequest = new CardMoveRequest();
        List<String> cardIds = List.of("AB", "CD");
        cardMoveRequest.setCardIds(cardIds);
        cardMoveRequest.setTargetUsername("targetUser");

        Card card1 = new Card();
        card1.setInternalCode("cattermelon");
        card1.setCode("AB");

        Card card2 = new Card();
        card2.setInternalCode("cattermelon");
        card2.setCode("CD");

        List<Card> transformedCards = List.of(card1, card2);
        Game game = new Game();

        when(gameEngineService.transformCardsToInternalRepresentation(cardIds)).thenReturn(transformedCards);
        when(gameEngineService.findGameById(gameId)).thenReturn(game);
        doNothing().when(gameDeckService).removeCardsFromPlayerPile(any(Game.class), eq(userId), anyString());
        doNothing().when(gameDeckService).placeCardsToPlayPile(any(Game.class), eq(userId), anyList(), anyString());
        doNothing().when(gameEngineService).handleAttackCard(any(Game.class), eq(userId));

        gameEngineController.handleCardMove(gameId, userId, cardMoveRequest);

        verify(gameEngineService).handleAttackCard(game, userId);
        verify(gameDeckService).removeCardsFromPlayerPile(game, userId, "AB,CD");
        verify(gameDeckService).placeCardsToPlayPile(game, userId, transformedCards, "AB,CD");
        verify(gameEngineService).dispatchGameState(gameId, userId);
    }

    @Test
    void testHandleCardMove_TwoBeardCatCards() throws IOException, InterruptedException {
        Long gameId = 1L;
        Long userId = 2L;
        CardMoveRequest cardMoveRequest = new CardMoveRequest();
        List<String> cardIds = List.of("AB", "CD");
        cardMoveRequest.setCardIds(cardIds);
        cardMoveRequest.setTargetUsername("targetUser");

        Card card1 = new Card();
        card1.setInternalCode("beardcat");
        card1.setCode("AB");

        Card card2 = new Card();
        card2.setInternalCode("beardcat");
        card2.setCode("CD");

        List<Card> transformedCards = List.of(card1, card2);
        Game game = new Game();

        when(gameEngineService.transformCardsToInternalRepresentation(cardIds)).thenReturn(transformedCards);
        when(gameEngineService.findGameById(gameId)).thenReturn(game);
        doNothing().when(gameDeckService).removeCardsFromPlayerPile(any(Game.class), eq(userId), anyString());
        doNothing().when(gameDeckService).placeCardsToPlayPile(any(Game.class), eq(userId), anyList(), anyString());
        doNothing().when(gameEngineService).handleShuffleCard(any(Game.class), eq(userId));

        gameEngineController.handleCardMove(gameId, userId, cardMoveRequest);

        verify(gameEngineService).handleShuffleCard(game, userId);
        verify(gameDeckService).removeCardsFromPlayerPile(game, userId, "AB,CD");
        verify(gameDeckService).placeCardsToPlayPile(game, userId, transformedCards, "AB,CD");
        verify(gameEngineService).dispatchGameState(gameId, userId);
    }

    @Test
    void testHandleCardMove_TwoHairyPotatoCatCards() throws IOException, InterruptedException {
        Long gameId = 1L;
        Long userId = 2L;
        CardMoveRequest cardMoveRequest = new CardMoveRequest();
        List<String> cardIds = List.of("AB", "CD");
        cardMoveRequest.setCardIds(cardIds);
        cardMoveRequest.setTargetUsername("targetUser");

        Card card1 = new Card();
        card1.setInternalCode("hairypotatocat");
        card1.setCode("AB");

        Card card2 = new Card();
        card2.setInternalCode("hairypotatocat");
        card2.setCode("CD");

        List<Card> transformedCards = List.of(card1, card2);
        Game game = new Game();

        when(gameEngineService.transformCardsToInternalRepresentation(cardIds)).thenReturn(transformedCards);
        when(gameEngineService.findGameById(gameId)).thenReturn(game);
        doNothing().when(gameDeckService).removeCardsFromPlayerPile(any(Game.class), eq(userId), anyString());
        doNothing().when(gameDeckService).placeCardsToPlayPile(any(Game.class), eq(userId), anyList(), anyString());
        doNothing().when(gameEngineService).handleSkipCard(any(Game.class), eq(userId));

        gameEngineController.handleCardMove(gameId, userId, cardMoveRequest);

        verify(gameEngineService).handleSkipCard(game, userId);
        verify(gameDeckService).removeCardsFromPlayerPile(game, userId, "AB,CD");
        verify(gameDeckService).placeCardsToPlayPile(game, userId, transformedCards, "AB,CD");
        verify(gameEngineService).dispatchGameState(gameId, userId);
    }

    @Test
    void testHandleExplosionPlacement() throws IOException, InterruptedException {
        Long gameId = 1L;
        Long userId = 2L;
        String placementPosition = "1";

        doNothing().when(gameEngineService).handleExplosionPlacement(gameId, userId, Integer.parseInt(placementPosition));

        gameEngineController.handleExplosionPlacement(gameId, userId, placementPosition);

        verify(gameEngineService, times(1)).handleExplosionPlacement(gameId, userId, Integer.parseInt(placementPosition));
    }

    @Test
    public void testLoadCachedGame_Success() throws IOException, InterruptedException {
        // Given
        Long gameId = 1L;
        Long userId = 2L;

        gameEngineController.loadCachedGame(gameId, userId);

        verify(gameEngineService, times(1)).reloadGameState(gameId, userId);
    }

    @Test
    public void testLoadCachedGame_UserNotPartOfGame() throws IOException, InterruptedException {
        Long gameId = 1L;
        Long userId = 2L;

        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Targeted User is not part of the game"))
                .when(gameEngineService).reloadGameState(gameId, userId);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            gameEngineController.loadCachedGame(gameId, userId);
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Targeted User is not part of the game", exception.getReason());
    }

}
