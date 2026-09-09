package com.example.basketballmatching.user.service;

import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.service.RedisService;
import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.dto.*;
import com.example.basketballmatching.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
     * 이메일 인증에 성공한 사용자는 회원가입 처리한다.
     *
     * 처리 과정 :
     * 1. 이메일, 닉네임, 비밀번호 일치 여부 검증
     * 2. Redis에서 이메일 인증 완료 여부 확인 및 재사용 방지 삭제
     * 3. 비밀번호 해싱
     * 4. 사용자 엔티티 생성 및 저장
     *
     * @Transactional
     * 회원가입 중 예외 발생 시 DB 작업을 롤백한다.
     * 단, Redis 작업은 DB트랜잭션에 포함하지 않는다.
     */
    @Transactional
    public SignUpResponse signUp(SignUpRequest request) {

        log.info("유저 회원가입 시작 : {}", request.email());


        /*
         * DTO의 형식 검증(@Valid)와 별개로 비즈니스 규칙을 검증한다.
         * - 이메일 중복 검증
         * - 닉네임 중복 검증
         * - 비밀번호와 비밀번호 확인 일치 검증
         */

        validationByUser(request);


        /*
         * Redis에 email:auth:verified:{email} 키가 존재하는지 확인한다.
         * 인증되지 않은 이메일이면 회원가입을 허용하지 않고 예외 처리
         * 확인된 인증 번호는 재사용하지 않게 Redis에서 제거한다.
         */
        confirmEmailAuth(request);

        /*
         * 평문 비밀번호는 DB에 저장하지 않도록 단방향 해싱한다.
         * 로그인 시 평문 비밀번호와 DB에 저장된 해시값을
         * passwordEncoder.matches(평문, 해시값)로 비교한다.
         */
        String encodedPassword = passwordEncoder.encode(request.password());

        /*
         * 엔티티 생성 규칙을 UserEntity의 정적 팩토리 메서드에 위임한다.
         */
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


        // 이메일 인증을 통과한 사용자이므로 인증 상태를 true로 전환한다.
        userEntity.setEmailAuth();


        UserEntity savedUserEntity = userRepository.save(userEntity);


        log.info("유저 회원가입 완료");

        /*
         * 비밀번호와 같은 민감정보는 노출하지 않도록
         * 응답 DTO로 변환한다.
         */
        return SignUpResponse.fromEntity(savedUserEntity);
    }


    /**
     * 회원가입 시 이메일 중복 여부를 확인한다.
     *
     * @Transactional
     * 단순 중복 검증 조회로 readOnly=true로 DB 쓰기 작업은 제한한다.
     */
    @Transactional(readOnly = true)
    public void checkEmail(String email) {

        boolean exists = userRepository.existsByEmail(email);

        if (exists) {
            throw new CustomException(ALREADY_EXIST_EMAIL);
        }

    }

    /**
     * 회원가입 시 닉네임 중복 여부를 검증한다.
     * 조회 전용 트랜잭션임을 명시한다.
     * JPA 구현체가 불필요한 변경 감지와 flush를 줄일 수 있도록
     * 읽기 전용 힌트를 제공한다.
     * 쓰기를 무조건 차단하는 장치로 보기는 어렵다.
     */
    @Transactional(readOnly = true)
    public void checkNickname(String nickname) {

        boolean exists = userRepository.existsByNickname(nickname);

        if (exists) {
            throw new CustomException(ALREADY_EXIST_NICKNAME);
        }


    }


    /**
     * 로그인된 사용자가 회원 정보 조회 시 사용자의 회원 정보를 반환한다.
     *
     * 처리과정
     * 1. DB 조회 시 deletedAt is Null 검증으로 사용자의 회원 탈퇴 여부를 검증한다.
     * 2. 사용자 검증 후 별도의 응답 DTO를 반환한다.
     **/
    @Transactional(readOnly = true)
    public UserProfileResponse getUserInfo(Long userId) {

        log.info("[유저 정보 조회 시작] userId : {}", userId);

        // JPA를 이용하여 deletedAt is Null로 사용자 탈퇴 여부 검증 후 사용자 조회
        UserEntity userEntity = getUser(userId);


        return UserProfileResponse.fromEntity(userEntity);

    }




    /**
     * 로그인된 사용자는 자신의 회원정보를 변경할 수 있다.
     *
     * 처리과정
     * 1. 사용자 회원 탈퇴 여부 검증 및 사용자를 조회한다.
     * 2. 요청 DTO 닉네임 값의 DB 존재 여부를 검증한다.
     * 3. 사용자 정보 수정 후 별도의 응답 DTO를 반환한다.
     *
     */
    @Transactional
    public UserProfileResponse editUserInfo(Long userId, UpdateUserRequest request) {

        log.info("[유저 회원정보 수정 시작 : {}]", userId);

        // 사용자 회원 탈퇴 여부 검증 후 사용자 조회
        UserEntity userEntity = getUser(userId);


        /*
         * 비즈니스 규칙 상 닉네임은 중복이 불가능 하다.
         * 요청 DTO의 닉네임값의 존재 여부를 검증한다.
         */
        if (request.nickname() != null) {
            boolean exists = userRepository.existsByNicknameAndUserIdNot(request.nickname(), userEntity.getUserId());

            if (exists) {
                throw new CustomException(ALREADY_EXIST_NICKNAME);
            }
        }

        // 요청 DTO의 요청 값의 존재 여부에 따른 DB 정보 수정은 UserEntity에게 위임한다.
        userEntity.editUserInfo(
                request.nickname(),
                request.phone(),
                request.address(),
                request.genderType(),
                request.position()
        );


        log.info("[유저 회원정보 수정 완료 : {}]", true);


        // 검증 완료 및 수정 후 별도의 응답 DTO 반환
        return UserProfileResponse.fromEntity(userEntity);
    }


    /*
     * 사용자의 비밀번호 찾기 이메일 인증 번호와 Redis의 인증번호 일치 여부를 검증한다.
     *
     * 처리과정
     * 1. 사용자 존재 및 회원탈퇴 여부를 검증한다.
     * 2. Redis의 저장된 값과 사용자의 인증번호 일치 여부를 검증한다.
     * 3. 검증 완료 후 사용자의 인증 확인 정보를 Redis에 저장한다.
     */
    @Transactional(readOnly = true)
    public void verifyPasswordCode(String email, String code) {


        // 사용자 존재 및 회원 탈퇴 여부 검증
        userRepository.findByEmailAndDeletedDateTimeIsNull(email)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        String data = redisService.getData(PASSWORD_AUTH_PREFIX + email);

        /**
         * Redis의 password:auth:{email}키 값 존재 여부를 검증한다.
         * 존재 확인 후 사용자 요청 인증 코드와 Redis에 저장된 value값 일치 여부를 검증한다.
         */
        if (data == null || !data.equals(code)) {
            throw new CustomException(EMAIL_NOT_VERIFIED);
        }

        // 인증 확인 후 인증 정보를 Redis에 저장한다.
        redisService.setDataExpireMinutes(PASSWORD_CHANGE_PREFIX + email, code, PASSWORD_CHANGE_EXPIRE_MINUTES);

    }

    /*
     * Redis에 저장된 인증 확인 정보 검증 후 사용자 비밀번호를 변경합니다.
     *
     * 처리과정 :
     * 1. 사용자 존재 및 회원 탈퇴 여부 검증
     * 2. Redis에 저장된 password:change:{email} 키 값 존재 확인
     * 3. 비밀번호와 비밀번호 확인 일치 여부 검증
     * 4. 기존 비밀번호화 새로운 비밀번호 일치 여부 검증
     * 5. 사용자 새로운 비밀번호 저장 후 인증 확인 정보 재사용 방지를 위해 Redis에서 제거
     */
    @Transactional
    public void resetPassword(String email, String newPassword, String checkNewPassword) {

        log.info("[검증 후 비밀번호 변경 시작] email : {}", email);

        UserEntity userEntity = getUser(email);

        String data = redisService.getData(PASSWORD_CHANGE_PREFIX + email);

        // Redis의 저장된 인증 확인 정보 존재 여부 검증
        if (data == null) {
            throw new CustomException(EMAIL_NOT_VERIFIED);
        }

        // 비밀번호와 새로운 비밀번호 일치 여부 검증
        if (!newPassword.equals(checkNewPassword)) {
            throw new CustomException(PASSWORD_NOT_MATCH);
        }

        /**
         * 새로운 비밀번호와 기존 비밀번호의 일치 여부 검증 -> 일치하면 예외 발생
         * 비밀번호를 복호화 하지않고 password.matches()로 일치 검증
         */
        if (passwordEncoder.matches(newPassword, userEntity.getPassword())) {
            throw new CustomException(SAME_AS_OLD_PASSWORD);
        }

        String encodedPassword = passwordEncoder.encode(newPassword);

        /**
         * 모든 검증 완료 후 사용자의 새로운 비밀번호 DB에 저장
         * JPA Dirty Checking으로 DB에 변경사항이 자동으로 반영되므로
         * save()하지 않는다.
         */
        userEntity.setPassword(encodedPassword);


        log.info("[검증 후 비밀번호 변경 완료] email : {}", email);

        // 인증 확인 정보 재사용 방지를 위해 Redis에서 제거
        redisService.deleteData(PASSWORD_CHANGE_PREFIX + email);

    }


    /*
     * 사용자는 현재 비밀번호를 이용하여 새로운 비밀번호로 변경할 수 있습니다.
     *
     * 처리과정 :
     * 1. 요청 DTO의 현재 비밀번호 값과 DB에 저장된 현재 비밀번호 값의 일치 여부를 검증한다.
     * 2. 새로운 비밀번호와 새로운 비밀번호 확인 일치 검증
     * 3. 새로운 비밀번호와 기존 비밀번호가 일치하면 예외 발생
     * 4. 검증 완료 후 새로운 비밀번호 값을 DB에 반영한다.
     */
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {

        log.info("[비밀번호 변경 시작] userId : {}", userId);

        UserEntity userEntity = getUser(userId);


        // 요청 DTO의 기존 비밀번호 값과 DB에 저장된 기존 비밀번호 값의 일치 여부 검증
        if (!passwordEncoder.matches(request.currentPassword(), userEntity.getPassword())) {
            throw new CustomException(PASSWORD_NOT_MATCH);
        }

        // 새로운 비밀번호와 새로운 비밀번호 확인 일치 여부 검증
        if (!request.newPassword().equals(request.newCheckPassword())) {
            throw new CustomException(PASSWORD_NOT_MATCH);
        }

        // 기존 비밀번호와 새로운 비밀번호가 일치하면 예외 발생
        if (passwordEncoder.matches(request.newPassword(), userEntity.getPassword())) {
            throw new CustomException(SAME_AS_OLD_PASSWORD);
        }

        String encodedPassword = passwordEncoder.encode(request.newPassword());

        // 검증 완료 후 결과를 DB에 반영한다.
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
