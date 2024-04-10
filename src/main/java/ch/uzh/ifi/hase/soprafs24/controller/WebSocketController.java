package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.rest.dto.GameGetDTO;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {

    private final UserService userService;

    private final Logger log = LoggerFactory.getLogger(WebSocketController.class);

    private final WebSocketService webSocketService;

    public WebSocketController(WebSocketService webSocketService, UserService userService) {
        this.webSocketService = webSocketService;
        this.userService = userService;
    }

    public void joinedUser(long joinedUserId, GameGetDTO gameGetDTO) {
        this.webSocketService.sendMessageToClients("/game/joiners/" + joinedUserId, gameGetDTO);
    }

    public void leftUser(long leftUserId, GameGetDTO gameGetDTO) {
        this.webSocketService.sendMessageToClients("/game/leavers/" + leftUserId, gameGetDTO);
    }
}
