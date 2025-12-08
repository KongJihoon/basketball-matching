package com.example.basketballmatching.gameUsers.service.impl;

import com.example.basketballmatching.gameCreator.repository.GameQueryRepository;
import com.example.basketballmatching.gameUsers.dto.CurrentGameListDto;
import com.example.basketballmatching.gameUsers.dto.LastGameListDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameUserCacheService {


    private final GameQueryRepository gameQueryRepository;


    @Cacheable(
            cacheNames = "myCurrentGameList",
            key = "T(String).valueOf(#userId)"
            + " + ':' + #pageable.pageNumber"
            + " + ':' + #pageable.pageSize"
            + " + ':' + #pageable.sort.toString()",
            unless = "#result == null || #result.isEmpty()"
    )
    @Transactional(readOnly = true)
    public List<CurrentGameListDto> getMyCurrentGameListCached(Long userId, Pageable pageable) {

        log.info("[현재 예정 게임 리스트 조회 캐싱] userId = {}, page = {}, size = {}, sort = {}",
                userId, pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());


        return gameQueryRepository.getCurrentGameList(userId,pageable);
    }


    @Cacheable(
            cacheNames = "myLastGameList",
            key = "T(String).valueOf(#userId)"
            + " + ':' + #pageable.pageNumber"
            + " + ':' + #pageable.pageSize"
            + " + ':' + #pageable.sort.toString()",
            unless = "#result == null || #result.isEmpty()"
    )
    @Transactional(readOnly = true)
    public List<LastGameListDto> getMyLastGameListCached(Long userId, Pageable pageable) {

        log.info("[지난 게임 리스트 조회 캐싱] userId = {}, page = {}, size = {}, sort = {}",
                userId, pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        return gameQueryRepository.getLastGameList(userId, pageable);

    }


}
