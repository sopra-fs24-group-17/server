package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPutDTO;
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

  @GetMapping("/users")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public List<UserGetDTO> getAllUsers() {
    // fetch all users in the internal representation
    List<User> users = userService.getUsers();
    List<UserGetDTO> userGetDTOs = new ArrayList<>();
    // convert each user to the API representation
    for (User user : users) {
      userGetDTOs.add(DTOMapper.INSTANCE.convertEntityToUserGetDTO(user));
    }
    return userGetDTOs;
  }

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

  @PostMapping("/users/login")
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public UserGetDTO loginUser(@RequestBody UserPostDTO userPostDTO, HttpServletResponse response) {
      // authenticate user
      User authenticatedUser = userService.authenticateUser(userPostDTO.getUsername(), userPostDTO.getPassword());
      if (authenticatedUser == null) {
          throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
      }
      User userOnline = userService.setOnline(authenticatedUser.getUsername());
      // add authentication token to request header
      response.setHeader("token", userOnline.getToken());
      // convert internal representation of user back to API
      return DTOMapper.INSTANCE.convertEntityToUserGetDTO(userOnline);
  }

    @PostMapping("dashboard/{userId}/logout")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void logout(@PathVariable Long userId, @RequestHeader("token") String token) {
      User verifiedUser = userService.verifyTokenAndId(token, userId);
      User userOffline = userService.setOffline(userService.getUserByToken(token).getUsername());
      return;
    }

    @PostMapping("users/password-reset")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void resetPassword(@RequestBody UserPostDTO userPostDTO) {
      User userByEmail = userService.getUserByEmail(userPostDTO.getEmail());
      userService.resetPassword(userByEmail, userPostDTO);
    }

    @PutMapping("dashboard/{userId}/profile")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResponseBody
    public UserGetDTO editProfile(@RequestBody UserPutDTO userPutDTO, @PathVariable Long userId, @RequestHeader("token") String token) {
        User verifiedUser = userService.verifyTokenAndId(token, userId);
        User updatedUser = userService.editUser(userId, DTOMapper.INSTANCE.convertUserPutDTOtoEntity(userPutDTO));
        return DTOMapper.INSTANCE.convertEntityToUserGetDTO(updatedUser);
    }
}
