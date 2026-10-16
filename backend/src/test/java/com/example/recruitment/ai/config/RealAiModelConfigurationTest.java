package com.example.recruitment.ai.config;

import com.example.recruitment.RecruitmentApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class RealAiModelConfigurationTest {

    // 读取真实配置并隔离本机 .env.local，不连接模型供应商或查询数据库。
    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(RecruitmentApplication.class)
            .withPropertyValues("spring.config.import=", "ai.enabled=false", "ai.mock=true", "ai.api-key=");

    @ParameterizedTest
    @ValueSource(strings = {"dev", "prod"})
    void mockStartsWithoutApiKey(String profile) {
        runner.withPropertyValues("spring.profiles.active=" + profile).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(AiProperties.class).realMode()).isFalse();
            assertThat(context).doesNotHaveBean(ChatModel.class).doesNotHaveBean(EmbeddingModel.class);
            assertUnusedModelsAbsent(context);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"dev", "prod"})
    void realModeCreatesChatAndEmbeddingModels(String profile) {
        runner.withPropertyValues("spring.profiles.active=" + profile,
                "ai.enabled=true", "ai.mock=false", "ai.api-key=test-only",
                "ai.base-url=http://127.0.0.1:1", "ai.model=test-chat", "ai.embedding-model=test-embedding")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(ChatModel.class).hasSingleBean(EmbeddingModel.class);
                    assertThat(context).hasSingleBean(ChatClient.Builder.class);
                    assertThat(context.getBean(ChatModel.class).getDefaultOptions().getModel()).isEqualTo("test-chat");
                    assertUnusedModelsAbsent(context);
                });
    }

    @Test
    void realModeCanDisableEmbedding() {
        runner.withPropertyValues("ai.enabled=true", "ai.mock=false", "ai.api-key=test-only",
                "ai.embedding-enabled=false").run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(ChatModel.class);
            assertThat(context).doesNotHaveBean(EmbeddingModel.class);
        });
    }

    @Test
    void enabledWithoutKeyDoesNotCreateModels() {
        runner.withPropertyValues("ai.enabled=true", "ai.mock=false").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(ChatModel.class).doesNotHaveBean(EmbeddingModel.class);
        });
    }

    @Test
    void mockWithKeyDoesNotCreateModels() {
        runner.withPropertyValues("ai.enabled=true", "ai.mock=true", "ai.api-key=test-only").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(ChatModel.class).doesNotHaveBean(EmbeddingModel.class);
        });
    }

    private void assertUnusedModelsAbsent(org.springframework.context.ApplicationContext context) {
        assertThat(context.containsBean("openAiAudioSpeechModel")).isFalse();
        assertThat(context.containsBean("openAiAudioTranscriptionModel")).isFalse();
        assertThat(context.containsBean("openAiImageModel")).isFalse();
        assertThat(context.containsBean("openAiModerationModel")).isFalse();
    }
}
