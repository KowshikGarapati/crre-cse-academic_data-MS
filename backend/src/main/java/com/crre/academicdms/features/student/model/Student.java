package com.crre.academicdms.features.student.model;

import com.crre.academicdms.features.user.model.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "students")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "registration_number", nullable = false, unique = true, length = 20)
    private String registrationNumber;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "entry_type", nullable = false, length = 20)
    private String entryType = "REGULAR"; // REGULAR, LATERAL

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE"; // ACTIVE, INACTIVE

    @Column(name = "admission_category", length = 30)
    private String admissionCategory;

    @Column(name = "tuition_category", length = 30)
    private String tuitionCategory;

    @Column(name = "date_of_birth")
    private java.time.LocalDate dateOfBirth;

    @Column(length = 20)
    private String gender;

    @Column(name = "entrance_exam_name", length = 50)
    private String entranceExamName;

    @Column(name = "entrance_hall_ticket", length = 50)
    private String entranceHallTicket;

    @Column(name = "entrance_rank")
    private Integer entranceRank;

    @Column(name = "is_synthetic", nullable = false)
    private boolean isSynthetic = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Student() {}

    public Student(String registrationNumber, String fullName, String entryType) {
        this.registrationNumber = registrationNumber;
        this.fullName = fullName;
        this.entryType = entryType;
        this.status = "ACTIVE";
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEntryType() { return entryType; }
    public void setEntryType(String entryType) { this.entryType = entryType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAdmissionCategory() { return admissionCategory; }
    public void setAdmissionCategory(String admissionCategory) { this.admissionCategory = admissionCategory; }
    public String getTuitionCategory() { return tuitionCategory; }
    public void setTuitionCategory(String tuitionCategory) { this.tuitionCategory = tuitionCategory; }

    public java.time.LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(java.time.LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getEntranceExamName() { return entranceExamName; }
    public void setEntranceExamName(String entranceExamName) { this.entranceExamName = entranceExamName; }

    public String getEntranceHallTicket() { return entranceHallTicket; }
    public void setEntranceHallTicket(String entranceHallTicket) { this.entranceHallTicket = entranceHallTicket; }

    public Integer getEntranceRank() { return entranceRank; }
    public void setEntranceRank(Integer entranceRank) { this.entranceRank = entranceRank; }

    public boolean isSynthetic() { return isSynthetic; }
    public void setSynthetic(boolean synthetic) { isSynthetic = synthetic; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
