package ch.uzh.ifi.hase.soprafs24.rest.dto;
import ch.uzh.ifi.hase.soprafs24.constant.ProfileVisibility;

import ch.uzh.ifi.hase.soprafs24.constant.TutorialFlag;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserPutDTO {

    private String username;

    private String password;

    private String email;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date birthdate;

    private String countryoforigin;

    private ProfileVisibility profilevisibility;

    private TutorialFlag tutorialflag;

    private String avatar;

}
