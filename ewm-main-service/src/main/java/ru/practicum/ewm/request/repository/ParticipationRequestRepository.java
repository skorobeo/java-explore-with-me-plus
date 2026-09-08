package ru.practicum.ewm.request.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("""
        select r.event.id, count(r)
        from ParticipationRequest r
        where r.event.id in :eventIds
        and r.status = 'CONFIRMED'
        group by r.event.id
        """)
    List<Object[]> countConfirmedByEventIds(@Param("eventIds") List<Long> eventIds);
}