package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.GameState;
import ch.uzh.ifi.hase.soprafs24.entity.*;
import ch.uzh.ifi.hase.soprafs24.event.*;
import ch.uzh.ifi.hase.soprafs24.repository.CardRepository;
import ch.uzh.ifi.hase.soprafs24.repository.GameDeckRepository;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

@Service
@Slf4j
@Transactional
public class GameEngineService {

    private GameDeckRepository gameDeckRepository;

    private GameRepository gameRepository;

    private UserRepository userRepository;

    private CardRepository cardRepository;

    private GameDeckService gameDeckService;

    private UserService userService;

    private GameService gameService;


    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    public GameEngineService(@Qualifier("gameRepository") GameRepository gameRepository,
                             GameDeckRepository gameDeckRepository,
                             CardRepository cardRepository,
                             UserRepository userRepository,
                             GameDeckService gameDeckService,
                             UserService userService,
                             ApplicationEventPublisher eventPublisher,
                                GameService gameService) {

        this.gameRepository = gameRepository;
        this.gameDeckRepository = gameDeckRepository;
        this.cardRepository = cardRepository;
        this.userRepository = userRepository;
        this.gameDeckService = gameDeckService;
        this.userService = userService;
        this.eventPublisher = eventPublisher;
        this.gameService = gameService;
    }

    /**
     * Returns a game instance based on an id
     * @param gameId referencing a Game
     * @return Game object
     */
    public Game findGameById(Long gameId) {
        Optional<Game> optionalGame = this.gameRepository.findByGameId(gameId);

        // Ensure that the gameId is valid
        if (!optionalGame.isPresent()) {
            log.info("Invalid GameId provided");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid GameId provided");
        }
        return optionalGame.get();
    }

    /**
     * Helper method to get the next player whose turn it is
     * @param currentUser, a user object of the user terminating his move
     * @param players a linked hash set of the players that are participating in a game
     * @return
     */
    private User getNextPlayer(User currentUser, List<User> players) {
        if (players.isEmpty() || (players.size() == 1 && players.contains(currentUser))) {
            return null;
        }
        int currentIndex = players.indexOf(currentUser);
        int nextIndex = (currentIndex + 1) % players.size();
        return players.get(nextIndex);
    }

