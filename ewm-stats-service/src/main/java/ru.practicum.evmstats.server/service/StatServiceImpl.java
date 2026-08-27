package ru.practicum.evmstats.server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.evmstats.server.mapper.HitMapper;
import ru.practicum.evmstats.server.repository.StatRepository;
import ru.practicum.ewmstats.dto.EndpointHit;
import ru.practicum.ewmstats.dto.ViewStats;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatServiceImpl implements StatService{

    private final StatRepository statsRepository;

    @Override
    @Transactional
    public void saveHit(EndpointHit endpointHitDto) {
        statsRepository.save(HitMapper.toEntity(endpointHitDto));
    }

    @Override
    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        return unique ?
                statsRepository.getStatUnique(start, end, uris) : statsRepository.getStat(start, end, uris);
    }
}
