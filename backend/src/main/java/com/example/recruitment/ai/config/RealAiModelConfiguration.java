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
import org.springframework.core.type.AnnotatedTypeMetadata;

/** 仅在业务真实模式下装配模型，避免 Mock / 空密钥触发框架的密钥校验。 */
@Configuration(proxyBeanMethods = false)
@Conditional(RealAiModelConfiguration.RealModeCondition.class)
@Import(OpenAiChatAutoConfiguration.class)
public class RealAiModelConfiguration {

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
