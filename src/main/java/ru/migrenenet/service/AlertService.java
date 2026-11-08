package ru.migrenenet.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import ru.migrenenet.repo.DiaryRepository;
import ru.migrenenet.service.AnalyticsService.Stats;

/** Красные флаги для врача. Пороги вынесены в константы. */
@Service
public class AlertService {
    public enum Level { RED, YELLOW, GREEN }
    public record Alert(String patientId, Level level, String message) {}

    static final int MAX_MED_DAYS = 10;
    static final double VAS_GROWTH = 1.2;
    static final int MAX_LIMITED_DAYS = 8;

    private final AnalyticsService analytics;
    private final DiaryRepository repo;
    public AlertService(AnalyticsService analytics, DiaryRepository repo) { this.analytics = analytics; this.repo = repo; }

    public List<Alert> forPatient(String id, LocalDate today) {
        Stats now = analytics.forPeriod(id, today.minusDays(29), today);
        Stats prev = analytics.forPeriod(id, today.minusDays(59), today.minusDays(30));
        var out = new ArrayList<Alert>();
        if (now.medDays() > MAX_MED_DAYS)
            out.add(new Alert(id, Level.RED, "За последние 30 дней %d дней приёма обезболивающих. Рекомендуется пересмотр терапии / беседа о лекарственно-индуцированной головной боли.".formatted(now.medDays())));
        if (prev.headacheDays() > 0 && now.avgVas() - prev.avgVas() >= VAS_GROWTH)
            out.add(new Alert(id, Level.RED, "Средняя интенсивность приступов выросла с %.1f до %.1f балла за месяц. Требуется оценка профилактической терапии.".formatted(prev.avgVas(), now.avgVas())));
        if (now.limitedDays() >= MAX_LIMITED_DAYS)
            out.add(new Alert(id, Level.YELLOW, "%d дней с ограничением активности или пропуском работы.".formatted(now.limitedDays())));
        if (now.headacheDays() >= 15)
            out.add(new Alert(id, Level.YELLOW, "Боль %d дней в месяц — возможна хроническая форма.".formatted(now.headacheDays())));
        if (prev.headacheDays() > 0 && now.headacheDays() <= prev.headacheDays() / 2)
            out.add(new Alert(id, Level.GREEN, "Дней с болью стало вдвое меньше (%d → %d). Цель профилактики достигнута.".formatted(prev.headacheDays(), now.headacheDays())));
        return out;
    }

    public List<Alert> all(LocalDate today) {
        return repo.findPatientIds().stream().flatMap(p -> forPatient(p, today).stream()).toList();
    }
}
