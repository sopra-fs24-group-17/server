package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.FriendRequestStatus;
import ch.uzh.ifi.hase.soprafs24.constant.ProfileVisibility;
import ch.uzh.ifi.hase.soprafs24.constant.TutorialFlag;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserFriendsRequests;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("dev")
public class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private PasswordService passwordService;

  @Mock
  private EmailSenderService emailSenderService;

  @Mock
  private UserFriendsService userFriendsService;

  @Mock
  private ContentModerationService contentModerationService;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @InjectMocks
  private UserService userService;

  private User testUser;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);

    // given
    testUser = new User();
    testUser.setId(1L);
    testUser.setEmail("test@email.com");
    testUser.setUsername("testUsername");
    testUser.setPassword("password");

    // when -> any object is being save in the userRepository -> return the dummy
    // testUser
    when(userRepository.save(Mockito.any())).thenReturn(testUser);
  }

    private User setupTestUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("existingUser");
        user.setPassword("existingPassword");
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            user.setBirthdate(sdf.parse("2000-01-01"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        user.setEmail("existing@email.com");
        user.setCountryoforigin("USA");
        user.setProfilevisibility(ProfileVisibility.FALSE);
        user.setTutorialflag(TutorialFlag.TRUE);
        return user;
    }

  @Test
  public void createUser_validInputs_success() {
    // when -> any object is being save in the userRepository -> return the dummy
    // testUser
    User createdUser = userService.createUser(testUser);

    // then
    verify(userRepository, Mockito.times(1)).save(Mockito.any());

    assertEquals(testUser.getId(), createdUser.getId());
    assertEquals(testUser.getEmail(), createdUser.getEmail());
    assertEquals(testUser.getUsername(), createdUser.getUsername());
    assertNotNull(createdUser.getToken());
    assertEquals(UserStatus.OFFLINE, createdUser.getStatus());
  }

  @Test
  public void createUser_duplicateName_throwsException() {
    userService.createUser(testUser);
    when(userRepository.findByUsername(Mockito.any())).thenReturn(testUser);
    assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
  }

  @Test
  public void createUser_duplicateEmail_throwsException() {
      userService.createUser(testUser);
      when(userRepository.findByEmail(Mockito.any())).thenReturn(testUser);
      assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
  }

  @Test
  public void createUser_duplicateInputs_throwsException() {
    userService.createUser(testUser);
    when(userRepository.findByUsername(Mockito.any())).thenReturn(testUser);
    assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
  }

  @Test
  public void createUser_emptyUsername_throwsException() {
      testUser = new User();
      testUser.setId(1L);
      testUser.setUsername("");
      testUser.setPassword("password");

      when(userRepository.findUserById(Mockito.any())).thenReturn(testUser);
      assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
  }

  @Test
  public void createUser_invalidUsername_throwsException() {
     testUser = new User();
     testUser.setId(1L);
     testUser.setUsername("        ");
     testUser.setEmail("test@email.com");

     when(userRepository.findUserById(Mockito.any())).thenReturn(testUser);
     assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
  }

  @Test
  public void createUser_emptyEmail_throwsException() {
      testUser = new User();
      testUser.setId(1L);
      testUser.setUsername("user");
      testUser.setEmail("");

      when(userRepository.findUserById(Mockito.any())).thenReturn(testUser);

      assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
  }

  @Test
  public void createUser_invalidEmail_throwsException() {
      testUser = new User();
      testUser.setId(1L);
      testUser.setUsername("user");
      testUser.setPassword("password");
      testUser.setEmail("invalidemail"); // contains no @ symbol

      when(userRepository.findUserById(Mockito.any())).thenReturn(testUser);

      assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
  }

  @Test
  public void createUser_emptyPassword_throwsException() {
      testUser = new User();
      testUser.setId(1L);
      testUser.setUsername("user");
      testUser.setPassword("");
      testUser.setEmail("test@email.com");

      when(userRepository.findUserById(Mockito.any())).thenReturn(testUser);

      assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
  }

  @Test
  public void createUser_invalidPassword_throwsException() {
      testUser = new User();
      testUser.setId(1L);
      testUser.setUsername("user");
      testUser.setPassword("                  ");
      testUser.setEmail("test@email.com");

      when(userRepository.findUserById(Mockito.any())).thenReturn(testUser);

      assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
  }

  @Test
  public void authenticateUser_success_returnsUser() {
      testUser = new User();
      testUser.setId(1L);
      testUser.setUsername("user");
      testUser.setPassword("password");
      testUser.setEmail("test@email.com");

      when(userRepository.findByUsername(Mockito.any())).thenReturn(testUser);
      when(passwordService.verifyPassword(Mockito.anyString(), Mockito.anyString())).thenReturn(true);

      User authenticatedUser = userService.authenticateUser(testUser.getUsername(), testUser.getPassword());
      assertEquals(authenticatedUser.getUsername(), testUser.getUsername());
  }

  @Test
  public void authenticateUser_invalid_throws401() {
      testUser = new User();
      testUser.setId(1L);
      testUser.setUsername("user");
      testUser.setPassword("password");
      testUser.setEmail("test@email.com");

      when(userRepository.findByUsername(Mockito.anyString())).thenReturn(null);
      assertThrows(ResponseStatusException.class, () -> userService.authenticateUser(testUser.getUsername(), testUser.getPassword()));
  }

  @Test
  public void verifyTokenAndId_success_returnsUser() {
      testUser = new User();
      testUser.setId(1L);
      testUser.setUsername("user");
      testUser.setPassword("password");
      testUser.setEmail("test@email.com");
      testUser.setToken("123");

      when(userRepository.findUserByToken(Mockito.anyString())).thenReturn(testUser);
      when(userService.verifyUserByToken(Mockito.anyString())).thenReturn(testUser);

      User verifiedUser = userService.verifyTokenAndId(testUser.getToken(), testUser.getId());
      assertEquals(verifiedUser.getUsername(), testUser.getUsername());

  }

    @Test
    public void verifyTokenAndId_invalid_throwsException() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("user");
        testUser.setPassword("password");
        testUser.setEmail("test@email.com");
        testUser.setToken("123");

        assertThrows(ResponseStatusException.class, () -> userService.verifyTokenAndId(testUser.getToken(), testUser.getId()));
    }

    @Test
    public void setOnline_success() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("user");
        testUser.setPassword("password");
        testUser.setEmail("test@email.com");
        testUser.setToken("123");
        testUser.setStatus(UserStatus.OFFLINE);

        when(userRepository.findByUsername(Mockito.anyString())).thenReturn(testUser);

        User onlineUser = userService.setOnline(testUser.getUsername());
        assertNotEquals(onlineUser.getStatus(), testUser.getStatus());
    }

    @Test
    public void setOffline_success() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("user");
        testUser.setPassword("password");
        testUser.setEmail("test@email.com");
        testUser.setToken("123");
        testUser.setStatus(UserStatus.ONLINE);

        when(userRepository.findByUsername(Mockito.anyString())).thenReturn(testUser);

        User offlineUser = userService.setOffline(testUser.getUsername());
        assertNotEquals(offlineUser.getStatus(), testUser.getStatus());

    }

    @Test
    public void setOffline_invalid_throwsException() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("user");
        testUser.setPassword("password");
        testUser.setEmail("test@email.com");
        testUser.setToken("123");
        testUser.setStatus(UserStatus.OFFLINE);

        when(userRepository.findByUsername(Mockito.anyString())).thenReturn(testUser);
        assertThrows(ResponseStatusException.class, () -> userService.setOffline(testUser.getUsername()));
    }

    @Test
    public void getUserByToken_success() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("user");
        testUser.setPassword("password");
        testUser.setEmail("test@email.com");
        testUser.setToken("123");
        testUser.setStatus(UserStatus.OFFLINE);

        when(userRepository.findUserByToken(Mockito.anyString())).thenReturn(testUser);
        assertEquals(userService.verifyUserByToken(testUser.getToken()), testUser);

    }

    @Test
    public void getUserByToken_invalid_throwsException() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("user");
        testUser.setPassword("password");
        testUser.setEmail("test@email.com");
        testUser.setToken("123");
        testUser.setStatus(UserStatus.OFFLINE);

        when(userRepository.findUserByToken(Mockito.anyString())).thenReturn(null);
        assertThrows(ResponseStatusException.class, () -> userService.verifyUserByToken(testUser.getToken()));
    }

    @Test
    public void getUserByEmail_success() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("user");
        testUser.setPassword("password");
        testUser.setEmail("test@email.com");
        testUser.setToken("123");
        testUser.setStatus(UserStatus.OFFLINE);

        when(userRepository.findByEmail(Mockito.anyString())).thenReturn(testUser);
        assertEquals(userService.getUserByEmail(testUser.getEmail()), testUser);
    }

    @Test
    public void getUserByEmail_invalid_throwsException() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("user");
        testUser.setPassword("password");
        testUser.setEmail("test@email.com");
        testUser.setToken("123");
        testUser.setStatus(UserStatus.OFFLINE);

        when(userRepository.findByEmail(Mockito.anyString())).thenReturn(null);
        assertThrows(ResponseStatusException.class, () -> userService.getUserByEmail(testUser.getEmail()));
    }

    @Test
    public void resetPassword_success() {
        User testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("user");
        testUser.setPassword("oldPassword");
        testUser.setEmail("test@email.com");
        testUser.setToken("123");
        testUser.setStatus(UserStatus.OFFLINE);

        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setUsername("user");
        userPostDTO.setEmail("test@email.com");

        when(passwordService.generateRandomPassword(10)).thenReturn("randomPassword");
        when(passwordService.securePassword("randomPassword")).thenReturn("securedPassword");

        userService.resetPassword(testUser, userPostDTO);

        verify(passwordService, Mockito.times(1)).securePassword("randomPassword");
        verify(emailSenderService, Mockito.times(1)).sendNewPassword(testUser.getEmail(), testUser.getUsername(), "randomPassword");
        assertEquals("securedPassword", testUser.getPassword());
        assertTrue(testUser.getOtp());
    }

    @Test
    public void resetPassword_invalidUsername_throwsException() {
        User testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("user");
        testUser.setPassword("password");
        testUser.setEmail("test@email.com");
        testUser.setToken("123");
        testUser.setStatus(UserStatus.OFFLINE);

        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setUsername("differentUser");
        userPostDTO.setEmail("test@email.com");

        assertThrows(ResponseStatusException.class, () -> userService.resetPassword(testUser, userPostDTO));

        verify(passwordService, Mockito.never()).generateRandomPassword(Mockito.anyInt());
        verify(emailSenderService, Mockito.never()).sendNewPassword(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void editUser_UserNotFound_ThrowsException() {
        Long userId = 1L;
        User modifiedUser = new User(); // Populate as needed for the test.

        when(userRepository.findUserById(userId)).thenReturn(null);

        assertThrows(ResponseStatusException.class, () -> userService.editUser(userId, modifiedUser));
    }

    @Test
    public void editUser_SuccessfulUpdate_UpdatesFields() {
        Long userId = 1L;
        User existingUser = new User();
        existingUser.setId(userId);
        existingUser.setUsername("oldUsername");
        existingUser.setPassword("oldPassword");
        existingUser.setEmail("oldEmail@test.com");

        User modifiedUser = new User();
        modifiedUser.setUsername("newUsername");
        modifiedUser.setPassword("newPassword");
        modifiedUser.setEmail("newEmail@test.com");

        when(userRepository.findUserById(userId)).thenReturn(existingUser);
        when(passwordService.securePassword("newPassword")).thenReturn("securedNewPassword");

        User result = userService.editUser(userId, modifiedUser);

        assertEquals("newUsername", result.getUsername());
        assertEquals("securedNewPassword", result.getPassword());
        assertEquals("newEmail@test.com", result.getEmail());
    }

    @Test
    public void editUser_EmptyPassword_ThrowsException() {
        Long userId = 1L;
        User existingUser = new User();
        existingUser.setId(userId);
        existingUser.setUsername("username");
        existingUser.setPassword("oldPassword");

        User modifiedUser = new User();
        modifiedUser.setUsername("username");
        modifiedUser.setPassword("   ");

        when(userRepository.findUserById(userId)).thenReturn(existingUser);
        assertThrows(ResponseStatusException.class, () -> userService.editUser(userId, modifiedUser));
    }

    @Test
    public void getUsers_ReturnsListOfUsers() {
        User user1 = new User();
        user1.setUsername("user1");
        User user2 = new User();
        user2.setUsername("user2");
        List<User> mockUsers = Arrays.asList(user1, user2);

        when(userRepository.findAll()).thenReturn(mockUsers);
        List<User> users = userService.getUsers();

        assertNotNull(users);
        assertEquals(2, users.size());
        assertEquals(mockUsers, users);

        verify(userRepository, Mockito.times(1)).findAll();
    }

    @Test
    public void verifyToken_validToken() {
      User testUser = new User();
      testUser.setToken("123");

      when(userRepository.findUserByToken(Mockito.anyString())).thenReturn(testUser);
      assertEquals(userService.verifyUserByToken(testUser.getToken()), testUser);
    }

    @Test
    public void verifyToken_invalidToken_throwsException() {
        User testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("user");
        testUser.setPassword("password");
        testUser.setEmail("test@email.com");
        testUser.setToken("123");
        testUser.setStatus(UserStatus.OFFLINE);

        assertThrows(ResponseStatusException.class, () -> userService.verifyUserByToken("456"));
    }

    @Test
    public void whenGetProfileUser_visibilityRestrictedToFriends_thenThrowUnauthorized() {
        testUser = new User();
        testUser.setId(2L);
        testUser.setProfilevisibility(ProfileVisibility.FALSE);

        User invokingUser = new User();
        invokingUser.setId(1L);

        when(userRepository.findUserById(testUser.getId())).thenReturn(testUser);
        when(userFriendsService.areUsersFriends(testUser, invokingUser)).thenReturn(false);

        assertThrows(ResponseStatusException.class, () -> {
            userService.getProfileUser(testUser.getId(), invokingUser);
        }, "unauthorized to access this profile");
    }

    @Test
    public void whenGetProfileUser_visibilityUnrestricted_thenReturnUser() {
        testUser = new User();
        testUser.setId(2L);
        testUser.setProfilevisibility(ProfileVisibility.TRUE);

        User invokingUser = new User();
        invokingUser.setId(1L);

        when(userRepository.findUserById(testUser.getId())).thenReturn(testUser);
        when(userFriendsService.areUsersFriends(testUser, invokingUser)).thenReturn(false);

        User result = userService.getProfileUser(testUser.getId(), invokingUser);

        verify(userRepository, Mockito.times(1)).findUserById(testUser.getId());
        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
    }

    @Test
    public void addFriends_validRequest_createsFriendship() {
        User user1 = new User();
        user1.setId(1L);
        user1.setToken("valid-token");

        User user2 = new User();
        user2.setId(2L);

        when(userRepository.findUserByToken("valid-token")).thenReturn(user1);
        when(userRepository.findUserById(2L)).thenReturn(user2);

        userService.addFriends(2L, "valid-token");

        verify(userFriendsService).createFriendshipRequest(user1, user2);
    }

    @Test
    public void addFriends_invalidToken_throwsException() {
        when(userRepository.findUserByToken("invalid-token")).thenReturn(null);

        assertThrows(ResponseStatusException.class, () -> userService.addFriends(2L, "invalid-token"));
    }

    @Test
    public void addFriends_invalidUserId_throwsException() {
        User user1 = new User();
        user1.setId(1L);
        user1.setToken("valid-token");

        when(userRepository.findUserByToken("valid-token")).thenReturn(user1);
        when(userRepository.findUserById(2L)).thenReturn(null);

        assertThrows(ResponseStatusException.class, () -> userService.addFriends(2L, "valid-token"));
    }

    @Test
    public void editFriends_validRequest_processesFriendshipRequest() {
        Long userId = 1L;
        Long requestId = 1L;
        FriendRequestStatus newStatus = FriendRequestStatus.ACCEPTED;

        userService.editFriends(userId, requestId, newStatus);

        verify(userFriendsService).processFriendshipRequest(userId, requestId, newStatus);
    }

    @Test
    public void editFriends_invalidRequestId_throwsException() {
        Long userId = 1L;
        Long invalidRequestId = 999L;
        FriendRequestStatus newStatus = FriendRequestStatus.ACCEPTED;

        Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
                .when(userFriendsService).processFriendshipRequest(userId, invalidRequestId, newStatus);

        assertThrows(ResponseStatusException.class, () -> userService.editFriends(userId, invalidRequestId, newStatus));
    }

    @Test
    public void getPendingFriendshipRequests_validUserId_returnsRequests() {
        Long userId = 1L;
        List<UserFriendsRequests> expectedRequests = Arrays.asList(new UserFriendsRequests(), new UserFriendsRequests());

        when(userFriendsService.findAllFriendshipRequestsReceived(userId)).thenReturn(expectedRequests);

        List<UserFriendsRequests> actualRequests = userService.getPendingFriendshipRequests(userId);

        assertEquals(expectedRequests, actualRequests);
        assertNotNull(actualRequests);
        assertEquals(2, actualRequests.size());
    }

    @Test
    public void getPendingFriendshipRequests_noRequests_returnsEmptyList() {
        Long userId = 1L;
        when(userFriendsService.findAllFriendshipRequestsReceived(userId)).thenReturn(Collections.emptyList());

        List<UserFriendsRequests> actualRequests = userService.getPendingFriendshipRequests(userId);

        assertTrue(actualRequests.isEmpty());
    }

    @Test
    public void getUsersFriends_validUserId_returnsFriendsList() {
        Long userId = 1L;
        List<User> expectedFriends = Arrays.asList(new User(), new User());

        when(userFriendsService.getFriends(userId)).thenReturn(expectedFriends);

        List<User> actualFriends = userService.getUsersFriends(userId);

        assertEquals(expectedFriends, actualFriends);
        assertNotNull(actualFriends);
        assertEquals(2, actualFriends.size());
    }

    @Test
    public void getUsersFriends_noFriends_returnsEmptyList() {
        Long userId = 1L;
        when(userFriendsService.getFriends(userId)).thenReturn(Collections.emptyList());

        List<User> actualFriends = userService.getUsersFriends(userId);

        assertTrue(actualFriends.isEmpty());
    }

    @Test
    public void getUserById_validUserId_returnsUser() {
        Long userId = 1L;
        User expectedUser = new User();
        expectedUser.setId(userId);

        when(userRepository.findUserById(userId)).thenReturn(expectedUser);

        User actualUser = userService.getUserById(userId);

        assertEquals(expectedUser, actualUser);
        assertEquals(userId, actualUser.getId());
    }

    @Test
    public void getUserById_invalidUserId_throwsException() {
        Long userId = 999L;
        when(userRepository.findUserById(userId)).thenReturn(null);

        assertThrows(ResponseStatusException.class, () -> userService.getUserById(userId));
    }

    @Test
    public void editUser_UpdateCountryOfOrigin_Success() {
        User existingUser = setupTestUser();
        User modifiedUser = new User();
        modifiedUser.setCountryoforigin("Canada");

        when(userRepository.findUserById(1L)).thenReturn(existingUser);
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(existingUser);

        User result = userService.editUser(1L, modifiedUser);

        assertEquals("Canada", result.getCountryoforigin());
        verify(userRepository).saveAndFlush(existingUser);
    }

    @Test
    public void editUser_UpdateProfileVisibility_Success() {
        User existingUser = setupTestUser();
        User modifiedUser = new User();
        modifiedUser.setProfilevisibility(ProfileVisibility.TRUE);

        when(userRepository.findUserById(1L)).thenReturn(existingUser);
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(existingUser);

        User result = userService.editUser(1L, modifiedUser);

        assertEquals(ProfileVisibility.TRUE, result.getProfilevisibility());
        verify(userRepository).saveAndFlush(existingUser);
    }

    @Test
    public void editUser_UpdateTutorialFlag_Success() {
        User existingUser = setupTestUser();
        User modifiedUser = new User();
        modifiedUser.setTutorialflag(TutorialFlag.FALSE);

        when(userRepository.findUserById(1L)).thenReturn(existingUser);
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(existingUser);

        User result = userService.editUser(1L, modifiedUser);

        assertEquals(TutorialFlag.FALSE, result.getTutorialflag());
        verify(userRepository).saveAndFlush(existingUser);
    }

    @Test
    public void editUser_UpdateBirthDate_Success() throws Exception {
        User existingUser = setupTestUser();
        User modifiedUser = new User();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date newBirthdate = sdf.parse("1900-01-01");
        modifiedUser.setBirthdate(newBirthdate);

        when(userRepository.findUserById(1L)).thenReturn(existingUser);
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(existingUser);

        User result = userService.editUser(1L, modifiedUser);

        assertEquals(newBirthdate, result.getBirthdate());
        verify(userRepository).saveAndFlush(existingUser);
    }

    @Test
    public void getUsersWithStats_ReturnsListOfUsersWithStats() {
        User user1 = new User();
        user1.setUsername("user1");
        User user2 = new User();
        user2.setUsername("user2");

        List<User> mockUsers = Arrays.asList(user1, user2);
        when(userRepository.findAllWithStatistics()).thenReturn(mockUsers);

        List<User> users = userService.getUsersWithStats();

        assertNotNull(users);
        assertEquals(2, users.size());
        assertEquals(mockUsers, users);
        verify(userRepository, Mockito.times(1)).findAllWithStatistics();
    }

    @Test
    public void createUser_UsernameToxicityScoreTooHigh_ThrowsException() {
        User offensiveUser = new User();
        offensiveUser.setUsername("OffensiveUsername");
        offensiveUser.setEmail("test@example.com");
        offensiveUser.setPassword("password");

        when(contentModerationService.checkToxicity(offensiveUser.getUsername())).thenReturn(0.85);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.createUser(offensiveUser)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("offensive username is not permitted", exception.getReason());
    }

    @Test
    public void getProfileUser_NullUser_ThrowsException() {
        Long userId = 999L;
        User invokingUser = new User();
        invokingUser.setId(1L);

        when(userRepository.findUserById(userId)).thenReturn(null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.getProfileUser(userId, invokingUser)
        );
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("user profile not found", exception.getReason());
    }

    @Test
    public void createUser_EmailToxicityScoreTooHigh_ThrowsException() {
        // Arrange
        User offensiveUser = new User();
        offensiveUser.setUsername("testUser");
        offensiveUser.setEmail("offensive@example.com");
        offensiveUser.setPassword("password");

        when(contentModerationService.checkToxicity(offensiveUser.getEmail())).thenReturn(0.85);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.createUser(offensiveUser)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("offensive email is not permitted", exception.getReason());
    }

    @Test
    public void verifyTokenAndId_Unauthorized_ThrowsException() {
        User testUser = new User();
        testUser.setId(1L);
        testUser.setToken("123");

        when(userRepository.findUserByToken("123")).thenReturn(testUser);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.verifyTokenAndId("123", 2L)
        );
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertEquals("Unauthorized", exception.getReason());
    }

    @Test
    public void editUser_UpdateAvatar_Success() {
        User modifiedUser = new User();
        modifiedUser.setAvatar("newAvatarUrl");
        User existingUser = setupTestUser();

        when(userRepository.findUserById(1L)).thenReturn(existingUser);
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(existingUser);

        User updatedUser = userService.editUser(1L, modifiedUser);

        assertEquals("newAvatarUrl", updatedUser.getAvatar());
        verify(userRepository).saveAndFlush(existingUser);
    }

    @Test
    public void editUser_UpdateAvatar_UserNotFound_ThrowsException() {
        Long nonExistentUserId = 999L;
        User modifiedUser = new User();
        modifiedUser.setAvatar("newAvatarUrl");

        when(userRepository.findUserById(nonExistentUserId)).thenReturn(null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.editUser(nonExistentUserId, modifiedUser)
        );

        assertEquals("user does not exist in DB", exception.getReason());
    }

}
