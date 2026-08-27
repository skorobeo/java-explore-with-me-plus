package ru.practicum.explore.client.exceptions;

import java.net.http.HttpResponse;

public class StatusValidator {
    public static void validate(HttpResponse<String> response, int expectedStatus) {
         if (response.statusCode() != expectedStatus) {
            throw new RuntimeException("Статусы кода разные что-то пошло не так!! " + response.statusCode()
                    + " | " + expectedStatus);
        }
    }
}
