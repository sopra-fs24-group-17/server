package ch.uzh.ifi.hase.soprafs24.rest.dto;

import ch.uzh.ifi.hase.soprafs24.constant.GameMode;

import ch.uzh.ifi.hase.soprafs24.constant.GameState;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class GamePutDTO {

    private Long gameId;

    private GameState state;

}