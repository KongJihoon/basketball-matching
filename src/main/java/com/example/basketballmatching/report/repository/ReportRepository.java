package com.example.basketballmatching.report.repository;

import com.example.basketballmatching.report.domain.ReportEntity;
import com.example.basketballmatching.report.type.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<ReportEntity, Long> {

    boolean existsByReportUser_UserIdAndTargetUser_UserIdAndGameEntity_GameId(
            Long reporterId, Long targetUserId, Long gameId
    );


    @EntityGraph(attributePaths = {
            "gameEntity",
            "reportUser",
            "targetUser"
    })
    Page<ReportEntity> findAllByReportStatusOrderByReportedDateTimeDesc(
            ReportStatus reportStatus, Pageable pageable
    );
}


