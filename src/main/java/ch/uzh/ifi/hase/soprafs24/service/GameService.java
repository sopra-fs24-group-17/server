package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePutDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.GameDTOMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Random;

@Service
@Transactional
public class GameService {

    private final GameRepository gameRepository;

    private final UserService userService;

    public GameService(GameRepository gameRepository, UserService userService) {
        this.gameRepository = gameRepository;
        this.userService = userService;
    }

    /**
     * Creates a new game entity
     * @param token of the user initiating a new game
     * @param gamePostDTO the DTO of the frontend containing (mode and max-player count)
     * @return the newly created game entity
     */
    public Game createNewGame(String token, GamePostDTO gamePostDTO) {

        // First verify that User is authorized to initiate a game
        User verifiedUser = userService.getUserByToken(token);
        Game game = GameDTOMapper.INSTANCE.convertGamePostDTOToEntity(gamePostDTO);

        Long uniqueGameId = generateUniqueGameId();
        game.setGameId(uniqueGameId);

        game.setInitiatingUser(verifiedUser);

        // Add the initiating user to the player set
        game.getPlayers().add(verifiedUser);

        return gameRepository.save(game);
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

    public Game findGameById(String token, GamePutDTO gamePutDTO, Long gameId){
        User verifiedUser = userService.getUserByToken(token);
        Game game = GameDTOMapper.INSTANCE.convertGamePutDTOToEntity(gamePutDTO);

        // Alternatively gameRepository.findByGameId(gameId), but had issues with return type Optional<Game>
        game.getPlayers().add(verifiedUser);

        return gameRepository.save(game);
    }
}
