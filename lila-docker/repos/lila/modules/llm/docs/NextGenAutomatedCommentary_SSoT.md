# Next-Generation LLM Commentary Engine Architecture (SSoT)

This document serves as the Single Source of Truth (SSoT) for the future evolution of the `llm` commentary module. The goal is to elevate the commentary from a "Static Pattern Matcher" to a "Deep Causal Reasoning Engine" capable of providing Master-level, book-quality explanations that justify a premium user experience.

The current `NarrativeLexicon` and structural tagging systems are robust and expressive. The next phase focuses entirely on injecting **Deep Causality**, **Long-Term Context**, and **Domain-Specific Logic** into the engine's analytical core.

---

## 1. Counterfactual Analysis (반사실적 교차 검증)

**Goal:** Explain the *Why* behind moves by analyzing what happens if they are not made. Move beyond "this was a good move" to "this move was necessary because otherwise X happens."

**Core Mechanisms:**
*   **Null Move Profiling:** Introduce a mechanism to calculate the Engine PV (Principal Variation) assuming the opponent is allowed to skip their turn (or makes a non-interfering passing move).
*   **Threat Extraction:** By comparing the actual PV with the Null Move PV, definitively identify the *threat* the opponent's previous move created or the *threat* the current move defends against.
*   **Causal Narrative Generation:** Bridge the extracted threat into the narrative.
    *   *Example:* Instead of "Nd5 occupies an outpost", generate "Nd5! A brilliant decision. If White plays passively here with h3, Black's devastating Nxf3+ shatters the kingside. Nd5 forces the queen to retreat while shutting down the attack."

## 2. Multi-Ply Plan Tracking (장기 계획 추적 상태 머신)

**Goal:** Understand chess as a continuous narrative rather than a sequence of isolated 1-ply snapshots. Connect setup moves to their eventual execution.

**Core Mechanisms:**
*   **Stateful Intent Memory:** Create a state machine (e.g., `PlanStateTracker`) that persists identified plans across multiple moves (e.g., recognizing that `a3` on move 15 is preparation for a `b4` minority attack much later).
*   **Phase Transitions:** Track when a plan moves from `Preparation` -> `Execution` -> `Fruition/Failure`.
*   **Contextual Callbacks:** When a critical move is played, the engine must recall the setup from earlier in the game.
    *   *Example:* "Black finally executes the d5 break. This validates the painstaking preparation started with c6 and Nbd7 five moves ago."

## 3. "Pawn Structure" Knowledge Base (폰 구조 기반 지식 베이스)

**Goal:** Evaluate middle-game plans based on well-established, GM-level understanding of central pawn structures, rather than relying solely on engine evaluations or superficial ECO names.

**Core Mechanisms:**
*   **Structure Classifier:** A robust module capable of identifying classical pawn structures (e.g., Carlsbad, French Advance, Isolani, Hanging Pawns, Najdorf/Scheveningen center) regardless of move order or specific opening names.
*   **Structural Playbooks (The Oracle):** A database mapping identified structures to standard, high-level plans for both sides.
    *   *If Carlsbad:* White = Minority Attack on Queenside, Black = Kingside Attack or e4 break.
*   **Plan Alignment Scoring:** Evaluate player moves not just by centipawn loss, but by how well they align with the *correct* strategic playbook for the current pawn structure.

## 4. Endgame Oracle (엔드게임 전용 수학적/개념적 평가기)

**Goal:** Replace the current superficial endgame heuristics with rigorous, rule-based chess theory capable of explaining *how* to convert or draw an endgame.

**Core Mechanisms:**
*   **Rule-Based Evaluators:** Implement explicit, code-level logic for fundamental endgame concepts:
    *   *Opposition (Direct, Distant, Diagonal)*
    *   *Rule of the Square*
    *   *Key Squares (for pawn promotion)*
    *   *Triangulation (Losing a tempo to gain the opposition)*
    *   *Zugzwang Detection* (Moving beyond hardcoded values to actual calculation)
*   **Theoretical Positions Library:** Recognize forced win/draw patterns (e.g., Lucena Position, Philidor Position, Vancura Defense) and explain them textually rather than simply reporting a `+5.0` or `0.0` evaluation.
    *   *Example:* "By playing Rg2, White seamlessly enters the Lucena position, guaranteeing the pawn's promotion using the classic 'building a bridge' technique."

---

*This document should be referenced when designing and implementing new analytical capabilities for the LLM module.*
