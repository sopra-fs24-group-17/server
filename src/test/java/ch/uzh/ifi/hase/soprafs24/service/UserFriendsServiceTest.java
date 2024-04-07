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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

@ActiveProfiles("dev")
@ExtendWith(MockitoExtension.class)
public class UserFriendsServiceTest {

    @Mock
    private UserFriendsRequestRepository userFriendsRequestRepository;

    @Mock
    private UserFriendsRepository userFriendsRepository;

    @InjectMocks
    private UserFriendsService userFriendsService;

    @Test
    public void createFriendshipRequest_Success() {
        User requestingUser = new User();
        requestingUser.setId(1L);

        User requestedUser = new User();
        requestedUser.setId(2L);

        Mockito.when(userFriendsRequestRepository.findFriendshipRequests(requestingUser.getId(), requestedUser.getId()))
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

        Mockito.when(userFriendsRequestRepository.findFriendshipRequests(requestingUser.getId(), requestedUser.getId()))
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

        Mockito.when(userFriendsRequestRepository.findUserFriendsRequestsById(requestId)).thenReturn(request);

        userFriendsService.processFriendshipRequest(requestedUserId, requestId, FriendRequestStatus.ACCEPTED);

        Mockito.verify(userFriendsRequestRepository).save(request);
        Mockito.verify(userFriendsRepository).save(any(UserFriends.class));
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

        Mockito.when(userFriendsRequestRepository.findUserFriendsRequestsById(requestId)).thenReturn(request);

        assertThrows(ResponseStatusException.class, () -> {
            userFriendsService.processFriendshipRequest(requestedUserId, requestId, FriendRequestStatus.ACCEPTED);
        });
    }
}
