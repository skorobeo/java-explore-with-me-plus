package ru.practicum.ewm.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import ru.practicum.ewm.event.model.AdminStateAction;
import ru.practicum.ewm.location.dto.LocationDto;

import java.time.LocalDateTime;

@Data
@Builder
public class UpdateEventAdminRequest {
    @Size(max = 2000, min = 20)
    private String annotation;

    private Long category;

    @Size(max = 7000, min = 20)
    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventDate;

    private LocationDto location;

    private Boolean paid;

    private Integer participantLimit;

    private Boolean requestModeration;

    private AdminStateAction stateAction;

    @Size(max = 120, min = 3)
    private String title;
}
