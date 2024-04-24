package ch.uzh.ifi.hase.soprafs24.websocket.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class CardMoveRequest {
    private Long gameId;
    private Long userId;
    private List<String> cardIds;
    private Long targetUserId;
}
