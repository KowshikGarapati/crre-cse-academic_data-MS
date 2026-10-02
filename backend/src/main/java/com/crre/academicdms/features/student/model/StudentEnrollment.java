package com.crre.academicdms.features.student.model;

import com.crre.academicdms.features.academic.model.Section;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "student_enrollments", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"student_id", "section_id"})
})
public class StudentEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @Column(name = "is_current", nullable = false)
    private boolean current = true;

    @Column(name = "enrolled_at", nullable = false, updatable = false)
    private Instant enrolledAt = Instant.now();

    public StudentEnrollment() {}

    public StudentEnrollment(Student student, Section section, boolean current) {
        this.student = student;
        this.section = section;
        this.current = current;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public Section getSection() { return section; }
    public void setSection(Section section) { this.section = section; }
    public boolean isCurrent() { return current; }
    public void setCurrent(boolean current) { this.current = current; }
    public Instant getEnrolledAt() { return enrolledAt; }
}
