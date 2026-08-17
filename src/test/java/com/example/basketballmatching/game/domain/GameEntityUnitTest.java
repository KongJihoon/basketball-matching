package com.example.basketballmatching.game.domain;

import com.example.basketballmatching.game.type.FieldStatus;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.type.GenderType;
import com.example.basketballmatching.user.type.LoginProvider;
import com.example.basketballmatching.user.type.Position;
import com.example.basketballmatching.user.type.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.example.basketballmatching.game.type.CityName.SEOUL;
import static com.example.basketballmatching.game.type.GameStatus.CLOSED;
import static com.example.basketballmatching.game.type.GameStatus.RECRUITING;
import static com.example.basketballmatching.game.type.MatchFormat.FIVE_ON_FIVE;
import static com.example.basketballmatching.game.type.MatchFormat.THREE_ON_THREE;
import static com.example.basketballmatching.game.type.MatchGenderType.FEMALE_ONLY;
import static com.example.basketballmatching.game.type.MatchGenderType.MIXED;
import static com.example.basketballmatching.global.exception.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.*;


@DisplayName("GameEntity 경기 수정 단위 테스트")
class GameEntityUnitTest {


    private static final LocalDateTime NOW =
            LocalDateTime.of(
                    2026,
                    8,
                    8,
                    12,
                    0
            );

    private UserEntity creator;

    private GameEntity game;

    @BeforeEach
    void setUp() {
        creator = createUser(1L, "creator@test.com", "경기 생성자");

        game = createGame(creator);

        ParticipantGameEntity.createCreator(
                game, creator, NOW
        );

    }

    @Nested
    @DisplayName("경기 조건 변경")
    class MatchConditionChange {

        @Test
        @DisplayName("생성자 외 참가자가 있으면 예외발생")
        void update_fail_matchFormatChangWithParticipant() {
            // given

            UserEntity participant = createUser(2L, "participant@test.com", "참가자");

            ParticipantGameEntity participation = ParticipantGameEntity.createParticipation(game, participant, NOW);


            // when

            CustomException exception = assertThrows(CustomException.class,
                    () -> game.updateGame(
                            null,
                            null,
                            10,
                            FIVE_ON_FIVE,
                            null,
                            null,
                            null,
                            NOW
                    ));

            // then

            assertEquals(GAME_UPDATE_NOT_ALLOWED, exception.getErrorCode());

        }

        @Test
        @DisplayName("생성자 외 참가자가 있으면 참가 성별 조건을 변경할 수 없다")
        void update_fail_genderChangedWithParticipant() {
            // given
            UserEntity participant =
                    createUser(
                            2L,
                            "participant@test.com",
                            "참가자"
                    );

            ParticipantGameEntity participation = ParticipantGameEntity.createParticipation(
                    game,
                    participant,
                    NOW
            );



            // when
            CustomException exception =
                    assertThrows(
                            CustomException.class,
                            () -> game.updateGame(
                                    null,
                                    null,
                                    null,
                                    null,
                                    FEMALE_ONLY,
                                    null,
                                    null,
                                    NOW
                            )
                    );

            // then
            assertEquals(
                    GAME_UPDATE_NOT_ALLOWED,
                    exception.getErrorCode()
            );
        }


    }

    @Nested
    @DisplayName("경기 정원 변경")
    class HeadCountChange {

        @Test
        @DisplayName("현재 참가 인원보다 정원을 작게 변경할 수 없다")
        void update_fail_headCountLessThanParticipants() {
            // given
            increaseParticipantCount(6);

            // when
            CustomException exception =
                    assertThrows(
                            CustomException.class,
                            () -> game.updateGame(
                                    null,
                                    null,
                                    6,
                                    null,
                                    null,
                                    null,
                                    null,
                                    NOW
                            )
                    );

            // then
            assertEquals(
                    INVALID_HEADCOUNT,
                    exception.getErrorCode()
            );
        }

        @Test
        @DisplayName("마감 경기의 정원을 늘리면 모집 중으로 변경된다")
        void update_success_reopenClosedGame() {
            // given
            increaseParticipantCount(5);

            // when
            game.updateGame(
                    null,
                    null,
                    8,
                    null,
                    null,
                    null,
                    null,
                    NOW
            );

            // then
            assertAll(
                    () -> assertEquals(
                            8,
                            game.getHeadCount()
                    ),
                    () -> assertEquals(
                            RECRUITING,
                            game.getGameStatus()
                    )
            );
        }

