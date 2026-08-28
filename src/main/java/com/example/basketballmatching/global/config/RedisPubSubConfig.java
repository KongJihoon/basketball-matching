package com.example.basketballmatching.global.config;

import com.example.basketballmatching.notifications.redis.NotificationRedisSubscriber;
import com.example.basketballmatching.notifications.type.RedisTopic;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
@RequiredArgsConstructor
public class RedisPubSubConfig {

    @Bean
    public ChannelTopic gameCreatedTopic() {
        return new ChannelTopic(RedisTopic.GAME_CREATED.getValue());
    }

    @Bean
    public MessageListenerAdapter gameCreatedSubscriberAdapter(NotificationRedisSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber);
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter gameCreatedSubscriberAdapter,
            ChannelTopic gameCreatedTopic
    ) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();

        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(gameCreatedSubscriberAdapter, gameCreatedTopic);

        return container;

    }

}
