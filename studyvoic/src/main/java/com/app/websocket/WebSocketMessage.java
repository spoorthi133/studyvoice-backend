package com.app.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {

    // Type of event
    private MessageType type;

    // Which room this message belongs to
    private Long roomId;

    // Who sent this message
    private String senderEmail;

    // Sender's display name
    private String senderName;

    // Optional extra content (e.g. note text)
    private String content;
    private String token;
    // All possible message types
    public enum MessageType {
        JOIN_ROOM,
        LEAVE_ROOM,
        RAISE_HAND,
        LOWER_HAND,
        NEXT_SPEAKER,
        NOTE_UPDATE,
        QUEUE_UPDATE,
        TIMER_START,
        TIMER_PAUSE,
        TIMER_RESUME,
        TIMER_END,
        BREAK_START,
        BREAK_END,
        TIMER_SYNC,
        CHAT_MESSAGE,
        ERROR
    }
}