package ru.migrenenet.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.migrenenet.model.DiaryEntry;
import ru.migrenenet.model.DiaryEntry.Relief;
import ru.migrenenet.repo.DiaryRepository;
import ru.migrenenet.service.AlertService;
import ru.migrenenet.service.AlertService.Alert;
import ru.migrenenet.service.AnalyticsService;
import ru.migrenenet.service.AnalyticsService.Stats;

@Validated
@RestController
@RequestMapping("/api")
public class ApiController {
    public record DiaryRequest(
        @NotBlank String patientId,
        LocalDate date,
        boolean pain,
        @Min(0) @Max(10) int vas,
        @DecimalMin("0") @DecimalMax("24") double painHours,
        String location,
        boolean worseOnExertion,
        Set<String> symptoms,
        Set<String> triggers,
        boolean medTaken,
        String drug,
        @Min(0) Integer doseMg,
        String medTime,
        Relief relief,
        @Min(0) @Max(3) int functionLevel,
        @DecimalMin("0") @DecimalMax("24") double sleepHours,
        String sleepQuality) {}

    private final DiaryRepository repo;
    private final AnalyticsService analytics;
    private final AlertService alerts;

    public ApiController(DiaryRepository repo, AnalyticsService analytics, AlertService alerts) {
        this.repo = repo; this.analytics = analytics; this.alerts = alerts;
    }

    /** Сохранить дневник (повторная отправка за ту же дату обновляет запись). */
    @PostMapping("/diary")
    public DiaryEntry save(@Valid @RequestBody DiaryRequest r) {
        LocalDate d = r.date() != null ? r.date() : LocalDate.now().minusDays(1);
        DiaryEntry e = repo.findByPatientIdAndEntryDate(r.patientId(), d).orElseGet(DiaryEntry::new);
        e.setPatientId(r.patientId()); e.setEntryDate(d);
        e.setPain(r.pain()); e.setVas(r.pain() ? r.vas() : 0); e.setPainHours(r.pain() ? r.painHours() : 0);
        e.setLocation(r.location()); e.setWorseOnExertion(r.worseOnExertion());
        e.setSymptoms(r.symptoms() == null ? new java.util.HashSet<>() : new java.util.HashSet<>(r.symptoms()));
        e.setTriggers(r.triggers() == null ? new java.util.HashSet<>() : new java.util.HashSet<>(r.triggers()));
        e.setMedTaken(r.medTaken()); e.setDrug(r.drug()); e.setDoseMg(r.doseMg()); e.setMedTime(r.medTime());
        e.setRelief(r.medTaken() ? r.relief() : null);
        e.setFunctionLevel(r.functionLevel()); e.setSleepHours(r.sleepHours()); e.setSleepQuality(r.sleepQuality());
        return repo.save(e);
    }

    @GetMapping("/patients/{id}/entries")
    public List<DiaryEntry> entries(@PathVariable String id, @RequestParam(defaultValue = "30") @Min(1) @Max(365) int days) {
        LocalDate to = LocalDate.now();
        return repo.findByPatientIdAndEntryDateBetweenOrderByEntryDate(id, to.minusDays(days - 1L), to);
    }

    @GetMapping("/patients/{id}/stats")
    public Stats stats(@PathVariable String id, @RequestParam(defaultValue = "30") @Min(1) @Max(365) int days) {
        LocalDate to = LocalDate.now();
        return analytics.forPeriod(id, to.minusDays(days - 1L), to);
    }

    @GetMapping("/doctor/alerts")
    public List<Alert> doctorAlerts() { return alerts.all(LocalDate.now()); }
}
