package com.example.basketballmatching.report.repository;

import com.example.basketballmatching.report.entity.ReportEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReportRepository extends JpaRepository<ReportEntity, Long> {

    boolean existsByTargetUser_UserIdAndGameEntity_GameId(Long reportedUserId, Long gameId);

    Page<ReportEntity> findAllByIsBannedFalseOrderByReportedDateTimeDesc(Pageable pageable);

    Optional<ReportEntity> findByTargetUser_UserId(Long targetUserUserId);

}


