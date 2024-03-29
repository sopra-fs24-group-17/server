package ch.uzh.ifi.hase.soprafs24.rest.dto;

import ch.uzh.ifi.hase.soprafs24.constant.ProfileVisibility;
import ch.uzh.ifi.hase.soprafs24.constant.TutorialFlag;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserFriendsGetDTO {

    private Long id;

    private Long userId;

    private Long friendId;

}