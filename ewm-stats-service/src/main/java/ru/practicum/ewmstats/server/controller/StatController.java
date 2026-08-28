package ru.practicum.ewmstats.server.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewmstats.server.exception.BadRequestException;
import ru.practicum.ewmstats.server.service.StatService;
import ru.practicum.ewmstats.dto.EndpointHit;
import ru.practicum.ewmstats.dto.ViewStats;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class StatController {
    private final StatService statService;

    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public void saveHit(@RequestBody EndpointHit hit) {
        statService.saveHit(hit);
    }

    @GetMapping("/stats")
    public List<ViewStats> getStats(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end,
            @RequestParam(required = false) List<String> uris,
            @RequestParam(defaultValue = "false") boolean unique) {


        if (start.isAfter(end)) {
            throw new BadRequestException("Start date must be before or equal to end date.");
        }

        return statService.getStats(start, end, uris, unique);
    }
}
