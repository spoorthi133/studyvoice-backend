package com.app.repository;

import com.app.model.StudyRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudyRoomRepository extends JpaRepository<StudyRoom, Long> {

    // Find all active rooms
    List<StudyRoom> findByActiveTrue();

    // Find active rooms by topic
    List<StudyRoom> findByTopicContainingIgnoreCaseAndActiveTrue(String topic);

    // Find a specific active room by id
    Optional<StudyRoom> findByIdAndActiveTrue(Long id);
}