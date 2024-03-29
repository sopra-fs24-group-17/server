package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserFriends;
import ch.uzh.ifi.hase.soprafs24.repository.UserFriendsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserFriendsService {

    private final UserFriendsRepository userFriendsRepository;

    @Autowired
    public UserFriendsService(UserFriendsRepository userFriendsRepository) {
        this.userFriendsRepository = userFriendsRepository;
    }

    /**
     * Verifies if two users are in a friendship relation.
     * @param userA user object of the invoking user.
     * @param profileUser user object of the profile user
     * @return boolean indicating whether the users are friends (true) or not (false).
     */
    public boolean areUsersFriends(User userA, User profileUser) {
        return userFriendsRepository.findFriendshipBetweenUsers(userA.getId(), profileUser.getId()).isPresent()
                || userFriendsRepository.findFriendshipBetweenUsers(profileUser.getId(), userA.getId()).isPresent();
    }

    public List<UserFriends> getFriends (User user) {
        return userFriendsRepository.findUserFriendsByUserId(user.getId());
    }
}
