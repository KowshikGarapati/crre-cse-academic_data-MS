package com.crre.academicdms.features.academic.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "academic_years")
public class AcademicYear {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "year_name", nullable = false, unique = true, length = 50)
    private String yearName;

    @Column(name = "year_number", nullable = false)
    private int yearNumber;

    @Column(name = "is_current", nullable = false)
    private boolean current = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public AcademicYear() {}

    public AcademicYear(String yearName, int yearNumber, boolean current) {
        this.yearName = yearName;
        this.yearNumber = yearNumber;
        this.current = current;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getYearName() { return yearName; }
    public void setYearName(String yearName) { this.yearName = yearName; }
    public int getYearNumber() { return yearNumber; }
    public void setYearNumber(int yearNumber) { this.yearNumber = yearNumber; }
    public boolean isCurrent() { return current; }
    public void setCurrent(boolean current) { this.current = current; }
    public Instant getCreatedAt() { return createdAt; }
}
