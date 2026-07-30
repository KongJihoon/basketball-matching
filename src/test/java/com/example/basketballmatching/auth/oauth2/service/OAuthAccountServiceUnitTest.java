package com.example.basketballmatching.auth.oauth2.service;

import com.example.basketballmatching.auth.oauth2.domain.OAuthAccountEntity;
import com.example.basketballmatching.auth.oauth2.dto.OAuthAccountDecision;
import com.example.basketballmatching.auth.oauth2.dto.OAuthSignUpRequest;
import com.example.basketballmatching.auth.oauth2.dto.OAuthTicketPayload;
import com.example.basketballmatching.auth.oauth2.repository.OAuthAccountRepository;
import com.example.basketballmatching.auth.oauth2.type.OAuthFlowType;
import com.example.basketballmatching.auth.oauth2.type.OAuthProvider;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.exception.ErrorCode;
import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import com.example.basketballmatching.user.type.GenderType;
import com.example.basketballmatching.user.type.LoginProvider;
import com.example.basketballmatching.user.type.Position;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static com.example.basketballmatching.auth.oauth2.type.OAuthFlowType.*;
import static com.example.basketballmatching.auth.oauth2.type.OAuthProvider.*;
import static com.example.basketballmatching.global.exception.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuthAccountServiceUnitTest {

    private final static Long KAKAO_USER_ID = 123456789L;

    private final static String PROVIDER_USER_ID = String.valueOf(KAKAO_USER_ID);

    private final static String EMAIL = "test@example.com";

    private static final String NICKNAME = "커리";

    private static final String NAME = "테스트회원";

    private static final String TICKET = "signup-ticket";

    @Mock
    private OAuthAccountRepository oAuthAccountRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OAuthAccountService oAuthAccountService;


    @Nested
    @DisplayName("카카오 계정 판별")
    class ResolveKakaoAccount {

        @Test
        @DisplayName("연결된 카카오 계정 존재 시 로그인 흐름 반환")
        void resolveKakaoAccount_success_login() {
            // given

            UserEntity user = createOAuthUser();

            OAuthAccountEntity oAuthAccount = OAuthAccountEntity.create(user, KAKAO, PROVIDER_USER_ID);

            when(oAuthAccountRepository.findWithUserByProviderAndProviderUserId(KAKAO, PROVIDER_USER_ID))
                    .thenReturn(Optional.of(oAuthAccount));


            // when

            OAuthAccountDecision result = oAuthAccountService.resolveKakaoAccount(KAKAO_USER_ID, EMAIL);


            // then

            assertAll(
                    () -> assertEquals(LOGIN, result.flowType()),
                    () -> assertEquals(EMAIL, result.email())
            );

            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("연결된 카카오 계정이 존재하지 않을 시 회원가입 흐름 반환")
        void resolveKakaoAccount_success_signup() {
            // given

            when(oAuthAccountRepository.findWithUserByProviderAndProviderUserId(KAKAO, PROVIDER_USER_ID))
                    .thenReturn(Optional.empty());

            when(userRepository.findByEmail(EMAIL))
                    .thenReturn(Optional.empty());

            // when

            OAuthAccountDecision result = oAuthAccountService.resolveKakaoAccount(KAKAO_USER_ID, EMAIL);

            // then

            assertAll(
                    () -> assertEquals(SIGNUP, result.flowType()),
                    () -> assertEquals(EMAIL, result.email())
            );

            verify(oAuthAccountRepository, never()).save(any(OAuthAccountEntity.class));
        }

        @Test
        @DisplayName("동일 이메일의 LOCAL 계정 존재 시 예외 발생")
        void resolveKakaoAccount_fail_AlreadyExistsEmail() {
            // given

            UserEntity localUser = createLocalUser();

            when(oAuthAccountRepository.findWithUserByProviderAndProviderUserId(KAKAO, PROVIDER_USER_ID))
                    .thenReturn(Optional.empty());

            when(userRepository.findByEmail(EMAIL))
                    .thenReturn(Optional.of(localUser));

            // when

            CustomException exception = assertThrows(CustomException.class, () -> oAuthAccountService.resolveKakaoAccount(KAKAO_USER_ID, EMAIL));

            // then

            assertEquals(OAUTH_ACCOUNT_LINK_REQUIRED, exception.getErrorCode());

            verify(oAuthAccountRepository, never()).save(any(OAuthAccountEntity.class));
        }

    }

    @Nested
    @DisplayName("OAuth 추가정보 회원가입")
    class CompleteSignUp {

        @Test
        @DisplayName("회원가입 흐름 사용자 추가정보 입력 후 회원가입")
        void completeSignUp_success() {
            // given

            OAuthTicketPayload payload = OAuthTicketPayload.signup(KAKAO, PROVIDER_USER_ID, EMAIL);

            OAuthSignUpRequest request = createSignUpRequest();

            when(oAuthAccountRepository.existsByProviderAndProviderUserId(KAKAO, PROVIDER_USER_ID))
                    .thenReturn(false);

            when(userRepository.existsByEmail(EMAIL))
                    .thenReturn(false);

            when(userRepository.existsByNickname(NICKNAME))
                    .thenReturn(false);

            when(userRepository.save(any(UserEntity.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);

            ArgumentCaptor<OAuthAccountEntity> accountCaptor = ArgumentCaptor.forClass(OAuthAccountEntity.class);


            // when

            String result = oAuthAccountService.completeSignUp(payload, request);

            // then
            verify(userRepository).save(userCaptor.capture());

            verify(oAuthAccountRepository).save(accountCaptor.capture());

            UserEntity savedUser = userCaptor.getValue();
            OAuthAccountEntity savedAccount = accountCaptor.getValue();


            assertAll(
                    () -> assertEquals(EMAIL, result),
                    () -> assertEquals(EMAIL, savedUser.getEmail()),
                    () -> assertEquals(LoginProvider.KAKAO, savedUser.getLoginProvider()),
                    () -> assertNull(savedUser.getPassword()),
                    () -> assertEquals(KAKAO, savedAccount.getProvider()),
                    () -> assertEquals(PROVIDER_USER_ID, savedAccount.getProviderUserId()),
                    () -> assertEquals(savedUser, savedAccount.getUserEntity())

            );
        }

        @Test
        @DisplayName("이미 사용중인 닉네임 예외 발생")
        void completeSignUp_fail_alreadyExistsNickname() {
            // given

            OAuthTicketPayload payload = OAuthTicketPayload.signup(KAKAO, PROVIDER_USER_ID, EMAIL);

            OAuthSignUpRequest request = createSignUpRequest();

            when(oAuthAccountRepository.existsByProviderAndProviderUserId(KAKAO, PROVIDER_USER_ID))
                    .thenReturn(false);

            when(userRepository.existsByEmail(EMAIL))
                    .thenReturn(false);

            when(userRepository.existsByNickname(NICKNAME))
                    .thenReturn(true);

            // when

            CustomException exception = assertThrows(CustomException.class, () -> oAuthAccountService.completeSignUp(payload, request));

            // then

            assertEquals(ALREADY_EXIST_NICKNAME, exception.getErrorCode());

            verify(userRepository, never()).save(any(UserEntity.class));
            verify(oAuthAccountRepository, never()).save(any(OAuthAccountEntity.class));



        }

    }

    private static UserEntity createOAuthUser() {
        return UserEntity.createOAuth(
                EMAIL,
                NICKNAME,
                NAME,
                LocalDate.of(1997, 1, 1),
                "010-1111-2222",
                "서울특별시 강남구",
                Position.GUARD,
                GenderType.MALE,
                LoginProvider.KAKAO
        );
    }

    private UserEntity createLocalUser() {
        return UserEntity.create(
                EMAIL,
                "encoded-password",
                NICKNAME,
                NAME,
                LocalDate.of(1997, 1, 1),
                "010-1234-5678",
                "서울특별시 강남구",
                Position.GUARD,
                GenderType.MALE
        );
    }

    private OAuthSignUpRequest createSignUpRequest() {
        return new OAuthSignUpRequest(
                TICKET,
                NICKNAME,
                NAME,
                LocalDate.of(1997, 1, 1),
                "010-1234-5678",
                "서울특별시 강남구",
                Position.GUARD,
                GenderType.MALE
        );
    }

}