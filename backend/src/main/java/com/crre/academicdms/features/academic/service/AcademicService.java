package com.crre.academicdms.features.academic.service;

import com.crre.academicdms.features.academic.dto.AcademicDtos.*;
import com.crre.academicdms.features.academic.model.*;
import com.crre.academicdms.features.academic.repository.*;
import com.crre.academicdms.features.responsibility.dto.ResponsibilitySummaryDto;
import com.crre.academicdms.features.responsibility.model.Responsibility;
import com.crre.academicdms.features.responsibility.repository.ResponsibilityRepository;
import com.crre.academicdms.features.student.model.Student;
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
    private final SemesterRepository semesterRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final TimetableRepository timetableRepository;
    private final ResponsibilityRepository responsibilityRepository;

    public AcademicService(
        DepartmentRepository departmentRepository,
        ProgramRepository programRepository,
        SemesterRepository semesterRepository,
        SectionRepository sectionRepository,
        SubjectRepository subjectRepository,
        StudentEnrollmentRepository studentEnrollmentRepository,
        TimetableRepository timetableRepository,
        ResponsibilityRepository responsibilityRepository
    ) {
        this.departmentRepository = departmentRepository;
        this.programRepository = programRepository;
        this.semesterRepository = semesterRepository;
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

        List<SemesterDto> sems = semesterRepository.findAll().stream()
            .map(s -> new SemesterDto(s.getId(), s.getProgram().getId(), s.getSemesterNumber(), s.getTermName(), s.isCurrent()))
            .toList();

        List<SectionDto> sects = sectionRepository.findAll().stream()
            .map(s -> new SectionDto(
                s.getId(),
                s.getSemester().getId(),
                s.getSectionNumber(),
                s.getSectionName(),
                s.getSemester().getProgram() != null ? s.getSemester().getProgram().getCode() : null,
                s.getSemester().getProgram() != null ? s.getSemester().getProgram().getName() : null
            ))
            .toList();

        List<SubjectDto> subjs = subjectRepository.findAll().stream()
            .map(s -> new SubjectDto(s.getId(), s.getProgram().getId(), s.getSemester().getId(), s.getCode(), s.getName(), s.getType(), s.getCredits()))
            .toList();

        return new AcademicMetadataDto(depts, progs, sems, sects, subjs);
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
        if (!caller.isHod()) return false;
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
            .map(TimetableEntryDto::fromEntity)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ResponsibilitySummaryDto> getFacultyTeaching(UUID facultyUserId) {
        return responsibilityRepository.findByUserIdAndStatus(facultyUserId, "ACTIVE").stream()
            .filter(r -> "TEACHING".equalsIgnoreCase(r.getType()))
            .map(ResponsibilitySummaryDto::fromEntity)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PeriodDefinitionDto> getAcademicPeriods() {
        return java.util.Arrays.stream(AcademicPeriod.values())
            .map(p -> new PeriodDefinitionDto(
                p.getPeriodNumber(),
                p.getStartTime().toString(),
                p.getEndTime().toString(),
                p.getTimeRange(),
                p.getLabel()
            ))
            .toList();
    }

    @Transactional(readOnly = true)
    public SectionDetailOverviewDto getSectionDetailOverview(UUID sectionId) {
        Section sec = sectionRepository.findById(sectionId)
            .orElseThrow(() -> new IllegalArgumentException("Section not found: " + sectionId));

        Semester sem = sec.getSemester();
        Program prog = sem != null ? sem.getProgram() : null;
        AcademicYear ay = sem != null ? sem.getAcademicYear() : null;

        // Class teacher
        List<Responsibility> ctResps = responsibilityRepository.findByTypeAndStatus("CLASS_TEACHER", "ACTIVE").stream()
            .filter(r -> r.getSection() != null && r.getSection().getId().equals(sectionId))
            .toList();

        UUID classTeacherId = null;
        String classTeacherName = "Unassigned";
        String classTeacherEmail = null;
        if (!ctResps.isEmpty()) {
            com.crre.academicdms.features.user.model.User ctUser = ctResps.get(0).getUser();
            classTeacherId = ctUser.getId();
            classTeacherName = ctUser.getFullName();
            classTeacherEmail = ctUser.getEmail();
        }

        // Enrollments
        List<StudentEnrollment> enrollments = studentEnrollmentRepository.findBySectionId(sectionId);
        long totalStudents = enrollments.size();
        long regularStudents = enrollments.stream().filter(e -> "REGULAR".equalsIgnoreCase(e.getStudent().getEntryType())).count();
        long lateralStudents = enrollments.stream().filter(e -> "LATERAL".equalsIgnoreCase(e.getStudent().getEntryType())).count();

        // Student roster
        List<StudentDto> roster = enrollments.stream()
            .map(e -> {
                Student s = e.getStudent();
                return new StudentDto(
                    s.getId(),
                    s.getRegistrationNumber(),
                    s.getFullName(),
                    s.getEntryType(),
                    s.getStatus(),
                    sec.getId(),
                    sec.getSectionName(),
                    prog != null ? prog.getName() : "",
                    sem != null ? sem.getTermName() : ""
                );
            })
            .sorted(java.util.Comparator.comparing(StudentDto::registrationNumber))
            .toList();

        // Timetable
        List<TimetableEntryDto> timetable = getTimetableForSection(sectionId);

        // Subjects & Faculty
        List<Responsibility> teachingResps = responsibilityRepository.findByTypeAndStatus("TEACHING", "ACTIVE").stream()
            .filter(r -> r.getSection() != null && r.getSection().getId().equals(sectionId) && r.getSubject() != null)
            .toList();

        java.util.Map<UUID, EnrolledSubjectFacultyDto> subjectMap = new java.util.LinkedHashMap<>();
        for (Responsibility r : teachingResps) {
            Subject sub = r.getSubject();
            com.crre.academicdms.features.user.model.User fac = r.getUser();
            subjectMap.put(sub.getId(), new EnrolledSubjectFacultyDto(
                sub.getId(),
                sub.getCode(),
                sub.getName(),
                sub.getType(),
                sub.getCredits(),
                fac != null ? fac.getId() : null,
                fac != null ? fac.getFullName() : "TBA",
                fac != null ? fac.getEmail() : null
            ));
        }

        if (prog != null && sem != null) {
            List<Subject> allSemSubjects = subjectRepository.findByProgramIdAndSemesterId(prog.getId(), sem.getId());
            for (Subject sub : allSemSubjects) {
                if (!subjectMap.containsKey(sub.getId())) {
                    subjectMap.put(sub.getId(), new EnrolledSubjectFacultyDto(
                        sub.getId(),
                        sub.getCode(),
                        sub.getName(),
                        sub.getType(),
                        sub.getCredits(),
                        null,
                        "TBA",
                        null
                    ));
                }
            }
        }

        return new SectionDetailOverviewDto(
            sec.getId(),
            sec.getSectionName(),
            sec.getSectionNumber(),
            prog != null ? prog.getId() : null,
            prog != null ? prog.getCode() : null,
            prog != null ? prog.getName() : null,
            prog != null ? prog.getDegree() : null,
            sem != null ? sem.getId() : null,
            sem != null ? sem.getTermName() : null,
            sem != null ? sem.getSemesterNumber() : 0,
            ay != null ? ay.getYearName() : null,
            classTeacherId,
            classTeacherName,
            classTeacherEmail,
            totalStudents,
            regularStudents,
            lateralStudents,
            new java.util.ArrayList<>(subjectMap.values()),
            timetable,
            roster
        );
    }
}
