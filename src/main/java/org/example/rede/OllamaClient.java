package org.example.rede;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class OllamaClient {
    private static final String OLLAMA_URL = "http://localhost:11434";
    private static final String MODELO_EMBEDDING = "nomic-embed-text";
    private static final String MODELO_LLM = "qwen2.5-coder:0.5b-instruct-q4_K_M";
    private final HttpClient httpClient;

    public OllamaClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(30))
                .build();
    }

    public double[] gerarEmbedding(String texto) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("model", MODELO_EMBEDDING);
        body.addProperty("prompt", texto);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OLLAMA_URL + "/api/embeddings"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Ollama embeddings HTTP " + response.statusCode() + ": " + response.body());
        }

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        if (!json.has("embedding")) {
            throw new RuntimeException("Resposta sem campo 'embedding': " + response.body());
        }

        JsonArray arr = json.getAsJsonArray("embedding");
        double[] vetor = new double[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            vetor[i] = arr.get(i).getAsDouble();
        }
        return vetor;
    }        // nomic-embed-text

    public String gerar(String promptChatML) throws Exception {
        return null;
    }        // qwen2.5-coder


}