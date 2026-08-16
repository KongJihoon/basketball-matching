package com.example.basketballmatching.game.service.impl;

import com.example.basketballmatching.game.dto.GameUserLevelDto;
import com.example.basketballmatching.game.repository.query.GameQueryRepository;
import com.example.basketballmatching.game.service.GameUserService;
import com.example.basketballmatching.global.dto.CommonResponse;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.example.basketballmatching.global.exception.ErrorCode.USER_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameUserServiceImpl implements GameUserService {


    private final UserRepository userRepository;

    private final GameQueryRepository gameQueryRepository;






    @Override
    @Transactional(readOnly = true)
    public CommonResponse<GameUserLevelDto> getMyGameUserLevel(Long userId) {

        UserEntity userEntity = getUser(userId);

        GameUserLevelDto gameUserLevelDto = GameUserLevelDto.fromEntity(userEntity);


        return CommonResponse.of("유저 랭크 조회를 완료하였습니다.", gameUserLevelDto);
    }


    private UserEntity getUser(Long userId) {
        return userRepository.findByUserIdAndDeletedDateTimeIsNull(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));
    }







}
