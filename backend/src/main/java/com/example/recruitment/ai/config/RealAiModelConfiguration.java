package com.example.recruitment.ai.config;

import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import java.time.Duration;
import org.springframework.core.type.AnnotatedTypeMetadata;

/** 仅在业务真实模式下装配模型，避免 Mock / 空密钥触发框架的密钥校验。 */
@Configuration(proxyBeanMethods = false)
@Conditional(RealAiModelConfiguration.RealModeCondition.class)
@Import(OpenAiChatAutoConfiguration.class)
public class RealAiModelConfiguration {

    /** Chat 与 Embedding 的同步 HTTP 调用均应用连接/读取超时。 */
    @Bean
    RestClientCustomizer aiTimeoutCustomizer(AiProperties properties) {
        return builder -> {
            var factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(Duration.ofSeconds(Math.min(5, properties.getTimeoutSeconds())));
            factory.setReadTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()));
            builder.requestFactory(factory);
        };
    }

    @Configuration(proxyBeanMethods = false)
    @Conditional(RealModeCondition.class)
    @ConditionalOnProperty(prefix = "ai", name = "embedding-enabled", havingValue = "true", matchIfMissing = true)
    @Import(OpenAiEmbeddingAutoConfiguration.class)
    static class EmbeddingConfiguration {
    }

    static class RealModeCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return Binder.get(context.getEnvironment())
                    .bind("ai", AiProperties.class)
                    .orElseGet(AiProperties::new)
                    .realMode();
        }
    }
}
