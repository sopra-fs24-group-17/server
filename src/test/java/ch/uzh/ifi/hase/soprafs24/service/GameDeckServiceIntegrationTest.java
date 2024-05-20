package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.GameState;
import ch.uzh.ifi.hase.soprafs24.entity.Card;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.GameDeck;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePostDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.*;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("dev")
@ExtendWith(MockitoExtension.class)
public class GameDeckServiceIntegrationTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GameRepository cardRepository;

    @Mock
    private GameRepository userRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private GameService gameService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private GameEngineService gameEngineService;

    @Mock
    private GameDeckService gameDeckService;

    private User mockUser;
    private GamePostDTO gamePostDTO;

    private Game game;

    private Long gameId = 1L;

    @BeforeEach
    public void setup() {
        mockUser = new User();
        mockUser.setUsername("TestUser");
        gamePostDTO = new GamePostDTO();

        game = new Game();
        game.setGameId(gameId);
        game.setState(GameState.ONGOING);
        List<User> players = new ArrayList<>();
        players.add(mockUser);
        game.setPlayers(players);
    }

    @Test
    public void gameDeckServiceIntegrationTest() throws IOException, InterruptedException {
        when(gameDeckService.fetchDeck(any(Game.class), eq(true))).thenReturn(new GameDeck());
        when(gameDeckService.drawCardsFromDealerPile(any(GameDeck.class), eq(1))).thenReturn(new ArrayList<Card>(Arrays.asList(new Card())));

        // Create a game
        Game game = new Game();
        // Add users
        User anotherUser = new User();
        game.getPlayers().add(anotherUser);
        game.getPlayers().add(mockUser);

        // Create a Deck
        GameDeck fetchedDeck = gameDeckService.fetchDeck(game, true);

        // Create a Dealer Pile
        gameDeckService.createDealerPile(game);

        // Draw a card from dealer pile
        List <Card> obtainedCards = gameDeckService.drawCardsFromDealerPile(fetchedDeck, 1);

        // Share cards with the users
        gameDeckService.createPlayerPile(fetchedDeck, mockUser.getId(), obtainedCards.get(0).getCode());

        assertNotNull(fetchedDeck);
        assertNotNull(obtainedCards);
        assertEquals(2, game.getPlayers().size());
        assertEquals(1, obtainedCards.size());

    }
}