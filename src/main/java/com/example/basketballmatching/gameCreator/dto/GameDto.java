package com.example.basketballmatching.gameCreator.dto;

import com.example.basketballmatching.gameCreator.entity.GameEntity;
import com.example.basketballmatching.gameCreator.type.*;
import com.example.basketballmatching.gameUsers.type.GameUserLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class GameDto {

    @Schema(name = "경기 PK", example = "1")
    private Long gameId;

    @Schema(name = "경기 제목", example = "서울 xx체육관 3vs3 경기 인원 모집", defaultValue = "서울 xx체육관 3vs3 경기 인원 모집")
    private String title;

    @Schema(name = "경기 상세 내용", example = "3대3경기 인원 모집입니다.", defaultValue = "3대3경기 인원 모집입니다.")
    private String content;

    @Schema(name = "경기 인원 수", example = "6")
    private int headCount;

    @Schema(name = "경기 참가 인원 수", example = "1", defaultValue = "1")
    private int participantCount;

    @Schema(name = "경기 실내외", example = "INDOOR")
    private FieldStatus fieldStatus;

    @Schema(name = "경기 형식", example = "THREE_ON_THREE")
    private MatchFormat matchFormat;

    @Schema(name = "경기 상태", example = "RECRUITING")
    private GameStatus gameStatus;

    @Schema(name = "경기 시작 날짜", example = "2025-11-22T15:00:00:00")
    private LocalDateTime startDateTime;

    @Schema(name = "경기 종료 날짜", example = "2025-11-22T17:00:00:00")
    private LocalDateTime endDateTime;


    @Schema(name = "경기 장소", example = "서울 xx 체육관")
    private String placeName;

    @Schema(name = "경기 주소", example = "서울특별시 강남구..")
    private String address;

    private Double latitude;

    private Double longitude;

    @Schema(name = "도시 이름", example = "INCHEON")
    private CityName cityName;

    @Schema(name = "경기 주소", example = "서울특별시 강남구..")
    private MatchGenderType matchGenderType;

    @Schema(name = "경기 생성자 아이디", example = "1")
    private Long creatorId;

    @Schema(name = "경기 생성자 닉네임", example = "커리")
    private String creatorNickname;

    @Schema(name = "경기 참가자 수준", example = "BEGINNER")
    private GameUserLevel gameUserLevel;



    public static GameDto fromEntity(GameEntity gameEntity) {

        return GameDto.builder()
                .gameId(gameEntity.getGameId())
                .title(gameEntity.getTitle())
                .content(gameEntity.getContent())
                .headCount(gameEntity.getHeadCount())
                .participantCount(gameEntity.getParticipantCount())
                .fieldStatus(gameEntity.getFieldStatus())
                .matchFormat(gameEntity.getMatchFormat())
                .gameStatus(gameEntity.getGameStatus())
                .startDateTime(gameEntity.getStartDateTime())
                .endDateTime(gameEntity.getEndDateTime())
                .placeName(gameEntity.getPlaceName())
                .address(gameEntity.getAddress())
                .latitude(gameEntity.getLatitude())
                .longitude(gameEntity.getLongitude())
                .cityName(gameEntity.getCityName())
                .matchGenderType(gameEntity.getMatchGenderType())
                .gameUserLevel(gameEntity.getGameUserLevel())
                .creatorId(gameEntity.getUserEntity().getUserId())
                .creatorNickname(gameEntity.getUserEntity().getNickname())
                .build();

    }
}
