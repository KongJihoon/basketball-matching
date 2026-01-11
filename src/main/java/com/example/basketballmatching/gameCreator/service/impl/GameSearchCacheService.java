package com.example.basketballmatching.gameCreator.service.impl;

import com.example.basketballmatching.gameCreator.dto.GameSearchCacheDto;
import com.example.basketballmatching.gameCreator.dto.SearchGameDto;
import com.example.basketballmatching.gameCreator.repository.GameQueryRepository;
import com.example.basketballmatching.gameCreator.type.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameSearchCacheService {

    private final GameQueryRepository gameQueryRepository;

    @Cacheable(
            cacheNames = "gameSearch",
            key = "#date + ':' + #cityName + ':' + #matchFormat + ':' + #fieldStatus + ':' + #matchGenderType + ':' + #gameStatus + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort.toString()",
            unless = "#result == null || #result.getContent() == null || #result.getContent().isEmpty()"
    )
    public GameSearchCacheDto<SearchGameDto> searchGameCached(
            LocalDate date,
            CityName cityName,
            MatchFormat matchFormat,
            FieldStatus fieldStatus,
            MatchGenderType matchGenderType,
            GameStatus gameStatus,
            Pageable pageable
    ) {
        log.info("[DB 조회 수행] date={}, city={}, page={}, size={}, sort={}",
                date, cityName, pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
        Page<SearchGameDto> searchGameDtos = gameQueryRepository.searchByKeyword(date, cityName, matchFormat, fieldStatus, matchGenderType, gameStatus, pageable);

        return new GameSearchCacheDto<>(searchGameDtos.getContent(), searchGameDtos.getTotalElements());
    }

}
