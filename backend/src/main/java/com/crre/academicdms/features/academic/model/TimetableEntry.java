package com.crre.academicdms.features.academic.model;

import com.crre.academicdms.features.responsibility.model.Responsibility;
import com.crre.academicdms.features.user.model.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "timetables", uniqueConstraints = {
    @UniqueConstraint(name = "uk_timetable_section_day_period", columnNames = {"section_id", "day_of_week", "period_number"}),
    @UniqueConstraint(name = "uk_timetable_faculty_day_period", columnNames = {"faculty_user_id", "day_of_week", "period_number"})
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teaching_assignment_id")
    private Responsibility teachingAssignment;

    @Column(name = "day_of_week", nullable = false, length = 20)
    private String dayOfWeek; // MONDAY, TUESDAY, etc.

    @Column(name = "period_number", nullable = false)
    private int periodNumber;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "room_number", length = 50)
    private String roomNumber = "LH-301";

    @Column(name = "session_type", length = 20, nullable = false)
    private String sessionType = "THEORY"; // THEORY, LAB

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public TimetableEntry() {}

    public TimetableEntry(Section section, Subject subject, User facultyUser, String dayOfWeek, int periodNumber, LocalTime startTime, LocalTime endTime, String roomNumber) {
        this.section = section;
        this.subject = subject;
        this.facultyUser = facultyUser;
        this.dayOfWeek = dayOfWeek;
        this.periodNumber = periodNumber;
        this.startTime = startTime;
        this.endTime = endTime;
        this.roomNumber = roomNumber;
        this.sessionType = "THEORY";
    }

    public TimetableEntry(Section section, Subject subject, User facultyUser, Responsibility teachingAssignment, String dayOfWeek, int periodNumber, LocalTime startTime, LocalTime endTime, String roomNumber, String sessionType) {
        this.section = section;
        this.subject = subject;
        this.facultyUser = facultyUser;
        this.teachingAssignment = teachingAssignment;
        this.dayOfWeek = dayOfWeek;
        this.periodNumber = periodNumber;
        this.startTime = startTime;
        this.endTime = endTime;
        this.roomNumber = roomNumber;
        this.sessionType = (sessionType != null) ? sessionType : "THEORY";
    }

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
    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public Responsibility getTeachingAssignment() { return teachingAssignment; }
    public void setTeachingAssignment(Responsibility teachingAssignment) { this.teachingAssignment = teachingAssignment; }
    public String getSessionType() { return sessionType; }
    public void setSessionType(String sessionType) { this.sessionType = sessionType; }
    public Instant getCreatedAt() { return createdAt; }
}
