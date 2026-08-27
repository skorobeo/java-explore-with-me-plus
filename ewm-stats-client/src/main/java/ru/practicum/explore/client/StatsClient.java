package ru.practicum.explore.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.practicum.ewmstats.dto.EndpointHit;
import ru.practicum.ewmstats.dto.ViewStats;
import ru.practicum.explore.client.exceptions.StatusValidator;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class StatsClient {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String serverUrl;

    public StatsClient(@Value("${stats-server.url}") String serverUrl, HttpClient httpClient) {
        this.serverUrl = serverUrl;
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    public void saveHit(EndpointHit hit) {
        try {
            String body = objectMapper.writeValueAsString(hit);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl + "/hit"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            StatusValidator.validate(response, 201);
        } catch (Exception e) {
            throw new RuntimeException("Error while sending hit to stats service", e);
        }
    }

    public List<ViewStats> getStats(LocalDateTime start,
                                    LocalDateTime end,
                                    List<String> uris,
                                    Boolean unique) {
        try {
            StringBuilder queryBuilder = new StringBuilder("/stats?");
            queryBuilder.append("start=").append(encode(start.format(FORMATTER)));
            queryBuilder.append("&end=").append(encode(end.format(FORMATTER)));
            queryBuilder.append("&unique=").append(unique != null ? unique : false);

            if (uris != null && !uris.isEmpty()) {
                for (String u : uris) {
                    queryBuilder.append("&uris=").append(encode(u));
                }
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl + queryBuilder))
                    .GET()
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            StatusValidator.validate(response, 200);
            return objectMapper.readValue(response.body(), new TypeReference<List<ViewStats>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при получении статистики", e);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}