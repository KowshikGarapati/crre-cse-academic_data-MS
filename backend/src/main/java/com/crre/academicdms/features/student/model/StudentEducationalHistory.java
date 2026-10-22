package com.crre.academicdms.features.student.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "student_educational_history")
public class StudentEducationalHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "qualification_type", nullable = false, length = 50)
    private String qualificationType; // SSC, INTERMEDIATE, DIPLOMA, OTHER

    @Column(name = "board_or_university", nullable = false, length = 150)
    private String boardOrUniversity;

    @Column(name = "institution_name", nullable = false, length = 255)
    private String institutionName;

    @Column(name = "hall_ticket_number", length = 50)
    private String hallTicketNumber;

    @Column(name = "completion_year", nullable = false)
    private int completionYear;

    @Column(length = 100)
    private String stream;

    @Column(length = 100)
    private String branch;

    @Column(name = "maximum_marks", precision = 7, scale = 2)
    private BigDecimal maximumMarks;

    @Column(name = "marks_obtained", precision = 7, scale = 2)
    private BigDecimal marksObtained;

    @Column(precision = 5, scale = 2)
    private BigDecimal percentage;

    @Column(name = "additional_details", columnDefinition = "TEXT")
    private String additionalDetails;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public StudentEducationalHistory() {}

    public StudentEducationalHistory(
        Student student,
        String qualificationType,
        String boardOrUniversity,
        String institutionName,
        String hallTicketNumber,
        int completionYear,
        String stream,
        String branch,
        BigDecimal maximumMarks,
        BigDecimal marksObtained,
        BigDecimal percentage,
        String additionalDetails
    ) {
        this.student = student;
        this.qualificationType = qualificationType;
        this.boardOrUniversity = boardOrUniversity;
        this.institutionName = institutionName;
        this.hallTicketNumber = hallTicketNumber;
        this.completionYear = completionYear;
        this.stream = stream;
        this.branch = branch;
        this.maximumMarks = maximumMarks;
        this.marksObtained = marksObtained;
        this.percentage = percentage;
        this.additionalDetails = additionalDetails;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public String getQualificationType() { return qualificationType; }
    public void setQualificationType(String qualificationType) { this.qualificationType = qualificationType; }
    public String getBoardOrUniversity() { return boardOrUniversity; }
    public void setBoardOrUniversity(String boardOrUniversity) { this.boardOrUniversity = boardOrUniversity; }
    public String getInstitutionName() { return institutionName; }
    public void setInstitutionName(String institutionName) { this.institutionName = institutionName; }
    public String getHallTicketNumber() { return hallTicketNumber; }
    public void setHallTicketNumber(String hallTicketNumber) { this.hallTicketNumber = hallTicketNumber; }
    public int getCompletionYear() { return completionYear; }
    public void setCompletionYear(int completionYear) { this.completionYear = completionYear; }
    public String getStream() { return stream; }
    public void setStream(String stream) { this.stream = stream; }
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
    public BigDecimal getMaximumMarks() { return maximumMarks; }
    public void setMaximumMarks(BigDecimal maximumMarks) { this.maximumMarks = maximumMarks; }
    public BigDecimal getMarksObtained() { return marksObtained; }
    public void setMarksObtained(BigDecimal marksObtained) { this.marksObtained = marksObtained; }
    public BigDecimal getPercentage() { return percentage; }
    public void setPercentage(BigDecimal percentage) { this.percentage = percentage; }
    public String getAdditionalDetails() { return additionalDetails; }
    public void setAdditionalDetails(String additionalDetails) { this.additionalDetails = additionalDetails; }
    public Instant getCreatedAt() { return createdAt; }
}
