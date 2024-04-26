package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.Card;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.service.GameDeckService;
import ch.uzh.ifi.hase.soprafs24.service.GameEngineService;
import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.CardMoveRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Controller
public class GameEngineController {

    @Autowired
    private GameDeckService gameDeckService;

    @Autowired
    private GameEngineService gameEngineService;

    @Autowired
    private WebSocketService webSocketService;

    Logger logger = LoggerFactory.getLogger(GameDeckService.class);

    /**
     * Handles the request of an user to play a card
     * @param gameId of the game the user is currently playing
     * @param userId of the user making the move
     * @param cardMoveRequest details of the played card
     */
    @MessageMapping("/move/cards/{gameId}/{userId}")
    public void handleCardMove(
            @DestinationVariable Long gameId,
            @DestinationVariable Long userId,
            @Payload CardMoveRequest cardMoveRequest) throws IOException, InterruptedException {

        Long targetUserId = cardMoveRequest.getTargetUserId();
        logger.info(String.format("Move for game %s by user %s: card(s) played (%s)" , gameId, userId, cardMoveRequest.getCardIds()));
        logger.info(targetUserId.toString());

        // Transformation to internal representation
        List<Card> transformedCards = gameEngineService.transformCardsToInternalRepresentation(cardMoveRequest.getCardIds());

        Game game = gameEngineService.findGameById(gameId);

        // Remove from the player pile (verifies that the moves are valid, and that the user actually possessed the cards he played)
        gameDeckService.removeCardsFromPlayerPile(game, userId, String.join(",", cardMoveRequest.getCardIds()));

        // Add cards to the play pile (i.e. game stack)
        gameDeckService.placeCardsToPlayPile(game, userId ,transformedCards, String.join(",", cardMoveRequest.getCardIds()));

        // Game Logic
        if(transformedCards.size() == 1) {
            if (Objects.equals(transformedCards.get(0).getInternalCode(), "shuffle")) {
                gameEngineService.handleShuffleCard(game, userId);
            }
            else if (Objects.equals(transformedCards.get(0).getInternalCode(), "future")) {
                gameEngineService.handleFutureCard(game, userId);
            }
            else if (Objects.equals(transformedCards.get(0).getInternalCode(), "skip")) {
                gameEngineService.handleSkipCard(game, userId);
            }
            else if (Objects.equals(transformedCards.get(0).getInternalCode(), "favor")) {
                gameEngineService.handleFavorCard(game, userId, targetUserId);
            }
            else if (Objects.equals(transformedCards.get(0).getInternalCode(), "attack")) {
                gameEngineService.handleAttackCard(game, userId);
            }
        }
        else if(transformedCards.size()==2) {
            if (Objects.equals(transformedCards.get(0).getInternalCode(), (transformedCards.get(1).getInternalCode())) && (Objects.equals(transformedCards.get(0).getInternalCode(), "tacocat"))) {
                gameEngineService.handleFutureCard(game, userId);
            }
            else if (Objects.equals(transformedCards.get(0).getInternalCode(), (transformedCards.get(1).getInternalCode())) && (Objects.equals(transformedCards.get(0).getInternalCode(), "cattermelon"))) {
                gameEngineService.handleAttackCard(game, userId);
            }
            else if (Objects.equals(transformedCards.get(0).getInternalCode(), (transformedCards.get(1).getInternalCode())) && (Objects.equals(transformedCards.get(0).getInternalCode(), "hairypotatocat"))) {
                gameEngineService.handleSkipCard(game, userId);
            }
            else if (Objects.equals(transformedCards.get(0).getInternalCode(), (transformedCards.get(1).getInternalCode())) && (Objects.equals(transformedCards.get(0).getInternalCode(), "beardcat"))) {
                gameEngineService.handleShuffleCard(game, userId);
            }
        }
    }

    // No -> block another users action -> wait on client side for couple of seconds after card play to see if a user interferes

    /**
     * Start the setup of the game indicated in the path variable
     * @param gameId of the game that the user started
     */
    @MessageMapping("/start/{gameId}")
    public void handleStartGame(
            @DestinationVariable("gameId") Long gameId) throws IOException, InterruptedException {

        logger.info(String.format("Game: %s, started" , gameId));
        Game initializedGame = gameEngineService.startGame(gameId);
    }

    /**
     * Routine executed once the user finished playing, including: draw end of move card, validate explosion, among others
     * @param gameId of the game that the user started
     * @param userId of the user finishing a turn
     */
    @MessageMapping("/terminateMove/{gameId}/{userId}")
    public void handleTerminatingMove(
            @DestinationVariable("gameId") Long gameId,
            @DestinationVariable("userId") Long userId) throws IOException, InterruptedException{

        logger.info(String.format("Game: %s, user: %s terminated his turn" , gameId, userId));

        // Handle termination of move draw
        String explosionCard = gameEngineService.drawCardMoveTermination(gameId, userId);

        if (explosionCard != null) {
            // To DO -- handle explosion
            gameEngineService.handleExplosionCard(gameId, userId, explosionCard);
        }

        // Handle turnValidation (finding next player and communicating through websocket)
        gameEngineService.turnValidation(gameId, userId);

        // Dispatch gameState
        gameEngineService.dispatchGameState(gameId, userId);
    }

    /**
     * Removes a player that leaves before losing
     * @param gameId of the game the player is currently in
     * @param userId of the user leaving the game
     */
    @MessageMapping("leaving/{gameId}/{userId}")
    public void handleLeavingUser(
            @DestinationVariable("gameId") Long gameId,
            @DestinationVariable("userId") Long userId) throws IOException, InterruptedException {

        logger.info(String.format("User %s left game %s" , userId, gameId));

        // Handle user leaving an ongoing game session
        gameEngineService.removeUserFromGame(gameId, userId);
    }
}
