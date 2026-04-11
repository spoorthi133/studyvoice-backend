package com.app.repository;

import com.app.model.ChatMessage;
import com.app.model.StudyRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository
        extends JpaRepository<ChatMessage, Long> {

    // Get all messages in a room ordered oldest first
    List<ChatMessage> findByRoomOrderBySentAtAsc(StudyRoom room);

    // Get last 50 messages in a room
    List<ChatMessage> findTop50ByRoomOrderBySentAtDesc(StudyRoom room);

    // Count messages in a room
    long countByRoom(StudyRoom room);
}