package ch.uzh.ifi.hase.soprafs24.rest.mapper;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserFriendsRequests;
import ch.uzh.ifi.hase.soprafs24.rest.dto.*;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

/**
 * DTOMapper
 * This class is responsible for generating classes that will automatically
 * transform/map the internal representation
 * of an entity (e.g., the User) to the external/API representation (e.g.,
 * UserGetDTO for getting, UserPostDTO for creating)
 * and vice versa.
 * Additional mappers can be defined for new entities.
 * Always created one mapper for getting information (GET) and one mapper for
 * creating information (POST).
 */
@Mapper
public interface UserDTOMapper {

  UserDTOMapper INSTANCE = Mappers.getMapper(UserDTOMapper.class);

  @Mapping(source = "username", target = "username")
  @Mapping(source = "password", target = "password")
  @Mapping(source = "email", target = "email")
  User convertUserPostDTOtoEntity(UserPostDTO userPostDTO);

  @Mapping(source = "id", target = "id")
  @Mapping(source = "email", target = "email")
  @Mapping(source = "username", target = "username")
  @Mapping(source = "status", target = "status")
  @Mapping(source = "birthdate", target = "birthdate")
  @Mapping(source = "profilevisibility", target = "profilevisibility")
  @Mapping(source = "countryoforigin", target = "countryoforigin")
  @Mapping(source = "otp", target = "otp")
  @Mapping(source = "tutorialflag", target = "tutorialflag")
  @Mapping(source = "avatar", target = "avatar")
  @Mapping(target = "token", ignore = true) //Will be sent in the header

  //User Statistics
  @Mapping(source = "userStats.gamesPlayed", target = "gamesplayed")
  @Mapping(source = "userStats.gamesWon", target = "gameswon")
  @Mapping(source = "userStats.winLossRatio", target = "winlossratio")
  @Mapping(source = "userStats.totalFriends", target = "totalfriends")
  @Mapping(source = "userStats.achievementsUnlocked", target = "achievementsunlocked")
  @Mapping(source = "userStats.lastPlayed", target = "lastplayed")
  UserGetDTO convertEntityToUserGetDTO(User user);

  @Mapping(source = "username", target = "username")
  @Mapping(source = "password", target = "password")
  @Mapping(source = "email", target = "email")
  @Mapping(source = "birthdate", target = "birthdate")
  @Mapping(source = "countryoforigin", target = "countryoforigin")
  @Mapping(source = "profilevisibility", target = "profilevisibility")
  @Mapping(source = "tutorialflag", target = "tutorialflag")
  @Mapping(source = "avatar", target = "avatar")
  User convertUserPutDTOtoEntity(UserPutDTO userPutDTO);

  @Mapping(target = "userid", ignore = true)
  @Mapping(source = "username", target = "username")
  @Mapping(source = "userStats.gamesPlayed", target = "gamesplayed")
  @Mapping(source = "userStats.gamesWon", target = "gameswon")
  @Mapping(source = "userStats.winLossRatio", target = "winlossratio")
  @Mapping(source = "userStats.totalFriends", target = "totalfriends")
  @Mapping(source = "userStats.achievementsUnlocked", target = "achievementsunlocked")
  @Mapping(source = "userStats.lastPlayed", target = "lastplayed")
  UserStatsGetDTO convertEntityToUserStatsGetDTO(User user);

  @Mapping(source = "id", target = "id")
  @Mapping(source = "email", target = "email")
  @Mapping(source = "username", target = "username")
  @Mapping(source = "status", target = "status")
  @Mapping(source = "birthdate", target = "birthdate")
  @Mapping(source = "countryoforigin", target = "countryoforigin")
  @Mapping(source = "avatar", target = "avatar")
  @Mapping(target = "otp", ignore = true)
  @Mapping(target = "tutorialflag", ignore = true)
  @Mapping(source = "profilevisibility", target = "profilevisibility")
  @Mapping(target = "token", ignore = true) //Will be sent in the header

  //User Statistics
  @Mapping(source = "userStats.gamesPlayed", target = "gamesplayed")
  @Mapping(source = "userStats.gamesWon", target = "gameswon")
  @Mapping(source = "userStats.winLossRatio", target = "winlossratio")
  @Mapping(source = "userStats.totalFriends", target = "totalfriends")
  @Mapping(source = "userStats.achievementsUnlocked", target = "achievementsunlocked")
  @Mapping(source = "userStats.lastPlayed", target = "lastplayed")
  UserGetDTO convertEntityToProfileUserGetDTO(User user);

  //Friendship Requests
  @Mapping(source = "id", target = "requestId")
  @Mapping(source = "requestingUser.username", target = "requestingUserUsername")
  @Mapping(source = "requestdate", target = "requestDate")
  UserFriendsRequestGetDTO convertEntityToUserFriendsRequestGetDTO(UserFriendsRequests userFriendsRequests);

  @Mapping(source = "status", target = "status")
  UserFriendsRequests convertUserFriendsRequestPostDTOToUserFriendsRequests(UserFriendsRequestPutDTO userFriendsRequestPutDTO);
}
