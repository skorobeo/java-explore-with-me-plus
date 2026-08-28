package ru.practicum.ewmstats.server.mapper;

import ru.practicum.ewmstats.server.model.Hit;
import ru.practicum.ewmstats.dto.EndpointHit;

public class HitMapper {

    public static Hit toEntity(EndpointHit endpointHitDto) {
        return Hit.builder()
                .id(endpointHitDto.getId())
                .app(endpointHitDto.getApp())
                .uri(endpointHitDto.getUri())
                .ip(endpointHitDto.getIp())
                .timestamp(endpointHitDto.getTimestamp())
                .build();
    }

    public static EndpointHit toDto(Hit endpointHit) {
        return EndpointHit.builder()
                .id(endpointHit.getId())
                .app(endpointHit.getApp())
                .uri(endpointHit.getUri())
                .ip(endpointHit.getIp())
                .timestamp(endpointHit.getTimestamp())
                .build();
    }
}
