package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Card;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.GameDeck;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.event.*;
import ch.uzh.ifi.hase.soprafs24.repository.CardRepository;
import ch.uzh.ifi.hase.soprafs24.repository.GameDeckRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
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
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.Nullable;

@Service
@Transactional
@Slf4j
public class GameDeckService {

    private final HttpClient httpClient;

    ObjectMapper objectMapper;

    private GameDeckRepository gameDeckRepository;

    private CardRepository cardRepository;

    private UserService userService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    public GameDeckService(GameDeckRepository gameDeckRepository, CardRepository cardRepository, UserService userService ,ApplicationEventPublisher eventPublisher, HttpClient httpClient) {
        this.objectMapper = new ObjectMapper();
        this.gameDeckRepository = gameDeckRepository;
        this.cardRepository = cardRepository;
        this.userService = userService;
        this.eventPublisher = eventPublisher;
        this.httpClient = httpClient;
    }

    /**
     * Helper method to build an http request given an input url
     * @param url
     * @return
     */
    public HttpRequest buildGetRequest(String url) {
        return HttpRequest.newBuilder().GET().uri(URI.create(url)).build();
    }

    /**
     * Helper method that updates the card repository
     * @param cards cards to be saved
     * @return List of cards objects.
     */
    @Transactional
    public List<Card> saveCards(List<Card> cards) {
        if (cards == null || cards.isEmpty()) {
            return Collections.emptyList();
        }
        List<Card> savedCards = cardRepository.saveAll(cards);
        cardRepository.flush();
        return savedCards;
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
        HttpRequest newDeckRequest = buildGetRequest(newDeckUri);

        HttpResponse<String> newDeckResponse = httpClient.send(newDeckRequest, HttpResponse.BodyHandlers.ofString());

        // Conversion of response to format suitable for extraction
        JsonNode rootNode = objectMapper.readTree(newDeckResponse.body());
        GameDeck gameDeck = new GameDeck();

        // Extraction of necessary variables
        gameDeck.setDeckID(rootNode.get("deck_id").asText());
        gameDeck.setRemainingCardsDeck(rootNode.get("remaining").asInt());
        gameDeck.setGame(game);
        gameDeck = gameDeckRepository.saveAndFlush(gameDeck);

        log.info(gameDeck.getDeckID());

        return gameDeck;
    }

    /**
     * Allows drawing a specified number of cards from a specific deck.
     * An exception is thrown if the number of cards to be drawn exceeds the available cards in the deck.
     * @param gameDeck object from which the cards shall be drawn.
     * @return the cards objects.
     * @throws IOException
     * @throws InterruptedException
     */
    public List<Card> drawCardsFromDeck(GameDeck gameDeck) throws IOException, InterruptedException {

        String drawCardsFromDeckUri = String.format("https://deckofcardsapi.com/api/deck/%s/draw/?count=%s", gameDeck.getDeckID(), gameDeck.getRemainingCardsDeck());
        HttpRequest drawCardsFromDeckRequest = buildGetRequest(drawCardsFromDeckUri);
        HttpResponse<String> drawCardsFromDeckResponse = httpClient.send(drawCardsFromDeckRequest, HttpResponse.BodyHandlers.ofString());

        List<String> cardsPath = Arrays.asList("cards");
        List<String> additionalInfo = null;
        List<Card> cards = parseCards(gameDeck,drawCardsFromDeckResponse.body(), "deck_id", cardsPath, additionalInfo);

        gameDeck.setRemainingCardsDeck(0);
        gameDeckRepository.saveAndFlush(gameDeck);

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

        String jsonResponse = getRemainingPileStats(gameDeck, 1L);
        Map<String, Integer> parsedPileCardCounts = parsePileCardCounts(jsonResponse);
        Integer dealerCount = parsedPileCardCounts.get("dealer");

        if (numberOfCards > dealerCount) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Number of cards to be drawn exceeds available cards");
        }

