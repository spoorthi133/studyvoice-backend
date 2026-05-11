package com.app.dto;

import com.app.model.ParticipationRecord;
import java.time.format.DateTimeFormatter;

public class ParticipationHistoryDto {

    public Long id;
    public String roomName;
    public String roomTopic;
    public Long roomId;
    public String joinedAt;
    public String leftAt;
    public int speakCount;
    public long speakingSeconds;
    public boolean active;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static ParticipationHistoryDto from(ParticipationRecord r) {
        ParticipationHistoryDto dto = new ParticipationHistoryDto();
        dto.id              = r.getId();
        dto.roomId          = r.getRoom().getId();
        dto.roomName        = r.getRoom().getName();
        dto.roomTopic       = r.getRoom().getTopic();
        dto.joinedAt        = r.getJoinedAt() != null
                              ? r.getJoinedAt().format(FMT) : null;
        dto.leftAt          = r.getLeftAt() != null
                              ? r.getLeftAt().format(FMT) : null;
        dto.speakCount      = r.getSpeakCount();
        dto.speakingSeconds = r.getSpeakingSeconds();
        dto.active          = r.isActive();
        return dto;
    }
}
