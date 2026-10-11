package com.crre.academicdms.features.academic.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import com.crre.academicdms.features.academic.model.TimetableEntry;

public class AcademicDtos {

    public record DepartmentDto(UUID id, String code, String name) {}

    public record ProgramDto(UUID id, UUID departmentId, String code, String name, String degree) {}

    public record SectionDto(
        UUID id,
        UUID semesterId,
        int sectionNumber,
        String sectionName,
        String programCode,
        String programName
    ) {
        public SectionDto(UUID id, UUID semesterId, int sectionNumber, String sectionName) {
            this(id, semesterId, sectionNumber, sectionName, null, null);
        }
    }

    public record SubjectDto(UUID id, UUID programId, UUID semesterId, String code, String name, String type, BigDecimal credits) {}

    public record SemesterDto(UUID id, UUID programId, int semesterNumber, String termName, boolean current) {}

    public record AcademicMetadataDto(
        List<DepartmentDto> departments,
        List<ProgramDto> programs,
        List<SemesterDto> semesters,
        List<SectionDto> sections,
        List<SubjectDto> subjects
    ) {
        public AcademicMetadataDto(
            List<DepartmentDto> departments,
            List<ProgramDto> programs,
            List<SectionDto> sections,
            List<SubjectDto> subjects
        ) {
            this(departments, programs, List.of(), sections, subjects);
        }
    }

    public record StudentDto(
        UUID id,
        String registrationNumber,
        String fullName,
        String entryType,
        String status,
        UUID sectionId,
        String sectionName,
        String programName,
        String semesterName
    ) {}

    public record TimetableEntryDto(
        UUID id,
        UUID sectionId,
        String sectionName,
        UUID subjectId,
        String subjectCode,
        String subjectName,
        String facultyName,
        String dayOfWeek,
        int periodNumber,
        String startTime,
        String endTime,
        String roomNumber,
        String sessionType
    ) {
        public static TimetableEntryDto fromEntity(TimetableEntry t) {
            return new TimetableEntryDto(
                t.getId(),
                t.getSection() != null ? t.getSection().getId() : null,
                t.getSection() != null ? t.getSection().getSectionName() : null,
                t.getSubject() != null ? t.getSubject().getId() : null,
                t.getSubject() != null ? t.getSubject().getCode() : null,
                t.getSubject() != null ? t.getSubject().getName() : null,
                t.getFacultyUser() != null ? t.getFacultyUser().getFullName() : "TBA",
                t.getDayOfWeek(),
                t.getPeriodNumber(),
                t.getStartTime() != null ? t.getStartTime().toString() : "",
                t.getEndTime() != null ? t.getEndTime().toString() : "",
                t.getRoomNumber() != null ? t.getRoomNumber() : "Room TBA",
                t.getSessionType() != null ? t.getSessionType() : "LECTURE"
            );
        }
    }

    public record PeriodDefinitionDto(
        int periodNumber,
        String startTime,
        String endTime,
        String timeRange,
        String label
    ) {}

    public record EnrolledSubjectFacultyDto(
        UUID subjectId,
        String subjectCode,
        String subjectName,
        String subjectType,
        BigDecimal credits,
        UUID facultyId,
        String facultyName,
        String facultyEmail
    ) {}

    public record SectionDetailOverviewDto(
        UUID sectionId,
        String sectionName,
        int sectionNumber,
        UUID programId,
        String programCode,
        String programName,
        String degree,
        UUID semesterId,
        String semesterName,
        int semesterNumber,
        String academicYearName,
        UUID classTeacherId,
        String classTeacherName,
        String classTeacherEmail,
        long totalStudents,
        long regularStudents,
        long lateralStudents,
        List<EnrolledSubjectFacultyDto> subjects,
        List<TimetableEntryDto> timetable,
        List<StudentDto> studentRoster
    ) {
        @com.fasterxml.jackson.annotation.JsonProperty("students")
        public List<StudentDto> students() {
            return studentRoster();
        }
    }
}
