package ch.uzh.ifi.hase.soprafs24.repository;
import ch.uzh.ifi.hase.soprafs24.entity.UserFriends;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository("userfriendsRepository")
public interface UserFriendsRepository extends JpaRepository<UserFriends, Long> {

    @Query("SELECT uf FROM UserFriends uf WHERE (uf.user.id = :userId AND uf.friend.id = :friendId) OR (uf.user.id = :friendId AND uf.friend.id = :userId)")
    Optional<UserFriends> findFriendshipBetweenUsers(@Param("userId") Long userId, @Param("friendId") Long friendId);
}

