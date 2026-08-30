package ru.practicum.ewmstats.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViewStats {

    private String app; // название сервиса

    private String uri; // страница, по которой посчитали просмотры

    private Long hits;  // сколько раз открыли
}