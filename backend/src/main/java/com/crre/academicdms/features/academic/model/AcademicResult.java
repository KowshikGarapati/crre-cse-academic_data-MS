package com.crre.academicdms.features.academic.model;

import com.crre.academicdms.features.student.model.Student;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "academic_results", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"student_id", "subject_id"})
})
public class AcademicResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(name = "internal_marks", precision = 5, scale = 2)
    private BigDecimal internalMarks;

    @Column(name = "external_marks", precision = 5, scale = 2)
    private BigDecimal externalMarks;

    @Column(name = "total_marks", precision = 5, scale = 2)
    private BigDecimal totalMarks;

    @Column(name = "grade_letter", length = 5)
    private String gradeLetter;

    @Column(name = "grade_points", precision = 3, scale = 1)
    private BigDecimal gradePoints;

    @Column(name = "credits_earned", precision = 3, scale = 1)
    private BigDecimal creditsEarned;

    @Column(name = "is_pass")
    private Boolean pass = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public AcademicResult() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }
    public BigDecimal getInternalMarks() { return internalMarks; }
    public void setInternalMarks(BigDecimal internalMarks) { this.internalMarks = internalMarks; }
    public BigDecimal getExternalMarks() { return externalMarks; }
    public void setExternalMarks(BigDecimal externalMarks) { this.externalMarks = externalMarks; }
    public BigDecimal getTotalMarks() { return totalMarks; }
    public void setTotalMarks(BigDecimal totalMarks) { this.totalMarks = totalMarks; }
    public String getGradeLetter() { return gradeLetter; }
    public void setGradeLetter(String gradeLetter) { this.gradeLetter = gradeLetter; }
    public BigDecimal getGradePoints() { return gradePoints; }
    public void setGradePoints(BigDecimal gradePoints) { this.gradePoints = gradePoints; }
    public BigDecimal getCreditsEarned() { return creditsEarned; }
    public void setCreditsEarned(BigDecimal creditsEarned) { this.creditsEarned = creditsEarned; }
    public Boolean getPass() { return pass; }
    public void setPass(Boolean pass) { this.pass = pass; }
    public Instant getCreatedAt() { return createdAt; }
}
