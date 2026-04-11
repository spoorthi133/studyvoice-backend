package com.app.repository;

import com.app.model.ParticipationRecord;
import com.app.model.StudyRoom;
import com.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipationRepository
        extends JpaRepository<ParticipationRecord, Long> {

    // Find active session for a user in a room
    Optional<ParticipationRecord> findByUserAndRoomAndActiveTrue(
            User user, StudyRoom room);

    // Get all records for a room
    List<ParticipationRecord> findByRoom(StudyRoom room);

    // Get all records for a user
    List<ParticipationRecord> findByUser(User user);

    // Get active sessions in a room
    List<ParticipationRecord> findByRoomAndActiveTrue(StudyRoom room);

    // Get total speaking seconds for a user in a room
    @Query("SELECT SUM(p.speakingSeconds) FROM ParticipationRecord p " +
            "WHERE p.user = :user AND p.room = :room")
    Long getTotalSpeakingSeconds(User user, StudyRoom room);

    // Get all records for a user ordered by most recent
    List<ParticipationRecord> findByUserOrderByJoinedAtDesc(User user);
}