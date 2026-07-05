package org.example.rag;

public class RespostaRAG {
    private final String chaveFinal;           // null = não submeter (ambas falharam)
    private final boolean validacaoPassou;
    private final int tentativaUsada;          // 1, 2, ou 0 se nenhuma validou

    private final String chavePrimaria;
    private final double scorePrimario;
    private final String chunkUsadoPrimario;
    private final String respostaBrutaLLMPrimaria;

    private final String chaveSecundaria;      // null se 1ª já validou (2ª nunca correu)
    private final double scoreSecundario;
    private final String chunkUsadoSecundario;
    private final String respostaBrutaLLMSecundaria;

    public RespostaRAG(String chaveFinal, boolean validacaoPassou, int tentativaUsada,
                       String chavePrimaria, double scorePrimario, String chunkUsadoPrimario, String respostaBrutaLLMPrimaria,
                       String chaveSecundaria, double scoreSecundario, String chunkUsadoSecundario, String respostaBrutaLLMSecundaria) {
        if (chaveFinal != null && tentativaUsada != 1 && tentativaUsada != 2) {
            throw new IllegalArgumentException("chaveFinal preenchida exige tentativaUsada 1 ou 2, recebido: " + tentativaUsada);
        }
        if (chaveFinal == null && tentativaUsada != 0) {
            throw new IllegalArgumentException("chaveFinal null exige tentativaUsada 0, recebido: " + tentativaUsada);
        }
        this.chaveFinal = chaveFinal;
        this.validacaoPassou = validacaoPassou;
        this.tentativaUsada = tentativaUsada;
        this.chavePrimaria = chavePrimaria;
        this.scorePrimario = scorePrimario;
        this.chunkUsadoPrimario = chunkUsadoPrimario;
        this.respostaBrutaLLMPrimaria = respostaBrutaLLMPrimaria;
        this.chaveSecundaria = chaveSecundaria;
        this.scoreSecundario = scoreSecundario;
        this.chunkUsadoSecundario = chunkUsadoSecundario;
        this.respostaBrutaLLMSecundaria = respostaBrutaLLMSecundaria;
    }

    public boolean deveSubmeter() { return chaveFinal != null; }

    public String getChaveFinal() { return chaveFinal; }
    public boolean isValidacaoPassou() { return validacaoPassou; }
    public int getTentativaUsada() { return tentativaUsada; }
    public String getChavePrimaria() { return chavePrimaria; }
    public double getScorePrimario() { return scorePrimario; }
    public String getChunkUsadoPrimario() { return chunkUsadoPrimario; }
    public String getRespostaBrutaLLMPrimaria() { return respostaBrutaLLMPrimaria; }
    public String getChaveSecundaria() { return chaveSecundaria; }
    public double getScoreSecundario() { return scoreSecundario; }
    public String getChunkUsadoSecundario() { return chunkUsadoSecundario; }
    public String getRespostaBrutaLLMSecundaria() { return respostaBrutaLLMSecundaria; }

    public String[] dadosParaSubmissao() {
        if (!deveSubmeter()) return null;
        return new String[]{ chaveFinal, getChunkFinal(), getRespostaBrutaLLMFinal() };
    }

    public String getChunkFinal() {
        if (tentativaUsada == 1) return chunkUsadoPrimario;
        if (tentativaUsada == 2) return chunkUsadoSecundario;
        return null;
    }

    public String getRespostaBrutaLLMFinal() {
        if (tentativaUsada == 1) return respostaBrutaLLMPrimaria;
        if (tentativaUsada == 2) return respostaBrutaLLMSecundaria;
        return null;
    }
}