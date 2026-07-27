package com.example.basketballmatching.auth.oauth2.domain;

import com.example.basketballmatching.auth.oauth2.type.OAuthProvider;
import com.example.basketballmatching.global.entity.BaseEntity;
import com.example.basketballmatching.user.domain.UserEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "oauth_account_entity",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_oauth_provider_user",
                        columnNames = {
                                "provider",
                                "provider_user_id"
                        }
                ),
                @UniqueConstraint(
                        name = "uq_oauth_user_provider",
                        columnNames = {
                                "user_id",
                                "provider"
                        }
                )
        }
)
public class OAuthAccountEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long oauthAccountId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity userEntity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OAuthProvider provider;

    @Column(nullable = false)
    private String providerUserId;


    @Builder(access = AccessLevel.PRIVATE)
    private OAuthAccountEntity(UserEntity userEntity, OAuthProvider provider, String providerUserId) {
        this.userEntity = userEntity;
        this.provider = provider;
        this.providerUserId = providerUserId;

    }


    public static OAuthAccountEntity create(UserEntity userEntity, OAuthProvider provider, String providerUserId) {

        return OAuthAccountEntity.builder()
                .userEntity(userEntity)
                .provider(provider)
                .providerUserId(providerUserId)
                .build();
    }

}
