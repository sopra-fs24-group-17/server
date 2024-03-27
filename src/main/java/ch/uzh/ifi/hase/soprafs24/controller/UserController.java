package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPutDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserStatsGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@RestController
public class UserController {
  private final UserService userService;
  UserController(UserService userService) {
    this.userService = userService;
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
      userGetDTOs.add(DTOMapper.INSTANCE.convertEntityToUserGetDTO(user));
    }
    return userGetDTOs;
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
    User userInput = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);
    // create user
    User createdUser = userService.createUser(userInput);
    User userOnline = userService.setOnline(createdUser.getUsername());
    // add authentication token to response header
    response.setHeader("token", createdUser.getToken());
    // convert internal representation of user back to API
    return DTOMapper.INSTANCE.convertEntityToUserGetDTO(createdUser);
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
      return DTOMapper.INSTANCE.convertEntityToUserGetDTO(userOnline);
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
        User updatedUser = userService.editUser(userId, DTOMapper.INSTANCE.convertUserPutDTOtoEntity(userPutDTO));
        return DTOMapper.INSTANCE.convertEntityToUserGetDTO(updatedUser);
    }

    /**
     * API endpoint to retrieve the user statistics for the leaderboard.
     * @param token of the user requesting the user statistics overview.
     * @return list of all user objects (username) and their statistics.
     */
    @GetMapping("/dashboard/{userId}/profile/stats")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<UserStatsGetDTO> getAllUsersStats(@RequestHeader("token") String token) {
      // verify that token and userId belong to the same user
      User verifiedUser =  userService.getUserByToken(token);
      // fetch users along with their statistics
      List<User> users = userService.getUsersWithStats();
        List<UserStatsGetDTO> userStatsGetDTOs = new ArrayList<>();
        for (User user : users) {
            userStatsGetDTOs.add(DTOMapper.INSTANCE.convertEntityToUserStatsGetDTO(user));
        }
        return userStatsGetDTOs;
    }
}
