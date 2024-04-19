package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.GameState;
import ch.uzh.ifi.hase.soprafs24.entity.Card;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserStats;
import ch.uzh.ifi.hase.soprafs24.event.GameStartEvent;
import ch.uzh.ifi.hase.soprafs24.event.PeekIntoDeckEvent;
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

@Service
@Transactional
public class GameEngineService {

    private GameDeckRepository gameDeckRepository;

    private GameRepository gameRepository;

    private GameDeckService gameDeckService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    Logger logger = LoggerFactory.getLogger(GameDeckService.class);

    @Autowired
    public GameEngineService(@Qualifier("gameRepository") GameRepository gameRepository,
                             GameDeckRepository gameDeckRepository,
                             GameDeckService gameDeckService,
                             ApplicationEventPublisher eventPublisher) {

        this.gameRepository = gameRepository;
        this.gameDeckRepository = gameDeckRepository;
        this.gameDeckService = gameDeckService;
        this.eventPublisher = eventPublisher;
    }

    public Game startGame(Long gameId) throws IOException, InterruptedException {

        Optional<Game> optionalGame = this.gameRepository.findByGameId(gameId);

        // Ensure that the gameId is valid
        if (!optionalGame.isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid GameId provided");
        }

        Game currentGame = optionalGame.get();
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

        // Update lastPlayed and gamesPlayed for each user and update userIds
        for (User player: players) {
            UserStats stats = player.getUserStats();
            stats.setGamesPlayed(stats.getGamesPlayed()+1);

            stats.setLastPlayed(new Date());
            playerIds.add(player.getId());
        }

        // Distribute Cards
        // To Do -- awaiting Jorge

        // Change below to a CardGetDTO or something similar.
        List<CardGetDTO> dummyCardsPlayers = new ArrayList<>();

        // Create dealer pile
        this.gameDeckService.createDealerPile(currentGame);

        // Assign active player
        if (!players.isEmpty()) {
            User firstPlayer = players.iterator().next();
            currentGame.setCurrentTurn(firstPlayer);
        }

        // Publish Event that game Started along with the corresponding cards to a game- and user-specific channel
        for (Long id: playerIds) {
            GameStartEvent gameStartEvent = new GameStartEvent(this, currentGame.getGameId(), id, dummyCardsPlayers);
            eventPublisher.publishEvent(gameStartEvent);
        }

        // Publish event whose turn it is
        // To Do -- implement an event and publish it

        return currentGame;
    }


}
