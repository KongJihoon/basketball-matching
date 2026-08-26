package com.example.basketballmatching.blacklist.service;

import com.example.basketballmatching.blacklist.repository.BlackListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class BlackListStore {

    private final BlackListRepository blackListRepository;

    private final Clock clock;

    @Transactional(readOnly = true)
    public boolean isBlacklisted(String email) {

        return blackListRepository.existsByUserEntity_EmailAndExpiresAtAfter(
                email, LocalDateTime.now(clock)
        );

    }


}
