package ch.uzh.ifi.hase.soprafs24.rest.dto;
import ch.uzh.ifi.hase.soprafs24.constant.GameMode;
import ch.uzh.ifi.hase.soprafs24.entity.User;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class GamePostDTO {

    private GameMode mode;

    private Integer maxPlayers;
}
