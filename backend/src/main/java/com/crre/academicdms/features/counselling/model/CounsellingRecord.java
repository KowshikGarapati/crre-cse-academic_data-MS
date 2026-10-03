package com.crre.academicdms.features.counselling.model;

import com.crre.academicdms.features.student.model.Student;
import com.crre.academicdms.features.user.model.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "counselling_records")
public class CounsellingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counsellor_user_id", nullable = false)
    private User counsellor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "meeting_date", nullable = false)
    private LocalDate meetingDate = LocalDate.now();

    @Column(name = "record_type", nullable = false, length = 50)
    private String recordType = "GENERAL"; // GENERAL, ACADEMIC, ATTENDANCE_WARNING, CAREER, FOLLOW_UP

    @Column(nullable = false, columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "action_plan", columnDefinition = "TEXT")
    private String actionPlan;

    @Column(name = "follow_up_date")
    private LocalDate followUpDate;

    @Column(nullable = false, length = 20)
    private String status = "OPEN"; // OPEN, RESOLVED, PENDING_FOLLOW_UP

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public CounsellingRecord() {}

    public CounsellingRecord(
        User counsellor,
        Student student,
        LocalDate meetingDate,
        String recordType,
        String remarks,
        String actionPlan,
        LocalDate followUpDate,
        String status
    ) {
        this.counsellor = counsellor;
        this.student = student;
        this.meetingDate = meetingDate;
        this.recordType = recordType;
        this.remarks = remarks;
        this.actionPlan = actionPlan;
        this.followUpDate = followUpDate;
        this.status = status != null ? status : "OPEN";
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getCounsellor() { return counsellor; }
    public void setCounsellor(User counsellor) { this.counsellor = counsellor; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public LocalDate getMeetingDate() { return meetingDate; }
    public void setMeetingDate(LocalDate meetingDate) { this.meetingDate = meetingDate; }
    public String getRecordType() { return recordType; }
    public void setRecordType(String recordType) { this.recordType = recordType; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public String getActionPlan() { return actionPlan; }
    public void setActionPlan(String actionPlan) { this.actionPlan = actionPlan; }
    public LocalDate getFollowUpDate() { return followUpDate; }
    public void setFollowUpDate(LocalDate followUpDate) { this.followUpDate = followUpDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
