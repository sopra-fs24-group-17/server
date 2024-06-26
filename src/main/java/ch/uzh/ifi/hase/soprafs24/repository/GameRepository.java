package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.constant.GameMode;
import ch.uzh.ifi.hase.soprafs24.constant.GameState;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository("gameRepository")
public interface GameRepository extends JpaRepository<Game, Long> {
    Optional<Game> findByGameId(Long id);

    List<Game> findByStateAndMode(GameState state, GameMode mode);

    List<Game> findByInitiatingUserAndStateAndMode(User initiatingUser, GameState state, GameMode mode);

    @Query("SELECT g FROM Game g WHERE g.creationdate <= :cutoff")
    List<Game> findGamesCreatedBefore(Date cutoff);

}
