package com.crre.academicdms.features.academic.model;

import com.crre.academicdms.features.user.model.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "timetables", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"section_id", "day_of_week", "period_number"})
})
public class TimetableEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_user_id")
    private User facultyUser;

    @Column(name = "day_of_week", nullable = false, length = 20)
    private String dayOfWeek; // MONDAY, TUESDAY, etc.

    @Column(name = "period_number", nullable = false)
    private int periodNumber;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public TimetableEntry() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Section getSection() { return section; }
    public void setSection(Section section) { this.section = section; }
    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }
    public User getFacultyUser() { return facultyUser; }
    public void setFacultyUser(User facultyUser) { this.facultyUser = facultyUser; }
    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public int getPeriodNumber() { return periodNumber; }
    public void setPeriodNumber(int periodNumber) { this.periodNumber = periodNumber; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public Instant getCreatedAt() { return createdAt; }
}
