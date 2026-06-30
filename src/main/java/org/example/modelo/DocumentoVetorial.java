package org.example.modelo;
// Modelo PARTILHADO. Contrato com Kaiky — só muda por PR aprovado pelos dois.
public class DocumentoVetorial {
    private String texto; private double[] vetor;
    public DocumentoVetorial() {}
    public DocumentoVetorial(String texto, double[] vetor) { this.texto = texto; this.vetor = vetor; }
    // getters/setters
}