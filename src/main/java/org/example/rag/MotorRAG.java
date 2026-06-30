package org.example.rag;
// Fronteira ÚNICA motor determinístico ⇄ RAG. Implementada por Kaiky.
// O AgenteExplorador só conhece esta interface. PENDENTE de validação do Kaiky.
public interface MotorRAG {
    void ingerirManual(String textoManual) throws Exception; // arranque: chunking+embeddings
    RespostaRAG resolverEnigma(String enigma) throws Exception;
    boolean estaPronto(); // Ollama vivo + manual ingerido
}