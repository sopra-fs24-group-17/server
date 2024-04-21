package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.GameState;
import ch.uzh.ifi.hase.soprafs24.entity.Card;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserStats;
import ch.uzh.ifi.hase.soprafs24.event.*;
import ch.uzh.ifi.hase.soprafs24.repository.GameDeckRepository;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.CardGetDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
public class GameEngineService {

    private GameDeckRepository gameDeckRepository;

    private GameRepository gameRepository;

    private GameDeckService gameDeckService;

    private UserService userService;


    @Autowired
    private ApplicationEventPublisher eventPublisher;

    Logger logger = LoggerFactory.getLogger(GameDeckService.class);

    @Autowired
    public GameEngineService(@Qualifier("gameRepository") GameRepository gameRepository,
                             GameDeckRepository gameDeckRepository,
                             GameDeckService gameDeckService,
                             UserService userService,
                             ApplicationEventPublisher eventPublisher) {

        this.gameRepository = gameRepository;
        this.gameDeckRepository = gameDeckRepository;
        this.gameDeckService = gameDeckService;
        this.userService = userService;
        this.eventPublisher = eventPublisher;
    }

    public Game findGameById(Long gameId) {
        Optional<Game> optionalGame = this.gameRepository.findByGameId(gameId);

        // Ensure that the gameId is valid
        if (!optionalGame.isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid GameId provided");
        }
        return optionalGame.get();
    }

    public Game startGame(Long gameId) throws IOException, InterruptedException {

        Game currentGame = findGameById(gameId);
        GameState state = currentGame.getState();

        // Verify that the game can actually be started
        if (!state.equals(GameState.PREPARING)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can't start a game that is beyond the preparation phase");
        }

        // Swap state from preparing to ongoing
        currentGame.setState(GameState.ONGOING);

        // Fetch all active players
        Set<User> players = currentGame.getPlayers();

        List <Long> playerIds = new ArrayList<>();

        // Create dealer pile
        this.gameDeckService.createDealerPile(currentGame);

        // Remove Explosions and Defusions from Deck
        List<Card> explosions = gameDeckService.removeExplosionsFromDealerPile(currentGame.getGameDeck());
        List<Card> defusions = gameDeckService.removeDefusionsFromDealerPile(currentGame.getGameDeck());

        // Update lastPlayed and gamesPlayed for each user and update userIds
        for (User player: players) {

            GameStartEvent gameStartEvent = new GameStartEvent(this, currentGame.getGameId(), player.getId());
            eventPublisher.publishEvent(gameStartEvent);

            UserStats stats = player.getUserStats();
            stats.setGamesPlayed(stats.getGamesPlayed()+1);

            stats.setLastPlayed(new Date());
            playerIds.add(player.getId());

            // Create individual player piles
            List<Card> playerCards = gameDeckService.drawCardsFromDealerPile(currentGame.getGameDeck(), 7);
            List<String> cardValues = new ArrayList<>();

            for (Card card : playerCards) {
                cardValues.add(card.getCode());
            }

            // Add defusion to the player pile
            if (!defusions.isEmpty()) {
                Card defusionCard = defusions.remove(0);
                cardValues.add(defusionCard.getCode());
                playerCards.add(defusionCard);
            }
            gameDeckService.createPlayerPile(currentGame.getGameDeck(), player.getId(), String.join(",", cardValues));

            for (Card card : playerCards) {
                if (card.getInternalCode() == null) {
                    logger.error("Internal code is null for card code: " + card.getCode());
                }
            }

            // Publish cards to user through websocket
            PlayerCardEvent playerCardEvent = new PlayerCardEvent(this, player.getId(), gameId, playerCards);
            eventPublisher.publishEvent(playerCardEvent);
        }

        // Return Explosions and Defusions to DealerPile
        int explosionsToReturn = Math.max(0, players.size() - 1);
        List<Card> explosionsToBeReturned = explosions.stream().limit(explosionsToReturn).toList();

        List<String> cardValues = new ArrayList<>();
        List<Card> combined = Stream.concat(explosionsToBeReturned.stream(), defusions.stream()).toList();

        for (Card card : combined) {
            cardValues.add(card.getCode());
        }
        gameDeckService.returnCardsToDealerPile(currentGame, String.join(",", cardValues));

        // Shuffle Dealer Pile
        gameDeckService.shuffleCardsInPile(currentGame.getGameDeck());

        // Assign active player
        if (!players.isEmpty()) {
            User firstPlayer = players.iterator().next();
            currentGame.setCurrentTurn(firstPlayer);
            gameRepository.saveAndFlush(currentGame);
        }

        // Publish event whose turn it is
        YourTurnEvent yourTurnEvent = new YourTurnEvent(this, currentGame.getCurrentTurn().getId(), currentGame.getGameId());
        eventPublisher.publishEvent(yourTurnEvent);

        return currentGame;
    }

