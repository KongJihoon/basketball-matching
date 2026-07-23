package com.example.basketballmatching.gameCreator.service.impl;

import com.example.basketballmatching.gameCreator.dto.CreateGameDto;
import com.example.basketballmatching.gameCreator.repository.GameRepository;
import com.example.basketballmatching.gameCreator.repository.ParticipantGameRepository;
import com.example.basketballmatching.gameCreator.service.GameService;
import com.example.basketballmatching.gameCreator.type.*;
import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import com.example.basketballmatching.user.type.GenderType;
import com.example.basketballmatching.user.type.LoginProvider;
import com.example.basketballmatching.user.type.Position;
import com.example.basketballmatching.user.type.UserType;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

@SpringBootTest
@ActiveProfiles("test-only")
class GameConcurrencyTest {

    @Autowired
    private GameService gameService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private ParticipantGameRepository participantGameRepository;

    @BeforeEach
    void tearDown() {
        participantGameRepository.deleteAll();
        gameRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("경기 생성 동시성 이슈 테스트 - 같은 장소 같은 시간")
    void createGameConcurrencyTest() throws InterruptedException {
        // given

        int threadCount = 100;

        List<Long> userIds = IntStream.range(0, threadCount)
                .mapToObj(this::saveUser)
                .map(UserEntity::getUserId)
                .toList();


        // when

        LocalDateTime start = LocalDateTime.now().plusMinutes(60).withSecond(0).withNano(0);

        LocalDateTime end = start.plusMinutes(90);


        // then

        CreateGameDto.Request req = CreateGameDto.Request.builder()
                .title("동시성 테스트")
                .content("동시 생성")
                .headCount(10)
                .matchFormat(MatchFormat.FIVE_ON_FIVE)
                .fieldStatus(FieldStatus.OUTDOOR)
                .matchGenderType(MatchGenderType.MIXED)
                .placeName("테스트코트12323")
                .address("서울특별시 강남구2 어딘가12323")
                .latitude(37.0)
                .longitude(127.0)
                .startDateTime(start)
                .endDateTime(end)
                .build();

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(threadCount);

        List<Throwable> errors = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount; i++) {

            final int idx = i;

            pool.submit(() -> {

                try {
                    startGate.await();
                    gameService.createGame(userIds.get(idx), req);
                } catch (Throwable t) {
                    errors.add(t);

                } finally {
                  doneGate.countDown();
                }

            });

        }
        startGate.countDown();
        doneGate.await();
        pool.shutdown();

        long count = gameRepository.countByPlaceNameAndAddressAndStartDateTimeAndEndDateTime(req.getPlaceName(), req.getAddress(), req.getStartDateTime(), req.getEndDateTime());

        Assertions.assertEquals(1L, count);

        Assertions.assertEquals(threadCount - 1, errors.size());

    }



    private UserEntity saveUser(int i) {
        return userRepository.save(
                UserEntity.builder()
                        .email("user" + i + "@test.com")
                        .password("pw")
                        .nickname("nick" + i)
                        .name("name" + i)
                        .birth(LocalDate.of(1997, 1, 1))
                        .phone("010-0000-000" + i)
                        .address("서울")
                        .position(Position.NONE)
                        .genderType(GenderType.NONE)
                        .userType(UserType.USER)
                        .loginProvider(LoginProvider.LOCAL)
                        .emailAuth(true)
                        .build()
        );
    }
}
