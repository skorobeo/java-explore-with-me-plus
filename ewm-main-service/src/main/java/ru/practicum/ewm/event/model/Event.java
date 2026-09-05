package ru.practicum.ewm.event.model;

import jakarta.persistence.*;
import lombok.Data;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.location.model.Location;
import ru.practicum.ewm.user.model.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@Data
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "annotation", nullable = false)
    private String annotation;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @Column(name = "created_on", nullable = false)
    private LocalDateTime createdOn;

    @Column(name = "published_on")
    private LocalDateTime publishedOn;

    @Column(name ="paid", nullable = false)
    private Boolean paid;

    @Column(name = "participant_limit", nullable = false)
    private Integer participantLimit;

    @Column(name = "request_moderation", nullable = false)
    private Boolean requestModeration;

    @Column(name = "state", nullable = false)
    @Enumerated(EnumType.STRING)
    private State state;

    @Embedded
    private Location location;

    @JoinColumn(name = "category_id")
    @ManyToOne
    private Category category;

    @JoinColumn(name = "initiator_id")
    @ManyToOne
    private User initiator;
}
