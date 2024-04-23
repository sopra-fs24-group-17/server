package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Card;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.GameDeck;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.event.*;
import ch.uzh.ifi.hase.soprafs24.repository.CardRepository;
import ch.uzh.ifi.hase.soprafs24.repository.GameDeckRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;
import java.net.URI;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class GameDeckService {

    final HttpClient httpClient;

    ObjectMapper objectMapper;

    private GameDeckRepository gameDeckRepository;

    private CardRepository cardRepository;

    private UserService userService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    Logger logger = LoggerFactory.getLogger(GameDeckService.class);
    public GameDeckService(GameDeckRepository gameDeckRepository, CardRepository cardRepository, UserService userService ,ApplicationEventPublisher eventPublisher) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.gameDeckRepository = gameDeckRepository;
        this.cardRepository = cardRepository;
        this.userService = userService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Fetches a card deck of 54 cards from the external deckofcards api
     * @param game object which the deck will be assigned to
     * @return a gameDeck object
     * @throws IOException
     * @throws InterruptedException
     */
    public GameDeck fetchDeck(Game game, boolean init) throws IOException, InterruptedException {

        String newDeckUri = "https://deckofcardsapi.com/api/deck/new/shuffle/?deck_count=1&jokers_enabled=true";

        HttpRequest newDeckRequest = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(newDeckUri))
                .build();

        HttpResponse<String> newDeckResponse = httpClient.send(newDeckRequest, HttpResponse.BodyHandlers.ofString());

        // Conversion of response to format suitable for extraction
        JsonNode rootNode = objectMapper.readTree(newDeckResponse.body());
        GameDeck gameDeck = new GameDeck();

        // Extraction of necessary variables
        gameDeck.setDeckID(rootNode.get("deck_id").asText());
        gameDeck.setRemainingCards(rootNode.get("remaining").asInt());
        gameDeck.setGame(game);
        gameDeck = gameDeckRepository.saveAndFlush(gameDeck);
        return gameDeck;
    }

    /**
     * Allows drawing a specified number of cards from a specific deck.
     * An exception is thrown if the number of cards to be drawn exceeds the available cards in the deck.
     * @param gameDeck object from which the cards shall be drawn.
     * @param numberOfCards specifying how many cards are to be drawn.
     * @return the cards objects.
     * @throws IOException
     * @throws InterruptedException
     */
    public List<Card> drawCardsFromDeck(GameDeck gameDeck, Integer numberOfCards) throws IOException, InterruptedException {

        if (numberOfCards > gameDeck.getRemainingCards()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Number of cards to be drawn exceeds available cards");
        }

        String drawCardsFromDeckUri = String.format("https://deckofcardsapi.com/api/deck/%s/draw/?count=%s", gameDeck.getDeckID(), numberOfCards);

        HttpRequest drawCardsFromDeckRequest = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(drawCardsFromDeckUri))
                .build();

        HttpResponse<String> drawCardsFromDeckResponse = httpClient.send(drawCardsFromDeckRequest, HttpResponse.BodyHandlers.ofString());

        List<Card> cards = parseCards(drawCardsFromDeckResponse.body(), gameDeck);

        return saveCards(cards);
    }

    /**
     * Allows drawing a specified number of cards from the dealer pile.
     * An exception is thrown if the number of cards to be drawn exceeds the available cards in the pile.
     * @param gameDeck object from whose dealer stack the cards shall be drawn.
     * @param numberOfCards specifying how many cards are to be drawn.
     * @return the cards objects.
     * @throws IOException
     * @throws InterruptedException
     */
    public List<Card> drawCardsFromDealerPile(GameDeck gameDeck, Integer numberOfCards) throws IOException, InterruptedException {
        if (numberOfCards > gameDeck.getRemainingCardsDealerStack()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Number of cards to be drawn exceeds available cards");
        }

        String drawCardsFromPileUri = String.format("https://www.deckofcardsapi.com/api/deck/%s/pile/%s/draw/?count=%s", gameDeck.getDeckID(), gameDeck.getDealerPileId(), numberOfCards);

        HttpRequest drawCardsFromPileRequest = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(drawCardsFromPileUri))
                .build();

        HttpResponse<String> drawCardsFromPileResponse = httpClient.send(drawCardsFromPileRequest, HttpResponse.BodyHandlers.ofString());

        List<Card> cards = parseCardsDealer(drawCardsFromPileResponse.body(), gameDeck);

        List<Card> savedCards = saveCards(cards);

        // Publish a draw cards event
        DrawCardsEvent drawCardsEvent = new DrawCardsEvent(this, numberOfCards, gameDeck.getGame().getGameId(), "placeholder");
        eventPublisher.publishEvent(drawCardsEvent);

        return cards;
    }

    /**
     * Allows shuffling of a card deck
     * @param gameDeck object which shall be reshuffled
     * @throws IOException
     * @throws InterruptedException
     */
    public void shuffleCards(GameDeck gameDeck) throws IOException, InterruptedException {

        if (gameDeck.getRemainingCards() <=1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Shuffle doesn't alter the state of the remaining deck");
        }

        String shuffleCardsUri = String.format("https://www.deckofcardsapi.com/api/deck/%s/shuffle/?remaining=true", gameDeck.getDeckID());

        HttpRequest shuffleCardsRequest = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(shuffleCardsUri))
                .build();

        HttpResponse<String> shuffleCardsResponse = httpClient.send(shuffleCardsRequest, HttpResponse.BodyHandlers.ofString());

        // Publish a shuffling event
        ShufflingEvent shufflingEvent = new ShufflingEvent(this, gameDeck.getGame().getGameId(), "placeholder");
        eventPublisher.publishEvent(shufflingEvent);
    }

    /**
     * Allows shuffling of a card deck
     * @param gameDeck object which shall be reshuffled
     * @throws IOException
     * @throws InterruptedException
     */
    public void shuffleCardsInDealerPile(GameDeck gameDeck) throws IOException, InterruptedException {

        if (gameDeck.getRemainingCardsDealerStack() <=1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Shuffle doesn't alter the state of the remaining pile");
        }

        String shuffleCardsUri = String.format("https://www.deckofcardsapi.com/api/deck/%s/pile/%s/shuffle/", gameDeck.getDeckID(), gameDeck.getDealerPileId());

        HttpRequest shuffleCardsRequest = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(shuffleCardsUri))
                .build();

        HttpResponse<String> shuffleCardsResponse = httpClient.send(shuffleCardsRequest, HttpResponse.BodyHandlers.ofString());

        // Publish a shuffling event
        ShufflingEvent shufflingEvent = new ShufflingEvent(this, gameDeck.getGame().getGameId(), "placeholder");
        eventPublisher.publishEvent(shufflingEvent);
    }

    /**
     * Helper method to parse the response from the draw cards api call.
     * @param jsonResponse the response from invoking the draw cards api.
     * @param gameDeck object from which the cards were drawn.
     * @return cards objects.
     * @throws IOException
     */
    public List<Card> parseCards(String jsonResponse, GameDeck gameDeck) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(jsonResponse);

        String deckId = rootNode.get("deck_id").asText();
        JsonNode cardsNode = rootNode.get("cards");
        List<Card> cards = new ArrayList<>();

        // Extraction of necessary variables
        for (JsonNode cardNode : cardsNode) {
            Card card = new Card();
            card.setCode(cardNode.get("code").asText());
            card.setSuit(cardNode.get("suit").asText());
            card.setImage(cardNode.get("image").asText()); // Change this value based on the cardVal and the cards we are going to use
            card.setDeckId(deckId);

            cards.add(card);
        }

        // Update remaining cards in the deck
        gameDeck.setRemainingCards(rootNode.get("remaining").asInt());
        gameDeckRepository.saveAndFlush(gameDeck);

        return saveCards(cards);
    }

    public List<Card> parseCardsDealer(String jsonResponse, GameDeck gameDeck) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(jsonResponse);

        String deckId = rootNode.get("deck_id").asText();
        JsonNode cardsNode = rootNode.get("cards");
        List<Card> cards = new ArrayList<>();

        // Extraction of necessary variables
        for (JsonNode cardNode : cardsNode) {
            Card card = new Card();
            card.setCode(cardNode.get("code").asText());
            card.setSuit(cardNode.get("suit").asText());
            card.setImage(cardNode.get("image").asText()); // Change this value based on the cardVal and the cards we are going to use
            card.setDeckId(deckId);

            cards.add(card);
        }

        // Update remaining cards in the deck
        JsonNode dealerNode = rootNode.path("piles").path("dealer");
        int remainingCardsDealer = dealerNode.path("remaining").asInt();
        gameDeck.setRemainingCardsDealerStack(remainingCardsDealer);
        gameDeckRepository.saveAndFlush(gameDeck);

        return saveCards(cards);
    }

    /**
     * Creates the piles for the individual players
     * @param gameDeck from which the cards are drawn
     * @param userId acting as a naming variable for the pile id.
     * @throws IOException
     * @throws InterruptedException
     */
    public void createPlayerPile(GameDeck gameDeck, Long userId, String cardsToBeAdded) throws IOException, InterruptedException {

        String createPlayerPile = String.format("https://www.deckofcardsapi.com/api/deck/%s/pile/%s/add/?cards=%s", gameDeck.getDeckID(), userId, cardsToBeAdded);

        HttpRequest createPlayerPileRequest = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(createPlayerPile))
                .build();

        HttpResponse<String> createPlayerPileResponse = httpClient.send(createPlayerPileRequest, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Generates a dealer pile by transferting all cards from a deck to a pile called "dealer"
     * @param game object for which the dealer pile shall be created
     * @throws IOException
     * @throws InterruptedException
     */
    public void createDealerPile(Game game) throws IOException, InterruptedException {

        Integer remainingCardsGameDeck = game.getGameDeck().getRemainingCards();

        // Deplete cardDeck entirely
        List<Card> drawnCards = drawCardsFromDeck(game.getGameDeck(), game.getGameDeck().getRemainingCards());

        // Add cards to the dealer pile
        List<String> cardValues = new ArrayList<>();

        for (Card card : drawnCards) {
            cardValues.add(card.getCode());
        }

        // Concatenates all drawn cards and adds them to the dealer pile. Note: order gets reverted as such.
        String joinedCardValues = String.join(",", cardValues);

        String createDealerPileUri = String.format("https://www.deckofcardsapi.com/api/deck/%s/pile/dealer/add/?cards=%s", game.getGameDeck().getDeckID(), joinedCardValues);

        HttpRequest createDealerPileRequest = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(createDealerPileUri))
                .build();

        HttpResponse<String> createDealerPileResponse = httpClient.send(createDealerPileRequest, HttpResponse.BodyHandlers.ofString());

        game.getGameDeck().setDealerPileId("dealer");
        game.getGameDeck().setRemainingCardsDealerStack(remainingCardsGameDeck);
        game.getGameDeck().setRemainingCards(0);
        gameDeckRepository.saveAndFlush(game.getGameDeck());
    }

    /**
     * This method shall be called dynamically to update the top three cards in the dealer pile.
     * @param game
     * @throws IOException
     * @throws InterruptedException
     */
    public List<Card> peekIntoDealerPile(Game game) throws IOException, InterruptedException {

        List<Card> topThreeCards = drawCardsFromDealerPile(game.getGameDeck(), 3);

        List<String> cardsToBePlacedBackOnDealerPile = new ArrayList<>();

        for (Card card : topThreeCards) {
            cardsToBePlacedBackOnDealerPile.add(card.getCode());
        }

        // Reverse the List to maintain correct ordering
        Collections.reverse(cardsToBePlacedBackOnDealerPile);

        // Place cards back to the dealer pile
        String returnCards = String.join(",", cardsToBePlacedBackOnDealerPile);
        returnCardsToDealerPile(game, returnCards);

        // Publish event for peeking into deck
        PeekIntoDeckEvent peekIntoDeckEvent = new PeekIntoDeckEvent(this, game.getGameId(), game.getCurrentTurn().getUsername(), game.getCurrentTurn().getId(), topThreeCards);
        eventPublisher.publishEvent(peekIntoDeckEvent);

        return topThreeCards;
    }

    /**
     * Helper method that places cards on top of the dealer pile
     * @param game object for which the cards shall be placed on top of the dealer pile.
     * @param cardsToBeReturned a string of the cards to be returned, like "AS,KS,X1"
     * @throws IOException
     * @throws InterruptedException
     */
    public void returnCardsToDealerPile(Game game, String cardsToBeReturned) throws IOException, InterruptedException {

        String returnCardsToPileUri = String.format("https://www.deckofcardsapi.com/api/deck/%s/pile/dealer/add/?cards=%s", game.getGameDeck().getDeckID(), cardsToBeReturned);

        HttpRequest returnCardsToDealerPileRequest = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(returnCardsToPileUri))
                .build();

        HttpResponse<String> returnCardsToDealerPileResponse = httpClient.send(returnCardsToDealerPileRequest, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Helper method that places cards on player piles, whereby player piles are labeled by the respective userId.
     * @param gameDeck indicating the playing deck
     * @param userId indicating the participating user
     * @param cardsToBeReturned indicating the cards to be placed on the user pile
     * @throws IOException
     * @throws InterruptedException
     */
    public void returnCardsToPlayerPile(GameDeck gameDeck, Long userId, String cardsToBeReturned) throws IOException, InterruptedException {

        String returnCardsToPlayerPileUri = String.format("https://www.deckofcardsapi.com/api/deck/%s/pile/%s/add/?cards=%s", gameDeck.getDeckID(), userId, cardsToBeReturned);

        HttpRequest returnCardsToPlayerPileRequest = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(returnCardsToPlayerPileUri))
                .build();

        HttpResponse<String> returnCardsToPlayerPileResponse = httpClient.send(returnCardsToPlayerPileRequest, HttpResponse.BodyHandlers.ofString());

    }

    /**
     * Helper method to remove cards from a player pile (facilitate playing cards and stealing mechanisms)
     * @param game corresponding to the current game session
     * @param userId of the user facilitating a card move
     * @param cardsToBeRemoved indicating the cards to be played
     * @throws IOException
     * @throws InterruptedException
     */
    public void removeCardsFromPlayerPile(Game game, Long userId, String cardsToBeRemoved) throws IOException, InterruptedException {

        String removeCardsFromPlayerPileUri = String.format("https://www.deckofcardsapi.com/api/deck/%s/pile/%s/draw/?cards=%s", game.getGameDeck().getDeckID(), userId, cardsToBeRemoved);

        HttpRequest removeCardsFromPlayerPileRequest = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(removeCardsFromPlayerPileUri))
                .build();

        try {

            HttpResponse<String> removeCardsFromPlayerPileResponse = httpClient.send(removeCardsFromPlayerPileRequest, HttpResponse.BodyHandlers.ofString());

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(removeCardsFromPlayerPileResponse.body());

            // Assert that the user held possessed the cards he played
            if (rootNode.has("success") && !rootNode.get("success").asBoolean()) {
                String errorMessage = rootNode.has("error") ? rootNode.get("error").asText() : "Unknown error";
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Move is invalid, user doesn't poses the card(s) played");
            }
        } catch (IOException | InterruptedException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Move is invalid, user doesn't poses the card(s) played");
        }
    }

    /**
     * Helper method to place cards on the play stack
     * @param game corresponding to the current game session
     * @param cardsPlayed indicating the cards that were played
     * @throws IOException
     * @throws InterruptedException
     */
    public void placeCardsToPlayPile(Game game, Long userId, List<Card> cards, String cardsPlayed) throws IOException, InterruptedException {

        User player = userService.getUserById(userId);

        String placeCardsOnPlayPileUri = String.format("https://www.deckofcardsapi.com/api/deck/%s/pile/play/add/?cards=%s", game.getGameDeck().getDeckID(), cardsPlayed);

        HttpRequest placeCardsOnPlayPileRequest = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(placeCardsOnPlayPileUri))
                .build();

        HttpResponse<String> placeCardsOnPlayPileResponse = httpClient.send(placeCardsOnPlayPileRequest, HttpResponse.BodyHandlers.ofString());

        // Publish event(s)
        for (Card card: cards) {
            CardPlayedEvent cardPlayedEvent = new CardPlayedEvent(this, card.getInternalCode(), game.getGameId(), player.getUsername());
            eventPublisher.publishEvent(cardPlayedEvent);
        }
    }

    /**
     * Allows returning of an explosion card to the dealer pile at an arbitrary location
     * @param game object in which the card is to be returned
     * @param location at which the returned explosion card is to be placed
     * 69 or an integer smaller than -1 -> random location
     * 0 -> top of the pile
     * -1 or a location that exceeds the number of remaining cards in the dealer pile returns it to the bottom of the pile
     *  any other integer > 0 and smaller than the number of remaining cards invokes an exact placement at the desired location.
     * @param cardToBeReturned
     * @throws IOException
     * @throws InterruptedException
     */
    public void returnExplosionCardToDealerPile(Game game, Integer location, Card cardToBeReturned) throws IOException, InterruptedException {

        // Random placement in the pile
        if (location == 69 || location < -1) {
            returnCardsToDealerPile(game, cardToBeReturned.getCode());
            shuffleCardsInDealerPile(game.getGameDeck());
        }
        // Return to Top of Stack
        else if (location == 0) {
            returnCardsToDealerPile(game, cardToBeReturned.getCode());
        }
        // Return to Bottom of Stack
        else if (location == -1 || game.getGameDeck().getRemainingCards() <= location) {
            List<Card> drawnDeck = drawCardsFromDealerPile(game.getGameDeck(), game.getGameDeck().getRemainingCardsDealerStack());
            List<String> cardsToBePlacedBackOnDealerPile = new ArrayList<>();

            for (Card card : drawnDeck) {
                cardsToBePlacedBackOnDealerPile.add(card.getCode());
            }

            String returnCards = String.join(",", cardsToBePlacedBackOnDealerPile) + cardToBeReturned.getCode();
            returnCardsToDealerPile(game, returnCards);
        }
        else {
            List<Card> drawnCards = drawCardsFromDealerPile(game.getGameDeck(), location);
            List<String> cardsToBePlacedBackOnDealerPile = new ArrayList<>();

            for (Card card : drawnCards) {
                cardsToBePlacedBackOnDealerPile.add(card.getCode());
            }

            String returnCards = String.join(",", cardsToBePlacedBackOnDealerPile) + cardToBeReturned.getCode();
            returnCardsToDealerPile(game, returnCards);
        }

        // Publish event for returning explosion card
        ExplosionReturnedToDeckEvent explosionReturnedToDeckEvent = new ExplosionReturnedToDeckEvent(this, game.getGameId(), "placeholder");
        eventPublisher.publishEvent(explosionReturnedToDeckEvent);
    }

    public List<Card> removeExplosionsFromDealerPile(GameDeck gameDeck) throws IOException, InterruptedException {

        String drawExplosionsUri = String.format("https://deckofcardsapi.com/api/deck/%s/pile/dealer/draw/?cards=AS,AH,AC,AD", gameDeck.getDeckID());

        HttpRequest drawExplosionsRequest = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(drawExplosionsUri))
                .build();

        HttpResponse<String> drawExplosionResponse = httpClient.send(drawExplosionsRequest, HttpResponse.BodyHandlers.ofString());

        List<Card> cards = parseCardsDealer(drawExplosionResponse.body(), gameDeck);

        return saveCards(cards);
    }

    public List<Card> removeDefusionsFromDealerPile(GameDeck gameDeck) throws IOException, InterruptedException {

        String drawDefusionsUri = String.format("https://deckofcardsapi.com/api/deck/%s/pile/dealer/draw/?cards=KS,KH,KC,KD,X1,X2", gameDeck.getDeckID());

        HttpRequest drawDefusionsRequest = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(drawDefusionsUri))
                .build();

        HttpResponse<String> drawDefusionResponse = httpClient.send(drawDefusionsRequest, HttpResponse.BodyHandlers.ofString());

        List<Card> cards = parseCardsDealer(drawDefusionResponse.body(), gameDeck);

        return  saveCards(cards);
    }

    public Card drawCardFromPlayerPile(GameDeck gameDeck, Long targetUserId) throws IOException, InterruptedException {
        // Validation on target player should be done at the front end. A player without cards shouldn't be allowed to grab cards from
        String drawCardFromPlayerUri = String.format("https://www.deckofcardsapi.com/api/deck/%s/pile/%s/draw/random", gameDeck.getDeckID(), targetUserId);

        HttpRequest drawCardFromPlayerRequest = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(drawCardFromPlayerUri))
                .build();
        try {
            HttpResponse<String> drawCardFromPlayerResponse = httpClient.send(drawCardFromPlayerRequest, HttpResponse.BodyHandlers.ofString());

            List<Card> cards = parseCards(drawCardFromPlayerResponse.body(), gameDeck);

            return cards.get(0);

        }  catch (IOException | InterruptedException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Targeted user doesn't poses any card");
        }
    }

    @Transactional
    public List<Card> saveCards(List<Card> cards) {
        if (cards == null || cards.isEmpty()) {
            return Collections.emptyList();
        }
        List<Card> savedCards = cardRepository.saveAll(cards);
        cardRepository.flush();
        return savedCards;
    }
}
