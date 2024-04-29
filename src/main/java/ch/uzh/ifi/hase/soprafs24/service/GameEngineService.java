package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.GameState;
import ch.uzh.ifi.hase.soprafs24.entity.Card;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserStats;
import ch.uzh.ifi.hase.soprafs24.event.*;
import ch.uzh.ifi.hase.soprafs24.repository.CardRepository;
import ch.uzh.ifi.hase.soprafs24.repository.GameDeckRepository;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
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
import java.util.stream.Stream;

@Service
@Transactional
public class GameEngineService {

    private GameDeckRepository gameDeckRepository;

    private GameRepository gameRepository;

    private UserRepository userRepository;

    private CardRepository cardRepository;

    private GameDeckService gameDeckService;

    private UserService userService;


    @Autowired
    private ApplicationEventPublisher eventPublisher;

    Logger logger = LoggerFactory.getLogger(GameDeckService.class);

    @Autowired
    public GameEngineService(@Qualifier("gameRepository") GameRepository gameRepository,
                             GameDeckRepository gameDeckRepository,
                             CardRepository cardRepository,
                             UserRepository userRepository,
                             GameDeckService gameDeckService,
                             UserService userService,
                             ApplicationEventPublisher eventPublisher) {

        this.gameRepository = gameRepository;
        this.gameDeckRepository = gameDeckRepository;
        this.cardRepository = cardRepository;
        this.userRepository = userRepository;
        this.gameDeckService = gameDeckService;
        this.userService = userService;
        this.eventPublisher = eventPublisher;
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid GameId provided");
        }
        return optionalGame.get();
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can't start a game that is beyond the preparation phase");
        }

        // Swap state from preparing to ongoing
        currentGame.setState(GameState.ONGOING);

        // Fetch all active players
        List<User> players = currentGame.getPlayers();

        List <Long> playerIds = new ArrayList<>();

        // Create dealer pile
        this.gameDeckService.createDealerPile(currentGame);

        // Remove Explosions and Defusions from Deck
        List<Card> explosions = gameDeckService.removeSpecificCardsFromDealerPile(currentGame.getGameDeck(), "AS,AH,AC,AD");
        List<Card> defusions = gameDeckService.removeSpecificCardsFromDealerPile(currentGame.getGameDeck(), "KS,KH,KC,KD,X1,X2");

        // Update lastPlayed and gamesPlayed for each user and update userIds
        for (User player: players) {

            GameStartEvent gameStartEvent = new GameStartEvent(this, currentGame.getGameId(), player.getId());
            eventPublisher.publishEvent(gameStartEvent);

            UserStats stats = player.getUserStats();
            stats.setGamesPlayed(stats.getGamesPlayed()+1);

            stats.setLastPlayed(new Date());
            playerIds.add(player.getId());

            // Create individual player piles
            List<Card> playerCards = gameDeckService.drawCardsFromDealerPile(currentGame.getGameDeck(), 5);
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

        User nextPlayer = getNextPlayer(terminatingUser, currentGame.getPlayers());

        if (currentGame.isSkipDraw()) {
            currentGame.setSkipDraw(false);
            gameRepository.saveAndFlush(currentGame);
        }

        if (currentGame.isRepeatTurn()) {
            nextPlayer = terminatingUser;
            drawCardMoveTermination(gameId, userId);
            currentGame.setRepeatTurn(false);
            gameRepository.saveAndFlush(currentGame);
        }

        if (nextPlayer != null) {
            currentGame.setCurrentTurn(nextPlayer);
            gameRepository.saveAndFlush(currentGame);
        } else {
            // Just one user left, initiate end of game and evaluation
            terminatingGame(gameId);
        }

        if (currentGame.isAttacked()) {
            currentGame.setRepeatTurn(true);
            currentGame.setAttacked(false);
            gameRepository.saveAndFlush(currentGame);
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
    private User getNextPlayer(User currentUser, List<User> players) {
        if (players.isEmpty() || (players.size() == 1 && players.contains(currentUser))) {
            return null;
        }
        int currentIndex = players.indexOf(currentUser);
        int nextIndex = (currentIndex + 1) % players.size();
        return players.get(nextIndex);
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User can only leave ongoing games");
        }

        List<User> players = currentGame.getPlayers();

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

        // Publish Loss Event
        LossEvent lossEvent = new LossEvent(this, gameId, terminatingUser.getUsername());
    }

    /**
     * Terminates an ongoing game
     * @param gameId referencing a Game
     */
    public void terminatingGame(Long gameId) {

        Game gameToBeTerminated = findGameById(gameId);

        // Assert that just one player is left in the game
        List<User> players = gameToBeTerminated.getPlayers();

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
     * Handler of the favor card
     * @param game currently active game
     * @param userId referencing the user that triggered the action
     * @throws IOException
     * @throws InterruptedException
     */
    public void handleFavorCard(Game game, Long userId, Long targetUserId) throws IOException, InterruptedException {

        // Assert that the targetUser is still part of the game
        List<User> players = game.getPlayers();
        List<Long> playerIds = new ArrayList<>();
        for(User player: players) {
            playerIds.add(player.getId());
        }

        if(!playerIds.contains(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Targeted User is not part of the game");
        }

        // Grab a random card
        Card randomCard = gameDeckService.drawCardFromPlayerPile(game.getGameDeck(), targetUserId, null);
        List<Card> randomCards = new ArrayList<>();
        randomCards.add(randomCard);

        // Publish a steal card event
        StealCardEvent stealCardEvent = new StealCardEvent(this, targetUserId, game.getGameId(), randomCards);
        eventPublisher.publishEvent(stealCardEvent);

        // Give it to triggering user
        gameDeckService.returnCardsToPile(game.getGameDeck(), userId.toString(), randomCard.getCode());

        // Publish a draw cards event
        PlayerCardEvent playerCardEvent = new PlayerCardEvent(this, userId, game.getGameId(), randomCards);
        eventPublisher.publishEvent(playerCardEvent);
    }

    /**
     * Implements the final draw that indicates end of turn
     * @param gameId referencing a game instance
     * @param userId referencing the user that triggered the action
     * @return String containing the drawn card
     * @throws IOException
     * @throws InterruptedException
     */
    public String drawCardMoveTermination(Long gameId, Long userId) throws IOException, InterruptedException {
        Game game = findGameById(gameId);

        if (game.isSkipDraw()) {
            game.setSkipDraw(false);
            gameRepository.saveAndFlush(game);
            return null;
        } else {
            List<Card> cards = gameDeckService.drawCardsFromDealerPile(game.getGameDeck(), 1);
            gameDeckService.returnCardsToPile(game.getGameDeck(), userId.toString(), cards.get(0).getCode());

            if (Objects.equals(cards.get(0).getInternalCode(), "explosion")) {

                if (game.isRepeatTurn()) {
                    game.setRepeatTurn(false);
                }
                else {
                    game.setSkipDraw(true);
                }
                return cards.get(0).getCode();
            }

            PlayerCardEvent playerCardEvent = new PlayerCardEvent(this, userId, gameId, cards);
            eventPublisher.publishEvent(playerCardEvent);
        }
        return null;
    }

    /**
     * Handler of the skip turn card
     * @param game currently active game
     * @param userId referencing the user that triggered the action
     * @throws IOException
     * @throws InterruptedException
     */
    public void handleSkipCard(Game game, Long userId) throws IOException, InterruptedException {
        game.setSkipDraw(true);
        gameRepository.saveAndFlush(game);
        SkipEvent skipEvent = new SkipEvent(this, game.getGameId(), game.getCurrentTurn().getUsername());
        eventPublisher.publishEvent(skipEvent);

        turnValidation(game.getGameId(), userId);

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

        // Implies that current User skips his draw
        game.setSkipDraw(true);

        // Make the next user to grab two cards from pile
        game.setAttacked(true);
        gameRepository.saveAndFlush(game);

        // To Do - Trigger Attack Event but on next user channel.
        AttackEvent attackEvent = new AttackEvent(this, game.getGameId(), game.getCurrentTurn().getUsername(), nextUserUserName);
        eventPublisher.publishEvent(attackEvent);

        turnValidation(game.getGameId(), userId);
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
            // Draw the card from the user pile and place it on top of the play pile
            Card drawnCard = gameDeckService.drawCardFromPlayerPile(game.getGameDeck(), userId,defuseCard);
            List<Card> drawnCards = new ArrayList<>();
            drawnCards.add(drawnCard);
            gameDeckService.placeCardsToPlayPile(game, userId, drawnCards, drawnCard.getCode());

            // Send message to respective user that his defuse card was taken
            DefuseEvent defuseEvent = new DefuseEvent(this, userId, game.getGameId(), drawnCards);
            eventPublisher.publishEvent(defuseEvent);

            // Place explosion card back on deck at random location
            // To do -- allow user to select where exactly to place the explosion card
            gameDeckService.returnCardsToPile(game.getGameDeck(), "dealer", explosionId);
            gameDeckService.shuffleCardsInDealerPile(game.getGameDeck());

            turnValidation(gameId, userId);

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

        List<Card> topCardsPlayPile = gameDeckService.exploreTopCardPlayPile(game.getGameDeck());
        Card topCardPlayPile;
        if (topCardsPlayPile == null) {
            topCardPlayPile = new Card();
            topCardPlayPile.setCode("");
            topCardPlayPile.setInternalCode("");
        } else {
            topCardPlayPile = topCardsPlayPile.get(topCardsPlayPile.size() - 1);
        }
        // Publish Event
        GameStateEvent gameStateEvent = new GameStateEvent(this, gameId,topCardPlayPile, parsedPileCardCounts);
        eventPublisher.publishEvent(gameStateEvent);

    }
}
