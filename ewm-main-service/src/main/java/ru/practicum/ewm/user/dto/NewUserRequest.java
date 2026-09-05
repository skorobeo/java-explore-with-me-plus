package ru.practicum.ewm.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NewUserRequest{
    @NotBlank
    @Size(max = 254, min = 6)
    @Email
    private String email;

    @NotBlank
    @Size(max = 250, min = 2)
    private String name;
}
