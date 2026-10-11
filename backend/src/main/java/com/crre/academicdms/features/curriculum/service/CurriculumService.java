package com.crre.academicdms.features.curriculum.service;

import com.crre.academicdms.features.academic.model.Section;
import com.crre.academicdms.features.academic.model.Subject;
import com.crre.academicdms.features.academic.repository.SubjectRepository;
import com.crre.academicdms.features.academic.service.HodService;
import com.crre.academicdms.features.curriculum.dto.CurriculumDtos.*;
import com.crre.academicdms.features.curriculum.model.Concept;
import com.crre.academicdms.features.curriculum.model.Resource;
import com.crre.academicdms.features.curriculum.model.Unit;
import com.crre.academicdms.features.curriculum.repository.ConceptRepository;
import com.crre.academicdms.features.curriculum.repository.ResourceRepository;
import com.crre.academicdms.features.curriculum.repository.UnitRepository;
import com.crre.academicdms.features.responsibility.model.Responsibility;
import com.crre.academicdms.features.responsibility.repository.ResponsibilityRepository;
import com.crre.academicdms.features.student.model.Student;
import com.crre.academicdms.features.student.model.StudentEnrollment;
import com.crre.academicdms.features.student.repository.StudentEnrollmentRepository;
import com.crre.academicdms.features.student.repository.StudentRepository;
import com.crre.academicdms.features.user.model.Role;
import com.crre.academicdms.features.user.model.User;
import com.crre.academicdms.features.user.repository.UserRepository;
import com.crre.academicdms.security.UserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CurriculumService {

    private final SubjectRepository subjectRepository;
    private final UnitRepository unitRepository;
    private final ConceptRepository conceptRepository;
    private final ResourceRepository resourceRepository;
    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final ResponsibilityRepository responsibilityRepository;
    private final UserRepository userRepository;
    private final HodService hodService;

    public CurriculumService(
        SubjectRepository subjectRepository,
        UnitRepository unitRepository,
        ConceptRepository conceptRepository,
        ResourceRepository resourceRepository,
        StudentRepository studentRepository,
        StudentEnrollmentRepository studentEnrollmentRepository,
        ResponsibilityRepository responsibilityRepository,
        UserRepository userRepository,
        HodService hodService
    ) {
        this.subjectRepository = subjectRepository;
        this.unitRepository = unitRepository;
        this.conceptRepository = conceptRepository;
        this.resourceRepository = resourceRepository;
        this.studentRepository = studentRepository;
        this.studentEnrollmentRepository = studentEnrollmentRepository;
        this.responsibilityRepository = responsibilityRepository;
        this.userRepository = userRepository;
        this.hodService = hodService;
    }

    @Transactional(readOnly = true)
    public SubjectCurriculumDto getCurriculumForSubject(UUID subjectId, UserPrincipal caller) {
        Subject subject = subjectRepository.findById(subjectId)
            .orElseThrow(() -> new IllegalArgumentException("Subject not found with id: " + subjectId));

        List<Unit> units = unitRepository.findBySubjectIdOrderByUnitNumberAsc(subjectId);
        List<UUID> unitIds = units.stream().map(Unit::getId).toList();

        List<Concept> allConcepts = unitIds.isEmpty() ? List.of() : conceptRepository.findByUnitIdInOrderByConceptOrderAsc(unitIds);
        Map<UUID, List<Concept>> conceptsByUnit = allConcepts.stream().collect(Collectors.groupingBy(c -> c.getUnit().getId()));

        List<UUID> conceptIds = allConcepts.stream().map(Concept::getId).toList();
        List<Resource> allResources = conceptIds.isEmpty() ? List.of() : resourceRepository.findByConceptIdInOrderByCreatedAtAsc(conceptIds);
        Map<UUID, List<Resource>> resourcesByConcept = allResources.stream().collect(Collectors.groupingBy(r -> r.getConcept().getId()));

        List<UnitDto> unitDtos = units.stream().map(u -> {
            List<Concept> uConcepts = conceptsByUnit.getOrDefault(u.getId(), List.of());
            List<ConceptDto> conceptDtos = uConcepts.stream().map(c -> {
                List<Resource> cResources = resourcesByConcept.getOrDefault(c.getId(), List.of());
                List<ResourceDto> resourceDtos = cResources.stream().map(r -> new ResourceDto(
                    r.getId(),
                    c.getId(),
                    r.getTitle(),
                    r.getDescription(),
                    r.getResourceType(),
                    r.getUrl(),
                    r.getFileSizeBytes(),
                    r.getUploadedBy() != null ? r.getUploadedBy().getFullName() : "Faculty Staff",
                    r.getCreatedAt()
                )).toList();

                return new ConceptDto(
                    c.getId(),
                    u.getId(),
                    c.getConceptOrder(),
                    c.getTitle(),
                    c.getDescription(),
                    resourceDtos
                );
            }).toList();

            return new UnitDto(
                u.getId(),
                subject.getId(),
                u.getUnitNumber(),
                u.getTitle(),
                u.getDescription(),
                conceptDtos
            );
        }).toList();

        return new SubjectCurriculumDto(
            subject.getId(),
            subject.getCode(),
            subject.getName(),
            subject.getType(),
            subject.getCredits(),
            null,
            unitDtos
        );
    }

    @Transactional(readOnly = true)
    public List<SubjectCurriculumDto> getStudentCurriculumResources(UserPrincipal caller) {
        if (caller == null) {
            throw new AccessDeniedException("Authentication required");
        }

        Student student = studentRepository.findByUserId(caller.getId())
            .orElseGet(() -> studentRepository.findByRegistrationNumberIgnoreCase(caller.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Student record not found for user: " + caller.getUsername())));

        Optional<StudentEnrollment> enrollment = studentEnrollmentRepository.findByStudentIdAndCurrentTrue(student.getId());
        if (enrollment.isEmpty()) {
            return List.of();
        }

        Section section = enrollment.get().getSection();
        if (section.getSemester() == null) {
            return List.of();
        }

        List<Subject> subjects = subjectRepository.findBySemesterId(section.getSemester().getId());
        List<Responsibility> sectionTeaching = responsibilityRepository.findBySectionIdAndStatus(section.getId(), "ACTIVE").stream()
            .filter(r -> "TEACHING".equalsIgnoreCase(r.getType()) && r.getSubject() != null)
            .toList();

        Map<UUID, String> facultyMap = new HashMap<>();
        for (Responsibility r : sectionTeaching) {
            if (r.getSubject() != null && r.getUser() != null) {
                facultyMap.put(r.getSubject().getId(), r.getUser().getFullName());
            }
        }

        List<SubjectCurriculumDto> result = new ArrayList<>();
        for (Subject sub : subjects) {
            SubjectCurriculumDto curr = getCurriculumForSubject(sub.getId(), caller);
            String instructor = facultyMap.getOrDefault(sub.getId(), "Faculty Staff");
            result.add(new SubjectCurriculumDto(
                curr.subjectId(),
                curr.subjectCode(),
                curr.subjectName(),
                curr.type(),
                curr.credits(),
                instructor,
                curr.units()
            ));
        }

        result.sort(Comparator.comparing(SubjectCurriculumDto::subjectCode));
        return result;
    }

    @Transactional
    public ResourceDto createResource(UUID conceptId, CreateResourceRequest req, UserPrincipal caller) {
        if (caller == null) {
            throw new AccessDeniedException("Authentication required");
        }

        Concept concept = conceptRepository.findById(conceptId)
            .orElseThrow(() -> new IllegalArgumentException("Concept not found with id: " + conceptId));

        Subject subject = concept.getUnit().getSubject();

        // Authorization check: Must be assigned Teaching faculty for this subject, HOD of this dept, or ADMIN
        boolean isAuthorized = false;
        if (caller.getRole() == Role.ADMIN) {
            isAuthorized = true;
        } else if (caller.isHod()) {
            var hodDept = hodService.resolveHodDepartment(caller);
            if (subject.getProgram().getDepartment().getId().equals(hodDept.getId())) {
                isAuthorized = true;
            }
        } else if (caller.getRole() == Role.FACULTY) {
            boolean teachesSubject = responsibilityRepository.findByUserIdAndTypeAndStatus(caller.getId(), "TEACHING", "ACTIVE").stream()
                .anyMatch(r -> r.getSubject() != null && r.getSubject().getId().equals(subject.getId()));
            if (teachesSubject) {
                isAuthorized = true;
            }
        }

        if (!isAuthorized) {
            throw new AccessDeniedException("Faculty can only add resources to assigned teaching subjects");
        }

        User uploader = userRepository.findById(caller.getId()).orElse(null);

        Resource resource = new Resource(
            concept,
            req.title().trim(),
            req.description() != null ? req.description().trim() : null,
            req.resourceType().trim().toUpperCase(),
            req.url().trim(),
            req.fileSizeBytes(),
            uploader
        );

        Resource saved = resourceRepository.save(resource);

        return new ResourceDto(
            saved.getId(),
            concept.getId(),
            saved.getTitle(),
            saved.getDescription(),
            saved.getResourceType(),
            saved.getUrl(),
            saved.getFileSizeBytes(),
            uploader != null ? uploader.getFullName() : "Faculty Staff",
            saved.getCreatedAt()
        );
    }
}
