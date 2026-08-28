
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.evmstats.server.model.Hit;
import ru.practicum.evmstats.server.repository.StatRepository;
import ru.practicum.ewmstats.dto.ViewStats;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class StatRepositoryIntegrationTest {

    @Autowired
    private StatRepository statRepository;

    private Hit hit1;
    private Hit hit2;

    @BeforeEach
    void setUp() {
        hit1 = Hit.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.168.0.1")
                .timestamp(LocalDateTime.of(2024, 1, 1, 10, 0, 0))
                .build();
        hit2 = Hit.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.168.0.2")
                .timestamp(LocalDateTime.of(2024, 1, 1, 11, 0, 0))
                .build();
        statRepository.save(hit1);
        statRepository.save(hit2);
    }

    @Test
    void getStatUnique_countsDistinctIp() {
        List<ViewStats> result = statRepository.getStatUnique(
                LocalDateTime.of(2024, 1, 1, 0, 0),
                LocalDateTime.of(2024, 1, 2, 0, 0),
                null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUri()).isEqualTo("/events/1");
        assertThat(result.get(0).getHits()).isEqualTo(2L); // уникальные IP: 2
    }

    @Test
    void getStat_countsAllHits() {
        // Добавим ещё один хит с тем же IP, чтобы проверить разницу
        Hit hit3 = Hit.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.168.0.1")
                .timestamp(LocalDateTime.of(2024, 1, 1, 12, 0, 0))
                .build();
        statRepository.save(hit3);

        List<ViewStats> result = statRepository.getStat(
                LocalDateTime.of(2024, 1, 1, 0, 0),
                LocalDateTime.of(2024, 1, 2, 0, 0),
                null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getHits()).isEqualTo(3L); // всего хитов: 3
    }

    @Test
    void getStatFiltersByUris() {
        Hit hitOther = Hit.builder()
                .app("ewm-main-service")
                .uri("/events/2")
                .ip("192.168.0.3")
                .timestamp(LocalDateTime.of(2024, 1, 1, 13, 0, 0))
                .build();
        statRepository.save(hitOther);

        List<ViewStats> result = statRepository.getStat(
                LocalDateTime.of(2024, 1, 1, 0, 0),
                LocalDateTime.of(2024, 1, 2, 0, 0),
                List.of("/events/1"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUri()).isEqualTo("/events/1");
    }
}