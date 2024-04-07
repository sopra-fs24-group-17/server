package ch.uzh.ifi.hase.soprafs24.rest.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Setter
@Getter
public class UserFriendsRequestGetDTO {

    private Long requestId;

    private String requestingUserUsername;

    private Date requestDate;

}
