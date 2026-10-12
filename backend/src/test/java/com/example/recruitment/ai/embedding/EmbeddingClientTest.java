package com.example.recruitment.ai.embedding;

import com.example.recruitment.ai.config.AiProperties;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmbeddingClientTest {
    @Test
    void secondCallFailureRecomputesBothVectorsLocally() {
        AiProperties properties = realProperties();
        EmbeddingModel model = mock(EmbeddingModel.class);
        ObjectProvider<EmbeddingModel> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(model);
        when(model.embed("Java Spring")).thenReturn(new float[]{1, 0, 0});
        when(model.embed("Spring Java")).thenThrow(new IllegalStateException("offline"));
        EmbeddingClient client = new EmbeddingClient(properties, provider);
        var result = client.similarity("Java Spring", "Spring Java");
        assertEquals("FALLBACK", result.source());
        properties.setMock(true);
        assertEquals(client.similarity("Java Spring", "Spring Java").score(), result.score());
    }

    @Test
    void successfulModelAndDisabledEmbeddingReportActualSource() {
        AiProperties properties = realProperties();
        EmbeddingModel model = mock(EmbeddingModel.class);
        ObjectProvider<EmbeddingModel> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(model);
        when(model.embed(anyString())).thenReturn(new float[]{2, 0});
        var client = new EmbeddingClient(properties, provider);
        assertEquals("EMBEDDING", client.similarity("a", "b").source());
        assertEquals(100, client.similarity("a", "b").score());
        properties.setEmbeddingEnabled(false);
        assertEquals("LOCAL", client.similarity("a", "b").source());
    }

    @Test
    void invalidModelVectorsFallBack() {
        EmbeddingModel model = mock(EmbeddingModel.class);
        ObjectProvider<EmbeddingModel> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(model);
        when(model.embed(anyString())).thenReturn(new float[]{Float.NaN});
        assertEquals("FALLBACK", new EmbeddingClient(realProperties(), provider).similarity("aa", "bb").source());
    }

    private AiProperties realProperties() {
        var properties = new AiProperties();
        properties.setEnabled(true); properties.setMock(false); properties.setApiKey("test");
        return properties;
    }
}
