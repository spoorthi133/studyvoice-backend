package com.app.controller;

import com.app.model.ChatMessage;
import com.app.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // Get last 50 messages for a room
    @GetMapping("/room/{roomId}")
    public ResponseEntity<List<ChatMessage>> getRecentMessages(
            @PathVariable Long roomId) {

        return ResponseEntity.ok(
                chatService.getRecentMessages(roomId));
    }

    // Get full chat history
    @GetMapping("/room/{roomId}/history")
    public ResponseEntity<List<ChatMessage>> getFullHistory(
            @PathVariable Long roomId) {

        return ResponseEntity.ok(
                chatService.getFullHistory(roomId));
    }

    // Get message count
    @GetMapping("/room/{roomId}/count")
    public ResponseEntity<Long> getMessageCount(
            @PathVariable Long roomId) {

        return ResponseEntity.ok(
                chatService.getMessageCount(roomId));
    }
}