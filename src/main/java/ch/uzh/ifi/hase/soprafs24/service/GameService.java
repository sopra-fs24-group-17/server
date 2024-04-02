package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class GameService {

    private final GameRepository gameRepository; // Inject your game repository

    public GameService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    public Game createNewGame(/* parameters */) {
        Game game = new Game();
        // Remaining fields to be set

        Long uniqueGameId = generateUniqueGameId();
        game.setGameId(uniqueGameId);

        return gameRepository.save(game);
    }

    private Long generateUniqueGameId() {
        Random random = new Random();
        Long gameId;
        do {
            gameId = 100000L + random.nextLong(900000L); // Generate 6 digit number
        } while (gameRepository.findByGameId(gameId)); // Check uniqueness (maybe there is a better way?)

        return gameId;
    }
}
