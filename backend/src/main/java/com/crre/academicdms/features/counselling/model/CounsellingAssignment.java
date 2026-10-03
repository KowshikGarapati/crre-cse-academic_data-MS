package com.crre.academicdms.features.counselling.model;

import com.crre.academicdms.features.student.model.Student;
import com.crre.academicdms.features.user.model.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "counselling_assignments", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"counsellor_user_id", "student_id"})
})
public class CounsellingAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counsellor_user_id", nullable = false)
    private User counsellor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt = Instant.now();

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE"; // ACTIVE, INACTIVE

    public CounsellingAssignment() {}

    public CounsellingAssignment(User counsellor, Student student, String status) {
        this.counsellor = counsellor;
        this.student = student;
        this.status = status != null ? status : "ACTIVE";
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getCounsellor() { return counsellor; }
    public void setCounsellor(User counsellor) { this.counsellor = counsellor; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public Instant getAssignedAt() { return assignedAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
