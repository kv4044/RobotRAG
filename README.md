# NeymarRAG — Explorer Agent for the 3D RAG Arena

An autonomous Java agent that plays in the **3D RAG Arena**: it connects to a
remote server, reads real-time telemetry and makes its own decisions, combining
a **deterministic engine** (navigation, heat map, combat) with a local
**RAG pipeline** (solves vault riddles using Ollama).

Academic project — Instituto Politécnico de Tomar.

🏆 **Class champion** in the battle phase — see [Results](#results).

---

## Contents
1. [How it works](#how-it-works)
2. [Architecture](#architecture)
3. [Requirements](#requirements)
4. [Installing Ollama](#installing-ollama)
5. [Building the project](#building-the-project)
6. [Getting a room code](#getting-a-room-code)
7. [Running the agent](#running-the-agent)
8. [Game modes](#game-modes)
9. [Package structure](#package-structure)
10. [Results](#results)
11. [Team](#team)

---

## How it works

The agent runs an endless **Sense-Think-Act** loop:

1. **Sense** — requests telemetry from the server (`/perceive`): position, HP,
   walls, resources, vaults and rival robots within the vision radius (4.5 units).
2. **Think** — the deterministic engine picks the next action based on:
    - **Collision filter**: removes directions blocked by walls.
    - **Combat** (Battle mode only): attacks or flees depending on the HP difference.
    - **Attraction**: shortest path (BFS) to the nearest resource or vault.
    - **Heat map**: when there is no target, explores the least-visited areas.
    - **Frontier exploration**: if the surroundings are already known, heads
      for territory that hasn't been mapped yet.
3. **Act** — sends the chosen action (`/action`): `MOVER_NORTE/SUL/ESTE/OESTE`
   (move north/south/east/west).

When the robot steps on a **vault**, it triggers the **RAG pipeline**: it
downloads the technical manual, splits it into chunks and vectorises them
(embeddings). When it reads the riddle on the terminal, it uses **cosine
similarity** to find the relevant paragraph and a small language model
(`qwen2.5-coder`) to extract the unlock key, which is submitted to `/unlock`.

Each cycle pauses (`Thread.sleep`, ≥ 400 ms) to respect the server's
anti-flood system.

A **visual panel** (Java Swing) shows the heat map in real time: visited cells
(with a counter), walls, resources (green), vaults (yellow; "F" if failed; they
disappear once opened), cells already seen (grey) and rivals.

---

## Architecture

Two halves, split by responsibility (strict single-responsibility principle):

**Deterministic engine** (navigation and communication)
- `AgenteExplorador` — orchestrator; runs the loop and only delegates.
- `ArenaClient` — HTTP communication with the Arena only.
- `MotorHeuristico` — every decision (heuristics, memory, combat).
- `PainelMapaCalor` — drawing only (Swing).

**RAG pipeline** (riddle solving)
- `MotorRAG` (interface) — `ingerirManual`, `resolverEnigma`, `estaPronto`.
- `OllamaClient` — communication with Ollama only (embeddings + generation).
- `DocumentoVetorial` — model: text chunk + vector.
- `RespostaRAG` — result: key, similarity, success.

The deterministic engine and the RAG pipeline talk only through the `MotorRAG`
interface — neither side knows the other's internals.

---

## Requirements

- **Java 11+** (uses the built-in `java.net.http.HttpClient`)
- **Maven** (dependency management)
- **Ollama** (local AI engine)
- An internet connection (the Arena server runs in the cloud)

Main dependency (already in `pom.xml`): Gson 2.10.1.

---

## Installing Ollama

The AI "brain" runs **locally** on your machine, not on the server.

**1. Install** Ollama from https://ollama.com/download (Windows, macOS or Linux).

**2. Download the two required models** (in a terminal):

```bash
ollama pull qwen2.5-coder:0.5b-instruct-q4_K_M
ollama pull nomic-embed-text
```

- `qwen2.5-coder:0.5b-instruct` → extracts the key from the riddle (reasoning/logic).
- `nomic-embed-text` → turns text into vectors (semantic search).

**3. Check** that Ollama is running. Open http://localhost:11434 in your
browser — it should say "Ollama is running".

Ollama must be running in the background **before** you start the agent.

---

## Building the project

From the project root:

```bash
mvn clean install
```

Make sure your IDE (IntelliJ) re-imports Maven after any change to `pom.xml`.

---

## Getting a room code

The Arena identifies each match by a **6-character room code**
(e.g. `D7EE87`). To get one:

1. Open the server's web console in your browser.
2. In the **"Instanciar Nova Arena"** (create new arena) panel, enter the scenario password:
    - `aluno_treino_2026` → **Mission** scenario (peaceful training)
    - `arena_batalha_global` → **Battle** scenario (combat enabled)
3. Click **"Gerar Código de Sala"** (generate room code). The server returns a
   6-character code — **that** is what you enter in the agent's menu (not the password).
4. To watch the robot live in 3D, use the **"Aceder a Arena Existente"**
   (join existing arena) panel with the same code.

> The room code changes every session. Don't hard-code it — enter it in the
> menu each time you start the agent.

The live API documentation is available at `/docs` (Swagger UI) on the server.

---

## Running the agent

On startup, a **configuration menu** lets you set:

- **ID do Robô** (robot ID) — your agent's name (default: `Neymar`).
- **ID da Sala** (room ID) — the 6-character code generated above (default: empty).
- **Modo de Jogo** (game mode) — `Missão` (Mission) or `Batalha` (Battle).

Fill it in and confirm: the robot joins the room and starts playing on its own.
A window opens with the heat map updating in real time.

To run it from the IDE, run the `AgenteExplorador` class (`main` method).

---

## Game modes

**Mission** — combat disabled. The robot focuses on exploring the maze,
navigating without hitting walls and opening every vault by solving the riddles
with RAG, while keeping HP penalties to a minimum.

**Battle (Battle Royale)** — combat enabled. Besides opening vaults, the robot
spots rivals on its radar and decides whether to **attack or flee** based on
the energy difference: it attacks if it has more HP than the rival (and more
than 50 HP), and flees along a safe route (BFS) when at a disadvantage. Vaults
still heal (+100 HP) and can turn a fight around.

---

## Package structure

```
org.example
├── AgenteExplorador        # orchestrator (Sense-Think-Act loop)
├── Configuracao            # session config (room, robot, mode)
├── modelo/                 # DTOs: Percecao, Recurso, Cofre, OutroRobot, ...
├── rede/                   # ArenaClient (HTTP)
├── heuristica/             # MotorHeuristico (deterministic decisions)
├── rag/                    # MotorRAG, OllamaClient, DocumentoVetorial
└── ui/                     # PainelMapaCalor, MenuConfiguracao
```

---

## Physics notes (server)

- Starting HP 200, cap 250, death at 0 (no respawn).
- Moving: −1 HP · resource: +20 HP · vault opened: +100 HP · failed unlock: −10 HP.
- Vision radius: 4.5 units; walls block movement **and** vision (occlusion).
- Respect the anti-flood limit: pause ≥ 400 ms between cycles.

---

## Results

The agent was evaluated in two phases, against the robots of the other teams in
the class and a reference robot provided by the teacher:

- **Collection phase (Mission)**: open every vault on the map. ✅ Completed successfully.
- **Battle phase**: 🏆 **class champion**. Beat every opponent, including the
  runner-up, whose strategy was to run away from everyone and stockpile energy.

### Energy and combat strategy

Energy (HP) is managed with thresholds:

| Energy | Behaviour |
|---|---|
| **< 80** | **Emergency**: heads for the nearest resource or vault (both heal), including ones it has memorised |
| **80 – 229** | **Opportunistic**: keeps on with the mission but picks up energy along the way |
| **≥ 230** | **Vault focus** (230 = the 250 cap − 20 from a resource, so no energy is wasted) |

In Battle mode, combat takes top priority:

- **Attacks** if it has more energy than the rival and more than 50 HP: chases it
  along the shortest path (BFS), around walls, until it is knocked out.
- **Flees** if it has no advantage and the rival is 2 cells away or closer,
  picking the cell furthest from it.
- Otherwise it **ignores** the rival and carries on with the mission.

---

## Team

The work was split along the two halves of the architecture:

| Member | Responsible for |
|---|---|
| **Kaiky Ferreira** — [kv4044](https://github.com/kv4044) | **RAG pipeline**: Ollama integration (embeddings and generation), vectorising the manual, *cosine similarity* search, key extraction and vault unlocking (`rag/`, `OllamaClient`) |
| **Victor Lopes** — [MarcondezVictor](https://github.com/MarcondezVictor) | **Deterministic engine**: Sense-Think-Act loop, reading and interpreting the map, movement and navigation (BFS), heat map and frontier exploration, energy and combat strategy, visual panel and configuration menu (`AgenteExplorador`, `heuristica/`, `rede/ArenaClient`, `ui/`, `modelo/`) |
