package ch.uzh.ifi.hase.soprafs24.rest.dto;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class FriendsGetDTO {

    private String friendName;

    private String friendAvatar;

    private UserStatus status;

}