    /**
     * Transform a game in preparing state to an ongoing game
     * @param gameId referencing a Game
     * @return Game object
     * @throws IOException
     * @throws InterruptedException
     */
    public Game startGame(Long gameId) throws IOException, InterruptedException {

        Game currentGame = findGameById(gameId);
        GameState state = currentGame.getState();

        // Verify that the game can actually be started
        if (!state.equals(GameState.PREPARING)) {
            log.info("Can't start a game that is beyond the preparation phase");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can't start a game that is beyond the preparation phase");
        }

        // Swap state from preparing to ongoing
        currentGame.setState(GameState.ONGOING);

        // Create dealer pile
        this.gameDeckService.createDealerPile(currentGame);

        // Remove Explosions and Defusions from Deck
        List<Card> explosions = gameDeckService.removeSpecificCardsFromPile(currentGame.getGameDeck(), "AS,AH,AC,AD", "dealer");
        List<Card> defusions = gameDeckService.removeSpecificCardsFromPile(currentGame.getGameDeck(), "KS,KH,KC,KD,X1,X2", "dealer");

        // Fetch all active players
        List<User> players = currentGame.getPlayers();

        for (User player: players) {

            GameStartEvent gameStartEvent = new GameStartEvent(this, currentGame.getGameId(), player.getId());
            eventPublisher.publishEvent(gameStartEvent);

            UserStats stats = player.getUserStats();
            stats.setGamesPlayed(stats.getGamesPlayed()+1);

            stats.setLastPlayed(new Date());

            // Create individual player piles
            List<Card> playerCards = gameDeckService.drawCardsFromDealerPile(currentGame.getGameDeck(), 5);
            List<String> cardValues = new ArrayList<>();

            for (Card card : playerCards) {
                cardValues.add(card.getCode());
            }
            // Add one defusion card to the player pile
            if (!defusions.isEmpty()) {
                Card defusionCard = defusions.remove(0);
                cardValues.add(defusionCard.getCode());
                playerCards.add(defusionCard);
            }
            gameDeckService.createPlayerPile(currentGame.getGameDeck(), player.getId(), String.join(",", cardValues));

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
        gameDeckService.returnCardsToPile(currentGame.getGameDeck(), "dealer", String.join(",", cardValues));

        // Shuffle Dealer Pile
        gameDeckService.shuffleCardsInDealerPile(currentGame.getGameDeck());

        // Assign active player
        if (!players.isEmpty()) {
            User firstPlayer = players.iterator().next();
            currentGame.setCurrentTurn(firstPlayer);
            gameRepository.saveAndFlush(currentGame);
        }

        // Publish event whose turn it is
        YourTurnEvent yourTurnEvent = new YourTurnEvent(this, currentGame.getCurrentTurn().getId(), currentGame.getGameId(), currentGame.getCurrentTurn().getUsername());
        eventPublisher.publishEvent(yourTurnEvent);

        return currentGame;
    }

    public boolean turnValidation(Long gameId, Long userId) throws IOException, InterruptedException {
        return turnValidation(gameId, userId, false);
    }
    /**
     * Handles a websocket request from a user who wants to terminate his turn.
     * First checks whether the user invoking a termination of his move was actually the current active player
     * @param gameId of the game in which the user wants to terminate his move.
     * @param userId of the user wanting to terminate his move.
     * @throws IOException
     * @throws InterruptedException
     */
    public boolean turnValidation(Long gameId, Long userId, Boolean skipPlayed) throws IOException, InterruptedException{

        User terminatingUser = userService.getUserById(userId);
        Game currentGame = findGameById(gameId);

        if (!userId.equals(currentGame.getCurrentTurn().getId())) {
            log.info("It's not your turn");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "It's not your turn");
        }

        // All players in the game channel are informed that the user terminated his move
        EndTurnEvent endTurnEvent = new EndTurnEvent(this, terminatingUser.getUsername(), gameId, userId);
        eventPublisher.publishEvent(endTurnEvent);

        User nextPlayer = getNextPlayer(terminatingUser, currentGame.getPlayers());

        if (currentGame.isRepeatTurn()) {
            nextPlayer = terminatingUser;

            if (!skipPlayed) {
                drawCardMoveTermination(gameId, userId, false);
            }
          
            currentGame.setRepeatTurn(false);
            gameRepository.saveAndFlush(currentGame);
        }

        if (currentGame.isAttacked()) {
            currentGame.setRepeatTurn(true);
            currentGame.setAttacked(false);
            gameRepository.saveAndFlush(currentGame);
        }

    if (nextPlayer != null) {
        currentGame.setCurrentTurn(nextPlayer);
        gameRepository.saveAndFlush(currentGame);
    } else {
        terminatingGame(gameId); // Just one user left, initiate end of game and evaluation
    }

    // Publish event whose turn it is (user- and game-specific channel)
    YourTurnEvent yourTurnEvent = new YourTurnEvent(this, currentGame.getCurrentTurn().getId(), currentGame.getGameId(), currentGame.getCurrentTurn().getUsername());
    eventPublisher.publishEvent(yourTurnEvent);

    return true;
}

    /**
     * Implements the final draw that indicates end of turn
     * @param gameId referencing a game instance
     * @param userId referencing the user that triggered the action
     * @return String containing the drawn card
     * @throws IOException
     * @throws InterruptedException
     */
    public String drawCardMoveTermination(Long gameId, Long userId, boolean random) throws IOException, InterruptedException {

        Game game = findGameById(gameId);

        List<Card> cards = gameDeckService.drawCardsFromDealerPile(game.getGameDeck(), 1, random);
        gameDeckService.returnCardsToPile(game.getGameDeck(), userId.toString(), cards.get(0).getCode());

        if (Objects.equals(cards.get(0).getInternalCode(), "explosion")) {
            return cards.get(0).getCode();
        }

        PlayerCardEvent playerCardEvent = new PlayerCardEvent(this, userId, gameId, cards);
        eventPublisher.publishEvent(playerCardEvent);

        return null;
    }

