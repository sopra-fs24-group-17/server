package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.GameState;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.event.GameCreationEvent;
import ch.uzh.ifi.hase.soprafs24.event.GameJoinEvent;
import ch.uzh.ifi.hase.soprafs24.event.GameLeaveEvent;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePutDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.GameDTOMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.Random;
import java.util.Set;

@Service
@Transactional
public class GameService {

    private final GameRepository gameRepository;

    private final UserService userService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    public GameService(GameRepository gameRepository, UserService userService, ApplicationEventPublisher eventPublisher) {
        this.gameRepository = gameRepository;
        this.userService = userService;
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
        return savedGame;
    }

    /**
     * To be completed @Jorge
     * @param token
     * @param gamePutDTO
     * @param gameId
     * @return
     */
    public Game findGameById(String token, GamePutDTO gamePutDTO, Long gameId) {
        User verifiedUser = userService.verifyUserByToken(token);
        Game game = GameDTOMapper.INSTANCE.convertGamePutDTOToEntity(gamePutDTO);

        // Alternatively gameRepository.findByGameId(gameId), but had issues with return type Optional<Game>
        game.getPlayers().add(verifiedUser);
        gameRepository.saveAndFlush(game);

        // @Jorge
        // TO DO -- Verify State of the Game still allows joining of users
        // TO DO -- Verify max player count hasn't been reached
        // TO DO -- Change the method name to something more meaningful like joinGame()
        // TO DO -- Exception handling
        // TO Do -- Tests

        // After successfully adding a player to the game, publish the event for the EventListener
        GameJoinEvent gameJoinEvent = new GameJoinEvent(this, verifiedUser.getUsername(), gameId);
        eventPublisher.publishEvent(gameJoinEvent);

        return game;
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
        if (!state.equals(null) && !state.equals(GameState.PREPARING)) {
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
