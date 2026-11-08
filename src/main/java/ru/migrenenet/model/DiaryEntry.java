package ru.migrenenet.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/** Одна запись утреннего дневника за сутки. */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"patientId", "entryDate"}))
public class DiaryEntry {
    public enum Relief { FULL, PARTIAL, NONE }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String patientId;
    private LocalDate entryDate;
    private boolean pain;
    private int vas;                 // ВАШ 0-10
    private double painHours;
    private String location;
    private boolean worseOnExertion;
    private boolean medTaken;
    private String drug;
    private Integer doseMg;
    private String medTime;
    @Enumerated(EnumType.STRING) private Relief relief;
    private int functionLevel;       // 0 норма, 1 трудности, 2 снижение активности, 3 пропуск
    private double sleepHours;
    private String sleepQuality;
    @ElementCollection(fetch = FetchType.EAGER) private Set<String> symptoms = new HashSet<>();
    @ElementCollection(fetch = FetchType.EAGER) private Set<String> triggers = new HashSet<>();

    public Long getId() { return id; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String v) { patientId = v; }
    public LocalDate getEntryDate() { return entryDate; }
    public void setEntryDate(LocalDate v) { entryDate = v; }
    public boolean isPain() { return pain; }
    public void setPain(boolean v) { pain = v; }
    public int getVas() { return vas; }
    public void setVas(int v) { vas = v; }
    public double getPainHours() { return painHours; }
    public void setPainHours(double v) { painHours = v; }
    public String getLocation() { return location; }
    public void setLocation(String v) { location = v; }
    public boolean isWorseOnExertion() { return worseOnExertion; }
    public void setWorseOnExertion(boolean v) { worseOnExertion = v; }
    public boolean isMedTaken() { return medTaken; }
    public void setMedTaken(boolean v) { medTaken = v; }
    public String getDrug() { return drug; }
    public void setDrug(String v) { drug = v; }
    public Integer getDoseMg() { return doseMg; }
    public void setDoseMg(Integer v) { doseMg = v; }
    public String getMedTime() { return medTime; }
    public void setMedTime(String v) { medTime = v; }
    public Relief getRelief() { return relief; }
    public void setRelief(Relief v) { relief = v; }
    public int getFunctionLevel() { return functionLevel; }
    public void setFunctionLevel(int v) { functionLevel = v; }
    public double getSleepHours() { return sleepHours; }
    public void setSleepHours(double v) { sleepHours = v; }
    public String getSleepQuality() { return sleepQuality; }
    public void setSleepQuality(String v) { sleepQuality = v; }
    public Set<String> getSymptoms() { return symptoms; }
    public void setSymptoms(Set<String> v) { symptoms = v; }
    public Set<String> getTriggers() { return triggers; }
    public void setTriggers(Set<String> v) { triggers = v; }
}
