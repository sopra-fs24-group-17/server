package ch.uzh.ifi.hase.soprafs24.controller;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.GameDTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
public class GameController {

    private final GameService gameService;

    GameController(GameService gameService) {this.gameService = gameService;}

    /**
     * @param token of the user wanting to initiate a new game
     * @param gamePostDTO containing game-mode and max-player count of the game to be created
     * @return a GameGetDTO instance containing gameId, mode, max-player count and  name of the initiating user.
     */
    @PostMapping("/dashboard/games/new")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public GameGetDTO createNewGame(@RequestHeader("token") String token,
                                    @RequestBody GamePostDTO gamePostDTO) {
        Game game = gameService.createNewGame(token, gamePostDTO);
        return GameDTOMapper.INSTANCE.convertEntityToGameGetDTO(game);
    }

    /** Allows a user to join a game. Joining is only possible if the game hasn't started so far (state = Preparation) and if the max. player count has not been reached.
     * @param token of the user joining an existing game
     * @param gameId containing gameId of the existing game
     * @return a GameGetDTO instance containing gameId, mode, max-player count and name of the initiating user.
     */
    @PutMapping("/dashboard/games/join/{gameId}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public GameGetDTO joinGame(@RequestHeader("token") String token,
                               @PathVariable Long gameId) {
        Game updatedGame = gameService.joinGame(token, gameId);
        return GameDTOMapper.INSTANCE.convertEntityToGameGetDTO(updatedGame);
    }

    /**
     * Allows a user to leave a game. Leaving the game is only possible if the game hasn't started so far (state = Preparation)
     * @param token of the user leaving a particular game
     * @param gameId of the game that the user wants to leave.
     * @return a GameGETDTO instance containing gameId, mode, max-player count, name of the initiating user and available slots.
     */
    @PutMapping("/dashboard/games/leave/{gameId}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public GameGetDTO leaveGame(@RequestHeader("token") String token,
                                @PathVariable Long gameId) {
        Game updatedGame = gameService.leaveGame(token, gameId);
        return GameDTOMapper.INSTANCE.convertEntityToGameGetDTO(updatedGame);
    }

    /**
     * API endpoint to fetch all public games that are still in preparation phase
     * @param token of the user requesting to see all public games
     * @return a list of GameGetDTO isntances.
     */
    @GetMapping("dashboard/games")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<GameGetDTO> getAllJoinablePublicGames(@RequestHeader("token") String token) {
        List<Game> preparingGames = gameService.getGames(token);
        List<GameGetDTO> gameGetDTOs = new ArrayList<>();
        for (Game game : preparingGames) {
            gameGetDTOs.add(GameDTOMapper.INSTANCE.convertEntityToGameGetDTO(game));
        }
        return gameGetDTOs;
    }
}
