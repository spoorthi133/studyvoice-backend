package com.app.service;

import com.app.model.ParticipationRecord;
import com.app.model.StudyRoom;
import com.app.model.User;
import com.app.repository.ParticipationRepository;
import com.app.repository.StudyRoomRepository;
import com.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ParticipationService {

    private final ParticipationRepository participationRepository;
    private final UserRepository userRepository;
    private final StudyRoomRepository studyRoomRepository;

    // Called when user joins a room
    @Transactional
    public void recordJoin(String email, Long roomId) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        StudyRoom room = studyRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        // Check if already has active session
        boolean alreadyActive = participationRepository
                .findByUserAndRoomAndActiveTrue(user, room)
                .isPresent();

        // Only create new record if not already active
        if (!alreadyActive) {
            ParticipationRecord record = ParticipationRecord.builder()
                    .user(user)
                    .room(room)
                    .build();
            participationRepository.save(record);
        }
    }

    // Called when user leaves a room
    @Transactional
    public void recordLeave(String email, Long roomId) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        StudyRoom room = studyRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        // Find their active session
        participationRepository
                .findByUserAndRoomAndActiveTrue(user, room)
                .ifPresent(record -> {

                    // Stop speaking timer if they were speaking
                    if (record.getSpeakingStartedAt() != null) {
                        long seconds = ChronoUnit.SECONDS.between(
                                record.getSpeakingStartedAt(),
                                LocalDateTime.now());
                        record.setSpeakingSeconds(
                                record.getSpeakingSeconds() + seconds);
                        record.setSpeakingStartedAt(null);
                    }

                    // Mark session as ended
                    record.setLeftAt(LocalDateTime.now());
                    record.setActive(false);
                    participationRepository.save(record);
                });
    }

    // Called when user becomes the speaker
    @Transactional
    public void recordSpeakingStart(String email, Long roomId) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        StudyRoom room = studyRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        participationRepository
                .findByUserAndRoomAndActiveTrue(user, room)
                .ifPresent(record -> {
                    // Start the speaking timer
                    record.setSpeakingStartedAt(LocalDateTime.now());
                    // Increment speak count
                    record.setSpeakCount(record.getSpeakCount() + 1);
                    participationRepository.save(record);
                });
    }

    // Called when user stops speaking
    @Transactional
    public void recordSpeakingEnd(String email, Long roomId) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        StudyRoom room = studyRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        participationRepository
                .findByUserAndRoomAndActiveTrue(user, room)
                .ifPresent(record -> {
                    if (record.getSpeakingStartedAt() != null) {
                        // Calculate how long they spoke
                        long seconds = ChronoUnit.SECONDS.between(
                                record.getSpeakingStartedAt(),
                                LocalDateTime.now());
                        // Add to total
                        record.setSpeakingSeconds(
                                record.getSpeakingSeconds() + seconds);
                        // Clear timer
                        record.setSpeakingStartedAt(null);
                        participationRepository.save(record);
                    }
                });
    }

    // Get all participation records for a room
    public List<ParticipationRecord> getRoomParticipation(Long roomId) {
        StudyRoom room = studyRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));
        return participationRepository.findByRoom(room);
    }

    // Get all participation records for a user
    public List<ParticipationRecord> getUserHistory(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return participationRepository.findByUserOrderByJoinedAtDesc(user);
    }

    // Get currently active participants in a room
    public List<ParticipationRecord> getActiveParticipants(Long roomId) {
        StudyRoom room = studyRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));
        return participationRepository.findByRoomAndActiveTrue(room);
    }
}