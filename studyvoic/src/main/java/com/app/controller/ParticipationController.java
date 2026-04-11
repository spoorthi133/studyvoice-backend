package com.app.controller;

import com.app.model.ParticipationRecord;
import com.app.service.ParticipationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/participation")
@RequiredArgsConstructor
public class ParticipationController {

    private final ParticipationService participationService;

    // Get all participation records for a room
    @GetMapping("/room/{roomId}")
    public ResponseEntity<List<ParticipationRecord>> getRoomParticipation(
            @PathVariable Long roomId) {

        return ResponseEntity.ok(
                participationService.getRoomParticipation(roomId));
    }

    // Get currently active participants in a room
    @GetMapping("/room/{roomId}/active")
    public ResponseEntity<List<ParticipationRecord>> getActiveParticipants(
            @PathVariable Long roomId) {

        return ResponseEntity.ok(
                participationService.getActiveParticipants(roomId));
    }

    // Get logged in user's study history
    @GetMapping("/user/me")
    public ResponseEntity<List<ParticipationRecord>> getMyHistory(
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(
                participationService.getUserHistory(
                        userDetails.getUsername()));
    }
}