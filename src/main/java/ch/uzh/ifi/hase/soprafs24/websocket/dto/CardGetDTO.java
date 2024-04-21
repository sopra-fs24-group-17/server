package ch.uzh.ifi.hase.soprafs24.websocket.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CardGetDTO {
    private String code;
    private String internalCode;
    private String image;
}
