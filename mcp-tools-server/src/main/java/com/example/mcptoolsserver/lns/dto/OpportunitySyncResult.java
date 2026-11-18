package com.example.mcptoolsserver.lns.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record OpportunitySyncResult(

        LocalDateTime startedAt,

        LocalDateTime completedAt,

        int searchPagesProcessed,

        int globalPartnerPagesProcessed,

        int candidateIdsFound,

        int detailsFetched,

        int inserted,

        int updated,

        int ignoredOtherProgrammes,

        Map<String, Integer> savedByProgramme
) {

}
