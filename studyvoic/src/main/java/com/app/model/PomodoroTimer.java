package com.app.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PomodoroTimer {

    // Which room this timer belongs to
    private Long roomId;

    // Current state of the timer
    private TimerState state;

    // How many seconds are left
    private long secondsRemaining;

    // When the timer was last started/resumed
    private LocalDateTime startedAt;

    // How many pomodoro cycles completed
    private int cyclesCompleted;

    // Standard durations
    public static final long FOCUS_DURATION  = 25 * 60; // 25 minutes
    public static final long BREAK_DURATION  = 5  * 60; // 5 minutes

    // All possible timer states
    public enum TimerState {
        IDLE,       // not started yet
        FOCUS,      // 25 min focus running
        PAUSED,     // timer paused
        BREAK,      // 5 min break running
        FINISHED    // session completed
    }

    // Create a fresh timer for a room
    public static PomodoroTimer createForRoom(Long roomId) {
        PomodoroTimer timer = new PomodoroTimer();
        timer.setRoomId(roomId);
        timer.setState(TimerState.IDLE);
        timer.setSecondsRemaining(FOCUS_DURATION);
        timer.setCyclesCompleted(0);
        return timer;
    }

    // Calculate actual seconds remaining based on time passed
    public long calculateSecondsRemaining() {
        if (state != TimerState.FOCUS && state != TimerState.BREAK) {
            return secondsRemaining;
        }

        if (startedAt == null) return secondsRemaining;

        long secondsPassed = java.time.temporal.ChronoUnit.SECONDS
                .between(startedAt, LocalDateTime.now());

        long remaining = secondsRemaining - secondsPassed;
        return Math.max(0, remaining);
    }

    // Check if timer has expired
    public boolean isExpired() {
        return calculateSecondsRemaining() <= 0;
    }
}