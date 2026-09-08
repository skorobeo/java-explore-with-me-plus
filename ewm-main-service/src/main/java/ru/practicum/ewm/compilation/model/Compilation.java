package ru.practicum.ewm.compilation.model;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.ewm.event.model.Event;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@Entity
@Table(name = "compilations")
@AllArgsConstructor
@NoArgsConstructor
public class Compilation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 50)
    private String title;

    @Builder.Default
    @Column(name = "pinned", nullable = false)
    private Boolean pinned = false;

    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "compilation_events",
            joinColumns = @JoinColumn(name = "compilations_id"),
            inverseJoinColumns = @JoinColumn(name = "event_id")
    )

    private List<Event> events = new ArrayList<>();
}