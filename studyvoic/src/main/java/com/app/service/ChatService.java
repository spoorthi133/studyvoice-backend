package com.app.service;

import com.app.model.ChatMessage;
import com.app.model.StudyRoom;
import com.app.model.User;
import com.app.repository.ChatMessageRepository;
import com.app.repository.StudyRoomRepository;
import com.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final StudyRoomRepository studyRoomRepository;

    // Save a new chat message
    @Transactional
    public ChatMessage saveMessage(String email,
                                   Long roomId,
                                   String content) {

        // Step 1: Validate content
        if (content == null || content.trim().isEmpty()) {
            throw new RuntimeException("Message cannot be empty");
        }

        if (content.length() > 1000) {
            throw new RuntimeException(
                    "Message too long (max 1000 characters)");
        }

        // Step 2: Find user
        User sender = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Step 3: Find room
        StudyRoom room = studyRoomRepository.findByIdAndActiveTrue(roomId)
                .orElseThrow(() ->
                        new RuntimeException("Room not found or inactive"));

        // Step 4: Build and save message
        ChatMessage message = ChatMessage.builder()
                .sender(sender)
                .room(room)
                .content(content.trim())
                .build();

        return chatMessageRepository.save(message);
    }

    // Get last 50 messages for a room (for new joiners)
    public List<ChatMessage> getRecentMessages(Long roomId) {
        StudyRoom room = studyRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        // Get last 50 messages and reverse so oldest is first
        List<ChatMessage> messages = chatMessageRepository
                .findTop50ByRoomOrderBySentAtDesc(room);

        Collections.reverse(messages);
        return messages;
    }

    // Get full chat history for a room
    public List<ChatMessage> getFullHistory(Long roomId) {
        StudyRoom room = studyRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        return chatMessageRepository.findByRoomOrderBySentAtAsc(room);
    }

    // Get message count for a room
    public long getMessageCount(Long roomId) {
        StudyRoom room = studyRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        return chatMessageRepository.countByRoom(room);
    }
}