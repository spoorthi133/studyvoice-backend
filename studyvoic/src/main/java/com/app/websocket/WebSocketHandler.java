package com.app.websocket;

import com.app.config.JwtUtil;
import com.app.model.User;
import com.app.repository.UserRepository;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.app.service.ParticipationService;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import com.app.model.PomodoroTimer;
import com.app.service.PomodoroService;
import com.app.service.ParticipationService;
import com.app.service.ChatService;
@Slf4j
@Component
public class WebSocketHandler extends TextWebSocketHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final ChatService chatService;
    private final ParticipationService participationService;
    private final PomodoroService pomodoroService;
    public WebSocketHandler(JwtUtil jwtUtil,
                            UserRepository userRepository,
                            ParticipationService participationService,
                            PomodoroService pomodoroService,
                            ChatService chatService) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.participationService = participationService;
        this.pomodoroService = pomodoroService;
        this.chatService = chatService;
    }


    // roomId → list of sessions in that room
    private final Map<Long, Set<WebSocketSession>> roomSessions
            = new ConcurrentHashMap<>();

    // roomId → queue of emails waiting to speak
    private final Map<Long, Queue<String>> raiseHandQueues
            = new ConcurrentHashMap<>();

    // roomId → current speaker email
    private final Map<Long, String> currentSpeakers
            = new ConcurrentHashMap<>();

    // sessionId → email
    private final Map<String, String> sessionUsers
            = new ConcurrentHashMap<>();

    // sessionId → roomId
    private final Map<String, Long> sessionRooms
            = new ConcurrentHashMap<>();

    // email → session (for P2P WebRTC routing)
    private final Map<String, WebSocketSession> emailToSession
            = new ConcurrentHashMap<>();

    // ─── Called when phone connects ───────────────────────────────────
    @Override
    public void afterConnectionEstablished(WebSocketSession session)
            throws Exception {
        log.info("New WebSocket connection: {}", session.getId());
    }

    // ─── Called when phone sends a message ───────────────────────────
    @Override
    protected void handleTextMessage(WebSocketSession session,
                                     TextMessage message) throws Exception {
        try {
            WebSocketMessage wsMessage = jsonMapper.readValue(
                    message.getPayload(), WebSocketMessage.class);

            String email = authenticateSession(session, wsMessage);
            if (email == null) {
                sendError(session, "Invalid or missing token");
                return;
            }

            switch (wsMessage.getType()) {
                case JOIN_ROOM    -> handleJoinRoom(session, wsMessage, email);
                case LEAVE_ROOM   -> handleLeaveRoom(session, wsMessage, email);
                case RAISE_HAND   -> handleRaiseHand(session, wsMessage, email);
                case LOWER_HAND   -> handleLowerHand(session, wsMessage, email);
                case NEXT_SPEAKER -> handleNextSpeaker(session, wsMessage, email);
                case NOTE_UPDATE  -> handleNoteUpdate(session, wsMessage, email);
                case TIMER_START  -> handleTimerStart(session, wsMessage, email);
                case TIMER_PAUSE  -> handleTimerPause(session, wsMessage, email);
                case TIMER_RESUME -> handleTimerResume(session, wsMessage, email);
                case BREAK_END    -> handleBreakEnd(session, wsMessage, email);
                case TIMER_SYNC   -> handleTimerSync(session, wsMessage, email);
                case CHAT_MESSAGE -> handleChatMessage(session, wsMessage, email);
                case MUTE_STATUS     -> handleMuteStatus(session, wsMessage, email);
                case WHITEBOARD_DRAW -> handleWhiteboardDraw(session, wsMessage, email);
                case WHITEBOARD_CLEAR -> handleWhiteboardClear(session, wsMessage, email);
                case REACTION        -> handleReaction(session, wsMessage, email);
                case WEBRTC_OFFER    -> handleWebRtcSignal(session, wsMessage, email);
                case WEBRTC_ANSWER   -> handleWebRtcSignal(session, wsMessage, email);
                case WEBRTC_ICE      -> handleWebRtcSignal(session, wsMessage, email);
                default -> sendError(session, "Unknown message type");
            }

        } catch (Exception e) {
            log.error("Error handling message: {}", e.getMessage());
            sendError(session, "Error processing message");
        }
    }

    // ─── Called when phone disconnects ───────────────────────────────
    @Override
    public void afterConnectionClosed(WebSocketSession session,
                                      CloseStatus status) {
        String email = sessionUsers.get(session.getId());
        Long roomId = sessionRooms.get(session.getId());

        if (email != null && roomId != null) {
            removeFromRoom(session, roomId, email);
        }

        if (email != null) emailToSession.remove(email);
        sessionUsers.remove(session.getId());
        sessionRooms.remove(session.getId());
        log.info("Connection closed: {}", session.getId());
    }

    // ─── JOIN ROOM ────────────────────────────────────────────────────
    private void handleJoinRoom(WebSocketSession session,
                                WebSocketMessage message,
                                String email) throws IOException {
        Long roomId = message.getRoomId();

        roomSessions.computeIfAbsent(roomId,
                        k -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                .add(session);

        sessionRooms.put(session.getId(), roomId);
        sessionUsers.put(session.getId(), email);
        emailToSession.put(email, session);
        raiseHandQueues.computeIfAbsent(roomId, k -> new LinkedList<>());

        String fullName = getFullName(email);

        broadcastToRoom(roomId, WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.JOIN_ROOM)
                .roomId(roomId)
                .senderEmail(email)
                .senderName(fullName)
                .content(fullName + " joined the room")
                .build());
        participationService.recordJoin(email, roomId);
        log.info("User {} joined room {}", email, roomId);
    }

    // ─── LEAVE ROOM ───────────────────────────────────────────────────
    private void handleLeaveRoom(WebSocketSession session,
                                 WebSocketMessage message,
                                 String email) throws IOException {
        Long roomId = message.getRoomId();
        removeFromRoom(session, roomId, email);
    }

    // ─── RAISE HAND ───────────────────────────────────────────────────
    private void handleRaiseHand(WebSocketSession session,
                                 WebSocketMessage message,
                                 String email) throws IOException {
        Long roomId = message.getRoomId();
        Queue<String> queue = raiseHandQueues.computeIfAbsent(
                roomId, k -> new LinkedList<>());

        boolean isNew = !queue.contains(email);
        if (isNew) queue.add(email);

        // Broadcast individual raise-hand event so clients can show a notification
        broadcastToRoom(roomId, WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.RAISE_HAND)
                .roomId(roomId)
                .senderEmail(email)
                .senderName(getFullName(email))
                .content(isNew ? "raised" : "already_raised")
                .build());

        // Also send the full queue so clients can update their list
        broadcastToRoom(roomId, WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.QUEUE_UPDATE)
                .roomId(roomId)
                .senderEmail(email)
                .senderName(getFullName(email))
                .content(buildQueueContent(queue))
                .build());

        log.info("User {} raised hand in room {}", email, roomId);
    }

    // ─── LOWER HAND ───────────────────────────────────────────────────
    private void handleLowerHand(WebSocketSession session,
                                 WebSocketMessage message,
                                 String email) throws IOException {
        Long roomId = message.getRoomId();
        Queue<String> queue = raiseHandQueues.get(roomId);

        if (queue != null) queue.remove(email);

        broadcastToRoom(roomId, WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.LOWER_HAND)
                .roomId(roomId)
                .senderEmail(email)
                .senderName(getFullName(email))
                .content("lowered")
                .build());

        broadcastToRoom(roomId, WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.QUEUE_UPDATE)
                .roomId(roomId)
                .senderEmail(email)
                .senderName(getFullName(email))
                .content(buildQueueContent(queue))
                .build());
    }

    // ─── NEXT SPEAKER ─────────────────────────────────────────────────
    private void handleNextSpeaker(WebSocketSession session,
                                   WebSocketMessage message,
                                   String email) throws IOException {
        Long roomId = message.getRoomId();
        Queue<String> queue = raiseHandQueues.get(roomId);

        // Capture previous speaker BEFORE updating the map
        String previousSpeaker = currentSpeakers.get(roomId);

        String nextSpeaker = (queue != null && !queue.isEmpty())
                ? queue.poll() : null;

        if (nextSpeaker != null) {
            currentSpeakers.put(roomId, nextSpeaker);
        } else {
            currentSpeakers.remove(roomId);
        }

        broadcastToRoom(roomId, WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.NEXT_SPEAKER)
                .roomId(roomId)
                .senderEmail(email)
                .senderName(nextSpeaker != null ? getFullName(nextSpeaker) : null)
                .content(nextSpeaker != null ? nextSpeaker : "none")
                .build());

        // Stop previous speaker's timer
        if (previousSpeaker != null) {
            participationService.recordSpeakingEnd(previousSpeaker, roomId);
        }
        // Start new speaker's timer
        if (nextSpeaker != null) {
            participationService.recordSpeakingStart(nextSpeaker, roomId);
        }
        log.info("Next speaker in room {}: {}", roomId, nextSpeaker);
    }

    // ─── NOTE UPDATE ──────────────────────────────────────────────────
    private void handleNoteUpdate(WebSocketSession session,
                                  WebSocketMessage message,
                                  String email) throws IOException {
        Long roomId = message.getRoomId();

        broadcastToRoom(roomId, WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.NOTE_UPDATE)
                .roomId(roomId)
                .senderEmail(email)
                .senderName(getFullName(email))
                .content(message.getContent())
                .build());
    }

    // ─── HELPERS ──────────────────────────────────────────────────────

    private void removeFromRoom(WebSocketSession session,
                                Long roomId, String email) {
        Set<WebSocketSession> sessions = roomSessions.get(roomId);
        if (sessions != null) sessions.remove(session);

        Queue<String> queue = raiseHandQueues.get(roomId);
        if (queue != null) queue.remove(email);

        String currentSpeaker = currentSpeakers.get(roomId);
        if (email.equals(currentSpeaker)) currentSpeakers.remove(roomId);

        try {
            broadcastToRoom(roomId, WebSocketMessage.builder()
                    .type(WebSocketMessage.MessageType.LEAVE_ROOM)
                    .roomId(roomId)
                    .senderEmail(email)
                    .senderName(getFullName(email))
                    .content(getFullName(email) + " left the room")
                    .build());
        } catch (IOException e) {
            log.error("Error broadcasting leave: {}", e.getMessage());
        }
        participationService.recordLeave(email, roomId);
        log.info("User {} left room {}", email, roomId);
    }

    private void broadcastToRoom(Long roomId,
                                 WebSocketMessage message) throws IOException {
        Set<WebSocketSession> sessions = roomSessions.get(roomId);
        if (sessions == null) return;

        String json = jsonMapper.writeValueAsString(message);
        for (WebSocketSession s : sessions) {
            if (s.isOpen()) {
                s.sendMessage(new TextMessage(json));
            }
        }
    }

    private void sendError(WebSocketSession session,
                           String errorMsg) throws IOException {
        String json = jsonMapper.writeValueAsString(
                WebSocketMessage.builder()
                        .type(WebSocketMessage.MessageType.ERROR)
                        .content(errorMsg)
                        .build());
        session.sendMessage(new TextMessage(json));
    }

    private String authenticateSession(WebSocketSession session,
                                       WebSocketMessage message) {
        try {
            String existingEmail = sessionUsers.get(session.getId());
            if (existingEmail != null) return existingEmail;

            // Try token field first, fall back to content
            String token = message.getToken() != null
                    ? message.getToken()
                    : message.getContent();
            if (token == null || token.isEmpty()) return null;

            String email = jwtUtil.extractEmail(token);
            if (email != null && jwtUtil.isTokenValid(token, email)) {
                sessionUsers.put(session.getId(), email);
                return email;
            }
        } catch (Exception e) {
            log.error("Auth error: {}", e.getMessage());
        }
        return null;
    }

    private String getFullName(String email) {
        return userRepository.findByEmail(email)
                .map(User::getFullName)
                .orElse(email);
    }

    private String buildQueueContent(Queue<String> queue) {
        if (queue == null || queue.isEmpty()) return "";
        return String.join(",", queue);
    }
    // ─── TIMER START ──────────────────────────────────────────────────
    private void handleTimerStart(WebSocketSession session,
                                  WebSocketMessage message,
                                  String email) throws IOException {
        Long roomId = message.getRoomId();
        PomodoroTimer timer = pomodoroService.startTimer(roomId);

        broadcastToRoom(roomId, WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.TIMER_START)
                .roomId(roomId)
                .senderEmail(email)
                .senderName(getFullName(email))
                .content(pomodoroService.formatTime(
                        timer.getSecondsRemaining()))
                .build());
    }

    // ─── TIMER PAUSE ──────────────────────────────────────────────────
    private void handleTimerPause(WebSocketSession session,
                                  WebSocketMessage message,
                                  String email) throws IOException {
        Long roomId = message.getRoomId();
        PomodoroTimer timer = pomodoroService.pauseTimer(roomId);

        broadcastToRoom(roomId, WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.TIMER_PAUSE)
                .roomId(roomId)
                .senderEmail(email)
                .content(pomodoroService.formatTime(
                        timer.getSecondsRemaining()))
                .build());
    }

    // ─── TIMER RESUME ─────────────────────────────────────────────────
    private void handleTimerResume(WebSocketSession session,
                                   WebSocketMessage message,
                                   String email) throws IOException {
        Long roomId = message.getRoomId();
        PomodoroTimer timer = pomodoroService.resumeTimer(roomId);

        broadcastToRoom(roomId, WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.TIMER_RESUME)
                .roomId(roomId)
                .senderEmail(email)
                .content(pomodoroService.formatTime(
                        timer.getSecondsRemaining()))
                .build());
    }

    // ─── BREAK END ────────────────────────────────────────────────────
    private void handleBreakEnd(WebSocketSession session,
                                WebSocketMessage message,
                                String email) throws IOException {
        Long roomId = message.getRoomId();
        PomodoroTimer timer = pomodoroService.endBreak(roomId);

        broadcastToRoom(roomId, WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.BREAK_END)
                .roomId(roomId)
                .senderEmail(email)
                .content(pomodoroService.formatTime(
                        timer.getSecondsRemaining()))
                .build());
    }

    // ─── TIMER SYNC ───────────────────────────────────────────────────
    private void handleTimerSync(WebSocketSession session,
                                 WebSocketMessage message,
                                 String email) throws IOException {
        Long roomId = message.getRoomId();
        PomodoroTimer timer = pomodoroService.getTimerStatus(roomId);

        // Send only to the requesting user
        String json = jsonMapper.writeValueAsString(
                WebSocketMessage.builder()
                        .type(WebSocketMessage.MessageType.TIMER_SYNC)
                        .roomId(roomId)
                        .content(timer.getState() + ":" +
                                pomodoroService.formatTime(
                                        timer.getSecondsRemaining()))
                        .build());
        session.sendMessage(new TextMessage(json));
    }

    // ─── CHAT MESSAGE ─────────────────────────────────────────────────
    private void handleChatMessage(WebSocketSession session,
                                   WebSocketMessage message,
                                   String email) throws IOException {
        Long roomId = message.getRoomId();

        // Save to database
        chatService.saveMessage(email, roomId, message.getContent());

        // Broadcast to everyone in room
        broadcastToRoom(roomId, WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.CHAT_MESSAGE)
                .roomId(roomId)
                .senderEmail(email)
                .senderName(getFullName(email))
                .content(message.getContent())
                .build());

        log.info("Chat message from {} in room {}", email, roomId);
    }

    // ─── MUTE STATUS ──────────────────────────────────────────────────
    private void handleMuteStatus(WebSocketSession session,
                                  WebSocketMessage message,
                                  String email) throws IOException {
        Long roomId = message.getRoomId();
        broadcastToRoom(roomId, WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.MUTE_STATUS)
                .roomId(roomId)
                .senderEmail(email)
                .senderName(getFullName(email))
                .content(message.getContent()) // "muted" or "unmuted"
                .build());
    }

    // ─── WHITEBOARD DRAW ──────────────────────────────────────────────
    private void handleWhiteboardDraw(WebSocketSession session,
                                      WebSocketMessage message,
                                      String email) throws IOException {
        Long roomId = message.getRoomId();
        broadcastToRoom(roomId, WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.WHITEBOARD_DRAW)
                .roomId(roomId)
                .senderEmail(email)
                .content(message.getContent()) // JSON path data
                .build());
    }

    // ─── WHITEBOARD CLEAR ─────────────────────────────────────────────
    private void handleWhiteboardClear(WebSocketSession session,
                                       WebSocketMessage message,
                                       String email) throws IOException {
        Long roomId = message.getRoomId();
        broadcastToRoom(roomId, WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.WHITEBOARD_CLEAR)
                .roomId(roomId)
                .senderEmail(email)
                .build());
    }

    // ─── REACTION ─────────────────────────────────────────────────────
    private void handleReaction(WebSocketSession session,
                                WebSocketMessage message,
                                String email) throws IOException {
        Long roomId = message.getRoomId();
        broadcastToRoom(roomId, WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.REACTION)
                .roomId(roomId)
                .senderEmail(email)
                .senderName(getFullName(email))
                .content(message.getContent())
                .build());
    }

    // ─── WEBRTC SIGNALING (P2P routing) ──────────────────────────────
    private void handleWebRtcSignal(WebSocketSession session,
                                    WebSocketMessage message,
                                    String email) throws IOException {
        String targetEmail = message.getTargetEmail();
        if (targetEmail == null) {
            sendError(session, "targetEmail required for WebRTC signaling");
            return;
        }

        WebSocketSession targetSession = emailToSession.get(targetEmail);
        if (targetSession == null || !targetSession.isOpen()) {
            log.warn("WebRTC target {} not found or disconnected", targetEmail);
            return;
        }

        String json = jsonMapper.writeValueAsString(
                WebSocketMessage.builder()
                        .type(message.getType())
                        .roomId(message.getRoomId())
                        .senderEmail(email)
                        .senderName(getFullName(email))
                        .content(message.getContent())
                        .build());
        targetSession.sendMessage(new TextMessage(json));
        log.info("WebRTC {} relayed from {} to {}", message.getType(), email, targetEmail);
    }
}