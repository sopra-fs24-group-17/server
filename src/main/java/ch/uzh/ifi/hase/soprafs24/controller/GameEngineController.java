package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.Card;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.GameDeck;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePostDTO;
import ch.uzh.ifi.hase.soprafs24.service.GameDeckService;
import ch.uzh.ifi.hase.soprafs24.service.GameEngineService;
import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.CardMoveRequest;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.ExplosionCardRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.IntStream;

import static com.sun.activation.registries.LogSupport.log;

@Slf4j
@RestController
public class GameEngineController {

    @Autowired
    private GameDeckService gameDeckService;

    @Autowired
    private GameEngineService gameEngineService;

    @Autowired
    private WebSocketService webSocketService;

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
        GameEngineController.log.info(String.format("Move for game %s by user %s: card(s) played (%s)" , gameId, userId, cardMoveRequest.getCardIds()));
        GameEngineController.log.info(targetUserId.toString());

        // Transformation to internal representation
        List<Card> transformedCards = gameEngineService.transformCardsToInternalRepresentation(cardMoveRequest.getCardIds());

        Game game = gameEngineService.findGameById(gameId);

        // Remove from the player pile (verifies that the moves are valid, and that the user actually possessed the cards he played)
        // Nope implementation paused at the moment due to higher complexity
        /*
        if(cardMoveRequest.getNegationUsers() != null){
            Set<String> negationUsers = new HashSet<String>(cardMoveRequest.getNegationUsers());
            List<String> negationUsersList = cardMoveRequest.getNegationUsers();
            List<String> cardsIdsNegationUser = new ArrayList<>();
            List<String> allPlayedCards = cardMoveRequest.getCardIds();
            Integer difference = allPlayedCards.size() - negationUsersList.size(); // Amount of cards that are not negations. Placed in the bottom of the stack.
            for(String user : negationUsers){
                // Find all the negations played by the same player
                for (int i = 0; i < negationUsersList.size(); i++) {
                    if (Objects.equals(negationUsersList.get(i), user)){
                        cardsIdsNegationUser.add(allPlayedCards.get(difference+i));
                        //allPlayedCards.remove(difference + i);
                    }
                }
                // Remove cards from current user
                gameDeckService.removeCardsFromPlayerPile(game, Long.valueOf(user), String.join(",", cardsIdsNegationUser));
            }
            // Update cardMoveRequest so we only have the cards played by active user and we can remove them from his hand.
            cardMoveRequest.setCardIds(allPlayedCards.subList(0, difference));
        }
         */
        gameDeckService.removeCardsFromPlayerPile(game, userId, String.join(",", cardMoveRequest.getCardIds()));

        // Add cards to the play pile (i.e. game stack)
        gameDeckService.placeCardsToPlayPile(game, userId ,transformedCards, String.join(",", cardMoveRequest.getCardIds()));

        // Check for potential chain of nope cards before handling logic
        // Nope implementation paused at the moment due to higher complexity
        /*
        if (Objects.equals(transformedCards.get(transformedCards.size() - 1).getInternalCode(), "nope")){
            while (transformedCards.size() > 2){
                Card lastCard = transformedCards.get(transformedCards.size() - 1);
                Card preLastCard = transformedCards.get(transformedCards.size() - 2);
                // Remove top cards if both are negation
                if(Objects.equals(lastCard.getInternalCode(), preLastCard.getInternalCode())){
                    transformedCards.remove(transformedCards.size() - 1);
                    transformedCards.remove(transformedCards.size() - 1);
                    log.info("Consecutive nope cards removed");
                }
            }
        }
         */

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
            else if (Objects.equals(transformedCards.get(0).getInternalCode(), "lucky")) {
                gameEngineService.handleLuckyCard(game, userId);
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
            // Nope card
            // Nope implementation paused at the moment due to higher complexity
            /*
            else if (Objects.equals(transformedCards.get(transformedCards.size() - 1).getInternalCode(), "nope")){
                gameEngineService.handleNopeCard(game);
            }
             */
        }

