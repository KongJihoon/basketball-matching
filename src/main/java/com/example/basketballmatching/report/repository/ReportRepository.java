package com.example.basketballmatching.report.repository;

import com.example.basketballmatching.report.entity.ReportEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<ReportEntity, Long> {

    boolean existsByReportedUser_UserIdAndGameEntity_GameId(Long reportedUserId, Long gameId);

    Page<ReportEntity> findAllByOrderByReportedDateTimeDesc(Pageable pageable);

}


