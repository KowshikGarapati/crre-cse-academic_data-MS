package com.crre.academicdms.features.responsibility.model;

import com.crre.academicdms.features.academic.model.Department;
import com.crre.academicdms.features.academic.model.Program;
import com.crre.academicdms.features.academic.model.Section;
import com.crre.academicdms.features.academic.model.Subject;
import com.crre.academicdms.features.user.model.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "responsibilities")
public class Responsibility {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String type; // TEACHING, CLASS_TEACHER, HOD_DEPT, COUNSELLOR, PRINCIPAL_INSTITUTION

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id")
    private Program program;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id")
    private Section section;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE"; // ACTIVE, INACTIVE

    @Column(length = 255)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Responsibility() {}

    public Responsibility(User user, String type, Department department, Program program, Subject subject, Section section, String description) {
        this.user = user;
        this.type = type;
        this.department = department;
        this.program = program;
        this.subject = subject;
        this.section = section;
        this.status = "ACTIVE";
        this.description = description;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public Program getProgram() { return program; }
    public void setProgram(Program program) { this.program = program; }
    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }
    public Section getSection() { return section; }
    public void setSection(Section section) { this.section = section; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
