package ru.practicum.ewm.event.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.config.StatsClientConfig;
import ru.practicum.ewm.event.dto.*;
import ru.practicum.ewm.event.mapper.EventMapper;
import ru.practicum.ewm.event.model.AdminStateAction;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.State;
import ru.practicum.ewm.event.model.StateAction;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.event.service.EventService;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.location.mapper.LocationMapper;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;
import ru.practicum.ewmstats.dto.ViewStats;
import ru.practicum.explore.client.StatsClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ru.practicum.ewm.event.model.State.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final StatsClient statsClient;


    @Override
    public List<EventFullDto> getAdminEvents(List<Long> users, List<State> states, List<Long> categories, LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);
        Specification<Event> spec = Specification.where(null);

        if (categories != null && !categories.isEmpty()) {
            spec = spec.and((root, query, cb) -> root.get("category").get("id").in(categories));
        }

        if (users != null && !users.isEmpty()) {
            spec = spec.and((root, query, cb) -> root.get("initiator").get("id").in(users));
        }

        if (rangeStart != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("eventDate"), rangeStart));
        }

        if (rangeEnd != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("eventDate"), rangeEnd));
        }

        if (states != null && !states.isEmpty()) {
            spec = spec.and((root, query, cb) -> root.get("state").in(states));
        }

        List<Event> events = eventRepository.findAll(spec, pageable).getContent();

        List<String> uris = events.stream()
                .map(event -> "/events/" + event.getId())
                .collect(Collectors.toList());

        List<ViewStats> stats = statsClient.getStats(
                LocalDateTime.of(2001, 1, 1, 0, 0),
                LocalDateTime.now(),
                uris,
                false
        );

        Map<String, Long> viewsByUri = stats.stream()
                .collect(Collectors.toMap(ViewStats::getUri, ViewStats::getHits));

        return events.stream()
                .map(event ->  {
                    Long views = viewsByUri.getOrDefault("/events/" + event.getId(), 0L);
                    return EventMapper.toEventFullDto(event, views, 0L);
                })
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto patchAdminEventsId(Long eventId, UpdateEventAdminRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (request.getAnnotation() != null) {
            event.setAnnotation(request.getAnnotation());
        }

        if (request.getCategory() != null) {
            Category category = categoryRepository.findById(request.getCategory())
                    .orElseThrow(() -> new NotFoundException("Категория с id=" + request.getCategory() + " не найдена"));
            event.setCategory(category);
        }

        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }

        if (request.getEventDate() != null) {
            event.setEventDate(request.getEventDate());
        }

        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }

        if (request.getLocation() != null) {
            event.setLocation(LocationMapper.toLocation(request.getLocation()));
        }

        if (request.getParticipantLimit() != null) {
            event.setParticipantLimit(request.getParticipantLimit());
        }

        if (request.getRequestModeration() != null) {
            event.setRequestModeration(request.getRequestModeration());
        }

        if (request.getEventDate() != null &&
                request.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
            throw new ConflictException("Дата начала события должна быть не ранее чем за час от даты публикации");
        }

        if (request.getStateAction() == AdminStateAction.PUBLISH_EVENT) {
            if (event.getState() != State.PENDING) {
                throw new ConflictException("Cannot publish the event because it's not in the right state: " + event.getState());
            }
            event.setState(PUBLISHED);
            event.setPublishedOn(LocalDateTime.now());
        }

        if (request.getStateAction() == AdminStateAction.REJECT_EVENT) {
            if (event.getState() == State.PUBLISHED) {
                throw new ConflictException("Cannot reject the event because it's already published");
            }
            event.setState(CANCELED);
        }


        return EventMapper.toEventFullDto(eventRepository.save(event), 0L, 0L);
    }

    @Override
    public List<EventShortDto> getEvents(String text, List<Long> categories, Boolean paid, LocalDateTime rangeStart, LocalDateTime rangeEnd, Boolean onlyAvailable, String sort, int from, int size) {
        Specification<Event> spec = (root, query, cb) -> cb.equal(root.get("state"), State.PUBLISHED);

        if (text != null && !text.isBlank()) {
            String pattern = "%" + text.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("annotation")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            ));
        }

        if (categories != null && !categories.isEmpty()) {
            spec = spec.and((root, query, cb) -> root.get("category").get("id").in(categories));
        }

        if (paid != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("paid"), paid));
        }

        if (rangeStart != null && rangeEnd != null) {
            spec = spec.and((root, query, cb) -> cb.between(root.get("eventDate"), rangeStart, rangeEnd));
        } else {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("eventDate"), LocalDateTime.now()));
        }

        Sort sortOrder = "VIEWS".equalsIgnoreCase(sort)
                ? Sort.unsorted()
                : Sort.by(Sort.Direction.ASC, "eventDate");

        Pageable pageable = PageRequest.of(from / size, size, sortOrder);

        List<Event> events = eventRepository.findAll(spec, pageable).getContent();

        List<String> uris = events.stream()
                .map(event -> "/events/" + event.getId())
                .collect(Collectors.toList());

        List<ViewStats> stats = statsClient.getStats(
                LocalDateTime.of(2001, 1, 1, 0, 0),
                LocalDateTime.now(),
                uris,
                false
        );

        Map<String, Long> viewsByUri = stats.stream()
                .collect(Collectors.toMap(ViewStats::getUri, ViewStats::getHits));

        return events.stream()
                .map(event -> {
                    Long views = viewsByUri.getOrDefault("/events/" + event.getId(), 0L);
                    return EventMapper.toEventShortDto(event, views, 0L);
                })
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getPublicEventById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + id + " не найдено"));
        if (event.getState() != State.PUBLISHED) {
            throw new NotFoundException("Событие с id=" + id + " не найдено");
        }
        List<ViewStats> stats = statsClient.getStats(
                LocalDateTime.of(2000, 1, 1, 0, 0),
                LocalDateTime.now(),
                List.of("/events/" + event.getId()),
                false
        );

        Long views = stats.stream()
                .findFirst()
                .map(ViewStats::getHits)
                .orElse(0L);

        return EventMapper.toEventFullDto(event, views, 0L);
    }

    @Override
    public List<EventShortDto> getUserIdEvents(Long userId, int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);
        List<Event> events = eventRepository.findByInitiatorId(userId, pageable);

        List<String> uris = events.stream()
                .map(event -> "/events/" + event.getId())
                .collect(Collectors.toList());

        List<ViewStats> stats = statsClient.getStats(
                LocalDateTime.of(2000, 1, 1, 0, 0),
                LocalDateTime.now(),
                uris,
                false
        );

        Map<String, Long> viewsByUri = stats.stream()
                .collect(Collectors.toMap(ViewStats::getUri, ViewStats::getHits));

        return events.stream()
                .map(event -> {
                    Long views = viewsByUri.getOrDefault("/events/" + event.getId(), 0L);
                    return EventMapper.toEventShortDto(event, views, 0L);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto postUserIdEvent(Long userId, NewEventDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
    Category category = categoryRepository.findById(dto.getCategory())
            .orElseThrow(() -> new NotFoundException("Категория с id=" + dto.getCategory() + " не найдена"));
        if (dto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ConflictException("Event date must be at least 2 hours from now");
        }
        Event savedEvent = eventRepository.save(EventMapper.toEvent(dto, category, user));
        return EventMapper.toEventFullDto(savedEvent, 0L, 0L);
    }

    @Override
    public EventFullDto getUserIdEventId(Long userId, Long eventId) {
      Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
              .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId +
                      "и событие с id=" + eventId + " не найдены"));
        List<ViewStats> stats = statsClient.getStats(
                LocalDateTime.of(2003, 5, 6, 12, 2),
                LocalDateTime.now(),
                List.of("/events/" + event.getId()),
                false
        );

        Long views = stats.stream()
                .findFirst()
                .map(ViewStats::getHits)
                .orElse(0L);

        return EventMapper.toEventFullDto(event, views, 0L);
    }

    @Transactional
    @Override
    public EventFullDto patchUserIdEventId(Long userId, Long eventId,
                                           UpdateEventUserRequest updateEventUserRequest) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId +
                        "и событие с id=" + eventId + " не найдены"));

        if (event.getState() != State.PENDING && event.getState() != State.CANCELED) {
            throw new ConflictException("Изменить можно только отменённые события или события в ожидании модерации");
        }
        if (updateEventUserRequest.getEventDate() != null &&
                updateEventUserRequest.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ConflictException("Дата события не может быть раньше чем через 2 часа от текущего момента");
        }
        if (updateEventUserRequest.getTitle() != null) {
            event.setTitle(updateEventUserRequest.getTitle());
        }
        if (updateEventUserRequest.getEventDate() != null) {
            event.setEventDate(updateEventUserRequest.getEventDate());
        }

        if (updateEventUserRequest.getAnnotation() != null) {
            event.setAnnotation(updateEventUserRequest.getAnnotation());
        }

        if (updateEventUserRequest.getDescription() != null) {
            event.setDescription(updateEventUserRequest.getDescription());
        }

        if (updateEventUserRequest.getPaid() != null) {
            event.setPaid(updateEventUserRequest.getPaid());
        }

        if (updateEventUserRequest.getRequestModeration() != null) {
            event.setRequestModeration(updateEventUserRequest.getRequestModeration());
        }

        if (updateEventUserRequest.getParticipantLimit() != null) {
            event.setParticipantLimit(updateEventUserRequest.getParticipantLimit());
        }

        if (updateEventUserRequest.getCategory() != null) {
            Category category = categoryRepository.findById(updateEventUserRequest.getCategory())
                    .orElseThrow(() -> new NotFoundException("Категория с id=" + updateEventUserRequest.getCategory() + " не найдена"));
            event.setCategory(category);
        }

        if (updateEventUserRequest.getLocation() != null) {
            event.setLocation(LocationMapper.toLocation(updateEventUserRequest.getLocation()));
        }

        if (updateEventUserRequest.getStateAction() == StateAction.SEND_TO_REVIEW) {
            event.setState(PENDING);
        }

        if (updateEventUserRequest.getStateAction() == StateAction.CANCEL_REVIEW) {
            event.setState(CANCELED);
        }
        return EventMapper.toEventFullDto(eventRepository.save(event), 0L, 0L);
    }

}
