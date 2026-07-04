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

    // STUB — /unlock: encoding (path vs corpo) por confirmar no Swagger.
    public RespostaAcao desbloquear(String roomId, String robotId, String code,
                                    String ragChunk, String llmRaw) throws Exception {
        throw new UnsupportedOperationException("Confirmar formato do /unlock no Swagger.");
    }

    // STUB — /download_manual: formato de retorno (texto cru?) por confirmar.
    public String descarregarManual(String roomId) throws Exception {
        throw new UnsupportedOperationException("Confirmar formato do /download_manual no Swagger.");
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