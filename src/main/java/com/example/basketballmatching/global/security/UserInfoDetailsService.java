package com.example.basketballmatching.global.security;

import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import static com.example.basketballmatching.global.exception.ErrorCode.USER_NOT_FOUND;

@Service
@Getter
@RequiredArgsConstructor
public class UserInfoDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        UserEntity userEntity = userRepository.findByEmailAndDeletedDateTimeIsNull(email)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        return new UserInfoDetails(userEntity);
    }
}
