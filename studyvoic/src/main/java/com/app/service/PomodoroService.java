package com.app.service;

import com.app.model.PomodoroTimer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class PomodoroService {

    // roomId → timer for that room
    private final Map<Long, PomodoroTimer> roomTimers
            = new ConcurrentHashMap<>();

    // Get or create timer for a room
    public PomodoroTimer getTimer(Long roomId) {
        return roomTimers.computeIfAbsent(
                roomId, PomodoroTimer::createForRoom);
    }

    // Start the focus timer
    public PomodoroTimer startTimer(Long roomId) {
        PomodoroTimer timer = getTimer(roomId);

        // Only start if IDLE or FINISHED
        if (timer.getState() == PomodoroTimer.TimerState.IDLE ||
                timer.getState() == PomodoroTimer.TimerState.FINISHED) {

            timer.setState(PomodoroTimer.TimerState.FOCUS);
            timer.setSecondsRemaining(PomodoroTimer.FOCUS_DURATION);
            timer.setStartedAt(LocalDateTime.now());
            log.info("Timer started for room {}", roomId);
        }

        return timer;
    }

    // Pause the timer
    public PomodoroTimer pauseTimer(Long roomId) {
        PomodoroTimer timer = getTimer(roomId);

        // Only pause if currently running
        if (timer.getState() == PomodoroTimer.TimerState.FOCUS ||
                timer.getState() == PomodoroTimer.TimerState.BREAK) {

            // Save remaining seconds before pausing
            long remaining = timer.calculateSecondsRemaining();
            timer.setSecondsRemaining(remaining);
            timer.setState(PomodoroTimer.TimerState.PAUSED);
            timer.setStartedAt(null);
            log.info("Timer paused for room {} with {}s remaining",
                    roomId, remaining);
        }

        return timer;
    }

    // Resume a paused timer
    public PomodoroTimer resumeTimer(Long roomId) {
        PomodoroTimer timer = getTimer(roomId);

        // Only resume if paused
        if (timer.getState() == PomodoroTimer.TimerState.PAUSED) {
            timer.setState(PomodoroTimer.TimerState.FOCUS);
            timer.setStartedAt(LocalDateTime.now());
            log.info("Timer resumed for room {}", roomId);
        }

        return timer;
    }

    // Start break timer
    public PomodoroTimer startBreak(Long roomId) {
        PomodoroTimer timer = getTimer(roomId);

        timer.setState(PomodoroTimer.TimerState.BREAK);
        timer.setSecondsRemaining(PomodoroTimer.BREAK_DURATION);
        timer.setStartedAt(LocalDateTime.now());
        timer.setCyclesCompleted(timer.getCyclesCompleted() + 1);
        log.info("Break started for room {} (cycle {})",
                roomId, timer.getCyclesCompleted());

        return timer;
    }

    // End break — go back to focus
    public PomodoroTimer endBreak(Long roomId) {
        PomodoroTimer timer = getTimer(roomId);

        timer.setState(PomodoroTimer.TimerState.IDLE);
        timer.setSecondsRemaining(PomodoroTimer.FOCUS_DURATION);
        timer.setStartedAt(null);
        log.info("Break ended for room {}", roomId);

        return timer;
    }

    // Reset timer completely
    public PomodoroTimer resetTimer(Long roomId) {
        PomodoroTimer timer = PomodoroTimer.createForRoom(roomId);
        roomTimers.put(roomId, timer);
        log.info("Timer reset for room {}", roomId);
        return timer;
    }

    // Get current timer status with live remaining seconds
    public PomodoroTimer getTimerStatus(Long roomId) {
        PomodoroTimer timer = getTimer(roomId);

        // Update remaining seconds to live value
        long remaining = timer.calculateSecondsRemaining();
        timer.setSecondsRemaining(remaining);

        // Auto transition if expired
        if (timer.isExpired()) {
            if (timer.getState() == PomodoroTimer.TimerState.FOCUS) {
                // Focus done → start break automatically
                return startBreak(roomId);
            } else if (timer.getState() == PomodoroTimer.TimerState.BREAK) {
                // Break done → go back to idle
                return endBreak(roomId);
            }
        }

        return timer;
    }

    // Format seconds as MM:SS string
    public String formatTime(long seconds) {
        long minutes = seconds / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }
}