package com.app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "participation_records")
public class ParticipationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Who participated
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Which room
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private StudyRoom room;

    // When they joined
    @Column(nullable = false)
    private LocalDateTime joinedAt;

    // When they left (null if still in room)
    @Column
    private LocalDateTime leftAt;

    // How many times they spoke
    @Column(nullable = false)
    private int speakCount;

    // Total seconds they spoke
    @Column(nullable = false)
    private long speakingSeconds;

    // When they started speaking (null if not speaking)
    @Column
    private LocalDateTime speakingStartedAt;

    // Is this session still active?
    @Column(nullable = false)
    private boolean active;

    @PrePersist
    protected void onCreate() {
        joinedAt = LocalDateTime.now();
        active = true;
        speakCount = 0;
        speakingSeconds = 0;
    }
}