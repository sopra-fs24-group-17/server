package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.ProfileVisibility;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class UserService {
  private final Logger log = LoggerFactory.getLogger(UserService.class);
  private final UserRepository userRepository;
  private final PasswordService passwordService;
  private final EmailSenderService emailSenderService;

  private final UserFriendsService userFriendsService;

  @Autowired
  public UserService(@Qualifier("userRepository") UserRepository userRepository, PasswordService passwordService, EmailSenderService emailSenderService, UserFriendsService userFriendsService) {
    this.userRepository = userRepository;
    this.passwordService = passwordService;
    this.emailSenderService = emailSenderService;
    this.userFriendsService = userFriendsService;
  }

  /**
   * Returns a list of all users in the user repository.
   * @return a list of all users entities in the user repository.
   */
  public List<User> getUsers() {return this.userRepository.findAll();}

  /**
   * Returns a list of all users in the user repository along with their statistics.
   * @return a list of all users entities in the user repository along with their statisticcs.
   */
  public List<User> getUsersWithStats() {return this.userRepository.findAllWithStatistics();}

  /**
   * Creates a new user in the user repository.
   * @param newUser the object of the user to be created.
   * @return the created user object.
   */
  public User createUser(User newUser) {
    newUser.setToken(UUID.randomUUID().toString());
    newUser.setStatus(UserStatus.OFFLINE);
    checkIfUserNameExists(newUser);
    checkIfEmailExists(newUser);

    if (newUser.getPassword().replaceAll("\\s+", "").equals("")){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "password cannot be empty");
    }
    String hashedPassword = passwordService.securePassword(newUser.getPassword());
    newUser.setPassword(hashedPassword);

    newUser = userRepository.save(newUser);
    userRepository.flush();

    log.debug("Created Information for User: {}", newUser);
    return newUser;
  }

  /**
   * Checks if a username already exists in the user repository.
   * @param userToBeCreated user object of the user to be created.
   */
  private void checkIfUserNameExists(User userToBeCreated) {
      if (userToBeCreated.getUsername().replaceAll("\\s+", "").equals("")) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username cannot be empty");
      }
      User userByUserName = userRepository.findByUsername(userToBeCreated.getUsername());
      if (userByUserName != null) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                  String.format("Username %s already taken", userToBeCreated.getUsername()));
      }
  }

  /**
   * Checks if a userid belongs to a valid user in the repository.
   * @param userId userId of the profile that gets accessed.
   * @param invokingUser use object of the user that accesses the profile.
   * @return user object corresponding to the profile
   */
  public User getProfileUser(Long userId, User invokingUser){
      User profileUser = userRepository.findUserById(userId);
      if (profileUser == null) {
          throw new ResponseStatusException(HttpStatus.NOT_FOUND, "user profile not found");
      }
      // if profile visibility is set to false, only friends can access the profile
      if (profileUser.getProfilevisibility() == ProfileVisibility.FALSE && !Objects.equals(invokingUser.getId(), userId)) {
          if (!userFriendsService.areUsersFriends(profileUser, invokingUser)) {
              throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthorized to access this profile");
          }
      }
      return profileUser;
  }

  /**
   * Checks if an email address already exists in the user repository.
   * @param userToBeCreated user object of the user to be created.
   */
  private void checkIfEmailExists(User userToBeCreated) {
      if (!userToBeCreated.getEmail().contains("@")) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email address is invalid");
      }
      User userByEmail = userRepository.findByEmail(userToBeCreated.getEmail());
      if (userByEmail != null) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                  String.format("Email %s already taken", userToBeCreated.getEmail()));
      }
  }

  /**
   * Invokes user credential verification after a login attempt.
   * @param username of the user trying to login.
   * @param password of the user trying to login.
   * @return the user object if the credentials are valid.
   */
  public User authenticateUser(String username, String password) {
      User user = userRepository.findByUsername(username);
      if (user == null || !(passwordService.verifyPassword(user.getPassword(), password))) {
          throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
      }
      return user;
  }

  /**
   * Verifies that a user is accessing/modifying his profile by comparing the profile userId with the userId associated with the token provided.
   * @param token of the user provided through a request header.
   * @param userId of the accessed profile.
   * @return the user object if the userId matches.
   */
  public User verifyTokenAndId(String token, Long userId) {
      if (getUserByToken(token).getId() != userId) {
          throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
      }
      return getUserByToken(token);
  }

  /**
   * Invoked after logout, to change the status of a user to offline.
   * @param username of the user trying to logout.
   * @return if the user has been online, the status is changed to offline and the user object is returned.
   */
  public User setOnline(String username) {
      User userByUsername = userRepository.findByUsername(username);

      if (userByUsername.getStatus() == UserStatus.ONLINE) {
          throw new ResponseStatusException(HttpStatus.FORBIDDEN, "user is already logged in");
      }
      userByUsername.setStatus(UserStatus.ONLINE);
      userByUsername = userRepository.save(userByUsername);
      userRepository.flush();
      return userByUsername;
  }

  /**
   * Invoked after login, to change the status of a user to online.
   * @param username of the user trying to login.
   * @return if the user has been offline, the status is changed to online and the user object is returned.
   */
  public User setOffline(String username) {
      User userByUsername = userRepository.findByUsername(username);

      if (userByUsername.getStatus() == UserStatus.OFFLINE) {
          throw new ResponseStatusException(HttpStatus.FORBIDDEN, "user is not logged in");
      }
      userByUsername.setStatus(UserStatus.OFFLINE);
      userByUsername = userRepository.save(userByUsername);
      userRepository.flush();
      return userByUsername;
  }

  /**
   * Verifies if a token is valid, i.e. belongs to an actual user.
   * @param token the token provided through the header of an api request from the client.
   * @return if the user is valid, the corresponding user object is returned.
   */
  public User getUserByToken(String token) {
      User userByToken = userRepository.findUserByToken(token);
      if (userByToken == null) {
          throw new ResponseStatusException(HttpStatus.NOT_FOUND, "invalid token");
      }
      return userByToken;
  }

  /**
   * Retrieves a user from the user repository by email.
   * @param email the email that is to be looked-up in the repo.
   * @return the corresponding user object, if found.
   */
  public User getUserByEmail(String email) {
      User userByEmail = userRepository.findByEmail(email);
      if (userByEmail == null) {
          throw new ResponseStatusException(HttpStatus.NOT_FOUND, "invalid email");
      }
      return userByEmail;
  }

  /**
   * Invokes a password reset for a user following a Post request.
   * @param user the user object that matches the provided email address in the password reset attempt.
   * @param userPostDTO the DTO holding username and email address of the user requiring password reset.
   * @return if the user is valid, a one time password (otp) is sent by email and the otp flag is set to true.
   */
  public void resetPassword(User user, UserPostDTO userPostDTO) {
      if (!user.getUsername().equals(userPostDTO.getUsername())){
          throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid user credentials");
      }
      String oneTimePassword = passwordService.generateRandomPassword(10);
      user.setPassword(passwordService.securePassword(oneTimePassword));
      user.setOtp(true); //OneTimePassword Flag set to true

      // Send out Email with oneTimePassword
      emailSenderService.sendNewPassword(user.getEmail(), user.getUsername(), oneTimePassword);
  }

  /**
   * Edits a user object in the user repository.
   * @param userId the userId of the user to be modified.
   * @param modifiedUser the user object resulting from the PUT-DTO holding the required modifications.
   * @return the modified user object from the user repository
   */
  public User editUser(Long userId, User modifiedUser) {
      User user = userRepository.findUserById(userId);
      if (user == null){
          throw new ResponseStatusException(HttpStatus.NOT_FOUND, "user does not exist in DB");
      }
      boolean isUpdated = false;
      // Username Update
       if (modifiedUser.getUsername() != null && !user.getUsername().equals(modifiedUser.getUsername())) {
           checkIfUserNameExists(modifiedUser);
           user.setUsername(modifiedUser.getUsername());
           isUpdated = true;
       }
       // Password Update
       if (modifiedUser.getPassword() != null) {
           if (modifiedUser.getPassword().replaceAll("\\s+", "").isEmpty()){
               throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "password cannot be empty");
           }
           if (!passwordService.securePassword(modifiedUser.getPassword()).equals(user.getPassword())) {
               user.setPassword(passwordService.securePassword(modifiedUser.getPassword()));
               user.setOtp(false); // One Time Password Flag set to false
               isUpdated = true;
           }
       }
       // Email Update
       if (modifiedUser.getEmail() != null && ! modifiedUser.getEmail().equals(user.getEmail())) {
           checkIfEmailExists(modifiedUser);
           user.setEmail(modifiedUser.getEmail());
           isUpdated = true;
       }
       // Birthdate Update
       if (modifiedUser.getBirthdate() != null && !modifiedUser.getBirthdate().equals(user.getBirthdate())) {
           user.setBirthdate(modifiedUser.getBirthdate());
           isUpdated = true;
       }
       // Countryoforigin Update
       if (modifiedUser.getCountryoforigin() != null && !modifiedUser.getCountryoforigin().equals(user.getCountryoforigin())) {
           user.setCountryoforigin(modifiedUser.getCountryoforigin());
           isUpdated = true;
       }
       // Profilevisibility Update
       if (modifiedUser.getProfilevisibility() != null && !modifiedUser.getProfilevisibility().equals(user.getProfilevisibility())) {
           user.setProfilevisibility(modifiedUser.getProfilevisibility());
           isUpdated = true;
       }
       // Tutorialflag Update
       if (modifiedUser.getTutorialflag() != null && !modifiedUser.getTutorialflag().equals(user.getTutorialflag())) {
           user.setTutorialflag(modifiedUser.getTutorialflag());
           isUpdated = true;
       }
       // Avatar Update
       if (modifiedUser.getAvatar() != null && !modifiedUser.getAvatar().equals(user.getAvatar())) {
           user.setAvatar(modifiedUser.getAvatar());
           isUpdated = true;
       }
       if (isUpdated) {
           userRepository.save(user);
           userRepository.flush();
           log.debug("Updated Information for User: {}", user);
       }
       return user;
  }
}