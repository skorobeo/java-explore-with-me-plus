package ru.practicum.client.test.StatsClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.practicum.ewmstats.dto.EndpointHit;
import ru.practicum.ewmstats.dto.ViewStats;
import ru.practicum.explore.client.StatsClient;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class StatsClientTest {
    private HttpClient mockHttpClient;
    private HttpResponse<String> mockResponse;

    @BeforeEach
    void setUp() {
        mockHttpClient = Mockito.mock(HttpClient.class);
        mockResponse = Mockito.mock(HttpResponse.class);
    }

    @Test
    void saveHitIsOkTest() throws Exception {
        StatsClient statsClient = new StatsClient("http://localhost:9090", mockHttpClient);
        Mockito.when(mockResponse.statusCode()).thenReturn(201);
        EndpointHit endpointHit = EndpointHit.builder().id(1L).app("Test").uri("Test").ip("Test").build();
        Mockito.when(mockHttpClient.send(Mockito.any(HttpRequest.class),
                        Mockito.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(mockResponse);

        assertDoesNotThrow(() -> statsClient.saveHit(endpointHit));
    }

    @Test
    void getStatsIsOkTest() throws Exception {
        Mockito.when(mockResponse.statusCode()).thenReturn(200);
        String json = "[{\"app\": \"ewm-main-service\", \"uri\": \"/events/1\", \"hits\": 6}]";
        Mockito.when(mockResponse.body()).thenReturn(json);
        Mockito.when(mockHttpClient.send(Mockito.any(HttpRequest.class),
                        Mockito.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(mockResponse);
        StatsClient statsClient = new StatsClient("http://localhost:9090", mockHttpClient);
        List<ViewStats> result = statsClient.getStats(LocalDateTime.now(), LocalDateTime.now(),
                null, null);

        assertEquals(1, result.size());
    }

    @Test
    void getStatsBadStatusTest() throws Exception {
        Mockito.when(mockResponse.statusCode()).thenReturn(500);
        String json = "[{\"app\": \"ewm-main-service\", \"uri\": \"/events/1\", \"hits\": 6}]";
        Mockito.when(mockResponse.body()).thenReturn(json);
        Mockito.when(mockHttpClient.send(Mockito.any(HttpRequest.class),
                        Mockito.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(mockResponse);
        StatsClient statsClient = new StatsClient("http://localhost:9090", mockHttpClient);

        assertThrows(RuntimeException.class, () -> statsClient.getStats(LocalDateTime.now(),
                LocalDateTime.now(), null, null));
    }

    @Test
    void saveHitBadStatusTest() throws Exception {
        StatsClient statsClient = new StatsClient("http://localhost:9090", mockHttpClient);
        Mockito.when(mockResponse.statusCode()).thenReturn(500);
        EndpointHit endpointHit = EndpointHit.builder().id(1L).app("Test").uri("Test").ip("Test").build();
        Mockito.when(mockHttpClient.send(Mockito.any(HttpRequest.class),
                        Mockito.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(mockResponse);

        assertThrows(RuntimeException.class, () -> statsClient.saveHit(endpointHit));
    }
}
