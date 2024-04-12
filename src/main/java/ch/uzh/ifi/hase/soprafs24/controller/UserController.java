package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.Notification;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserFriendsRequests;
import ch.uzh.ifi.hase.soprafs24.rest.dto.*;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.GameDTOMapper;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.NotificationDTOMapper;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.UserDTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.UserFriendsService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
public class UserController {
  private final UserService userService;
  //private final ImageService imageService;

  @Value("${app.static.resource.path}")
  private String staticResourcePath;

  UserController(UserService userService/*, ImageService imageService*/) {
    this.userService = userService;
    //this.imageService = imageService;
  }

  /**
   * API endpoint to get a user overview.
   * @param token of the user requesting access to the overview page.
   * @return a list containing all the userDTOs.
   */
  @GetMapping("/users")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public List<UserGetDTO> getAllUsers(@RequestHeader("token") String token) {
    User verifiedUser = userService.getUserByToken(token);
    List<User> users = userService.getUsers();
    List<UserGetDTO> userGetDTOs = new ArrayList<>();
    // convert each user to the API representation
    for (User user : users) {
      userGetDTOs.add(UserDTOMapper.INSTANCE.convertEntityToUserGetDTO(user));
    }
    return userGetDTOs;
  }

  /**
   * API endpoint to get a user profile.
   * @param userId containing username, email and password.
   * @param token token of the user requesting access to the profile.
   * @return the created user object.
   */
  @GetMapping("/dashboard/{userId}/profile")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public UserGetDTO getProfileUser(@PathVariable Long userId, @RequestHeader("token") String token) {
      User verifiedUser = userService.getUserByToken(token);
      User profileUser = userService.getProfileUser(userId, verifiedUser);
      return UserDTOMapper.INSTANCE.convertEntityToProfileUserGetDTO(profileUser);
  }

