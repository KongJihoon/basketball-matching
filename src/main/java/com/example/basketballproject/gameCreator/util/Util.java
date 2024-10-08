package com.example.basketballproject.gameCreator.util;

import com.example.basketballproject.gameCreator.type.CityName;

public class Util {


    public static CityName getCityName(String address) {

        String[] parts = address.split(" ");

        CityName cityName = null;

        switch (parts[0]) {
            case "서울":
                cityName = CityName.SEOUL;
                break;
            case "경기":
                cityName = CityName.GYEONGGI;
                break;
            case "인천":
                cityName = CityName.INCHEON;
                break;
            case "강원특별자치도":
                cityName = CityName.GANGWON;
                break;
            case "대전":
                cityName = CityName.DAEJEON;
                break;
            case "세종특별자치시":
                cityName = CityName.SEJONG;
                break;
            case "충남":
                cityName = CityName.CHUNGNAM;
                break;
            case "충북":
                cityName = CityName.CHUNGBUK;
                break;
            case "대구":
                cityName = CityName.DAEGU;
                break;
            case "경북":
                cityName = CityName.GYEONGBUK;
                break;
            case "부산":
                cityName = CityName.BUSAN;
                break;
            case "울산":
                cityName = CityName.ULSAN;
                break;
            case "경남":
                cityName = CityName.GYEONGNAM;
                break;
            case "광주":
                cityName = CityName.GWANGJU;
                break;
            case "전남":
                cityName = CityName.JEONNAM;
                break;
            case "전북특별자치도":
                cityName = CityName.JEONBUK;
                break;
            case "제주특별자치도":
                cityName = CityName.JEJU;
                break;
        }

        return cityName;
    }

}
