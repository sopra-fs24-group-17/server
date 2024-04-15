package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.GameDeck;
import ch.uzh.ifi.hase.soprafs24.repository.GameDeckRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;
import java.net.URI;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Transactional
public class GameDeckService {

    final HttpClient httpClient;
    ObjectMapper objectMapper;
    private GameDeckRepository gameDeckRepository;
    Logger logger = LoggerFactory.getLogger(GameDeckService.class);
    public GameDeckService(GameDeckRepository gameDeckRepository) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.gameDeckRepository = gameDeckRepository;
    }

    public GameDeck fetchDeck(Game game) throws IOException, InterruptedException {

        String newDeckUri = "https://deckofcardsapi.com/api/deck/new/shuffle/?deck_count=1&jokers_enabled=true";

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(newDeckUri))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        logger.info(response.body());

        // Conversion of response to format suitable for extraction
        JsonNode rootNode = objectMapper.readTree(response.body());
        GameDeck gameDeck = new GameDeck();

        // Extraction of necessary variables
        gameDeck.setDeckID(rootNode.get("deck_id").asText());
        gameDeck.setRemainingCards(rootNode.get("remaining").asLong());
        gameDeck.setGame(game);
        gameDeck = gameDeckRepository.saveAndFlush(gameDeck);
        return gameDeck;
    }
}
