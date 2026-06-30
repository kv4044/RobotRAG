package org.example.modelo;
// Elemento de "cofres_no_mundo". Swagger devolveu [] → chaves do cofre NÃO
// confirmadas (assumi padrão de Recurso por analogia). O campo do enigma
// (terminal_desafio?) só surge em cima do cofre e tem nome DESCONHECIDO.
// CONFIRMAR numa sala com cofre antes de desserializar e disparar o RAG.
public class Cofre {
    private String id; private String type; // NÃO CONFIRMADO
    private int x; private int y; private double z;
    // TODO: campo do enigma — nome real por confirmar.
    // getters/setters
}