        // Dispatch Stats
        gameEngineService.dispatchGameState(gameId,userId);
    }

    //THIS IS JUST A TEST ENDPOINT WHILE I FIX THE WEBSOCKET ISSUE
    /*
    @GetMapping("/lucky/{gameId}/{userId}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void test_nopeCard(
            @PathVariable("gameId") Long gameId,
            @PathVariable("userId") Long userId) throws IOException, InterruptedException {

        Game game = gameEngineService.findGameById(gameId);

        Card card = gameDeckService.drawCardFromPlayerPile(game.getGameDeck(), userId, null);
        log(card.getCode());
        // Draw a random card
        //Card randomCard = gameDeckService.drawRandomCardDealerPile(game.getGameDeck());
        //log(randomCard.getCode());
        // Give that card to triggering user
        //gameDeckService.returnCardsToPile(game.getGameDeck(), userId.toString(), randomCard.getCode());

    }
    */


    // No -> block another users action -> wait on client side for couple of seconds after card play to see if a user interferes

    /**
     * Start the setup of the game indicated in the path variable
     * @param gameId of the game that the user started
     */
    @MessageMapping("/start/{gameId}")
    public void handleStartGame(
            @DestinationVariable("gameId") Long gameId) throws IOException, InterruptedException {

        GameEngineController.log.info(String.format("Game: %s, started" , gameId));
        Game initializedGame = gameEngineService.startGame(gameId);
        gameEngineService.dispatchGameState(gameId, initializedGame.getCurrentTurn().getId());
    }

    /**
     * Routine executed once the user finished playing, including: draw end of move card, validate explosion, among others
     * @param gameId of the game that the user started
     * @param userId of the user finishing a turn
     */

    @MessageMapping("/terminateMove/{gameId}/{userId}")
    public void handleTerminatingMove(
            @DestinationVariable("gameId") Long gameId,
            @DestinationVariable("userId") Long userId,
            @Payload ExplosionCardRequest explosionCardRequest) throws IOException, InterruptedException{

        GameEngineController.log.info(String.format("Game: %s, user: %s terminated his turn" , gameId, userId));

        // Handle turnValidation (finding next player and communicating through websocket)
        gameEngineService.turnValidation(gameId, userId);

        // Handle termination of move draw
        String explosionCard = gameEngineService.drawCardMoveTermination(gameId, userId);

        if (explosionCard != null) {
            // To DO -- handle explosion
            gameEngineService.handleExplosionCard(gameId, userId, explosionCard, explosionCardRequest.getPosition());

        }

        // Dispatch gameState
        gameEngineService.dispatchGameState(gameId, userId);
    }


     //THIS IS JUST A TEST ENDPOINT WHILE I FIX THE WEBSOCKET ISSUE
    /*
    @GetMapping("/terminateMove/{gameId}/{userId}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void test_handleStartGame(
            @PathVariable("gameId") Long gameId,
            @PathVariable("userId") Long userId) throws IOException, InterruptedException {

        logger.info(String.format("Game: %s, user: %s terminated his turn" , gameId, userId));

        // Handle turnValidation (finding next player and communicating through websocket)
        gameEngineService.turnValidation(gameId, userId);

        // Dispatch gameState
        gameEngineService.dispatchGameState(gameId, userId);

        // Handle termination of move draw
        String explosionCard = "AS";

        if (explosionCard != null) {
            // To DO -- handle explosion
            gameEngineService.handleExplosionCard(gameId, userId, explosionCard, null);
        }

    }

     */

    /**
     * Removes a player that leaves before losing
     * @param gameId of the game the player is currently in
     * @param userId of the user leaving the game
     */
    @MessageMapping("leaving/{gameId}/{userId}")
    public void handleLeavingUser(
            @DestinationVariable("gameId") Long gameId,
            @DestinationVariable("userId") Long userId) throws IOException, InterruptedException {

        GameEngineController.log.info(String.format("User %s left game %s" , userId, gameId));

        // Handle user leaving an ongoing game session
        gameEngineService.removeUserFromGame(gameId, userId);
    }
}
