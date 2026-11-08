package ru.migrenenet.service;

import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import ru.migrenenet.model.DiaryEntry;
import ru.migrenenet.model.DiaryEntry.Relief;
import ru.migrenenet.repo.DiaryRepository;

/** Пять критериев оценки эффективности лечения. */
@Service
public class AnalyticsService {
    public record Stats(int headacheDays, double avgVas, int medDays, int limitedDays, Integer reliefPercent) {}

    private final DiaryRepository repo;
    public AnalyticsService(DiaryRepository repo) { this.repo = repo; }

    public Stats forPeriod(String patientId, LocalDate from, LocalDate to) {
        return compute(repo.findByPatientIdAndEntryDateBetweenOrderByEntryDate(patientId, from, to));
    }

    public static Stats compute(List<DiaryEntry> list) {
        var pain = list.stream().filter(DiaryEntry::isPain).toList();
        double avg = pain.stream().mapToInt(DiaryEntry::getVas).average().orElse(0);
        var med = list.stream().filter(DiaryEntry::isMedTaken).toList();
        int limited = (int) list.stream().filter(e -> e.getFunctionLevel() >= 2).count();
        long full = med.stream().filter(e -> e.getRelief() == Relief.FULL).count();
        Integer rel = med.isEmpty() ? null : (int) Math.round(100.0 * full / med.size());
        return new Stats(pain.size(), Math.round(avg * 10) / 10.0, med.size(), limited, rel);
    }
}
