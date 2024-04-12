package ch.uzh.ifi.hase.soprafs24.rest.mapper;

import ch.uzh.ifi.hase.soprafs24.constant.FriendRequestStatus;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserFriendsRequests;
import ch.uzh.ifi.hase.soprafs24.rest.dto.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * DTOMapperTest
 * Tests if the mapping between the internal and the external/API representation
 * works.
 */
public class DTOMapperTest {
  @Test
  public void testCreateUser_fromUserPostDTO_toUser_success() {
    // create UserPostDTO
    UserPostDTO userPostDTO = new UserPostDTO();
    userPostDTO.setUsername("username");

    // MAP -> Create user
    User user = UserDTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

    // check content
    assertEquals(userPostDTO.getUsername(), user.getUsername());
  }

  @Test
  public void testGetUser_fromUser_toUserGetDTO_success() {
    // create User
    User user = new User();
    user.setUsername("firstname@lastname");
    user.setStatus(UserStatus.OFFLINE);
    user.setToken("1");

    // MAP -> Create UserGetDTO
    UserGetDTO userGetDTO = UserDTOMapper.INSTANCE.convertEntityToUserGetDTO(user);

    // check content
    assertEquals(user.getId(), userGetDTO.getId());
    assertEquals(user.getUsername(), userGetDTO.getUsername());
    assertEquals(user.getStatus(), userGetDTO.getStatus());
  }

    @Test
    public void testUpdateUser_fromUserPutDTO_toUser_success() {
        UserPutDTO userPutDTO = new UserPutDTO();
        userPutDTO.setUsername("updateUsername");
        userPutDTO.setEmail("update@example.com");

        User user = UserDTOMapper.INSTANCE.convertUserPutDTOtoEntity(userPutDTO);

        assertEquals(userPutDTO.getUsername(), user.getUsername());
        assertEquals(userPutDTO.getEmail(), user.getEmail());
    }

    @Test
    public void testGetUserFriendsRequest_fromUserFriendsRequests_toUserFriendsRequestGetDTO_success() {
        UserFriendsRequests request = new UserFriendsRequests();
        UserFriendsRequestGetDTO requestGetDTO = UserDTOMapper.INSTANCE.convertEntityToUserFriendsRequestGetDTO(request);
        assertEquals(request.getId(), requestGetDTO.getRequestId());
    }

    @Test
    public void testConvertUserFriendsRequestPutDTO_toUserFriendsRequests_success() {
        UserFriendsRequestPutDTO putDTO = new UserFriendsRequestPutDTO();
        putDTO.setStatus(FriendRequestStatus.ACCEPTED);
        UserFriendsRequests request = UserDTOMapper.INSTANCE.convertUserFriendsRequestPostDTOToUserFriendsRequests(putDTO);
        assertEquals(putDTO.getStatus(), request.getStatus());
    }

}
