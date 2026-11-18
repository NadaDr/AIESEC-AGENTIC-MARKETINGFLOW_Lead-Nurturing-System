package com.example.mcptoolsserver.lns.repository;

import com.example.mcptoolsserver.lns.entity.ExploredOpportunity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExploredOpportunityRepository extends JpaRepository<ExploredOpportunity, Long> {

    List<ExploredOpportunity> findByStudentExternalId(String studentExternalId);

    List<ExploredOpportunity> findByStudentExternalIdAndEmailedFalse(String studentExternalId);

    boolean existsByStudentExternalIdAndOpportunityExternalId(
            String studentExternalId, String opportunityExternalId);
}