    /**
     * Remove a user from an ongoing game
     * @param gameId referencing a Game
     * @param userId referencing the user to be removed
     * @throws IOException
     * @throws InterruptedException
     */
    public void removeUserFromGame(Long gameId, Long userId) throws IOException, InterruptedException {

        // Assert first that user was actually part of the game
        User terminatingUser = userService.getUserById(userId);

        Game currentGame = findGameById(gameId);

        if (!currentGame.getState().equals(GameState.ONGOING)) {
            log.info("User can only leave ongoing games");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User can only leave ongoing games");
        }

        List<User> players = currentGame.getPlayers();

        if (!players.contains(terminatingUser)) {
            log.info("User is not part of the game");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not part of the game");
        }

        // If it was the turn of the leaving player, invoke turnValidation to find next player
        if (currentGame.getCurrentTurn().getId().equals(userId)) {
            turnValidation(gameId, userId);
        }

        // Add loosing user to the leaderboard
        gameService.addPlayerLeaderboard(currentGame, terminatingUser.getUsername());

        // Remove from the player list for the game
        players.remove(terminatingUser);
        gameRepository.saveAndFlush(currentGame);
        if (players.size() <= 1) {
            // terminate game
            terminatingGame(gameId);
        }

        // Publish Loss Event
        LossEvent lossEvent = new LossEvent(this, gameId, terminatingUser.getUsername());
        eventPublisher.publishEvent(lossEvent);
    }

    /**
     * Terminates an ongoing game
     * @param gameId referencing a Game
     */
    public void terminatingGame(Long gameId) {

        Game gameToBeTerminated = findGameById(gameId);

        // Assert that just one player is left in the game
        List<User> players = gameToBeTerminated.getPlayers();

        if (players.size() > 1) {
            log.info("Still more than one active player in the game session found");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Still more than one active player in the game session found");
        }

        // Set game state to finished
        gameToBeTerminated.setState(GameState.FINISHED);

        // Determine winning player
        User winningUser = players.iterator().next();
        gameToBeTerminated.setWinningPlayer(winningUser);
        winningUser.getUserStats().setGamesWon(winningUser.getUserStats().getGamesWon()+1);

        // Retrieve leaderboard
        List<String> leaderboard = gameToBeTerminated.getLeaderboard();
        leaderboard.add(winningUser.getUsername());
        log.info(String.format("Game %s finished with following leaderboard %s", gameId, String.join(",", leaderboard)));

        gameRepository.saveAndFlush(gameToBeTerminated);

        // Publish end game event
        EndGameEvent endGameEvent = new EndGameEvent(this, winningUser.getUsername(), gameId, leaderboard);
        eventPublisher.publishEvent(endGameEvent);
    }

    /**
     * Return a list of cards with internal code
     * @param cardsToBeTransformed cards without internal code
     * @return List of cards objects with internal code
     */
    public List<Card> transformCardsToInternalRepresentation(List<String> cardsToBeTransformed) {

        List<Card> cardsPlayed = new ArrayList<>();

        for (String cardId : cardsToBeTransformed) {
            Card transformedCard = this.cardRepository.findByCode(cardId);
            cardsPlayed.add(transformedCard);
        }
        return cardsPlayed;
    }

    /**
     * Handler of shuffle card
     * @param game currently active game
     * @param userId referencing the user that triggered the action
     * @throws IOException
     * @throws InterruptedException
     */
    public void handleShuffleCard(Game game, Long userId) throws IOException, InterruptedException {
        gameDeckService.shuffleCardsInDealerPile(game.getGameDeck());
    }

    /**
     * Handler of the get lucky card
     * @param game currently active game
     * @param userId referencing the user that triggered the action
     * @throws IOException
     * @throws InterruptedException
     */
    public void handleLuckyCard(Game game, Long userId) throws IOException, InterruptedException {
        // Draw a random card
        String explosionCard = drawCardMoveTermination(game.getGameId(), userId, true);

        if (explosionCard != null) {
            // Handle explosion
            handleExplosionCard(game.getGameId(), userId, explosionCard);
        }
        else {
            // Handle turnValidation (finding next player and communicating through websocket)
            turnValidation(game.getGameId(), userId);

            // Dispatch gameState
            dispatchGameState(game.getGameId(), userId);
        }
    }

    /**
     * Handler of the look to the future card
     * @param game currently active game
     * @param userId referencing the user that triggered the action
     * @throws IOException
     * @throws InterruptedException
     */
    public void handleFutureCard(Game game, Long userId) throws IOException, InterruptedException {
        gameDeckService.peekIntoDealerPile(game);
    }

