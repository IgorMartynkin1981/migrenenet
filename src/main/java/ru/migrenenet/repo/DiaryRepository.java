package ru.migrenenet.repo;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.migrenenet.model.DiaryEntry;

public interface DiaryRepository extends JpaRepository<DiaryEntry, Long> {
    Optional<DiaryEntry> findByPatientIdAndEntryDate(String patientId, LocalDate date);
    List<DiaryEntry> findByPatientIdAndEntryDateBetweenOrderByEntryDate(String patientId, LocalDate from, LocalDate to);
    @Query("select distinct e.patientId from DiaryEntry e")
    List<String> findPatientIds();
}
