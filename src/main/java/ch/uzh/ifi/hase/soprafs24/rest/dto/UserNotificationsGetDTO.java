package ch.uzh.ifi.hase.soprafs24.rest.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Setter
@Getter
public class UserNotificationsGetDTO {
    private long notificationId;

    private String message;

    private Date timestamp;
}
