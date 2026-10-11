package com.crre.academicdms.features.academic.service;

import com.crre.academicdms.features.academic.dto.AcademicDtos.StudentDto;
import com.crre.academicdms.features.academic.dto.HodDtos.DepartmentAcademicStructureDto;
import com.crre.academicdms.features.academic.dto.PrincipalDtos.CollegeDepartmentSummaryDto;
import com.crre.academicdms.features.academic.dto.PrincipalDtos.CollegeOverviewDto;
import com.crre.academicdms.features.academic.model.Department;
import com.crre.academicdms.features.academic.model.Institution;
import com.crre.academicdms.features.academic.model.Program;
import com.crre.academicdms.features.academic.repository.DepartmentRepository;
import com.crre.academicdms.features.academic.repository.InstitutionRepository;
import com.crre.academicdms.features.academic.repository.ProgramRepository;
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
public class PrincipalService {

    private final InstitutionRepository institutionRepository;
    private final DepartmentRepository departmentRepository;
    private final ProgramRepository programRepository;
    private final FacultyProfileRepository facultyProfileRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final StudentRepository studentRepository;
    private final ResponsibilityRepository responsibilityRepository;
    private final HodService hodService;

    public PrincipalService(
        InstitutionRepository institutionRepository,
        DepartmentRepository departmentRepository,
        ProgramRepository programRepository,
        FacultyProfileRepository facultyProfileRepository,
        StudentEnrollmentRepository studentEnrollmentRepository,
        StudentRepository studentRepository,
        ResponsibilityRepository responsibilityRepository,
        HodService hodService
    ) {
        this.institutionRepository = institutionRepository;
        this.departmentRepository = departmentRepository;
        this.programRepository = programRepository;
        this.facultyProfileRepository = facultyProfileRepository;
        this.studentEnrollmentRepository = studentEnrollmentRepository;
        this.studentRepository = studentRepository;
        this.responsibilityRepository = responsibilityRepository;
        this.hodService = hodService;
    }

    @Transactional(readOnly = true)
    public CollegeOverviewDto getCollegeOverview(UserPrincipal caller) {
        enforcePrincipalOrAdmin(caller);

        Institution inst = institutionRepository.findAll().stream().findFirst()
            .orElseGet(() -> new Institution("CRRE", "Sir C. R. Reddy College of Engineering"));

        List<Department> departments = departmentRepository.findAll();
        List<CollegeDepartmentSummaryDto> deptDtos = new ArrayList<>();

        long totalFaculty = 0;
        long totalStudents = 0;
        int totalPrograms = 0;

        for (Department dept : departments) {
            List<Program> progs = programRepository.findByDepartmentId(dept.getId());
            totalPrograms += progs.size();

            long facCount = facultyProfileRepository.findByDepartmentId(dept.getId()).size();
            totalFaculty += facCount;

            long studCount = studentEnrollmentRepository.findAll().stream()
                .filter(e -> e.getSection().getSemester().getProgram().getDepartment().getId().equals(dept.getId()))
                .count();
            totalStudents += studCount;

            // Find HOD name
            String hodName = "Unassigned";
            List<Responsibility> hodResps = responsibilityRepository.findByTypeAndStatus(
                "HOD_DEPT", "ACTIVE"
            );
            Optional<Responsibility> h = hodResps.stream()
                .filter(r -> r.getDepartment() != null && r.getDepartment().getId().equals(dept.getId()))
                .findFirst();
            if (h.isPresent()) {
                hodName = h.get().getUser().getFullName();
            }

            List<com.crre.academicdms.features.academic.dto.PrincipalDtos.PrincipalProgramDto> progDtos = progs.stream()
                .map(p -> {
                    long pStudCount = studentEnrollmentRepository.findAll().stream()
                        .filter(e -> e.getSection().getSemester().getProgram().getId().equals(p.getId()))
                        .count();
                    return new com.crre.academicdms.features.academic.dto.PrincipalDtos.PrincipalProgramDto(
                        p.getId(), p.getId(), p.getCode(), p.getName(), p.getDegree(), pStudCount
                    );
                })
                .toList();

            deptDtos.add(new CollegeDepartmentSummaryDto(
                dept.getId(),
                dept.getId(),
                dept.getCode(),
                dept.getName(),
                hodName,
                progs.size(),
                facCount,
                studCount,
                progs.stream().map(Program::getName).toList(),
                progDtos
            ));
        }

        return new CollegeOverviewDto(
            inst.getId(),
            inst.getCode(),
            inst.getName(),
            caller.getFullName(),
            departments.size(),
            totalPrograms,
            totalFaculty,
            totalStudents,
            deptDtos
        );
    }

    @Transactional(readOnly = true)
    public DepartmentAcademicStructureDto getDepartmentDetails(UUID deptId, UserPrincipal caller) {
        enforcePrincipalOrAdmin(caller);
        return hodService.getDepartmentStructureById(deptId);
    }

    @Transactional(readOnly = true)
    public List<StudentDto> getAllCollegeStudents(UserPrincipal caller) {
        enforcePrincipalOrAdmin(caller);

        List<StudentEnrollment> enrollments = studentEnrollmentRepository.findAll();
        return enrollments.stream()
            .map(e -> {
                Student s = e.getStudent();
                return new StudentDto(
                    s.getId(),
                    s.getRegistrationNumber(),
                    s.getFullName(),
                    s.getEntryType(),
                    s.getStatus(),
                    e.getSection().getId(),
                    e.getSection().getSectionName(),
                    e.getSection().getSemester().getProgram().getName(),
                    e.getSection().getSemester().getTermName()
                );
            })
            .sorted(Comparator.comparing(StudentDto::registrationNumber))
            .toList();
    }

    private void enforcePrincipalOrAdmin(UserPrincipal caller) {
        if (caller == null || (caller.getRole() != Role.PRINCIPAL && caller.getRole() != Role.ADMIN)) {
            throw new AccessDeniedException("Access denied: Caller is not the Principal or an Administrator");
        }
    }
}
