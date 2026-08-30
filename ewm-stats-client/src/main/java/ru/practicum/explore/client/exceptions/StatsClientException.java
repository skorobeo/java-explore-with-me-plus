package ru.practicum.explore.client.exceptions;

public class StatsClientException extends RuntimeException {
    public StatsClientException(String message) {
        super(message);
    }

    public StatsClientException(String message, Throwable cause) {
        super(message, cause);
    }
}