package org.example.rag;

import org.example.modelo.DocumentoVetorial;
import org.example.rede.OllamaClient;

import java.util.ArrayList;
import java.util.List;

public class MotorRAGImpl implements MotorRAG {

    private final OllamaClient client;
    private List<DocumentoVetorial> chunks;

    public MotorRAGImpl(OllamaClient client) {
        this.client = client;
    }

    @Override
    public void ingerirManual(String textoManual) throws Exception {
        String[] linhas = textoManual.split("\n");
        chunks = new ArrayList<>();
        for (String linha : linhas) {
            String texto = linha.trim();
            if (texto.isEmpty()) continue;
            double[] vetor = client.gerarEmbedding(texto);
            chunks.add(new DocumentoVetorial(texto, vetor));
        }
    }

    @Override
    public boolean estaPronto() {
        return client.estaDisponivel() && chunks != null && !chunks.isEmpty();
    }

    @Override
    public RespostaRAG resolverEnigma(String enigma) throws Exception {
        if (!estaPronto()) throw new IllegalStateException("Manual não ingerido ou Ollama indisponível.");

        double[] vetorEnigma = client.gerarEmbedding(enigma);

        // ordena índices dos chunks por score decrescente
        List<Integer> ordem = new ArrayList<>();
        double[] scores = new double[chunks.size()];
        for (int i = 0; i < chunks.size(); i++) {
            scores[i] = VetorUtils.cosineSimilarity(vetorEnigma, chunks.get(i).getVetor());
            ordem.add(i);
        }
        ordem.sort((a, b) -> Double.compare(scores[b], scores[a]));

        int idx1 = ordem.get(0);
        String chunk1 = chunks.get(idx1).getTexto();
        String resp1 = client.gerar(montarPrompt(chunk1, enigma)).trim();
        String chave1 = ExtratorChave.extrair(resp1);
        boolean valida1 = validar(chave1, chunk1);

        if (valida1) {
            return new RespostaRAG(chave1, true, 1,
                    chave1, scores[idx1], chunk1, resp1,
                    null, -1.0, null, null);
        }

        if (chunks.size() < 2) {
            return new RespostaRAG(null, false, 0,
                    chave1, scores[idx1], chunk1, resp1,
                    null, -1.0, null, null);
        }

        int idx2 = ordem.get(1);
        String chunk2 = chunks.get(idx2).getTexto();
        String resp2 = client.gerar(montarPrompt(chunk2, enigma)).trim();
        String chave2 = ExtratorChave.extrair(resp2);
        boolean valida2 = validar(chave2, chunk2);

        if (valida2) {
            return new RespostaRAG(chave2, true, 2,
                    chave1, scores[idx1], chunk1, resp1,
                    chave2, scores[idx2], chunk2, resp2);
        }

        // ambas falharam -> não submete
        return new RespostaRAG(null, false, 0,
                chave1, scores[idx1], chunk1, resp1,
                chave2, scores[idx2], chunk2, resp2);
    }

    private boolean validar(String chave, String chunk) {
        if (chave == null) return false;
        String chunkNorm = chunk.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        String chaveNorm = chave.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        return chunkNorm.contains(chaveNorm); // compara sem hífen; chaveFinal continua "XTR-99"
    }

    private String montarPrompt(String chunk, String enigma) {
        return  "<|im_start|>system\n" +
                "És um sistema de resposta baseado exclusivamente no manual fornecido.\n" +
                "Regras:\n" +
                "- Utiliza apenas a informação presente no manual.\n" +
                "- Nunca inventes informação.\n" +
                "- A resposta deve ser exatamente o código presente no manual.\n" +
                "- Se não existir resposta no manual responde apenas NULL.\n" +
                "<|im_end|>\n" +

                "<|im_start|>user\n" +
                "Manual:\n" +
                chunk +
                "\n\nPergunta:\n" +
                enigma +
                "\n<|im_end|>\n" +

                "<|im_start|>assistant\n";
    }
}