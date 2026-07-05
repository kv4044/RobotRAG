package org.example.rag;

public class VetorUtils {
    private VetorUtils() {}

    public static double cosineSimilarity(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length || a.length == 0) {
            throw new IllegalArgumentException("Vetores inválidos ou de dimensões diferentes.");
        }
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0; // vetor nulo não tem direção — similaridade indefinida tratada como mínima, nunca escolhida
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}