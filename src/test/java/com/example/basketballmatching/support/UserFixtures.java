package com.example.basketballmatching.support;

import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.type.GenderType;
import com.example.basketballmatching.user.type.LoginProvider;
import com.example.basketballmatching.user.type.Position;
import com.example.basketballmatching.user.type.UserType;
import lombok.Builder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

public final class UserFixtures {


    private UserFixtures() {}

    @Builder
    public static UserEntity create(
            Long userId,
            String email,
            String password,
            String nickname,
            String name,
            LocalDate birth,
            String phone,
            String address,
            String addressDetail,
            String zipCode,
            Position position,
            UserType userType,
            GenderType genderType,
            LoginProvider loginProvider,
            boolean emailAuth
    ) {
        UserEntity user = UserEntity.create(
                email,
                password,
                nickname,
                name,
                birth,
                phone,
                address,
                addressDetail,
                zipCode,
                position,
                genderType
        );

        ReflectionTestUtils.setField(user, "userId", userId);
        ReflectionTestUtils.setField(user, "userType", userType);
        ReflectionTestUtils.setField(user, "loginProvider", loginProvider);


        if (emailAuth) {
            user.verifiedEmail();
        }

        return user;
    }
}
