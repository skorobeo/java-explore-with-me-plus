package ru.practicum.ewm.compilation.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.compilation.model.Compilation;

import java.util.List;
import java.util.Optional;

public interface CompilationRepository extends JpaRepository<Compilation, Long> {

    @Query("""
            SELECT DISTINCT c FROM Compilation c
            LEFT JOIN FETCH c.events e
            LEFT JOIN FETCH e.category
            LEFT JOIN FETCH e.initiator
            WHERE c.id IN :ids
            """)
    List<Compilation> findDetailedByIdIn(@Param("ids") List<Long> ids);

    List<Compilation> findByPinned(Boolean pinned, Pageable pageable);

    List<Compilation> findAllBy(Pageable pageable);

    @Query("""
            SELECT DISTINCT c FROM Compilation c
            LEFT JOIN FETCH c.events e
            LEFT JOIN FETCH e.category
            LEFT JOIN FETCH e.initiator
            WHERE c.id = :id
            """)
    Optional<Compilation> findDetailedById(@Param("id") Long id);

    boolean existsByTitle(String title);

    boolean existsByTitleAndIdNot(String title, Long id);
}