        String drawCardsFromPileUri = String.format("https://www.deckofcardsapi.com/api/deck/%s/pile/%s/draw/?count=%s", gameDeck.getDeckID(), gameDeck.getDealerPileId(), numberOfCards);
        HttpRequest drawCardsFromPileRequest = buildGetRequest(drawCardsFromPileUri);
        HttpResponse<String> drawCardsFromPileResponse = httpClient.send(drawCardsFromPileRequest, HttpResponse.BodyHandlers.ofString());

        List<String> cardsPath = List.of("cards");
        List<String> additionalInfo = Arrays.asList("piles", "dealer");
        List<Card> cards = parseCards(gameDeck,drawCardsFromPileResponse.body(), "deck_id", cardsPath, additionalInfo);

        String playerName = "dealer";

        if (gameDeck.getGame().getCurrentTurn() != null) {
            playerName = gameDeck.getGame().getCurrentTurn().getUsername();
        }

        // Publish a draw cards event
        DrawCardsEvent drawCardsEvent = new DrawCardsEvent(this, numberOfCards, gameDeck.getGame().getGameId(), playerName);
        eventPublisher.publishEvent(drawCardsEvent);

        return cards;
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
        HttpRequest shuffleCardsRequest = buildGetRequest(shuffleCardsUri);
        HttpResponse<String> shuffleCardsResponse = httpClient.send(shuffleCardsRequest, HttpResponse.BodyHandlers.ofString());

        Game currentGame = gameDeck.getGame();

        String playerName = "dealer";

        if (currentGame.getCurrentTurn() != null) {
            playerName = currentGame.getCurrentTurn().getUsername();
        }

        // Publish a shuffling event
        ShufflingEvent shufflingEvent = new ShufflingEvent(this, gameDeck.getGame().getGameId(), playerName);
        eventPublisher.publishEvent(shufflingEvent);
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
        HttpRequest createPlayerPileRequest = buildGetRequest(createPlayerPile);

