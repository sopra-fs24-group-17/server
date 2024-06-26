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
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class WebSocketService {

    @Autowired
    protected SimpMessagingTemplate sendMessage;

    private void sendWebSocketMessage(String destination, Map<String, Object> params) {
        JSONObject message = new JSONObject();
        params.forEach(message::put);
        this.sendMessage.convertAndSend(destination, message.toString());
    }

    public void sendMessageToClients(String destination, Object dto) {
        this.sendMessage.convertAndSend(destination, dto);
    }

    public void sendMessageJoinedUser(String userName, Long gameId, Integer maxPlayers, Integer currentPlayers) {
        Map<String, Object> params = Map.of(
                "type", "join",
                "userName", userName,
                "gameId", gameId,
                "maxPlayers", maxPlayers,
                "currentPlayers", currentPlayers
        );
        sendWebSocketMessage("/game/" + gameId, params);
    }

    public void sendMessageLeftUser(String userName, Long gameId, Integer maxPlayers, Integer currentPlayers) {
        Map<String, Object> params = Map.of(
                "type", "leave",
                "userName", userName,
                "gameId", gameId,
                "maxPlayers", maxPlayers,
                "currentPlayers", currentPlayers
        );
        sendWebSocketMessage("/game/" + gameId, params);
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
        Map<String, Object> params = Map.of(
                "type", "drawing",
                "gameId", gameId,
                "user", invokingPlayerUserName,
                "numberOfCards", numberOfCards
        );
        sendWebSocketMessage("/game/" + gameId, params);
    }

    public void sendMessageShuffling(Long gameId, String invokingPlayerUserName) {
        Map<String, Object> params = Map.of(
                "type", "shuffling",
                "gameId", gameId,
                "user", invokingPlayerUserName
        );
        sendWebSocketMessage("/game/" + gameId, params);
    }

    public void sendMessagePeekIntoDeck(Long gameId, String invokingPlayerUserName, Long userId, List<Card> futureCards) {
        JSONArray cardsArray = new JSONArray();
        futureCards.forEach(card -> {
            JSONObject cardJson = new JSONObject();
            cardJson.put("code", card.getCode());
            cardJson.put("internalCode", card.getInternalCode());
            cardsArray.put(cardJson);
        });
        Map<String, Object> params = Map.of(
                "type", "peekIntoDeck",
                "gameId", gameId,
                "user", invokingPlayerUserName,
                "cards", cardsArray
        );
        sendWebSocketMessage("/game/" + gameId + "/" + userId, params);
    }

    public void sendMessageExplosionReturnedToDeck(Long gameId, String invokingPlayerUserName) {
        Map<String, Object> params = Map.of(
                "type", "placedBackToDeck",
                "gameId", gameId,
                "user", invokingPlayerUserName
        );
        sendWebSocketMessage("/game/" + gameId, params);
    }

    public void sendMessageGameStarted(Long gameId, Long userId) {
        Map<String, Object> params = Map.of(
                "type", "start",
                "gameId", gameId
        );
        sendWebSocketMessage("/game/" + gameId + "/" + userId, params);
    }

    public void sendMessageYourTurn(Long userId, Long gameId, String userName) {
        Map<String, Object> params = Map.of(
                "type", "startTurn",
                "userId", userId,
                "userName", userName
        );
        sendWebSocketMessage("/game/" + gameId + "/" + userId, params);
    }

    public void setSendMessageEndTurn(Long userId, Long gameId, String userName) {
        Map<String, Object> params = Map.of(
                "type", "endTurn",
                "userId", userId,
                "terminatingUser", userName
        );
        sendWebSocketMessage("/game/" + gameId + "/" + userId, params);
    }

    public void sendMessageEndGame(Long gameId, String userName, List<String> leaderboard) {
        JSONArray leaderboardArray = new JSONArray();
        Collections.reverse(leaderboard);
        int place = 1;
        for (String s : leaderboard) {
            JSONObject position = new JSONObject();
            position.put("username", s);
            position.put("position", place);
            leaderboardArray.put(position);
            place++;
        }
        Map<String, Object> params = Map.of(
                "type", "endGame",
                "winningUser", userName,
                "leaderboard", leaderboardArray
        );
        sendWebSocketMessage("/game/" + gameId, params);
    }

    public void sendMessagePlayerCards(Long gameId, Long userId, List<Card> playerCards) {
        JSONArray cardsArray = new JSONArray();
        playerCards.forEach(card -> {
            JSONObject cardJson = new JSONObject();
            cardJson.put("code", card.getCode());
            cardJson.put("internalCode", card.getInternalCode());
            cardsArray.put(cardJson);
        });
        Map<String, Object> params = Map.of(
                "type", "cards",
                "cards", cardsArray
        );
        sendWebSocketMessage("/game/" + gameId + "/" + userId, params);
    }

    public void sendMessageCardPlayed(Long gameId, String userName, String internalCode, String externalCode) {
        Map<String, Object> params = Map.of(
                "type", "cardPlayed",
                "userName", userName,
                "cardPlayed", internalCode,
                "externalCode", externalCode
        );
        sendWebSocketMessage("/game/" + gameId, params);
    }

    public void sendMessageStolenCard(Long gameId, Long userId, List<Card> stolenCards) {
        JSONArray cardsArray = new JSONArray();
        stolenCards.forEach(card -> {
            JSONObject cardJson = new JSONObject();
            cardJson.put("code", card.getCode());
            cardJson.put("internalCode", card.getInternalCode());
            cardsArray.put(cardJson);
        });
        Map<String, Object> params = Map.of(
                "type", "cardStolen",
                "cards", cardsArray
        );
        sendWebSocketMessage("/game/" + gameId + "/" + userId, params);
    }

    public void sendMessageDefuseCardPlayed(Long gameId, Long userId, List<Card> defuseCard) {
        JSONArray cardsArray = new JSONArray();
        defuseCard.forEach(card -> {
            JSONObject cardJson = new JSONObject();
            cardJson.put("code", card.getCode());
            cardJson.put("internalCode", card.getInternalCode());
            cardsArray.put(cardJson);
        });
        Map<String, Object> params = Map.of(
                "type", "defuseCard",
                "cards", cardsArray
        );
        sendWebSocketMessage("/game/" + gameId + "/" + userId, params);
    }

    public void sendMessageExplosion(Long gameId, String userName) {
        Map<String, Object> params = Map.of(
                "type", "explosion",
                "terminatingUser", userName
        );
        sendWebSocketMessage("/game/" + gameId, params);
    }

    public void sendMessageExplosionIndividual(Long gameId, Long userId) {
        Map<String, Object> params = Map.of(
                "type", "explosion"
        );
        sendWebSocketMessage("/game/" + gameId + "/" + userId, params);
    }

    public void lossEvent(Long gameId, String userName) {
        Map<String, Object> params = Map.of(
                "type", "loss",
                "looserUser", userName
        );
        sendWebSocketMessage("/game/" + gameId, params);
    }

    public void sendGameState(Long gameId, Card topCard, Map<String, Integer> remainingCardStats, Integer numberOfPlayers, List<String> playerNames, List<Long> playerIds, List<String> playerAvatars, String activePlayer) {
        JSONObject pilesJson = new JSONObject();
        JSONArray playerNamesJson = new JSONArray(playerNames);
        JSONObject playersJson = new JSONObject();

        // Populate pilesJson
        remainingCardStats.forEach(pilesJson::put);

        for (int i = 0; i < playerNames.size(); i++) {
            JSONObject playerInfo = new JSONObject();
            playerInfo.put("name", playerNames.get(i));
            playerInfo.put("avatar", playerAvatars.get(i));
            playersJson.put(playerIds.get(i).toString(), playerInfo);
        }

        // Create the params map
        Map<String, Object> params = Map.of(
                "type", "gameState",
                "topCardCode", topCard.getCode(),
                "topCardInternalCode", topCard.getInternalCode(),
                "piles", pilesJson,
                "numberOfPlayers", numberOfPlayers,
                "playerNames", playerNamesJson,  // Keeping this in case you still need it
                "players", playersJson,            // Adding the new playersJson object with names and avatars
                "activePlayer", activePlayer
        );

        // Send the WebSocket message
        sendWebSocketMessage("/game/" + gameId, params);
    }

    public void sendPlacementRequest(Long gameId, Long userId) {
        Map<String, Object> params = Map.of(
                "type", "placementRequest",
                "userId", userId
        );
        sendWebSocketMessage("/game/" + gameId + "/" + userId, params);
    }

    public void sendMessageGetLucky(Long gameId, Long userId, Card randomCard) {
        JSONArray cardsArray = new JSONArray();
        JSONObject cardJson = new JSONObject();
        cardJson.put("code", randomCard.getCode());
        cardJson.put("internalCode", randomCard.getInternalCode());
        cardsArray.put(cardJson);

        Map<String, Object> params = Map.of(
                "type", "cards",
                "gameId", gameId,
                "user", userId,
                "cards", cardsArray
        );
        sendWebSocketMessage("/game/" + gameId + "/" + userId, params);
    }

    public void sendMessageGameCreated(Long gameId) {
        this.sendMessage.convertAndSend("/game/new", gameId);
    }
}
