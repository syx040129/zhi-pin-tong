package com.example.recruitment.ai.embedding;

import com.example.recruitment.ai.config.AiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 向量化客户端：真实模式调用 Embedding 模型；
 * Mock / 失败时使用本地 n-gram 哈希向量（维度 128，归一化），保证相似度计算可用。
 */
@Slf4j
@Component
public class EmbeddingClient {

    private static final int DIM = 128;

    private final AiProperties aiProperties;
    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;

    public EmbeddingClient(AiProperties aiProperties,
                           ObjectProvider<EmbeddingModel> embeddingModelProvider) {
        this.aiProperties = aiProperties;
        this.embeddingModelProvider = embeddingModelProvider;
    }

    /**
     * 返回归一化向量（真实模式优先）
     */
    public List<Double> embed(String text) {
        float[] vector = tryRealEmbed(text);
        if (vector == null) {
            vector = localEmbed(text);
        }
        List<Double> result = new ArrayList<>(vector.length);
        for (float v : vector) {
            result.add((double) v);
        }
        return normalize(result);
    }

    public record SimilarityResult(int score, String source, String explanation) {}

    /** Both texts must use the same vector space, including when either call fails. */
    public SimilarityResult similarity(String first, String second) {
        if (aiProperties.realMode() && aiProperties.isEmbeddingEnabled()) {
            try {
                EmbeddingModel model = embeddingModelProvider.getIfAvailable();
                if (model == null) throw new IllegalStateException("Embedding model unavailable");
                float[] a = model.embed(first);
                float[] b = model.embed(second);
                if (!valid(a) || !valid(b) || a.length != b.length) {
                    throw new IllegalStateException("Invalid embedding vectors");
                }
                return new SimilarityResult(similarityScore(a, b), "EMBEDDING", "使用真实 Embedding 模型计算文本相似度");
            } catch (Exception e) {
                log.warn("匹配向量调用失败，两个文本均改用本地向量: {}", e.getMessage());
                return new SimilarityResult(similarityScore(localEmbed(first), localEmbed(second)),
                        "FALLBACK", "向量模型调用失败，本次改用本地哈希向量近似计算");
            }
        }
        return new SimilarityResult(similarityScore(localEmbed(first), localEmbed(second)), "LOCAL",
                "当前使用本地哈希向量近似计算，不代表真实模型的语义判断");
    }

    private static boolean valid(float[] vector) {
        if (vector == null || vector.length == 0) return false;
        double norm = 0;
        for (float value : vector) {
            if (!Float.isFinite(value)) return false;
            norm += value * value;
        }
        return norm > 0;
    }

    private static int similarityScore(float[] a, float[] b) {
        List<Double> first = new ArrayList<>();
        List<Double> second = new ArrayList<>();
        for (float value : a) first.add((double) value);
        for (float value : b) second.add((double) value);
        return (int) Math.round(Math.max(0, Math.min(1, cosine(normalize(first), normalize(second)))) * 100);
    }

    private float[] tryRealEmbed(String text) {
        if (!aiProperties.isEmbeddingEnabled()) {
            return null;
        }
        EmbeddingModel model = embeddingModelProvider.getIfAvailable();
        if (model == null || !aiProperties.realMode()) {
            return null;
        }
        try {
            return model.embed(text);
        } catch (Exception e) {
            log.warn("Embedding 模型调用失败，使用本地向量: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 本地确定性向量：按 2-gram 哈希映射到 128 维稀疏桶
     */
    public static float[] localEmbed(String text) {
        float[] vector = new float[DIM];
        if (text == null || text.isBlank()) {
            return vector;
        }
        String compact = text.toLowerCase().replaceAll("\\s+", " ");
        for (int i = 0; i < compact.length() - 1; i++) {
            String gram = compact.substring(i, i + 2);
            int hash = Math.floorMod(gram.hashCode(), DIM);
            vector[hash] += 1f;
        }
        double norm = 0;
        for (float v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= norm;
            }
        }
        return vector;
    }

    public static List<Double> normalize(List<Double> vector) {
        double norm = 0;
        for (double v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        if (norm == 0) {
            return vector;
        }
        List<Double> result = new ArrayList<>(vector.size());
        for (double v : vector) {
            result.add(v / norm);
        }
        return result;
    }

    /**
     * 余弦相似度
     */
    public static double cosine(List<Double> a, List<Double> b) {
        int n = Math.min(a.size(), b.size());
        double dot = 0;
        for (int i = 0; i < n; i++) {
            dot += a.get(i) * b.get(i);
        }
        return dot;
    }
}
