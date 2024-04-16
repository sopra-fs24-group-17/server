package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Card;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.GameDeck;
import ch.uzh.ifi.hase.soprafs24.event.DrawCardsEvent;
import ch.uzh.ifi.hase.soprafs24.event.ShufflingEvent;
import ch.uzh.ifi.hase.soprafs24.repository.CardRepository;
import ch.uzh.ifi.hase.soprafs24.repository.GameDeckRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.coyote.Response;
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
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class GameDeckService {

    final HttpClient httpClient;

    ObjectMapper objectMapper;

    private GameDeckRepository gameDeckRepository;

    private CardRepository cardRepository;

    private List<Character> suits = new ArrayList<>(List.of('S', 'H', 'C', 'D'));

    private Map<String, String> dictionary = new HashMap<>() {{
        put("A", "bomb");
        put("2", "deactivation");
        put("3", "cat_1");
        put("4", "attack");
        put("5", "skip");
        put("6", "pick");
        put("7", "shuffle");
        put("8", "cat_2");
        put("9", "cat_3");
        put("0", "cat_4");
        put("J", "cat_5");
        put("Q", "future");
        put("K", "no");
        put("X", "deactivation");
    }};

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    Logger logger = LoggerFactory.getLogger(GameDeckService.class);
    public GameDeckService(GameDeckRepository gameDeckRepository, CardRepository cardRepository, ApplicationEventPublisher eventPublisher) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.gameDeckRepository = gameDeckRepository;
        this.cardRepository = cardRepository;
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
        logger.info(newDeckResponse.body());

        // Conversion of response to format suitable for extraction
        JsonNode rootNode = objectMapper.readTree(newDeckResponse.body());
        GameDeck gameDeck = new GameDeck();

        // For init of game deck from scratch we need to remove a deactivation for each player and there should be always 1 less explosion.
        if(init){
            int numPlayers = game.getPlayers().size();
            HttpResponse<String> removeBombRes = null;
            // remove deactivation - better to handle it in the pile creator
            // remove all bombs
            for(int i = 0; i<4; i++){
                String text = "https://www.deckofcardsapi.com/api/deck/%s/pile/%s/draw/?cards=%s";
                String removeBombURI = String.format(text, rootNode.get("deck_id").asText(), "bombs", "A"+suits.get(i));
                HttpRequest removeBombReq = HttpRequest.newBuilder()
                        .GET()
                        .uri(URI.create(removeBombURI))
                        .build();

                removeBombRes = httpClient.send(removeBombReq, HttpResponse.BodyHandlers.ofString());
                logger.info(removeBombRes.body());
            }
            // Refresh info of deck req
            if(removeBombRes != null){
                rootNode = objectMapper.readTree(removeBombRes.body());
            }
        }

        // Extraction of necessary variables
        gameDeck.setDeckID(rootNode.get("deck_id").asText());
        gameDeck.setRemainingCards(rootNode.get("remaining").asLong());
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
    public List<Card> drawCards(GameDeck gameDeck, Integer numberOfCards) throws IOException, InterruptedException {

        if (numberOfCards > gameDeck.getRemainingCards()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Number of cards to be drawn exceeds available cards");
        }

        String drawCardsUri = String.format("https://deckofcardsapi.com/api/deck/%s/draw/?count=%s", gameDeck.getDeckID(), numberOfCards);

        HttpRequest drawCardsRequest = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(drawCardsUri))
                .build();

        HttpResponse<String> drawCardsResponse = httpClient.send(drawCardsRequest, HttpResponse.BodyHandlers.ofString());
        logger.info(drawCardsResponse.body());

        List<Card> cards = parseCards(drawCardsResponse.body(), gameDeck);

        cards = cardRepository.saveAll(cards);
        cardRepository.flush();

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

        if (gameDeck.getRemainingCards() >=1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Shuffle doesn't alter the state of the remaining deck");
        }

        String shuffleCardsUri = String.format("https://www.deckofcardsapi.com/api/deck/%s/shuffle/?remaining=true", gameDeck.getDeckID());

        HttpRequest shuffleCardsRequest = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(shuffleCardsUri))
                .build();

        HttpResponse<String> shuffleCardsResponse = httpClient.send(shuffleCardsRequest, HttpResponse.BodyHandlers.ofString());
        logger.info(shuffleCardsResponse.body());

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
            String cardVal = cardMapper(cardNode.get("code").asText());
            card.setCode(cardNode.get("code").asText());
            card.setValue(cardVal);
            card.setSuit(cardNode.get("suit").asText());
            card.setImage(cardNode.get("image").asText()); // Change this value based on the cardVal and the cards we are going to use
            card.setDeckId(deckId);

            cards.add(card);
        }

        // Update remaining cards in the deck
        gameDeck.setRemainingCards(rootNode.get("remaining").asLong());
        gameDeckRepository.saveAndFlush(gameDeck);

        return cards;
    }

    /**
     * Draw the initial set of piles for the players
     * @param gameDeck object from which the cards were drawn.
     * @return cards objects.
     * @throws IOException
     **/
    // For the moment void. We are storing the cards in each player hand in the http response body
    public void initialDraw(Game game, GameDeck gameDeck) throws IOException, InterruptedException {
        int numPlayers = game.getPlayers().size();
        String player = "player";
        String card_group = "1";
        String giveDeactivation = "https://www.deckofcardsapi.com/api/deck/%s/pile/%s/add/?cards=%s";
        HttpResponse<String> pileRes = null;

        for(int i=0; i<numPlayers;i++){
            // Give each player a deactivation
            if(i%4 == 0){
                card_group = "X"; // We have 5 players
            }
            giveDeactivation = String.format(giveDeactivation, gameDeck.getDeckID(), player+(i+1), card_group+suits.get(i));
            HttpRequest deactivationRequest = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(giveDeactivation))
                    .build();

            HttpResponse<String> deactivationResponse = httpClient.send(deactivationRequest, HttpResponse.BodyHandlers.ofString());
            logger.info(deactivationResponse.body());

            // Give each player the rest of cards
            List<Card> cards = drawCards(gameDeck, 7);

        }
        // Add bombs to the deck
        String returnURI = "https://www.deckofcardsapi.com/api/deck/%s/return/?cards=%s";

        for(int i=0; i<numPlayers-1;i++){
            returnURI = String.format(returnURI, gameDeck.getDeckID(), "A"+suits.get(i));
            HttpRequest returnBombReq = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(returnURI))
                    .build();

            HttpResponse<String> returnBombRes = httpClient.send(returnBombReq, HttpResponse.BodyHandlers.ofString());
            logger.info(returnBombRes.body());
        }
    }

    String cardMapper(String value){
        return dictionary.get(String.valueOf(value.charAt(0)));
    }
}
