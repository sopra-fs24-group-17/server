package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.constant.FriendRequestStatus;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserFriendsRequests;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserFriendsRequestPutDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.service.EmailSenderService;
import ch.uzh.ifi.hase.soprafs24.service.PasswordService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UserControllerTest
 * This is a WebMvcTest which allows to test the UserController i.e. GET/POST
 * request without actually sending them over the network.
 * This tests if the UserController works.
 */
@WebMvcTest(UserController.class)
public class UserControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private UserService userService;

  @MockBean
  private EmailSenderService emailSenderService;

  @MockBean
  private PasswordService passwordService;

  @Test
  public void givenUsers_whenGetUsers_thenReturnJsonArray() throws Exception {
    // given
    User user = new User();
    user.setEmail("test@email.com");
    user.setUsername("firstnamelastname");
    user.setPassword("password");
    user.setToken("1234");
    user.setStatus(UserStatus.OFFLINE);

    List<User> allUsers = Collections.singletonList(user);

    // this mocks the UserService -> we define above what the userService should
    // return when getUsers() is called
    given(userService.getUsers()).willReturn(allUsers);

    // when
    MockHttpServletRequestBuilder getRequest = get("/users").contentType(MediaType.APPLICATION_JSON)
            .header("token", user.getToken());;

    // then
    mockMvc.perform(getRequest).andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].username", is(user.getUsername())))
        .andExpect(jsonPath("$[0].status", is(user.getStatus().toString())));
  }

    @Test
    public void givenUsers_whenGetUsersWithoutValidToken_thenThrow401() throws Exception {
        // given
        User user = new User();
        user.setEmail("test@email.com");
        user.setUsername("firstnamelastname");
        user.setPassword("password");
        user.setToken("1234");
        user.setStatus(UserStatus.OFFLINE);

        Mockito.doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, String.format("unauthorized"))).when(userService).verifyUserByToken(Mockito.any());
        MockHttpServletRequestBuilder getRequest = get("/users").contentType(MediaType.APPLICATION_JSON)
                .header("token", "12");

        mockMvc.perform(getRequest)
                .andExpect(status().isUnauthorized());
    }


  @Test
  public void createUser_validInput_userCreated() throws Exception {
    // given
    User user = new User();
    user.setId(1L);
    user.setEmail("test@email.com");
    user.setUsername("testUsername");
    user.setPassword("password");
    user.setToken("1");
    user.setStatus(UserStatus.ONLINE);

    UserPostDTO userPostDTO = new UserPostDTO();
    userPostDTO.setEmail("test@email.com");
    userPostDTO.setUsername("testUsername");
    userPostDTO.setPassword("password");

    given(userService.createUser(Mockito.any())).willReturn(user);

    MockHttpServletRequestBuilder postRequest = post("/users/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(userPostDTO));

    mockMvc.perform(postRequest)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id", is(user.getId().intValue())))
        .andExpect(jsonPath("$.username", is(user.getUsername())))
        .andExpect(jsonPath("$.status", is(user.getStatus().toString())));
  }

    @Test
    public void createUser_invalidInput_throws400() throws Exception {
        // given
        User user = new User();
        user.setId(1L);
        user.setEmail("test@email.com");
        user.setUsername("");
        user.setPassword("password");
        user.setToken("1");
        user.setStatus(UserStatus.ONLINE);

        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setEmail("test@email.com");
        userPostDTO.setUsername("");
        userPostDTO.setPassword("password");

        given(userService.createUser(Mockito.any())).willReturn(user);

        MockHttpServletRequestBuilder postRequest = post("/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(userPostDTO));

        Mockito.doThrow(new ResponseStatusException(HttpStatus.CONFLICT, String.format("username cannot be empty"))).when(userService).createUser(Mockito.any());

        mockMvc.perform(postRequest)
                .andExpect(status().isConflict());
    }

    @Test
    public void createUser_alreadyExits_throws409() throws Exception {

        User user = new User();
        user.setId(1L);
        user.setUsername("testUsername");
        user.setPassword("password");
        user.setEmail("test@email.com");
        user.setToken("1");
        user.setStatus(UserStatus.ONLINE);

        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setUsername(user.getUsername());
        userPostDTO.setPassword(user.getPassword());

        MockHttpServletRequestBuilder postRequest = post("/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(userPostDTO));

        given(userService.setOnline(Mockito.any())).willReturn(user);
        Mockito.doThrow(new ResponseStatusException(HttpStatus.CONFLICT, String.format("Username testUsername already taken"))).when(userService).createUser(Mockito.any());

        mockMvc.perform(postRequest)
                .andExpect(status().isConflict());
    }

    @Test
    public void login_validInput() throws Exception {
        // given
        User user = new User();
        user.setId(1L);
        user.setEmail("test@email.com");
        user.setUsername("testUsername");
        user.setPassword("password");
        user.setToken("1");
        user.setStatus(UserStatus.ONLINE);

        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setEmail("test@email.com");
        userPostDTO.setUsername("testUsername");
        userPostDTO.setPassword("password");

        given(userService.createUser(Mockito.any())).willReturn(user);

        MockHttpServletRequestBuilder postRequest = post("/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(userPostDTO));

        mockMvc.perform(postRequest)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(user.getId().intValue())))
                .andExpect(jsonPath("$.username", is(user.getUsername())))
                .andExpect(jsonPath("$.status", is(user.getStatus().toString())));
    }


    @Test
    public void putUser_validEdit_returnUpdatedUser() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@email.com");
        user.setUsername("testUsername");
        user.setPassword("password");
        user.setToken("1");
        user.setStatus(UserStatus.ONLINE);

        given(userService.verifyUserByToken(user.getToken())).willReturn(user);
        given(userService.verifyTokenAndId(user.getToken(), user.getId())).willReturn(user);
        given(userService.editUser(user.getId(), user)).willReturn(user);

        MockHttpServletRequestBuilder putRequest = put("/dashboard/" + user.getId() + "/profile/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new JSONObject().put("username","testUsername2").put("countryoforigin","jamaica").toString())
                .header("token", user.getToken());

        mockMvc.perform(putRequest)
                .andExpect(status().isNoContent());
    }

    @Test
    public void putUser_unauthorizedEdit_throws401() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@email.com");
        user.setUsername("testUsername");
        user.setPassword("password");
        user.setToken("1");
        user.setStatus(UserStatus.ONLINE);

        given(userService.verifyUserByToken(user.getToken())).willReturn(user);
        Mockito.doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, String.format("Unauthorized"))).when(userService).verifyTokenAndId(Mockito.anyString(), Mockito.anyLong());

        MockHttpServletRequestBuilder putRequest = put("/dashboard/" + 69 + "/profile/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new JSONObject().put("username",user.getUsername()).put("birthDate","2024-03-02").toString())
                .header("token", user.getToken());

        mockMvc.perform(putRequest)
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void putUser_userNotFound_throws404() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@email.com");
        user.setUsername("testUsername");
        user.setPassword("password");
        user.setToken("1");
        user.setStatus(UserStatus.ONLINE);

        when(userService.editUser(Mockito.anyLong(), Mockito.any())).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("user with userId: 1 was not found")));
        when(userService.verifyUserByToken(Mockito.anyString())).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("user with userId: 1 was not found")));

        MockHttpServletRequestBuilder putRequest = put("/dashboard/" + user.getId() + "/profile/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new JSONObject().put("username",user.getUsername()).put("birthDate","2024-03-02").toString())
                .header("token", user.getToken());

        mockMvc.perform(putRequest)
                .andExpect(status().isNotFound());
    }

    @Test
    public void validPasswordReset_sendsOneTimePassword() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@email.com");
        user.setUsername("testUsername");
        user.setPassword("password");
        user.setToken("1");
        user.setStatus(UserStatus.ONLINE);

        given(userService.getUserByEmail(user.getEmail())).willReturn(user);

        MockHttpServletRequestBuilder postRequest = post("/users/password-reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new JSONObject().put("username","testUsername").put("email","test@email.com").toString());

        mockMvc.perform(postRequest)
                .andExpect(status().isOk());
    }

    @Test
    public void invalidPasswordReset_throws401() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@email.com");
        user.setUsername("testUsername");
        user.setPassword("password");
        user.setToken("1");
        user.setStatus(UserStatus.ONLINE);

        Mockito.doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, String.format("Unauthorized"))).when(userService).getUserByEmail(Mockito.anyString());

        MockHttpServletRequestBuilder postRequest = post("/users/password-reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new JSONObject().put("username","testUsername2").put("email","test@email.com").toString());

        mockMvc.perform(postRequest)
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void validLogin_ReturnsUserAndToken() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("validUsername");
        user.setPassword("validPassword");
        user.setEmail("test@email.com");
        user.setToken("12345");
        user.setStatus(UserStatus.ONLINE);

        UserGetDTO userGetDTO = new UserGetDTO();
        userGetDTO.setId(user.getId());
        userGetDTO.setUsername(user.getUsername());
        userGetDTO.setStatus(user.getStatus());

        given(userService.authenticateUser(Mockito.anyString(), Mockito.anyString())).willReturn(user);
        given(userService.setOnline(Mockito.anyString())).willReturn(user);

        MockHttpServletRequestBuilder postRequest = post("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new JSONObject()
                        .put("username", user.getUsername())
                        .put("password", user.getPassword())
                        .toString());

        mockMvc.perform(postRequest)
                .andExpect(status().isCreated())
                .andExpect(header().string("token", user.getToken()))
                .andExpect(jsonPath("$.username").value(user.getUsername()))
                .andExpect(jsonPath("$.status").value(user.getStatus().name()));
    }

    @Test
    public void invalidLogin_ReturnsUserAndToken() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("validUsername");
        user.setPassword("validPassword");
        user.setEmail("test@email.com");
        user.setToken("12345");
        user.setStatus(UserStatus.ONLINE);

        UserGetDTO userGetDTO = new UserGetDTO();
        userGetDTO.setId(user.getId());
        userGetDTO.setUsername(user.getUsername());
        userGetDTO.setStatus(user.getStatus());

        given(userService.setOnline(Mockito.anyString())).willReturn(user);
        Mockito.doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, String.format("Unauthorized"))).when(userService).authenticateUser(Mockito.anyString(), Mockito.anyString());

        MockHttpServletRequestBuilder postRequest = post("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new JSONObject()
                        .put("username", user.getUsername())
                        .put("password", "fakepassword")
                        .toString());

        mockMvc.perform(postRequest)
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void validLogout_isOK() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("validUsername");
        user.setPassword("validPassword");
        user.setEmail("test@email.com");
        user.setToken("12345");
        user.setStatus(UserStatus.ONLINE);

        given(userService.setOffline(Mockito.anyString())).willReturn(user);
        given(userService.verifyUserByToken(Mockito.anyString())).willReturn(user);
        given(userService.verifyTokenAndId(Mockito.anyString(), Mockito.anyLong())).willReturn(user);

        MockHttpServletRequestBuilder postRequest = post("/dashboard/" + user.getId() + "/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new JSONObject().toString())
                .header("token", user.getToken());

        mockMvc.perform(postRequest)
                .andExpect(status().isOk());
    }

    @Test
    public void invalidLogout_throws401() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("validUsername");
        user.setPassword("validPassword");
        user.setEmail("test@email.com");
        user.setToken("12345");
        user.setStatus(UserStatus.ONLINE);

        given(userService.setOffline(Mockito.anyString())).willReturn(user);
        Mockito.doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, String.format("Unauthorized"))).when(userService).verifyTokenAndId(Mockito.anyString(), Mockito.anyLong());

        MockHttpServletRequestBuilder postRequest = post("/dashboard/" + user.getId() + "/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new JSONObject().toString())
                        .header("token", "69");


        mockMvc.perform(postRequest)
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void givenUsers_whenGetUsersStats_thenReturnJsonArray() throws Exception {
        User user = new User();
        user.setToken("123");
        user.setId(1L);

        Mockito.when(userService.getUsersWithStats()).thenReturn(Collections.singletonList(user));
        Mockito.when(userService.verifyUserByToken("123")).thenReturn(user);

        mockMvc.perform(get("/dashboard/" + user.getId() + "/profile/stats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("token", user.getToken()))
                .andExpect(status().isOk());
    }

    @Test
    public void givenUsers_whenGetUserStats_throws401() throws Exception {
        User user = new User();
        user.setToken("123");
        user.setId(1L);

        Mockito.doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthorized"))
                .when(userService).verifyUserByToken("456");

        mockMvc.perform(get("/dashboard/" + user.getId() + "/profile/stats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("token", "456"))
                        .andExpect(status().isUnauthorized());
    }
    @Test
    public void whenGetOwnProfile_thenReturnsUserProfile() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@email.com");
        user.setUsername("testUsername");
        user.setPassword("password");
        user.setToken("1");
        user.setStatus(UserStatus.ONLINE);
        String token = "123";
        given(userService.verifyUserByToken(token)).willReturn(user);
        given(userService.getProfileUser(user.getId(), user)).willReturn(user);

        mockMvc.perform(get("/dashboard/{userId}/profile", user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is(user.getUsername())));
    }

    @Test
    public void whenGetOtherUserProfile_thenReturnsUserProfile() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@email.com");
        user.setUsername("testUsername");
        user.setPassword("password");
        user.setToken("1");
        user.setStatus(UserStatus.ONLINE);
        String token = "123";
        Long userId = 2L;
        User otherUser = new User();

        given(userService.verifyUserByToken(token)).willReturn(user);
        given(userService.getProfileUser(userId, user)).willReturn(otherUser);

        mockMvc.perform(get("/dashboard/{userId}/profile", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is(otherUser.getUsername())));
    }

    @Test
    public void friendshipRequestIsCreated() throws Exception {
        Long userId = 1L;
        String token = "sample-token";

        mockMvc.perform(put("/dashboard/{userId}/friends/requests", userId)
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(userService).addFriends(eq(userId), eq(token));
    }

    @Test
    public void getPendingFriendshipRequests_Success() throws Exception {
        Long userId = 1L;
        String token = "valid-token";

        User user = new User();
        user.setId(userId);
        Mockito.when(userService.verifyTokenAndId(token, userId)).thenReturn(user);

        List<UserFriendsRequests> mockRequests = new ArrayList<>();

        Mockito.when(userService.getPendingFriendshipRequests(userId)).thenReturn(mockRequests);

        mockMvc.perform(get("/dashboard/{userId}/friends/requests", userId)
                        .header("token", token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(mockRequests.size())));

        Mockito.verify(userService).getPendingFriendshipRequests(userId);
    }

    @Test
    public void getPendingFriendshipRequests_Unauthorized() throws Exception {
        Long userId = 1L;
        String invalidToken = "invalid-token";

        Mockito.doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED)).when(userService).verifyTokenAndId(invalidToken, userId);

        mockMvc.perform(get("/dashboard/{userId}/friends/requests", userId)
                        .header("token", invalidToken))
                .andExpect(status().isUnauthorized());

        Mockito.verify(userService).verifyTokenAndId(invalidToken, userId);
    }

    @Test
    public void getPendingFriendshipRequests_NoContent() throws Exception {
        Long userId = 1L;
        String token = "valid-token";

        Mockito.when(userService.verifyTokenAndId(token, userId)).thenReturn(new User());
        Mockito.when(userService.getPendingFriendshipRequests(userId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/dashboard/{userId}/friends/requests", userId)
                        .header("token", token))
                .andExpect(status().isOk())
                .andExpect(content().string("[]"));

        Mockito.verify(userService).getPendingFriendshipRequests(userId);
    }

    @Test
    public void processFriendRequest_Success() throws Exception {
        Long userId = 1L;
        Long requestId = 2L;
        String token = "valid-token";
        UserFriendsRequestPutDTO requestDto = new UserFriendsRequestPutDTO();
        requestDto.setStatus(FriendRequestStatus.ACCEPTED);

        mockMvc.perform(put("/dashboard/{userId}/friends/requests/{requestId}", userId, requestId)
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(requestDto)))
                .andExpect(status().isNoContent());

        verify(userService).editFriends(userId, requestId, requestDto.getStatus());
    }

    @Test
    public void getUserFriends_Success() throws Exception {
        Long userId = 1L;
        String token = "valid-token";

        User verifiedUser = new User();
        verifiedUser.setId(userId);
        verifiedUser.setUsername("verifiedUser");
        verifiedUser.setAvatar("avatarUrl");

        User friend1 = new User();
        friend1.setId(2L);
        friend1.setUsername("friend1");
        friend1.setAvatar("avatarUrl1");

        User friend2 = new User();
        friend2.setId(3L);
        friend2.setUsername("friend2");
        friend2.setAvatar("avatarUrl2");

        List<User> friends = Arrays.asList(friend1, friend2);

        when(userService.verifyTokenAndId(token, userId)).thenReturn(verifiedUser);
        when(userService.getUsersFriends(userId)).thenReturn(friends);

        mockMvc.perform(get("/dashboard/{userId}/friends", userId)
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].friendName", is(friend1.getUsername())))
                .andExpect(jsonPath("$[0].friendAvatar", is(friend1.getAvatar())))
                .andExpect(jsonPath("$[1].friendName", is(friend2.getUsername())))
                .andExpect(jsonPath("$[1].friendAvatar", is(friend2.getAvatar())));

        verify(userService).verifyTokenAndId(token, userId);
        verify(userService).getUsersFriends(userId);
    }

    @Test
    public void whenGetPendingFriendshipRequestsIsEmpty_thenReturnEmptyList() throws Exception {
        Long userId = 1L;
        String token = "valid-token";
        User user = new User();
        user.setId(userId);
        when(userService.verifyTokenAndId(token, userId)).thenReturn(user);
        when(userService.getPendingFriendshipRequests(userId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/dashboard/{userId}/friends/requests", userId)
                        .header("token", token))
                .andExpect(status().isOk())
                .andExpect(content().string("[]"));
    }

    @Test
    public void whenGetPendingFriendshipRequestsHasSingleItem_thenReturnSingleDTO() throws Exception {
        Long userId = 1L;
        String token = "valid-token";
        User user = new User();
        user.setId(userId);
        UserFriendsRequests request = new UserFriendsRequests();
        request.setId(2L);  // Sample properties
        request.setStatus(FriendRequestStatus.PENDING);

        when(userService.verifyTokenAndId(token, userId)).thenReturn(user);
        when(userService.getPendingFriendshipRequests(userId)).thenReturn(Collections.singletonList(request));

        mockMvc.perform(get("/dashboard/{userId}/friends/requests", userId)
                        .header("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    public void whenGetPendingFriendshipRequestsHasMultipleItems_thenReturnMultipleDTOs() throws Exception {
        Long userId = 1L;
        String token = "valid-token";
        User user = new User();
        user.setId(userId);
        UserFriendsRequests request1 = new UserFriendsRequests();
        request1.setId(2L);
        request1.setStatus(FriendRequestStatus.PENDING);
        UserFriendsRequests request2 = new UserFriendsRequests();
        request2.setId(3L);
        request2.setStatus(FriendRequestStatus.PENDING);

        when(userService.verifyTokenAndId(token, userId)).thenReturn(user);
        when(userService.getPendingFriendshipRequests(userId)).thenReturn(Arrays.asList(request1, request2));

        mockMvc.perform(get("/dashboard/{userId}/friends/requests", userId)
                        .header("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }


  /**
   * Helper Method to convert userPostDTO into a JSON string such that the input
   * can be processed
   * Input will look like this: {"name": "Test User", "username": "testUsername"}
   * 
   * @param object
   * @return string
   */
  private String asJsonString(final Object object) {
    try {
      return new ObjectMapper().writeValueAsString(object);
    } catch (JsonProcessingException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          String.format("The request body could not be created.%s", e.toString()));
    }
  }
}