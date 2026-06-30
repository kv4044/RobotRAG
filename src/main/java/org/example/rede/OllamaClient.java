package org.example.rede;
import java.util.List;
import org.example.modelo.DocumentoVetorial;

// SÓ Ollama (localhost:11434). DOMÍNIO DO KAIKY — stub mínimo; ele implementa.
public class OllamaClient {
    public double[] gerarEmbedding(String texto) throws Exception { return null; }   // nomic-embed-text
    public String gerar(String promptChatML) throws Exception { return null; }        // qwen2.5-coder
    public List<DocumentoVetorial> vetorizarChunks(List<String> chunks) throws Exception { return null; }
}