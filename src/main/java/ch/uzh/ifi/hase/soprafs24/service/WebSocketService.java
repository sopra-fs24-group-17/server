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

import java.util.List;

@Service
@Transactional
public class WebSocketService {

    @Autowired
    protected SimpMessagingTemplate sendMessage;

    public void sendMessageToClients(String destination, Object dto) {
        this.sendMessage.convertAndSend(destination, dto);
    }

    public void sendMessageToClients(String destination, String message) {
        this.sendMessage.convertAndSend(destination, message);
    }

    public void sendMessageJoinedUser(String userName, Long gameId) {

        JSONObject message = new JSONObject();
        message.put("type", "join");
        message.put("userName", userName);
        message.put("gameId", gameId);

        this.sendMessage.convertAndSend("/game/" + gameId, message.toString());
    }

    public void sendMessageLeftUser(String userName, Long gameId) {

        JSONObject message = new JSONObject();
        message.put("type", "leave");
        message.put("userName", userName);
        message.put("gameId", gameId);

        this.sendMessage.convertAndSend("/game/" + gameId, message.toString());
    }

    public void sendMessageFriendLogin(String userName, Long userId) {
        this.sendMessage.convertAndSend("/login", userName);
    }

    public void sendMessageFriendLogout(String userName, Long userId) {
        this.sendMessage.convertAndSend("/logout", userName);
    }

    public void sendMessageFriendshipRequestAccepted(String userName, Long userId) {
        this.sendMessage.convertAndSend("/friendshiprequest/acceptance/" + userId, userName);
    }

    public void sendMessageFriendshipRequestReceived(String userName, Long userId) {
        this.sendMessage.convertAndSend("/friendshiprequest/received/" + userId, userName);
    }

    public void sendMessageCardsDrawn(Long gameId, String invokingPlayerUserName, Integer numberOfCards) {
        JSONObject message = new JSONObject();
        message.put("type", "drawing");
        message.put("gameId", gameId);
        message.put("user", invokingPlayerUserName);
        message.put("numberOfCards", numberOfCards);

        this.sendMessage.convertAndSend("/game/" + gameId, message.toString());
    }

    public void sendMessageShuffling(Long gameId, String invokingPlayerUserName) {
        JSONObject message = new JSONObject();
        message.put("type", "shuffling");
        message.put("gameId", gameId);
        message.put("user", invokingPlayerUserName);

        this.sendMessage.convertAndSend("/game/" + gameId, message.toString());
    }

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

    public void sendMessageExplosionReturnedToDeck(Long gameId, String invokingPlayerUserName) {
        JSONObject message = new JSONObject();
        message.put("type", "placedBackToDeck");
        message.put("gameId", gameId);
        message.put("user", invokingPlayerUserName);

        this.sendMessage.convertAndSend("/game/" + gameId, message.toString());
    }

    public void sendMessageGameStarted(Long gameId, Long userId) {
        JSONObject message = new JSONObject();
        message.put("type", "start");
        message.put("gameId", gameId);

        this.sendMessage.convertAndSend("/game/" + gameId + "/" + userId, message.toString());
    }

    public void sendMessageYourTurn(Long userId, Long gameId) {
        JSONObject message = new JSONObject();
        message.put("type", "startTurn");

        this.sendMessage.convertAndSend("/game/" + gameId + "/" + userId, message.toString());
    }

    public void setSendMessageEndTurn(Long gameId, String userName) {
        JSONObject message = new JSONObject();
        message.put("type", "endTurn");
        message.put("terminatingUser", userName);

        this.sendMessage.convertAndSend("/game/" + gameId, message.toString());
    }

    public void sendMessageEndGame(Long gameId, String userName) {
        JSONObject message = new JSONObject();
        message.put("type", "endGame");
        message.put("winningUser", userName);

        this.sendMessage.convertAndSend("/game/" + gameId, message.toString());
    }

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

    public void sendMessageCardPlayed(Long gameId, String userName, String internalCode) {
        JSONObject message = new JSONObject();
        message.put("type", "cardPlayed");
        message.put("userName", userName);
        message.put("cardPlayed", internalCode);

        this.sendMessage.convertAndSend("/game/" + gameId, message.toString());
    }

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

    public void sendMessageExplosion(Long gameId, String userName) {
        JSONObject message = new JSONObject();
        message.put("type", "explosion");
        message.put("terminatingUser", userName);

        this.sendMessage.convertAndSend("/game/" + gameId, message.toString());
    }

    public void lossEvent(Long gameId, String userName) {
        JSONObject message = new JSONObject();
        message.put("type", "loss");
        message.put("looserUser", userName);

        this.sendMessage.convertAndSend("/game/" + gameId, message.toString());
    }
    
    public void sendMessageGameCreated(Long gameId) {
        this.sendMessage.convertAndSend("/game/new", gameId);
    }
}