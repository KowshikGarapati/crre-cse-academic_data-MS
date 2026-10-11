package com.crre.academicdms.features.faculty.service;

import com.crre.academicdms.features.academic.dto.AcademicDtos.StudentDto;
import com.crre.academicdms.features.academic.model.Section;
import com.crre.academicdms.features.academic.model.Subject;
import com.crre.academicdms.features.academic.model.TimetableEntry;
import com.crre.academicdms.features.academic.repository.SectionRepository;
import com.crre.academicdms.features.academic.repository.SubjectRepository;
import com.crre.academicdms.features.academic.repository.TimetableRepository;
import com.crre.academicdms.features.counselling.model.CounsellingAssignment;
import com.crre.academicdms.features.counselling.model.CounsellingRecord;
import com.crre.academicdms.features.counselling.repository.CounsellingAssignmentRepository;
import com.crre.academicdms.features.counselling.repository.CounsellingRecordRepository;
import com.crre.academicdms.features.faculty.dto.FacultyDtos.*;
import com.crre.academicdms.features.faculty.model.FacultyProfile;
import com.crre.academicdms.features.faculty.repository.FacultyProfileRepository;
import com.crre.academicdms.features.responsibility.model.Responsibility;
import com.crre.academicdms.features.responsibility.repository.ResponsibilityRepository;
import com.crre.academicdms.features.student.model.Student;
import com.crre.academicdms.features.student.model.StudentEnrollment;
import com.crre.academicdms.features.student.repository.StudentEnrollmentRepository;
import com.crre.academicdms.features.student.repository.StudentRepository;
import com.crre.academicdms.features.user.model.Role;
import com.crre.academicdms.security.UserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class FacultyService {

    private final ResponsibilityRepository responsibilityRepository;
    private final TimetableRepository timetableRepository;
    private final SectionRepository sectionRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final CounsellingAssignmentRepository counsellingAssignmentRepository;
    private final CounsellingRecordRepository counsellingRecordRepository;
    private final FacultyProfileRepository facultyProfileRepository;

    public FacultyService(
        ResponsibilityRepository responsibilityRepository,
        TimetableRepository timetableRepository,
        SectionRepository sectionRepository,
        StudentEnrollmentRepository enrollmentRepository,
        StudentRepository studentRepository,
        CounsellingAssignmentRepository counsellingAssignmentRepository,
        CounsellingRecordRepository counsellingRecordRepository,
        FacultyProfileRepository facultyProfileRepository
    ) {
        this.responsibilityRepository = responsibilityRepository;
        this.timetableRepository = timetableRepository;
        this.sectionRepository = sectionRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.studentRepository = studentRepository;
        this.counsellingAssignmentRepository = counsellingAssignmentRepository;
        this.counsellingRecordRepository = counsellingRecordRepository;
        this.facultyProfileRepository = facultyProfileRepository;
    }

    @Transactional(readOnly = true)
    public List<MySubjectDto> getMySubjects(UserPrincipal caller) {
        List<Responsibility> teaching = responsibilityRepository.findByUserIdAndTypeAndStatus(
            caller.getId(), "TEACHING", "ACTIVE"
        );

        return teaching.stream()
            .filter(r -> r.getSubject() != null && r.getSection() != null)
            .map(r -> new MySubjectDto(
                r.getSubject().getId(),
                r.getSubject().getCode(),
                r.getSubject().getName(),
                r.getSubject().getType(),
                r.getSubject().getCredits(),
                r.getSection().getId(),
                r.getSection().getSectionName(),
                r.getSection().getSemester().getProgram().getName(),
                r.getSection().getSemester().getTermName()
            ))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<MyClassDto> getMyClasses(UserPrincipal caller) {
        List<Responsibility> teaching = responsibilityRepository.findByUserIdAndTypeAndStatus(
            caller.getId(), "TEACHING", "ACTIVE"
        );

        Map<UUID, MyClassDto> classMap = new LinkedHashMap<>();
        for (Responsibility r : teaching) {
            if (r.getSection() != null && r.getSubject() != null) {
                Section sec = r.getSection();
                Subject sub = r.getSubject();
                long count = enrollmentRepository.findBySectionId(sec.getId()).size();
                classMap.put(sec.getId(), new MyClassDto(
                    sec.getId(),
                    sec.getSectionName(),
                    sec.getSemester().getProgram().getName(),
                    sec.getSemester().getTermName(),
                    sub.getCode(),
                    sub.getName(),
                    count
                ));
            }
        }
        return new ArrayList<>(classMap.values());
    }

    @Transactional(readOnly = true)
    public List<FacultyTimetableEntryDto> getMyTimetable(UserPrincipal caller) {
        List<TimetableEntry> entries = timetableRepository.findAll().stream()
            .filter(e -> e.getFacultyUser() != null && e.getFacultyUser().getId().equals(caller.getId()))
            .sorted(com.crre.academicdms.features.academic.util.TimetableUtils.CHRONOLOGICAL_COMPARATOR)
            .toList();

        return entries.stream()
            .map(this::mapToFacultyTimetableEntryDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<FacultyTimetableEntryDto> getTodayClasses(String targetDay, UserPrincipal caller) {
        String day = (targetDay != null && !targetDay.trim().isEmpty())
            ? targetDay.trim().toUpperCase()
            : java.time.LocalDate.now().getDayOfWeek().name();

        List<TimetableEntry> entries = timetableRepository.findAll().stream()
            .filter(e -> e.getFacultyUser() != null && e.getFacultyUser().getId().equals(caller.getId()))
            .filter(e -> e.getDayOfWeek() != null && e.getDayOfWeek().trim().equalsIgnoreCase(day))
            .sorted(Comparator.comparing((TimetableEntry e) -> e.getStartTime() != null ? e.getStartTime() : java.time.LocalTime.MIN)
                .thenComparingInt(TimetableEntry::getPeriodNumber))
            .toList();

        return entries.stream()
            .map(this::mapToFacultyTimetableEntryDto)
            .toList();
    }

    private FacultyTimetableEntryDto mapToFacultyTimetableEntryDto(TimetableEntry e) {
        String programName = (e.getSection() != null && e.getSection().getSemester() != null && e.getSection().getSemester().getProgram() != null)
            ? e.getSection().getSemester().getProgram().getName()
            : "B.Tech CSE";
        String termName = (e.getSection() != null && e.getSection().getSemester() != null)
            ? e.getSection().getSemester().getTermName()
            : "Semester 3";
        String sectionName = e.getSection() != null ? e.getSection().getSectionName() : "Section 1";
        String subjectCode = e.getSubject() != null ? e.getSubject().getCode() : "";
        String subjectName = e.getSubject() != null ? e.getSubject().getName() : "";

        return new FacultyTimetableEntryDto(
            e.getId(),
            e.getDayOfWeek() != null ? e.getDayOfWeek().toUpperCase() : "MONDAY",
            e.getPeriodNumber(),
            e.getStartTime(),
            e.getEndTime(),
            subjectCode,
            subjectName,
            sectionName,
            programName,
            termName,
            e.getRoomNumber() != null ? e.getRoomNumber() : "LH-301"
        );
    }

    @Transactional(readOnly = true)
    public MySectionOverviewDto getMySectionOverview(UserPrincipal caller) {
        List<Responsibility> classTeacherResps = responsibilityRepository.findByUserIdAndTypeAndStatus(
            caller.getId(), "CLASS_TEACHER", "ACTIVE"
        );

        if (classTeacherResps.isEmpty() || classTeacherResps.get(0).getSection() == null) {
            return null;
        }

        Section sec = classTeacherResps.get(0).getSection();
        List<StudentEnrollment> enrollments = enrollmentRepository.findBySectionId(sec.getId());
        long total = enrollments.size();
        long regular = enrollments.stream().filter(e -> "REGULAR".equalsIgnoreCase(e.getStudent().getEntryType())).count();
        long lateral = enrollments.stream().filter(e -> "LATERAL".equalsIgnoreCase(e.getStudent().getEntryType())).count();

        return new MySectionOverviewDto(
            sec.getId(),
            sec.getSectionName(),
            sec.getSemester().getProgram().getName(),
            sec.getSemester().getTermName(),
            sec.getSemester().getAcademicYear().getYearName(),
            caller.getFullName(),
            total,
            regular,
            lateral
        );
    }

    @Transactional(readOnly = true)
    public List<StudentDto> getMySectionStudents(UserPrincipal caller) {
        List<Responsibility> classTeacherResps = responsibilityRepository.findByUserIdAndTypeAndStatus(
            caller.getId(), "CLASS_TEACHER", "ACTIVE"
        );

        if (classTeacherResps.isEmpty() || classTeacherResps.get(0).getSection() == null) {
            throw new AccessDeniedException("User is not the designated Class Teacher for any section");
        }

        UUID sectionId = classTeacherResps.get(0).getSection().getId();
        return getSectionStudentsInternal(sectionId);
    }

    @Transactional(readOnly = true)
    public List<StudentDto> getStudentsForClass(UUID sectionId, UserPrincipal caller) {
        // Enforce Scope:
        // Caller must be ADMIN, PRINCIPAL, HOD of department, Class Teacher of section, or Teaching a subject in this section
        boolean authorized = caller.getRole() == Role.ADMIN
            || caller.getRole() == Role.PRINCIPAL
            || isHodOfSection(caller, sectionId)
            || isClassTeacherOfSection(caller, sectionId)
            || isTeachingSection(caller, sectionId);

        if (!authorized) {
            throw new AccessDeniedException("Access denied: You are not authorized to view students for this section");
        }

        return getSectionStudentsInternal(sectionId);
    }

    @Transactional(readOnly = true)
    public List<SectionStudentsGroupDto> getMyStudentsGrouped(UserPrincipal caller) {
        List<Responsibility> teaching = responsibilityRepository.findByUserIdAndTypeAndStatus(
            caller.getId(), "TEACHING", "ACTIVE"
        );

        List<SectionStudentsGroupDto> groups = new ArrayList<>();
        Set<String> processedKeys = new HashSet<>();

        for (Responsibility r : teaching) {
            if (r.getSection() != null && r.getSubject() != null) {
                Section sec = r.getSection();
                Subject sub = r.getSubject();
                String key = sec.getId() + "_" + sub.getId();
                if (!processedKeys.add(key)) continue;

                com.crre.academicdms.features.academic.model.Semester sem = sec.getSemester();
                com.crre.academicdms.features.academic.model.Program prog = sem != null ? sem.getProgram() : null;

                List<StudentEnrollment> enrollments = enrollmentRepository.findBySectionId(sec.getId());
                List<StudentDto> students = enrollments.stream()
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
                    .sorted(Comparator.comparing(StudentDto::registrationNumber))
                    .toList();

                groups.add(new SectionStudentsGroupDto(
                    sec.getId(),
                    sec.getSectionName(),
                    sub.getId(),
                    sub.getCode(),
                    sub.getName(),
                    prog != null ? prog.getId() : null,
                    prog != null ? prog.getCode() : "",
                    prog != null ? prog.getName() : "",
                    sem != null ? sem.getId() : null,
                    sem != null ? sem.getTermName() : "",
                    students.size(),
                    students
                ));
            }
        }
        return groups;
    }

    @Transactional(readOnly = true)
    public List<CounsellingStudentDto> getMyCounsellingStudents(UserPrincipal caller) {
        List<CounsellingAssignment> assignments = counsellingAssignmentRepository.findByCounsellorIdAndStatus(
            caller.getId(), "ACTIVE"
        );

        return assignments.stream()
            .sorted(Comparator.comparing(a -> a.getStudent().getRegistrationNumber()))
            .map(a -> {
                Student s = a.getStudent();
                List<StudentEnrollment> enrollments = enrollmentRepository.findByStudentId(s.getId());
                Section sec = enrollments.isEmpty() ? null : enrollments.get(0).getSection();
                List<CounsellingRecord> records = counsellingRecordRepository.findByCounsellorIdAndStudentIdOrderByMeetingDateDesc(
                    caller.getId(), s.getId()
                );
                CounsellingRecord lastRecord = records.isEmpty() ? null : records.get(0);

                return new CounsellingStudentDto(
                    s.getId(),
                    s.getRegistrationNumber(),
                    s.getFullName(),
                    s.getEntryType(),
                    sec != null ? sec.getSectionName() : "N/A",
                    sec != null ? sec.getSemester().getProgram().getName() : "N/A",
                    sec != null ? sec.getSemester().getTermName() : "N/A",
                    records.size(),
                    lastRecord != null ? lastRecord.getMeetingDate() : null,
                    lastRecord != null ? lastRecord.getStatus() : "NO_SESSIONS"
                );
            })
            .toList();
    }

    @Transactional(readOnly = true)
    public List<CounsellingRecordDto> getMyCounsellingRecords(UserPrincipal caller) {
        List<CounsellingRecord> records = counsellingRecordRepository.findByCounsellorIdOrderByMeetingDateDesc(caller.getId());
        return records.stream()
            .map(r -> new CounsellingRecordDto(
                r.getId(),
                r.getStudent().getId(),
                r.getStudent().getRegistrationNumber(),
                r.getStudent().getFullName(),
                r.getMeetingDate(),
                r.getRecordType(),
                r.getRemarks(),
                r.getActionPlan(),
                r.getFollowUpDate(),
                r.getStatus()
            ))
            .toList();
    }

    @Transactional
    public CounsellingRecordDto createCounsellingRecord(CreateCounsellingRecordRequest req, UserPrincipal caller) {
        boolean assigned = counsellingAssignmentRepository.existsByCounsellorIdAndStudentIdAndStatus(
            caller.getId(), req.studentId(), "ACTIVE"
        );

        if (!assigned && caller.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Access denied: You are not assigned as counsellor for this student");
        }

        Student student = studentRepository.findById(req.studentId())
            .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        CounsellingRecord record = new CounsellingRecord(
            caller.getUser(),
            student,
            req.meetingDate() != null ? req.meetingDate() : java.time.LocalDate.now(),
            req.recordType() != null ? req.recordType() : "GENERAL",
            req.remarks(),
            req.actionPlan(),
            req.followUpDate(),
            req.status() != null ? req.status() : "OPEN"
        );

        CounsellingRecord saved = counsellingRecordRepository.save(record);

        return new CounsellingRecordDto(
            saved.getId(),
            student.getId(),
            student.getRegistrationNumber(),
            student.getFullName(),
            saved.getMeetingDate(),
            saved.getRecordType(),
            saved.getRemarks(),
            saved.getActionPlan(),
            saved.getFollowUpDate(),
            saved.getStatus()
        );
    }

    private List<StudentDto> getSectionStudentsInternal(UUID sectionId) {
        List<StudentEnrollment> enrollments = enrollmentRepository.findBySectionId(sectionId);
        return enrollments.stream()
            .map(e -> {
                Student s = e.getStudent();
                Section sec = e.getSection();
                return new StudentDto(
                    s.getId(),
                    s.getRegistrationNumber(),
                    s.getFullName(),
                    s.getEntryType(),
                    s.getStatus(),
                    sec.getId(),
                    sec.getSectionName(),
                    sec.getSemester().getProgram().getName(),
                    sec.getSemester().getTermName()
                );
            })
            .sorted(Comparator.comparing(StudentDto::registrationNumber))
            .toList();
    }

    @Transactional(readOnly = true)
    public FacultyProfileDto getMyProfile(UserPrincipal caller) {
        FacultyProfile fp = facultyProfileRepository.findByUserId(caller.getId())
            .orElseThrow(() -> new IllegalArgumentException("Faculty profile not found for user: " + caller.getUsername()));

        List<Responsibility> resps = responsibilityRepository.findByUserIdAndTypeAndStatus(
            caller.getId(), null, "ACTIVE"
        );
        List<String> roleLabels = resps.stream()
            .map(r -> {
                if ("CLASS_TEACHER".equals(r.getType()) && r.getSection() != null) {
                    return "Class Teacher (" + r.getSection().getSectionName() + ")";
                } else if ("TEACHING".equals(r.getType()) && r.getSubject() != null) {
                    return r.getSubject().getCode() + " (" + (r.getSection() != null ? r.getSection().getSectionName() : "") + ")";
                } else {
                    return r.getType().replace('_', ' ');
                }
            })
            .toList();

        return new FacultyProfileDto(
            fp.getId(),
            caller.getId(),
            caller.getFullName(),
            caller.getEmail(),
            fp.getEmployeeNumber(),
            fp.getDepartment() != null ? fp.getDepartment().getId() : null,
            fp.getDepartment() != null ? fp.getDepartment().getName() : null,
            fp.getDesignation(),
            fp.getQualification(),
            fp.getSpecialization(),
            fp.getProfessionalExperience(),
            fp.getInstitutionalContact(),
            fp.isActiveStatus(),
            roleLabels
        );
    }

    private boolean isHodOfSection(UserPrincipal caller, UUID sectionId) {
        if (!caller.isHod()) return false;
        Section sec = sectionRepository.findById(sectionId).orElse(null);
        if (sec == null) return false;
        UUID deptId = sec.getSemester().getProgram().getDepartment().getId();
        return responsibilityRepository.existsByUserIdAndTypeAndDepartmentIdAndStatus(
            caller.getId(), "HOD_DEPT", deptId, "ACTIVE"
        );
    }

    private boolean isClassTeacherOfSection(UserPrincipal caller, UUID sectionId) {
        return responsibilityRepository.existsByUserIdAndTypeAndSectionIdAndStatus(
            caller.getId(), "CLASS_TEACHER", sectionId, "ACTIVE"
        );
    }

    private boolean isTeachingSection(UserPrincipal caller, UUID sectionId) {
        return responsibilityRepository.existsByUserIdAndTypeAndSectionIdAndStatus(
            caller.getId(), "TEACHING", sectionId, "ACTIVE"
        );
    }
}
