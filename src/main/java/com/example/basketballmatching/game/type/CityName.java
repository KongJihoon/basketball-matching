package com.example.basketballmatching.game.type;

import com.example.basketballmatching.global.exception.CustomException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

import static com.example.basketballmatching.global.exception.ErrorCode.UNSUPPORTED_CITY;

@Getter
@RequiredArgsConstructor
public enum CityName {

    SEOUL("서울특별시"),
    GYEONGGI("경기도"),
    INCHEON("인천광역시"),
    GANGWON("강원특별자치도"),
    DAEJEON("대전광역시"),
    SEJONG("세종특별자치시"),
    CHUNGNAM("충청남도"),
    CHUNGBUK("충청북도"),
    DAEGU("대구광역시"),
    GYEONGBUK("경상북도"),
    GYEONGNAM("경상남도"),
    BUSAN("부산광역시"),
    ULSAN("울산광역시"),
    GWANGJU("광주광역시"),
    JEONNAM("전라남도"),
    JEONBUK("전북특별자치도"),
    JEJU("제주특별자치도");

    private final String cityName;

    public static CityName fromAddress(
            String address
    ) {
        if (address == null || address.isBlank()) {
            throw new CustomException(
                    UNSUPPORTED_CITY
            );
        }

        String cityPrefix = address
                .trim()
                .split("\\s+")[0];

        return Arrays.stream(values())
                .filter(city ->
                        city.cityName.equals(cityPrefix)
                )
                .findFirst()
                .orElseThrow(() ->
                        new CustomException(
                                UNSUPPORTED_CITY
                        )
                );
    }
}