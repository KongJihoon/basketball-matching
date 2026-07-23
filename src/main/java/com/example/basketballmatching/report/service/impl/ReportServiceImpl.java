package com.example.basketballmatching.report.service.impl;

import com.example.basketballmatching.gameCreator.entity.GameEntity;
import com.example.basketballmatching.gameCreator.repository.GameRepository;
import com.example.basketballmatching.gameCreator.repository.ParticipantGameRepository;
import com.example.basketballmatching.global.dto.CommonResponse;
import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.report.dto.CreateReportDto;
import com.example.basketballmatching.report.dto.ReportListDto;
import com.example.basketballmatching.report.entity.ReportEntity;
import com.example.basketballmatching.report.repository.ReportRepository;
import com.example.basketballmatching.report.service.ReportService;
import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

import static com.example.basketballmatching.global.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final GameRepository gameRepository;

    private final ParticipantGameRepository participantGameRepository;

    private final UserRepository userRepository;

    private final ReportRepository reportRepository;


    @Override
    @Transactional
    public CheckResponse createReport(Long reportUserId, Long targetUserId, Long gameId, CreateReportDto createReportDto) {

        GameEntity gameEntity = gameRepository.findByGameIdAndDeletedDateTimeIsNull(gameId)
                .orElseThrow(() -> new CustomException(GAME_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();

        if (gameEntity.getEndDateTime().isAfter(now)) {
            throw new CustomException(NOT_GAME_ENDED);
        }

        UserEntity reportUser = userRepository.findById(reportUserId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        if (!Objects.equals(gameEntity.getUserEntity().getUserId(), reportUser.getUserId())) {
            throw new CustomException(NOT_GAME_CREATOR);
        }

        boolean exists = participantGameRepository.existsByUserEntity_UserIdAndGameEntity_GameId(targetUserId, gameEntity.getGameId());

        if (!exists) {
            throw new CustomException(PARTICIPANT_NOT_FOUND);
        }

        UserEntity targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));


        boolean existsByReport = reportRepository.existsByTargetUser_UserIdAndGameEntity_GameId(targetUser.getUserId(), gameEntity.getGameId());

        if (existsByReport) {
            throw new CustomException(ALREADY_REPORTED_USER);
        }

        ReportEntity reportEntity = ReportEntity.create(reportUser, targetUser, gameEntity, createReportDto);

        reportRepository.save(reportEntity);

        return CheckResponse.of(true, "해당 유저 신고를 완료하였습니다.");
    }

    @Override
    @Transactional
    public CommonResponse<Page<ReportListDto>> getReportedUserList(Long userId, Pageable pageable) {

        userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));


        Page<ReportEntity> reportEntities = reportRepository.findAllByIsBannedFalseOrderByReportedDateTimeDesc(pageable);


        Page<ReportListDto> reportListDtos = reportEntities
                .map(ReportListDto::fromEntity);

        return CommonResponse.of("신고목록 조회를 완료하였습니다.", reportListDtos);
    }
}
