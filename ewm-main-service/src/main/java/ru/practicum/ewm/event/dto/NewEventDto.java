package ru.practicum.ewm.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import ru.practicum.ewm.location.dto.LocationDto;

import java.time.LocalDateTime;

@Data
@Builder
public class NewEventDto {

    @NotBlank
    @Size(max = 2000, min = 20)
    private String annotation;

    @NotNull
    private Long category;

    @NotBlank
    @Size(max = 7000, min = 20)
    private String description;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventDate;

    @NotNull
    private LocationDto location;

    private Boolean paid;

    private Integer participantLimit;

    private Boolean requestModeration;

    @NotBlank
    @Size(max = 120, min = 3)
    private String title;
}
