package ch.uzh.ifi.hase.soprafs24.rest.dto;

import ch.uzh.ifi.hase.soprafs24.constant.ProfileVisibility;
import ch.uzh.ifi.hase.soprafs24.constant.TutorialFlag;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserGetDTO {

  private Long id;

  private String username;

  private UserStatus status;

  private String email;

  private Boolean otp;

  private Date birthdate;

  private Date creationdate;

  private String token;

  private ProfileVisibility profilevisibility;

  private String countryoforigin;

  private Integer unreadnotifications;

  private String avatar;

  private TutorialFlag tutorialflag;

  private Integer gamesplayed;

  private Integer gameswon;

  private Double winlossratio;

  private Integer totalfriends;

  private Integer achievementsunlocked;

  private Date lastplayed;

}