        HttpResponse<String> createPlayerPileResponse = httpClient.send(createPlayerPileRequest, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Generates a dealer pile by transferring all cards from a deck to a pile called "dealer"
     * @param game object for which the dealer pile shall be created
     * @throws IOException
     * @throws InterruptedException
     */
    public void createDealerPile(Game game) throws IOException, InterruptedException {

        // Deplete cardDeck entirely
        List<Card> drawnCards = drawCardsFromDeck(game.getGameDeck());

        // Add cards to the dealer pile
        List<String> cardValues = new ArrayList<>();

        for (Card card : drawnCards) {
            cardValues.add(card.getCode());
        }

        // Concatenates all drawn cards and adds them to the dealer pile. Note: order gets reverted as such.
        String joinedCardValues = String.join(",", cardValues);

        String createDealerPileUri = String.format("https://www.deckofcardsapi.com/api/deck/%s/pile/dealer/add/?cards=%s", game.getGameDeck().getDeckID(), joinedCardValues);
        HttpRequest createDealerPileRequest = buildGetRequest(createDealerPileUri);
        HttpResponse<String> createDealerPileResponse = httpClient.send(createDealerPileRequest, HttpResponse.BodyHandlers.ofString());

        game.getGameDeck().setDealerPileId("dealer");
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

        // Place cards back to the dealer pile
        String returnCards = String.join(",", cardsToBePlacedBackOnDealerPile);
        returnCardsToPile(game.getGameDeck(), "dealer", returnCards);

        // Publish event for peeking into deck
        PeekIntoDeckEvent peekIntoDeckEvent = new PeekIntoDeckEvent(this, game.getGameId(), game.getCurrentTurn().getUsername(), game.getCurrentTurn().getId(), topThreeCards);
        eventPublisher.publishEvent(peekIntoDeckEvent);

        return topThreeCards;
    }

    /**
     * Helper method that places cards on a specified pile.
     * @param gameDeck indicating the playing deck
     * @param pileIdentifier indicating the pile identifier, which could be 'dealer' or a specific userId.
     * @param cardsToBeReturned a string of the cards to be returned, like "AS,KS,X1"
     * @throws IOException
     * @throws InterruptedException
     */
    public void returnCardsToPile(GameDeck gameDeck, String pileIdentifier, String cardsToBeReturned) throws IOException, InterruptedException {
        String returnCardsToPileUri = String.format("https://www.deckofcardsapi.com/api/deck/%s/pile/%s/add/?cards=%s", gameDeck.getDeckID(), pileIdentifier, cardsToBeReturned);
        HttpRequest returnCardsToPileRequest = buildGetRequest(returnCardsToPileUri);
        HttpResponse<String> returnCardsToPileResponse = httpClient.send(returnCardsToPileRequest, HttpResponse.BodyHandlers.ofString());

        // Calculate the number of cards to be returned
        int numCardsToBeReturned = cardsToBeReturned.isEmpty() ? 0 : cardsToBeReturned.split(",").length;

        // Update the remaining dealer cards count
        gameDeck.setRemainingCardsDealerStack(gameDeck.getRemainingCardsDealerStack() + numCardsToBeReturned);
        gameDeckRepository.saveAndFlush(gameDeck);
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
        HttpRequest removeCardsFromPlayerPileRequest = buildGetRequest(removeCardsFromPlayerPileUri);

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
        HttpRequest placeCardsOnPlayPileRequest = buildGetRequest(placeCardsOnPlayPileUri);
        HttpResponse<String> placeCardsOnPlayPileResponse = httpClient.send(placeCardsOnPlayPileRequest, HttpResponse.BodyHandlers.ofString());

        // Publish event(s)
        for (Card card: cards) {
            CardPlayedEvent cardPlayedEvent = new CardPlayedEvent(this, card.getInternalCode(), game.getGameId(), player.getUsername(), card.getCode());
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

        String jsonResponse = getRemainingPileStats(game.getGameDeck(), 1L);
        Map<String, Integer> parsedPileCardCounts = parsePileCardCounts(jsonResponse);

        Integer dealerCount = parsedPileCardCounts.get("dealer");

        // Random placement in the pile
        if (location == 69 || location < -1) {
            returnCardsToPile(game.getGameDeck(), "dealer", cardToBeReturned.getCode());
            shuffleCardsInDealerPile(game.getGameDeck());
        }
        // Return to Top of Stack
        else if (location == 0) {
            returnCardsToPile(game.getGameDeck(), "dealer", cardToBeReturned.getCode());
        }
        // Return to Bottom of Stack
        else if (location == -1 || dealerCount <= location) {
            List<Card> drawnDeck = drawCardsFromDealerPile(game.getGameDeck(), dealerCount);
            Collections.reverse(drawnDeck);
            List<String> cardsToBePlacedBackOnDealerPile = drawnDeck.stream().map(Card::getCode).collect(Collectors.toList());

            String returnCards =  cardToBeReturned.getCode() + "," + String.join(",", cardsToBePlacedBackOnDealerPile);
            returnCardsToPile(game.getGameDeck(), "dealer", returnCards);
        }
        else {
            List<Card> drawnCards = drawCardsFromDealerPile(game.getGameDeck(), location);
            Collections.reverse(drawnCards);
            List<String> cardsToBePlacedBackOnDealerPile = drawnCards.stream().map(Card::getCode).collect(Collectors.toList());

            String returnCards = cardToBeReturned.getCode() + "," + String.join(",", cardsToBePlacedBackOnDealerPile);
            returnCardsToPile(game.getGameDeck(),"dealer", returnCards);
        }

        // Publish event for returning explosion card
        ExplosionReturnedToDeckEvent explosionReturnedToDeckEvent = new ExplosionReturnedToDeckEvent(this, game.getGameId(), game.getCurrentTurn().getUsername());
        eventPublisher.publishEvent(explosionReturnedToDeckEvent);
    }


    /**
     * Helper method that removes specified cards from the specified pile.
     * @param gameDeck indicating the playing deck
     * @param cardsToRemove a comma-separated string of the cards to be removed, e.g., "AS,AH,AC,AD" or "KS,KH,KC,KD,X1,X2"
     * @return List of cards objects.
     * @throws IOException
     * @throws InterruptedException
     */
    public List<Card> removeSpecificCardsFromPile(GameDeck gameDeck, String cardsToRemove, String pileType) throws IOException, InterruptedException {
        String uri = String.format("https://deckofcardsapi.com/api/deck/%s/pile/%s/draw/?cards=%s", gameDeck.getDeckID(), pileType, cardsToRemove);
        HttpRequest request = buildGetRequest(uri);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        List<String> cardsPath = List.of("cards");
        List<String> additionalInfo = Arrays.asList("piles", pileType);
        List<Card> cards = parseCards(gameDeck, response.body(), "deck_id", cardsPath, additionalInfo);

        return saveCards(cards);
    }


    /**
     * Helper method that draws a card from the player pile, either randomly or a specific card.
     * @param gameDeck indicating the playing deck
     * @param userId indicating the user pile the card is to be drawn from
     * @param cardId (optional) indicating the specific card to be drawn. If null or empty, draws a random card.
     * @return Card object.
     * @throws IOException
     * @throws InterruptedException
     */
    public Card drawCardFromPlayerPile(GameDeck gameDeck, Long userId, @Nullable String cardId) throws IOException, InterruptedException {
        String baseUri = "https://www.deckofcardsapi.com/api/deck/%s/pile/%s/draw/";
        String uri;

        if (cardId != null && !cardId.isEmpty()) {
            uri = String.format(baseUri + "?cards=%s", gameDeck.getDeckID(), userId, cardId);
        } else {
            uri = String.format(baseUri + "random/", gameDeck.getDeckID(), userId);
        }

        HttpRequest request = buildGetRequest(uri);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        List<String> cardsPath = List.of("cards");
        List<Card> cards = parseCards(gameDeck, response.body(), "deck_id", cardsPath, null);
        return cards.get(0);
    }

    /**
     * Helper method that validates if the player is in posesion of a defuse card
     * @param gameDeck indicating the playing deck
     * @param userId indicating the active user pile
     * @return String
     * @throws IOException
     * @throws InterruptedException
     */
    public String exploreDefuseCardInPlayerPile(GameDeck gameDeck, Long userId) throws IOException, InterruptedException {

        String listCardInPlayerPileUri = String.format("https://www.deckofcardsapi.com/api/deck/%s/pile/%s/list/", gameDeck.getDeckID(), userId);
        HttpRequest listCardInPlayerPileRequest = buildGetRequest(listCardInPlayerPileUri);
        HttpResponse<String> drawCardFromPlayerResponse = httpClient.send(listCardInPlayerPileRequest, HttpResponse.BodyHandlers.ofString());

        List<String> cardsPath = Arrays.asList("piles", userId.toString(), "cards");
        List<Card> cards = parseCards(gameDeck,drawCardFromPlayerResponse.body(), "deck_id", cardsPath, null);

        for (Card card : cards) {
            if ("defuse".equals(card.getInternalCode())) {
                return card.getCode();
            }
        }

        return null;
    }


    /**
     * Helper method to obtain statistics about a pile
     * @param gameDeck indicating the playing deck
     * @param userId indicating the user the pile belongs to
     * @return Response body of api request
     * @throws IOException
     * @throws InterruptedException
     */
    public String getRemainingPileStats(GameDeck gameDeck, Long userId) throws IOException, InterruptedException {
        String remainingCardStatsUri = String.format("https://www.deckofcardsapi.com/api/deck/%s/pile/%s/list/", gameDeck.getDeckID(), userId);
        HttpRequest remainingCardsStatsRequest = buildGetRequest(remainingCardStatsUri);
        HttpResponse<String> remainingCardsStatsResponse = httpClient.send(remainingCardsStatsRequest, HttpResponse.BodyHandlers.ofString());

        return remainingCardsStatsResponse.body();
    }


    /**
     * Returns top card in the played cards pile
     * @param gameDeck indicating the playing deck
     * @return List of cards objects
     * @throws IOException
     * @throws InterruptedException
     */
    public List<Card> exploreTopCardPlayPile(GameDeck gameDeck) throws IOException, InterruptedException{
        String remainingPlayPileCards = String.format("https://www.deckofcardsapi.com/api/deck/%s/pile/play/list/", gameDeck.getDeckID());
        HttpRequest remainingCardsStatsRequest = buildGetRequest(remainingPlayPileCards);
        HttpResponse<String> remainingPlayCardsResponse = httpClient.send(remainingCardsStatsRequest, HttpResponse.BodyHandlers.ofString());

        List<String> cardsPath = Arrays.asList("piles", "play", "cards");
        List<Card> cards = parseCards(gameDeck, remainingPlayCardsResponse.body(), "deck_id", cardsPath, null);

        if (cards.isEmpty()){
            return null;
        }
        return saveCards(cards);
    }


    /**
     * Helper method to obtain remaining cards of a pile
     * @param jsonResponse response body of a request to the cards api
     * @return Integer - Remaining cards
     * @throws IOException
     */
    public Map<String, Integer> parsePileCardCounts(String jsonResponse) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(jsonResponse);

        JsonNode pilesNode = rootNode.path("piles");
        Map<String, Integer> pileCardCounts = new HashMap<>();

        if (pilesNode.isMissingNode()) {
            throw new IllegalStateException("The 'piles' node is missing");
        }

        pilesNode.fields().forEachRemaining(pile -> {
            String pileName = pile.getKey();
            JsonNode pileDetails = pile.getValue();
            int remainingCards = pileDetails.path("remaining").asInt();
            pileCardCounts.put(pileName, remainingCards);
        });

        return pileCardCounts;
    }

    /**
     * Method that reloads a designated player pile, used to reload the state of a game after connectivity issues.
     * @param gameDeck
     * @param userId
     * @throws IOException
     * @throws InterruptedException
     */
    public void reloadPlayerPile(GameDeck gameDeck, Long userId) throws IOException, InterruptedException {
        String listCardInPlayerPileUri = String.format("https://www.deckofcardsapi.com/api/deck/%s/pile/%s/list/", gameDeck.getDeckID(), userId);
        HttpRequest listCardInPlayerPileRequest = buildGetRequest(listCardInPlayerPileUri);
        HttpResponse<String> drawCardFromPlayerResponse = httpClient.send(listCardInPlayerPileRequest, HttpResponse.BodyHandlers.ofString());

        List<String> cardsPath = Arrays.asList("piles", userId.toString(), "cards");
        List<Card> cards = parseCards(gameDeck,drawCardFromPlayerResponse.body(), "deck_id", cardsPath, null);

        PlayerCardEvent playerCardEvent = new PlayerCardEvent(this, userId, gameDeck.getGame().getGameId(), cards);        eventPublisher.publishEvent(playerCardEvent);
        eventPublisher.publishEvent(playerCardEvent);
    }


    /**
     * General method to parse card data from different types of JSON responses.
     * @param gameDeck the card deck object that is used for the game session
     * @param jsonResponse the JSON response string.
     * @param deckIdKey the JSON key to fetch deck ID.
     * @param cardsKeyPath the JSON path to the cards array.
     * @param additionalPileInfo optional additional JSON path for pile-specific information.
     * @return List of Card objects.
     * @throws IOException
     */
    public List<Card> parseCards(GameDeck gameDeck, String jsonResponse, String deckIdKey, List<String> cardsKeyPath, @Nullable List<String> additionalPileInfo) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(jsonResponse);

        String deckId = rootNode.get(deckIdKey).asText();
        JsonNode cardsNode = rootNode;
        for (String key : cardsKeyPath) {
            cardsNode = cardsNode.path(key);
        }
        List<Card> cards = new ArrayList<>();

        for (JsonNode cardNode : cardsNode) {
            Card card = new Card();
            card.setCode(cardNode.get("code").asText());
            card.setSuit(cardNode.get("suit").asText());
            card.setImage(cardNode.get("image").asText());
            card.setDeckId(deckId);
            cards.add(card);
        }

        if (additionalPileInfo != null) {
            JsonNode additionalNode = rootNode;
            for (String key : additionalPileInfo) {
                additionalNode = additionalNode.path(key);
            }
            int remainingCards = additionalNode.path("remaining").asInt();
            gameDeck.setRemainingCardsDealerStack(remainingCards);
            gameDeckRepository.saveAndFlush(gameDeck);
        }

        return saveCards(cards);
    }
}
