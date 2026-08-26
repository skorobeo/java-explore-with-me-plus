package ru.practicum.ewmstats.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EndpointHit {

    private Long id;    // id записи в БД

    private String app; // Название сервиса-отправителя

    private String uri; // Адрес страницы, которую открыли

    private String ip;  // IP пользователя. По нему считаем просмотры

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")    // когда был запрос
    private LocalDateTime timestamp;
}