package ru.practicum.ewm.event.service;

import ru.practicum.ewm.event.dto.*;
import ru.practicum.ewm.event.model.State;

import java.time.LocalDateTime;
import java.util.List;

public interface EventService {
    List<EventFullDto> getAdminEvents(List<Long> users, List<State> states,
                                      List<Long> categories, LocalDateTime rangeStart,
                                      LocalDateTime rangeEnd, int from, int size);

    EventFullDto patchAdminEventsId(Long eventId, UpdateEventAdminRequest request);

    List<EventShortDto> getEvents(String text, List<Long> categories,
                            Boolean paid, LocalDateTime rangeStart,
                            LocalDateTime rangeEnd, Boolean onlyAvailable,
                            String sort, int from, int size);

    EventFullDto getPublicEventById(Long id);

    List<EventShortDto> getUserIdEvents(Long userId, int from, int size);

    EventFullDto postUserIdEvent(Long userId, NewEventDto dto);

    EventFullDto getUserIdEventId(Long userId, Long eventId);

    EventFullDto patchUserIdEventId(Long userId, Long eventId, UpdateEventUserRequest updateEventUserRequest);
}
