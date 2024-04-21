package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
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
import org.springframework.web.bind.annotation.PathVariable;

import java.io.IOException;

@Controller
public class GameEngineController {

    @Autowired
    private GameDeckService gameDeckService;

    @Autowired
    private GameEngineService gameEngineService;

    @Autowired
    private WebSocketService webSocketService;

    Logger logger = LoggerFactory.getLogger(GameDeckService.class);

    @MessageMapping("/move/cards/{gameId}/{userId}")
    public void handleCardMove(
            @PathVariable Long gameId,
            @PathVariable Long userId,
            @Payload CardMoveRequest cardMoveRequest) {

        logger.info(String.format("Move for game %s by user %s: card(s) played (%s)" , gameId, userId, cardMoveRequest.getCardIds()));

        // To do -- Awaiting mapper from Jorge

        // To do -- handle game logic

        // Shuffling

        // Drawing

        // Explosion drawn -> don't end user's turn, await for his defuse card, else count as loss and put the user out of the game.
        // Ensure that he is skipped for the next iterations

        // Playing defuse card -> place explosion back on deck

        // Skip -> end turn without drawing

        // No -> block another users action -> wait on client side for couple of seconds after card play to see if a user interferes
    }

    @MessageMapping("/start/{gameId}")
    public void handleStartGame(
            @DestinationVariable("gameId") Long gameId) throws IOException, InterruptedException {

        logger.info(String.format("Game: %s, started" , gameId));

        // To do -- distribute cards (Jorge)
        Game initializedGame = gameEngineService.startGame(gameId);

    }

    @MessageMapping("/terminateMove/{gameId}/{userId}")
    public void handleTerminatingMove(
            @DestinationVariable("gameId") Long gameId,
            @DestinationVariable("userId") Long userId) throws IOException, InterruptedException{

        logger.info(String.format("Game: %s, user: %s terminated his turn" , gameId, userId));

        // To do -- Awaiting mapper from Jorge
        // User is required to draw a card from the dealer pile
        // Send the card through the websocket to the user

        // Handle turnValidation (finding next player and communicating through websocket)
        gameEngineService.turnValidation(gameId, userId);
    }

    @MessageMapping("leaving/{gameId}/{userId}")
    public void handleLeavingUser(
            @DestinationVariable("gameId") Long gameId,
            @DestinationVariable("userId") Long userId) throws IOException, InterruptedException {

        logger.info(String.format("User %s left game %s" , userId, gameId));

        // Handle user leaving an ongoing game session
        gameEngineService.userLeavingOngoingGame(gameId, userId);
    }
}