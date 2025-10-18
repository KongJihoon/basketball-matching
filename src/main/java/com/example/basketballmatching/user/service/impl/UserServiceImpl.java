package com.example.basketballmatching.user.service.impl;

import com.example.basketballmatching.global.dto.ApiResponse;
import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.service.RedisService;
import com.example.basketballmatching.user.dto.ChangePasswordDto;
import com.example.basketballmatching.user.dto.EditUserDto;
import com.example.basketballmatching.user.dto.SignUpDto;
import com.example.basketballmatching.user.dto.UserDto;
import com.example.basketballmatching.user.entity.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import com.example.basketballmatching.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.example.basketballmatching.global.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final PasswordEncoder passwordEncoder;

    private final UserRepository userRepository;

    private final RedisService redisService;


    /**
     * 유저 회원가입
     */
    @Override
    @Transactional
    public ApiResponse<SignUpDto.Response> signUp(SignUpDto.Request request) {

        log.info("유저 회원가입 시작 : {}", request.getEmail());

        boolean existsByEmail = userRepository.existsByEmail(request.getEmail());

        boolean existsByNickname = userRepository.existsByNickname(request.getNickname());

        validationByUser(request, existsByEmail, existsByNickname);

        request.setPassword(passwordEncoder.encode(request.getPassword()));

        UserEntity userEntity = SignUpDto.Request.toEntity(request);

        String data = redisService.getData("email:auth:verified:" + request.getEmail());

        if (data == null) {
            throw new CustomException(EMAIL_NOT_VERIFIED);
        }

        redisService.deleteData("email:auth:verified:" + request.getEmail());

        userEntity.setEmailAuth();


        userRepository.save(userEntity);

        log.info("유저 회원가입 완료");

        return ApiResponse.of("회원가입에 성공하였습니다.", SignUpDto.Response.fromDto(UserDto.fromEntity(userEntity)));
    }

    /**
     * 이메일 중복 확인
     */

    @Override
    public CheckResponse checkEmail(String email) {

        boolean exists = userRepository.existsByEmail(email);

        if (exists) {
            throw new CustomException(ALREADY_EXIST_EMAIL);
        }

        return CheckResponse.of(true, "사용가능한 이메일입니다.");
    }

    /**
     * 닉네입 중복 확인
     */

    @Override
    public CheckResponse checkNickname(String nickname) {

        boolean exists = userRepository.existsByNickname(nickname);

        if (exists) {
            throw new CustomException(ALREADY_EXIST_NICKNAME);
        }

        return CheckResponse.of(true, "사용가능한 닉네임입니다.");

    }

    /**
     * 회원 정보 조회
     */
    @Override
    public ApiResponse<UserDto> getUserInfo(Long userId) {

        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        UserDto userDto = UserDto.fromEntity(userEntity);


        return ApiResponse.of("회원정보 조회에 성공하였습니다.", userDto);
    }


    /**
     * 회원 정보 수정
     */
    @Override
    @Transactional
    public ApiResponse<UserDto> editUserInfo(Long userId, EditUserDto editUserDto) {

        log.info("[유저 회원정보 수정 시작 : {}]", userId);

        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        boolean exists = userRepository.existsByNickname(editUserDto.getNickname());

        if (editUserDto.getNickname() != null && exists) {
            throw new CustomException(ALREADY_EXIST_NICKNAME);
        }

        userEntity.editUserInfo(editUserDto);


        UserDto userDto = UserDto.fromEntity(userEntity);

        log.info("[유저 회원정보 수정 완료 : {}]", true);

        return ApiResponse.of("회원정보 수정이 완료되었습니다.", userDto);
    }


    /**
     * 비밀번호 찾기 인증번호 확인
     */
    @Override
    public CheckResponse verifyPasswordCode(String email, String code) {


        userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        String data = redisService.getData("password:auth:" + email);

        if (data == null || !data.equals(code)) {
            throw new CustomException(EMAIL_NOT_VERIFIED);
        }

        // 인증 확인 후 레디스에 비밀번호 변경 가능 저장

        redisService.setDataExpireMinutes("password:change:" + email, code, 10L);




        return CheckResponse.of(true, "비밀번호를 변경해주세요.");
    }

    /**
     * 검증 후 비밀번호 찾기
     */
    @Override
    @Transactional
    public CheckResponse resetPassword(String email, String newPassword, String checkNewPassword) {

        log.info("[검증 후 비밀번호 변경 시작] email : {}", email);

        UserEntity userEntity = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        String data = redisService.getData("password:change:" + email);

        if (data == null) {
            throw new CustomException(EMAIL_NOT_VERIFIED);
        }

        if (!newPassword.equals(checkNewPassword)) {
            throw new CustomException(PASSWORD_NOT_MATCH);
        }


        userEntity.setPassword(passwordEncoder.encode(newPassword));

        userRepository.save(userEntity);

        redisService.deleteData("password:change:" + email);

        return CheckResponse.of(true, "비밀번호 변경을 완료하였습니다.");
    }

    @Override
    @Transactional
    public CheckResponse changePassword(Long userId, ChangePasswordDto request) {

        log.info("[비밀번호 변경 시작] userId : {}", userId);

        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getCurrentPassword(), userEntity.getPassword())) {
            throw new CustomException(PASSWORD_NOT_MATCH);
        }

        if (!request.getNewPassword().equals(request.getNewCheckPassword())) {
            throw new CustomException(PASSWORD_NOT_MATCH);
        }


        userEntity.setPassword(passwordEncoder.encode(request.getNewPassword()));

        userRepository.save(userEntity);

        log.info("[비밀번호 변경 완료] userId : {}", userId);


        return CheckResponse.of(true, "비밀번호 변경을 완료하였습니다.");
    }


    private static void validationByUser(SignUpDto.Request request, boolean existsByEmail, boolean existsByNickname) {
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
}
