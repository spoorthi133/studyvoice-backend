package com.app.service;

import com.app.model.StudyRoom;
import com.app.model.User;
import com.app.repository.StudyRoomRepository;
import com.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.HashSet;

@Service
@RequiredArgsConstructor
public class StudyRoomService {

    private final StudyRoomRepository studyRoomRepository;
    private final UserRepository userRepository;

    // Create a new study room
    @Transactional
    public StudyRoom createRoom(String name, String topic,
                                int maxParticipants, String creatorEmail) {

        // Step 1: Find the user creating the room
        User creator = userRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Step 2: Build the room
        StudyRoom room = StudyRoom.builder()
                .name(name)
                .topic(topic)
                .maxParticipants(maxParticipants)
                .createdBy(creator)
                .participants(new HashSet<>())
                .build();

        // Step 3: Add creator as first participant
        room.getParticipants().add(creator);

        // Step 4: Save and return
        return studyRoomRepository.save(room);
    }

    // Join an existing room
    @Transactional
    public StudyRoom joinRoom(Long roomId, String userEmail) {

        // Step 1: Find the room
        StudyRoom room = studyRoomRepository.findByIdAndActiveTrue(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found or inactive"));

        // Step 2: Find the user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Step 3: Check if room is full
        if (room.getParticipants().size() >= room.getMaxParticipants()) {
            throw new RuntimeException("Room is full");
        }

        // Step 4: Add user to room
        room.getParticipants().add(user);

        // Step 5: Save and return
        return studyRoomRepository.save(room);
    }

    // Leave a room
    @Transactional
    public void leaveRoom(Long roomId, String userEmail) {

        // Step 1: Find the room
        StudyRoom room = studyRoomRepository.findByIdAndActiveTrue(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        // Step 2: Find the user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Step 3: Remove user from participants
        room.getParticipants().remove(user);

        // Step 4: If creator leaves and no one is left, close the room
        if (room.getParticipants().isEmpty()) {
            room.setActive(false);
        }

        // Step 5: Save
        studyRoomRepository.save(room);
    }

    // Get all active rooms
    public List<StudyRoom> getAllActiveRooms() {
        return studyRoomRepository.findByActiveTrue();
    }

    // Get rooms by topic
    public List<StudyRoom> getRoomsByTopic(String topic) {
        return studyRoomRepository.findByTopicContainingIgnoreCaseAndActiveTrue(topic);
    }

    // Get a single room
    public StudyRoom getRoom(Long roomId) {
        return studyRoomRepository.findByIdAndActiveTrue(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));
    }
}