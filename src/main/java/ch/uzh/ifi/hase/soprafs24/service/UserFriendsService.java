package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.FriendRequestStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserFriends;
import ch.uzh.ifi.hase.soprafs24.entity.UserFriendsRequests;
import ch.uzh.ifi.hase.soprafs24.event.FriendshipRequestAcceptanceEvent;
import ch.uzh.ifi.hase.soprafs24.event.FriendshipRequestSendEvent;
import ch.uzh.ifi.hase.soprafs24.event.GameJoinEvent;
import ch.uzh.ifi.hase.soprafs24.repository.UserFriendsRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserFriendsRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class UserFriendsService {

    private final UserFriendsRepository userFriendsRepository;
    private final UserFriendsRequestRepository userFriendsRequestRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    public UserFriendsService(UserFriendsRepository userFriendsRepository, UserFriendsRequestRepository userFriendsRequestRepository, ApplicationEventPublisher eventPublisher) {
        this.userFriendsRepository = userFriendsRepository;
        this.userFriendsRequestRepository = userFriendsRequestRepository;
        this.eventPublisher = eventPublisher;
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

    /**
     * Creates a friendship request but only if none is present currently (regardless of the state: pending, accepted or rejected)
     * @param requestingUser the user invoking the friendship request.
     * @param requestedUser the user receiving the friendship request.
     */
    public void createFriendshipRequest(User requestingUser, User requestedUser) {
        // Verify that no request exists currently.
        Optional<UserFriendsRequests> existingRequest = userFriendsRequestRepository.findFriendshipRequests(requestingUser.getId(), requestedUser.getId());
        if (existingRequest.isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "friendship request was already sent");
        }
        // Create friendship request if it doesn't exist so far.
        UserFriendsRequests friendRequest = new UserFriendsRequests();
        friendRequest.setRequestingUser(requestingUser);
        friendRequest.setRequestedUser(requestedUser);
        userFriendsRequestRepository.saveAndFlush(friendRequest);

        //Publish Friendship receipt event
        FriendshipRequestSendEvent friendshipRequestSendEvent = new FriendshipRequestSendEvent(this,
                requestingUser.getUsername(),
                requestedUser.getId());
        eventPublisher.publishEvent(friendshipRequestSendEvent);

    }

    /**
     * Modifies a friendship request, either setting its status to accepted or rejected. If accepted, a userFriend object is created
     * @param requestedUserId the id of the user who received the friendship request.
     * @param requestId the id of the friendship-request.
     * @param newStatus the altered status of the friendship-request (either ACCEPTED or REJECTED)
     */
    public void processFriendshipRequest(Long requestedUserId, Long requestId, FriendRequestStatus newStatus) {
        UserFriendsRequests userFriendsRequests = userFriendsRequestRepository.findUserFriendsRequestsById(requestId);
        if (userFriendsRequests == null || userFriendsRequests.getStatus() != FriendRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "friendship request does not exist");
        }
        if (!Objects.equals(userFriendsRequests.getRequestedUser().getId(), requestedUserId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthorized to process this friendship request");
        }
        if (newStatus != null) {
            userFriendsRequests.setStatus(newStatus);
            userFriendsRequestRepository.save(userFriendsRequests);
        }
        if (newStatus == FriendRequestStatus.ACCEPTED) {
            UserFriends userFriends = new UserFriends();
            userFriends.setUser(userFriendsRequests.getRequestingUser());
            userFriends.setFriend(userFriendsRequests.getRequestedUser());
            userFriendsRepository.save(userFriends);

            //Publish Friendship acceptance event
            FriendshipRequestAcceptanceEvent friendshipRequestAcceptanceEvent = new FriendshipRequestAcceptanceEvent(this,
                    userFriendsRequests.getRequestingUser().getUsername(),
                    userFriendsRequests.getRequestedUser().getId());
            eventPublisher.publishEvent(friendshipRequestAcceptanceEvent);

        }
    }

    /**
     * Retrieves all friendship requests that a given user received
     * @param userId of the user whose friendship requests are fetched.
     */
    public List<UserFriendsRequests> findAllFriendshipRequestsReceived(Long userId) {
        List<UserFriendsRequests> allRequests = new ArrayList<>();
        // Add all received requests
        allRequests.addAll(userFriendsRequestRepository.findAllReceivedRequests(userId));
        return allRequests;
    }

    /**
     * Retrieves all friends for a particular user and returns the corresponding user objects.
     * @param userId of the user whose friends shall be retrieved.
     * @return an array of all user objects that are friends of the invoking user.
     */
    public List<User> getFriends(Long userId) {
        List<UserFriends> friendships = userFriendsRepository.findAllFriendshipsForUser(userId);
        List<User> friends = new ArrayList<>();

        for (UserFriends friendship : friendships) {
            User friend = (friendship.getUser().getId().equals(userId)) ? friendship.getFriend() : friendship.getUser();
            friends.add(friend);
        }
        return friends;
    }
}
