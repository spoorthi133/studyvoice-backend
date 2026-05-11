package com.app.controller;

import com.app.dto.ParticipationHistoryDto;
import com.app.model.ParticipationRecord;
import com.app.service.ParticipationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/participation")
@RequiredArgsConstructor
public class ParticipationController {

    private final ParticipationService participationService;

    @GetMapping("/room/{roomId}")
    public ResponseEntity<List<ParticipationHistoryDto>> getRoomParticipation(
            @PathVariable Long roomId) {
        return ResponseEntity.ok(
                participationService.getRoomParticipation(roomId).stream()
                        .map(ParticipationHistoryDto::from)
                        .collect(Collectors.toList()));
    }

    @GetMapping("/room/{roomId}/active")
    public ResponseEntity<List<ParticipationHistoryDto>> getActiveParticipants(
            @PathVariable Long roomId) {
        return ResponseEntity.ok(
                participationService.getActiveParticipants(roomId).stream()
                        .map(ParticipationHistoryDto::from)
                        .collect(Collectors.toList()));
    }

    @GetMapping("/user/me")
    public ResponseEntity<List<ParticipationHistoryDto>> getMyHistory(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                participationService.getUserHistory(userDetails.getUsername()).stream()
                        .map(ParticipationHistoryDto::from)
                        .collect(Collectors.toList()));
    }
}