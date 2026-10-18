package com.example.basketballmatching.auth.oauth2.repository;

import com.example.basketballmatching.auth.oauth2.domain.OAuthAccountEntity;
import com.example.basketballmatching.auth.oauth2.type.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OAuthAccountRepository extends JpaRepository<OAuthAccountEntity, Long> {


    /**
     * Provider와 Provider 사용자 ID로 OAuth 연결 정보를 조회
     *
     * 로그인 처리에서 연결된 사용자 정보도 바로 필요하므로
     * Fetch Join을 사용해 OAuthAccountEntity와 UserEntity를 함께 조회한다.
     */
    @Query("""
            select oauthAccount
            from OAuthAccountEntity oauthAccount
            join fetch oauthAccount.userEntity
            where oauthAccount.provider = :provider
            and oauthAccount.providerUserId = :providerUserId
            """)
    Optional<OAuthAccountEntity> findWithUserByProviderAndProviderUserId(
            @Param("provider")OAuthProvider provider,
            @Param("providerUserId") String providerUserId
    );

    boolean existsByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);


}
