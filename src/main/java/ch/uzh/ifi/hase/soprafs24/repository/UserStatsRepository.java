package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.entity.UserStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("userstatsRepository")
public interface UserStatsRepository extends JpaRepository<UserStats, Long> {

    UserStats findUserStatsByUserId(Long id);

}
