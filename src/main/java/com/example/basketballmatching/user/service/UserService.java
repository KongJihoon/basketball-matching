package com.example.basketballmatching.user.service;

import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.global.dto.CommonResponse;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.security.TokenProvider;
import com.example.basketballmatching.global.service.RedisService;
import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.dto.*;
import com.example.basketballmatching.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.example.basketballmatching.global.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private static final String EMAIL_VERIFIED_PREFIX =
            "email:auth:verified:";

    private static final String PASSWORD_AUTH_PREFIX =
            "password:auth:";

    private static final String PASSWORD_CHANGE_PREFIX =
            "password:change:";

    private static final long PASSWORD_CHANGE_EXPIRE_MINUTES = 10L;

    private final PasswordEncoder passwordEncoder;

    private final UserRepository userRepository;

    private final RedisService redisService;


    /**
     * 유저 회원가입
     */

    @Transactional
    public SignUpResponse signUp(SignUpRequest request) {

        log.info("유저 회원가입 시작 : {}", request.email());


        // 유저 유효성 검사
        validationByUser(request);

        // 이메일 인증 여부 확인(Redis)
        confirmEmailAuth(request);

        String encodedPassword = passwordEncoder.encode(request.password());

        UserEntity userEntity = UserEntity.create(
                request.email(),
                encodedPassword,
                request.nickname(),
                request.name(),
                request.birth(),
                request.phone(),
                request.address(),
                request.position(),
                request.genderType()
        );


        userEntity.setEmailAuth();


        UserEntity savedUserEntity = userRepository.save(userEntity);


        log.info("유저 회원가입 완료");

        return SignUpResponse.fromEntity(savedUserEntity);
    }


    /**
     * 이메일 중복 확인
     */


    public void checkEmail(String email) {

        boolean exists = userRepository.existsByEmail(email);

        if (exists) {
            throw new CustomException(ALREADY_EXIST_EMAIL);
        }

    }

    /**
     * 닉네입 중복 확인
     */
    public void checkNickname(String nickname) {

        boolean exists = userRepository.existsByNickname(nickname);

        if (exists) {
            throw new CustomException(ALREADY_EXIST_NICKNAME);
        }


    }


    /**
     * 회원 정보 조회
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getUserInfo(Long userId) {

        log.info("[유저 정보 조회 시작] userId : {}", userId);

        UserEntity userEntity = getUser(userId);


        return UserProfileResponse.fromEntity(userEntity);

    }




    /**
     * 회원 정보 수정
     */

    @Transactional
    public UserProfileResponse editUserInfo(Long userId, UpdateUserRequest request) {

        log.info("[유저 회원정보 수정 시작 : {}]", userId);

        UserEntity userEntity = getUser(userId);


        if (request.nickname() != null) {
            boolean exists = userRepository.existsByNicknameAndUserIdNot(request.nickname(), userEntity.getUserId());

            if (exists) {
                throw new CustomException(ALREADY_EXIST_NICKNAME);
            }
        }

        userEntity.editUserInfo(
                request.nickname(),
                request.phone(),
                request.address(),
                request.genderType(),
                request.position()
        );


        log.info("[유저 회원정보 수정 완료 : {}]", true);

        return UserProfileResponse.fromEntity(userEntity);
    }


    /**
     * 비밀번호 찾기 인증번호 확인
     */
    public void verifyPasswordCode(String email, String code) {


        userRepository.findByEmailAndDeletedDateTimeIsNull(email)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        String data = redisService.getData(PASSWORD_AUTH_PREFIX + email);

        if (data == null || !data.equals(code)) {
            throw new CustomException(EMAIL_NOT_VERIFIED);
        }

        // 인증 확인 후 레디스에 비밀번호 변경 가능 저장

        redisService.setDataExpireMinutes(PASSWORD_CHANGE_PREFIX + email, code, PASSWORD_CHANGE_EXPIRE_MINUTES);


    }

    /**
     * 검증 후 비밀번호 리셋
     */

    @Transactional
    public void resetPassword(String email, String newPassword, String checkNewPassword) {

        log.info("[검증 후 비밀번호 변경 시작] email : {}", email);

        UserEntity userEntity = getUser(email);

        String data = redisService.getData(PASSWORD_CHANGE_PREFIX + email);

        if (data == null) {
            throw new CustomException(EMAIL_NOT_VERIFIED);
        }

        if (!newPassword.equals(checkNewPassword)) {
            throw new CustomException(PASSWORD_NOT_MATCH);
        }

        if (passwordEncoder.matches(newPassword, userEntity.getPassword())) {
            throw new CustomException(SAME_AS_OLD_PASSWORD);
        }

        String encodedPassword = passwordEncoder.encode(newPassword);

        userEntity.setPassword(encodedPassword);


        redisService.deleteData(PASSWORD_CHANGE_PREFIX + email);

    }


    /**
     * 비밃번호 변경
     */

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {

        log.info("[비밀번호 변경 시작] userId : {}", userId);

        UserEntity userEntity = getUser(userId);

        if (!passwordEncoder.matches(request.currentPassword(), userEntity.getPassword())) {
            throw new CustomException(PASSWORD_NOT_MATCH);
        }

        if (!request.newPassword().equals(request.newCheckPassword())) {
            throw new CustomException(PASSWORD_NOT_MATCH);
        }

        if (passwordEncoder.matches(request.newPassword(), userEntity.getPassword())) {
            throw new CustomException(SAME_AS_OLD_PASSWORD);
        }

        String encodedPassword = passwordEncoder.encode(request.newPassword());

        userEntity.setPassword(encodedPassword);


        log.info("[비밀번호 변경 완료] userId : {}", userId);


    }




    private UserEntity getUser(Long userId) {
        return userRepository.findByUserIdAndDeletedDateTimeIsNull(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));
    }


    private UserEntity getUser(String email) {
        return userRepository.findByEmailAndDeletedDateTimeIsNull(email)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));
    }

    private void validationByUser(SignUpRequest request) {
        boolean existsByEmail = userRepository.existsByEmail(request.email());

        boolean existsByNickname = userRepository.existsByNickname(request.nickname());

        if (existsByEmail) {
            throw new CustomException(ALREADY_EXIST_EMAIL);
        }

        if (existsByNickname) {
            throw new CustomException(ALREADY_EXIST_NICKNAME);
        }

        if (!request.password().equals(request.checkPassword())) {
            throw new CustomException(PASSWORD_NOT_MATCH);
        }
    }

    private void confirmEmailAuth(SignUpRequest request) {

        String key = EMAIL_VERIFIED_PREFIX + request.email();

        String data = redisService.getData(key);

        if (data == null) {
            throw new CustomException(EMAIL_NOT_VERIFIED);
        }

        redisService.deleteData(key);
    }
}
