package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Card;
import ch.uzh.ifi.hase.soprafs24.event.GameStartEvent;
import ch.uzh.ifi.hase.soprafs24.eventlistener.GameCreationEventListener;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.CardGetDTO;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class WebSocketService {

    @Autowired
    protected SimpMessagingTemplate sendMessage;

    /**
     * Send a message to client through WS
     * @param destination receiver of the message
     * @param dto DTO object
     */
    public void sendMessageToClients(String destination, Object dto) {
        this.sendMessage.convertAndSend(destination, dto);
    }

    /**
     * Send a message to multiple clients through WS
     * @param destination receiver of the message
     * @param message content of the message
     */
    public void sendMessageToClients(String destination, String message) {
        this.sendMessage.convertAndSend(destination, message);
    }

    /**
     * WS message triggered when a user joined a game
     * @param userName user that joined to the game
     * @param gameId referencing the game the user joined to
     */
    public void sendMessageJoinedUser(String userName, Long gameId) {

        JSONObject message = new JSONObject();
        message.put("type", "join");
        message.put("userName", userName);
        message.put("gameId", gameId);

        this.sendMessage.convertAndSend("/game/" + gameId, message.toString());
    }

    /**
     * WS message triggered when a user leaves a game
     * @param userName user that joined to the game
     * @param gameId referencing the game the user joined to
     */
    public void sendMessageLeftUser(String userName, Long gameId) {

        JSONObject message = new JSONObject();
        message.put("type", "leave");
        message.put("userName", userName);
        message.put("gameId", gameId);

        this.sendMessage.convertAndSend("/game/" + gameId, message.toString());
    }

    /**
     * WS message triggered when a friend is active
     * @param userName user that joined the platform
     * @param userId referencing a user
     */
    public void sendMessageFriendLogin(String userName, Long userId) {
        this.sendMessage.convertAndSend("/login", userName);
    }

    /**
     * WS message triggered when a user is inactive
     * @param userName user that left the platform
     * @param userId referencing a user
     */
    public void sendMessageFriendLogout(String userName, Long userId) {
        this.sendMessage.convertAndSend("/logout", userName);
    }

    /**
     * WS message triggered when a friendship request is accepted
     * @param userName user that accepts the request
     * @param userId referencing the user that sends the request
     */
    public void sendMessageFriendshipRequestAccepted(String userName, Long userId) {
        this.sendMessage.convertAndSend("/friendshiprequest/acceptance/" + userId, userName);
    }

    /**
     * WS message triggered when a friendship request is received
     * @param userName user that accepts the request
     * @param userId referencing the user that sends the request
     */
    public void sendMessageFriendshipRequestReceived(String userName, Long userId) {
        this.sendMessage.convertAndSend("/friendshiprequest/received/" + userId, userName);
    }

    /**
     * WS message triggered cards are drawn from dealer pile
     * @param gameId referencing an active game
     * @param invokingPlayerUserName referencing the user triggering the action
     * @param numberOfCards amount of cards retrieved
     */
    public void sendMessageCardsDrawn(Long gameId, String invokingPlayerUserName, Integer numberOfCards) {
        JSONObject message = new JSONObject();
        message.put("type", "drawing");
        message.put("gameId", gameId);
        message.put("user", invokingPlayerUserName);
        message.put("numberOfCards", numberOfCards);

        this.sendMessage.convertAndSend("/game/" + gameId, message.toString());
    }

    /**
     * WS message triggered when shuffle is performed
     * @param gameId referencing an active game
     * @param invokingPlayerUserName referencing the user triggering the action
     */
    public void sendMessageShuffling(Long gameId, String invokingPlayerUserName) {
        JSONObject message = new JSONObject();
        message.put("type", "shuffling");
        message.put("gameId", gameId);
        message.put("user", invokingPlayerUserName);

        this.sendMessage.convertAndSend("/game/" + gameId, message.toString());
    }

    /**
     * WS message that returns the output of a future card
     * @param gameId referencing an active game
     * @param invokingPlayerUserName referencing the user triggering the action
     * @param userId referencing the user triggering the action
     * @param futureCards next 3 cards in the stack
     */
    public void sendMessagePeekIntoDeck(Long gameId, String invokingPlayerUserName, Long userId, List<Card> futureCards) {
        JSONObject message = new JSONObject();
        message.put("type", "peekIntoDeck");
        message.put("gameId", gameId);
        message.put("user", invokingPlayerUserName);

        JSONArray cardsArray = new JSONArray();
        for (Card card : futureCards) {
            JSONObject cardJson = new JSONObject();
            cardJson.put("code", card.getCode());
            cardJson.put("internalCode", card.getInternalCode());
            cardsArray.put(cardJson);
        }
        message.put("cards", cardsArray);

        this.sendMessage.convertAndSend("/game/" + gameId + "/" + userId, message.toString());
    }

    /**
     * WS message indicating a bomb has returned to the dealer pile
     * @param gameId referencing an active game
     * @param invokingPlayerUserName referencing the user triggering the action
     */
    public void sendMessageExplosionReturnedToDeck(Long gameId, String invokingPlayerUserName) {
        JSONObject message = new JSONObject();
        message.put("type", "placedBackToDeck");
        message.put("gameId", gameId);
        message.put("user", invokingPlayerUserName);

        this.sendMessage.convertAndSend("/game/" + gameId, message.toString());
    }

    /**
     * WS message triggered when a game starts
     * @param gameId referencing an active game
     * @param userId referencing the user triggering the action
     */
    public void sendMessageGameStarted(Long gameId, Long userId) {
        JSONObject message = new JSONObject();
        message.put("type", "start");
        message.put("gameId", gameId);

        this.sendMessage.convertAndSend("/game/" + gameId + "/" + userId, message.toString());
    }

    /**
     * WS message indicating current user turn
     * @param gameId referencing an active game
     * @param userId referencing the active user
     */
    public void sendMessageYourTurn(Long userId, Long gameId) {
        JSONObject message = new JSONObject();
        message.put("type", "startTurn");
        message.put("userId", userId);

        this.sendMessage.convertAndSend("/game/" + gameId + "/" + userId, message.toString());
    }

    /**
     * WS message indicating end of turn
     * @param gameId referencing an active game
     * @param userName referencing the user that just finished a turn
     */
    public void setSendMessageEndTurn(Long gameId, String userName) {
        JSONObject message = new JSONObject();
        message.put("type", "endTurn");
        message.put("terminatingUser", userName);

        this.sendMessage.convertAndSend("/game/" + gameId, message.toString());
    }

    /**
     * WS message indicating a game is over
     * @param gameId referencing an active game
     * @param userName referencing a user
     */
    public void sendMessageEndGame(Long gameId, String userName) {
        JSONObject message = new JSONObject();
        message.put("type", "endGame");
        message.put("winningUser", userName);

        this.sendMessage.convertAndSend("/game/" + gameId, message.toString());
    }

    /**
     * WS message indicating the cards of a player
     * @param gameId referencing an active game
     * @param userId referencing the active user
     * @param playerCards cards currently present in player hand
     */
    public void sendMessagePlayerCards(Long gameId, Long userId, List<Card> playerCards) {
        JSONObject message = new JSONObject();
        message.put("type", "cards");

        JSONArray cardsArray = new JSONArray();
        for (Card card : playerCards) {
            JSONObject cardJson = new JSONObject();
            cardJson.put("code", card.getCode());
            cardJson.put("internalCode", card.getInternalCode());
            cardsArray.put(cardJson);
        }
        message.put("cards", cardsArray);

        this.sendMessage.convertAndSend("/game/" + gameId + "/" + userId, message.toString());
    }

    /**
     * WS message indicating the cards that have been already discarded
     * @param gameId referencing an active game
     * @param userName referencing the active user
     * @param internalCode internal representation of the card
     */
    public void sendMessageCardPlayed(Long gameId, String userName, String internalCode) {
        JSONObject message = new JSONObject();
        message.put("type", "cardPlayed");
        message.put("userName", userName);
        message.put("cardPlayed", internalCode);

        this.sendMessage.convertAndSend("/game/" + gameId, message.toString());
    }

    /**
     * WS message triggered when a favor card is played
     * @param gameId referencing an active game
     * @param userId referencing the user receiving the action
     * @param stolenCards indicating the cards retreived from the player hand.
     */
    public void sendMessageStolenCard(Long gameId, Long userId, List<Card> stolenCards) {
        JSONObject message = new JSONObject();
        message.put("type", "cardStolen");

        JSONArray cardsArray = new JSONArray();
        for (Card card : stolenCards) {
            JSONObject cardJson = new JSONObject();
            cardJson.put("code", card.getCode());
            cardJson.put("internalCode", card.getInternalCode());
            cardsArray.put(cardJson);
        }
        message.put("cards", cardsArray);

        this.sendMessage.convertAndSend("/game/" + gameId + "/" + userId, message.toString());
    }

    /**
     * WS message triggered when a player uses his defuser
     * @param gameId referencing an active game
     * @param userId referencing the user triggering the action
     * @param defuseCard indicating the played card
     */
    public void sendMessageDefuseCardPlayed(Long gameId, Long userId, List<Card> defuseCard) {
        JSONObject message = new JSONObject();
        message.put("type", "defuseCard");

        JSONArray cardsArray = new JSONArray();
        for (Card card : defuseCard) {
            JSONObject cardJson = new JSONObject();
            cardJson.put("code", card.getCode());
            cardJson.put("internalCode", card.getInternalCode());
            cardsArray.put(cardJson);
        }
        message.put("cards", cardsArray);
        this.sendMessage.convertAndSend("/game/" + gameId + "/" + userId, message.toString());
    }

    /**
     * WS message triggered when a explosion is drawn
     * @param gameId referencing an active game
     * @param userName referencing the user triggering the action
     */
    public void sendMessageExplosion(Long gameId, String userName) {
        JSONObject message = new JSONObject();
        message.put("type", "explosion");
        message.put("terminatingUser", userName);

        this.sendMessage.convertAndSend("/game/" + gameId, message.toString());
    }

    /**
     * WS message triggered when a player losses in the game
     * @param gameId referencing an active game
     * @param userName referencing the affected user
     */
    public void sendMessageExplosionIndividual(Long gameId, Long userId) {
        JSONObject message = new JSONObject();
        message.put("type", "explosion");

        this.sendMessage.convertAndSend("/game/" + gameId + "/" + userId, message.toString());
    }

    public void lossEvent(Long gameId, String userName) {
        JSONObject message = new JSONObject();
        message.put("type", "loss");
        message.put("looserUser", userName);

        this.sendMessage.convertAndSend("/game/" + gameId, message.toString());
    }

    /**
     * WS message sending current game state
     * @param gameId referencing an active game
     * @param topCard referencing the last played card
     * @param remainingCardStats indicating cards left in pile
     */
    public void sendGameState(Long gameId, Card topCard, Map<String, Integer> remainingCardStats) {
        JSONObject message = new JSONObject();
        message.put("type", "gameState");

        // Add Info for topCard
        message.put("topCardCode", topCard.getCode());
        message.put("topCardInternalCode", topCard.getInternalCode());

        JSONObject pilesJson = new JSONObject();
        for (Map.Entry<String, Integer> entry : remainingCardStats.entrySet()) {
            pilesJson.put(entry.getKey(), entry.getValue());
        }
        message.put("piles", pilesJson);

        this.sendMessage.convertAndSend("/game/" + gameId, message.toString());

    }

    /**
     * WS message triggered when a game is created
     * @param gameId referencing an active game
     */
    public void sendMessageGameCreated(Long gameId) {
        this.sendMessage.convertAndSend("/game/new", gameId);
    }
}