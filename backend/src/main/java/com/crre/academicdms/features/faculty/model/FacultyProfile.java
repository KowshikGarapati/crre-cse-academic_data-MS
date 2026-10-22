package com.crre.academicdms.features.faculty.model;

import com.crre.academicdms.features.academic.model.Department;
import com.crre.academicdms.features.user.model.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "faculty_profiles")
public class FacultyProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true, unique = true)
    private User user;

    @Column(name = "faculty_name", length = 150)
    private String facultyName;

    @Column(name = "employee_number", nullable = false, unique = true, length = 50)
    private String employeeNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(nullable = false, length = 100)
    private String designation;

    @Column(nullable = false, length = 100)
    private String qualification;

    @Column(length = 150)
    private String specialization;

    @Column(name = "professional_experience", length = 255)
    private String professionalExperience;

    @Column(name = "institutional_contact", length = 100)
    private String institutionalContact;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "is_synthetic", nullable = false)
    private boolean isSynthetic = false;

    @Column(name = "active_status", nullable = false)
    private boolean activeStatus = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public FacultyProfile() {}

    public FacultyProfile(
        User user,
        String employeeNumber,
        Department department,
        String designation,
        String qualification,
        String specialization,
        String professionalExperience,
        String institutionalContact
    ) {
        this.user = user;
        this.facultyName = user != null ? user.getFullName() : null;
        this.employeeNumber = employeeNumber;
        this.department = department;
        this.designation = designation;
        this.qualification = qualification;
        this.specialization = specialization;
        this.professionalExperience = professionalExperience;
        this.institutionalContact = institutionalContact;
        this.activeStatus = true;
        this.isSynthetic = false;
    }

    public FacultyProfile(
        String facultyName,
        String employeeNumber,
        Department department,
        String designation,
        String qualification,
        String specialization,
        String sourceUrl
    ) {
        this.user = null;
        this.facultyName = facultyName;
        this.employeeNumber = employeeNumber;
        this.department = department;
        this.designation = designation;
        this.qualification = qualification;
        this.specialization = specialization;
        this.sourceUrl = sourceUrl;
        this.activeStatus = true;
        this.isSynthetic = false;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) {
        this.user = user;
        if (user != null && (this.facultyName == null || this.facultyName.isBlank())) {
            this.facultyName = user.getFullName();
        }
    }
    public String getFacultyName() {
        return (facultyName != null && !facultyName.isBlank())
            ? facultyName
            : (user != null ? user.getFullName() : null);
    }
    public void setFacultyName(String facultyName) { this.facultyName = facultyName; }
    public String getEmployeeNumber() { return employeeNumber; }
    public void setEmployeeNumber(String employeeNumber) { this.employeeNumber = employeeNumber; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = qualification; }
    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }
    public String getProfessionalExperience() { return professionalExperience; }
    public void setProfessionalExperience(String professionalExperience) { this.professionalExperience = professionalExperience; }
    public String getInstitutionalContact() { return institutionalContact; }
    public void setInstitutionalContact(String institutionalContact) { this.institutionalContact = institutionalContact; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public boolean isSynthetic() { return isSynthetic; }
    public void setSynthetic(boolean synthetic) { isSynthetic = synthetic; }
    public boolean isActiveStatus() { return activeStatus; }
    public void setActiveStatus(boolean activeStatus) { this.activeStatus = activeStatus; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
