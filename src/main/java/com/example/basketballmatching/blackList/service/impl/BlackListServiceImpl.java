package com.example.basketballmatching.blackList.service.impl;


import com.example.basketballmatching.blackList.dto.BlackListDto;
import com.example.basketballmatching.blackList.entity.BlackListEntity;
import com.example.basketballmatching.blackList.repository.BlackListRepository;
import com.example.basketballmatching.blackList.service.BlackListService;
import com.example.basketballmatching.gameCreator.entity.ParticipantGameEntity;
import com.example.basketballmatching.gameCreator.repository.ParticipantGameRepository;
import com.example.basketballmatching.gameCreator.type.ParticipantGameStatus;
import com.example.basketballmatching.global.dto.CommonResponse;
import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.service.RedisService;
import com.example.basketballmatching.report.entity.ReportEntity;
import com.example.basketballmatching.report.repository.ReportRepository;
import com.example.basketballmatching.user.entity.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.example.basketballmatching.gameCreator.type.ParticipantGameStatus.ACCEPT;
import static com.example.basketballmatching.gameCreator.type.ParticipantGameStatus.APPLY;
import static com.example.basketballmatching.global.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
public class BlackListServiceImpl implements BlackListService {

    private final UserRepository userRepository;

    private final ReportRepository reportRepository;

    private final BlackListRepository blackListRepository;

    private final ParticipantGameRepository participantGameRepository;

    private final RedisService redisService;

    @Override
    @Transactional
    public CheckResponse createBlackListUser(Long userId, Long reportId) {

        // 관리자 존재 여부
        userRepository.findByUserIdAndDeletedDateTimeIsNull(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        // 신고 내역 존재 여부
        ReportEntity reportEntity = reportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(NOT_FOUND_REPORT));

        if (reportEntity.isBanned()) {
            throw new CustomException(ALREADY_CHECK_REPORT);
        }

        // 신고자 존재 여부 -> 회원탈퇴 가능성
        UserEntity targetUser = getUser(reportEntity.getTargetUser().getUserId());

        // 블랙 유저 존재 여부
        validateBlackUser(targetUser);

        BlackListEntity blackListEntity = BlackListEntity.create(targetUser, LocalDateTime.now());

        blackListRepository.save(blackListEntity);


        // 블랙 유저 예정 경기 상태 변경
        List<ParticipantGameEntity> list = handleBlackUserStatus(targetUser);

        participantGameRepository.saveAll(list);

        reportEntity.setBanned();
        reportRepository.save(reportEntity);

        redisService.setDataExpireDays("blackList:" + targetUser.getEmail(), "BLACKLIST", 7L);



        return CheckResponse.of(true, "신고 유저 블랙리스트 등록에 성공하였습니다.");
    }

    @Override
    @Transactional(readOnly = true)
    public CommonResponse<Page<BlackListDto>> getBlackLists(Long userId, Pageable pageable) {

        userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        List<BlackListEntity> blackListEntity = blackListRepository.findAllByOrderByBannedDateTimeDesc()
                .stream().filter(
                        blackListEntities -> {
                            Long expiration = redisService.getExpiration("blackList:" + blackListEntities.getUserEntity().getEmail());

                            return expiration != null && expiration > 0;
                        }
                ).toList();

        List<BlackListDto> blackListDtos = blackListEntity.stream().map(BlackListDto::fromEntity)
                .toList();


        Page<BlackListDto> listDtoPage = new PageImpl<>(blackListDtos, pageable, blackListDtos.size());


        return CommonResponse.of("블랙리스트 유저 조회에 성공하였습니다.", listDtoPage);
    }

    private void validateBlackUser(UserEntity targetUser) {
        String data = redisService.getData("blackList:" + targetUser.getEmail());

        boolean exists = blackListRepository.existsByUserEntity_UserId(targetUser.getUserId());

        if (exists && data != null) {
            throw new CustomException(ALREADY_BLACK_USER);
        }
    }

    private UserEntity getUser(Long userId) {
        return userRepository.findByUserIdAndDeletedDateTimeIsNull(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));
    }



    private List<ParticipantGameEntity> handleBlackUserStatus(UserEntity targetUser) {
        LocalDateTime now = LocalDateTime.now();


        List<ParticipantGameEntity> list = participantGameRepository.findByUserEntity_UserIdAndParticipantGameStatusIn(targetUser.getUserId(), List.of(ParticipantGameStatus.ACCEPT, APPLY))
                .stream()
                .filter(participantGameEntity -> participantGameEntity.getGameEntity().getStartDateTime().isAfter(now))
                .toList();


        list.forEach(participantGameEntity -> {
            if (participantGameEntity.getParticipantGameStatus().equals(ACCEPT)) {
                participantGameEntity.kickout(now);
                return;
            }

            if (participantGameEntity.getParticipantGameStatus().equals(APPLY)) {
                participantGameEntity.cancel(now);
            }

        });

        return list;
    }
}
