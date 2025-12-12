package com.example.basketballmatching.user.service.impl;

import com.example.basketballmatching.gameCreator.entity.GameEntity;
import com.example.basketballmatching.gameCreator.entity.ParticipantGameEntity;
import com.example.basketballmatching.gameCreator.repository.GameQueryRepository;
import com.example.basketballmatching.gameCreator.repository.GameRepository;
import com.example.basketballmatching.gameCreator.repository.ParticipantGameRepository;
import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.global.dto.CommonResponse;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.security.TokenProvider;
import com.example.basketballmatching.global.service.RedisService;
import com.example.basketballmatching.notifications.service.NotificationService;
import com.example.basketballmatching.user.dto.ChangePasswordDto;
import com.example.basketballmatching.user.dto.EditUserDto;
import com.example.basketballmatching.user.dto.SignUpDto;
import com.example.basketballmatching.user.dto.UserDto;
import com.example.basketballmatching.user.entity.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import com.example.basketballmatching.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.example.basketballmatching.gameCreator.type.ParticipantGameStatus.*;
import static com.example.basketballmatching.global.exception.ErrorCode.*;
import static com.example.basketballmatching.notifications.type.NotificationType.DELETE_GAME;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final PasswordEncoder passwordEncoder;

    private final UserRepository userRepository;

    private final RedisService redisService;

    private final UserCacheService userCacheService;

    private final TokenProvider tokenProvider;

    private final ParticipantGameRepository participantGameRepository;
    private final GameRepository gameRepository;
    private final NotificationService notificationService;
    private final GameQueryRepository gameQueryRepository;



    /**
     * 유저 회원가입
     */
    @Override
    @Transactional
    public CommonResponse<SignUpDto.Response> signUp(SignUpDto.Request request) {

        log.info("유저 회원가입 시작 : {}", request.getEmail());


        // 유저 유효성 검사
        validationByUser(request);

        // 이메일 인증 여부 확인(Redis)
        confirmEmailAuth(request);

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        UserEntity userEntity = SignUpDto.Request.toEntity(request, encodedPassword);


        userEntity.setEmailAuth();


        userRepository.save(userEntity);

        log.info("유저 회원가입 완료");

        return CommonResponse.of("회원가입에 성공하였습니다.", SignUpDto.Response.fromDto(UserDto.fromEntity(userEntity)));
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
    @Transactional(readOnly = true)
    public CommonResponse<UserDto> getUserInfo(Long userId) {

        log.info("[유저 정보 조회 시작] userId : {}", userId);

        UserDto userDto = userCacheService.getUserDtoCached(userId);

        return CommonResponse.of("회원정보 조회에 성공하였습니다.", userDto);
    }




    /**
     * 회원 정보 수정
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = "userDto", key = "#userId")
    public CommonResponse<UserDto> editUserInfo(Long userId, EditUserDto editUserDto) {

        log.info("[유저 회원정보 수정 시작 : {}]", userId);

        UserEntity userEntity = getUser(userId);


        if (editUserDto.getNickname() != null) {
            boolean exists = userRepository.existsByNicknameAndUserIdNot(editUserDto.getNickname(), userEntity.getUserId());
            if (exists) {
                throw new CustomException(ALREADY_EXIST_NICKNAME);
            }
        }

        userEntity.editUserInfo(editUserDto);

        UserDto userDto = UserDto.fromEntity(userEntity);

        log.info("[유저 회원정보 수정 완료 : {}]", true);

        return CommonResponse.of("회원정보 수정이 완료되었습니다.", userDto);
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

        UserEntity userEntity = getUser(email);

        String data = redisService.getData("password:change:" + email);

        if (data == null) {
            throw new CustomException(EMAIL_NOT_VERIFIED);
        }

        if (!newPassword.equals(checkNewPassword)) {
            throw new CustomException(PASSWORD_NOT_MATCH);
        }

        String encodedPassword = passwordEncoder.encode(newPassword);

        userEntity.setPassword(encodedPassword);

        userRepository.save(userEntity);

        redisService.deleteData("password:change:" + email);

        return CheckResponse.of(true, "비밀번호 변경을 완료하였습니다.");
    }



    @Override
    @Transactional
    public CheckResponse changePassword(Long userId, ChangePasswordDto request) {

        log.info("[비밀번호 변경 시작] userId : {}", userId);

        UserEntity userEntity = getUser(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), userEntity.getPassword())) {
            throw new CustomException(PASSWORD_NOT_MATCH);
        }

        if (!request.getNewPassword().equals(request.getNewCheckPassword())) {
            throw new CustomException(PASSWORD_NOT_MATCH);
        }
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());

        userEntity.setPassword(encodedPassword);

        userRepository.save(userEntity);

        log.info("[비밀번호 변경 완료] userId : {}", userId);


        return CheckResponse.of(true, "비밀번호 변경을 완료하였습니다.");
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "userDto", key = "#userId")
    public CheckResponse deleteUser(Long userId, String token) {

        UserEntity userEntity = getUser(userId);

        // 토큰 유효성 검사 및 토큰 로그아웃 처리
        deleteTokenByUser(token, userEntity);

        LocalDateTime now = LocalDateTime.now();


        // 삭제 유저 게임 삭제 및 참가 유저 강퇴 처리
        deleteCreatorGameAndParticipantUsers(userEntity, now);


        // 삭제 유저 참가 게임 강퇴 및 취소 처리
        deleteParticipantByDeleteUser(userEntity, now);



        userEntity.setDeletedDateTime(now);

        userRepository.save(userEntity);


        return CheckResponse.of(true, "회원탈퇴에 성공하였습니다.");
    }

    private void deleteTokenByUser(String token, UserEntity userEntity) {
        if (token == null) {
            throw new CustomException(NOT_FOUND_TOKEN);
        }

        redisService.setDataExpireMillis("logout:access:" + token, "LOGOUT", tokenProvider.getRemainingTime(token));

        redisService.deleteData("refreshToken:" + userEntity.getEmail());
    }

    private void deleteParticipantByDeleteUser(UserEntity userEntity, LocalDateTime now) {
        List<ParticipantGameEntity> participantGameEntities = participantGameRepository.findByUserEntity_UserIdAndParticipantGameStatusIn(userEntity.getUserId(), List.of(ACCEPT, APPLY))
                .stream().filter(participantGameEntity -> participantGameEntity.getGameEntity().getStartDateTime().isAfter(now)).toList();


        participantGameEntities.forEach(
                participantGameEntity -> {
                    if (participantGameEntity.getParticipantGameStatus().equals(APPLY)) {
                        participantGameEntity.setParticipantGameStatusAndCanceledDateTime(CANCEL, now);

                    }

                    if (participantGameEntity.getParticipantGameStatus().equals(ACCEPT)) {
                        participantGameEntity.setParticipantGameStatusAndKickoutDateTime(KICKOUT, now);


                    }
                }
        );

        participantGameRepository.saveAll(participantGameEntities);


    }


    private void deleteCreatorGameAndParticipantUsers(UserEntity userEntity, LocalDateTime now) {
        List<GameEntity> gameEntities = gameRepository.findByUserEntity_UserIdAndDeletedDateTimeIsNull(userEntity.getUserId())
                .stream().filter(gameEntity -> gameEntity.getStartDateTime().isAfter(now))
                .toList();

        List<ParticipantGameEntity> deleteGameParticipants = new ArrayList<>();

        gameEntities.forEach(gameEntity -> {

            List<ParticipantGameEntity> participantUsers = gameQueryRepository.getParticipantUsers(gameEntity.getGameId(), now);


            deleteGameParticipants.addAll(participantUsers);

            participantUsers.stream()
                    .filter(participantGameEntity -> !Objects.equals(participantGameEntity.getUserEntity().getUserId(), userEntity.getUserId()))
                    .forEach(
                            participantGameEntity -> notificationService.send(DELETE_GAME, participantGameEntity.getUserEntity(), participantGameEntity.getGameEntity().getTitle() + "의 게임이 삭제되었습니다."));
            gameEntity.setDeletedDateTime(now);


        });

        participantGameRepository.saveAll(deleteGameParticipants);

        gameRepository.saveAll(gameEntities);

    }

    private UserEntity getUser(Long userId) {
        return userRepository.findByUserIdAndDeletedDateTimeIsNull(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));
    }


    private UserEntity getUser(String email) {
        return userRepository.findByEmail(email)
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
        String data = redisService.getData("email:auth:verified:" + request.getEmail());

        if (data == null) {
            throw new CustomException(EMAIL_NOT_VERIFIED);
        }

        redisService.deleteData("email:auth:verified:" + request.getEmail());
    }
}
