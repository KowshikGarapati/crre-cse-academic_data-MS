package com.crre.academicdms.features.academic.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "semesters", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"program_id", "academic_year_id", "semester_number"})
})
public class Semester {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @Column(name = "semester_number", nullable = false)
    private int semesterNumber;

    @Column(name = "term_name", nullable = false, length = 50)
    private String termName;

    @Column(name = "is_current", nullable = false)
    private boolean current = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Semester() {}

    public Semester(Program program, AcademicYear academicYear, int semesterNumber, String termName, boolean current) {
        this.program = program;
        this.academicYear = academicYear;
        this.semesterNumber = semesterNumber;
        this.termName = termName;
        this.current = current;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Program getProgram() { return program; }
    public void setProgram(Program program) { this.program = program; }
    public AcademicYear getAcademicYear() { return academicYear; }
    public void setAcademicYear(AcademicYear academicYear) { this.academicYear = academicYear; }
    public int getSemesterNumber() { return semesterNumber; }
    public void setSemesterNumber(int semesterNumber) { this.semesterNumber = semesterNumber; }
    public String getTermName() { return termName; }
    public void setTermName(String termName) { this.termName = termName; }
    public boolean isCurrent() { return current; }
    public void setCurrent(boolean current) { this.current = current; }
    public Instant getCreatedAt() { return createdAt; }
}
