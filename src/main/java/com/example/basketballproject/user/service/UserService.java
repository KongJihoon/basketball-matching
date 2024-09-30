package com.example.basketballproject.user.service;


import com.example.basketballproject.global.exception.CustomException;
import com.example.basketballproject.global.exception.ErrorCode;
import com.example.basketballproject.global.service.MailService;
import com.example.basketballproject.user.dto.FindUserDto;
import com.example.basketballproject.user.dto.UserDto;
import com.example.basketballproject.user.entity.UserEntity;
import com.example.basketballproject.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import static com.example.basketballproject.global.exception.ErrorCode.USER_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final MailService mailService;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        return userRepository.findByLoginIdAndDeletedDateTimeNull(loginId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));
    }


    public UserDto getUserInfo(String loginId) {

        UserEntity userEntity = userRepository.findByLoginIdAndDeletedDateTimeNull(loginId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        return UserDto.fromEntity(userEntity);

    }

    public String findLoginId(String email) {

        UserEntity userEntity = userRepository.findByEmailAndDeletedDateTimeNull(email)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        return userEntity.getLoginId();
    }

    public boolean findPassword(String loginId) throws NoSuchAlgorithmException {

        UserEntity user = userRepository.findByLoginIdAndDeletedDateTimeNull(loginId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        String certificationNumber = createCertificationNumber();

        String encode = passwordEncoder.encode(certificationNumber);

        user.setPassword(encode);

        userRepository.save(user);

        String email = user.getEmail();

        boolean isSuccess = mailService.TemporaryPassword(email, certificationNumber);

        return isSuccess;

    }



    private String createCertificationNumber() throws NoSuchAlgorithmException {

        String certificationNumber;

        int sum = SecureRandom.getInstanceStrong().nextInt(999999);

        certificationNumber = String.format("%06d", sum);

        return certificationNumber;
    }


}