    /**
     * Handler of the skip turn card
     * @param game currently active game
     * @param userId referencing the user that triggered the action
     * @throws IOException
     * @throws InterruptedException
     */
    public void handleSkipCard(Game game, Long userId) throws IOException, InterruptedException {
        SkipEvent skipEvent = new SkipEvent(this, game.getGameId(), game.getCurrentTurn().getUsername());
        eventPublisher.publishEvent(skipEvent);
        turnValidation(game.getGameId(), userId, true);
    }

    /**
     * Handler of attack card
     * @param game currently active game
     * @param userId referencing the user that triggered the action
     * @throws IOException
     * @throws InterruptedException
     */
    public void handleAttackCard(Game game, Long userId) throws IOException, InterruptedException {
        User currentUser = userRepository.findUserById(userId);
        User nextUser = getNextPlayer(currentUser, game.getPlayers());

        String nextUserUserName = "placeholder";

        if (nextUser != null) {
            nextUserUserName = nextUser.getUsername();
        }
        // Make the next user to grab two cards from pile
        game.setAttacked(true);
        game.setRepeatTurn(false);
        gameRepository.saveAndFlush(game);

        // To Do - Trigger Attack Event but on next user channel.
        AttackEvent attackEvent = new AttackEvent(this, game.getGameId(), game.getCurrentTurn().getUsername(), nextUserUserName);
        eventPublisher.publishEvent(attackEvent);

        turnValidation(game.getGameId(), userId);
    }

    /**
     * Handler of the favor card
     * @param game currently active game
     * @param userId referencing the user that triggered the action
     * @throws IOException
     * @throws InterruptedException
     */
    public void handleFavorCard(Game game, Long userId, String targetUserName) throws IOException, InterruptedException {

        User targetUser = userRepository.findByUsername(targetUserName);
        Long targetUserId = targetUser.getId();

        List<Long> playerIds = game.getPlayers().stream().map(User::getId).toList();

        // Assert that the targetUser is still part of the game
        if(!playerIds.contains(targetUserId)) {
            log.info("Targeted User is not part of the game");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Targeted User is not part of the game");
        }

        // Grab a random card
        Card randomCard = gameDeckService.drawCardFromPlayerPile(game.getGameDeck(), targetUserId, null);
        List<Card> randomCards = new ArrayList<>(List.of(randomCard));

        // Publish a steal card event
        StealCardEvent stealCardEvent = new StealCardEvent(this, targetUserId, game.getGameId(), randomCards);
        eventPublisher.publishEvent(stealCardEvent);

        // Give it to triggering user
        gameDeckService.returnCardsToPile(game.getGameDeck(), userId.toString(), randomCard.getCode());

        // Publish a draw cards event
        PlayerCardEvent playerCardEvent = new PlayerCardEvent(this, userId, game.getGameId(), randomCards);
        eventPublisher.publishEvent(playerCardEvent);
    }

    public void handleExplosionPlacement(Long gameId, Long userId, Integer placementPosition) throws IOException, InterruptedException {
        Game game = findGameById(gameId);

        // take top card from playPile
        List<Card> topCard = gameDeckService.exploreTopCardPlayPile(game.getGameDeck());
        log.info(topCard.get(0).getCode());
        log.info(topCard.get(topCard.size()-1).getCode());

        List<String> cardValues = new ArrayList<>();
        cardValues.add(topCard.get(topCard.size() - 1).getCode());

        List<Card> explosionCard = gameDeckService.removeSpecificCardsFromPile(game.getGameDeck(),String.join(",", cardValues), "play");

        // take defuseCard from player and put to playPile
        String defuseCard = gameDeckService.exploreDefuseCardInPlayerPile(game.getGameDeck(), userId);

        if (defuseCard == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User doesn't hold any defuse card");
        }

        Card drawnCard = gameDeckService.drawCardFromPlayerPile(game.getGameDeck(), userId,defuseCard);
        List<Card> drawnCards = new ArrayList<>();
        drawnCards.add(drawnCard);
        gameDeckService.placeCardsToPlayPile(game, userId, drawnCards, drawnCard.getCode());

        // return explosion card according to user request
        gameDeckService.returnExplosionCardToDealerPile(game, placementPosition, explosionCard.get(0));
        dispatchGameState(gameId, userId);
        turnValidation(gameId, userId);
    }

