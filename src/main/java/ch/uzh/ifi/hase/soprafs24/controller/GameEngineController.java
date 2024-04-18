package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.service.GameDeckService;
import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.CardMoveRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class GameEngineController {

    @Autowired
    private GameDeckService gameDeckService;

    @Autowired
    private WebSocketService webSocketService;

    Logger logger = LoggerFactory.getLogger(GameDeckService.class);

    @MessageMapping("/move/cards/{gameId}/{userId}")
    public void handleCardMove(
            @PathVariable Long gameId,
            @PathVariable Long userId,
            @Payload CardMoveRequest cardMoveRequest) {

        logger.info(String.format("Move for game %s by user %s: card(s) played (%s)" , gameId, userId, cardMoveRequest.getCardIds()));

        // To do -- handle game logic
    }

    @MessageMapping("/start/{gameId}")
    public void handleStartGame(
            @PathVariable("gameId") Long gameId) {

        logger.info(String.format("Game: %s, started" , gameId));

        // To do -- handle start of game logic
    }

    @MessageMapping("/terminateMove/{gameId}/{userId}")
    public void handleTerminatingMove(
            @PathVariable("gameId") Long gameId,
            @PathVariable("userId") Long userId) {

        logger.info(String.format("Game: %s, user: %s terminated his turn" , gameId, userId));

        // To do -- handle terminating move logic
    }

    @MessageMapping("leaving/{gameId}/{userId}")
    public void handleLeavingUser(
            @PathVariable("gamId") Long gameId,
            @PathVariable("userId") Long userId) {

        logger.info(String.format("User %s left game %s" , userId, gameId));

        // To do -- handle leaving user logic
    }
}
