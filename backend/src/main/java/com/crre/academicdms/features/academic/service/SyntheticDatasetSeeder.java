package com.crre.academicdms.features.academic.service;

import com.crre.academicdms.features.academic.model.*;
import com.crre.academicdms.features.academic.repository.*;
import com.crre.academicdms.features.counselling.model.CounsellingAssignment;
import com.crre.academicdms.features.counselling.model.CounsellingRecord;
import com.crre.academicdms.features.counselling.repository.CounsellingAssignmentRepository;
import com.crre.academicdms.features.counselling.repository.CounsellingRecordRepository;
import com.crre.academicdms.features.curriculum.model.Concept;
import com.crre.academicdms.features.curriculum.model.Resource;
import com.crre.academicdms.features.curriculum.model.Unit;
import com.crre.academicdms.features.curriculum.repository.ConceptRepository;
import com.crre.academicdms.features.curriculum.repository.ResourceRepository;
import com.crre.academicdms.features.curriculum.repository.UnitRepository;
import com.crre.academicdms.features.faculty.model.FacultyProfile;
import com.crre.academicdms.features.faculty.repository.FacultyProfileRepository;
import com.crre.academicdms.features.responsibility.model.Responsibility;
import com.crre.academicdms.features.responsibility.repository.ResponsibilityRepository;
import com.crre.academicdms.features.student.model.Student;
import com.crre.academicdms.features.student.model.StudentEducationalHistory;
import com.crre.academicdms.features.student.model.StudentEnrollment;
import com.crre.academicdms.features.student.repository.StudentEducationalHistoryRepository;
import com.crre.academicdms.features.student.repository.StudentEnrollmentRepository;
import com.crre.academicdms.features.student.repository.StudentRepository;
import com.crre.academicdms.features.user.model.Role;
import com.crre.academicdms.features.user.model.User;
import com.crre.academicdms.features.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Component
public class SyntheticDatasetSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SyntheticDatasetSeeder.class);

    private final InstitutionRepository institutionRepository;
    private final DepartmentRepository departmentRepository;
    private final ProgramRepository programRepository;
    private final AcademicYearRepository academicYearRepository;
    private final SemesterRepository semesterRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;
    private final UnitRepository unitRepository;
    private final ConceptRepository conceptRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final FacultyProfileRepository facultyProfileRepository;
    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final StudentEducationalHistoryRepository educationalHistoryRepository;
    private final ResponsibilityRepository responsibilityRepository;
    private final CounsellingAssignmentRepository counsellingAssignmentRepository;
    private final CounsellingRecordRepository counsellingRecordRepository;
    private final TimetableRepository timetableRepository;
    private final PasswordEncoder passwordEncoder;

    // First and last names for synthetic student generation
    private static final String[] FIRST_NAMES = {
        "Akhil", "Tejash", "Yugandar", "Bhavani", "Kavya", "Suresh", "Venkata", "Sai", "Manish",
        "Pooja", "Ananya", "Rohit", "Sneha", "Kalyan", "Deepika", "Aditya", "Divya", "Chaitanya",
        "Harini", "Praveen", "Swathi", "Goutham", "Meghana", "Varun", "Sindhu", "Prasad", "Ramya",
        "Naveen", "Keerthi", "Vamsi", "Aparna", "Lokesh", "Sravani", "Tarun", "Pavani", "Jagadeesh",
        "Mounika", "Kishore", "Anusha", "Mahesh", "Sruthi", "Hemanth", "Lavanya", "Sandeep", "Jyothi",
        "Phani", "Geethika", "Avinash", "Sirisha", "Dinesh", "Sahithi", "Raviteja", "Pranavi", "Nikhil",
        "Tejaswi", "Ganesh", "Sowmya", "Karthik", "Rupa", "Sharath", "Bhavya", "Manoj", "Prathyusha",
        "Vinay", "Chandana", "Ajay"
    };

    private static final String[] LAST_NAMES = {
        "Shariff", "Patro", "Garapati", "Kandula", "Vangala", "Gudapati", "Nallamilli", "Chundru",
        "Bhimavarapu", "Rayavarapu", "Muddana", "Gedela", "Kondapalli", "Yelamarthi", "Uppalapati",
        "Bandaru", "Chintalapati", "Gokaraju", "Dandu", "Sagi", "Penmetsa", "Datla", "Alluri",
        "Kalidindi", "Gadiraju", "Kolla", "Maddipati", "Bollineni", "Potluri", "Koppula", "Vemuri",
        "Tummala", "Galla", "Chennupati", "Katragadda", "Mullapudi", "Cherukuri", "Paruchuri"
    };

    public SyntheticDatasetSeeder(
        InstitutionRepository institutionRepository,
        DepartmentRepository departmentRepository,
        ProgramRepository programRepository,
        AcademicYearRepository academicYearRepository,
        SemesterRepository semesterRepository,
        SectionRepository sectionRepository,
        SubjectRepository subjectRepository,
        UnitRepository unitRepository,
        ConceptRepository conceptRepository,
        ResourceRepository resourceRepository,
        UserRepository userRepository,
        FacultyProfileRepository facultyProfileRepository,
        StudentRepository studentRepository,
        StudentEnrollmentRepository studentEnrollmentRepository,
        StudentEducationalHistoryRepository educationalHistoryRepository,
        ResponsibilityRepository responsibilityRepository,
        CounsellingAssignmentRepository counsellingAssignmentRepository,
        CounsellingRecordRepository counsellingRecordRepository,
        TimetableRepository timetableRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.institutionRepository = institutionRepository;
        this.departmentRepository = departmentRepository;
        this.programRepository = programRepository;
        this.academicYearRepository = academicYearRepository;
        this.semesterRepository = semesterRepository;
        this.sectionRepository = sectionRepository;
        this.subjectRepository = subjectRepository;
        this.unitRepository = unitRepository;
        this.conceptRepository = conceptRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
        this.facultyProfileRepository = facultyProfileRepository;
        this.studentRepository = studentRepository;
        this.studentEnrollmentRepository = studentEnrollmentRepository;
        this.educationalHistoryRepository = educationalHistoryRepository;
        this.responsibilityRepository = responsibilityRepository;
        this.counsellingAssignmentRepository = counsellingAssignmentRepository;
        this.counsellingRecordRepository = counsellingRecordRepository;
        this.timetableRepository = timetableRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        boolean needSeedStudents = studentRepository.count() < 720;
        if (needSeedStudents) {
            ensureAllStudentsHaveUsers();
        }

        log.info("Initializing/verifying CRRE Academic DMS V2.2 Dataset (CSE Department, 4 Programs, 10 Sections, 35 Faculty, conflict-free Timetable)...");

        // 1. Institution & Department
        Institution institution = institutionRepository.findByCode("CRRE")
            .orElseGet(() -> institutionRepository.save(new Institution("CRRE", "Sir C. R. Reddy College of Engineering")));

        Department cseDept = departmentRepository.findByCode("CSE")
            .orElseGet(() -> departmentRepository.save(new Department(institution, "CSE", "Computer Science and Engineering")));

        // 2. Academic Year (2025-2026, 2nd Year)
        AcademicYear academicYear = academicYearRepository.findByYearName("2025-2026")
            .orElseGet(() -> academicYearRepository.save(new AcademicYear("2025-2026", 2, true)));

        // 3. Four Programs under CSE
        // AIML (1 sec), AIDS (2 secs), CS (1 sec), CSE (6 secs)
        Program aimlProgram = getOrCreateProgram(cseDept, "AIML", "Artificial Intelligence & Machine Learning");
        Program aidsProgram = getOrCreateProgram(cseDept, "AIDS", "Artificial Intelligence & Data Science");
        Program csProgram = getOrCreateProgram(cseDept, "CS", "Cyber Security");
        Program cseProgram = getOrCreateProgram(cseDept, "CSE", "Computer Science and Engineering");

        // 4. Semesters (3rd Semester for each Program)
        Semester aimlSem = getOrCreateSemester(aimlProgram, academicYear, 3, "3rd Semester");
        Semester aidsSem = getOrCreateSemester(aidsProgram, academicYear, 3, "3rd Semester");
        Semester csSem = getOrCreateSemester(csProgram, academicYear, 3, "3rd Semester");
        Semester cseSem = getOrCreateSemester(cseProgram, academicYear, 3, "3rd Semester");

        // 5. Ten Sections
        List<Section> sections = new ArrayList<>();
        // Sec 0: AIML Sec 1
        sections.add(getOrCreateSection(aimlSem, 1, "Section 1"));
        // Sec 1: AIDS Sec 1
        sections.add(getOrCreateSection(aidsSem, 1, "Section 1"));
        // Sec 2: AIDS Sec 2
        sections.add(getOrCreateSection(aidsSem, 2, "Section 2"));
        // Sec 3: CS Sec 1
        sections.add(getOrCreateSection(csSem, 1, "Section 1"));
        // Sec 4 to 9: CSE Sec 1 to 6
        for (int i = 1; i <= 6; i++) {
            sections.add(getOrCreateSection(cseSem, i, "Section " + i));
        }

        // 6. Subjects and Curriculum for each Program
        Map<UUID, List<Subject>> programSubjectsMap = new HashMap<>();
        programSubjectsMap.put(aimlProgram.getId(), seedProgramSubjects(aimlProgram, aimlSem));
        programSubjectsMap.put(aidsProgram.getId(), seedProgramSubjects(aidsProgram, aidsSem));
        programSubjectsMap.put(csProgram.getId(), seedProgramSubjects(csProgram, csSem));
        programSubjectsMap.put(cseProgram.getId(), seedProgramSubjects(cseProgram, cseSem));

        // 7. Seed 35 Faculty Members & Profiles
        List<User> facultyUsers = seedFacultyMembers(cseDept);

        // 8. Assign Class Teachers (10 sections -> 10 designated faculty)
        seedClassTeachers(sections, facultyUsers, cseDept);

        // 9. Assign Teaching Responsibilities & Conflict-Free Timetable
        seedTimetablesAndTeaching(sections, programSubjectsMap, facultyUsers, cseDept);

        // 10. Seed 720 Students (66 regular + 6 lateral per section) if not present
        List<Student> allStudents;
        if (needSeedStudents) {
            allStudents = seedStudents(sections);
            // 11. Counselling Assignments (Distribute students across faculty counsellors)
            seedCounselling(allStudents, facultyUsers);
        } else {
            allStudents = studentRepository.findAll();
        }

        log.info("Synthetic dataset verification/generation complete! 10 sections, {} students, 35 faculty members with full curriculum and conflict-free timetables.", allStudents.size());
    }

    private Program getOrCreateProgram(Department dept, String code, String name) {
        Program p = programRepository.findByCode(code).orElse(null);
        if (p == null) {
            return programRepository.save(new Program(dept, code, name, "B.Tech"));
        }
        if (!name.equals(p.getName())) {
            p.setName(name);
            p = programRepository.save(p);
        }
        return p;
    }

    private Semester getOrCreateSemester(Program program, AcademicYear year, int semNum, String termName) {
        return semesterRepository.findByProgramIdAndSemesterNumber(program.getId(), semNum)
            .orElseGet(() -> semesterRepository.save(new Semester(program, year, semNum, termName, true)));
    }

    private Section getOrCreateSection(Semester semester, int secNum, String secName) {
        return sectionRepository.findBySemesterIdAndSectionNumber(semester.getId(), secNum)
            .orElseGet(() -> sectionRepository.save(new Section(semester, secNum, secName)));
    }

    private List<Subject> seedProgramSubjects(Program program, Semester semester) {
        List<SubjectDef> defs = List.of(
            new SubjectDef("CS301", "Discrete Mathematics & Graph Theory", "THEORY", new BigDecimal("3.0")),
            new SubjectDef("CS302", "Data Structures & Algorithms", "THEORY", new BigDecimal("3.0")),
            new SubjectDef("CS303", "Object Oriented Programming through Java", "THEORY", new BigDecimal("3.0")),
            new SubjectDef("CS304", "Digital Logic & Computer Organization", "THEORY", new BigDecimal("3.0")),
            new SubjectDef("CS305", "Universal Human Values & Professional Ethics", "THEORY", new BigDecimal("3.0")),
            new SubjectDef("CS306L", "Data Structures & Algorithms Lab", "LAB", new BigDecimal("1.5")),
            new SubjectDef("CS307L", "Java Programming Lab", "LAB", new BigDecimal("1.5"))
        );

        List<Subject> subjects = new ArrayList<>();
        for (SubjectDef def : defs) {
            Subject sub = subjectRepository.findByProgramIdAndCode(program.getId(), def.code).orElse(null);
            if (sub == null) {
                sub = subjectRepository.save(new Subject(
                    program, semester, def.code, def.name, def.type, def.credits
                ));
            } else {
                sub.setName(def.name);
                sub.setType(def.type);
                sub.setCredits(def.credits);
                sub = subjectRepository.save(sub);
            }
            subjects.add(sub);

            // Seed distinct Curriculum Units, Concepts, and Educational Resources
            seedCurriculumForSubject(sub);
        }
        return subjects;
    }

    private record UnitSpec(String title, String desc, String videoUrl, String videoTitle, String[] topics) {}

    private void seedCurriculumForSubject(Subject subject) {
        List<UnitSpec> specs = getSubjectCurriculumSpecs(subject.getCode());
        List<Unit> existingUnits = unitRepository.findBySubjectIdOrderByUnitNumberAsc(subject.getId());

        for (int i = 0; i < specs.size(); i++) {
            UnitSpec spec = specs.get(i);
            Unit unit;
            if (i < existingUnits.size()) {
                unit = existingUnits.get(i);
                unit.setTitle("Unit " + (i + 1) + ": " + spec.title);
                unit.setDescription(spec.desc);
                unit = unitRepository.save(unit);
            } else {
                unit = unitRepository.save(new Unit(
                    subject,
                    i + 1,
                    "Unit " + (i + 1) + ": " + spec.title,
                    spec.desc
                ));
            }

            List<Concept> existingConcepts = conceptRepository.findByUnitIdOrderByConceptOrderAsc(unit.getId());
            for (int c = 0; c < spec.topics.length; c++) {
                String topicName = spec.topics[c];
                Concept concept;
                if (c < existingConcepts.size()) {
                    concept = existingConcepts.get(c);
                    concept.setTitle("Topic " + (i + 1) + "." + (c + 1) + ": " + topicName);
                    concept.setDescription("Institutional syllabus coverage for " + topicName + " including theoretical formulations, analytical derivations, and practical applications.");
                    concept = conceptRepository.save(concept);
                } else {
                    concept = conceptRepository.save(new Concept(
                        unit,
                        c + 1,
                        "Topic " + (i + 1) + "." + (c + 1) + ": " + topicName,
                        "Institutional syllabus coverage for " + topicName + " including theoretical formulations, analytical derivations, and practical applications."
                    ));
                }

                // Delete old resources for this concept to refresh them cleanly
                List<Resource> oldResources = resourceRepository.findByConceptIdOrderByCreatedAtAsc(concept.getId());
                if (!oldResources.isEmpty()) {
                    resourceRepository.deleteAllInBatch(oldResources);
                }

                // 1 Video Lecture
                resourceRepository.save(new Resource(
                    concept,
                    spec.videoTitle + " - " + topicName,
                    "Recommended university lecture video covering core concepts, visual demonstrations, and exam problem solutions.",
                    "YOUTUBE_VIDEO",
                    spec.videoUrl,
                    null,
                    null
                ));

                // 1 Handout Notes Resource
                resourceRepository.save(new Resource(
                    concept,
                    subject.getCode() + " - Unit " + (i + 1) + " Handout (" + topicName + ").pdf",
                    "Departmental verified lecture notes, derivations, discussion problems, and revision highlights.",
                    "PDF_DOCUMENT",
                    "/academics/notes/" + subject.getCode().toLowerCase() + "-u" + (i + 1) + "-c" + (c + 1) + ".pdf",
                    2457600L + ((c + 1) * 102400L),
                    null
                ));
            }
        }
    }

    private List<UnitSpec> getSubjectCurriculumSpecs(String code) {
        if ("CS301".equals(code)) {
            return List.of(
                new UnitSpec(
                    "Mathematical Logic & Set Theory",
                    "Fundamental propositional logic, truth tables, and inference rules.",
                    "https://www.youtube.com/watch?v=tyDKR4FG3Yw",
                    "Discrete Mathematics: Propositional Logic - TrevTutor",
                    new String[]{"Propositional Logic & Truth Tables", "Predicates & Quantifiers", "Proof Techniques & Mathematical Induction"}
                ),
                new UnitSpec(
                    "Relations, Functions & Pigeonhole Principle",
                    "Equivalence relations, partial orderings, and mapping properties.",
                    "https://www.youtube.com/watch?v=r05QOzxVffs",
                    "Relations & Equivalence Relations - Neso Academy",
                    new String[]{"Relations & Equivalence Classes", "Functions & Invertibility", "Pigeonhole Principle & Permutations"}
                ),
                new UnitSpec(
                    "Recurrence Relations & Generating Functions",
                    "Solving linear recurrences and generating function modeling.",
                    "https://www.youtube.com/watch?v=4802c6W4_Xg",
                    "Solving Recurrence Relations - Neso Academy",
                    new String[]{"Linear Homogeneous Recurrences", "Inhomogeneous Recurrence Relations", "Generating Functions & Coefficients"}
                ),
                new UnitSpec(
                    "Graph Fundamentals & Traversals",
                    "Graph representations, connectivity, Eulerian, and Hamiltonian cycles.",
                    "https://www.youtube.com/watch?v=09_LlHjoEiY",
                    "Graph Theory Full Course - freeCodeCamp",
                    new String[]{"Graphs, Subgraphs & Degree Sequences", "Euler & Hamiltonian Paths", "Planar Graphs & Coloring"}
                ),
                new UnitSpec(
                    "Trees, Lattices & Algebraic Structures",
                    "Minimum spanning trees, algebraic structures, and Boolean lattices.",
                    "https://www.youtube.com/watch?v=wXkXh3b49q0",
                    "Algebraic Structures in Discrete Mathematics - NPTEL",
                    new String[]{"Trees & Spanning Tree Algorithms", "Semigroups, Monoids & Groups", "Lattices & Boolean Algebras"}
                )
            );
        } else if ("CS302".equals(code) || "CS306L".equals(code)) {
            return List.of(
                new UnitSpec(
                    "Foundations & Asymptotic Analysis",
                    "Algorithmic efficiency notations, array operations, and pointer structures.",
                    "https://www.youtube.com/watch?v=RBSGKlAvoiM",
                    "Introduction and Analysis of Algorithms - MIT OCW",
                    new String[]{"Asymptotic Complexity & Big-O", "Arrays & Dynamic Memory Allocation", "Singly & Doubly Linked Lists"}
                ),
                new UnitSpec(
                    "Linear Data Structures & Recursion",
                    "Stack applications, queue variations, and recursive backtracking.",
                    "https://www.youtube.com/watch?v=0IAPZzGSbME",
                    "Stack & Queue Data Structures - Abdul Bari",
                    new String[]{"Stacks & Infix to Postfix Conversion", "Queues, Circular & Priority Queues", "Recursion & Backtracking"}
                ),
                new UnitSpec(
                    "Hierarchical Trees & Priority Heaps",
                    "Tree properties, balanced search trees, and heap structures.",
                    "https://www.youtube.com/watch?v=qH6yxkw0u78",
                    "Trees & Binary Search Trees - Abdul Bari",
                    new String[]{"Binary Trees & Traversal Algorithms", "Binary Search Trees & AVL Balance", "Binary Heaps & Heap Sort"}
                ),
                new UnitSpec(
                    "Graph Algorithms & Minimum Spanning Trees",
                    "Adjacency structures, graph traversals, and greedy spanning trees.",
                    "https://www.youtube.com/watch?v=pcKY4hjDrxk",
                    "Graph Traversals BFS & DFS - Abdul Bari",
                    new String[]{"Graph Representations & Structures", "Breadth First & Depth First Search", "Prim's & Kruskal's MST Algorithms"}
                ),
                new UnitSpec(
                    "Shortest Paths, Hashing & Optimization",
                    "Shortest path algorithms, hashing strategies, and disjoint sets.",
                    "https://www.youtube.com/watch?v=XB4MIexjvY0",
                    "Dijkstra's Algorithm - Abdul Bari",
                    new String[]{"Dijkstra & Bellman-Ford Shortest Path", "Hashing & Collision Resolution", "Disjoint Set Union & Dynamic Programming"}
                )
            );
        } else if ("CS303".equals(code) || "CS307L".equals(code)) {
            return List.of(
                new UnitSpec(
                    "Java Architecture & Object Foundations",
                    "JVM execution model, object instantiation, and encapsulation.",
                    "https://www.youtube.com/watch?v=eIrMbAQSU34",
                    "Java Tutorial for Beginners - Programming with Mosh",
                    new String[]{"JVM Architecture & Bytecode", "Classes, Objects & Constructors", "Encapsulation & Access Specifiers"}
                ),
                new UnitSpec(
                    "Inheritance, Polymorphism & Interfaces",
                    "Class hierarchies, dynamic dispatch, and interface abstraction.",
                    "https://www.youtube.com/watch?v=xk4_1vDrzzo",
                    "Object-Oriented Programming in Java - freeCodeCamp",
                    new String[]{"Inheritance Models & super Keyword", "Polymorphism & Dynamic Method Dispatch", "Abstract Classes & Interface Contracts"}
                ),
                new UnitSpec(
                    "Exception Handling & Package Architecture",
                    "Error handling paradigms, custom exceptions, and module packaging.",
                    "https://www.youtube.com/watch?v=1XAT5gxb-3s",
                    "Java Exception Handling - Telusko",
                    new String[]{"Exception Hierarchy & try-catch-finally", "Custom Exceptions & try-with-resources", "Package Structures & CLASSPATH"}
                ),
                new UnitSpec(
                    "Multithreading & Collections Framework",
                    "Concurrent execution, synchronization monitors, and Java collections.",
                    "https://www.youtube.com/watch?v=L93AMUPcx2k",
                    "Java Multithreading & Concurrency - Defog Tech",
                    new String[]{"Thread Lifecycle & Runnable Interface", "Synchronization & Deadlock Prevention", "List, Set, Map & Collections Utility"}
                ),
                new UnitSpec(
                    "Modern Java: Generics, Lambdas & Streams",
                    "Type-safe generics, functional programming, and stream pipelines.",
                    "https://www.youtube.com/watch?v=Q93JsQ8vcwY",
                    "Java Stream API Tutorial - Amigoscode",
                    new String[]{"Generics & Type Constraints", "Lambda Expressions & Functional Interfaces", "Streams API, Filters & Collectors"}
                )
            );
        } else if ("CS304".equals(code)) {
            return List.of(
                new UnitSpec(
                    "Digital Logic & Boolean Algebra",
                    "Binary arithmetic, logic minimization, and gate implementations.",
                    "https://www.youtube.com/watch?v=M0mx8S05v60",
                    "Digital Electronics Full Course - Neso Academy",
                    new String[]{"Number Systems & Complements", "K-Map Minimization & Boolean Algebra", "Universal Logic Gates (NAND/NOR)"}
                ),
                new UnitSpec(
                    "Combinational & Sequential Circuits",
                    "Arithmetic logic units, multiplexers, latches, and flip-flops.",
                    "https://www.youtube.com/watch?v=48fQpY1q1H4",
                    "Sequential Circuits & Flip Flops - Neso Academy",
                    new String[]{"Adders, Subtractors & ALU Design", "Multiplexers, Decoders & Encoders", "Latches, Flip-Flops & Counter Circuits"}
                ),
                new UnitSpec(
                    "Basic Computer Organization & CPU Design",
                    "Register transfers, instruction formatting, and control unit design.",
                    "https://www.youtube.com/watch?v=Ol8D69VKX2k",
                    "Computer Organization and Architecture - Gate Smashers",
                    new String[]{"Instruction Codes & Common Bus System", "Instruction Cycle & Control Logic", "Input-Output & Interrupt Architecture"}
                ),
                new UnitSpec(
                    "Register Organization & Addressing Modes",
                    "Processor registers, addressing modes, and stack operations.",
                    "https://www.youtube.com/watch?v=5UjM30jWq6g",
                    "Addressing Modes in Computer Architecture - Gate Smashers",
                    new String[]{"General Register Organization", "Instruction Formats & Addressing Modes", "Data Transfer, Manipulation & Stacks"}
                ),
                new UnitSpec(
                    "Memory Hierarchy & Peripheral Subsystems",
                    "Cache structures, virtual memory, and direct memory access.",
                    "https://www.youtube.com/watch?v=1x_hKj8hP80",
                    "Cache Memory Mapping Techniques - Gate Smashers",
                    new String[]{"Memory Hierarchy & Main Memory", "Cache Memory & Mapping Schemes", "DMA Controller & Peripheral Interfacing"}
                )
            );
        } else { // CS305 Universal Human Values & Professional Ethics
            return List.of(
                new UnitSpec(
                    "Introduction to Value Education",
                    "Need, basic guidelines, and content for value-based education.",
                    "https://www.youtube.com/watch?v=Q8lV9W791B0",
                    "Universal Human Values - Foundation Course",
                    new String[]{"Need & Process for Value Education", "Continuous Happiness & Prosperity Aspirations", "Right Understanding & Physical Facility"}
                ),
                new UnitSpec(
                    "Harmony in the Human Being",
                    "Understanding the coexistence of the Self ('I') and the material Body.",
                    "https://www.youtube.com/watch?v=Kz1P1s6q4_c",
                    "Harmony in the Self - UHV Lecture Series",
                    new String[]{"Co-existence of Sentient 'I' & 'Body'", "Needs of 'I' (Sukh) vs 'Body' (Suvidha)", "Self-Regulation (Sanyam) & Health (Swasthya)"}
                ),
                new UnitSpec(
                    "Harmony in the Family & Society",
                    "Relationship values, trust, respect, and comprehensive social goals.",
                    "https://www.youtube.com/watch?v=Zf8M4q3P_00",
                    "Trust and Respect in Relationships - UHV",
                    new String[]{"Values in Relationships (Trust & Respect)", "Affection, Care, Guidance & Gratitude", "Comprehensive Human Goals & Society"}
                ),
                new UnitSpec(
                    "Harmony in Nature & Universal Existence",
                    "Orders in nature, ecological mutual fulfillment, and universal coexistence.",
                    "https://www.youtube.com/watch?v=E_M5T7K3W_A",
                    "Harmony in Nature and Existence - AICTE UHV",
                    new String[]{"Four Orders in Nature & Interconnectedness", "Mutual Fulfillment in Natural Ecosystems", "Realizing Co-existence (Sah-astitva)"}
                ),
                new UnitSpec(
                    "Professional Ethics & Humanistic Society",
                    "Competence in professional ethics and transition towards humanistic systems.",
                    "https://www.youtube.com/watch?v=D_9k_1sB3eU",
                    "Professional Ethics and Humanistic Society - NPTEL",
                    new String[]{"Natural Acceptance of Ethical Conduct", "Competence in Professional Ethics", "Transition towards a Humane Global Order"}
                )
            );
        }
    }

    private List<User> seedFacultyMembers(Department dept) {
        // 1. Clean up test faculty members
        List<User> testUsers = userRepository.findAll().stream()
            .filter(u -> u.getRole() == Role.FACULTY && (
                (u.getFullName() != null && u.getFullName().contains("Test"))
                || (u.getUsername() != null && u.getUsername().startsWith("fac."))
            ))
            .toList();
        if (!testUsers.isEmpty()) {
            userRepository.deleteAll(testUsers);
            userRepository.flush();
        }

        // 35 Faculty accounts
        FacultyData[] facultyDefs = {
            new FacultyData("hod.cse", "FAC-CSE-001", "Dr. M. Sreenivasa Rao", Role.FACULTY, "Professor & HOD", "Ph.D (Computer Science)", "Computer Networks & High Performance Computing", "22 Years Academic Leadership", "hod.cse@crre.edu"),
            new FacultyData("ramalakshmi", "FAC-CSE-002", "CH. Rama Lakshmi", Role.FACULTY, "Assistant Professor", "M.Tech (CSE)", "Discrete Mathematics & Graph Theory", "8 Years Teaching & Academic Mentorship", "ramalakshmi@crre.edu"),
            new FacultyData("faculty.cse", "FAC-CSE-003", "Dr. Rajesh Kumar", Role.FACULTY, "Associate Professor", "Ph.D (CSE)", "Advanced Data Structures & Algorithms", "12 Years Teaching & Research", "rajesh.kumar@crre.edu"),
            new FacultyData("counsellor.cse", "FAC-CSE-004", "Smt. S. V. Naga Lakshmi", Role.FACULTY, "Assistant Professor", "M.Tech (AI)", "Student Mentorship & AI Systems", "6 Years Academic Guidance", "nagalakshmi.sv@crre.edu"),
            new FacultyData("FAC-CSE-005", "FAC-CSE-005", "Dr. K. Satyanarayana", Role.FACULTY, "Professor", "Ph.D (CSE)", "Software Engineering & Architecture", "18 Years Experience", "k.satyanarayana@crre.edu"),
            new FacultyData("FAC-CSE-006", "FAC-CSE-006", "Sri P. Ramesh", Role.FACULTY, "Assistant Professor", "M.Tech (CSE)", "Computer Organization & Architecture", "7 Years Experience", "p.ramesh@crre.edu"),
            new FacultyData("FAC-CSE-007", "FAC-CSE-007", "Dr. B. Suresh", Role.FACULTY, "Associate Professor", "Ph.D (CSE)", "Algorithms & Graph Analytics", "11 Years Experience", "b.suresh@crre.edu"),
            new FacultyData("FAC-CSE-008", "FAC-CSE-008", "Smt. T. Himabindu", Role.FACULTY, "Assistant Professor", "M.Tech (IT)", "Object Oriented Software Engineering", "9 Years Experience", "t.himabindu@crre.edu"),
            new FacultyData("FAC-CSE-009", "FAC-CSE-009", "Sri G. Venkatesh", Role.FACULTY, "Assistant Professor", "M.Tech (CSE)", "Operating Systems & Cloud Virtualization", "8 Years Experience", "g.venkatesh@crre.edu"),
            new FacultyData("FAC-CSE-010", "FAC-CSE-010", "Dr. D. Madhavi", Role.FACULTY, "Associate Professor", "Ph.D (AI & ML)", "Artificial Intelligence & Neural Networks", "14 Years Experience", "d.madhavi@crre.edu"),
            new FacultyData("FAC-CSE-011", "FAC-CSE-011", "Sri K. Ravindra", Role.FACULTY, "Assistant Professor", "M.Tech (CSE)", "Web Frameworks & Distributed Java", "6 Years Experience", "k.ravindra@crre.edu"),
            new FacultyData("FAC-CSE-012", "FAC-CSE-012", "Smt. M. Geetha", Role.FACULTY, "Assistant Professor", "M.Tech (CSE)", "Formal Languages & Automata Theory", "7 Years Experience", "m.geetha@crre.edu"),
            new FacultyData("FAC-CSE-013", "FAC-CSE-013", "Dr. N. V. Rao", Role.FACULTY, "Professor", "Ph.D (CSE)", "Data Mining & Information Retrieval", "20 Years Experience", "nv.rao@crre.edu"),
            new FacultyData("FAC-CSE-014", "FAC-CSE-014", "Sri P. Kishore", Role.FACULTY, "Assistant Professor", "M.Tech (CSE)", "Machine Learning & Scientific Computing", "5 Years Experience", "p.kishore@crre.edu"),
            new FacultyData("FAC-CSE-015", "FAC-CSE-015", "Smt. S. Anitha", Role.FACULTY, "Assistant Professor", "M.Tech (VLSI)", "Digital Electronics & Embedded Logic", "8 Years Experience", "s.anitha@crre.edu"),
            new FacultyData("FAC-CSE-016", "FAC-CSE-016", "Sri V. Mohan Krishna", Role.FACULTY, "Assistant Professor", "M.Tech (CSE)", "Computer Organization & Embedded Systems", "6 Years Experience", "v.mohankrishna@crre.edu"),
            new FacultyData("FAC-CSE-017", "FAC-CSE-017", "Dr. Y. Srinivasa Reddy", Role.FACULTY, "Associate Professor", "Ph.D (CSE)", "Digital Logic & Microprocessors", "13 Years Experience", "y.srinivasareddy@crre.edu"),
            new FacultyData("FAC-CSE-018", "FAC-CSE-018", "Smt. K. Vijaya Lakshmi", Role.FACULTY, "Assistant Professor", "M.Tech (ECE)", "VLSI Design & Digital Electronics", "9 Years Experience", "k.vijayalakshmi@crre.edu"),
            new FacultyData("FAC-CSE-019", "FAC-CSE-019", "Sri T. Ravi Teja", Role.FACULTY, "Assistant Professor", "M.Tech (CSE)", "Computer Architecture & Parallel Systems", "7 Years Experience", "t.raviteja@crre.edu"),
            new FacultyData("FAC-CSE-020", "FAC-CSE-020", "Dr. P. Sreedhar", Role.FACULTY, "Associate Professor", "Ph.D (CSE)", "Hardware Security & Computer Systems", "12 Years Experience", "p.sreedhar@crre.edu"),
            new FacultyData("FAC-CSE-021", "FAC-CSE-021", "Dr. A. Sudheer Kumar", Role.FACULTY, "Associate Professor", "Ph.D (Humanities & Ethics)", "Universal Human Values & Professional Ethics", "15 Years Experience", "a.sudheerkumar@crre.edu"),
            new FacultyData("FAC-CSE-022", "FAC-CSE-022", "Smt. L. Radhika", Role.FACULTY, "Assistant Professor", "M.A., M.Phil", "Value Education & Human Relations", "8 Years Experience", "l.radhika@crre.edu"),
            new FacultyData("FAC-CSE-023", "FAC-CSE-023", "Sri G. Kalyan Chakravarthy", Role.FACULTY, "Assistant Professor", "M.Tech (CSE)", "Social & Ethical Aspects in Computing", "6 Years Experience", "g.kalyanchakravarthy@crre.edu"),
            new FacultyData("FAC-CSE-024", "FAC-CSE-024", "Dr. B. Indira Priyadarshini", Role.FACULTY, "Associate Professor", "Ph.D (Management)", "Human Values, Ethics & Organizational Behavior", "11 Years Experience", "b.indirapriyadarshini@crre.edu"),
            new FacultyData("FAC-CSE-025", "FAC-CSE-025", "Sri M. Ramana Murthy", Role.FACULTY, "Assistant Professor", "M.Sc., M.Phil", "Professional Ethics & Value Systems", "10 Years Experience", "m.ramanamurthy@crre.edu"),
            new FacultyData("FAC-CSE-026", "FAC-CSE-026", "Sri N. Pradeep Kumar", Role.FACULTY, "Assistant Professor", "M.Tech (CSE)", "Algorithms Laboratory & Problem Solving", "5 Years Experience", "n.pradeepkumar@crre.edu"),
            new FacultyData("FAC-CSE-027", "FAC-CSE-027", "Smt. V. Sandhya Rani", Role.FACULTY, "Assistant Professor", "M.Tech (CSE)", "Data Structures Lab & Competitive Programming", "7 Years Experience", "v.sandhyarani@crre.edu"),
            new FacultyData("FAC-CSE-028", "FAC-CSE-028", "Sri D. Sai Praneeth", Role.FACULTY, "Assistant Professor", "M.Tech (AI & DS)", "Algorithms Engineering & Practical Labs", "4 Years Experience", "d.saipraneeth@crre.edu"),
            new FacultyData("FAC-CSE-029", "FAC-CSE-029", "Dr. C. Harish Babu", Role.FACULTY, "Associate Professor", "Ph.D (CSE)", "Data Structures & Experimental Computing", "10 Years Experience", "c.harishbabu@crre.edu"),
            new FacultyData("FAC-CSE-030", "FAC-CSE-030", "Smt. R. Swetha", Role.FACULTY, "Assistant Professor", "M.Tech (CSE)", "Programming Laboratories & Algorithmic Design", "6 Years Experience", "r.swetha@crre.edu"),
            new FacultyData("FAC-CSE-031", "FAC-CSE-031", "Sri K. Subba Rao", Role.FACULTY, "Assistant Professor", "M.Tech (CSE)", "Java Enterprise Applications & Systems Lab", "8 Years Experience", "k.subbarao@crre.edu"),
            new FacultyData("FAC-CSE-032", "FAC-CSE-032", "Smt. P. Jyotsna", Role.FACULTY, "Assistant Professor", "M.Tech (IT)", "Object-Oriented Programming Practicals", "6 Years Experience", "p.jyotsna@crre.edu"),
            new FacultyData("FAC-CSE-033", "FAC-CSE-033", "Sri S. Venkateswara Rao", Role.FACULTY, "Assistant Professor", "M.Tech (CSE)", "Java Programming & Software Tooling", "7 Years Experience", "s.venkateswararao@crre.edu"),
            new FacultyData("FAC-CSE-034", "FAC-CSE-034", "Dr. M. Bharathi", Role.FACULTY, "Associate Professor", "Ph.D (CSE)", "Object Technologies & Development Labs", "12 Years Experience", "m.bharathi@crre.edu"),
            new FacultyData("FAC-CSE-035", "FAC-CSE-035", "Sri E. Chandra Sekhar", Role.FACULTY, "Assistant Professor", "M.Tech (CSE)", "Java Frameworks & Practical Programming", "5 Years Experience", "e.chandrasekhar@crre.edu")
        };

        List<User> users = new ArrayList<>();
        for (FacultyData fd : facultyDefs) {
            User user = userRepository.findByUsername(fd.username).orElse(null);
            if (user == null) {
                user = new User(
                    fd.username,
                    fd.institutionalContact,
                    passwordEncoder.encode(fd.username),
                    fd.fullName,
                    fd.role
                );
                user.setUniqueId(fd.employeeNumber);
                user.setMustChangePassword(true);
                user.setEnabled(true);
                user = userRepository.save(user);
            } else {
                user.setRole(fd.role);
                user.setUniqueId(fd.employeeNumber);
                user.setFullName(fd.fullName);
                user = userRepository.save(user);
            }
            users.add(user);

            // Faculty Profile
            final User u = user;
            facultyProfileRepository.findByUserId(u.getId())
                .or(() -> facultyProfileRepository.findByEmployeeNumber(fd.employeeNumber))
                .ifPresentOrElse(
                    prof -> {
                        prof.setUser(u);
                        prof.setDepartment(dept);
                        prof.setDesignation(fd.designation);
                        prof.setQualification(fd.qualification);
                        prof.setSpecialization(fd.specialization);
                        prof.setProfessionalExperience(fd.experience);
                        prof.setInstitutionalContact(fd.institutionalContact);
                        prof.setActiveStatus(true);
                        facultyProfileRepository.save(prof);
                    },
                    () -> facultyProfileRepository.save(new FacultyProfile(
                        u,
                        fd.employeeNumber,
                        dept,
                        fd.designation,
                        fd.qualification,
                        fd.specialization,
                        fd.experience,
                        fd.institutionalContact
                    ))
                );
        }

        // Assign HOD Responsibility
        User hodUser = users.get(0);
        if (!responsibilityRepository.existsActiveDeptRole(hodUser.getId(), "HOD_DEPT", dept.getId())) {
            responsibilityRepository.save(new Responsibility(
                hodUser, "HOD_DEPT", dept, null, null, null, "Head of Department - CSE"
            ));
        }

        return users;
    }

    private void seedClassTeachers(List<Section> sections, List<User> facultyUsers, Department dept) {
        // Sec 0 -> Faculty 1 (ramalakshmi)
        // Sec 1 to 9 -> Faculty 4 to 12 (FAC-CSE-005 to FAC-CSE-013)
        // Keeping faculty.cse (index 2) as pure teaching faculty without Class Teacher responsibility
        int[] ctIndices = {1, 4, 5, 6, 7, 8, 9, 10, 11, 12};
        for (int i = 0; i < sections.size(); i++) {
            Section sec = sections.get(i);
            int facIdx = ctIndices[i % ctIndices.length];
            User ct = facultyUsers.get(facIdx);

            Optional<Responsibility> existing = responsibilityRepository.findAll().stream()
                .filter(r -> "CLASS_TEACHER".equals(r.getType()) && "ACTIVE".equals(r.getStatus())
                    && r.getSection() != null && r.getSection().getId().equals(sec.getId()))
                .findFirst();

            if (existing.isPresent()) {
                Responsibility r = existing.get();
                if (!r.getUser().getId().equals(ct.getId())) {
                    r.setUser(ct);
                }
                if (r.getProgram() == null) {
                    r.setProgram(sec.getSemester().getProgram());
                }
                if (r.getAcademicYear() == null || r.getSemester() == null) {
                    r.setAcademicYear(sec.getSemester().getAcademicYear());
                    r.setSemester(sec.getSemester());
                }
                responsibilityRepository.save(r);
            } else {
                Optional<Responsibility> inactive = responsibilityRepository.findAll().stream()
                    .filter(r -> "CLASS_TEACHER".equals(r.getType())
                        && r.getUser().getId().equals(ct.getId())
                        && r.getSection() != null && r.getSection().getId().equals(sec.getId()))
                    .findFirst();
                if (inactive.isPresent()) {
                    Responsibility r = inactive.get();
                    r.setStatus("ACTIVE");
                    r.setAcademicYear(sec.getSemester().getAcademicYear());
                    r.setSemester(sec.getSemester());
                    responsibilityRepository.save(r);
                } else {
                    responsibilityRepository.save(new Responsibility(
                        ct, "CLASS_TEACHER", dept, sec.getSemester().getProgram(),
                        sec.getSemester().getAcademicYear(), sec.getSemester(),
                        null, sec, "Class Teacher - " + sec.getSemester().getProgram().getCode() + " " + sec.getSectionName()
                    ));
                }
            }
        }
    }

    private void seedTimetablesAndTeaching(
        List<Section> sections,
        Map<UUID, List<Subject>> programSubjectsMap,
        List<User> facultyUsers,
        Department dept
    ) {
        final String[] DAYS = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"};

        // Staggered afternoon lab schedule for the 5 sections in each group (days 0..5: Mon..Sat)
        // Lab 1 (CS306L, subIdx 5) and Lab 2 (CS307L, subIdx 6)
        final int[][] GROUP_LAB_DAYS = {
            {0, 2}, // Sec 0: Mon (Lab 1), Wed (Lab 2)
            {1, 3}, // Sec 1: Tue (Lab 1), Thu (Lab 2)
            {2, 4}, // Sec 2: Wed (Lab 1), Fri (Lab 2)
            {3, 5}, // Sec 3: Thu (Lab 1), Sat (Lab 2)
            {4, 0}  // Sec 4: Fri (Lab 1), Mon (Lab 2)
        };

        // 35 Teachers mapped across 7 subjects and 5 section pairs (each teacher has exactly 2 sections, 0 clashes)
        final int[][] TEACHER_MAPPING = {
            {1, 0, 3, 4, 5},      // CS301 (DMGT)
            {2, 6, 7, 8, 9},      // CS302 (DSA) - faculty.cse (index 2) gets Pair 0 (Sec 0 & 1)
            {10, 11, 12, 13, 14}, // CS303 (OOP Java)
            {15, 16, 17, 18, 19}, // CS304 (DCO)
            {20, 21, 22, 23, 24}, // CS305 (UHV)
            {25, 26, 27, 28, 29}, // CS306L (DSA Lab)
            {30, 31, 32, 33, 34}  // CS307L (Java Lab)
        };

        // Step 0: Clean out existing timetable records first so they don't hold FKs to responsibilities
        timetableRepository.deleteAllInBatch();

        // Step 1: Remove old TEACHING responsibilities to eliminate duplicate assignments and reduce faculty load
        List<Responsibility> oldTeaching = responsibilityRepository.findAll().stream()
            .filter(r -> "TEACHING".equals(r.getType()))
            .toList();
        if (!oldTeaching.isEmpty()) {
            responsibilityRepository.deleteAllInBatch(oldTeaching);
        }

        // Step 2: Create exactly 70 clean TEACHING responsibilities (7 per section, 2 per faculty)
        Map<UUID, Map<Integer, Responsibility>> sectionSubjectRespMap = new HashMap<>();
        for (int sIndex = 0; sIndex < sections.size(); sIndex++) {
            Section section = sections.get(sIndex);
            List<Subject> subjects = programSubjectsMap.get(section.getSemester().getProgram().getId());
            if (subjects == null || subjects.size() < 7) continue;

            int pairIdx = sIndex / 2;
            Map<Integer, Responsibility> subjectRespMap = new HashMap<>();

            for (int subIdx = 0; subIdx < 7; subIdx++) {
                Subject sub = subjects.get(subIdx);
                int teacherIdx = TEACHER_MAPPING[subIdx][pairIdx];
                User assignedTeacher = facultyUsers.get(teacherIdx);

                Responsibility resp = responsibilityRepository.save(new Responsibility(
                    assignedTeacher,
                    "TEACHING",
                    dept,
                    section.getSemester().getProgram(),
                    section.getSemester().getAcademicYear(),
                    section.getSemester(),
                    sub,
                    section,
                    "Teaching " + sub.getCode() + " in " + section.getSemester().getProgram().getCode() + " " + section.getSectionName()
                ));
                subjectRespMap.put(subIdx, resp);
            }
            sectionSubjectRespMap.put(section.getId(), subjectRespMap);
        }

        // Step 3: Seed the 42 conflict-free timetable slots for each section (420 slots total)
        List<TimetableEntry> entriesToSave = new ArrayList<>();
        for (int sIndex = 0; sIndex < sections.size(); sIndex++) {
            Section section = sections.get(sIndex);
            List<Subject> subjects = programSubjectsMap.get(section.getSemester().getProgram().getId());
            if (subjects == null || subjects.size() < 7) continue;
            Map<Integer, Responsibility> subjectRespMap = sectionSubjectRespMap.get(section.getId());

            int pairIdx = sIndex / 2;
            int sLocal = sIndex % 5;
            int lab1Day = GROUP_LAB_DAYS[sLocal][0];
            int lab2Day = GROUP_LAB_DAYS[sLocal][1];

            String lectureRoom = "LH-" + (201 + sIndex);

            for (int dIndex = 0; dIndex < DAYS.length; dIndex++) {
                String day = DAYS[dIndex];

                for (int period = 1; period <= 7; period++) {
                    int p = period - 1; // 0..6
                    int subIdx;

                    if (p < 4) {
                        // Morning periods 1..4: Theory rotation across subjects 0..4
                        subIdx = (sLocal + p + dIndex * 3) % 5;
                    } else {
                        // Afternoon periods 5..7: Staggered 3-period Labs or Theory
                        if (dIndex == lab1Day) {
                            subIdx = 5; // CS306L (Lab 1)
                        } else if (dIndex == lab2Day) {
                            subIdx = 6; // CS307L (Lab 2)
                        } else {
                            subIdx = (sLocal + p + dIndex * 3) % 5; // Theory
                        }
                    }

                    Subject subject = subjects.get(subIdx);
                    int teacherIdx = TEACHER_MAPPING[subIdx][pairIdx];
                    User assignedFaculty = facultyUsers.get(teacherIdx);
                    Responsibility teachingResp = subjectRespMap != null ? subjectRespMap.get(subIdx) : null;

                    AcademicPeriod pDef = AcademicPeriod.of(period);
                    String sessionType = (subIdx >= 5) ? "LAB" : "THEORY";
                    String room = (subIdx >= 5) ? ("Lab - " + (sIndex + 1)) : lectureRoom;

                    entriesToSave.add(new TimetableEntry(
                        section,
                        subject,
                        assignedFaculty,
                        teachingResp,
                        day,
                        period,
                        pDef.getStartTime(),
                        pDef.getEndTime(),
                        room,
                        sessionType
                    ));
                }
            }
        }
        timetableRepository.saveAll(entriesToSave);
    }

    private List<Student> seedStudents(List<Section> sections) {
        List<Student> allStudents = new ArrayList<>();
        int nameCounter = 0;

        for (int sIdx = 0; sIdx < sections.size(); sIdx++) {
            Section section = sections.get(sIdx);
            String progCode = section.getSemester().getProgram().getCode();
            int secNum = section.getSectionNumber();

            // Branch code for registration number
            String branchCode;
            switch (progCode) {
                case "AIML" -> branchCode = "42";
                case "AIDS" -> branchCode = "54";
                case "CS" -> branchCode = "06";
                default -> branchCode = "05"; // CSE
            }

            // 66 Regular Students
            for (int r = 1; r <= 66; r++) {
                String regNo;
                if ("AIML".equals(progCode) || "CS".equals(progCode)) {
                    regNo = String.format("25B81A%s%02d", branchCode, r);
                } else if ("AIDS".equals(progCode)) {
                    regNo = (secNum == 1)
                        ? String.format("25B81A%s%02d", branchCode, r)
                        : String.format("25B81A%s2%02d", branchCode, r);
                } else {
                    // CSE 6 sections: secNum 1 to 6
                    regNo = String.format("25B81A%s%d%02d", branchCode, secNum, r);
                }

                String firstName = FIRST_NAMES[nameCounter % FIRST_NAMES.length];
                String lastName = LAST_NAMES[(nameCounter / FIRST_NAMES.length) % LAST_NAMES.length];
                String fullName = firstName + " " + lastName;
                nameCounter++;

                Student student = getOrCreateStudent(regNo, fullName, "REGULAR", section, r);
                allStudents.add(student);
            }

            // 6 Lateral Entry Students
            for (int l = 1; l <= 6; l++) {
                String regNo;
                if ("AIML".equals(progCode) || "CS".equals(progCode)) {
                    regNo = String.format("26B85A%s%02d", branchCode, l);
                } else if ("AIDS".equals(progCode)) {
                    regNo = (secNum == 1)
                        ? String.format("26B85A%s%02d", branchCode, l)
                        : String.format("26B85A%s2%02d", branchCode, l);
                } else {
                    regNo = String.format("26B85A%s%d%02d", branchCode, secNum, l);
                }

                String firstName = FIRST_NAMES[nameCounter % FIRST_NAMES.length];
                String lastName = LAST_NAMES[(nameCounter / FIRST_NAMES.length) % LAST_NAMES.length];
                String fullName = firstName + " " + lastName + " (Lateral)";
                nameCounter++;

                Student student = getOrCreateStudent(regNo, fullName, "LATERAL", section, 66 + l);
                allStudents.add(student);
            }
        }

        return allStudents;
    }

    private void ensureAllStudentsHaveUsers() {
        List<Student> studentsWithoutUser = studentRepository.findAll().stream()
            .filter(s -> s.getUser() == null)
            .toList();
        for (Student s : studentsWithoutUser) {
            String regNo = s.getRegistrationNumber();
            String username = regNo.toLowerCase();
            User user = userRepository.findByUsername(username).orElseGet(() -> {
                User u = new User(
                    username,
                    username + "@student.crre.edu.in",
                    passwordEncoder.encode(regNo),
                    s.getFullName(),
                    Role.STUDENT
                );
                u.setRegistrationNumber(regNo);
                u.setUniqueId("CRRE" + regNo);
                u.setAadharNumber(700000000000L + (long) (Math.abs(regNo.hashCode()) % 899999999999L));
                u.setApaarNumber(900000000000L + (long) (Math.abs((regNo + "A").hashCode()) % 899999999999L));
                u.setMustChangePassword(true);
                u.setEnabled(true);
                return userRepository.save(u);
            });
            s.setUser(user);
            studentRepository.save(s);
        }
    }

    private Student getOrCreateStudent(String regNo, String fullName, String entryType, Section section, int rollInSec) {
        Optional<Student> existing = studentRepository.findByRegistrationNumber(regNo);
        if (existing.isPresent()) {
            Student student = existing.get();
            if (student.getUser() == null) {
                String username = regNo.toLowerCase();
                User user = userRepository.findByUsername(username).orElseGet(() -> {
                    User u = new User(
                        username,
                        username + "@student.crre.edu.in",
                        passwordEncoder.encode(regNo),
                        fullName,
                        Role.STUDENT
                    );
                    u.setRegistrationNumber(regNo);
                    u.setUniqueId("CRRE" + regNo);
                    u.setAadharNumber(700000000000L + (long) (Math.abs(regNo.hashCode()) % 899999999999L));
                    u.setApaarNumber(900000000000L + (long) (Math.abs((regNo + "A").hashCode()) % 899999999999L));
                    u.setMustChangePassword(true);
                    u.setEnabled(true);
                    return userRepository.save(u);
                });
                student.setUser(user);
                student = studentRepository.save(student);
            }
            return student;
        }

        String username = regNo.toLowerCase();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            user = new User(
                username,
                username + "@student.crre.edu.in",
                passwordEncoder.encode(regNo), // Initial password = Registration Number
                fullName,
                Role.STUDENT
            );
            user.setRegistrationNumber(regNo);
            user.setUniqueId("CRRE" + regNo);
            user.setAadharNumber(700000000000L + (long) (Math.abs(regNo.hashCode()) % 899999999999L));
            user.setApaarNumber(900000000000L + (long) (Math.abs((regNo + "A").hashCode()) % 899999999999L));
            user.setMustChangePassword(true);
            user.setEnabled(true);
            user = userRepository.save(user);
        }

        Student student = new Student(regNo, fullName, entryType);
        student.setUser(user);
        student = studentRepository.save(student);

        // Enrollment
        final Student s = student;
        studentEnrollmentRepository.findByStudentIdAndCurrentTrue(student.getId())
            .orElseGet(() -> studentEnrollmentRepository.save(new StudentEnrollment(s, section, true)));

        // Educational History
        if ("REGULAR".equals(entryType)) {
            educationalHistoryRepository.save(new StudentEducationalHistory(
                student, "SSC", "Board of Secondary Education AP",
                "Z.P. High School, Eluru", "SYN-SSC-2021-" + rollInSec, 2021,
                "General", null, new BigDecimal("600"),
                new BigDecimal(String.valueOf(480 + (rollInSec % 100))),
                new BigDecimal(String.format("%.2f", (480.0 + (rollInSec % 100)) / 6.0)),
                "Passed in first division."
            ));
            educationalHistoryRepository.save(new StudentEducationalHistory(
                student, "INTERMEDIATE", "Board of Intermediate Education AP",
                "Sri Chaitanya Junior College, Eluru", "SYN-IPE-2023-" + rollInSec, 2023,
                "MPC", null, new BigDecimal("1000"),
                new BigDecimal(String.valueOf(820 + (rollInSec % 150))),
                new BigDecimal(String.format("%.2f", (820.0 + (rollInSec % 150)) / 10.0)),
                "Mathematics, Physics, Chemistry stream."
            ));
        } else {
            educationalHistoryRepository.save(new StudentEducationalHistory(
                student, "SSC", "Board of Secondary Education AP",
                "Municipal High School, Vijayawada", "SYN-SSC-2020-" + rollInSec, 2020,
                "General", null, new BigDecimal("600"),
                new BigDecimal(String.valueOf(490 + (rollInSec % 90))),
                new BigDecimal(String.format("%.2f", (490.0 + (rollInSec % 90)) / 6.0)),
                "Passed in first division."
            ));
            educationalHistoryRepository.save(new StudentEducationalHistory(
                student, "DIPLOMA", "SBTET Andhra Pradesh",
                "Govt Polytechnic College, Vijayawada", "SYN-DIP-2023-" + rollInSec, 2023,
                null, "Computer Engineering", new BigDecimal("1000"),
                new BigDecimal(String.valueOf(840 + (rollInSec % 140))),
                new BigDecimal(String.format("%.2f", (840.0 + (rollInSec % 140)) / 10.0)),
                "Lateral entry admission through ECET."
            ));
        }

        return student;
    }

    private void seedCounselling(List<Student> allStudents, List<User> facultyUsers) {
        // Counsellor.cse (Faculty index 3) mentors exactly 15 students: 25B81A4201 to 25B81A4215 (testC contract)
        User dedicatedCounsellor = facultyUsers.get(3);
        for (int i = 0; i < 15 && i < allStudents.size(); i++) {
            Student s = allStudents.get(i);
            if (!counsellingAssignmentRepository.existsByCounsellorIdAndStudentIdAndStatus(dedicatedCounsellor.getId(), s.getId(), "ACTIVE")) {
                counsellingAssignmentRepository.save(new CounsellingAssignment(dedicatedCounsellor, s, "ACTIVE"));
            }
        }

        // Remaining students are distributed among mentors, excluding faculty.cse (index 2) so faculty.cse has 0 mentees (testA contract)
        List<User> otherMentors = new ArrayList<>();
        for (int idx = 0; idx < facultyUsers.size(); idx++) {
            if (idx != 2 && idx != 3) {
                otherMentors.add(facultyUsers.get(idx));
            }
        }

        for (int i = 15; i < allStudents.size(); i++) {
            Student student = allStudents.get(i);
            int mentorIdx = (i - 15) % otherMentors.size();
            User mentor = otherMentors.get(mentorIdx);

            if (!counsellingAssignmentRepository.existsByCounsellorIdAndStudentIdAndStatus(mentor.getId(), student.getId(), "ACTIVE")) {
                counsellingAssignmentRepository.save(new CounsellingAssignment(mentor, student, "ACTIVE"));
            }

            // Seed confidential counselling session record for 1 in 20 students
            if (i % 20 == 0) {
                if (counsellingRecordRepository.findByStudentIdOrderByMeetingDateDesc(student.getId()).isEmpty()) {
                    counsellingRecordRepository.save(new CounsellingRecord(
                        mentor,
                        student,
                        LocalDate.now().minusDays((i % 30) + 1),
                        "ACADEMIC",
                        "Reviewed academic goals for 2nd Year 3rd Semester. Advised student on core programming lab assignments.",
                        "Complete Java OOP practice exercises before mid-term assessments.",
                        LocalDate.now().plusDays(14),
                        "OPEN"
                    ));
                }
            }
        }
    }

    private record SubjectDef(String code, String name, String type, BigDecimal credits) {}
    private record FacultyData(String username, String employeeNumber, String fullName, Role role, String designation, String qualification, String specialization, String experience, String institutionalContact) {}
}
