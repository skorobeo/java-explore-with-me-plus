package ru.practicum.ewm.compilation.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.ewm.compilation.model.Compilation;

import java.util.List;
import java.util.Optional;

public interface CompilationRepository extends JpaRepository<Compilation, Long> {
    @EntityGraph(attributePaths = {"events", "events.category", "events.initiator"})
    Optional<Compilation> findDetailedById(Long id);

    List<Compilation> findByPinned(Boolean pinned, Pageable pageable);

    List<Compilation> findAllBy(Pageable pageable);

    @EntityGraph(attributePaths = {"events", "events.category", "events.initiator"})
    List<Compilation> findDetailedByIdIn(List<Long> ids);

    boolean existsByTitle(String title);

    boolean existsByTitleAndIdNot(String title, Long id);
}
