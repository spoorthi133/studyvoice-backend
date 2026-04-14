package com.app.controller;

import com.app.model.StudyRoom;
import com.app.service.StudyRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
public class StudyRoomController {

    private final StudyRoomService studyRoomService;

    // Create a new room
    @PostMapping("/create")
    public ResponseEntity<StudyRoom> createRoom(
            @RequestParam String name,
            @RequestParam String topic,
            @RequestParam(defaultValue = "10") int maxParticipants,
            @AuthenticationPrincipal UserDetails userDetails) {

        StudyRoom room = studyRoomService.createRoom(
                name,
                topic,
                maxParticipants,
                userDetails.getUsername()
        );
        return ResponseEntity.ok(room);
    }

    // Join an existing room
    @PostMapping("/join/{roomId}")
    public ResponseEntity<StudyRoom> joinRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetails userDetails) {

        StudyRoom room = studyRoomService.joinRoom(
                roomId,
                userDetails.getUsername()
        );
        return ResponseEntity.ok(room);
    }

    // Leave a room
    @PostMapping("/leave/{roomId}")
    public ResponseEntity<String> leaveRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetails userDetails) {

        studyRoomService.leaveRoom(roomId, userDetails.getUsername());
        return ResponseEntity.ok("Left room successfully");
    }

    // Get all active rooms
    @GetMapping
    public ResponseEntity<List<StudyRoom>> getAllRooms() {
        return ResponseEntity.ok(studyRoomService.getAllActiveRooms());
    }

    // Get rooms by topic
    @GetMapping("/search")
    public ResponseEntity<List<StudyRoom>> searchRooms(
            @RequestParam String topic) {
        return ResponseEntity.ok(studyRoomService.getRoomsByTopic(topic));
    }

    // Get a single room
    @GetMapping("/{roomId}")
    public ResponseEntity<StudyRoom> getRoom(
            @PathVariable Long roomId) {
        return ResponseEntity.ok(studyRoomService.getRoom(roomId));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}