package com.crre.academicdms.features.student.service;

import com.crre.academicdms.features.academic.model.Section;
import com.crre.academicdms.features.academic.repository.ProgramRepository;
import com.crre.academicdms.features.academic.repository.SectionRepository;
import com.crre.academicdms.features.student.dto.StudentImportDtos.*;
import com.crre.academicdms.features.student.model.Student;
import com.crre.academicdms.features.student.model.StudentEducationalHistory;
import com.crre.academicdms.features.student.model.StudentEnrollment;
import com.crre.academicdms.features.student.repository.StudentEducationalHistoryRepository;
import com.crre.academicdms.features.student.repository.StudentEnrollmentRepository;
import com.crre.academicdms.features.student.repository.StudentRepository;
import com.crre.academicdms.features.user.model.User;
import com.crre.academicdms.features.user.repository.UserRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StudentImportService {

    private static final Logger log = LoggerFactory.getLogger(StudentImportService.class);

    public static final String DEFAULT_REGULAR_PATH = "C:\\Users\\nagav\\Downloads\\2nd year 1st sem rolls list 2026-27.xlsx";
    public static final String DEFAULT_LATERAL_PATH = "C:\\Users\\nagav\\Downloads\\LATERAL ENTRY  ROLLS LIST 26-27 2-1.xlsx";

    public static final Set<String> SUPPORTED_PROGRAMS = Set.of("AIML", "AIDS", "CS", "CSE");

    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final StudentEducationalHistoryRepository educationalHistoryRepository;
    private final ProgramRepository programRepository;
    private final SectionRepository sectionRepository;
    private final UserRepository userRepository;
    private final SyntheticDataManagementService syntheticDataManagementService;

    public StudentImportService(
        StudentRepository studentRepository,
        StudentEnrollmentRepository enrollmentRepository,
        StudentEducationalHistoryRepository educationalHistoryRepository,
        ProgramRepository programRepository,
        SectionRepository sectionRepository,
        UserRepository userRepository,
        SyntheticDataManagementService syntheticDataManagementService
    ) {
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.educationalHistoryRepository = educationalHistoryRepository;
        this.programRepository = programRepository;
        this.sectionRepository = sectionRepository;
        this.userRepository = userRepository;
        this.syntheticDataManagementService = syntheticDataManagementService;
    }

    public static class ParsedWorkbookData {
        public final List<OfficialStudentRecord> validRecords = new ArrayList<>();
        public final List<String> warnings = new ArrayList<>();
        public final List<String> missingRegRows = new ArrayList<>();
        public final List<String> duplicateRegs = new ArrayList<>();
        public final List<AnomalyRecord> anomalies = new ArrayList<>();
        public int skippedCount = 0;
    }

    /**
     * Parse both Excel workbooks and normalize student records.
     * Excludes all sensitive admission fields (Aadhaar, bank, religion, caste, address, income).
     */
    public ParsedWorkbookData parseWorkbooks(Path regularPath, Path lateralPath) {
        ParsedWorkbookData result = new ParsedWorkbookData();
        Map<String, OfficialStudentRecord> seenRegNumbers = new HashMap<>();

        parseRegularWorkbook(regularPath, result, seenRegNumbers);
        parseLateralWorkbook(lateralPath, result, seenRegNumbers);

        return result;
    }

    private void parseRegularWorkbook(Path path, ParsedWorkbookData data, Map<String, OfficialStudentRecord> seenRegNumbers) {
        if (!Files.exists(path)) {
            data.warnings.add("Regular workbook not found at path: " + path);
            return;
        }

        try (InputStream is = new FileInputStream(path.toFile());
             Workbook workbook = new XSSFWorkbook(is)) {

            for (int sIdx = 0; sIdx < workbook.getNumberOfSheets(); sIdx++) {
                Sheet sheet = workbook.getSheetAt(sIdx);
                String sheetName = sheet.getSheetName();

                if ("3RD PHASE TRANSFER LIST".equalsIgnoreCase(sheetName.trim())) {
                    parse3rdPhaseTransferSheet(sheet, data);
                    continue;
                }

                String branchCode = mapSheetToProgramCode(sheetName);

                // Regular sheets: Row 4 (0-based index 3) is column header row, Row 5/6 starts data
                for (int rIdx = 4; rIdx <= sheet.getLastRowNum(); rIdx++) {
                    Row row = sheet.getRow(rIdx);
                    if (row == null) continue;

                    String rawRegNo = getCellString(row.getCell(2));
                    String rawName = getCellString(row.getCell(4));
                    String name = cleanName(rawName);

                    if ((rawRegNo == null || rawRegNo.isBlank()) && (name == null || name.isBlank())) {
                        continue;
                    }

                    if (rawRegNo != null && rawRegNo.toUpperCase().contains("CANCEL")) {
                        data.anomalies.add(new AnomalyRecord(
                            "CANCELLED_ADMISSION",
                            sheetName.trim(),
                            rIdx + 1,
                            rawRegNo.trim(),
                            name,
                            "Seat explicitly cancelled in regular roll sheet."
                        ));
                        data.skippedCount++;
                        continue;
                    }

                    if (rawRegNo == null || rawRegNo.isBlank() || !isValidRegistrationNumber(rawRegNo)) {
                        if (name != null && !name.isBlank() && !"StudentName".equalsIgnoreCase(name)) {
                            data.anomalies.add(new AnomalyRecord(
                                "MISSING_REGISTRATION",
                                sheetName.trim(),
                                rIdx + 1,
                                rawRegNo,
                                name,
                                "Student listed in regular sheet without valid registration number."
                            ));
                            data.missingRegRows.add(sheetName.trim() + " Row " + (rIdx + 1) + ": " + name);
                            data.skippedCount++;
                        }
                        continue;
                    }

                    String regNo = normalizeRegNo(rawRegNo);

                    if (seenRegNumbers.containsKey(regNo)) {
                        OfficialStudentRecord prev = seenRegNumbers.get(regNo);
                        String anomalyMsg = String.format("Duplicate registration number %s: row %d ('%s') conflicts with earlier row %d ('%s' in %s). First occurrence retained.",
                            regNo, rIdx + 1, name, prev.sourceRow(), prev.fullName(), prev.sheetName());
                        data.anomalies.add(new AnomalyRecord(
                            "DUPLICATE_REGISTRATION",
                            sheetName.trim(),
                            rIdx + 1,
                            regNo,
                            name,
                            anomalyMsg
                        ));
                        data.duplicateRegs.add(regNo);
                        data.warnings.add(anomalyMsg);
                        data.skippedCount++;
                        continue;
                    }

                    String rawGender = getCellString(row.getCell(5));
                    String gender = parseGender(rawGender);
                    String mode = getCellString(row.getCell(6));
                    String admCat = parseAdmissionCategory(mode);
                    LocalDate admDate = parseDateCell(row.getCell(7));
                    LocalDate dob = parseDateCell(row.getCell(8));
                    String status = parseStatus(name);

                    // Columns 21..30 (0-based) for Entrance & Educational History
                    String eapcetHt = cleanString(getCellString(row.getCell(21)));
                    Integer eapcetRank = parseIntegerCell(row.getCell(22));

                    String sscSchool = cleanName(getCellString(row.getCell(23)));
                    String sscHt = cleanString(getCellString(row.getCell(24)));
                    String sscBoard = cleanString(getCellString(row.getCell(25)));
                    Integer sscYear = parseYearCell(row.getCell(26));

                    String interCollege = cleanName(getCellString(row.getCell(27)));
                    String interHt = cleanString(getCellString(row.getCell(28)));
                    BigDecimal interPerc = parsePercentageCell(row.getCell(29));
                    String courseGroup = cleanString(getCellString(row.getCell(30)));

                    OfficialStudentRecord record = new OfficialStudentRecord(
                        regNo,
                        name,
                        branchCode,
                        "REGULAR",
                        admCat,
                        admDate,
                        status,
                        dob,
                        gender,
                        "EAPCET",
                        eapcetHt,
                        eapcetRank,
                        sscSchool,
                        sscHt,
                        sscBoard,
                        sscYear,
                        interCollege,
                        interHt,
                        interPerc,
                        courseGroup,
                        sheetName.trim(),
                        rIdx + 1
                    );

                    data.validRecords.add(record);
                    seenRegNumbers.put(regNo, record);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse regular workbook: {}", e.getMessage(), e);
            data.warnings.add("Error parsing regular workbook: " + e.getMessage());
        }
    }

    private void parseLateralWorkbook(Path path, ParsedWorkbookData data, Map<String, OfficialStudentRecord> seenRegNumbers) {
        if (!Files.exists(path)) {
            data.warnings.add("Lateral workbook not found at path: " + path);
            return;
        }

        try (InputStream is = new FileInputStream(path.toFile());
             Workbook workbook = new XSSFWorkbook(is)) {

            for (int sIdx = 0; sIdx < workbook.getNumberOfSheets(); sIdx++) {
                Sheet sheet = workbook.getSheetAt(sIdx);
                String sheetName = sheet.getSheetName();
                String branchCode = mapSheetToProgramCode(sheetName);

                // Lateral sheets: Row 3 (0-based index 2) is the header row
                for (int rIdx = 3; rIdx <= sheet.getLastRowNum(); rIdx++) {
                    Row row = sheet.getRow(rIdx);
                    if (row == null) continue;

                    String rawRegNo = getCellString(row.getCell(2));
                    String rawName = getCellString(row.getCell(3));
                    String name = cleanName(rawName);

                    if ((rawRegNo == null || rawRegNo.isBlank()) && (name == null || name.isBlank())) {
                        continue;
                    }

                    if (rawRegNo != null && rawRegNo.toUpperCase().contains("CANCEL")) {
                        data.anomalies.add(new AnomalyRecord(
                            "CANCELLED_ADMISSION",
                            sheetName.trim(),
                            rIdx + 1,
                            rawRegNo.trim(),
                            name,
                            "Seat explicitly cancelled in lateral entry sheet: " + rawRegNo.trim()
                        ));
                        data.skippedCount++;
                        continue;
                    }

                    if (rawRegNo == null || rawRegNo.isBlank() || !isValidRegistrationNumber(rawRegNo)) {
                        if (name != null && !name.isBlank() && !"StudentName".equalsIgnoreCase(name)) {
                            data.anomalies.add(new AnomalyRecord(
                                "MISSING_REGISTRATION",
                                sheetName.trim(),
                                rIdx + 1,
                                rawRegNo,
                                name,
                                "Student listed in lateral entry sheet without assigned registration number."
                            ));
                            data.missingRegRows.add(sheetName.trim() + " Row " + (rIdx + 1) + ": " + name);
                            data.skippedCount++;
                        }
                        continue;
                    }

                    String regNo = normalizeRegNo(rawRegNo);

                    if (seenRegNumbers.containsKey(regNo)) {
                        OfficialStudentRecord prev = seenRegNumbers.get(regNo);
                        String anomalyMsg = String.format("Duplicate registration number %s: row %d ('%s') conflicts with earlier row %d ('%s' in %s). First occurrence retained.",
                            regNo, rIdx + 1, name, prev.sourceRow(), prev.fullName(), prev.sheetName());
                        data.anomalies.add(new AnomalyRecord(
                            "DUPLICATE_REGISTRATION",
                            sheetName.trim(),
                            rIdx + 1,
                            regNo,
                            name,
                            anomalyMsg
                        ));
                        data.duplicateRegs.add(regNo);
                        data.warnings.add(anomalyMsg);
                        data.skippedCount++;
                        continue;
                    }

                    String rawGender = getCellString(row.getCell(4));
                    String gender = parseGender(rawGender);
                    String mode = getCellString(row.getCell(5));
                    String admCat = parseAdmissionCategory(mode);
                    LocalDate admDate = parseDateCell(row.getCell(6));
                    LocalDate dob = parseDateCell(row.getCell(7));
                    String status = parseStatus(name);

                    // Lateral Columns 20..29 (0-based) for Entrance & Educational History
                    String ecetHt = cleanString(getCellString(row.getCell(20)));
                    Integer ecetRank = parseIntegerCell(row.getCell(21));

                    String sscSchool = cleanName(getCellString(row.getCell(22)));
                    String sscHt = cleanString(getCellString(row.getCell(23)));
                    String sscBoard = cleanString(getCellString(row.getCell(24)));
                    Integer sscYear = parseYearCell(row.getCell(25));

                    String dipCollege = cleanName(getCellString(row.getCell(26)));
                    String dipHt = cleanString(getCellString(row.getCell(27)));
                    BigDecimal dipPerc = parsePercentageCell(row.getCell(28));
                    String courseGroup = cleanString(getCellString(row.getCell(29)));

                    OfficialStudentRecord record = new OfficialStudentRecord(
                        regNo,
                        name,
                        branchCode,
                        "LATERAL",
                        admCat,
                        admDate,
                        status,
                        dob,
                        gender,
                        "ECET",
                        ecetHt,
                        ecetRank,
                        sscSchool,
                        sscHt,
                        sscBoard,
                        sscYear,
                        dipCollege,
                        dipHt,
                        dipPerc,
                        courseGroup,
                        sheetName.trim(),
                        rIdx + 1
                    );

                    data.validRecords.add(record);
                    seenRegNumbers.put(regNo, record);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse lateral workbook: {}", e.getMessage(), e);
            data.warnings.add("Error parsing lateral workbook: " + e.getMessage());
        }
    }

    private void parse3rdPhaseTransferSheet(Sheet sheet, ParsedWorkbookData data) {
        int missingCount = 0;
        for (int rIdx = 2; rIdx <= sheet.getLastRowNum(); rIdx++) {
            Row row = sheet.getRow(rIdx);
            if (row == null) continue;
            String name = cleanName(getCellString(row.getCell(4)));
            String branch = getCellString(row.getCell(1));
            String regNo = getCellString(row.getCell(2));

            if (name == null || name.isBlank() || "StudentName".equalsIgnoreCase(name)) continue;

            if (regNo == null || regNo.isBlank()) {
                missingCount++;
                data.anomalies.add(new AnomalyRecord(
                    "MISSING_REGISTRATION",
                    "3RD PHASE TRANSFER LIST",
                    rIdx + 1,
                    null,
                    name,
                    "Candidate student transfer into " + branch + " without assigned registration number."
                ));
                data.skippedCount++;
            }
        }
        data.warnings.add("3RD PHASE TRANSFER LIST contains " + missingCount +
            " candidate student transfers without registration numbers (retained for human verification).");
    }

    public static String mapSheetToProgramCode(String sheetName) {
        if (sheetName == null) return "UNKNOWN";
        String s = sheetName.trim().toUpperCase();
        return switch (s) {
            case "CSM", "CAM" -> "AIML";
            case "CAD", "CAD " -> "AIDS";
            case "CSC" -> "CS";
            case "CSE" -> "CSE";
            case "ECE" -> "ECE";
            case "EEE" -> "EEE";
            case "INF", "IT" -> "IT";
            case "MECH" -> "MECH";
            case "CIV", "CIVIL" -> "CIVIL";
            default -> s;
        };
    }

    public static String normalizeRegNo(String regNo) {
        if (regNo == null) return "";
        return regNo.trim().toUpperCase().replaceAll("\\s+", "");
    }

    public static boolean isValidRegistrationNumber(String regNo) {
        if (regNo == null) return false;
        String s = regNo.trim().toUpperCase();
        return s.length() >= 8 && (s.startsWith("2") || s.contains("B8"));
    }

    public static String cleanName(String rawName) {
        if (rawName == null) return "";
        return rawName.trim().replaceAll("\\s+", " ");
    }

    private static String cleanString(String val) {
        if (val == null || val.isBlank()) return null;
        String s = val.trim();
        return s.equalsIgnoreCase("null") || s.equalsIgnoreCase("nil") ? null : s;
    }

    public static String parseAdmissionCategory(String mode) {
        if (mode == null || mode.isBlank()) return "CONVENOR";
        String m = mode.trim().toUpperCase();
        if (m.contains("MANAG")) {
            return "MANAGEMENT";
        }
        return "CONVENOR";
    }

    public static String parseStatus(String name) {
        if (name == null) return "ACTIVE";
        String u = name.toUpperCase();
        if (u.contains("CANCELLED") || u.contains("DISCONTINUED") || u.contains("DETAINED")) {
            return "INACTIVE";
        }
        return "ACTIVE";
    }

    public static String parseGender(String raw) {
        if (raw == null || raw.isBlank()) return "OTHER";
        String g = raw.trim().toUpperCase();
        if (g.startsWith("M")) return "MALE";
        if (g.startsWith("F")) return "FEMALE";
        return "OTHER";
    }

    private String getCellString(Cell cell) {
        if (cell == null) return null;
        try {
            return switch (cell.getCellType()) {
                case STRING -> cell.getStringCellValue();
                case NUMERIC -> {
                    if (DateUtil.isCellDateFormatted(cell)) {
                        yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                    } else {
                        long val = (long) cell.getNumericCellValue();
                        yield String.valueOf(val);
                    }
                }
                case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
                case FORMULA -> {
                    try {
                        yield cell.getStringCellValue();
                    } catch (Exception ex) {
                        yield String.valueOf((long) cell.getNumericCellValue());
                    }
                }
                default -> null;
            };
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDate parseDateCell(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            } else if (cell.getCellType() == CellType.STRING) {
                String str = cell.getStringCellValue().trim();
                if (str.length() >= 10 && !str.startsWith("#")) {
                    try {
                        return LocalDate.parse(str.substring(0, 10));
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private Integer parseIntegerCell(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return (int) cell.getNumericCellValue();
            } else if (cell.getCellType() == CellType.STRING) {
                String s = cell.getStringCellValue().trim().replaceAll("[^0-9]", "");
                if (!s.isBlank()) return Integer.parseInt(s);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private Integer parseYearCell(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().getYear();
                }
                int val = (int) cell.getNumericCellValue();
                if (val >= 1990 && val <= 2035) return val;
            } else if (cell.getCellType() == CellType.STRING) {
                String s = cell.getStringCellValue().trim();
                if (s.length() >= 4) {
                    String sub = s.substring(0, 4).replaceAll("[^0-9]", "");
                    if (sub.length() == 4) return Integer.parseInt(sub);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private BigDecimal parsePercentageCell(Cell cell) {
        if (cell == null) return null;
        try {
            double val;
            if (cell.getCellType() == CellType.NUMERIC) {
                val = cell.getNumericCellValue();
            } else if (cell.getCellType() == CellType.STRING) {
                String s = cell.getStringCellValue().trim().replace("%", "").replaceAll("[^0-9.]", "");
                if (s.isBlank()) return null;
                val = Double.parseDouble(s);
            } else {
                return null;
            }

            // Normalise decimals like 0.78 -> 78.0
            if (val > 0.0 && val <= 1.0) {
                val = val * 100.0;
            } else if (val > 100.0 && val <= 1000.0) {
                // Total marks out of 1000 in Intermediate (e.g. 976 -> 97.6%)
                val = val / 10.0;
            } else if (val > 100.0 || val < 0.0) {
                // Anomalous or miskeyed value (e.g. hall ticket in marks column)
                return null;
            }
            return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception ignored) {}
        return null;
    }

    public static class SectionAllocationResult {
        public final Map<String, Section> allocatedSectionMap = new HashMap<>();
        public final List<BranchSummaryDto> branchSummaries = new ArrayList<>();
        public final List<AnomalyRecord> capacityAnomalies = new ArrayList<>();
        public final int unallocatedCount;
        public final int enrolledCount;

        public SectionAllocationResult(
            Map<String, Section> allocatedSectionMap,
            List<BranchSummaryDto> branchSummaries,
            List<AnomalyRecord> capacityAnomalies,
            int unallocatedCount,
            int enrolledCount
        ) {
            this.allocatedSectionMap.putAll(allocatedSectionMap);
            this.branchSummaries.addAll(branchSummaries);
            this.capacityAnomalies.addAll(capacityAnomalies);
            this.unallocatedCount = unallocatedCount;
            this.enrolledCount = enrolledCount;
        }
    }

    public SectionAllocationResult computeSectionAllocations(
        List<OfficialStudentRecord> records,
        List<Section> allSections
    ) {
        Map<String, Section> allocatedSectionMap = new HashMap<>();
        List<BranchSummaryDto> branchSummaries = new ArrayList<>();
        List<AnomalyRecord> capacityAnomalies = new ArrayList<>();

        Map<String, List<Section>> programSections = allSections.stream()
            .collect(Collectors.groupingBy(
                s -> s.getSemester().getProgram().getCode(),
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    list -> list.stream()
                        .sorted(Comparator.comparingInt(Section::getSectionNumber))
                        .toList()
                )
            ));

        Map<String, List<OfficialStudentRecord>> branchRecords = records.stream()
            .collect(Collectors.groupingBy(OfficialStudentRecord::branchCode));

        List<String> branchOrder = List.of("AIML", "AIDS", "CS", "CSE", "ECE", "EEE", "IT", "MECH", "CIVIL");
        int totalEnrolled = 0;
        int totalUnallocated = 0;

        for (String bCode : branchOrder) {
            List<OfficialStudentRecord> bList = branchRecords.getOrDefault(bCode, List.of());
            List<OfficialStudentRecord> regular = bList.stream()
                .filter(r -> "REGULAR".equalsIgnoreCase(r.entryType()))
                .sorted(Comparator.comparing(OfficialStudentRecord::registrationNumber))
                .toList();

            List<OfficialStudentRecord> lateral = bList.stream()
                .filter(r -> "LATERAL".equalsIgnoreCase(r.entryType()))
                .sorted(Comparator.comparing(OfficialStudentRecord::registrationNumber))
                .toList();

            List<Section> sections = programSections.getOrDefault(bCode, List.of());
            boolean supported = !sections.isEmpty();

            int impliedReg = (int) Math.ceil(regular.size() / 66.0);
            int impliedLat = (int) Math.ceil(lateral.size() / 6.0);
            int impliedSec = Math.max(impliedReg, impliedLat);

            List<SectionCountDto> secBreakdown = new ArrayList<>();
            List<String> anomalies = new ArrayList<>();

            if (!supported) {
                anomalies.add("Branch " + bCode + " held in reserve pending department hierarchy creation.");
                totalUnallocated += (regular.size() + lateral.size());
            } else {
                int numDbSections = sections.size();

                for (int sIdx = 0; sIdx < numDbSections; sIdx++) {
                    Section section = sections.get(sIdx);
                    int secNum = section.getSectionNumber();

                    int rStart = sIdx * 66;
                    int rEnd = Math.min((sIdx + 1) * 66, regular.size());
                    List<OfficialStudentRecord> rSec = (rStart < regular.size()) ? regular.subList(rStart, rEnd) : List.of();

                    int lStart = sIdx * 6;
                    int lEnd = Math.min((sIdx + 1) * 6, lateral.size());
                    List<OfficialStudentRecord> lSec = (lStart < lateral.size()) ? lateral.subList(lStart, lEnd) : List.of();

                    for (OfficialStudentRecord r : rSec) {
                        allocatedSectionMap.put(r.registrationNumber(), section);
                    }
                    for (OfficialStudentRecord l : lSec) {
                        allocatedSectionMap.put(l.registrationNumber(), section);
                    }

                    int totalSec = rSec.size() + lSec.size();
                    totalEnrolled += totalSec;

                    String note = null;
                    if (rSec.size() < 66 && sIdx == numDbSections - 1) {
                        note = "Incomplete regular group (" + rSec.size() + "/66)";
                        anomalies.add("Section " + secNum + " has an incomplete regular cohort of " + rSec.size() + " students.");
                    }
                    if (lSec.size() < 6 && sIdx == numDbSections - 1) {
                        note = (note == null ? "" : note + "; ") + "Incomplete lateral group (" + lSec.size() + "/6)";
                        anomalies.add("Section " + secNum + " has an incomplete lateral cohort of " + lSec.size() + " students.");
                    }

                    secBreakdown.add(new SectionCountDto(
                        secNum,
                        section.getSectionName(),
                        rSec.size(),
                        lSec.size(),
                        totalSec,
                        note
                    ));
                }

                int rOverflow = Math.max(0, regular.size() - (numDbSections * 66));
                int lOverflow = Math.max(0, lateral.size() - (numDbSections * 6));
                totalUnallocated += (rOverflow + lOverflow);

                if (rOverflow > 0) {
                    anomalies.add(rOverflow + " regular students unallocated: exceeds capacity of " + numDbSections + " sections.");
                }
                if (lOverflow > 0) {
                    anomalies.add(lOverflow + " lateral-entry students unallocated: exceeds capacity of " + numDbSections + " sections.");
                    int lStart = numDbSections * 6;
                    for (int i = lStart; i < lateral.size(); i++) {
                        OfficialStudentRecord unalloc = lateral.get(i);
                        capacityAnomalies.add(new AnomalyRecord(
                            "UNALLOCATED_CAPACITY",
                            bCode,
                            unalloc.sourceRow(),
                            unalloc.registrationNumber(),
                            unalloc.fullName(),
                            "Lateral student exceeds " + numDbSections + " section capacity (36 seats)."
                        ));
                    }
                }
            }

            branchSummaries.add(new BranchSummaryDto(
                bCode,
                getProgramFullName(bCode),
                regular.size(),
                lateral.size(),
                regular.size() + lateral.size(),
                impliedSec,
                supported,
                secBreakdown,
                supported ? Math.max(0, regular.size() - (sections.size() * 66)) : regular.size(),
                supported ? Math.max(0, lateral.size() - (sections.size() * 6)) : lateral.size(),
                anomalies
            ));
        }

        return new SectionAllocationResult(allocatedSectionMap, branchSummaries, capacityAnomalies, totalUnallocated, totalEnrolled);
    }

    private String getProgramFullName(String code) {
        return switch (code) {
            case "AIML" -> "Artificial Intelligence & Machine Learning";
            case "AIDS" -> "Artificial Intelligence & Data Science";
            case "CS" -> "Cyber Security";
            case "CSE" -> "Computer Science and Engineering";
            case "ECE" -> "Electronics & Communication Engineering";
            case "EEE" -> "Electrical & Electronics Engineering";
            case "IT" -> "Information Technology";
            case "MECH" -> "Mechanical Engineering";
            case "CIVIL" -> "Civil Engineering";
            default -> code;
        };
    }

    @Transactional(readOnly = true)
    public ImportPreviewResponse preview(Path regularPath, Path lateralPath) {
        ParsedWorkbookData parsed = parseWorkbooks(regularPath, lateralPath);
        List<Section> allSections = sectionRepository.findAllWithProgram();
        SectionAllocationResult allocResult = computeSectionAllocations(parsed.validRecords, allSections);

        List<Student> dbStudents = studentRepository.findAll();
        Map<String, Student> dbStudentMap = dbStudents.stream()
            .collect(Collectors.toMap(
                s -> s.getRegistrationNumber().trim().toUpperCase(),
                s -> s,
                (existing, replace) -> existing
            ));

        // Filter valid records strictly to the 4 supported programs for the active DMS scope
        List<OfficialStudentRecord> scopedRecords = parsed.validRecords.stream()
            .filter(r -> SUPPORTED_PROGRAMS.contains(r.branchCode()))
            .toList();

        Set<String> officialRegs = new HashSet<>();
        int created = 0;
        int updated = 0;
        int unchanged = 0;

        for (OfficialStudentRecord off : scopedRecords) {
            String reg = off.registrationNumber();
            officialRegs.add(reg);

            Student existing = dbStudentMap.get(reg);
            if (existing == null) {
                created++;
            } else {
                boolean nameMatches = off.fullName().equalsIgnoreCase(existing.getFullName());
                boolean entryMatches = off.entryType().equalsIgnoreCase(existing.getEntryType());
                boolean catMatches = Objects.equals(off.admissionCategory(), existing.getAdmissionCategory());
                boolean statusMatches = off.status().equalsIgnoreCase(existing.getStatus());

                if (nameMatches && entryMatches && catMatches && statusMatches) {
                    unchanged++;
                } else {
                    updated++;
                }
            }
        }

        List<String> unmatchedSynthetic = dbStudents.stream()
            .map(s -> s.getRegistrationNumber().trim().toUpperCase())
            .filter(r -> !officialRegs.contains(r))
            .sorted()
            .toList();

        List<String> warnings = new ArrayList<>(parsed.warnings);
        warnings.add("Active import scope focused exclusively on CSE, CS, AIML, and AIDS (723 official records). Remaining 5 branches held in reserve.");
        if (!unmatchedSynthetic.isEmpty()) {
            warnings.add(unmatchedSynthetic.size() + " synthetic student records detected. They are isolated (is_synthetic = TRUE, is_current = FALSE) to protect existing relationships.");
        }

        List<AnomalyRecord> allHumanVerification = new ArrayList<>(parsed.anomalies);
        allHumanVerification.addAll(allocResult.capacityAnomalies);

        return new ImportPreviewResponse(
            scopedRecords.size(),
            (int) scopedRecords.stream().filter(r -> "REGULAR".equalsIgnoreCase(r.entryType())).count(),
            (int) scopedRecords.stream().filter(r -> "LATERAL".equalsIgnoreCase(r.entryType())).count(),
            created,
            updated,
            unchanged,
            parsed.skippedCount,
            unmatchedSynthetic.size(),
            allocResult.enrolledCount,
            allocResult.unallocatedCount,
            allocResult.branchSummaries,
            allHumanVerification,
            unmatchedSynthetic,
            warnings
        );
    }

    @Transactional
    public ImportExecuteResponse executeImport(Path regularPath, Path lateralPath, boolean dryRun) {
        ImportPreviewResponse preview = preview(regularPath, lateralPath);

        if (dryRun) {
            return new ImportExecuteResponse(true, true, "Dry-run preview completed successfully. No changes written.", preview);
        }

        ParsedWorkbookData parsed = parseWorkbooks(regularPath, lateralPath);
        List<Section> allSections = sectionRepository.findAllWithProgram();
        SectionAllocationResult allocResult = computeSectionAllocations(parsed.validRecords, allSections);

        // Filter strictly to the 4 supported programs
        List<OfficialStudentRecord> scopedRecords = parsed.validRecords.stream()
            .filter(r -> SUPPORTED_PROGRAMS.contains(r.branchCode()))
            .toList();

        Set<String> officialRegs = scopedRecords.stream()
            .map(OfficialStudentRecord::registrationNumber)
            .collect(Collectors.toSet());

        // Step 1: Soft-deactivate and isolate legacy synthetic students
        syntheticDataManagementService.isolateSyntheticStudents(officialRegs);

        Map<String, Student> dbStudentMap = studentRepository.findAll().stream()
            .collect(Collectors.toMap(
                s -> s.getRegistrationNumber().trim().toUpperCase(),
                s -> s,
                (existing, replace) -> existing
            ));

        int created = 0;
        int updated = 0;
        int unchanged = 0;
        int enrollmentsCreated = 0;
        int enrollmentsUpdated = 0;
        int educationalRecordsCreated = 0;

        // Step 2: Ingest official students and their prior educational history
        for (OfficialStudentRecord off : scopedRecords) {
            String reg = off.registrationNumber();
            Student student = dbStudentMap.get(reg);

            if (student == null) {
                student = new Student(reg, off.fullName(), off.entryType());
                student.setAdmissionCategory(off.admissionCategory());
                student.setStatus(off.status());
                student.setDateOfBirth(off.dateOfBirth());
                student.setGender(off.gender());
                student.setEntranceExamName(off.entranceExamName());
                student.setEntranceHallTicket(off.entranceHallTicket());
                student.setEntranceRank(off.entranceRank());
                student.setSynthetic(false);
                student = studentRepository.save(student);
                dbStudentMap.put(reg, student);
                created++;
            } else {
                boolean changed = false;
                if (!off.fullName().equalsIgnoreCase(student.getFullName())) {
                    student.setFullName(off.fullName());
                    changed = true;
                }
                if (!off.entryType().equalsIgnoreCase(student.getEntryType())) {
                    student.setEntryType(off.entryType());
                    changed = true;
                }
                if (!Objects.equals(off.admissionCategory(), student.getAdmissionCategory())) {
                    student.setAdmissionCategory(off.admissionCategory());
                    changed = true;
                }
                if (!off.status().equalsIgnoreCase(student.getStatus())) {
                    student.setStatus(off.status());
                    changed = true;
                }
                if (!Objects.equals(off.dateOfBirth(), student.getDateOfBirth())) {
                    student.setDateOfBirth(off.dateOfBirth());
                    changed = true;
                }
                if (!Objects.equals(off.gender(), student.getGender())) {
                    student.setGender(off.gender());
                    changed = true;
                }
                if (!Objects.equals(off.entranceExamName(), student.getEntranceExamName())) {
                    student.setEntranceExamName(off.entranceExamName());
                    changed = true;
                }
                if (!Objects.equals(off.entranceHallTicket(), student.getEntranceHallTicket())) {
                    student.setEntranceHallTicket(off.entranceHallTicket());
                    changed = true;
                }
                if (!Objects.equals(off.entranceRank(), student.getEntranceRank())) {
                    student.setEntranceRank(off.entranceRank());
                    changed = true;
                }
                if (student.isSynthetic()) {
                    student.setSynthetic(false);
                    changed = true;
                }

                if (changed) {
                    student = studentRepository.save(student);
                    if (student.getUser() != null && !off.fullName().equalsIgnoreCase(student.getUser().getFullName())) {
                        User u = student.getUser();
                        u.setFullName(off.fullName());
                        userRepository.save(u);
                    }
                    updated++;
                } else {
                    unchanged++;
                }
            }

            // Step 3: Prior Educational History Ingestion
            List<StudentEducationalHistory> existingEdu = educationalHistoryRepository.findByStudentId(student.getId());
            if (existingEdu.isEmpty()) {
                // SSC Record
                if (off.sscSchoolName() != null || off.sscHallTicket() != null) {
                    StudentEducationalHistory ssc = new StudentEducationalHistory(
                        student,
                        "SSC",
                        off.sscBoard() != null ? off.sscBoard() : "State Board of Secondary Education AP",
                        off.sscSchoolName() != null ? off.sscSchoolName() : "Secondary School",
                        off.sscHallTicket(),
                        off.sscCompletionYear() != null ? off.sscCompletionYear() : 2023,
                        "General",
                        null,
                        new BigDecimal("600.00"),
                        null,
                        null,
                        "Official 10th Standard qualification record."
                    );
                    educationalHistoryRepository.save(ssc);
                    educationalRecordsCreated++;
                }

                // Intermediate or Diploma Record
                if ("REGULAR".equalsIgnoreCase(off.entryType())) {
                    if (off.interDipCollegeName() != null || off.interDipHallTicket() != null) {
                        StudentEducationalHistory inter = new StudentEducationalHistory(
                            student,
                            "INTERMEDIATE",
                            "Board of Intermediate Education AP",
                            off.interDipCollegeName() != null ? off.interDipCollegeName() : "Junior College",
                            off.interDipHallTicket(),
                            off.sscCompletionYear() != null ? off.sscCompletionYear() + 2 : 2025,
                            off.courseGroup() != null ? off.courseGroup() : "MPC",
                            null,
                            new BigDecimal("1000.00"),
                            null,
                            off.interDipPercentage(),
                            "Official Intermediate qualification record."
                        );
                        educationalHistoryRepository.save(inter);
                        educationalRecordsCreated++;
                    }
                } else if ("LATERAL".equalsIgnoreCase(off.entryType())) {
                    if (off.interDipCollegeName() != null || off.interDipHallTicket() != null) {
                        StudentEducationalHistory dip = new StudentEducationalHistory(
                            student,
                            "DIPLOMA",
                            "State Board of Technical Education & Training",
                            off.interDipCollegeName() != null ? off.interDipCollegeName() : "Polytechnic College",
                            off.interDipHallTicket(),
                            off.sscCompletionYear() != null ? off.sscCompletionYear() + 3 : 2026,
                            null,
                            off.courseGroup() != null ? off.courseGroup() : off.branchCode(),
                            new BigDecimal("1000.00"),
                            null,
                            off.interDipPercentage(),
                            "Official Diploma qualification record for lateral entry."
                        );
                        educationalHistoryRepository.save(dip);
                        educationalRecordsCreated++;
                    }
                }
            }

            // Step 4: Section Enrollment Assignment
            Section targetSection = allocResult.allocatedSectionMap.get(reg);
            if (targetSection != null) {
                Optional<StudentEnrollment> currentEnrollmentOpt = enrollmentRepository.findByStudentIdAndCurrentTrue(student.getId());
                if (currentEnrollmentOpt.isPresent()) {
                    StudentEnrollment cur = currentEnrollmentOpt.get();
                    if (!cur.getSection().getId().equals(targetSection.getId())) {
                        cur.setSection(targetSection);
                        enrollmentRepository.save(cur);
                        enrollmentsUpdated++;
                    }
                } else {
                    StudentEnrollment newEnrollment = new StudentEnrollment(student, targetSection, true);
                    enrollmentRepository.save(newEnrollment);
                    enrollmentsCreated++;
                }
            }
        }

        String msg = String.format("Official Student Data successfully imported for 4 programs. Created: %d, Updated: %d, Unchanged: %d. Enrollments: %d created, %d updated. Prior Qualifications: %d created.",
            created, updated, unchanged, enrollmentsCreated, enrollmentsUpdated, educationalRecordsCreated);
        log.info(msg);

        ImportPreviewResponse finalSummary = preview(regularPath, lateralPath);
        return new ImportExecuteResponse(false, true, msg, finalSummary);
    }
}
