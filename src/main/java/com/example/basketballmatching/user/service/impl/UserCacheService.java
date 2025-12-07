package com.example.basketballmatching.user.service.impl;

import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.user.dto.UserDto;
import com.example.basketballmatching.user.entity.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.example.basketballmatching.global.exception.ErrorCode.USER_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserCacheService {

    private final UserRepository userRepository;



    @Cacheable(cacheNames = "userDto", key = "#userId", unless = "#result == null")
    @Transactional(readOnly = true)
    public UserDto getUserDtoCached(Long userId) {

        log.info("[유저 캐시 정보 조회] userId : {}", userId);

        UserEntity userEntity = userRepository.findByUserIdAndDeletedDateTimeIsNull(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));
        return UserDto.fromEntity(userEntity);
    }
}
