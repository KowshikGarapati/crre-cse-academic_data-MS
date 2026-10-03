package com.crre.academicdms.features.counselling.repository;

import com.crre.academicdms.features.counselling.model.CounsellingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CounsellingRecordRepository extends JpaRepository<CounsellingRecord, UUID> {
    List<CounsellingRecord> findByCounsellorIdOrderByMeetingDateDesc(UUID counsellorUserId);
    List<CounsellingRecord> findByStudentIdOrderByMeetingDateDesc(UUID studentId);
    List<CounsellingRecord> findByCounsellorIdAndStudentIdOrderByMeetingDateDesc(UUID counsellorUserId, UUID studentId);
}
