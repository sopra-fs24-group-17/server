package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.service.GameDeckService;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.CardMoveRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.IOException;

@Controller
public class GameEngineController {

    @Autowired
    private GameDeckService gameDeckService;

    @Autowired
    private GameService gameService;

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
            @PathVariable("gameId") Long gameId) throws IOException, InterruptedException {

        logger.info(String.format("Game: %s, started" , gameId));

        // To do -- handle start of game logic
        // Swap state of game to ACTIVE
        // Distribute cards (Jorge)
        // create dealer pile
        gameService.startGame(gameId);

        // assign active player to be the first player in the list

        // publish an event that it's this players time to make a move

    }

    // THIS IS JUST A TEST ENDPOINT WHILE I FIX THE WEBSOCKET ISSUE
//    @GetMapping ("/start/{gameId}")
//    @ResponseStatus(HttpStatus.OK)
//    @ResponseBody
//    public void test_handleStartGame(
//            @PathVariable("gameId") Long gameId) throws IOException, InterruptedException {
//
//        logger.info(String.format("Game: %s, started" , gameId));
//
//        // To do -- handle start of game logic
//        // Swap state of game to ACTIVE
//        // Distribute cards (Jorge)
//        // create dealer pile
//        gameService.startGame(gameId);
//
//        // assign active player to be the first player in the list
//
//        // publish an event that it's this players time to make a move
//
//    }

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

        // Update gamesPlayed count for each player

        // Consider this as a loss
    }
}
