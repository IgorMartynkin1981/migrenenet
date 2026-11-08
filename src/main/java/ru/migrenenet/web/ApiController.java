package ru.migrenenet.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.migrenenet.bot.TelegramAuth;
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
        String patientId,
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
    private final TelegramAuth auth;
    private final String doctorKey;

    public ApiController(DiaryRepository repo, AnalyticsService analytics, AlertService alerts,
                         TelegramAuth auth, @Value("${doctor.key:}") String doctorKey) {
        this.repo = repo; this.analytics = analytics; this.alerts = alerts; this.auth = auth; this.doctorKey = doctorKey;
    }

    /** С токеном бота пациент определяется по подписи Telegram. Без токена (локальная разработка) берётся из запроса. */
    private String patient(String initData, String claimed) {
        if (!auth.enabled()) {
            if (claimed == null || claimed.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "patientId");
            return claimed;
        }
        String id = auth.userId(initData).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (claimed != null && !claimed.equals(id)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return id;
    }

    @GetMapping("/health")
    public Map<String, String> health() { return Map.of("status", "ok"); }

    /** Сохранить дневник (повторная отправка за ту же дату обновляет запись). */
    @PostMapping("/diary")
    public DiaryEntry save(@Valid @RequestBody DiaryRequest r,
                           @RequestHeader(value = "X-Telegram-Init-Data", required = false) String init) {
        String pid = patient(init, r.patientId());
        LocalDate d = r.date() != null ? r.date() : LocalDate.now().minusDays(1);
        DiaryEntry e = repo.findByPatientIdAndEntryDate(pid, d).orElseGet(DiaryEntry::new);
        e.setPatientId(pid); e.setEntryDate(d);
        e.setPain(r.pain()); e.setVas(r.pain() ? r.vas() : 0); e.setPainHours(r.pain() ? r.painHours() : 0);
        e.setLocation(r.location()); e.setWorseOnExertion(r.worseOnExertion());
        e.setSymptoms(r.symptoms() == null ? new HashSet<>() : new HashSet<>(r.symptoms()));
        e.setTriggers(r.triggers() == null ? new HashSet<>() : new HashSet<>(r.triggers()));
        e.setMedTaken(r.medTaken()); e.setDrug(r.drug()); e.setDoseMg(r.doseMg()); e.setMedTime(r.medTime());
        e.setRelief(r.medTaken() ? r.relief() : null);
        e.setFunctionLevel(r.functionLevel()); e.setSleepHours(r.sleepHours()); e.setSleepQuality(r.sleepQuality());
        return repo.save(e);
    }

    @GetMapping("/patients/{id}/entries")
    public List<DiaryEntry> entries(@PathVariable String id, @RequestParam(defaultValue = "30") @Min(1) @Max(365) int days,
                                    @RequestHeader(value = "X-Telegram-Init-Data", required = false) String init) {
        LocalDate to = LocalDate.now();
        return repo.findByPatientIdAndEntryDateBetweenOrderByEntryDate(patient(init, id), to.minusDays(days - 1L), to);
    }

    @GetMapping("/patients/{id}/stats")
    public Stats stats(@PathVariable String id, @RequestParam(defaultValue = "30") @Min(1) @Max(365) int days,
                       @RequestHeader(value = "X-Telegram-Init-Data", required = false) String init) {
        LocalDate to = LocalDate.now();
        return analytics.forPeriod(patient(init, id), to.minusDays(days - 1L), to);
    }

    /** Данные других пациентов: только по ключу врача. В продакшене (задан токен бота) ключ обязателен. */
    @GetMapping("/doctor/alerts")
    public List<Alert> doctorAlerts(@RequestHeader(value = "X-Doctor-Key", required = false) String key) {
        boolean denied = doctorKey.isBlank() ? auth.enabled() : !doctorKey.equals(key);
        if (denied) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return alerts.all(LocalDate.now());
    }
}
