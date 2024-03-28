package ch.uzh.ifi.hase.soprafs24.service;

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
import java.util.UUID;

@Service
@Transactional
public class UserService {
  private final Logger log = LoggerFactory.getLogger(UserService.class);
  private final UserRepository userRepository;
  private final PasswordService passwordService;
  private final EmailSenderService emailSenderService;

  @Autowired
  public UserService(@Qualifier("userRepository") UserRepository userRepository, PasswordService passwordService, EmailSenderService emailSenderService) {
    this.userRepository = userRepository;
    this.passwordService = passwordService;
    this.emailSenderService = emailSenderService;
  }

  public List<User> getUsers() {
    return this.userRepository.findAll();
  }

  public User findUserByUsername(String username) {
      return userRepository.findByUsername(username);
  }

  public User findUserByEmail(String email) {
      return userRepository.findByEmail(email);
  }

  public List<User> getUsersWithStats() {return this.userRepository.findAllWithStatistics();}

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

  public User authenticateUser(String username, String password) {
      User user = userRepository.findByUsername(username);
      if (user != null && (passwordService.verifyPassword(user.getPassword(), password))) {
          return user;
      }
      return null;
  }

  public User verifyTokenAndId(String token, Long userId) {
      if (getUserByToken(token).getId() != userId) {
          throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
      }
      return getUserByToken(token);
  }

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

  public User getUserByToken(String token) {
      User userByToken = userRepository.findUserByToken(token);
      if (userByToken == null) {
          throw new ResponseStatusException(HttpStatus.NOT_FOUND, "invalid token");
      }
      return userByToken;
  }

  public User verifyToken(String token) {
      User userByToken = userRepository.findUserByToken(token);
      if (userByToken == null) {
          throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthorized");
      }
      return userByToken;
  }

  public User getUserByEmail(String email) {
      User userByEmail = userRepository.findByEmail(email);
      if (userByEmail == null) {
          throw new ResponseStatusException(HttpStatus.NOT_FOUND, "invalid email");
      }
      return userByEmail;
  }

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

   public User editUser(Long userId, User modifiedUser) {
      User user = userRepository.findUserById(userId);
      if (user == null){
          throw new ResponseStatusException(HttpStatus.NOT_FOUND, "user does not exist in DB");
      }
      boolean isUpdated = false;
      // Username Update
       if (!user.getUsername().equals(modifiedUser.getUsername())) {
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