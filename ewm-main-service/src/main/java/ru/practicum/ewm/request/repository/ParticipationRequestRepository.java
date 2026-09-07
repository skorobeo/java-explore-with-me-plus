package ru.practicum.ewm.request.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.ewm.request.model.ParticipationRequest;
import ru.practicum.ewm.request.model.ParticipationRequestStatus;

import java.util.List;
import java.util.Optional;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {

    boolean existsByRequesterIdAndEventId(Long requesterId, Long eventId);

    Optional<ParticipationRequest> findByIdAndRequesterId(Long id, Long requesterId);

    @EntityGraph(attributePaths = {"event", "requester"})
    List<ParticipationRequest> findAllByRequesterIdOrderByIdAsc(Long requesterId);

    @EntityGraph(attributePaths = {"event", "requester"})
    List<ParticipationRequest> findAllByEventIdOrderByIdAsc(Long eventId);

    long countByEventIdAndStatus(Long eventId, ParticipationRequestStatus status);

    List<ParticipationRequest> findAllByEventIdAndStatus(Long eventId, ParticipationRequestStatus status);
}