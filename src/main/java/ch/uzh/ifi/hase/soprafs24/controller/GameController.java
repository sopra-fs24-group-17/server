package ch.uzh.ifi.hase.soprafs24.controller;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePutDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.GameDTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

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
    public GameGetDTO createNewGame(@RequestHeader("token") String token, @RequestBody GamePostDTO gamePostDTO) {
        Game game = gameService.createNewGame(token, gamePostDTO);
        return GameDTOMapper.INSTANCE.convertEntityToGameGetDTO(game);
    }

    /**
     * @param token of the user joining an existing game
     * @param gamePutDTO containing gameId of the existing game
     * @return a GameGetDTO instance containing gameId, mode, max-player count and  name of the initiating user.
     */
    @PutMapping("/dashboard/games/join/{gameId}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public GameGetDTO joinGame(@RequestHeader("token") String token, @RequestBody GamePutDTO gamePutDTO, @PathVariable Long gameId) {
        Game updatedGame = gameService.findGameById(token, gamePutDTO, gameId);
        return GameDTOMapper.INSTANCE.convertEntityToGameGetDTO(updatedGame);
    }
}
