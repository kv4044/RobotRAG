# NeymarRAG — Agente Explorador na Arena 3D RAG

Agente autónomo em Java que joga na **Arena 3D RAG**: liga-se a um servidor
remoto, lê telemetria em tempo real e decide as suas ações sozinho, combinando
um **motor determinístico** (navegação, mapa de calor, combate) com um
**pipeline RAG** local (resolve enigmas de cofres via Ollama).

Projeto académico — Instituto Politécnico de Tomar.

---

## Índice
1. [Como funciona](#como-funciona)
2. [Arquitetura](#arquitetura)
3. [Requisitos](#requisitos)
4. [Instalação do Ollama](#instalação-do-ollama)
5. [Compilar o projeto](#compilar-o-projeto)
6. [Obter o código da sala](#obter-o-código-da-sala)
7. [Executar o agente](#executar-o-agente)
8. [Modos de jogo](#modos-de-jogo)
9. [Estrutura de pacotes](#estrutura-de-pacotes)
10. [Resultados](#resultados)
11. [Equipa](#equipa)

---

## Como funciona

O agente corre um ciclo infinito **Sense-Think-Act** (Perceção → Decisão → Ação):

1. **Sense** — pede telemetria ao servidor (`/perceive`): posição, HP, muros,
   recursos, cofres e robôs rivais dentro do raio de visão (4.5 unidades).
2. **Think** — o motor determinístico decide a próxima ação com base em:
    - **Filtro de colisões**: remove direções bloqueadas por muros.
    - **Combate** (só em Batalha): ataca ou foge conforme o delta de HP.
    - **Atração**: rota mais curta (BFS) até ao recurso ou cofre mais próximo.
    - **Mapa de calor**: quando não há alvo, explora as zonas menos visitadas.
    - **Exploração por fronteira**: se o redor já é conhecido, procura o
      território ainda não mapeado.
3. **Act** — envia a ação escolhida (`/action`): `MOVER_NORTE/SUL/ESTE/OESTE`.

Quando o robô pisa um **cofre**, dispara o **pipeline RAG**: descarrega o manual
técnico, parte-o em blocos, vetoriza-os (embeddings), e ao ler o enigma do
terminal usa **cosine similarity** para achar o parágrafo relevante e um SLM
(`qwen2.5-coder`) para extrair a chave de desbloqueio, que é submetida em
`/unlock`.

Entre cada ciclo há uma pausa (`Thread.sleep`, ≥ 400 ms) para respeitar o
sistema anti-flood do servidor.

Um **painel visual** (Java Swing) mostra em tempo real o mapa de calor: casas
visitadas (com contador), muros, recursos (verde), cofres (amarelo; "F" se
falhado; desaparecem ao serem abertos), células já vistas (cinza) e rivais.

---

## Arquitetura

Dois hemisférios separados por responsabilidade (SRP estrito):

**Motor Determinístico** (navegação e comunicação)
- `AgenteExplorador` — orquestrador; corre o loop, só delega.
- `ArenaClient` — só HTTP com a Arena.
- `MotorHeuristico` — todas as decisões (heurística, memória, combate).
- `PainelMapaCalor` — só desenho (Swing).

**Pipeline RAG** (resolução de enigmas)
- `MotorRAG` (interface) — `ingerirManual`, `resolverEnigma`, `estaPronto`.
- `OllamaClient` — só comunicação com o Ollama (embeddings + geração).
- `DocumentoVetorial` — modelo: bloco de texto + vetor.
- `RespostaRAG` — resultado: chave, similaridade, sucesso.

O motor determinístico e o RAG comunicam apenas pela interface `MotorRAG` —
nenhum lado conhece os detalhes internos do outro.

---

## Requisitos

- **Java 11+** (usa `java.net.http.HttpClient` nativo)
- **Maven** (gestão de dependências)
- **Ollama** (motor de IA local)
- Ligação à internet (servidor da Arena está na cloud)

Dependência principal (já no `pom.xml`): Gson 2.10.1.

---

## Instalação do Ollama

O "cérebro" de IA corre **localmente** na tua máquina, não no servidor.

**1. Instalar** o Ollama a partir de https://ollama.com/download
(Windows, macOS ou Linux).

**2. Descarregar os dois modelos** necessários (num terminal):

```bash
ollama pull qwen2.5-coder:0.5b-instruct-q4_K_M
ollama pull nomic-embed-text
```

- `qwen2.5-coder:0.5b-instruct` → extrai a chave do enigma (raciocínio/lógica).
- `nomic-embed-text` → converte texto em vetores (busca semântica).

**3. Verificar** que o Ollama está a correr. Abre no browser:
http://localhost:11434 — deve mostrar "Ollama is running".

O Ollama tem de estar ativo em segundo plano **antes** de arrancar o agente.

---

## Compilar o projeto

Na raiz do projeto:

```bash
mvn clean install
```

Garante que a IDE (IntelliJ) reimporta o Maven após alterações ao `pom.xml`.

---

## Obter o código da sala

A Arena identifica cada partida por um **código de sala de 6 caracteres**
(ex.: `D7EE87`). Para o obter:

1. Abre a consola web do servidor no browser.
2. No painel **"Instanciar Nova Arena"**, insere a password do cenário:
    - `aluno_treino_2026` → cenário de **Missão** (treino pacífico)
    - `arena_batalha_global` → cenário de **Batalha** (combate ativo)
3. Clica em **"Gerar Código de Sala"**. O servidor devolve um código de 6
   caracteres — **é esse** que introduzes no menu do agente (não a password).
4. Para acompanhar o robô ao vivo em 3D, usa o painel **"Aceder a Arena
   Existente"** com o mesmo código.

> O código de sala muda a cada sessão. Não o fixes no código — introduz-lo no
> menu a cada arranque.

A documentação viva da API está em `/docs` (Swagger UI) no servidor.

---

## Executar o agente

Ao arrancar, aparece um **menu de configuração** onde defines:

- **ID do Robô** — nome do teu agente (por defeito: `Neymar`).
- **ID da Sala** — o código de 6 caracteres gerado acima (por defeito: vazio).
- **Modo de Jogo** — `Missão` ou `Batalha`.

Preenche, confirma, e o robô liga-se à sala e começa a jogar de forma autónoma.
Uma janela abre com o mapa de calor a atualizar em tempo real.

Para correr pela IDE: executa a classe `AgenteExplorador` (método `main`).

---

## Modos de jogo

**Missão** — combate desativado. O robô foca-se em explorar o labirinto,
navegar sem bater em paredes, e abrir todos os cofres resolvendo os enigmas via
RAG, minimizando penalizações de HP.

**Batalha (Battle Royale)** — combate ativo. Além de abrir cofres, o robô deteta
rivais no radar e decide **atacar ou fugir** pelo delta de energia: ataca se
tiver mais HP que o rival (e HP > 50), foge com rota segura (BFS) se estiver em
desvantagem. Os cofres continuam a curar (+100 HP), podendo virar a batalha.

---

## Estrutura de pacotes

```
org.example
├── AgenteExplorador        # orquestrador (loop Sense-Think-Act)
├── Configuracao            # config da sessão (sala, robô, modo)
├── modelo/                 # DTOs: Percecao, Recurso, Cofre, OutroRobot, ...
├── rede/                   # ArenaClient (HTTP)
├── heuristica/             # MotorHeuristico (decisão determinística)
├── rag/                    # MotorRAG, OllamaClient, DocumentoVetorial
└── ui/                     # PainelMapaCalor, MenuConfiguracao
```

---

## Notas de física (servidor)

- HP inicial 200, teto 250, morte a 0 (sem respawn).
- Movimento: −1 HP · recurso: +20 HP · cofre aberto: +100 HP · unlock falhado: −10 HP.
- Raio de visão: 4.5 unidades; muros bloqueiam movimento **e** visão (oclusão).
- Respeita o anti-flood: pausa ≥ 400 ms entre ciclos.

---

## Resultados

O agente foi avaliado em duas fases, contra os robôs das outras duplas da turma
e um robô de referência do professor:

- **Fase de coleta (Missão)**: abrir todos os cofres do mapa. ✅ Concluída com sucesso.
- **Fase de luta (Batalha)**: 🏆 **campeão da turma**. Venceu todos os
  adversários, incluindo o 2.º classificado, cuja estratégia era fugir de todos
  e acumular energia.

### Estratégia de energia e combate

A energia (HP) é gerida por limiares:

| Energia | Comportamento |
|---|---|
| **< 80** | **Emergência**: vai ao recurso ou cofre mais próximo (ambos curam), incluindo os que já memorizou |
| **80 – 229** | **Oportunista**: segue a missão, mas apanha energia que esteja no caminho |
| **≥ 230** | **Foco nos cofres** (230 = teto de 250 − 20 de um recurso, para não desperdiçar energia) |

Em modo Batalha, o combate tem prioridade máxima:

- **Ataca** se tiver mais energia que o rival e mais de 50 HP: persegue-o pela
  rota mais curta (BFS), contornando muros, até ao abate.
- **Foge** se não tiver vantagem e o rival estiver a 2 blocos ou menos,
  escolhendo a casa mais afastada dele.
- Caso contrário, **ignora** o rival e continua a missão.

---

## Equipa

O projeto foi dividido pelos dois hemisférios da arquitetura:

| Membro | Responsável por |
|---|---|
| **Kaiky Ferreira** — [kv4044](https://github.com/kv4044) | **Pipeline RAG**: integração com o Ollama (embeddings e geração), vetorização do manual, pesquisa por *cosine similarity*, extração da chave e desbloqueio dos cofres (`rag/`, `OllamaClient`) |
| **Victor Lopes** — [MarcondezVictor](https://github.com/MarcondezVictor) | **Motor determinístico**: ciclo Sense-Think-Act, leitura e interpretação do mapa, movimento e navegação (BFS), mapa de calor e exploração por fronteira, estratégia de energia e combate, painel visual e menu de configuração (`AgenteExplorador`, `heuristica/`, `rede/ArenaClient`, `ui/`, `modelo/`) |
