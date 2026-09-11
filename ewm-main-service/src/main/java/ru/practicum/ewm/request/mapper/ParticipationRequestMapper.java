package ru.practicum.ewm.request.mapper;

import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.model.ParticipationRequest;
import ru.practicum.ewm.request.model.ParticipationRequestStatus;
import ru.practicum.ewm.user.model.User;

import java.time.LocalDateTime;

public class ParticipationRequestMapper {

    public static ParticipationRequest toEntity(Event event, User requester,
                                                ParticipationRequestStatus status) {
        ParticipationRequest request = new ParticipationRequest();
        request.setCreated(LocalDateTime.now());
        request.setEvent(event);
        request.setRequester(requester);
        request.setStatus(status);
        return request;
    }

    public static ParticipationRequestDto toDto(ParticipationRequest request) {
        ParticipationRequestDto dto = new ParticipationRequestDto();
        dto.setId(request.getId());
        dto.setCreated(request.getCreated());
        dto.setEvent(request.getEvent().getId());
        dto.setRequester(request.getRequester().getId());
        dto.setStatus(request.getStatus());
        return dto;
    }
}