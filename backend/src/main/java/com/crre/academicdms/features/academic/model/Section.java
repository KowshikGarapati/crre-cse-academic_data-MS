package com.crre.academicdms.features.academic.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sections", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"semester_id", "section_number"})
})
public class Section {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    @Column(name = "section_number", nullable = false)
    private int sectionNumber;

    @Column(name = "section_name", nullable = false, length = 50)
    private String sectionName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Section() {}

    public Section(Semester semester, int sectionNumber, String sectionName) {
        this.semester = semester;
        this.sectionNumber = sectionNumber;
        this.sectionName = sectionName;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Semester getSemester() { return semester; }
    public void setSemester(Semester semester) { this.semester = semester; }
    public int getSectionNumber() { return sectionNumber; }
    public void setSectionNumber(int sectionNumber) { this.sectionNumber = sectionNumber; }
    public String getSectionName() { return sectionName; }
    public void setSectionName(String sectionName) { this.sectionName = sectionName; }
    public Instant getCreatedAt() { return createdAt; }
}
