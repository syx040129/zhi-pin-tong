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
import com.example.recruitment.ai.llm.LlmClient;
import com.example.recruitment.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RealAiModelConfigurationTest {

    @Test
    void realHttpCallsApplyTokenOptionsAndReadTimeout() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var slow = new AtomicBoolean(false);
        var requestBody = new AtomicReference<String>();
        server.createContext("/v1/chat/completions", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            if (slow.get()) {
                try { Thread.sleep(1800); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
            byte[] body = ("{\"id\":\"test\",\"object\":\"chat.completion\",\"created\":1,\"model\":\"test-chat\","
                    + "\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":\"ok\"},\"finish_reason\":\"stop\"}]}")
                    .getBytes(StandardCharsets.UTF_8);
            try {
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            } catch (java.io.IOException ignored) {
                // 超时客户端主动断开连接。
            } finally { exchange.close(); }
        });
        server.start();
        try {
            runner.withPropertyValues("ai.enabled=true", "ai.mock=false", "ai.api-key=test-only",
                    "ai.embedding-enabled=false", "ai.timeout-seconds=1", "ai.max-tokens=321", "ai.temperature=0.2",
                    "ai.base-url=http://127.0.0.1:" + server.getAddress().getPort())
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        LlmClient client = context.getBean(LlmClient.class);
                        assertThat(client.chat("system", "hello")).isEqualTo("ok");
                        var json = new ObjectMapper().readTree(requestBody.get());
                        assertThat(json.path("max_tokens").asInt()).isEqualTo(321);
                        assertThat(json.path("temperature").asDouble()).isEqualTo(0.2);
                        slow.set(true);
                        org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(Duration.ofSeconds(3), () ->
                                org.junit.jupiter.api.Assertions.assertThrows(BusinessException.class, () -> client.chat("system", "slow")));
                    });
        } finally { server.stop(0); }
    }

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
