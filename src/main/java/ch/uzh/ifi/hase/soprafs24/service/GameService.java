package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.GameMode;
import ch.uzh.ifi.hase.soprafs24.constant.GameState;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.GameDeck;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.event.GameCreationEvent;
import ch.uzh.ifi.hase.soprafs24.event.GameJoinEvent;
import ch.uzh.ifi.hase.soprafs24.event.GameLeaveEvent;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.GameDTOMapper;
import org.hibernate.service.spi.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class GameService {

    private final GameRepository gameRepository;

    private final UserService userService;

    private final GameDeckService gameDeckService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    public GameService(GameRepository gameRepository, UserService userService, GameDeckService gameDeckService, ApplicationEventPublisher eventPublisher) {
        this.gameRepository = gameRepository;
        this.userService = userService;
        this.gameDeckService = gameDeckService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Creates a new game entity
     * @param token of the user initiating a new game
     * @param gamePostDTO the DTO of the frontend containing (mode and max-player count)
     * @return the newly created game entity
     */
    public Game createNewGame(String token, GamePostDTO gamePostDTO) {

        // First verify that User is authorized to initiate a game
        User verifiedUser = userService.verifyUserByToken(token);
        Game game = GameDTOMapper.INSTANCE.convertGamePostDTOToEntity(gamePostDTO);

        Long uniqueGameId = generateUniqueGameId();
        game.setGameId(uniqueGameId);

        game.setInitiatingUser(verifiedUser);

        // Add the initiating user to the player set
        game.getPlayers().add(verifiedUser);
        Game savedGame = gameRepository.save(game);
        eventPublisher.publishEvent(new GameCreationEvent(this, savedGame.getGameId(), verifiedUser.getUsername()));

        try {
            // Create corresponding deck of cards for the game
            GameDeck gameDeck = gameDeckService.fetchDeck(game, true);

            game.setGameDeck(gameDeck);
            Game updatedGame = gameRepository.saveAndFlush(game);
            return savedGame;
        } catch (IOException | InterruptedException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to fetch a GameDeck");
        }
    }

    /**
     * Allows a user to join a game session. Joining is only allowed if the max player count hasn't been reached
     * and the game session is still PREPARING.
     * @param token of the user wanting to join a game session
     * @param gameId of the game that the user wants to join.
     * @return
     */
    public Game joinGame(String token, Long gameId) {
        // First verify that User is authorized to join a game
        User verifiedUser = userService.verifyUserByToken(token);

        Optional<Game> optionalGame = gameRepository.findByGameId(gameId);

        if (!optionalGame.isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid GameId provided");
        }

        Game currentGame = optionalGame.get();
        GameState state = currentGame.getState();

        if (!state.equals(GameState.PREPARING)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can't join a game that is beyond the preparation phase");
        }

        Set<User> players = currentGame.getPlayers();
        if (players.contains(verifiedUser)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is already part of the session");
        }
        if (players.size() >= currentGame.getMaxPlayers()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Game session full");
        } else {
            players.add(verifiedUser);

            GameJoinEvent gameJoinEvent = new GameJoinEvent(this, verifiedUser.getUsername(), gameId);
            eventPublisher.publishEvent(gameJoinEvent);
        }
        gameRepository.save(currentGame);
        return currentGame;
    }

    /**
     * Handles a user requesting to leave a game.
     * Leaving a game is only possible during the Preparation phase.
     * If the latest user leaves a game, the game status is changed to aborted.
     * @param token of the user requesting to leave the game.
     * @param gameId of the game that the user wants to leave.
     * @return the updated game entity.
     */
    public Game leaveGame(String token, Long gameId) {
        // First verify that User is authorized to leave a game
        User verifiedUser = userService.verifyUserByToken(token);

        Optional<Game> optionalGame = gameRepository.findByGameId(gameId);

        if (!optionalGame.isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid GameId provided");
        }

        Game currentGame = optionalGame.get();
        GameState state = currentGame.getState();
        // Verify that the gameState is Preparing, else we can't allow leavings of the game
        if (!state.equals(GameState.PREPARING)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can't leave a game that is beyond the preparation phase");
        }

        Set<User> players = currentGame.getPlayers();
        // Verify if the user requesting to leave a game is currently part of the Players
        if (players.contains(verifiedUser)) {
            players.remove(verifiedUser);

            // After successful leaving of a player, publish the event for the EventListener
            GameLeaveEvent gameLeaveEvent = new GameLeaveEvent(this, verifiedUser.getUsername(), currentGame.getGameId());
            eventPublisher.publishEvent(gameLeaveEvent);

            // If the last player left the game, change the status to aborted
            if (players.isEmpty()) {
                currentGame.setState(GameState.ABORTED);
            }

            gameRepository.save(currentGame);
            return currentGame;
        } else {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not part of the game");
        }
    }

    /**
     * Returns all games that are in the preparing state and have a public game mode.
     * @param token of the user requesting to see all available public games.
     * @return
     */
    public List<Game> getGames(String token) {
        User verifiedUser = userService.verifyUserByToken(token);
        return gameRepository.findByStateAndMode(GameState.PREPARING, GameMode.PUBLIC);
    }

    public Game startTurns(Game game) {
        List<Integer> playerIndices = new ArrayList<>();
        int num_players = game.getPlayers().size();
        for (int i = 0; i < num_players; i++) {
            playerIndices.add(i);
        }
        Collections.shuffle(playerIndices); // Shuffle the indices
        String turns = playerIndices.stream().map(Object::toString).collect(Collectors.joining(" "));
        game.setTurns(turns);
        gameRepository.save(game);
        return game;
    }

    /**
     * Set a new game
     * @return a game with turns and initial hands
     */
//    public Game startGame(String token, Long gameId) throws IOException, InterruptedException {
//        Optional<Game> optionalGame = gameRepository.findByGameId(gameId);
//
//        if (!optionalGame.isPresent()) {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid GameId provided");
//        }
//
//        Game currentGame = optionalGame.get();
//        // Extract pile for each player
//        gameDeckService.initialDraw(currentGame, currentGame.getGameDeck()); // Store which card for each player??
//        // Generates the dealerStack from which cards will be drawn during the game session
//        gameDeckService.createDealerPile(currentGame);
//        // Define turns
//        return startTurns(currentGame);
//    }

    /**
     * Generates a unique, six digit gameId
     * @return a unique, six digit gameId
     */
    private Long generateUniqueGameId() {
        Random random = new Random();
        Long gameId;
        do {
            gameId = 100000L + random.nextLong(900000L); // Generate random 6 digit number
        } while (gameRepository.findByGameId(gameId).isPresent()); //until unique
        return gameId;
    }
}
