package org.example.rede;
import java.net.http.HttpClient;
import org.example.modelo.*;

public class ArenaClient {
    private final HttpClient http = HttpClient.newHttpClient();
    private final String urlBase;
    public ArenaClient(String urlBase) { this.urlBase = urlBase; }

    // POST /arena/{room_id}/register  — CONFIRMAR: robot_id vai em query ou corpo?
    public EstadoRobot registar(String roomId, String robotId) throws Exception { return null; }

    // GET /arena/{room_id}/perceive/{robot_id}
    public Percecao perceber(String roomId, String robotId) throws Exception { return null; }

    // POST /arena/action  — corpo confirmado: {room_id, robot_id, action}
    public RespostaAcao agir(String roomId, String robotId, String action) throws Exception { return null; }

    // POST /arena/{room_id}/unlock  — params: room_id, robot_id, code, rag_chunk?, llm_raw?
    // CONFIRMAR: encoding (query vs corpo) e chave de status na resposta.
    public RespostaAcao desbloquear(String roomId, String robotId, String code,
                                    String ragChunk, String llmRaw) throws Exception { return null; }

    // GET /arena/{room_id}/download_manual → texto cru.
    public String descarregarManual(String roomId) throws Exception { return null; }
}