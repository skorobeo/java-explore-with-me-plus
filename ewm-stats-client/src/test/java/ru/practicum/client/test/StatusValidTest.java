package ru.practicum.client.test;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.practicum.explore.client.exceptions.StatusValidator;

import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StatusValidTest {
    @Test
    void createRespone200() throws Exception {
        HttpResponse<String> mockResponse = Mockito.mock(HttpResponse.class);
        Mockito.when(mockResponse.statusCode()).thenReturn(201);

        assertDoesNotThrow(() -> StatusValidator.validate(mockResponse, 201));
    }

    @Test
    void createRespone500() throws Exception {
        HttpResponse<String> mockResponse = Mockito.mock(HttpResponse.class);
        Mockito.when(mockResponse.statusCode()).thenReturn(500);

        assertThrows(RuntimeException.class, () -> StatusValidator.validate(mockResponse, 201));
    }
}