  /**
   * API endpoint to register a user.
   * @param userPostDTO containing username, email and password.
   * @return the created user object.
   */
  @PostMapping("/users/register")
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public UserGetDTO createUser(@RequestBody UserPostDTO userPostDTO, HttpServletResponse response) {
    // convert API user to internal representation
    User userInput = UserDTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);
    // create user
    User createdUser = userService.createUser(userInput);
    User userOnline = userService.setOnline(createdUser.getUsername());
    // add authentication token to response header
    response.setHeader("token", createdUser.getToken());
    // convert internal representation of user back to API
    return UserDTOMapper.INSTANCE.convertEntityToUserGetDTO(createdUser);
  }

  /**
   * API endpoint to login a user.
   * @param userPostDTO containing username and password.
   * @return the corresponding user object.
   */
  @PostMapping("/users/login")
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public UserGetDTO loginUser(@RequestBody UserPostDTO userPostDTO, HttpServletResponse response) {
      // authenticate user
      User authenticatedUser = userService.authenticateUser(userPostDTO.getUsername(), userPostDTO.getPassword());
      User userOnline = userService.setOnline(authenticatedUser.getUsername());
      // add authentication token to request header
      response.setHeader("token", userOnline.getToken());
      // convert internal representation of user back to API
      return UserDTOMapper.INSTANCE.convertEntityToUserGetDTO(userOnline);
  }

  /**
   * API endpoint to logout a user.
   * @param userId the userId of the user to be logged-out.
   * @param token of the user invoking the logout process.
   */
  @PostMapping("dashboard/{userId}/logout")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public void logout(@PathVariable Long userId, @RequestHeader("token") String token) {
      // verify that token and userId belong to the same user
      User verifiedUser = userService.verifyTokenAndId(token, userId);
      User userOffline = userService.setOffline(userService.getUserByToken(token).getUsername());
      return;
    }

    /**
     * API endpoint to invoke a password reset
     * @param userPostDTO containing username and email.
     */
    @PostMapping("users/password-reset")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void resetPassword(@RequestBody UserPostDTO userPostDTO) {
      User userByEmail = userService.getUserByEmail(userPostDTO.getEmail());
      userService.resetPassword(userByEmail, userPostDTO);
    }

    /**
     * API endpoint to edit a user profile.
     * @param userPutDTO the (modified) user DTO
     * @param userId the userId of the profile to be edited.
     * @param token of the user invoking the edits
     */
    @PutMapping("dashboard/{userId}/profile")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResponseBody
    public UserGetDTO editProfile(@RequestBody UserPutDTO userPutDTO, @PathVariable Long userId, @RequestHeader("token") String token) {
        // verify that token and userId belong to the same user
        User verifiedUser = userService.verifyTokenAndId(token, userId);
        // cast updates
        User updatedUser = userService.editUser(userId, UserDTOMapper.INSTANCE.convertUserPutDTOtoEntity(userPutDTO));
        return UserDTOMapper.INSTANCE.convertEntityToUserGetDTO(updatedUser);
    }

    /**
     * API endpoint to retrieve the user statistics for the leaderboard.
     * @param token of the user requesting the user statistics overview.
     * @return list of all user objects (username) and their statistics.
     */
    @GetMapping("/dashboard/{userId}/profile/stats")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<UserStatsGetDTO> getAllUsersStats(@PathVariable Long userId, @RequestHeader("token") String token) {
      // verify that token and userId belong to the same user
      User verifiedUser =  userService.getUserByToken(token);
      // fetch users along with their statistics
      List<User> users = userService.getUsersWithStats();
      List<UserStatsGetDTO> userStatsGetDTOs = new ArrayList<>();
      for (User user : users) {
          userStatsGetDTOs.add(UserDTOMapper.INSTANCE.convertEntityToUserStatsGetDTO(user));
      }
      return userStatsGetDTOs;
    }

    /**
     * API endpoint to create a friendship request.
     * @param userId of the user receiving the friendship request.
     * @param token of the user initialising the friendship request.
     */
    @PutMapping("/dashboard/{userId}/friends/requests")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResponseBody
    public void friendRequest(@PathVariable Long userId, @RequestHeader("token") String token){
        userService.addFriends(userId, token);
    }

    /**
     * API endpoint to process friendship requests
     * @param userId of the user processing his/her friendship requests.
     * @param requestId of the friendship request.
     * @param token of the user processing his/her friendship requests.
     */
    @PutMapping("/dashboard/{userId}/friends/requests/{requestId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResponseBody
    public void processFriendRequest(@PathVariable Long userId, @PathVariable Long requestId, @RequestHeader("token") String token, @RequestBody UserFriendsRequestPutDTO requestDto) {
        // verify that token and userId belong to the same user
        User verifiedUser = userService.verifyTokenAndId(token, userId);
        userService.editFriends(userId, requestId, requestDto.getStatus());
    }

    /**
     * API endpoint to retrieve all pending friendship requests.
     * @param userId of the user who wants to fetch his/her friendship requests (received)
     * @param token of the user who wants to fetch his/her friendship requests (received)
     * @return all the retrieved userFriendsRequestGetDTOs
     */
    @GetMapping("/dashboard/{userId}/friends/requests")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<UserFriendsRequestGetDTO> getPendingFriendshipRequests(@PathVariable Long userId, @RequestHeader("token") String token) {
        // verify that token and userId belong to the same user
        User verifiedUser = userService.verifyTokenAndId(token, userId);

        List<UserFriendsRequests> requests = userService.getPendingFriendshipRequests(userId);
        List<UserFriendsRequestGetDTO> userFriendsRequestGetDTOS = new ArrayList<>();
        for (UserFriendsRequests userFriendsRequests : requests) {
            userFriendsRequestGetDTOS.add(UserDTOMapper.INSTANCE.convertEntityToUserFriendsRequestGetDTO(userFriendsRequests));
        }
        return userFriendsRequestGetDTOS;
    }

    /**
     * API endpoint to retrieve all friends of a given user.
     * @param userId of the user whose friends shall be retrieved.
     * @param token of the user whose friends shall be retrieved.
     * @return a list of all friend users (containing their username and their avatar)
     */
    @GetMapping("/dashboard/{userId}/friends")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List <FriendsGetDTO> getUserFriends(@PathVariable Long userId, @RequestHeader("token") String token) {
        // verify that token and userId belong to the same user
        User verifiedUser = userService.verifyTokenAndId(token, userId);

        List<User> friends = userService.getUsersFriends(userId);
        List<FriendsGetDTO> friendsGetDTOS = new ArrayList<>();
        for (User friend: friends) {
            FriendsGetDTO friendDTO = new FriendsGetDTO();
            friendDTO.setFriendName(friend.getUsername());
            friendDTO.setFriendAvatar(friend.getAvatar());
            friendDTO.setStatus(friend.getStatus());
            friendsGetDTOS.add(friendDTO);
        }
        return friendsGetDTOS;
    }


/*    @PostMapping("/dashboard/{userId}/profile/uploadAvatar")
    public String createAvatar(@PathVariable Long userId, @RequestParam("avatar") MultipartFile avatarImage) throws IOException {
        List<String> allowedMimeTypes = Arrays.asList("image/jpeg", "image/png", "image/webp");

        // Validate MIME type
        String mimeType = avatarImage.getContentType();
        if (!allowedMimeTypes.contains(mimeType)) {
            // If the file type is not allowed, you could throw an exception or handle it as needed
            throw new IllegalArgumentException("Invalid file type. Only JPEG, PNG, and WEBP are allowed.");
        }

        String uploadDirectory = staticResourcePath.endsWith("/") ? staticResourcePath : staticResourcePath + "/";
        uploadDirectory += "images/avatars/";
        String fileName = imageService.saveImageToStorage(uploadDirectory, avatarImage);
        String path = "/images/avatars/" + fileName;

        // Return the path as before
        return path;
    }*/

    /**
     * API endpoint to retrieve notifications belonging to a user.
     * @param token of the user requesting the list of notifications.
     * @return list of all notifications belonging to a given user.
     */
    @GetMapping("/dashboard/{userId}/notifications")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<NotificationGetDTO> getNotifications(@RequestHeader("token") String token) {
        // verify that token and userId belong to the same user
        User verifiedUser =  userService.getUserByToken(token);
        // fetch users along with their statistics
        List<Notification> notifications = userService.obtainNotifications(verifiedUser.getId());
        // Convert notifications to DTOs
        List<NotificationGetDTO> userNotificationsGetDTOs = new ArrayList<>();
        for (Notification notification : notifications) {
            userNotificationsGetDTOs.add(NotificationDTOMapper.INSTANCE.convertEntityToNotificationGetDTO(notification));
        }
        return userNotificationsGetDTOs;
    }

    /**
     * API endpoint to send a notification from the server to the client.
     * @param token of the user requesting the list of notifications.
     * @return list of all notifications belonging to a given user.
     */
    // Probably this is redundant. We can only have a notify function on service and the retrieve all notifications endpoint.
    @PostMapping("/dashboard/{userId}/notifications/send")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public UserGetDTO sendNotification(@RequestHeader("token") String token, @RequestParam Long userId, @RequestParam String message) {
        User user = userService.sendNotification(userId, message);
        // I think is a good idea to return the updated user so we can display the number of unread notifications
        return UserDTOMapper.INSTANCE.convertEntityToUserGetDTO(user);
    }

}
