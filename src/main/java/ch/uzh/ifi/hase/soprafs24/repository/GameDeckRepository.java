package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.GameDeck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository("gameDeckRepository")
public interface GameDeckRepository extends JpaRepository<GameDeck, Long> {

    @Query("SELECT g FROM Game g LEFT JOIN FETCH g.gameDeck WHERE g.gameId = :gameId")
    Optional<Game> findByIdWithGameDeck(@Param("gameId") Long gameId);

}
