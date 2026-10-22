package com.crre.academicdms.features.academic.service;

import com.crre.academicdms.features.academic.dto.HodDtos.*;
import com.crre.academicdms.features.academic.model.*;
import com.crre.academicdms.features.academic.repository.*;
import com.crre.academicdms.features.faculty.dto.FacultyDtos.FacultyProfileDto;
import com.crre.academicdms.features.faculty.model.FacultyProfile;
import com.crre.academicdms.features.faculty.repository.FacultyProfileRepository;
import com.crre.academicdms.features.responsibility.model.Responsibility;
import com.crre.academicdms.features.responsibility.repository.ResponsibilityRepository;
import com.crre.academicdms.features.student.repository.StudentEnrollmentRepository;
import com.crre.academicdms.features.user.model.Role;
import com.crre.academicdms.security.UserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class HodService {

    private final DepartmentRepository departmentRepository;
    private final ProgramRepository programRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;
    private final ResponsibilityRepository responsibilityRepository;
    private final FacultyProfileRepository facultyProfileRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;

    public HodService(
        DepartmentRepository departmentRepository,
        ProgramRepository programRepository,
        SectionRepository sectionRepository,
        SubjectRepository subjectRepository,
        ResponsibilityRepository responsibilityRepository,
        FacultyProfileRepository facultyProfileRepository,
        StudentEnrollmentRepository studentEnrollmentRepository
    ) {
        this.departmentRepository = departmentRepository;
        this.programRepository = programRepository;
        this.sectionRepository = sectionRepository;
        this.subjectRepository = subjectRepository;
        this.responsibilityRepository = responsibilityRepository;
        this.facultyProfileRepository = facultyProfileRepository;
        this.studentEnrollmentRepository = studentEnrollmentRepository;
    }

    @Transactional(readOnly = true)
    public DepartmentAcademicStructureDto getMyDepartmentStructure(UserPrincipal caller) {
        Department dept = resolveHodDepartment(caller);
        return getDepartmentStructure(dept);
    }

    @Transactional(readOnly = true)
    public DepartmentAcademicStructureDto getDepartmentStructureById(UUID deptId) {
        Department dept = departmentRepository.findById(deptId)
            .orElseThrow(() -> new IllegalArgumentException("Department not found: " + deptId));
        return getDepartmentStructure(dept);
    }

    @Transactional(readOnly = true)
    public DepartmentAcademicStructureDto getDepartmentStructure(Department dept) {
        // Find HOD name
        String hodName = "Unassigned";
        List<Responsibility> hodResps = responsibilityRepository.findByTypeAndStatus(
            "HOD_DEPT", "ACTIVE"
        );
        Optional<Responsibility> activeHod = hodResps.stream()
            .filter(r -> r.getDepartment() != null && r.getDepartment().getId().equals(dept.getId()))
            .findFirst();
        if (activeHod.isPresent()) {
            hodName = activeHod.get().getUser().getFullName();
        }

        // Build academic years & programs hierarchy
        List<Program> programs = programRepository.findByDepartmentId(dept.getId());

        // Group by Academic Year
        Map<UUID, HodAcademicYearHierarchyDto> ayMap = new LinkedHashMap<>();

        for (Program prog : programs) {
            // Find semesters
            Map<UUID, List<Section>> sectionsBySemester = new HashMap<>();
            List<Section> allSections = sectionRepository.findAll();
            for (Section s : allSections) {
                if (s.getSemester().getProgram().getId().equals(prog.getId())) {
                    sectionsBySemester.computeIfAbsent(s.getSemester().getId(), k -> new ArrayList<>()).add(s);
                }
            }

            Map<UUID, List<HodSemesterSummaryDto>> semsByAy = new HashMap<>();
            for (Map.Entry<UUID, List<Section>> entry : sectionsBySemester.entrySet()) {
                Section firstSec = entry.getValue().get(0);
                Semester sem = firstSec.getSemester();
                AcademicYear ay = sem.getAcademicYear();

                List<HodSectionSummaryDto> secDtos = entry.getValue().stream()
                    .map(sec -> {
                        long studentCount = studentEnrollmentRepository.findBySectionId(sec.getId()).size();
                        String classTeacher = findClassTeacherName(sec.getId());
                        return new HodSectionSummaryDto(
                            sec.getId(),
                            sec.getId(),
                            sec.getSectionNumber(),
                            sec.getSectionName(),
                            studentCount,
                            classTeacher
                        );
                    })
                    .sorted(Comparator.comparingInt(HodSectionSummaryDto::sectionNumber))
                    .toList();

                semsByAy.computeIfAbsent(ay.getId(), k -> new ArrayList<>()).add(
                    new HodSemesterSummaryDto(sem.getId(), sem.getId(), sem.getSemesterNumber(), sem.getTermName(), secDtos)
                );
            }

            for (Map.Entry<UUID, List<HodSemesterSummaryDto>> entry : semsByAy.entrySet()) {
                Semester semSample = entry.getValue().isEmpty() ? null : allSections.stream()
                    .map(Section::getSemester)
                    .filter(s -> s.getAcademicYear().getId().equals(entry.getKey()))
                    .findFirst().orElse(null);

                if (semSample != null) {
                    AcademicYear yearObj = semSample.getAcademicYear();
                    HodProgramHierarchyDto progHierarchy = new HodProgramHierarchyDto(
                        prog.getId(),
                        prog.getId(),
                        prog.getCode(),
                        prog.getCode(),
                        prog.getName(),
                        prog.getName(),
                        prog.getDegree(),
                        entry.getValue().stream().sorted(Comparator.comparingInt(HodSemesterSummaryDto::semesterNumber)).toList()
                    );

                    ayMap.computeIfAbsent(yearObj.getId(), k -> new HodAcademicYearHierarchyDto(
                        yearObj.getId(),
                        yearObj.getId(),
                        yearObj.getYearName(),
                        yearObj.getYearNumber(),
                        yearObj.isCurrent(),
                        yearObj.isCurrent(),
                        new ArrayList<>()
                    )).programs().add(progHierarchy);
                }
            }
        }

        // Faculty profiles in department
        List<FacultyProfile> facultyProfiles = facultyProfileRepository.findByDepartmentId(dept.getId());
        List<FacultyProfileDto> facultyDtos = facultyProfiles.stream()
            .map(fp -> {
                List<String> roleLabels = List.of();
                if (fp.getUser() != null) {
                    List<Responsibility> resps = responsibilityRepository.findByUserIdAndTypeAndStatus(
                        fp.getUser().getId(), null, "ACTIVE"
                    );
                    roleLabels = resps.stream()
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
                }

                UUID uId = fp.getUser() != null ? fp.getUser().getId() : null;
                String fullName = fp.getFacultyName();
                String email = fp.getUser() != null ? fp.getUser().getEmail() : null;

                return new FacultyProfileDto(
                    fp.getId(),
                    uId,
                    fullName,
                    email,
                    fp.getEmployeeNumber(),
                    dept.getId(),
                    dept.getName(),
                    fp.getDesignation(),
                    fp.getQualification(),
                    fp.getSpecialization(),
                    fp.getProfessionalExperience(),
                    fp.getInstitutionalContact(),
                    fp.isActiveStatus(),
                    roleLabels,
                    fp.getSourceUrl(),
                    fp.isSynthetic()
                );
            })
            .toList();

        // Subjects in department
        List<Subject> subjects = subjectRepository.findAll().stream()
            .filter(s -> s.getProgram().getDepartment().getId().equals(dept.getId()))
            .toList();

        List<HodSubjectSummaryDto> subjectDtos = subjects.stream()
            .map(s -> {
                List<Responsibility> teaching = responsibilityRepository.findAll().stream()
                    .filter(r -> "TEACHING".equals(r.getType()) && "ACTIVE".equals(r.getStatus()) 
                        && r.getSubject() != null && r.getSubject().getId().equals(s.getId()))
                    .toList();
                List<String> facultyNames = teaching.stream().map(r -> r.getUser().getFullName()).distinct().toList();

                return new HodSubjectSummaryDto(
                    s.getId(),
                    s.getCode(),
                    s.getName(),
                    s.getType(),
                    s.getCredits(),
                    s.getProgram().getCode(),
                    s.getProgram().getName(),
                    s.getSemester().getTermName(),
                    facultyNames
                );
            })
            .toList();

        long totalFaculty = facultyDtos.size();
        long totalStudents = studentEnrollmentRepository.findAll().stream()
            .filter(e -> e.getSection() != null
                && e.getSection().getSemester() != null
                && e.getSection().getSemester().getProgram() != null
                && dept.getId().equals(e.getSection().getSemester().getProgram().getDepartment().getId()))
            .map(e -> e.getStudent().getId())
            .distinct()
            .count();

        return new DepartmentAcademicStructureDto(
            dept.getId(),
            dept.getCode(),
            dept.getName(),
            hodName,
            totalFaculty,
            totalStudents,
            new ArrayList<>(ayMap.values()),
            facultyDtos,
            subjectDtos
        );
    }

    public Department resolveHodDepartment(UserPrincipal caller) {
        if (caller.getRole() == Role.ADMIN || caller.getRole() == Role.PRINCIPAL) {
            // Default to first department (CSE)
            return departmentRepository.findByCode("CSE")
                .orElseGet(() -> departmentRepository.findAll().get(0));
        }

        if (!caller.isHod()) {
            throw new AccessDeniedException("Access denied: Caller is not a Department Head (HOD)");
        }

        List<Responsibility> hodResps = responsibilityRepository.findByUserIdAndTypeAndStatus(
            caller.getId(), "HOD_DEPT", "ACTIVE"
        );

        if (hodResps.isEmpty() || hodResps.get(0).getDepartment() == null) {
            // Fallback to department via FacultyProfile if available
            Optional<FacultyProfile> fp = facultyProfileRepository.findByUserId(caller.getId());
            if (fp.isPresent()) {
                return fp.get().getDepartment();
            }
            return departmentRepository.findByCode("CSE")
                .orElseThrow(() -> new AccessDeniedException("No authorized department found for this HOD"));
        }

        return hodResps.get(0).getDepartment();
    }

    private String findClassTeacherName(UUID sectionId) {
        List<Responsibility> ctResps = responsibilityRepository.findAll().stream()
            .filter(r -> "CLASS_TEACHER".equals(r.getType()) && "ACTIVE".equals(r.getStatus())
                && r.getSection() != null && r.getSection().getId().equals(sectionId))
            .toList();
        return ctResps.isEmpty() ? "Unassigned" : ctResps.get(0).getUser().getFullName();
    }
}
