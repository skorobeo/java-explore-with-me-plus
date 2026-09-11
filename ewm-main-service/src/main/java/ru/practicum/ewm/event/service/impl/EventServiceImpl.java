package ru.practicum.ewm.event.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.dto.NewEventDto;
import ru.practicum.ewm.event.dto.UpdateEventAdminRequest;
import ru.practicum.ewm.event.dto.UpdateEventUserRequest;
import ru.practicum.ewm.event.mapper.EventMapper;
import ru.practicum.ewm.event.model.AdminStateAction;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.State;
import ru.practicum.ewm.event.model.StateAction;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.event.service.EventService;
import ru.practicum.ewm.exception.BadRequestException;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.location.mapper.LocationMapper;
import ru.practicum.ewm.request.model.ParticipationRequestStatus;
import ru.practicum.ewm.request.repository.ParticipationRequestRepository;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;
import ru.practicum.ewmstats.dto.EndpointHit;
import ru.practicum.ewmstats.dto.ViewStats;
import ru.practicum.explore.client.StatsClient;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ru.practicum.ewm.event.model.State.CANCELED;
import static ru.practicum.ewm.event.model.State.PENDING;
import static ru.practicum.ewm.event.model.State.PUBLISHED;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private static final String APP_NAME = "ewm-main-service";
    private static final String EVENTS_URI_PREFIX = "/events/";
    private static final LocalDateTime STATS_HISTORY_START =
            LocalDateTime.of(2000, 1, 1, 0, 0);

    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_X_REAL_IP = "X-Real-IP";

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final ParticipationRequestRepository requestRepository;
    private final StatsClient statsClient;

    @Override
    public List<EventFullDto> getAdminEvents(
            List<Long> users,
            List<State> states,
            List<Long> categories,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            int from,
            int size) {

        validateDateRange(rangeStart, rangeEnd);

        Specification<Event> spec = Specification.where(null);

        if (categories != null && !categories.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    root.get("category").get("id").in(categories));
        }

        if (users != null && !users.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    root.get("initiator").get("id").in(users));
        }

        if (rangeStart != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(
                            root.get("eventDate"), rangeStart));
        }

        if (rangeEnd != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(
                            root.get("eventDate"), rangeEnd));
        }

        if (states != null && !states.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    root.get("state").in(states));
        }

        List<Event> events = eventRepository.findAll(spec);

        if (events.isEmpty()) {
            return List.of();
        }

        Map<String, Long> viewsByUri = getViewsMap(events);
        Map<Long, Long> confirmedRequests =
                getConfirmedRequestsMap(events);

        List<EventFullDto> result = events.stream()
                .map(event -> EventMapper.toEventFullDto(
                        event,
                        viewsByUri.getOrDefault(
                                EVENTS_URI_PREFIX + event.getId(), 0L),
                        confirmedRequests.getOrDefault(event.getId(), 0L)))
                .collect(Collectors.toList());

        return paginateFull(result, from, size);
    }

    @Override
    @Transactional
    public EventFullDto patchAdminEventsId(
            Long eventId,
            UpdateEventAdminRequest request) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(
                        "Событие с id=" + eventId + " не найдено"));

        validateAdminEventDate(request.getEventDate());

        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }

        if (request.getAnnotation() != null) {
            event.setAnnotation(request.getAnnotation());
        }

        if (request.getCategory() != null) {
            Category category = categoryRepository
                    .findById(request.getCategory())
                    .orElseThrow(() -> new NotFoundException(
                            "Категория с id=" + request.getCategory()
                                    + " не найдена"));

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
            event.setLocation(
                    LocationMapper.toLocation(request.getLocation()));
        }

        if (request.getParticipantLimit() != null) {
            event.setParticipantLimit(
                    request.getParticipantLimit());
        }

        if (request.getRequestModeration() != null) {
            event.setRequestModeration(
                    request.getRequestModeration());
        }

        if (request.getStateAction() == AdminStateAction.PUBLISH_EVENT) {
            if (event.getState() != PENDING) {
                throw new ConflictException(
                        "Нельзя опубликовать событие, так как оно "
                                + "находится не в состоянии PENDING");
            }

            if (event.getEventDate().isBefore(
                    LocalDateTime.now().plusHours(1))) {
                throw new ConflictException(
                        "Дата начала события должна быть не ранее "
                                + "чем через час от даты публикации");
            }

            event.setState(PUBLISHED);
            event.setPublishedOn(LocalDateTime.now());
        }

        if (request.getStateAction() == AdminStateAction.REJECT_EVENT) {
            if (event.getState() == PUBLISHED) {
                throw new ConflictException(
                        "Нельзя отклонить уже опубликованное событие");
            }

            event.setState(CANCELED);
        }

        Event saved = eventRepository.save(event);

        Long views = getViewsForOneEvent(saved.getId());
        Long confirmed = getConfirmedRequestsForOneEvent(saved.getId());

        return EventMapper.toEventFullDto(
                saved,
                views,
                confirmed);
    }

    @Override
    public List<EventShortDto> getEvents(
            String text,
            List<Long> categories,
            Boolean paid,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            Boolean onlyAvailable,
            String sort,
            int from,
            int size) {

        validateDateRange(rangeStart, rangeEnd);

        Specification<Event> spec =
                (root, query, cb) -> cb.equal(
                        root.get("state"), PUBLISHED);

        if (text != null && !text.isBlank()) {
            String pattern = "%" + text.toLowerCase() + "%";

            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(
                            cb.lower(root.get("annotation")),
                            pattern),
                    cb.like(
                            cb.lower(root.get("description")),
                            pattern)
            ));
        }

        if (categories != null && !categories.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    root.get("category").get("id").in(categories));
        }

        if (paid != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("paid"), paid));
        }

        if (rangeStart != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(
                            root.get("eventDate"), rangeStart));
        }

        if (rangeEnd != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(
                            root.get("eventDate"), rangeEnd));
        }

        if (rangeStart == null && rangeEnd == null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(
                            root.get("eventDate"),
                            LocalDateTime.now()));
        }

        List<Event> events = eventRepository.findAll(
                spec,
                Sort.by(Sort.Direction.ASC, "eventDate"));

        if (events.isEmpty()) {
            return List.of();
        }

        Map<String, Long> viewsByUri = getViewsMap(events);
        Map<Long, Long> confirmedRequests =
                getConfirmedRequestsMap(events);

        List<EventShortDto> result = events.stream()
                .map(event -> EventMapper.toEventShortDto(
                        event,
                        viewsByUri.getOrDefault(
                                EVENTS_URI_PREFIX + event.getId(), 0L),
                        confirmedRequests.getOrDefault(event.getId(), 0L)))
                .collect(Collectors.toList());

        if (Boolean.TRUE.equals(onlyAvailable)) {
            Map<Long, Event> eventById = events.stream()
                    .collect(Collectors.toMap(
                            Event::getId,
                            event -> event));

            result = result.stream()
                    .filter(dto -> {
                        Event event = eventById.get(dto.getId());

                        return event.getParticipantLimit() == 0
                                || dto.getConfirmedRequests()
                                < event.getParticipantLimit();
                    })
                    .collect(Collectors.toList());
        }

        if ("VIEWS".equalsIgnoreCase(sort)) {
            result.sort(Comparator.comparing(
                    EventShortDto::getViews).reversed());
        }

        return paginateShort(result, from, size);
    }

    @Override
    @Transactional
    public EventFullDto getPublicEventById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Событие с id=" + id + " не найдено"));

        if (event.getState() != PUBLISHED) {
            throw new NotFoundException(
                    "Событие с id=" + id + " не найдено");
        }

        registerView(id);

        Long views = getViewsForOneEvent(id);
        Long confirmed = getConfirmedRequestsForOneEvent(id);

        return EventMapper.toEventFullDto(
                event,
                views,
                confirmed);
    }

    @Override
    public List<EventShortDto> getUserIdEvents(
            Long userId,
            int from,
            int size) {

        Pageable pageable = PageRequest.of(from / size, size);
        Page<Event> page = eventRepository.findByInitiatorId(
                userId,
                pageable);

        List<Event> events = page.getContent();

        if (events.isEmpty()) {
            return List.of();
        }

        Map<String, Long> viewsByUri = getViewsMap(events);
        Map<Long, Long> confirmedRequests =
                getConfirmedRequestsMap(events);

        return events.stream()
                .map(event -> EventMapper.toEventShortDto(
                        event,
                        viewsByUri.getOrDefault(
                                EVENTS_URI_PREFIX + event.getId(), 0L),
                        confirmedRequests.getOrDefault(event.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto postUserIdEvent(
            Long userId,
            NewEventDto dto) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(
                        "Пользователь с id=" + userId
                                + " не найден"));

        Category category = categoryRepository
                .findById(dto.getCategory())
                .orElseThrow(() -> new NotFoundException(
                        "Категория с id=" + dto.getCategory()
                                + " не найдена"));

        if (dto.getEventDate().isBefore(
                LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException(
                    "Дата события должна быть минимум через два часа");
        }

        Event savedEvent = eventRepository.save(
                EventMapper.toEvent(dto, category, user));

        return EventMapper.toEventFullDto(
                savedEvent,
                0L,
                0L);
    }

    @Override
    public EventFullDto getUserIdEventId(
            Long userId,
            Long eventId) {

        Event event = eventRepository
                .findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException(
                        "Пользователь с id=" + userId
                                + " и событие с id=" + eventId
                                + " не найдены"));

        Long views = getViewsForOneEvent(eventId);
        Long confirmed = getConfirmedRequestsForOneEvent(eventId);

        return EventMapper.toEventFullDto(
                event,
                views,
                confirmed);
    }

    @Override
    @Transactional
    public EventFullDto patchUserIdEventId(
            Long userId,
            Long eventId,
            UpdateEventUserRequest request) {

        Event event = eventRepository
                .findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException(
                        "Пользователь с id=" + userId
                                + " и событие с id=" + eventId
                                + " не найдены"));

        if (event.getState() != PENDING
                && event.getState() != CANCELED) {
            throw new ConflictException(
                    "Изменить можно только отменённые события "
                            + "или события в ожидании модерации");
        }

        if (request.getEventDate() != null
                && request.getEventDate().isBefore(
                LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException(
                    "Дата события не может быть раньше чем "
                            + "через два часа от текущего момента");
        }

        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }

        if (request.getEventDate() != null) {
            event.setEventDate(request.getEventDate());
        }

        if (request.getAnnotation() != null) {
            event.setAnnotation(request.getAnnotation());
        }

        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }

        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }

        if (request.getRequestModeration() != null) {
            event.setRequestModeration(
                    request.getRequestModeration());
        }

        if (request.getParticipantLimit() != null) {
            event.setParticipantLimit(
                    request.getParticipantLimit());
        }

        if (request.getCategory() != null) {
            Category category = categoryRepository
                    .findById(request.getCategory())
                    .orElseThrow(() -> new NotFoundException(
                            "Категория с id=" + request.getCategory()
                                    + " не найдена"));

            event.setCategory(category);
        }

        if (request.getLocation() != null) {
            event.setLocation(
                    LocationMapper.toLocation(request.getLocation()));
        }

        if (request.getStateAction() == StateAction.SEND_TO_REVIEW) {
            event.setState(PENDING);
        }

        if (request.getStateAction() == StateAction.CANCEL_REVIEW) {
            event.setState(CANCELED);
        }

        Event saved = eventRepository.save(event);

        Long views = getViewsForOneEvent(saved.getId());
        Long confirmed = getConfirmedRequestsForOneEvent(saved.getId());

        return EventMapper.toEventFullDto(
                saved,
                views,
                confirmed);
    }

    private void validateDateRange(
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd) {

        if (rangeStart != null
                && rangeEnd != null
                && rangeStart.isAfter(rangeEnd)) {
            throw new BadRequestException(
                    "Дата начала диапазона не может быть позже "
                            + "даты окончания");
        }
    }

    private void validateAdminEventDate(LocalDateTime eventDate) {
        if (eventDate != null
                && eventDate.isBefore(
                LocalDateTime.now().plusHours(1))) {
            throw new BadRequestException(
                    "Дата начала события должна быть не ранее "
                            + "чем через час от текущего момента");
        }
    }


    private void registerView(Long eventId) {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder
                        .getRequestAttributes();

        if (attributes == null) {
            log.warn("Не удалось получить атрибуты запроса для "
                    + "регистрации просмотра события id={}", eventId);
            return;
        }

        HttpServletRequest request = attributes.getRequest();
        String ip = resolveClientIp(request);
        String uri = EVENTS_URI_PREFIX + eventId;
        LocalDateTime timestamp = LocalDateTime.now();

        EndpointHit hit = new EndpointHit();
        hit.setApp(APP_NAME);
        hit.setUri(uri);
        hit.setIp(ip);
        hit.setTimestamp(timestamp);

        log.debug("saveHit app={} uri={} ip={} ts={}",
                APP_NAME, uri, ip, timestamp);

        try {
            statsClient.saveHit(hit);
        } catch (Exception e) {
            log.error("Не удалось сохранить хит для uri={} ip={}: {}",
                    uri, ip, e.getMessage(), e);
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader(HEADER_X_FORWARDED_FOR);

        if (forwarded != null && !forwarded.isBlank()) {
            // X-Forwarded-For может содержать цепочку: client, proxy1, proxy2
            return forwarded.split(",")[0].trim();
        }

        String realIp = request.getHeader(HEADER_X_REAL_IP);

        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }

    private List<EventShortDto> paginateShort(
            List<EventShortDto> list,
            int from,
            int size) {

        return list.stream()
                .skip(from)
                .limit(size)
                .collect(Collectors.toList());
    }

    private List<EventFullDto> paginateFull(
            List<EventFullDto> list,
            int from,
            int size) {

        return list.stream()
                .skip(from)
                .limit(size)
                .collect(Collectors.toList());
    }

    private Map<String, Long> getViewsMap(List<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }

        List<String> uris = events.stream()
                .map(event -> EVENTS_URI_PREFIX + event.getId())
                .collect(Collectors.toList());

        List<ViewStats> stats = statsClient.getStats(
                STATS_HISTORY_START,
                statsEndTime(),
                uris,
                true);

        return stats.stream()
                .collect(Collectors.toMap(
                        ViewStats::getUri,
                        ViewStats::getHits,
                        Long::sum));
    }

    private Long getViewsForOneEvent(Long eventId) {
        String uri = EVENTS_URI_PREFIX + eventId;

        List<ViewStats> stats = statsClient.getStats(
                STATS_HISTORY_START,
                statsEndTime(),
                List.of(uri),
                true);

        long views = stats.stream()
                .mapToLong(ViewStats::getHits)
                .sum();

        log.debug("getStats uri={} unique=true -> views={}", uri, views);

        return views;
    }


    private LocalDateTime statsEndTime() {
        return LocalDateTime.now().plusSeconds(1);
    }

    private Map<Long, Long> getConfirmedRequestsMap(
            List<Event> events) {

        if (events.isEmpty()) {
            return Map.of();
        }

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .collect(Collectors.toList());

        return requestRepository
                .countConfirmedByEventIds(eventIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]));
    }

    private Long getConfirmedRequestsForOneEvent(Long eventId) {
        return requestRepository.countByEventIdAndStatus(
                eventId,
                ParticipationRequestStatus.CONFIRMED);
    }
}