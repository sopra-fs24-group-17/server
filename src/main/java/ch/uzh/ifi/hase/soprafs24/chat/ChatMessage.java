package ch.uzh.ifi.hase.soprafs24.chat;

import ch.uzh.ifi.hase.soprafs24.constant.MessageType;
import lombok.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class ChatMessage {

    private String content;

    private String sender;

    private MessageType type;

    private String time;

    public ChatMessage() {
        this.time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
    }
}
