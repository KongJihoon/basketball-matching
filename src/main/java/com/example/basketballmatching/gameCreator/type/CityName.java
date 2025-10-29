package com.example.basketballmatching.gameCreator.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
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
    JEONNAM("전남특별자치도"),
    JEONBUK("전북특별자치도"),
    JEJU("제주특별자치도");

    private String cityName;


    public static CityName getCityName(String address) {

        String[] parts = address.split(" ");

        String cityName = parts[0];

        for (CityName cityNames : CityName.values()) {

            if (cityNames.getCityName().equals(cityName)) {
                return cityNames;
            }

        }


        return null;
    }

}
