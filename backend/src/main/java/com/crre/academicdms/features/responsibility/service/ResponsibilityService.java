package com.crre.academicdms.features.responsibility.service;

import com.crre.academicdms.features.academic.model.Department;
import com.crre.academicdms.features.academic.model.Program;
import com.crre.academicdms.features.academic.model.Section;
import com.crre.academicdms.features.academic.model.Subject;
import com.crre.academicdms.features.academic.repository.DepartmentRepository;
import com.crre.academicdms.features.academic.repository.ProgramRepository;
import com.crre.academicdms.features.academic.repository.SectionRepository;
import com.crre.academicdms.features.academic.repository.SubjectRepository;
import com.crre.academicdms.features.responsibility.dto.ResponsibilityRequest;
import com.crre.academicdms.features.responsibility.dto.ResponsibilitySummaryDto;
import com.crre.academicdms.features.responsibility.model.Responsibility;
import com.crre.academicdms.features.responsibility.repository.ResponsibilityRepository;
import com.crre.academicdms.features.user.model.User;
import com.crre.academicdms.features.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ResponsibilityService {

    private final ResponsibilityRepository responsibilityRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final ProgramRepository programRepository;
    private final SubjectRepository subjectRepository;
    private final SectionRepository sectionRepository;

    public ResponsibilityService(
        ResponsibilityRepository responsibilityRepository,
        UserRepository userRepository,
        DepartmentRepository departmentRepository,
        ProgramRepository programRepository,
        SubjectRepository subjectRepository,
        SectionRepository sectionRepository
    ) {
        this.responsibilityRepository = responsibilityRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.programRepository = programRepository;
        this.subjectRepository = subjectRepository;
        this.sectionRepository = sectionRepository;
    }

    @Transactional(readOnly = true)
    public List<ResponsibilitySummaryDto> getActiveResponsibilitiesForUser(UUID userId) {
        return responsibilityRepository.findByUserIdAndStatus(userId, "ACTIVE")
            .stream()
            .map(ResponsibilitySummaryDto::fromEntity)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ResponsibilitySummaryDto> getAllResponsibilitiesForUser(UUID userId) {
        return responsibilityRepository.findByUserId(userId)
            .stream()
            .map(ResponsibilitySummaryDto::fromEntity)
            .toList();
    }

    @Transactional
    public ResponsibilitySummaryDto addResponsibility(UUID userId, ResponsibilityRequest req) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        // Enforce duplicate active assignments prevention
        if ("TEACHING".equalsIgnoreCase(req.type()) && req.subjectId() != null && req.sectionId() != null) {
            if (responsibilityRepository.existsActiveTeaching(userId, req.subjectId(), req.sectionId())) {
                throw new IllegalArgumentException("Duplicate active teaching assignment already exists for this subject and section");
            }
        } else if ("CLASS_TEACHER".equalsIgnoreCase(req.type()) && req.sectionId() != null) {
            if (responsibilityRepository.existsActiveSectionRole(userId, "CLASS_TEACHER", req.sectionId())) {
                throw new IllegalArgumentException("Duplicate active class teacher assignment already exists for this section");
            }
        } else if ("HOD_DEPT".equalsIgnoreCase(req.type()) && req.departmentId() != null) {
            if (responsibilityRepository.existsActiveDeptRole(userId, "HOD_DEPT", req.departmentId())) {
                throw new IllegalArgumentException("Duplicate active HOD assignment already exists for this department");
            }
        }

        Department department = req.departmentId() != null ? departmentRepository.findById(req.departmentId()).orElse(null) : null;
        Program program = req.programId() != null ? programRepository.findById(req.programId()).orElse(null) : null;
        Subject subject = req.subjectId() != null ? subjectRepository.findById(req.subjectId()).orElse(null) : null;
        Section section = req.sectionId() != null ? sectionRepository.findById(req.sectionId()).orElse(null) : null;

        String desc = req.description();
        if (desc == null || desc.isBlank()) {
            if (subject != null && section != null) {
                desc = subject.getName() + " - " + section.getSectionName();
            } else if ("CLASS_TEACHER".equalsIgnoreCase(req.type()) && section != null) {
                desc = "Class Teacher - " + section.getSectionName();
            } else if ("HOD_DEPT".equalsIgnoreCase(req.type()) && department != null) {
                desc = "HOD - " + department.getName();
            } else if ("COUNSELLOR".equalsIgnoreCase(req.type()) && section != null) {
                desc = "Counsellor - " + section.getSectionName();
            } else {
                desc = req.type();
            }
        }

        Responsibility responsibility = new Responsibility(
            user,
            req.type().toUpperCase(),
            department,
            program,
            subject,
            section,
            desc
        );

        Responsibility saved = responsibilityRepository.save(responsibility);
        return ResponsibilitySummaryDto.fromEntity(saved);
    }

    @Transactional
    public ResponsibilitySummaryDto updateResponsibility(UUID responsibilityId, ResponsibilityRequest req) {
        Responsibility r = responsibilityRepository.findById(responsibilityId)
            .orElseThrow(() -> new IllegalArgumentException("Responsibility not found with id: " + responsibilityId));

        if (req.departmentId() != null) {
            r.setDepartment(departmentRepository.findById(req.departmentId()).orElse(null));
        }
        if (req.programId() != null) {
            r.setProgram(programRepository.findById(req.programId()).orElse(null));
        }
        if (req.subjectId() != null) {
            r.setSubject(subjectRepository.findById(req.subjectId()).orElse(null));
        }
        if (req.sectionId() != null) {
            r.setSection(sectionRepository.findById(req.sectionId()).orElse(null));
        }
        if (req.description() != null && !req.description().isBlank()) {
            r.setDescription(req.description());
        }

        Responsibility saved = responsibilityRepository.save(r);
        return ResponsibilitySummaryDto.fromEntity(saved);
    }

    @Transactional
    public void removeResponsibility(UUID responsibilityId) {
        Responsibility r = responsibilityRepository.findById(responsibilityId)
            .orElseThrow(() -> new IllegalArgumentException("Responsibility not found with id: " + responsibilityId));

        // Mark INACTIVE rather than deleting underlying records; this preserves historical integrity
        r.setStatus("INACTIVE");
        responsibilityRepository.save(r);
    }
}
