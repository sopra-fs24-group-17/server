package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.FriendRequestStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserFriends;
import ch.uzh.ifi.hase.soprafs24.entity.UserFriendsRequests;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import ch.uzh.ifi.hase.soprafs24.repository.UserFriendsRequestRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserFriendsRepository;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ActiveProfiles("dev")
@ExtendWith(MockitoExtension.class)
public class UserFriendsServiceTest {

    @Mock
    private UserFriendsRequestRepository userFriendsRequestRepository;

    @Mock
    private UserFriendsRepository userFriendsRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private UserFriendsService userFriendsService;

    @Test
    public void createFriendshipRequest_Success() {
        User requestingUser = new User();
        requestingUser.setId(1L);

        User requestedUser = new User();
        requestedUser.setId(2L);

        when(userFriendsRequestRepository.findFriendshipRequests(requestingUser.getId(), requestedUser.getId()))
                .thenReturn(Optional.empty());

        userFriendsService.createFriendshipRequest(requestingUser, requestedUser);

        Mockito.verify(userFriendsRequestRepository).saveAndFlush(any(UserFriendsRequests.class));
    }

    @Test
    public void createFriendshipRequest_Failure_DuplicateRequest() {
        User requestingUser = new User();
        requestingUser.setId(1L);

        User requestedUser = new User();
        requestedUser.setId(2L);

        UserFriendsRequests existingRequest = new UserFriendsRequests();
        existingRequest.setRequestingUser(requestingUser);
        existingRequest.setRequestedUser(requestedUser);

        when(userFriendsRequestRepository.findFriendshipRequests(requestingUser.getId(), requestedUser.getId()))
                .thenReturn(Optional.of(existingRequest));

        assertThrows(ResponseStatusException.class, () -> {
            userFriendsService.createFriendshipRequest(requestingUser, requestedUser);
        });
    }

    @Test
    public void processFriendshipRequest_Success_Accept() {
        Long requestedUserId = 2L;
        Long requestId = 1L;
        User requestingUser = new User();
        requestingUser.setId(1L);
        User requestedUser = new User();
        requestedUser.setId(requestedUserId);

        UserFriendsRequests request = new UserFriendsRequests();
        request.setId(requestId);
        request.setRequestingUser(requestingUser);
        request.setRequestedUser(requestedUser);
        request.setStatus(FriendRequestStatus.PENDING);

        when(userFriendsRequestRepository.findUserFriendsRequestsById(requestId)).thenReturn(request);

        userFriendsService.processFriendshipRequest(requestedUserId, requestId, FriendRequestStatus.ACCEPTED);

        Mockito.verify(userFriendsRequestRepository).save(request);
        assertEquals(FriendRequestStatus.ACCEPTED, request.getStatus());
    }

    @Test
    public void processFriendshipRequest_Failure_Unauthorized() {
        Long requestedUserId = 2L;
        Long requestId = 1L;
        User requestingUser = new User();
        requestingUser.setId(1L);
        User anotherUser = new User();
        anotherUser.setId(3L);

        UserFriendsRequests request = new UserFriendsRequests();
        request.setId(requestId);
        request.setRequestingUser(requestingUser);
        request.setRequestedUser(anotherUser);
        request.setStatus(FriendRequestStatus.PENDING);

        when(userFriendsRequestRepository.findUserFriendsRequestsById(requestId)).thenReturn(request);

        assertThrows(ResponseStatusException.class, () -> {
            userFriendsService.processFriendshipRequest(requestedUserId, requestId, FriendRequestStatus.ACCEPTED);
        });
    }
    @Test
    public void getFriends_ReturnsAllFriendsOfUser() {
        Long userId = 1L;
        User user = new User();
        user.setId(userId);

        User friend1 = new User();
        friend1.setId(2L);
        User friend2 = new User();
        friend2.setId(3L);

        UserFriends friendship1 = new UserFriends();
        friendship1.setUser(user);
        friendship1.setFriend(friend1);

        UserFriends friendship2 = new UserFriends();
        friendship2.setUser(friend2);
        friendship2.setFriend(user);

        when(userFriendsRepository.findAllFriendshipsForUser(userId)).thenReturn(Arrays.asList(friendship1, friendship2));

        List<User> friends = userFriendsService.getFriends(userId);

        assertEquals(2, friends.size(), "Should return exactly two friends.");
        assertEquals(friend1.getId(), friends.get(0).getId(), "The first friend's ID should match.");
        assertEquals(friend2.getId(), friends.get(1).getId(), "The second friend's ID should match.");
    }

    @Test
    public void findAllFriendshipRequestsReceived_ReturnsAllRequests() {
        Long userId = 1L;

        UserFriendsRequests request1 = new UserFriendsRequests();
        request1.setId(1L);
        UserFriendsRequests request2 = new UserFriendsRequests();
        request2.setId(2L);

        when(userFriendsRequestRepository.findAllReceivedRequests(userId)).thenReturn(Arrays.asList(request1, request2));

        List<UserFriendsRequests> requests = userFriendsService.findAllFriendshipRequestsReceived(userId);

        assertEquals(2, requests.size(), "Should return exactly two requests.");
        assertEquals(1L, requests.get(0).getId(), "The first request's ID should match.");
        assertEquals(2L, requests.get(1).getId(), "The second request's ID should match.");
    }

    @Test
    public void areUsersFriends_UsersAreNotFriends_ReturnsFalse() {
        User userA = new User();
        userA.setId(1L);
        User profileUser = new User();
        profileUser.setId(2L);

        when(userFriendsRepository.findFriendshipBetweenUsers(userA.getId(), profileUser.getId())).thenReturn(Optional.empty());
        when(userFriendsRepository.findFriendshipBetweenUsers(profileUser.getId(), userA.getId())).thenReturn(Optional.empty());

        boolean areFriends = userFriendsService.areUsersFriends(userA, profileUser);
        assertFalse(areFriends, "Users should not be friends");
    }
}
