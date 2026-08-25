package com.example.basketballmatching.blacklist.repository;

import com.example.basketballmatching.blacklist.domain.BlackListEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface BlackListRepository extends JpaRepository<BlackListEntity, Long> {

    boolean existsByReportEntity_ReportId(Long reportId);

    boolean existsByUserEntity_UserIdAndExpiresAtAfter(Long userId, LocalDateTime now);

    boolean existsByUserEntity_EmailAndExpiresAtAfter(String email, LocalDateTime now);

    Page<BlackListEntity> findAllByExpiresAtAfterOrderByBannedDateTimeDesc(
            LocalDateTime now, Pageable pageable
    );

    Page<BlackListEntity> findAllByExpiresAtLessThanEqualOrderByBannedDateTimeDesc(LocalDateTime now, Pageable pageable);

}
