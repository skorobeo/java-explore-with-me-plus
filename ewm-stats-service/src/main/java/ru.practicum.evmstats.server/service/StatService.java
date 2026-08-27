package ru.practicum.evmstats.server.service;

import ru.practicum.ewmstats.dto.EndpointHit;
import ru.practicum.ewmstats.dto.ViewStats;

import java.time.LocalDateTime;
import java.util.List;

public interface StatService {

    void saveHit(EndpointHit endpointHitDto);

    List<ViewStats> getStats(LocalDateTime start, LocalDateTime end,
                             List<String> uris, Boolean unique);
}