    /**
     * Handler of the bomb card
     * @param gameId referencing a game instance
     * @param userId referencing the user that triggered the action
     * @param explosionId referencing an explosion card
     * @throws IOException
     * @throws InterruptedException
     */
    public void handleExplosionCard(Long gameId, Long userId, String explosionId) throws IOException, InterruptedException {
        Game game = findGameById(gameId);

        User explodedUser = userRepository.findUserById(userId);

        ExplosionEvent explosionEvent = new ExplosionEvent(this, gameId, explodedUser.getUsername());
        eventPublisher.publishEvent(explosionEvent);

        ExplosionEventIndividual explosionEventIndividual = new ExplosionEventIndividual(this, gameId, explodedUser.getId());
        eventPublisher.publishEvent(explosionEventIndividual);

        // Browse Pile of the Exploding User
        String defuseCard = gameDeckService.exploreDefuseCardInPlayerPile(game.getGameDeck(), userId);

        // If he has a defuse card, request him to play the defuse card
        if (defuseCard != null) {
            // send placementRequestEvent to client

            // put explosion card to playPile
            Card explosionCard = new Card();
            explosionCard.setCode(explosionId);
            explosionCard.setInternalCode("explosion");

            List<Card> playedCards = new ArrayList<>();
            playedCards.add(explosionCard);

            gameDeckService.placeCardsToPlayPile(game, userId, playedCards, explosionId);

            // dispatch GameState
            dispatchGameState(gameId, userId);

            // send placementRequest
            PlacementEvent placementEvent = new PlacementEvent(this, gameId, userId);
            eventPublisher.publishEvent(placementEvent);

        } else {
            // If he has no defuse card, put the user out of the game
            removeUserFromGame(game.getGameId(), userId);
        }
    }

    /**
     * Returns current game state
     * @param gameId referencing a game instance
     * @param userId referencing the user that triggered the action
     * @throws IOException
     * @throws InterruptedException
     */
    public void dispatchGameState(Long gameId, Long userId) throws IOException, InterruptedException {
        Game game = findGameById(gameId);
        String jsonResponse = gameDeckService.getRemainingPileStats(game.getGameDeck(), userId);
        Map<String, Integer> parsedPileCardCounts = gameDeckService.parsePileCardCounts(jsonResponse);

        Integer dealerCount = parsedPileCardCounts.get("dealer");
        game.getGameDeck().setRemainingCardsDealerStack(dealerCount);
        gameDeckRepository.saveAndFlush(game.getGameDeck());

        String activePlayer = game.getCurrentTurn().getUsername();

        List<Card> topCardsPlayPile = gameDeckService.exploreTopCardPlayPile(game.getGameDeck());
        Card topCardPlayPile;
        if (topCardsPlayPile == null) {
            topCardPlayPile = new Card();
            topCardPlayPile.setCode("");
            topCardPlayPile.setInternalCode("");
        } else {
            topCardPlayPile = topCardsPlayPile.get(topCardsPlayPile.size() - 1);
        }

        Integer numberPlayers = game.getPlayers().size();

        List<String> usernames = game.getPlayers().stream().map(User::getUsername).toList();
        List<String> avatars = game.getPlayers().stream().map(User::getAvatar).toList();

        List<Long> userIds = game.getPlayers().stream().map(User::getId).toList();

        // Publish Event
        GameStateEvent gameStateEvent = new GameStateEvent(this, gameId,topCardPlayPile, parsedPileCardCounts, numberPlayers, usernames, userIds, avatars, activePlayer);
        eventPublisher.publishEvent(gameStateEvent);
    }

    /**
     * Reloads the state of a cached game
     * @param gameId
     * @param userId
     * @throws IOException
     * @throws InterruptedException
     */
    public void reloadGameState(Long gameId, Long userId) throws IOException, InterruptedException {
        Game game = findGameById(gameId);

        if (!game.getState().equals(GameState.ONGOING)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can only reload state for an ongoing game");
        }

        List<Long> playerIds = game.getPlayers().stream().map(User::getId).toList();

        if(!playerIds.contains(userId)) {
            log.info("Targeted User is not part of the game");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Targeted User is not part of the game");
        }

        gameDeckService.reloadPlayerPile(game.getGameDeck(), userId);
        dispatchGameState(gameId,userId);

        YourTurnEvent yourTurnEvent = new YourTurnEvent(this, game.getCurrentTurn().getId(), game.getGameId(), game.getCurrentTurn().getUsername());
        eventPublisher.publishEvent(yourTurnEvent);

    }

}