        @Test
        @DisplayName("변경 정원과 참가 인원이 같으면 마감으로 변경된다")
        void update_success_closeFullGame() {
            // given
            increaseParticipantCount(5);

            game.updateGame(
                    null,
                    null,
                    8,
                    null,
                    null,
                    null,
                    null,
                    NOW
            );

            // when
            game.updateGame(
                    null,
                    null,
                    6,
                    null,
                    null,
                    null,
                    null,
                    NOW
            );

            // then
            assertEquals(
                    CLOSED,
                    game.getGameStatus()
            );
        }
    }

    @Nested
    @DisplayName("경기 일정 변경")
    class ScheduleChange {

        @Test
        @DisplayName("시작 시각만 입력하면 변경할 수 없다")
        void update_fail_onlyStartDateTime() {
            // given
            LocalDateTime newStartDateTime =
                    NOW.plusDays(3);

            // when
            CustomException exception =
                    assertThrows(
                            CustomException.class,
                            () -> game.updateGame(
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    newStartDateTime,
                                    null,
                                    NOW
                            )
                    );

            // then
            assertEquals(
                    GAME_SCHEDULE_REQUIRED_TOGETHER,
                    exception.getErrorCode()
            );
        }

        @Test
        @DisplayName("24시간 이내 일정으로 변경할 수 없다")
        void update_fail_scheduleWithin24Hours() {
            // given
            LocalDateTime newStartDateTime =
                    NOW.plusHours(23);

            LocalDateTime newEndDateTime =
                    newStartDateTime.plusHours(2);

            // when
            CustomException exception =
                    assertThrows(
                            CustomException.class,
                            () -> game.updateGame(
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    newStartDateTime,
                                    newEndDateTime,
                                    NOW
                            )
                    );

            // then
            assertEquals(
                    GAME_SCHEDULE_TOO_SOON,
                    exception.getErrorCode()
            );
        }

        @Test
        @DisplayName("유효한 일정으로 변경하면 시작·종료 시각이 모두 변경된다")
        void update_success_scheduleChanged() {
            // given
            LocalDateTime newStartDateTime =
                    NOW.plusDays(3);

            LocalDateTime newEndDateTime =
                    newStartDateTime.plusHours(2);

            // when
            game.updateGame(
                    null,
                    null,
                    null,
                    null,
                    null,
                    newStartDateTime,
                    newEndDateTime,
                    NOW
            );

            // then
            assertAll(
                    () -> assertEquals(
                            newStartDateTime,
                            game.getStartDateTime()
                    ),
                    () -> assertEquals(
                            newEndDateTime,
                            game.getEndDateTime()
                    )
            );
        }
    }

    private void increaseParticipantCount(
            int additionalCount
    ) {
        for (int i = 0; i < additionalCount; i++) {
            game.increaseParticipantCount();
        }
    }

    private UserEntity createUser(
            Long userId,
            String email,
            String nickname
    ) {
        return UserEntity.builder()
                .userId(userId)
                .email(email)
                .password("encoded-password")
                .nickname(nickname)
                .name("테스트회원")
                .birth(LocalDate.of(
                        1995,
                        1,
                        1
                ))
                .phone("010-1234-5678")
                .address("서울특별시 송파구")
                .position(Position.GUARD)
                .userType(UserType.USER)
                .genderType(GenderType.MALE)
                .loginProvider(LoginProvider.LOCAL)
                .build();
    }

    private GameEntity createGame(
            UserEntity creator
    ) {
        LocalDateTime startDateTime =
                NOW.plusDays(2);

        return GameEntity.create(
                "테스트 경기",
                "테스트 경기 내용",
                6,
                FieldStatus.INDOOR,
                THREE_ON_THREE,
                MIXED,
                startDateTime,
                startDateTime.plusHours(2),
                "잠실종합운동장 농구장",
                "서울특별시 송파구 올림픽로 25",
                SEOUL,
                37.515,
                127.073,
                creator,
                NOW
        );
    }

}
