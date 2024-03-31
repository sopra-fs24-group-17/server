package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.ProfileVisibility;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserStats;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

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
    Mockito.when(userRepository.save(Mockito.any())).thenReturn(testUser);
  }

  @Test
  public void createUser_validInputs_success() {
    // when -> any object is being save in the userRepository -> return the dummy
    // testUser
    User createdUser = userService.createUser(testUser);

    // then
    Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any());

    assertEquals(testUser.getId(), createdUser.getId());
    assertEquals(testUser.getEmail(), createdUser.getEmail());
    assertEquals(testUser.getUsername(), createdUser.getUsername());
    assertNotNull(createdUser.getToken());
    assertEquals(UserStatus.OFFLINE, createdUser.getStatus());
  }

  @Test
  public void createUser_duplicateName_throwsException() {
    userService.createUser(testUser);
    Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(testUser);
    assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
  }

  @Test
  public void createUser_duplicateEmail_throwsException() {
      userService.createUser(testUser);
      Mockito.when(userRepository.findByEmail(Mockito.any())).thenReturn(testUser);
      assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
  }

  @Test
  public void createUser_duplicateInputs_throwsException() {
    userService.createUser(testUser);
    Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(testUser);
    assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
  }

  @Test
  public void createUser_emptyUsername_throwsException() {
      testUser = new User();
      testUser.setId(1L);
      testUser.setUsername("");
      testUser.setPassword("password");

      Mockito.when(userRepository.findUserById(Mockito.any())).thenReturn(testUser);
      assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
  }

  @Test
  public void createUser_invalidUsername_throwsException() {
     testUser = new User();
     testUser.setId(1L);
     testUser.setUsername("        ");
     testUser.setEmail("test@email.com");

     Mockito.when(userRepository.findUserById(Mockito.any())).thenReturn(testUser);
     assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
  }

  @Test
  public void createUser_emptyEmail_throwsException() {
      testUser = new User();
      testUser.setId(1L);
      testUser.setUsername("user");
      testUser.setEmail("");

      Mockito.when(userRepository.findUserById(Mockito.any())).thenReturn(testUser);

      assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
  }

  @Test
  public void createUser_invalidEmail_throwsException() {
      testUser = new User();
      testUser.setId(1L);
      testUser.setUsername("user");
      testUser.setPassword("password");
      testUser.setEmail("invalidemail"); // contains no @ symbol

      Mockito.when(userRepository.findUserById(Mockito.any())).thenReturn(testUser);

      assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
  }

  @Test
  public void createUser_emptyPassword_throwsException() {
      testUser = new User();
      testUser.setId(1L);
      testUser.setUsername("user");
      testUser.setPassword("");
      testUser.setEmail("test@email.com");

      Mockito.when(userRepository.findUserById(Mockito.any())).thenReturn(testUser);

      assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
  }

  @Test
  public void createUser_invalidPassword_throwsException() {
      testUser = new User();
      testUser.setId(1L);
      testUser.setUsername("user");
      testUser.setPassword("                  ");
      testUser.setEmail("test@email.com");

      Mockito.when(userRepository.findUserById(Mockito.any())).thenReturn(testUser);

      assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
  }

  @Test
  public void authenticateUser_success_returnsUser() {
      testUser = new User();
      testUser.setId(1L);
      testUser.setUsername("user");
      testUser.setPassword("password");
      testUser.setEmail("test@email.com");

      Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(testUser);
      Mockito.when(passwordService.verifyPassword(Mockito.anyString(), Mockito.anyString())).thenReturn(true);

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

      Mockito.when(userRepository.findByUsername(Mockito.anyString())).thenReturn(null);
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

      Mockito.when(userRepository.findUserByToken(Mockito.anyString())).thenReturn(testUser);
      Mockito.when(userService.getUserByToken(Mockito.anyString())).thenReturn(testUser);

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

        Mockito.when(userRepository.findByUsername(Mockito.anyString())).thenReturn(testUser);

        User onlineUser = userService.setOnline(testUser.getUsername());
        assertNotEquals(onlineUser.getStatus(), testUser.getStatus());
    }

    @Test
    public void setOnline_invalid_throwsException() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("user");
        testUser.setPassword("password");
        testUser.setEmail("test@email.com");
        testUser.setToken("123");
        testUser.setStatus(UserStatus.ONLINE);

        Mockito.when(userRepository.findByUsername(Mockito.anyString())).thenReturn(testUser);
        assertThrows(ResponseStatusException.class, () -> userService.setOnline(testUser.getUsername()));
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

        Mockito.when(userRepository.findByUsername(Mockito.anyString())).thenReturn(testUser);

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

        Mockito.when(userRepository.findByUsername(Mockito.anyString())).thenReturn(testUser);
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

        Mockito.when(userRepository.findUserByToken(Mockito.anyString())).thenReturn(testUser);
        assertEquals(userService.getUserByToken(testUser.getToken()), testUser);

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

        Mockito.when(userRepository.findUserByToken(Mockito.anyString())).thenReturn(null);
        assertThrows(ResponseStatusException.class, () -> userService.getUserByToken(testUser.getToken()));
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

        Mockito.when(userRepository.findByEmail(Mockito.anyString())).thenReturn(testUser);
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

        Mockito.when(userRepository.findByEmail(Mockito.anyString())).thenReturn(null);
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

        Mockito.when(passwordService.generateRandomPassword(10)).thenReturn("randomPassword");
        Mockito.when(passwordService.securePassword("randomPassword")).thenReturn("securedPassword");

        userService.resetPassword(testUser, userPostDTO);

        Mockito.verify(passwordService, Mockito.times(1)).securePassword("randomPassword");
        Mockito.verify(emailSenderService, Mockito.times(1)).sendNewPassword(testUser.getEmail(), testUser.getUsername(), "randomPassword");
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

        Mockito.verify(passwordService, Mockito.never()).generateRandomPassword(Mockito.anyInt());
        Mockito.verify(emailSenderService, Mockito.never()).sendNewPassword(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void editUser_UserNotFound_ThrowsException() {
        Long userId = 1L;
        User modifiedUser = new User(); // Populate as needed for the test.

        Mockito.when(userRepository.findUserById(userId)).thenReturn(null);

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

        Mockito.when(userRepository.findUserById(userId)).thenReturn(existingUser);
        Mockito.when(passwordService.securePassword("newPassword")).thenReturn("securedNewPassword");

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

        Mockito.when(userRepository.findUserById(userId)).thenReturn(existingUser);
        assertThrows(ResponseStatusException.class, () -> userService.editUser(userId, modifiedUser));
    }

    @Test
    public void getUsers_ReturnsListOfUsers() {
        User user1 = new User();
        user1.setUsername("user1");
        User user2 = new User();
        user2.setUsername("user2");
        List<User> mockUsers = Arrays.asList(user1, user2);

        Mockito.when(userRepository.findAll()).thenReturn(mockUsers);
        List<User> users = userService.getUsers();

        assertNotNull(users);
        assertEquals(2, users.size());
        assertEquals(mockUsers, users);

        Mockito.verify(userRepository, Mockito.times(1)).findAll();
    }

    @Test
    public void verifyToken_validToken() {
      User testUser = new User();
      testUser.setToken("123");

      Mockito.when(userRepository.findUserByToken(Mockito.anyString())).thenReturn(testUser);
      assertEquals(userService.getUserByToken(testUser.getToken()), testUser);
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

        assertThrows(ResponseStatusException.class, () -> userService.getUserByToken("456"));
    }

    @Test
    public void whenGetProfileUser_visibilityRestrictedToFriends_thenThrowUnauthorized() {
        testUser = new User();
        testUser.setId(2L);
        testUser.setProfilevisibility(ProfileVisibility.FALSE);

        User invokingUser = new User();
        invokingUser.setId(1L);

        Mockito.when(userRepository.findUserById(testUser.getId())).thenReturn(testUser);
        Mockito.when(userFriendsService.areUsersFriends(testUser, invokingUser)).thenReturn(false);

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

        Mockito.when(userRepository.findUserById(testUser.getId())).thenReturn(testUser);
        Mockito.when(userFriendsService.areUsersFriends(testUser, invokingUser)).thenReturn(false);

        User result = userService.getProfileUser(testUser.getId(), invokingUser);

        Mockito.verify(userRepository, Mockito.times(1)).findUserById(testUser.getId());
        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
    }
}
