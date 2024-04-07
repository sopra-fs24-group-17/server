package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.entity.UserFriends;
import ch.uzh.ifi.hase.soprafs24.entity.UserFriendsRequests;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ch.uzh.ifi.hase.soprafs24.constant.FriendRequestStatus;

import java.util.List;
import java.util.Optional;

@Repository("userfriendsrequestRepository")
public interface UserFriendsRequestRepository extends JpaRepository<UserFriendsRequests, Long> {

    UserFriendsRequests findUserFriendsRequestsById(Long id);

    // Finding a specific friend request
    @Query("SELECT ufr FROM UserFriendsRequests ufr WHERE ufr.requestingUser.id = :userId AND ufr.requestedUser.id = :friendId")
    Optional<UserFriendsRequests> findFriendshipRequests(@Param("userId") Long userId, @Param("friendId") Long friendId);

    // Fetching all received requests by a user
    @Query("SELECT ufr FROM UserFriendsRequests ufr WHERE ufr.requestedUser.id = :userId AND ufr.status = 'PENDING'")
    List<UserFriendsRequests> findAllReceivedRequests(@Param("userId") Long userId);
}

