package com.example.mcptoolsserver.lns.repository;

import com.example.mcptoolsserver.lns.entity.SyncedRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SyncedRecordRepository extends JpaRepository<SyncedRecord, Long> {

    Optional<SyncedRecord> findByExternalId(String externalId);

    List<SyncedRecord> findAllByOrderBySyncedAtDesc();

    List<SyncedRecord> findByProgrammeCodeOrderByEarliestStartDateAsc(String programmeCode);

    List<SyncedRecord> findByTitleContainingIgnoreCase(String keyword);
}
