package ru.practicum.ewm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"ru.practicum.ewm", "ru.practicum.explore.client"})
public class EwmMainService {
    public static void main(String[] args) {
            SpringApplication.run(EwmMainService.class, args);
        }
}
