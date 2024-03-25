package ch.uzh.ifi.hase.soprafs24.rest.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Setter
@Getter
public class UserStatsGetDTO {

    private Long userid;

    private String username;

    private Integer gamesplayed;

    private Integer gameswon;

    private Double winlossratio;

    private Integer totalfriends;

    private Integer achievementsunlocked;

    private Date lastplayed;
}