    /**
     * Handles a websocket request from a user who wants to terminate his turn.
     * First checks whether the user invoking a termination of his move was actually the current active player
     * @param gameId of the game in which the user wants to terminate his move.
     * @param userId of the user wanting to terminate his move.
     * @throws IOException
     * @throws InterruptedException
     */
    public void turnValidation(Long gameId, Long userId) throws IOException, InterruptedException{

        User terminatingUser = userService.getUserById(userId);
        Game currentGame = findGameById(gameId);

        if (!userId.equals(currentGame.getCurrentTurn().getId())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "It's not your turn");
        }

        // All players in the game channel are informed that the user terminated his move
        EndTurnEvent endTurnEvent = new EndTurnEvent(this, terminatingUser.getUsername(), gameId);
        eventPublisher.publishEvent(endTurnEvent);

        Set<User> players = currentGame.getPlayers();

        // Assign next player as active player
        User nextPlayer = getNextPlayer(terminatingUser, players);

        if (nextPlayer != null) {
            currentGame.setCurrentTurn(nextPlayer);
            gameRepository.saveAndFlush(currentGame);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No next user found, if just one user is left - terminate game");
        }

        // Publish event whose turn it is (user- and game-specific channel)
        YourTurnEvent yourTurnEvent = new YourTurnEvent(this, currentGame.getCurrentTurn().getId(), currentGame.getGameId());
        eventPublisher.publishEvent(yourTurnEvent);
    }

    /**
     * Helper method to get the next player whose turn it is
     * @param currentUser, a user object of the user terminating his move
     * @param players a linked hash set of the players that are participating in a game
     * @return
     */
    private User getNextPlayer(User currentUser, Set<User> players) {

        Iterator<User> iterator = players.iterator();
        User nextUser = null;
        boolean found = false;

        while (iterator.hasNext()) {
            User user = iterator.next();
            if (found) {
                nextUser = user;
                break;
            }
            if (user.equals(currentUser)) {
                found = true;
            }
        }
        // If current user is last, wrap around to the first user
        if (found && nextUser == null) {
            nextUser = players.iterator().next();
        }
        return nextUser;
    }

    public void userLeavingOngoingGame(Long gameId, Long userId) throws IOException, InterruptedException {

        // Assert first that user was actually part of the game
        User terminatingUser = userService.getUserById(userId);

        Game currentGame = findGameById(gameId);

        if (!currentGame.getState().equals(GameState.ONGOING)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User can only leave ongoing games");
        }

        Set<User> players = currentGame.getPlayers();

        if (!players.contains(terminatingUser)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not part of the game");
        }

        // If it was the turn of the leaving player, invoke turnValidation to find next player
        if (currentGame.getCurrentTurn().getId().equals(userId)) {
            turnValidation(gameId, userId);
        }

        // Remove from the player list for the game
        players.remove(terminatingUser);
        gameRepository.saveAndFlush(currentGame);
        if (players.size() <= 1) {
            // terminate game
            terminatingGame(gameId);
        }
    }

    public void terminatingGame(Long gameId) {

        Game gameToBeTerminated = findGameById(gameId);

        // Assert that just one player is left in the game
        Set<User> players = gameToBeTerminated.getPlayers();

        if (players.size() != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Still more than one active player in the game session found");
        }

        // Set game state to finished
        gameToBeTerminated.setState(GameState.FINISHED);

        // Determine winning player
        User winningUser = players.iterator().next();
        gameToBeTerminated.setWinningPlayer(winningUser);
        winningUser.getUserStats().setGamesWon(winningUser.getUserStats().getGamesWon()+1);

        gameRepository.saveAndFlush(gameToBeTerminated);

        // Publish end game event
        EndGameEvent endGameEvent = new EndGameEvent(this, winningUser.getUsername(), gameId);
        eventPublisher.publishEvent(endGameEvent);
    }
}