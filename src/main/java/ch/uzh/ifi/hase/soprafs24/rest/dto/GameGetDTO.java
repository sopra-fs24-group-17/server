package ch.uzh.ifi.hase.soprafs24.rest.dto;

import ch.uzh.ifi.hase.soprafs24.constant.GameMode;

import ch.uzh.ifi.hase.soprafs24.constant.GameState;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class GameGetDTO {

    private Long gameId;

    private GameMode mode;

    private Integer maxPlayers;

    private String initiatingUserName;

    private GameState state;

    private String deckId;

}
