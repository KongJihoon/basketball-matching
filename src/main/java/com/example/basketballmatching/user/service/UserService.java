package com.example.basketballmatching.user.service;

import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.global.dto.CommonResponse;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.security.TokenProvider;
import com.example.basketballmatching.global.service.RedisService;
import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.dto.ChangePasswordDto;
import com.example.basketballmatching.user.dto.EditUserDto;
import com.example.basketballmatching.user.dto.SignUpDto;
import com.example.basketballmatching.user.dto.UserDto;
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
    public SignUpDto.Response signUp(SignUpDto.Request request) {

        log.info("유저 회원가입 시작 : {}", request.getEmail());


        // 유저 유효성 검사
        validationByUser(request);

        // 이메일 인증 여부 확인(Redis)
        confirmEmailAuth(request);

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        UserEntity userEntity = UserEntity.create(
                request.getEmail(),
                encodedPassword,
                request.getNickname(),
                request.getName(),
                request.getBirth(),
                request.getPhone(),
                request.getAddress(),
                request.getPosition(),
                request.getGenderType()
        );


        userEntity.setEmailAuth();


        userRepository.save(userEntity);

        UserDto userDto = UserDto.fromEntity(userEntity);

        log.info("유저 회원가입 완료");

        return SignUpDto.Response.fromDto(userDto);
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
    public UserDto getUserInfo(Long userId) {

        log.info("[유저 정보 조회 시작] userId : {}", userId);

        UserEntity userEntity = getUser(userId);

        UserDto userDto = UserDto.fromEntity(userEntity);

        return userDto;

    }




    /**
     * 회원 정보 수정
     */

    @Transactional
    public UserDto editUserInfo(Long userId, EditUserDto editUserDto) {

        log.info("[유저 회원정보 수정 시작 : {}]", userId);

        UserEntity userEntity = getUser(userId);


        if (editUserDto.getNickname() != null) {
            boolean exists = userRepository.existsByNicknameAndUserIdNot(editUserDto.getNickname(), userEntity.getUserId());
            if (exists) {
                throw new CustomException(ALREADY_EXIST_NICKNAME);
            }
        }

        userEntity.editUserInfo(
                editUserDto.getNickname(),
                editUserDto.getPhone(),
                editUserDto.getAddress(),
                editUserDto.getGenderType(),
                editUserDto.getPosition()
        );

        UserDto userDto = UserDto.fromEntity(userEntity);

        log.info("[유저 회원정보 수정 완료 : {}]", true);

        return userDto;
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
    public void changePassword(Long userId, ChangePasswordDto request) {

        log.info("[비밀번호 변경 시작] userId : {}", userId);

        UserEntity userEntity = getUser(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), userEntity.getPassword())) {
            throw new CustomException(PASSWORD_NOT_MATCH);
        }

        if (!request.getNewPassword().equals(request.getNewCheckPassword())) {
            throw new CustomException(PASSWORD_NOT_MATCH);
        }

        if (passwordEncoder.matches(request.getNewPassword(), userEntity.getPassword())) {
            throw new CustomException(SAME_AS_OLD_PASSWORD);
        }

        String encodedPassword = passwordEncoder.encode(request.getNewPassword());

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

    private void validationByUser(SignUpDto.Request request) {
        boolean existsByEmail = userRepository.existsByEmail(request.getEmail());

        boolean existsByNickname = userRepository.existsByNickname(request.getNickname());

        if (existsByEmail) {
            throw new CustomException(ALREADY_EXIST_EMAIL);
        }

        if (existsByNickname) {
            throw new CustomException(ALREADY_EXIST_NICKNAME);
        }

        if (!request.getPassword().equals(request.getCheckPassword())) {
            throw new CustomException(PASSWORD_NOT_MATCH);
        }
    }

    private void confirmEmailAuth(SignUpDto.Request request) {
        String data = redisService.getData(EMAIL_VERIFIED_PREFIX + request.getEmail());

        if (data == null) {
            throw new CustomException(EMAIL_NOT_VERIFIED);
        }

        redisService.deleteData(EMAIL_VERIFIED_PREFIX + request.getEmail());
    }
}
