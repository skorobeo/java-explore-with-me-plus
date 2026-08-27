package ru.practicum.evmstats.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.evmstats.server.model.Hit;
import ru.practicum.ewmstats.dto.ViewStats;

import java.time.LocalDateTime;
import java.util.List;

public interface StatRepository extends JpaRepository<Hit, Long> {

    @Query("""
       select new ru.practicum.ewmstats.dto.ViewStats(h.app, h.uri, COUNT(distinct h.ip))
       from Hit h 
       where h.timestamp between :start and :end
       and (:uris is null or h.uri in :uris)
       group by h.app, h.uri
       order by COUNT(distinct h.ip) desc
       """)
    List<ViewStats> getStatUnique(@Param("start") LocalDateTime startDate,
                                     @Param("end") LocalDateTime endDate,
                                     @Param("uris") List<String> uris);

    @Query("""
       select new ru.practicum.ewmstats.dto.ViewStats(h.app, h.uri, COUNT(h.ip))
       from Hit h 
       where h.timestamp between :start and :end
       and (:uris is null or h.uri in :uris)
       group by h.app, h.uri
       order by COUNT(h.ip) desc
       """)
    List<ViewStats> getStat(@Param("start") LocalDateTime startDate,
                               @Param("end") LocalDateTime endDate,
                               @Param("uris") List<String> uris);
}