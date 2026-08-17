package com.example.basketballmatching.game.service;


import com.example.basketballmatching.game.dto.response.MyCompletedGameResponse;
import com.example.basketballmatching.game.dto.response.MyUpcomingGameResponse;
import com.example.basketballmatching.game.repository.query.GameQueryRepository;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

import static com.example.basketballmatching.global.exception.ErrorCode.USER_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Slf4j
public class MyGameService {

    private final UserRepository userRepository;
    private final GameQueryRepository gameQueryRepository;
    private final Clock clock;


    @Transactional(readOnly = true)
    public Page<MyUpcomingGameResponse> getUpcomingGames(Long userId, Pageable pageable) {

        log.info("[내 예정 경기 조회 시작] userId={}, page={}, size={}", userId, pageable.getPageNumber(), pageable.getPageSize());


        validateActiveUser(userId);

        LocalDateTime now = LocalDateTime.now(clock);

        Page<MyUpcomingGameResponse> response = gameQueryRepository.findUpcomingGamesByUser(userId, pageable, now)
                .map(participation -> MyUpcomingGameResponse.fromEntity(participation, userId));


        log.info("[내 예정 경기 조회 완료] userId={}, page={}, size={}", userId, pageable.getPageNumber(), pageable.getPageSize());

        return response;
    }

    private void validateActiveUser(Long userId) {

        if (!userRepository.existsByUserIdAndDeletedDateTimeIsNull(userId)) {
            throw new CustomException(USER_NOT_FOUND);
        }
    }

    @Transactional(readOnly = true)
    public Page<MyCompletedGameResponse> getCompletedGames(Long userId, Pageable pageable) {

        log.info("[내 지난 경기 조회 시작] userId={}, page={}, size={}", userId, pageable.getPageNumber(), pageable.getPageSize());

        validateActiveUser(userId);

        LocalDateTime now = LocalDateTime.now(clock);

        Page<MyCompletedGameResponse> response = gameQueryRepository.findCompletedGamesByUser(userId, pageable, now)
                .map(participation -> MyCompletedGameResponse.fromEntity(participation, userId));



        log.info("[내 지난 경기 조회 완료] userId={}, totalElements={}", userId, response.getTotalElements());

        return response;
    }
}
