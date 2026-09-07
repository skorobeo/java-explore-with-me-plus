package ru.practicum.ewm.request.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.State;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.BadRequestException;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.mapper.ParticipationRequestMapper;
import ru.practicum.ewm.request.model.ParticipationRequest;
import ru.practicum.ewm.request.model.ParticipationRequestStatus;
import ru.practicum.ewm.request.model.RequestStatusAction;
import ru.practicum.ewm.request.repository.ParticipationRequestRepository;
import ru.practicum.ewm.request.service.ParticipationRequestService;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParticipationRequestServiceImpl implements ParticipationRequestService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final ParticipationRequestRepository requestRepository;
    private final EntityManager entityManager;

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        checkUserExists(userId);
        return requestRepository.findAllByRequesterIdOrderByIdAsc(userId).stream()
                .map(ParticipationRequestMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public ParticipationRequestDto addParticipationRequest(Long userId, Long eventId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (event.getState() != State.PUBLISHED) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии");
        }
        if (userId.equals(event.getInitiator().getId())) {
            throw new ConflictException("Инициатор события не может подать заявку на своё событие");
        }
        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ConflictException("Заявка на это событие уже подана");
        }

        int limit = event.getParticipantLimit() == null ? 0 : event.getParticipantLimit();
        if (limit > 0) {
            Event locked = entityManager.find(Event.class, eventId, LockModeType.PESSIMISTIC_WRITE);
            if (locked == null) {
                throw new NotFoundException("Событие с id=" + eventId + " не найдено");
            }
            event = locked;
            long confirmed = requestRepository.countByEventIdAndStatus(eventId,
                    ParticipationRequestStatus.CONFIRMED);
            if (confirmed >= limit) {
                throw new ConflictException("The participant limit has been reached");
            }
        }

        boolean moderation = Boolean.TRUE.equals(event.getRequestModeration());
        ParticipationRequestStatus status = (limit == 0 || !moderation)
                ? ParticipationRequestStatus.CONFIRMED
                : ParticipationRequestStatus.PENDING;

        ParticipationRequest saved = requestRepository.save(
                ParticipationRequestMapper.toEntity(event, user, status));
        return ParticipationRequestMapper.toDto(saved);
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        ParticipationRequest request = requestRepository.findByIdAndRequesterId(requestId, userId)
                .orElseThrow(() -> new NotFoundException("Заявка с id=" + requestId + " не найдена"));
        request.setStatus(ParticipationRequestStatus.CANCELED);
        return ParticipationRequestMapper.toDto(requestRepository.save(request));
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        checkEventOwner(userId, eventId);
        return requestRepository.findAllByEventIdOrderByIdAsc(eventId).stream()
                .map(ParticipationRequestMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult changeRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest updateRequest) {
        checkEventOwner(userId, eventId);
        Event locked = entityManager.find(Event.class, eventId, LockModeType.PESSIMISTIC_WRITE);
        if (locked == null) {
            throw new NotFoundException("Событие с id=" + eventId + " не найдено");
        }

        List<ParticipationRequest> requests = requestRepository.findAllById(updateRequest.getRequestIds());
        if (requests.size() != updateRequest.getRequestIds().size()) {
            throw new BadRequestException("Request must have status PENDING");
        }
        for (ParticipationRequest request : requests) {
            if (!request.getEvent().getId().equals(eventId)
                    || request.getStatus() != ParticipationRequestStatus.PENDING) {
                throw new BadRequestException("Request must have status PENDING");
            }
        }

        int limit = locked.getParticipantLimit() == null ? 0 : locked.getParticipantLimit();
        List<ParticipationRequest> confirmed = new ArrayList<>();
        List<ParticipationRequest> rejected = new ArrayList<>();

        if (updateRequest.getStatus() == RequestStatusAction.REJECTED) {
            for (ParticipationRequest request : requests) {
                request.setStatus(ParticipationRequestStatus.REJECTED);
                rejected.add(request);
            }
        } else if (updateRequest.getStatus() == RequestStatusAction.CONFIRMED) {
            long confirmedCount = requestRepository.countByEventIdAndStatus(eventId,
                    ParticipationRequestStatus.CONFIRMED);
            if (limit > 0 && confirmedCount + requests.size() > limit) {
                throw new ConflictException("The participant limit has been reached");
            }
            for (ParticipationRequest request : requests) {
                request.setStatus(ParticipationRequestStatus.CONFIRMED);
                confirmed.add(request);
            }
            if (limit > 0 && confirmedCount + confirmed.size() >= limit) {
                Set<Long> handledIds = new HashSet<>();
                for (ParticipationRequest request : requests) {
                    handledIds.add(request.getId());
                }
                List<ParticipationRequest> rest = requestRepository.findAllByEventIdAndStatus(eventId,
                        ParticipationRequestStatus.PENDING);
                for (ParticipationRequest request : rest) {
                    if (!handledIds.contains(request.getId())) {
                        request.setStatus(ParticipationRequestStatus.REJECTED);
                        rejected.add(request);
                    }
                }
            }
        } else {
            throw new BadRequestException("Request must have status PENDING");
        }

        requestRepository.saveAll(confirmed);
        requestRepository.saveAll(rejected);

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmed.stream()
                        .map(ParticipationRequestMapper::toDto)
                        .toList())
                .rejectedRequests(rejected.stream()
                        .map(ParticipationRequestMapper::toDto)
                        .toList())
                .build();
    }

    private void checkUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }
    }

    private void checkEventOwner(Long userId, Long eventId) {
        eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));
    }
}