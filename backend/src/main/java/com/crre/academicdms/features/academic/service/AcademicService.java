package com.crre.academicdms.features.academic.service;

import com.crre.academicdms.features.academic.dto.AcademicDtos.*;
import com.crre.academicdms.features.academic.model.*;
import com.crre.academicdms.features.academic.repository.*;
import com.crre.academicdms.features.responsibility.dto.ResponsibilitySummaryDto;
import com.crre.academicdms.features.responsibility.repository.ResponsibilityRepository;
import com.crre.academicdms.features.student.model.StudentEnrollment;
import com.crre.academicdms.features.student.repository.StudentEnrollmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AcademicService {

    private final DepartmentRepository departmentRepository;
    private final ProgramRepository programRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final TimetableRepository timetableRepository;
    private final ResponsibilityRepository responsibilityRepository;

    public AcademicService(
        DepartmentRepository departmentRepository,
        ProgramRepository programRepository,
        SectionRepository sectionRepository,
        SubjectRepository subjectRepository,
        StudentEnrollmentRepository studentEnrollmentRepository,
        TimetableRepository timetableRepository,
        ResponsibilityRepository responsibilityRepository
    ) {
        this.departmentRepository = departmentRepository;
        this.programRepository = programRepository;
        this.sectionRepository = sectionRepository;
        this.subjectRepository = subjectRepository;
        this.studentEnrollmentRepository = studentEnrollmentRepository;
        this.timetableRepository = timetableRepository;
        this.responsibilityRepository = responsibilityRepository;
    }

    @Transactional(readOnly = true)
    public AcademicMetadataDto getMetadata() {
        List<DepartmentDto> depts = departmentRepository.findAll().stream()
            .map(d -> new DepartmentDto(d.getId(), d.getCode(), d.getName()))
            .toList();

        List<ProgramDto> progs = programRepository.findAll().stream()
            .map(p -> new ProgramDto(p.getId(), p.getDepartment().getId(), p.getCode(), p.getName(), p.getDegree()))
            .toList();

        List<SectionDto> sects = sectionRepository.findAll().stream()
            .map(s -> new SectionDto(s.getId(), s.getSemester().getId(), s.getSectionNumber(), s.getSectionName()))
            .toList();

        List<SubjectDto> subjs = subjectRepository.findAll().stream()
            .map(s -> new SubjectDto(s.getId(), s.getProgram().getId(), s.getSemester().getId(), s.getCode(), s.getName(), s.getType(), s.getCredits()))
            .toList();

        return new AcademicMetadataDto(depts, progs, sects, subjs);
    }

    @Transactional(readOnly = true)
    public List<StudentDto> getStudentsForSection(UUID sectionId, com.crre.academicdms.security.UserPrincipal caller) {
        if (caller != null) {
            boolean authorized = caller.getRole() == com.crre.academicdms.features.user.model.Role.ADMIN
                || caller.getRole() == com.crre.academicdms.features.user.model.Role.PRINCIPAL
                || isHodOfSection(caller, sectionId)
                || isClassTeacherOfSection(caller, sectionId)
                || isTeachingSection(caller, sectionId)
                || isStudentOfSection(caller, sectionId);

            if (!authorized) {
                throw new org.springframework.security.access.AccessDeniedException("Access denied: You are not authorized to view students for this section");
            }
        }
        return getStudentsForSection(sectionId);
    }

    @Transactional(readOnly = true)
    public List<StudentDto> getStudentsForSection(UUID sectionId) {
        List<StudentEnrollment> enrollments = studentEnrollmentRepository
            .findBySectionIdAndCurrentTrueOrderByStudentRegistrationNumberAsc(sectionId);

        return enrollments.stream().map(e -> {
            var student = e.getStudent();
            var section = e.getSection();
            var semester = section != null ? section.getSemester() : null;
            var program = semester != null ? semester.getProgram() : null;

            return new StudentDto(
                student.getId(),
                student.getRegistrationNumber(),
                student.getFullName(),
                student.getEntryType(),
                student.getStatus(),
                section != null ? section.getId() : null,
                section != null ? section.getSectionName() : null,
                program != null ? program.getName() : null,
                semester != null ? semester.getTermName() : null
            );
        }).toList();
    }

    private boolean isHodOfSection(com.crre.academicdms.security.UserPrincipal caller, UUID sectionId) {
        if (caller.getRole() != com.crre.academicdms.features.user.model.Role.HOD) return false;
        Section sec = sectionRepository.findById(sectionId).orElse(null);
        if (sec == null) return false;
        UUID deptId = sec.getSemester().getProgram().getDepartment().getId();
        return responsibilityRepository.existsByUserIdAndTypeAndDepartmentIdAndStatus(
            caller.getId(), "HOD_DEPT", deptId, "ACTIVE"
        );
    }

    private boolean isClassTeacherOfSection(com.crre.academicdms.security.UserPrincipal caller, UUID sectionId) {
        return responsibilityRepository.existsByUserIdAndTypeAndSectionIdAndStatus(
            caller.getId(), "CLASS_TEACHER", sectionId, "ACTIVE"
        );
    }

    private boolean isTeachingSection(com.crre.academicdms.security.UserPrincipal caller, UUID sectionId) {
        return responsibilityRepository.existsByUserIdAndTypeAndSectionIdAndStatus(
            caller.getId(), "TEACHING", sectionId, "ACTIVE"
        );
    }

    private boolean isStudentOfSection(com.crre.academicdms.security.UserPrincipal caller, UUID sectionId) {
        if (caller.getRole() != com.crre.academicdms.features.user.model.Role.STUDENT) return false;
        return studentEnrollmentRepository.findBySectionId(sectionId).stream()
            .anyMatch(e -> e.getStudent().getUser() != null && e.getStudent().getUser().getId().equals(caller.getId()));
    }

    @Transactional(readOnly = true)
    public List<TimetableEntryDto> getTimetableForSection(UUID sectionId) {
        return timetableRepository.findBySectionIdOrderByDayOfWeekAscPeriodNumberAsc(sectionId)
            .stream()
            .sorted(com.crre.academicdms.features.academic.util.TimetableUtils.CHRONOLOGICAL_COMPARATOR)
            .map(t -> new TimetableEntryDto(
                t.getId(),
                t.getSection().getId(),
                t.getSection().getSectionName(),
                t.getSubject().getId(),
                t.getSubject().getCode(),
                t.getSubject().getName(),
                t.getFacultyUser() != null ? t.getFacultyUser().getFullName() : "TBA",
                t.getDayOfWeek(),
                t.getPeriodNumber(),
                t.getStartTime().toString(),
                t.getEndTime().toString()
            )).toList();
    }

    @Transactional(readOnly = true)
    public List<ResponsibilitySummaryDto> getFacultyTeaching(UUID facultyUserId) {
        return responsibilityRepository.findByUserIdAndStatus(facultyUserId, "ACTIVE").stream()
            .filter(r -> "TEACHING".equalsIgnoreCase(r.getType()))
            .map(ResponsibilitySummaryDto::fromEntity)
            .toList();
    }
}
