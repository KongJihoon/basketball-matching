package com.example.basketballmatching.auth.oauth2.repository;

import com.example.basketballmatching.auth.oauth2.domain.OAuthAccountEntity;
import com.example.basketballmatching.auth.oauth2.type.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OAuthAccountRepository extends JpaRepository<OAuthAccountEntity, Long> {


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
