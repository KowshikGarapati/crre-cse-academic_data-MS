package com.crre.academicdms.features.academic.repository;

import com.crre.academicdms.features.academic.model.TimetableEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TimetableRepository extends JpaRepository<TimetableEntry, UUID> {
    List<TimetableEntry> findBySectionIdOrderByDayOfWeekAscPeriodNumberAsc(UUID sectionId);
    List<TimetableEntry> findByFacultyUserIdOrderByDayOfWeekAscPeriodNumberAsc(UUID facultyUserId);
    boolean existsByFacultyUserIdAndDayOfWeekAndPeriodNumber(UUID facultyUserId, String dayOfWeek, int periodNumber);
    boolean existsBySectionIdAndDayOfWeekAndPeriodNumber(UUID sectionId, String dayOfWeek, int periodNumber);
    java.util.Optional<TimetableEntry> findByFacultyUserIdAndDayOfWeekAndPeriodNumber(UUID facultyUserId, String dayOfWeek, int periodNumber);
    java.util.Optional<TimetableEntry> findBySectionIdAndDayOfWeekAndPeriodNumber(UUID sectionId, String dayOfWeek, int periodNumber);
}
