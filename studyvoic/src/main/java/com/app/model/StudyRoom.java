package com.app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "study_rooms")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class StudyRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String topic;

    // The user who created the room
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    // Max number of people allowed in the room
    @Column(nullable = false)
    private int maxParticipants;

    // Is the room currently active?
    @Column(nullable = false)
    private boolean active;

    // Who is currently speaking
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_speaker_id")
    private User currentSpeaker;

    // All users currently in the room
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "room_participants",
            joinColumns = @JoinColumn(name = "room_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> participants = new HashSet<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        active = true;
    }
}