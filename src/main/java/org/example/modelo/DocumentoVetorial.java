package org.example.modelo;

public class DocumentoVetorial {
    private String texto;
    private double[] vetor;

    public DocumentoVetorial() {}

    public DocumentoVetorial(String texto, double[] vetor) {
        this.texto = texto;
        this.vetor = vetor;
    }

    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }

    public double[] getVetor() {
        if (vetor == null) throw new IllegalStateException("DocumentoVetorial sem vetor definido — instanciado via construtor vazio sem setVetor().");
        return vetor;
    }
    public void setVetor(double[] vetor) { this.vetor = vetor; }
}