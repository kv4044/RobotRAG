package org.example.rede;

import com.google.gson.Gson;
import org.example.modelo.Percecao;
import org.example.modelo.RespostaAcao;
import org.example.modelo.RespostaRegisto;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;



// SÓ HTTP com a Arena. Não decide, não desenha, não fala com o Ollama.
public class ArenaClient {

    private final HttpClient http;
    private final Gson gson;
    private final String urlBase;

    public ArenaClient(String urlBase) {
        this.urlBase = urlBase;
        this.gson = new Gson();
        // Timeout de ligação evita bloqueio infinito se o servidor não responder.
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    // POST /arena/{room_id}/register?robot_id=... — room no path, robot em query string.
    public RespostaRegisto registar(String roomId, String robotId) throws Exception {
        String url = urlBase + "/arena/" + roomId + "/register?robot_id=" + robotId;
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        return gson.fromJson(resp.body(), RespostaRegisto.class);
    }

    // GET /arena/{room_id}/perceive/{robot_id} — ambos no path. Confirmado no Swagger.
    public Percecao perceber(String roomId, String robotId) throws Exception {
        String url = urlBase + "/arena/" + roomId + "/perceive/" + robotId;
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        return gson.fromJson(resp.body(), Percecao.class);
    }

    // POST /arena/action — corpo JSON {room_id, robot_id, action}.
    public RespostaAcao agir(String roomId, String robotId, String action) throws Exception {
        String url = urlBase + "/arena/action";
        String corpo = gson.toJson(new PedidoAcao(roomId, robotId, action));
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(corpo))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        return gson.fromJson(resp.body(), RespostaAcao.class);
    }

    public String desbloquear(String roomId, String robotId, String code,
                              String ragChunk, String llmRaw) throws Exception {
        StringBuilder url = new StringBuilder(urlBase)
                .append("/arena/").append(enc(roomId)).append("/unlock")
                .append("?robot_id=").append(enc(robotId))
                .append("&code=").append(enc(code));
        if (ragChunk != null) url.append("&rag_chunk=").append(enc(ragChunk));
        if (llmRaw   != null) url.append("&llm_raw=").append(enc(llmRaw));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url.toString()))
                .timeout(Duration.ofSeconds(10))
                .header("accept", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

        String body = (resp.body() == null) ? "" : resp.body().trim();

        // SUCESSO: 200 + body "null"/vazio -> bau desapareceu no servidor.
        if (body.isEmpty() || "null".equals(body)) {
            return "sucesso";
        }
        // NÃO-SUCESSO: extrai só o campo status do JSON.
        com.google.gson.JsonObject j = com.google.gson.JsonParser.parseString(body).getAsJsonObject();
        return j.has("status") ? j.get("status").getAsString() : "desconhecido";
    }

    private static String enc(String v) {
        return java.net.URLEncoder.encode(v, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
    }

    // GET /arena/{room_id}/download_manual -> texto cru (uma secção por linha).
// NÃO é JSON apesar do accept:application/json — o body vem como text/plain.
// Devolve a String literal; o split("\n") acontece no MotorRAGImpl.ingerirManual.
    public String descarregarManual(String roomId) throws Exception {
        String url = urlBase + "/arena/" + roomId + "/download_manual";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() != 200) {
            throw new RuntimeException("download_manual HTTP " + resp.statusCode() + ": " + resp.body());
        }
        String body = resp.body();
        if (body == null || body.isBlank()) {
            throw new RuntimeException("Manual vazio devolvido pelo servidor.");
        }
        return body; // texto cru; ingestão trata o chunking
    }

    // Estrutura interna só para serializar o corpo do /action. Nomes = chaves JSON.
    private static class PedidoAcao {
        private final String room_id;
        private final String robot_id;
        private final String action;
        PedidoAcao(String roomId, String robotId, String action) {
            this.room_id = roomId;
            this.robot_id = robotId;
            this.action = action;
        }
    }
}