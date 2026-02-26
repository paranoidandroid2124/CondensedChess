# High‑Effort Book‑Style Commentary — 최대 품질 설계서 (Bookmaker / LLM 모듈)

- 문서 목적: “사람(책 저자)의 관점”을 최대한 반영한 **책 스타일 해설**을, 현재 Bookmaker 파이프라인 위에서 **최대 노력(지연시간/엔진 예산 풍부)**으로 구현하기 위한 **완전한 스펙/로드맵**을 제공한다.
- 독자: 서버/프론트 개발자, 체스 도메인 리뷰어, QA/코퍼스 관리자
- 범위: 라이브 코멘트(우선) + 오프라인 게임 분석(동일 골격 재사용)
- 마지막 업데이트: 2026‑01‑21 (Phase 6.8 완료)

---

## TL;DR (핵심 결론)

1) 현재 시스템은 “보드 인식 → 휴먼 태그 → 문장 템플릿”을 하고 있지만, 책 해설의 본체인 **저자 질문(결정지점) 선택 / 분기 증명 / 교훈 설계** 레이어가 얇아서, 결과적으로 **엔진 PV에 과도하게 결박**되어 있다.  
2) “최대 품질(책 수준)”로 가려면 파이프라인을 버릴 필요는 없지만, **주도권을 PV에서 저자 질문으로 옮기는 상위 레이어(AuthorQuestions/EvidencePlan/Outline/Post‑Critic)** 를 추가해야 한다.  
3) 지연시간/엔진 예산을 풍부하게 쓸 수 있다면, 라이브는 “한 번의 응답”이 아니라 **분석 세션**으로 설계하는 게 정답이다(즉시 요약 → 심층 증거 수집 → 책 문단으로 업그레이드).

## 목차(Quick Index)

- 1) 용어/정의(Glossary)
- 2) 현 상태(As‑Is) 파이프라인
- 3) 실제 테스트에서 드러난 대표 문제
- 4) 지금까지 적용된 개선(A–D + Dedup + Phase 6.8 Density)
- 5) PV 결박(왜 빈약해지는가)
- 6) 목표(To‑Be)와 축(axis) 설계
- 7) 라이브 심층 세션 모델
- 8) AuthorQuestions/EvidencePlan/Outline/Post‑Critic 스펙
- 8.6) Tempo‑Injected Conditional Plans (PV 밖 장기 플랜)
- 8.7) Idea Space Seed Library (최대 품질)
- 9) 기존 파이프라인과의 통합(버리는 것 vs 얹는 것)
- 10) 4개 코퍼스 케이스 플레이북(구체 예시)
- 11) 구현 로드맵(최대 노력)
- 12) 품질 지표/검증(DoD)
- 13) 코드 맵

---

## 1) 용어/정의 (Glossary)

- **Analysis FEN**: 해설 대상 포지션(보통 “수 두기 전”의 FEN).  
  - move‑annotation 모드에서 `NarrativeContext.fen`이 이 값을 가진다.
- **Played move**: 사용자가 실제로 둔 수(UCI). `NarrativeContext.playedMove`
- **Best move**: 엔진/평가 기준 PV1의 첫 수(UCI).
- **PV / MultiPV**: 엔진 변동(Variation) 라인. MultiPV는 상위 후보 여러 개.
- **Probe**: 특정 후보 수를 고정한 뒤, 클라이언트 엔진으로 더 깊게 분석해 증거를 얻는 루프.
- **Branch point / Decision point**: 책 해설에서 “여기서 무엇을 결정하는가?”에 해당하는 갈림길(긴장 해소, 구조 고정, 교환, 전개 우선순위 등).
- **Author question**: 저자가 독자에게 던지는 질문(2~3개)이 글의 구조를 만든다.

---

## 2) 현 상태(As‑Is) 파이프라인 — 무엇이 이미 되어 있고, 무엇이 부족한가

### 2.1 서버/클라 엔드포인트 흐름 (현 구현)

라이브 Bookmaker는 두 단계로 동작한다:

1) **Briefing (Stage 1, cheap)**  
   - API: `POST /api/llm/bookmaker-briefing` (`app/controllers/LlmController.scala`)  
   - 목적: 즉시 읽을 수 있는 짧은 문장(“상황/의도”) 제공

2) **Deep commentary (Stage 2, rule‑based, book‑style)**  
   - API: `POST /api/llm/bookmaker-position`  
   - 입력: FEN + playedMove + MultiPV(가능) + (optional) probeResults  
   - 출력: 본문 prose + variations + (optional) probeRequests
   - 현재 운영 가정: Stage 2는 **규칙 기반(서버)** 으로 “체스 내용”을 만들고, (추가 LLM이 있다면) 그 이후에 **문장만 폴리시**하는 역할로 제한한다.

3) **Probe loop (Stage 2b, client‑side engine evidence)**  
   - Stage 2가 `probeRequests`를 주면, 클라가 엔진으로 분석 → `probeResults` 생성  
   - 같은 Stage 2 endpoint를 `probeResults` 포함하여 재호출 → a1/a2 분기 근거 포함

관련 코드:
- 서버: `modules/llm/src/main/LlmApi.scala`, `app/controllers/LlmController.scala`
- UI: `ui/analyse/src/bookmaker.ts` (Briefing → Stage2 → probes → Stage2 재호출)

### 2.2 데이터 흐름(ASCII)

```
[Client ceval MultiPV] + (optional probeResults)
        |
        v
CommentaryEngine.assessExtended
  - features/motifs/plans/threats/pawn policy
  - candidates/alternatives/counterfactual
        |
        v
NarrativeContextBuilder
  - summary/threats/plans/pawnPlay/targets/meta
  - candidates a/b/c + whyNot + a1/a2(probeLines)
  - probeRequests(필요 시)
        |
        v
BookStyleRenderer
  - move annotation prose + alternatives + masters paragraph(옵션)
        |
        v
[UI render]
```

### 2.3 무엇이 “책”과 가장 다른가(핵심 구조적 격차)

책 해설은 일반적으로:
- ① **질문(결정지점) 제시** → ② 대표 분기 2~3개로 **증명** → ③ 독자가 해야 할 행동(교훈)으로 **수렴**

반면 현재 구조는 대부분:
- ① 후보(a/b/c) + ② PV 샘플라인 + ③ 평가/마무리

즉, “무엇을 말할지(저자 질문)”이 PV/태그에서 **후행적으로** 생기고, “분기”도 설명 목적이 아니라 엔진 출력의 부산물이 되기 쉽다.

---

## 3) 실제 테스트에서 드러난 대표 문제(원인 분석용)

### 3.1 코퍼스 4케이스에서 관찰된 결함 유형

| 범주 | 증상 | 결과 |
|---|---|---|
| 정확성(치명) | 불법 수순/즉시 되잡히는 캡처를 “이득”처럼 서술 | 신뢰 붕괴 |
| 논리/주체(치명) | “White seeks to prove that c4 and d4 are weak points” 같은 주체 뒤집힘 | 독자가 즉시 거부감 |
| 증거(중대) | recapture 선택지가 여러 개인데 단일 라인만 가정(“한 줄 세계관”) | 설득력 부족 |
| 맥락(중대) | “Philidor / Legal’s mate motif”인데 핵심 전술 테마가 서술 중심이 아님 | 테마 포지션에서 실패 |
| 스타일(경미~중대) | “roughly balanced” 반복, “The game begins” 같은 상투어 | 정보 밀도 저하 |
| 출처/DB(중대) | Masters games 숫자/실명/연도 인용이 검증 없이 등장하면 환각으로 보임 | 신뢰 붕괴 |

### 3.2 책 해설(인간)과의 근본 차이

책은:
- “대표 분기(구조 선택)”를 먼저 고른다(예: …exd5 / …Nxd5 / …cxd5).
- 독자가 빠질 함정을 정확히 찌른다(예: Legal’s mate에서 5…Bxd1?? 금지).
- “왜”를 한 문단 안에서 촘촘히 엮는다(계획 ↔ 반격 ↔ 교훈).

우리 시스템은:
- PV가 주도권을 가지고, 거기서 태그/문장을 뽑는다.
- 질문이 아니라 “후보 리스트”가 구조를 만든다.

---

## 4) 지금까지 적용된 개선(A–D) — 품질 바닥(신뢰) 깔기

> 이 섹션은 “현재 코드가 어디까지 해결했는가”를 설명한다.  
> (A–D는 내부 작업 구분이며, 앞으로도 로드맵 기준점으로 사용한다.)

### A) 정확성 게이트(legal / PV 파손 / 잘린 캡처)

목표: “불법/즉시 반박/트렁크 PV”로 신뢰가 깨지는 것을 차단.

핵심:
- PV parsing이 부분 성공(중간에서 끊김)일 경우, SAN 라인을 **인용하지 않는다**.
- 인용 라인이 마지막이 “캡처로 끝”나는 경우(즉시 recapture 가능성) 회피.
- Masters 인용 라인은 FEN 기반으로 검증, 불법이면 “라인 출력”을 하지 않고 안전한 표현으로 다운그레이드.

관련 코드:
- `modules/llm/src/main/scala/lila/llm/analysis/BookStyleRenderer.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/NarrativeUtils.scala` (`sanLineToUciList`)
- `modules/llm/src/main/scala/lila/llm/model/Variation.scala` (샘플라인 캡처 종료 방지)

### B) 주체/소유자(ownership) 뒤집힘 방지

목표: “White가 자기 약점을 ‘증명’한다” 같은 자해 문장 제거.

핵심:
- WeakComplex에 owner/색상/점유 여부 등을 명확히 보관하고, 렌더 단계에서 “누구의 약점인지”를 엄격히 판정.

관련 코드:
- `modules/llm/src/main/scala/lila/llm/model/NarrativeContext.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/NarrativeContextBuilder.scala`

### C) Practicality/오프닝 노이즈

목표: opening 초반에 의미 없는 “bad bishop” 같은 안전빵 노이즈를 줄이고, practical framing이 방향을 뒤집지 않게.

핵심:
- practicality 계산에 **양쪽 pieceActivity**가 모두 들어가도록 계약 수정
- “good/bad bishop” 태그는 중앙 폰 커밋이 생긴 뒤(예: 중앙 폰 advance 충분)만 생성

관련 코드:
- `modules/llm/src/main/scala/lila/llm/analysis/StrategicFeatureExtractorImpl.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/strategic/StrategicAnalyzers.scala`

### D) “한 줄 세계관” → probe 기반 분기 확보

목표: “recapture/구조 선택지가 여러 개인데 단일 라인만 말하는” 문제를 시스템적으로 깨기.

핵심:
- `ProbeDetector`가 단순 ghost plan 뿐 아니라,
  - PV 불신 시 `pv_repair_*`
  - playedMove가 topPV에 없고 중요한 수일 때 `played_*`
  - 긴장 정책이 Maintain인데 캡처로 긴장을 푸는 후보가 있을 때 `tension_*`
  를 probe로 요청.
- 클라이언트는 probe eval MultiPV를 `ProbeResult.replyPvs`로 반환하고, 서버는 이를 SAN으로 변환하여 `a1/a2` 분기 라인으로 렌더.

관련 코드:
- `modules/llm/src/main/scala/lila/llm/analysis/ProbeDetector.scala`
- `modules/llm/src/main/scala/lila/llm/model/ProbeModel.scala`
- `ui/analyse/src/bookmaker.ts`
- `modules/llm/src/main/scala/lila/llm/analysis/NarrativeContextBuilder.scala`
- `modules/llm/src/main/scala/lila/llm/analysis/BookStyleRenderer.scala`

### E) 코드 품질/중복 제거 (Dedup & SSOT)

목표: 기능 변경 없이도 “환각처럼 보이는 구현 중복”을 줄이고, 장기 유지보수를 가능하게.

대표 정리 포인트:
- `humanize()` 래퍼: 여러 파일에서 작은 래퍼가 생기기 쉬우므로 `NarrativeUtils.humanize`를 SSOT로 유지(래퍼는 최소화).
- UCI→SAN: “topMoves lookup → FEN 기반 변환 → fallback” 패턴이 중복되기 쉬움.  
  - SSOT: `NarrativeUtils.uciToSanOrFormat` + `NarrativeUtils.formatUciAsSan`  
  - Opening 쪽은 `OpeningEventDetector`가 SSOT를 호출하도록 유지.
- Threshold 상수: 여기저기 흩어지면 의미/튜닝이 무너짐 → `analysis/Thresholds.scala`로 통합 유지.
- Mate(체크메이트) 탐지: “#” 포함 여부/`mate` 필드 확인 같은 패턴이 여러 곳에서 반복될 수 있음(LOW).  
  - 원칙: “탐지 규칙”은 한 곳(NarrativeUtils 또는 전용 헬퍼)로 모으고, 렌더는 결과만 소비.
- 테스트 픽스처: 일부 반복은 “테스트 격리/명확성”을 위해 허용하되, 불필요한 환각성 숫자/DB 주장(Fake masters games)은 금지.

### F) 커버리지/견고성(기초 모티프/플랜 공백 방지)

목표: “아무 것도 못 찾았다”류의 공백을 줄이고, 계획/태그/근거가 최소한이라도 생성되게 하여 내러티브가 붕괴하지 않도록 한다.

대표 강화(현 구현 포함):
- 상태 모티프 커버리지 보강: `DoubledPawns`, `PawnChain`, static `Pin` 등 “보드에서 바로 읽히는 것”을 PV 없이도 생성할 수 있게 확장.
- Endgame feature: 특정 엔드게임 윈도우에서는 분석 결과를 항상 반환(호출부가 None 처리로 붕괴하지 않게).
- Plan fallback: 계획이 비는 경우에도 phase 기반 기본 plan을 최소 1개 제공하여 렌더/테이블이 안전하게 동작하도록 fail‑closed.

---

## 5) 우리가 가장 심하게 묶인 곳(PV 결박) — 왜 ‘빈약함’이 생기는가

현재 구조에서 PV 결박이 강한 지점:
- 모티프/전술 인식의 기본 경로가 상태/궤적 provider(`MoveAnalyzer.tokenizePv`)로 전환되어, PV 품질 편향을 완화한다(legacy path는 parity/rollback 용도로만 유지).
- 대안/후보 구조가 MultiPV 분포에 매임 → “설명 목적 분기”가 아니라 “엔진이 준 분기”가 된다.
- probe도 baseline/topPvMoves 중심 → PV가 앵커 역할을 한다(보강은 되지만 구심점이 되기 쉽다).

이 결박을 풀기 위해서는 **주도권 SSOT를 PV에서 “저자 질문/결정지점”으로 이동**시켜야 한다.

---

## 6) 목표(To‑Be): “최대 품질” 책 해설 시스템 설계

### 6.1 설계 원칙 (Principles)

1) **Truth first**: 그럴듯함보다 검증 통과가 우선.  
2) **Question first**: 매 포지션 2~3개의 저자 질문이 글의 뼈대(Outline)를 만든다.  
3) **Evidence is planned**: PV는 증거 후보일 뿐, 질문을 증명하기 위한 분기를 설계한다.  
4) **Teach, don’t narrate**: 독자 실수 패턴과 행동 지침으로 끝난다.  
5) **Deep is a session**: 라이브는 한 번에 끝이 아니라 “심층 세션” 결과로 업그레이드된다.  

### 6.2 축(axis) 기반 전체 구성

- A. Truth / Validation Gate
- B. Author Questions (2~3) = 글의 구심점
- C. Evidence Orchestration (probe/engine/DB를 질문 증명용으로 통제)
- D. Narrative Outline (결정지점 중심 문단)
- E. Pedagogy (독자 모델/교훈)
- F. Voice (문장/리듬/밀도)

---

## 7) 최대 품질을 위한 “라이브 심층 세션” 모델

지연시간이 충분하고(“바로”보다 “가장 좋은”), 엔진 예산도 충분하다는 가정에서 라이브를 다음처럼 설계한다.

### 7.1 2단계 UX: 즉시 + 심층 업그레이드

```
(사용자 수 두기)
   |
   v
Stage 0: 즉시 요약(0.2~0.8s)
- 안전 경보(메이트/큰 손실/전술 지뢰)
- 한 문단 브리핑(상황 + 의도)

Stage 1: 심층 세션(5~30s, 예산 풍부)
- AuthorQuestions 생성(2~3)
- EvidencePlan 수립(분기 설계)
- 엔진/프로브/DB로 증거 수집(멀티PV/recapture)
- 검증 게이트 통과한 증거만 사용
- Outline 기반 책 문단 생성 + 교훈 포함
```

권장 UI:
- Stage 0 텍스트를 먼저 보여주고, Stage 1이 완료되면 **동일 영역의 텍스트를 업그레이드**.
- “분석 중…”을 명확히 표시(사용자는 ‘최고’를 원하므로 수용 가능).

### 7.2 엔진/프로브 예산(권장 기본값)

> 실제 숫자는 튜닝 대상. 핵심은 “depth↑”보다 “분기 설계↑”가 더 중요하다는 점.

- Base MultiPV: 3 → 5 (가능하면 5)
- Probe per position:
  - DecisionPoint 관련 분기: 2~4
  - TacticalMine 관련 분기: 1~2
  - Total probe moves: 6~12 (현 UI cap은 확장 가능)
- Probe MultiPV replies: 2~3
- 각 probe PV 길이: 8~12 plies (증명용, 덤프 금지)

---

## 8) 신규 상위 레이어 스펙(핵심): AuthorQuestions / EvidencePlan / Outline / Post‑Critic

> 이 4개가 “책 관점”을 시스템에 주입하는 최소 구성요소이자, 최대 품질의 핵심이다.

### 8.1 AuthorQuestions (저자 질문) — SSOT

각 포지션에서 최대 3개 질문을 생성한다.

#### 질문 타입
- `TacticalMine`: “지금 단 하나의 지뢰는 무엇인가?”
- `TensionDecision`: “긴장을 유지/해소 중 무엇이 핵심인가?”
- `PlanClash`: “양측 계획 충돌의 핵심은?”
- `ConversionRule`: “이득을 굳히는 방법(교환/엔드게임 규칙)은?”
- `OpeningTheory`: “이 오프닝에서 ‘이 수’의 의미는?”
- `LatentPlan`(Tempo‑Injected Conditional Plans): “상대가 시간을 주면 어떤 장기 플랜이 작동하는가?” (구현 ✅)

> 구현 메모(현 코드 기준):
> - 현재 `AuthorQuestionKind`는 `TensionDecision/PlanClash/TacticalTest/StructuralCommitment/ConversionPlan/DefensiveTask`를 가진다.
> - 본 문서의 `LatentPlan`은 여기에 **새 kind를 추가**하여 “PV 밖 장기 플랜”을 1st‑class로 다루는 것을 의미한다.

#### 질문 생성 규칙(예시: 스코어링 기반)

- 전술 위협(메이트/큰 손실) 존재 → `TacticalMine` 우선순위 1
- 긴장 정책 Maintain + playedMove가 캡처(긴장 해소) → `TensionDecision` 우선순위 상
- opening + Masters 데이터 존재 + 분기점 변화 → `OpeningTheory` 우선순위 상
- evaluation swing(Δcp 크거나 counterfactual cpLoss 큼) → `ConversionRule` or `TacticalMine`

출력 형태(개념 스키마):
```text
AuthorQuestion(
  id, qType, priority,
  claimPrompt: "무엇을 주장해야 하는가",
  mustMention: ["핵심 단어/테마"],
  evidenceKinds: [BranchRecapture, Refutation, TypicalPlanLine]
)
```

### 8.2 EvidencePlan (질문별 증거 설계)

핵심: PV/프로브는 “그냥 더 깊게”가 아니라 **질문 증명용 분기**를 확보해야 한다.

증거 종류(권장):
- `RefutationLine`: “이 수가 왜 안 되는지”를 증명하는 라인
- `DefenseResource`: 상대의 핵심 방어/자원
- `BranchRecapture`: 캡처 이후 recapture 선택지(구조 선택지) 2~3개
- `TypicalPlanLine`: 전개/계획이 자연스럽게 드러나는 1라인(너무 길지 않게)
- `TheorySnippet`: Masters/DB 근거(검증 통과 시에만)

EvidencePlan은 엔진 작업(ceval/probe)을 “작업 리스트”로 만든다:
```text
EvidenceTask(
  taskId,
  questionId,
  kind,
  fen,
  forcedMove: Option[uci],
  branchMoves: List[uci],     // recapture 후보 등
  multiPv,
  depth/timeBudget
)
```

### 8.3 Truth / Validation Gate (검증 규칙)

모든 라인/주장은 아래 게이트를 통과해야 인용 가능:

1) **Legal SAN/Legal UCI**: FEN에서 순차 적용 가능한가
2) **Recapture sanity**: 라인의 마지막이 캡처로 끝나면(체크/메이트가 아닌 한) 인용 금지 또는 한 수 더 연장
3) **Orientation**: “누가 편해지는지”가 eval/실전성 문장과 모순되지 않는가
4) **Theme presence**: 테마 포지션인데 핵심 테마가 빠지면 fail(다시 증거 수집 or 톤 다운)
5) **DB claims**: Masters 숫자/실명/연도는 실제 데이터 기반일 때만. 그렇지 않으면 비정량 표현으로 다운그레이드.

### 8.4 Narrative Outline (문단 설계)

책 스타일 최소 구조:

1) **Context**: 현재 국면/긴장/핵심 불균형 1~2개
2) **Q1(결정지점)**: “지금 무엇을 결정하는가?” + 분기 2개로 증명
3) **Q2(상대 자원/의도)**: “상대가 무엇을 노리나?” + 핵심 방어/반격 1개
4) **Teaching point**: 흔한 실수 패턴 + 피하는 규칙
5) **Practical wrap‑up**: 객관/실전 결론(과장 금지, 반복 금지)

### 8.5 Post‑Critic (후처리 검열/편집)

목표: “허세 문장 / 상투어 / 반복” 제거.

룰 예:
- “The game begins.” 금지(9수 등에서 부정확)
- “roughly balanced” 반복 제한(문서당 1회)
- “Black has fewer forgiving choices” 같은 단정은 근거가 있을 때만(근거 없으면 톤 다운)

### 8.6 Tempo‑Injected Conditional Plans (구현 ✅)

> 목표: PV/MultiPV에 **직접 등장하지 않아도**, 책 해설처럼 “상대가 시간을 주면(=반응이 느리면) 작동하는 장기 플랜”을 **조건부로** 말하고, 그 조건을 **합법 라인(a/b 분기)** 으로 증명한다.

#### 8.6.1 문제 정의(왜 PV에 없을 수 있는가)

책은 종종 다음 형태의 문장을 쓴다:

- “If Black is slow / does not challenge the centre, White can start g4–g5 and build a kingside pawn storm.”

하지만 엔진 PV는 **최선 방어/최선 반격**을 전제로 하므로, 아래 이유로 g4 같은 장기 플랜이 PV 밖으로 밀릴 수 있다:

- 상대에게 즉시 필요한 방어/반격이 존재하면, PV는 그 라인을 우선한다.
- “g4 폰스톰”은 보통 **상대의 대응이 느릴 때** 가치가 커지고, 최선 방어가 있으면 우선순위가 내려간다.
- 즉, “PV에 없음”은 “플랜이 없음”이 아니라 “지금 당장 최선수순의 첫줄이 아닐 수 있음”이다.

이 격차를 메우려면, 엔진의 null‑move pruning 아이디어(“상대가 한 수 쉰다”)를 **텍스트 근거 생성용**으로 변형해야 한다.

#### 8.6.2 설계 원칙(Truth‑first / 책 톤)

1) **불법 라인 인용 금지**: null move(턴 넘김)는 텍스트에 절대 라인으로 인용하지 않는다.  
2) **조건부 서술 강제**: “will” 금지. 항상 “If allowed / if Black is slow / given time” 프레이밍.  
3) **증거(분기) 없으면 말하지 않음**: 조건부 플랜 문장은 최소 2개 분기(a/b)의 합법 evidence가 있어야 한다.  
4) **전술 오버라이드**: “느린 수”가 즉시 전술 승리/메이트를 허용하면, 장기 플랜이 아니라 “즉시 전술”을 말해야 한다.  
5) **반례(제동) 포함**: 플랜 문장은 반드시 “black’s main countermeasure(…h5/…f5/…c5 등)”를 같이 언급하거나, 최소한 Evidence 라인에서 드러나야 한다.

#### 8.6.3 전체 구조(2단계): Imagination(tempo injection) → Validation(legal quiet branches)

```
              (A) Imagination: Tempo Injection (internal hint only)
Real position ------------------> "What if opponent passes?" -> seed candidates
       |                                                   (NO line citations)
       |
       v
  (B) Validation: Legal quiet branches (evidence)
  pick 2~4 plausible quiet moves for opponent (legal)
       |
       v
  probe each branch -> does our best reply actually start the seed plan?
       |
       v
  if >=2 branches confirm -> write conditional plan sentence + evidence
  else -> omit or convert to "premature / refuted" note
```

#### 8.6.4 데이터 모델(SSOT) — 추가/확장 제안

**A) AuthorQuestions**

- `AuthorQuestionKind`에 추가:
  - `LatentPlan`
- `AuthorQuestion` payload(기존 유지 + anchors 강화):
  - `anchors`: `["g4", "pawn storm", "kingside"]` 같은 “반드시 눈에 띄어야 하는 토큰” 포함

**B) Probe purposes(SSOT)**

- `ProbeRequest.purpose` / `ProbeResult.purpose`에 목적값을 표준화(문자열 SSOT):
  - `tempo_injection_seed`  
    - “상대가 한 수 쉰다(턴만 바뀜)”를 내부 힌트로 사용해 seed 후보를 평가하는 용도
    - **결과는 라인으로 인용 금지**, seed 스코어링/선정에만 사용
  - `free_tempo_branches`  
    - 합법 분기(a/b): 상대의 실제 quiet move(“시간을 주는 수”) 이후, 우리 최선 응수가 seed를 시작하는지 검증 + 인용용 증거 생성
  - `latent_plan_refutation`(선택, 최대 품질 권장)  
    - “seed가 지금은 premature인지”를 빠르게 판단하기 위한 안전장치
    - refutation이 강하면 텍스트는 “매력적이지만 … 때문에 지금은 이르다”로 자동 전환

#### 8.6.5 Tempo Injection(내부 힌트) 구체화 — 엔진 null‑move pruning을 ‘seed scorer’로 사용

핵심은 “턴을 넘기는” 게 아니라, **FEN의 side‑to‑move만 뒤집어** “한 수 더 두는 세상”을 만든다(내부 힌트용).

- 입력 FEN: `afterPlayedFen` (playedMove 적용 후, 보통 상대 차례)
- `tiFen = flipSideToMove(afterPlayedFen)`  
  - 보드는 그대로, side‑to‑move만 뒤집는다(체스 규칙상 ‘도달 가능성’은 무시; 엔진 분석 가능)
- 후보 seed moves는 “무작위”가 아니라 아래 둘을 합친다:
  1) `PlanMatcher`/`PlanScoring`에서 “attack/pawn storm” 관련 plan이 상위일 때 대표 pawn pushes 후보
  2) `LatentPlanSeeder`(구조 기반)에서 g4/h4/f4 등 seed 후보 생성

`ProbeRequest(purpose=tempo_injection_seed)`는 다음을 포함한다:

```text
fen = tiFen
moves = [g2g4, h2h4, f2f4, ...]          // seed 후보들
multiPv = 1 or 2                          // seed 스코어링 용(상대 응수 다양성은 Stage B에서)
depth = HIGH (>= 20)                      // “최대 품질” 목표: 얕은 힌트 금지
```

선정 규칙(예시):
- `evalCp(seed@tiFen)`이 “자살이 아님(즉시 붕괴)” + “의미 있는 공격 시그널(후속 PV에 g4‑g5, 파일 오픈 등)”이면 seed 후보로 통과
- 통과 seed는 최대 1~2개로 제한(문단 수/잡음 통제)

> 중요: 이 단계의 PV/SAN은 텍스트에 절대 인용하지 않는다(불법 세계관).

#### 8.6.6 Validation(합법 증거) — “느린 수”를 새로 분류하지 말고, 엔진으로 ‘plausible quiet moves’를 선별

“느린 수 감지 로직이 복잡해질까?”를 피하려면, 사람이 규칙을 늘리기보다 **엔진을 활용해 ‘그럴듯한 quiet move’만 남긴다.**

1) 후보 생성(법칙 기반 최소 필터):
- `afterPlayedFen`에서 상대 legal moves를 나열
- `quiet` 조건(최소):
  - `!captures && !givesCheck`

2) plausible 필터(엔진/프로브 기반):
- quiet 후보 중 상위 N개(예: 8~12개)에 대해 **shallow screening probe**를 돌린다.
  - depth 10~12, multiPv=1, per‑move time budget 짧게
- 결과가 “즉시 큰 붕괴(예: moverLoss >= 200cp)”인 quiet는 제외(= 사실상 blunder)
- 남은 것 중에서 “실전적으로 흔한” 것(개발/룩 연결/퀸 이동 등)을 우선하되, 최종 선택은 **평가 손실이 ‘작지만 유의미한 slack’** 인 2~4개로 제한한다.

3) 최종 검증(인용용 deep probe):
- 선택된 quiet move 2~4개에 대해, 각 분기에서 우리 최선 응수 PV를 깊게 얻는다.
- 성공 조건:
  - 우리 최선 응수의 1수~2수 내에 seed(g4)가 실제로 등장하거나,
  - PV가 seed가 아니더라도, seed가 “2nd best within small margin”으로 남아 있고(선택), refutation이 약함

이 결과는 `ProbeResult.bestReplyPv`/`replyPvs`를 통해 합법 Evidence 라인(a/b)로 변환 가능하다.

#### 8.6.7 Evidence 라인 생성 규칙(책 스타일용)

`AuthorEvidenceBuilder`에 `free_tempo_branches` 목적을 추가한다고 가정할 때:

- 라인 형태(추천):
  - `playedMove` → `...quietMove` → `seedMove(g4)` → `...bestDefense` 까지 **짧게**
- 최소 라인 길이:
  - seed가 등장한 뒤 상대 제동 한 수까지(“왜 통하는지/어떻게 막는지”가 보이게)
- 안전 규칙(기존 그대로):
  - illegal/partial SAN 금지
  - 캡처로 끝나는 라인 인용 금지(체크/메이트 제외)

#### 8.6.8 내러티브 통합 규칙(BookStyleRenderer/Outline)

출력은 반드시 다음 2개 중 하나로 수렴한다:

**A) Latent plan confirmed (조건부 플랜 문장 + Evidence)**
- 조건:
  - `free_tempo_branches` Evidence가 2개 이상
  - (선택) `latent_plan_refutation`에서 “즉시 자살”이 아님
- 문장 스켈레톤:
  - “If Black is unambitious / gives White time (…a6 / …Re8), White can start g4–g5 …”
  - “Black’s main countermeasure is …h5 / …c5 / …f5 …” (Evidence 라인에서 드러나거나, 짧게 언급)

**B) Latent plan refuted/premature (톤 다운 + 반례)**
- 조건:
  - seed는 매력적이지만, 검증에서 “즉시 전술/반격”으로 붕괴하거나,
  - quiet branches에서 seed가 최선 응수가 아님(또는 한 분기만 성립)
- 문장 스켈레톤:
  - “The idea of g4 is tempting, but premature: Black can respond with …X …”

> 핵심: “검증 없이 멋있어 보이는 장기 플랜”은 금지. Evidence가 없으면 말하지 않는다.

#### 8.6.9 코퍼스/테스트 전략(최대 품질)

새 코퍼스 케이스는 반드시 “PV에는 g4가 없는데, 시간(quiet branch)을 주면 g4가 뜨는” 상황을 포함해야 한다.

- 기대치 예:
  - `mustHaveBeats`: `Evidence`, `TeachingPoint`(조건부 플랜의 교훈)
  - `mustHaveEvidencePurposes`: `free_tempo_branches`
  - `mustContain`: `if black is` / `given time` / `g4`
  - `mustNotContain`: “will play g4” 같은 단정형

또한 “느린 수가 전술 승리를 허용하는 함정” 케이스도 추가한다:
- 기대치: 조건부 플랜 문장 대신 “tactics decide immediately” 류로 전술 오버라이드가 작동해야 한다.

### 8.7 Idea Space Seed Library (구현 ✅)

> 목적: PV/MultiPV에 종속되지 않고(또는 PV 밖으로 밀려도), 책 해설이 다루는 **장기 플랜/구조적 의도/기물 최적화**를 “아이디어 공간”으로 명시하고, 이를 `LatentPlanSeeder → (tempo injection) → (legal validation)`으로 검증 가능한 형태로 만든다.

#### 8.7.1 Seed 공통 스키마(SSOT)

모든 Seed는 아래를 반드시 갖는다(= 설계의 완결성):

- `seedId`: 안정적인 ID (`PawnStorm_Kingside` 등)
- `family`: Pawn / Piece / Structure / Prophylaxis / Exchange / TacticalPrep
- `mapsTo`: (가능하면) `Plan.*` 또는 `Motif.*` (현 코드의 구분을 그대로 재사용)
- `candidateMoves`: “생성 가능한 UCI 후보(템플릿)”
- `preconditions`: “이 플랜이 말이 되는 조건(필수) + 점수(가점)”
- `typicalCounters`: “상대의 대표 제동/반격(최소 1개)”
- `evidencePlan`: 어떤 probe purpose로 어떤 분기를 확보해야 ‘말할 수 있는가’
- `renderPolicy`: 조건부/단정 금지, 전술 오버라이드, 톤 다운 규칙

#### 8.7.2 Seed별 증거 수집 규칙(최대 품질)

- “아이디어 제시”는 내부 힌트로 가능하지만, **책 문장으로 확정하려면** 아래 중 최소 1개를 만족해야 한다:
  - `free_tempo_branches`: 합법 quiet branch 2개 이상에서 seed가 최선 응수로 등장
  - `latent_plan_refutation`: seed가 “premature/unsafe”임을 보여주는 강한 반례(why‑not) 확보
  - (구조형 seed) `recapture_branches` / `keep_tension_branches` 등 구조 분기에서 seed의 “조건”이 구체적으로 드러남

#### 8.7.3 Seed Library Data Model (Scala SSOT)

> 목표: Seed를 “생성/검증/서술”의 3단계에서 동일한 SSOT로 다루고,  
> 어떤 Seed든 **(A) 말할 수 있는 조건**, **(B) 반례/제동**, **(C) 증명 정책**, **(D) 렌더링 정책**을 빠짐없이 갖도록 강제한다.

##### 핵심 설계 포인트

- **Seed 분류는 `PlanCategory`와 분리**: `PlanCategory`는 현재 코드에서 `Attack/Positional/Structural/...`이고, “PawnPlay/Prophylaxis” 같은 분류를 직접 담기 어렵다.  
  - 권장: `SeedFamily`(아이디어 공간 분류) + `mapsToPlan`(Plan 매핑)을 별도로 둔다.
- **preconditions는 “하드/소프트(가점)”를 둘 다 지원**: 책 해설은 “가능/불가”뿐 아니라 “그럴듯함(robustness)”이 중요하다.  
  - 권장: `Precondition` + `weight` + `required` 플래그.
- **EvidencePolicy는 1개가 아니라 “정책 묶음”이어야 최고 수준**:  
  - (internal) tempo injection 힌트, (legal) free‑tempo 검증, (safety) refutation/viability를 함께 구성해야 “책 톤”이 흔들리지 않는다.
- **CounterPattern은 ‘문장 생성용’이 아니라 ‘검증/분기 설계용’**으로도 쓰인다:  
  - 즉, CounterPattern이 있으면 `ProbeDetector`가 자동으로 “제동 수 후보”를 더 잘 뽑을 수 있어야 한다.

##### 제안 모델(초안, 설계 SSOT)

```scala
// =========================
// Seed Identity & Semantics
// =========================

enum SeedFamily:
  case PawnLevers
  case PieceManeuvers
  case Structural
  case Prophylaxis
  case Exchange
  case TacticalPreparation

enum SeedSubject:
  case PawnPush(file: chess.File)                 // g-pawn push family (g4/g5)
  case SquareFocus(square: chess.Square)          // outpost/weak square focus
  case PieceRole(role: chess.Role)                // e.g., Knight reroute / rook lift
  case BreakMove(seedMove: String /* UCI */)      // e4 break etc (keep as UCI for simplicity)

case class WeightedPrecondition(
  cond: Precondition,
  required: Boolean = false,
  weight: Double = 1.0
)

case class LatentSeed(
  id: String,                         // stable: "PawnStorm_Kingside"
  family: SeedFamily,
  subject: SeedSubject,
  mapsToPlanId: Option[lila.llm.model.PlanId] = None,
  // Template moves the seeder is allowed to propose (UCI); may be filtered by legality later.
  candidateMoves: List[String],
  // (A) "말할 수 있는 상태"를 위한 조건(필수 + 가점)
  preconditions: List[WeightedPrecondition],
  // (B) 제동/반례(최소 1개 권장)
  typicalCounters: List[CounterPattern],
  // (C) 증명 방법(Probe 정책 묶음)
  evidencePolicy: EvidencePolicy,
  // (D) 렌더링 템플릿(조건부 강제)
  narrative: NarrativeTemplate
)

// =========================
// Preconditions (static-ish)
// =========================

enum Flank:
  case Kingside, Queenside, Center

enum CenterState:
  case Closed, Open, Fluid

enum FileStatus:
  case Open, SemiOpen, Closed

enum StructureType:
  case CarlsbadLike
  case IQP
  case HangingPawns
  case FianchettoShell
  case MaroczyBindLike

enum Precondition:
  case KingPosition(owner: String /* us/them */, flank: Flank)          // from kingSafety.*CastledSide / king square
  case CenterStateIs(state: CenterState)                                 // from centralSpace.lockedCenter/openCenter + pawn policy
  case SpaceAdvantage(flank: Flank, min: Int)                             // from centralSpace.spaceDiff or feature heuristics
  case FileStatusIs(file: chess.File, status: FileStatus)                 // from open/semi-open computations
  case PieceRouteExists(role: chess.Role, from: chess.Square, to: chess.Square)
  case PawnStructureIs(tpe: StructureType)
  case NoImmediateDefensiveTask(maxThreatCp: Int = 150)                   // from threatsToUs + turnsToImpact

// =========================
// Evidence (probe strategy)
// =========================

case class EvidencePolicy(
  // Internal hint only: allow tempo-injected scoring to rank seeds (never cite lines).
  useTempoInjection: Boolean = true,
  tempoInjectionDepth: Int = 20,
  // Legal proof: require >=N legal quiet branches where the seed shows up quickly as best reply.
  freeTempoVerify: Option[FreeTempoVerify] = Some(FreeTempoVerify()),
  // Safety: is seed playable immediately? (if not, enforce "premature/refuted" wording)
  immediateViability: Option[ImmediateViability] = Some(ImmediateViability()),
  // Explicit refutation check: force the system to look for the main countermeasure lines.
  refutationCheck: Option[RefutationCheck] = Some(RefutationCheck())
)

case class FreeTempoVerify(
  minBranches: Int = 2,           // must have >=2 a)/b) legal branches
  maxBranches: Int = 4,
  seedMustAppearWithinPlies: Int = 2,
  deepProbeDepth: Int = 20,
  replyMultiPv: Int = 2
)

case class ImmediateViability(
  maxMoverLossCp: Int = 50,       // seed now should not be much worse than baseline (else "premature")
  deepProbeDepth: Int = 20,
  replyMultiPv: Int = 3
)

case class RefutationCheck(
  // If non-empty, the detector must specifically look for these counter themes (patterns) and/or concrete UCI moves.
  counters: List[CounterPattern] = Nil,
  deepProbeDepth: Int = 20,
  replyMultiPv: Int = 3
)

// =========================
// Counters (for "But...")
// =========================

enum CounterPattern:
  // ...h5 stops g4-g5 (hook / blockade)
  case PawnPushBlock(file: chess.File)
  // key square controlled by a defender (e.g., ...Nf6 controls g4/e4 etc)
  case PieceControl(role: chess.Role, square: chess.Square)
  // central counterstrike (e.g., ...d5 / ...e5) to punish flank play
  case CentralStrike
  // opposite-wing counterplay (rook/file break)
  case Counterplay(flank: Flank)

// =========================
// Rendering template
// =========================

case class NarrativeTemplate(
  // Always conditional; never "will"
  // e.g., "If {them} is slow, {us} can start {seed} to ...; but {them} should consider {counter}."
  template: String,
  mustBeConditional: Boolean = true
)
```

##### 구현 연결(현재 파이프라인에 얹는 방식)

- `LatentPlanSeeder`: `PositionFeatures + IntegratedContext + Position` → `List[LatentSeedCandidate]`
- `AuthorQuestionGenerator`: seed 후보가 의미 있으면 `AuthorQuestionKind.LatentPlan` 생성(anchors에 핵심 토큰 포함)
- `ProbeDetector`:
  - `useTempoInjection=true`면 `tempo_injection_seed` 목적 probe를 추가(내부 힌트)
  - `freeTempoVerify`면 `free_tempo_branches` 목적 probe를 추가(합법 quiet branches)
  - `immediateViability/refutationCheck`면 `latent_plan_refutation`/`reply_multipv`류로 안전장치 추가
- `AuthorEvidenceBuilder`: `purpose=free_tempo_branches`를 `a)/b)` 분기 라인으로 변환(합법 SAN 검증 통과 시에만)
- `BookStyleRenderer`: Evidence가 충족될 때만 “If allowed time…” 문장을 허용(그 외는 자동 톤다운)

> 최고 품질을 위해서는 “Seed가 존재한다”가 아니라 “Seed를 말할 자격이 있다(증거/반례/조건)”를 데이터 모델 단계에서 강제해야 한다.

#### 8.7.4 Seed 카탈로그(초안: 사용자 제안 + 최고 수준 보강)

> 아래는 “우리가 반드시 다뤄야 하는 책‑레벨 아이디어”의 시작점이다.  
> 구현 시에는 `LatentPlanSeeder`가 이 카탈로그를 기반으로 후보를 생성하고, `AuthorQuestionKind.LatentPlan`을 통해 evidence를 강제한다.

---

## 1) Pawn Levers & Storms (폰 레버 및 공격)

### 1.1 Kingside Pawn Storm (킹사이드 폰 스톰)
- Seed ID: `PawnStorm_Kingside`
- Maps to: `Plan.PawnStorm(side="kingside")`, (보조) `Plan.KingsideAttack`
- Candidate moves(예):
  - `g2g4`, `h2h4`, `f2f4` (+ follow‑ups `g4g5`, `h4h5`, `f4f5`)
- Preconditions(필수):
  - 상대 킹이 킹사이드(캐슬/위치)이고, 중앙이 열려 있지 않음(locked/maintain/closed tendency)
  - 우리 킹이 비교적 안전(즉시 전술/수비 과제 우선 아님)
  - 해당 파일이 내 말로 “완전히 막혀” 있지 않음(폰 전진 가능)
- Typical counters(예):
  - 상대의 `...h5`, `...f5`, `...c5`류의 제동(상황별) + “중앙 반격”
- Evidence plan(최대):
  - `tempo_injection_seed`로 seed 후보 스코어링(내부)
  - `free_tempo_branches`로 quiet branch 2~4개에서 “g4가 최선 응수로 등장” 확인
  - `latent_plan_refutation`으로 “지금 당장 g4는 premature인지” 안전장치 확보
- Narrative(조건부):
  - “(If given time) 상대 킹 앞의 방벽을 무너뜨리고 룩/퀸 라인을 열기 위한 공격.”

### 1.2 Queenside Minority Attack (마이너리티 어택)
- Seed ID: `MinorityAttack_Queenside`
- Maps to: `Plan.MinorityAttack`
- Candidate moves(예):
  - `b2b4` → `b4b5`(또는 `a2a4`로 준비) / 상황별 `c3c4`는 보조
- Preconditions(필수, Carlsbad 유사):
  - 전형적 교환 QGD 계열 구조(예):
    - White: `a2/b2/c3/d4/e3`  
    - Black: `a7/b7/c6/d5/e6`
  - 퀸사이드에서 “2 vs 3” 형태가 성립(백이 소수로 돌격하여 약점 생성 가능)
- Typical counters(예):
  - `...a5`로 고정/반격, `...b5`로 버티기, 또는 중앙에서 `...e5` 타이밍
- Evidence plan:
  - `free_tempo_branches`(상대가 느릴 때 b4‑b5가 통하는지) + `keep_tension_branches`(중앙이 열리면 플랜이 무너지는지)
- Narrative:
  - “소수 병력으로 구조적 약점(뒤쳐진 폰/고립된 폰)을 만들기 위한 전형적 돌격.”

### 1.3 The “Can Opener” (h‑pawn Push vs Fianchetto)
- Seed ID: `CanOpener_h_Pawn`
- Maps to: `Plan.PawnStorm(side="kingside")` (특화 서브타입)
- Candidate moves(예):
  - `h2h4` → `h4h5` (+ 목표 훅: 상대 `...g6`/`...g7` 구조의 “hook”)
- Preconditions(필수):
  - 상대가 킹사이드 피앙케토(…g6+…Bg7) 또는 유사 구조
  - 상대 킹이 그 뒤에 있고, h‑pawn 전진이 즉시 전술로 벌받지 않음
- Typical counters(예):
  - `...h5`로 고정, `...f5`/`...c5`로 중앙/측면 반격
- Evidence plan:
  - `latent_plan_refutation`(h‑pawn push가 즉시 약점인지) + `free_tempo_branches`(상대가 느릴 때 hook 타격이 성립하는지)
- Narrative:
  - “피앙케토의 훅(g6/h6)을 h‑pawn으로 타격해 킹 앞을 개방하는 ‘캔 오프너’.”

### 1.4 Central Pawn Breaks (중앙 돌파)
- Seed ID(파라미터): `CentralBreak_{e4|d4|...}`
- Maps to: `Plan.CentralBreakthrough` + `Plan.PawnBreakPreparation(breakMove)`
- Candidate moves:
  - 전진: `e3e4`, `d3d4`, `c3c4` 등(포지션/색/구조에 따라)
  - 희생 포함 가능(최대 품질): “pawn sacrifice to open lines”는 별도 표기로 남김
- Preconditions:
  - 전진 칸/돌파 레버가 존재(기본 합법)
  - 기물 지원(룩/퀸/나이트) + 상대 제동 수단(…c5/…e5) 대비
- Typical counters:
  - 상대의 중앙 고정/블로케이드(…Nd5/…e5), 또는 즉시 교환으로 단순화
- Evidence plan:
  - `keep_tension_branches`로 “언제 돌파가 가능한지(상대가 무엇을 해야 막는지)”를 분기로 증명
- Narrative:
  - “중앙을 열어 기물 활동성을 폭발시키는 핵심 돌파(타이밍 게임).”

### 1.5 Space Expansion (공간 확장 / Bind)
- Seed ID(파라미터): `SpaceGain_{c4|f4|...}`
- Maps to: `Plan.SpaceAdvantage`
- Candidate moves:
  - `c2c4`, `f2f4` 등(마로치 바인드/킹사이드 확장 등)
- Preconditions:
  - 상대 기물의 전초(d5/e5 등)를 장기적으로 통제할 구조(“킥” 가능)
  - 중앙이 과도하게 열리지 않음(확장 후 약점이 즉시 터지지 않음)
- Typical counters:
  - `...c5`/`...e5` 같은 즉시 반격, 또는 약점 타격(파일 오픈)
- Evidence plan:
  - `free_tempo_branches`(상대가 느리면 bind가 굳어지는지) + `latent_plan_refutation`(확장이 즉시 약점인지)
- Narrative:
  - “공간으로 상대 기물의 활동 반경을 조이고, 장기 타겟을 만든다.”

---

## 2) Piece Maneuvers & Optimization (기물 기동 및 최적화)

### 2.1 Knight Outpost Trek (나이트 전초기지 원정)
- Seed ID: `KnightOutpost_Route`
- Maps to: `Plan.MinorPieceManeuver(target)` / `Plan.Blockade(square)`
- Candidate moves:
  - 경로 기반(예): `Nc3e2`, `Ne2g3`, `Ng3f5` 등(“route”를 생성)
- Preconditions:
  - 폰으로 쫓아낼 수 없는 약한 칸(outpost/hole) 존재
  - 그 칸으로 가는 합법 경로(route) 존재
- Typical counters:
  - 전초기지 제거용 pawn break(…e5/…c5), 또는 교환으로 해소
- Evidence plan:
  - `free_tempo_branches`(상대가 느리면 route가 완성되는지) + `latent_plan_refutation`(route 도중 전술/템포 손해가 큰지)
- Narrative:
  - “나이트를 영구 전초기지에 박아 상대 진영을 지속적으로 압박.”

### 2.2 Rook Lift (룩 리프트)
- Seed ID: `RookLift_Kingside`
- Maps to: `Plan.RookActivation` (+ 공격이면 `Plan.KingsideAttack`)
- Candidate moves:
  - `Ra1a3`, `Rh1h3`, `Rf1f3` 등(3랭크/4랭크 활용)
- Preconditions:
  - 3랭크(또는 4랭크)가 폰으로 완전히 막혀 있지 않음
  - 룩이 이동할 “안전한 중계 칸”이 존재(즉시 전술로 벌받지 않음)
- Typical counters:
  - 중앙/측면 반격으로 tempo를 빼앗거나, 룩 리프트 루트를 차단
- Evidence plan:
  - `free_tempo_branches`로 “상대가 느리면 룩이 공격 전선에 합류”를 증명
- Narrative:
  - “1랭크 교통체증을 피해 3랭크로 룩을 공격 전선에 투입.”

### 2.3 Bishop Rerouting (비숍 재배치)
- Seed ID: `BadBishop_Reroute`
- Maps to: `Plan.PieceActivation`
- Candidate moves:
  - 비숍의 탈출 루트(예): `Bc8d7e8h5`, `Bf1d3`, `Bg2f1` 등(색/구조별)
- Preconditions:
  - ‘나쁜 비숍’(자기 폰 사슬에 갇힘) + 탈출 대각선/경로 존재
- Typical counters:
  - 루트를 차단하는 pawn push, 교환 유도
- Evidence plan:
  - `free_tempo_branches`(상대가 느리면 reroute 완성) + `latent_plan_refutation`(중간 수가 즉시 손해인지)
- Narrative:
  - “수동적 비숍을 활성 대각선으로 재배치해 역할을 바꾼다.”

### 2.4 Battery Formation (배터리 형성)
- Seed ID: `Battery_{QB|RR|QR}`
- Maps to: `Plan.FileControl` / `Plan.KingsideAttack` (문맥에 따라)
- Candidate moves:
  - `Qd1c2` + `Bc1d2`처럼 “겹치기”, 또는 rook doubling(파일/랭크)
- Preconditions:
  - 장악 가능한 파일/대각선 + 두 기물이 중첩될 공간/템포
- Typical counters:
  - 파일 contest(…Rc8), 교환(…Qd6), 또는 반대편에서 counterplay
- Evidence plan:
  - `reply_multipv`(상대 자원 분기) + `free_tempo_branches`(시간 주면 완성)
- Narrative:
  - “화력을 한 점에 집중해 돌파력을 극대화.”

---

## 3) Structural Transformation (구조적 변환)

### 3.1 Creating a Passed Pawn (통과한 폰 만들기)
- Seed ID: `CreatePassedPawn`
- Maps to: `Plan.PassedPawnCreation`
- Candidate moves:
  - 다수 지역에서 pawn trades를 유도하는 레버(파일/구조별)
- Preconditions:
  - majority(3v2 등) + 교환으로 통과폰이 남는 산술 우위
- Typical counters:
  - 교환 회피, 블로케이드 준비, 또는 counter‑majority 생성
- Evidence plan:
  - `keep_tension_branches`(교환/구조가 언제 고정되는지) + `conversion_reply_multipv`(방어 자원)
- Narrative:
  - “엔드게임의 결정적 무기인 통과폰을 만들기 위한 구조적 설계.”

### 3.2 Isolating Opponent’s Pawn (IQP/고립폰 만들기)
- Seed ID: `CreateIQP`
- Maps to: `Plan.WeakPawnAttack(pawnType="isolated")` (또는 전용 plan 확장)
- Candidate moves:
  - 인접 폰을 교환 강제하는 레버(구조별)
- Preconditions:
  - 상대 폰 구조가 유동적 + 강제 교환 레버 존재
- Typical counters:
  - 구조를 고정하거나(…c6), 조기 교환으로 약점 생성 회피
- Evidence plan:
  - `recapture_branches`(구조 선택) + `keep_tension_branches`(언제 고립이 생기는지)
- Narrative:
  - “상대의 한 폰을 영구 약점으로 만들고, 봉쇄/압박한다.”

### 3.3 Fixing Weaknesses (약점 고정: backward pawn 등)
- Seed ID: `FixBackwardPawn`
- Maps to: `Plan.WeakPawnAttack(pawnType="backward")` + `Plan.Blockade(square)`
- Candidate moves:
  - 전진 억제(블로케이드) 수 + 파일 장악
- Preconditions:
  - 상대의 뒤쳐진 폰이 “전진 불가” 상태로 고정 가능
- Typical counters:
  - break로 해소(…e5) 또는 교환으로 약점 제거
- Evidence plan:
  - `free_tempo_branches`(상대가 느리면 고정이 완성되는지) + `keep_tension_branches`
- Narrative:
  - “약점이 해소되지 않도록 고정시키고 장기 타겟으로 삼는다.”

---

## 4) Prophylaxis & Safety (예방 및 킹 안전)

### 4.1 Creating Luft (숨구멍 만들기)
- Seed ID: `Prophylaxis_Luft`
- Maps to: `Plan.Prophylaxis(againstWhat="back rank")`
- Candidate moves:
  - `h2h3`, `g2g3`(또는 흑의 `...h6`/`...g6`) 등
- Preconditions:
  - 잠재적 백랭크 약점(escape squares 부족, heavy piece 침투 가능)
- Typical counters:
  - 상대가 즉시 침투하는 경우는 “전술/수비 과제”가 오버라이드
- Evidence plan:
  - `defense_reply_multipv`(상대 침투 자원 분기) + 최소한의 안전성 검증
- Narrative:
  - “백랭크 사고를 예방하기 위한 숨구멍 확보.”

### 4.2 Restricting Opponent’s Piece (상대 기물 봉쇄/지배)
- Seed ID: `Restrict_{Knight|Bishop|...}`
- Maps to: `Plan.Prophylaxis(againstWhat)` / `Plan.Blockade(square)`
- Candidate moves:
  - `a2a3`로 …Nb4 방지 같은 “예방 폰무브”, 또는 key square 장악
- Preconditions:
  - 상대 기물의 이상적 진출로가 명확 + 미리 막을 수 있음
- Typical counters:
  - 반대편에서 counterplay, 또는 즉시 전술로 punishment
- Evidence plan:
  - `free_tempo_branches`(상대가 느리면 봉쇄가 완성) + `latent_plan_refutation`
- Narrative:
  - “상대 주요 기물의 진출로를 원천 봉쇄해 활동성을 마비시킨다.”

### 4.3 King Evacuation (킹 대피)
- Seed ID: `KingSafety_Run`
- Maps to: `Plan.DefensiveConsolidation`
- Candidate moves:
  - `Ke1f1`, `Kg1h2` 등 “위험 지역 회피” (상황별)
- Preconditions:
  - 곧 열릴 파일/대각선 위에 킹이 놓여 있고, 이동할 시간(tempo)이 존재
- Typical counters:
  - 상대의 forcing line(체크 연속)으로 “대피 불가”가 되면 전술 오버라이드
- Evidence plan:
  - `defense_reply_multipv`(forcing 자원) + `free_tempo_branches`(시간이 있을 때만)
- Narrative:
  - “전쟁터가 될 구역을 벗어나 킹을 안전지대로 선제 대피.”

---

## 5) Exchange Decisions (교환 전략)

### 5.1 Trading “Good” for “Bad” (좋은 기물 남기기)
- Seed ID: `Trade_BadBishop`
- Maps to: `Plan.Exchange(purpose="improve minor pieces")`
- Candidate moves:
  - 교환 각을 만드는 단순 교환 수(구조/기물별)
- Preconditions:
  - 내 bad bishop vs 상대 good piece 교환이 “구조적으로” 이득
- Typical counters:
  - 상대가 교환을 피하고 다른 전초/파일로 counterplay
- Evidence plan:
  - `reply_multipv`(상대가 피할 때/받을 때 분기) + `conversion_reply_multipv`(유리한 쪽이면)
- Narrative:
  - “구조적으로 나쁜 기물을 처분하고 장기 우위를 단순화한다.”

### 5.2 Simplification into Won Endgame (이긴 엔드게임으로 단순화)
- Seed ID: `Simplify_To_Endgame`
- Maps to: `Plan.Simplification`
- Candidate moves:
  - 퀸/룩/미들피스 교환 유도 수
- Preconditions:
  - 물량/구조 우세 + 교환 시 상대 역전 변수 감소
- Typical counters:
  - 상대의 perpetual/active counterplay가 남아 있으면 단순화는 위험(전술 오버라이드)
- Evidence plan:
  - `convert_reply_multipv`(방어 자원) + `latent_plan_refutation`(교환이 오히려 역전 허용인지)
- Narrative:
  - “변수를 제거하고 확실한 승리 엔드게임으로 전환.”

---

## 6) Dynamic & Tactical Preparation (전술 준비: ‘즉시’가 아니라 ‘포석’)

### 6.1 Overloading Preparation (과부하 유도 준비)
- Seed ID: `Prepare_Overload`
- Maps to: (우선) `Plan.KingsideAttack` 또는 `Plan.Counterplay(where)` + (증거는 tactic‑style)
- Candidate moves:
  - 한 수비 기물에 압박을 누적시키는 개선 수(파일/대각선/배터리)
- Preconditions:
  - 상대 수비 기물이 “다중 방어” 중이며, 압박을 단계적으로 추가 가능
- Typical counters:
  - 수비 기물 교환/해방, 또는 counter‑threat로 tempo 전환
- Evidence plan:
  - `reply_multipv`(상대 자원 분기) + 필요 시 `aggressive_why_not_*`(전술 시도 vs 안정)
- Narrative:
  - “수비 라인을 한계까지 과부하 시켜 붕괴를 유도.”

### 6.2 Clearance Sacrifice Prep (공간 확보 희생 준비)
- Seed ID: `Prepare_Clearance`
- Maps to: `Plan.Sacrifice(piece)` (또는 `Plan.CentralBreakthrough`의 포석)
- Candidate moves:
  - 길을 막는 말/폰을 치우기 위한 희생/전개(전술 연결)
- Preconditions:
  - ‘막힌 문’(key square/file/diagonal)이 존재하고, 제거하면 결정적 침투가 가능
- Typical counters:
  - 상대가 미리 차단/교환, 또는 희생이 정당화되지 않는 경우(=refuted)
- Evidence plan:
  - `latent_plan_refutation`(희생의 건전성) + `reply_multipv`(상대의 최강 대응 분기)
- Narrative:
  - “결정적인 타격을 가할 길을 열기 위해 장애물을 치울 준비.”

---

## 9) 기존 파이프라인과의 통합(버리는 것 vs 얹는 것)

### 9.1 버릴 필요 없음(재사용)
- `CommentaryEngine.assessExtended`: 보드 인식/피처/태그/후보 생성에 계속 사용
- `NarrativeContextBuilder`: 테이블/후보/프로브 요청 등 구조적 데이터는 계속 생산
- `BookStyleRenderer`: 기존 렌더 섹션을 유지하되, Outline 기반 섹션을 추가/교체
- `ProbeDetector`: 이미 존재하는 probe loop를 EvidencePlan 중심으로 확장

### 9.2 새로 추가해야 함(주도 레이어)
- `AuthorQuestions` 생성기
- `EvidencePlan` 오케스트레이터
- `Outline` 생성기 + Post‑Critic

### 9.3 ASCII 비교(핵심: 구심점 이동)

**현재(구심점=PV/후보)**
```
PV/MultiPV  -> candidates(a/b/c) -> prose
     \-> probe(보강) -> a1/a2
```

**목표(구심점=저자 질문/결정지점)**
```
Perception -> AuthorQuestions -> EvidencePlan -> VerifiedEvidence -> Outline -> prose
                        \-> engine/probe/DB는 증거 생산기로만 사용
```

---

## 10) 4개 코퍼스 케이스별 “책 관점 플레이북”(매우 구체)

> 아래는 “이 케이스에서 반드시 이렇게 써야 책처럼 된다”에 대한 구체 지침이다.  
> 구현 시 AuthorQuestions/EvidencePlan/Outline의 정답 예시로 사용한다.

### 10.1 Philidor `4...h6?` (Legal’s mate motif)

#### Perception 핵심
- 오프닝 초반, 흑이 개발을 미루고 가장자리 폰을 전진(느린 수)
- 테마: `5.Nxe5!` 전술 타이밍 + `...Bxd1??` 금지(레갈 메이트)

#### AuthorQuestions(우선순위)
1) `TacticalMine`: “왜 4...h6?가 전술적으로 위험한가?”  
   - mustMention: `5.Nxe5!`, `...Bxd1??`, `Bxf7+`, `Nd5#`
2) `DecisionPoint`: “흑은 무엇을 놓쳤나(개발/킹 안전)?”  
   - mustMention: `...Nf6`, `...Nc6`

#### EvidencePlan(필수 분기)
- Task A: played move `h7h6` probe(이미 있으면 생략)  
  - 목적: “느린 폰무브의 대가”를 수치/분기로 확인
- Task B: `5.Nxe5!` 이후 분기 2개 확보
  - B1(함정): `...Bxd1??` 강제 라인(메이트/큰 손실 확인)
  - B2(정답): `...dxe5` 이후 `Qxg4`로 단순화(“편한 우위”의 근거)

#### Outline(문단 구조)
1) Context: “느린 폰무브 + 개발 지연” 한 문장
2) Q1(TacticalMine): 레갈 메이트를 **2문장 내**에 못 박기(핵심 라인 포함)
3) Q2(DecisionPoint): 정답 대안 `...Nf6`/`...Nc6`의 ‘왜’(개발/트랩 회피)
4) Teaching point: “퀸 먹는 욕심은 금물(클래식 함정)” + “캡처 후 recapture 확인”

#### 금지 사항(회귀 방지)
- `Qxg7` 같은 즉시 잡히는 수를 “편한 게임”으로 서술 금지
- “bad bishop long-term” 같은 시기 부적절 단정 금지(오프닝 초반)

---

### 10.2 QID `9.cxd5` (Early tension release)

#### Perception 핵심
- 핵심은 `c4–d5` 긴장(maintain vs release)
- `cxd5`는 “합법/자연”이지만, 구조를 단순화해 흑의 equalize를 돕는 성격이 강함

#### AuthorQuestions(우선순위)
1) `TensionDecision`: “왜 지금 긴장을 풀면(9.cxd5) 흑이 편해질 수 있는가?”  
   - mustMention: recapture choices, `...O-O/...Nbd7/...c5` 자연스러움
2) `PlanClash`: “긴장을 유지하면(예: 9.O-O) 양측 선택지가 어떻게 열리나?”  
   - mustMention: `...dxc4` vs `...c5` 가능성

#### EvidencePlan(필수 분기: recapture branching)
- Task A: `9.cxd5` probe + reply MultiPV 3
  - 목적: 흑의 대표 recapture를 최소 2개 확보
- Task B: recapture 후보 강제 분기(가능하면 법적 수 생성 기반)
  - `...exd5`, `...Nxd5`, `...cxd5` 중 가능한 것 2개 이상
  - 각 분기에서 흑의 자연스러운 플랜(…O‑O/…Nbd7/…c5) 1라인 확보

#### Outline
1) Context: “긴장이 핵심인 구조” 한 문장
2) Q1: `cxd5`는 정리지만 흑에게 편한 루트(분기 2개로 증명)
3) Q2: `O‑O` 같은 유지 전략의 의미(흑의 선택지와 연결해서 설명)
4) Teaching point: “긴장 유지의 목적은 ‘기다리기’가 아니라 ‘상대의 커밋을 강제’하는 것”

---

### 10.3 Startpos `1.e4` (Sanity check)

#### Perception 핵심
- 1수째는 평가보다 “게임 성격의 분기”가 핵심 가치
- 과장된 practical claim 금지(“Black has fewer forgiving choices” 근거 없음)

#### AuthorQuestions(우선순위)
1) `OpeningTheory`: “1.e4가 무엇을 열어주나(오픈/세미오픈/클로즈드)?”
2) `Pedagogy`: “초반에는 ‘균형’ 반복보다 구조/성격 분기가 유용하다”

#### EvidencePlan
- 엔진 라인 고정 인용 최소화(특정 방어 한 줄을 ‘main point’로 단정 금지)
- 흑의 대표 응수(…e5/…c5/…e6/…c6) 1문장씩 “성격”만 설명(라인 덤프 금지)

#### Outline
1) Context: “중앙 주장 + 라인 열림” (f1 비숍/퀸)
2) Q1: 흑 응수에 따른 성격 분기(오픈/비대칭/견고)
3) Teaching point: “초반 평가는 0.00이라도 ‘성격 선택’이 핵심”

---

### 10.4 Ruy Exchange `7.c3` (prepare d4)

#### Perception 핵심
- `c3`는 `d4` 선언 성격 + Nb1 전개 경로 제한(`Nc3` 불가)이라는 실전적 제약
- 구조: 흑의 `c6–c7` 이중폰(장기 타겟) vs 비숍 페어(동적 보상)

#### AuthorQuestions(우선순위)
1) `DecisionPoint`: “왜 7.c3가 중요한가(중앙 브레이크 d4)?”
2) `PlanClash`: “백은 구조적 우위로 압박, 흑은 비숍 페어/중앙 반격”

#### EvidencePlan
- Task A: 7.c3 이후 대표 분기 2개
  - `...Bd6` vs `...exd4` (가능하면 둘 다 확보)
- Task B: 불법 수순 검증(특히 `c3` 이후 `Nc3` 같은 오류 방지)
- Masters 인용은 검증 통과 시에만(숫자/실명/연도는 데이터 기반일 때만)

#### Outline
1) Context: 교환 스페니시 구조의 핵심 한 문장(이중폰 vs 비숍 페어)
2) Q1: c3‑d4 계획 + “대신 Nb1은 보통 Nd2” (구체)
3) Q2: 흑의 반격 플랜(비숍 페어/…f5/…c5 등)과 백의 압박 플랜(c6/c7 타겟)
4) Teaching point: “대안 수(Nbd2/h3/c3)는 ‘중앙 장악’으로 뭉개지 말고 차이를 설명”

---

## 11) 구현 로드맵(최대 노력 버전)

> “최대”는 한 번에 다 넣으면 리스크가 크다. 하지만 예산(시간/엔진)을 충분히 준다면, 기능은 크게 나누어도 된다.  
> 아래는 “품질 최대화”를 목표로 하는 단계적 구현이다(각 단계가 독립적으로 가치가 있어야 함).

### Phase 1 — AuthorQuestions 도입(구심점 생성)
- 새로운 모델/필드 추가(예: `NarrativeContext.meta` 확장 또는 새 섹션)
- 질문 2~3개 생성 + 우선순위 스코어링
- 렌더: 기존 prose에 “결정지점 문단”을 1개 추가(최소한의 변화로 효과 확인)

### Phase 2 — EvidencePlan + Orchestrator(분기 설계)
- 질문별 evidenceKinds 설계
- probeRequests에 `questionId/kind` 같은 메타를 붙여 “왜 이 probe인가”를 추적 가능하게
- recapture branching을 위한 “법적 수 생성 기반” 분기 확보
- (구현 ✅) `ProbeRequest/ProbeResult`에 `purpose/questionId/questionKind/multiPv/fen` 메타 추가 + QID용 recapture probe 생성/렌더링
  - 서버: `modules/llm/src/main/scala/lila/llm/analysis/ProbeDetector.scala`, `modules/llm/src/main/scala/lila/llm/analysis/AuthorEvidenceBuilder.scala`, `modules/llm/src/main/scala/lila/llm/analysis/MovePredicates.scala`
  - 모델: `modules/llm/src/main/scala/lila/llm/model/ProbeModel.scala`, `modules/llm/src/main/scala/lila/llm/model/authoring/AuthoringModel.scala`
  - 렌더: `modules/llm/src/main/scala/lila/llm/analysis/BookStyleRenderer.scala`
  - 클라: `ui/analyse/src/bookmaker.ts`

### Phase 2.5 — SeedMoveGenerator & Content Expansion (구현 ✅)
- (구현 ✅) `SeedMoveGenerator` 도입: 동적 후보 생성 로직을 `LatentPlanSeeder`에서 분리하여 전용 모듈화.
- (구현 ✅) Latent Plan 전 씨드 활성화:
    - **Structural**: `CreatePassedPawn`, `FixBackwardPawn`, `CreateIQP`
    - **Prophylaxis**: `Prophylaxis_Luft`, `KingSafety_Run`, `Restrict_OpponentPiece`
    - **Tactical Prep**: `Prepare_Overload`, `Prepare_Clearance`, `BadBishop_Reroute`
- (구현 ✅) 검증: Unit Test 및 Corpus Runner를 통한 품질 및 안정성 확인.

### Phase 3 — Truth Gate 강화(테마/방향/DB)
- (구현 ✅) `TruthGate` 도입: 테마/트랩을 **엔진 없이도**(법적 수 + 체크메이트) 검증 가능한 경우, 반드시 서술에 반영
  - 파일: `modules/llm/src/main/scala/lila/llm/analysis/TruthGate.scala`
  - 현재 포함: Philidor `4...h6?`의 **Legal’s mate** 라인(엄격 적용 + 최종 체크메이트 확인)
  - 연결: `AuthorQuestionGenerator`(TacticalTest 질문 생성) + `BookStyleRenderer`(annotated move에 트랩 라인 강제 삽입)
- (구현 ✅) Recapture sanity 강화: “캡처로 끝나는 인용 라인(체크/메이트 제외)”을 금지/다운그레이드
  - PV 샘플 기반 인용은 제거되었고, 근거 기반 문장/분기 설명으로 대체됨(R+3)
  - PV 슬라이스: `modules/llm/src/main/scala/lila/llm/analysis/BookStyleRenderer.scala` (`bookSanSlice`)
  - Probe 인용: `modules/llm/src/main/scala/lila/llm/analysis/AuthorEvidenceBuilder.scala` (PV tail 확장 + unsafe capture tail 제거)
- (구현 ✅) 방향(Orientation) 게이트: “긴장 유지가 정답인데 캡처로 풀어버린” 케이스에서 실전 결론이 뒤집히지 않도록 보정
  - `modules/llm/src/main/scala/lila/llm/analysis/BookStyleRenderer.scala` (`renderPracticalWrapUp`)
  - 조건: `tensionPolicy=Maintain` + `playedSan`이 캡처 + `counterfactual` 존재(=미스) → “상대가 단순화/자연 전개로 편해짐” 방향으로 override
- DB 인용 strict(유지): 숫자/실명/연도는 `openingRef` 기반일 때만, PGN snippet은 FEN에서 SAN→UCI로 검증 통과 시에만 직접 인용
  - `modules/llm/src/main/scala/lila/llm/analysis/BookStyleRenderer.scala` (`renderMastersReferences`)

### Phase 4 — Post‑Critic(문장 편집기)
- 반복/상투어/허세 문장 제거
- “근거 없는 단정”을 근거가 있을 때만 유지(없으면 downgrade)
- (구현 ✅) 렌더 후 편집 레이어 `PostCritic` 추가(문장/문단 구조는 유지, 문자열 레벨에서 보수적으로 편집)
  - 파일: `modules/llm/src/main/scala/lila/llm/analysis/PostCritic.scala`
  - 적용 지점: `modules/llm/src/main/scala/lila/llm/analysis/BookStyleRenderer.scala` (`render`에서 최종 prose에 적용)
  - 현재 룰(요약):
    - “The game begins.” 제거/치환(국면+ply 기반: opening critical moment 등)
    - “tightening the screw / full control” 등 허세 플레버 제거/완화
    - “roughly balanced / small edge” 같은 저밀도 문장 반복 제한
    - 문단 경계(`\n\n`) 보존(리스트/대안 섹션 포맷 깨지지 않게)
  - 소스 레벨 병행 정리:
    - `NarrativeLexicon.getOpening`에서 “The game begins.” 템플릿 제거
    - `NarrativeLexicon.getAnalyticalFlavour`에서 상투/허세 문구 제거 및 대체
    - `renderPlanSummary` 문장 구조 정리(“pursue the pinning…” 류 문법 오류 제거)

### Phase 5 — Narrative Outline SSOT화(문단/분기/근거를 “강제”)

> 목표: 렌더러가 “즉흥적으로 말할 것”을 결정하지 않게 하고,  
> **Outline(문단 설계) 자체가 SSOT**가 되어 “책 해설의 분기/근거/교훈 밀도”를 구조적으로 강제한다.

#### 5.0 현재 구현 상태(2026‑01‑20 기준)
- (구현 ✅) Outline 모델(스키마) 도입: `NarrativeOutline.scala`
- (구현 ✅) OutlineBuilder/Validator 모듈화 시작: `NarrativeOutlineBuilder.scala`, `NarrativeOutlineValidator.scala` 생성.
- (구현 ✅) `BookStyleRenderer`가 내부적으로 Outline을 구성한 뒤 prose를 출력하도록 전환.
- 한계: 현재 Builder가 여전히 Renderer의 로직에 의존적(Extraction 단계)이며, Validator의 "강제 규칙"이 기초적인 수준임.

#### 5.1 To‑Be 구조(SSOT 분리 후)

```
CommentaryEngine.assessExtended
  -> NarrativeContextBuilder.build
     - AuthorQuestions (SSOT: 무엇을 말할지)
     - ProbeRequests (SSOT: 어떤 증거가 필요한지)
     - AuthorEvidence (probeResults가 있을 때만)
     - NarrativeOutline (SSOT: 어떤 순서/문단/분기/교훈으로 말할지)  <-- NEW
  -> OutlineRenderer (SSOT: 어떻게 문장화할지)
  -> PostCritic (문장 편집/검열)
```

핵심 설계 원칙:
- **OutlineBuilder는 “내용 선택/순서/근거 배치”만** 담당하고, 문장 다양화는 Renderer/Lexicon이 담당.
- **Validator는 fail‑closed**: 증거가 부족하면 “허세 문장”이 아니라 “톤 다운/보류/추가 probe 요청”으로 수렴.

#### 5.2 모듈 분리(권장 파일/역할) — 반드시 SSOT를 명확히
- Outline 모델(이미 존재 ✅):
  - `modules/llm/src/main/scala/lila/llm/model/authoring/NarrativeOutline.scala`
- OutlineBuilder(SSOT: beat 생성 + 근거 매핑 + 교훈 선택):
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineBuilder.scala`
  - 입력: `NarrativeContext`(또는 `AuthorQuestion + QuestionEvidence + 핵심 perception`)  
  - 출력: `NarrativeOutline` + `OutlineDiagnostics`(선택한 질문/사용한 evidence purpose/부족한 증거 등)
- OutlineValidator(SSOT: 강제 규칙):
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineValidator.scala`
  - 규칙(예):
    - `TacticalTest`가 있으면 2문장 내에 테마명(예: `Legal’s mate`) 등장 필수
    - `StructuralCommitment`면 Evidence beat에 분기 2개 이상(최소 a/b) 또는 “missing evidence”로 downgrade
    - `TensionDecision/PlanClash`면 `keep_tension_branches` 또는 `recapture_branches` 중 하나 이상 확보(없으면 주장 톤 다운)
    - `mustMention` 미충족 시: (1) 해당 토큰이 들어간 대체 문장 선택 → (2) beat 제거/톤 다운 → (3) probe 요구
- OutlineRenderer(문장화 전용):
  - 초기에는 `BookStyleRenderer` 내부에 두고, 안정화 후 분리:
    - `modules/llm/src/main/scala/lila/llm/analysis/OutlineRenderer.scala`
  - 입력: `NarrativeOutline` + `NarrativeContext`
  - 출력: prose (beat별 문장 템플릿 + lexicon variant 선택)

#### 5.3 “최대 노력”을 위한 Evidence 요구(=Probe 예산을 Outline이 지배)

핵심: “probe는 PV 보강”이 아니라 **Outline이 요구하는 분기(증명)**를 채우기 위한 수단이어야 한다.

- 질문 kind → 최소 Evidence purpose 매핑(권장):
  - `TacticalTest` → (TruthGate line) 또는 `reply_multipv` 2가지 방어/반격 라인
  - `TensionDecision` → `keep_tension_branches`(최우선) + 부족 시 `reply_multipv`
  - `PlanClash` → `keep_tension_branches`(필수)
  - `StructuralCommitment` → `recapture_branches`(필수)
  - `DefensiveTask` → `defense_reply_multipv`(필수)
  - `ConversionPlan` → `convert_reply_multipv`(필수)

이 매핑을 **코드에서 한 곳(SSOT)**으로 고정해야 한다:
- 현재는 `ProbeDetector`가 일부 purpose를 생성하고 있음 → 다음 단계에서 `EvidencePlanner`로 분리하여
  - “ghost plan probes”와 “evidence probes”를 명확히 구분하고
  - OutlineValidator가 “필요 증거가 부족함”을 신호하면 ProbePlanner가 즉시 보강하도록 계약을 만든다.

#### 5.4 모듈 분리 작업 순서(체크리스트, 권장)
1) (분리) `BookStyleRenderer.buildOutline` 로직을 `NarrativeOutlineBuilder`로 이동(동작 동일 유지)
2) (연결) `NarrativeContext`에 `outline: Option[NarrativeOutline]` 필드 추가(내부용, API 노출은 optional)
3) (이관) `BookStyleRenderer`는 `ctx.outline.getOrElse(NarrativeOutlineBuilder.build(ctx))`만 호출하도록 축소
4) (검증) `NarrativeOutlineValidator` 도입: mustMention/분기 수/테마 히트 등 “하드 게이트”를 구조적으로 처리
5) (증거 계획) `EvidencePlanner` 도입(또는 `ProbeDetector` 내부 분리):
   - 입력: `AuthorQuestion` + `OutlineRequirements`
   - 출력: `ProbeRequest`(purpose/questionId 기반) + “missing evidence reason”
6) (테스트) 코퍼스 runner가 prose뿐 아니라 Outline 구조(beat 존재/분기 수)를 검사할 수 있게 확장

> 이 순서를 지키면 “문장만 바꿔서 그럴듯하게”가 아니라,  
> **책 해설이 요구하는 구조적 DoD를 강제**하면서 리팩터링을 진행할 수 있다.

### Phase 6 — 코퍼스/평가 하네스 확장 (완료 ✅)
- `BookCommentaryCorpus.json`을 40개 케이스로 확장 (Opening, Tactics, Strategy, Endgame)
- 40/40 전 케이스 Pass 및 품질 지표(밀도) 검증 완료.

### Phase 6.8 — Pipeline Density Injection (완료 ✅)
- **데이터 통합**: `PawnPlay`, `Prophylaxis`, `Compensation`, `Practicality` 데이터를 `NarrativeOutlineBuilder`에 통합.
- **Logic Reconstruction**: `LogicReconstructor`를 구현하여 Greedy/Lazy/Phantom 수 감지 엔진 도입.
- **문장 체이닝**: `BookStyleRenderer`에서 인접한 비트(MainMove + PsychologicalVerdict)를 단일 문단으로 결합하여 가독성 향상.

### Phase 8 — Imagination Engine (Latent Plans & Idea Space) (진행 예정)
- **Seed Library**: 구조적 트리거를 가진 `LatentSeedLibrary` 구현.
- **Tempo Injection**: "Quiet Move" 프로브 생성 및 검증 로직.
- **조건부 내러티브**: "If given time..." 스타일의 상상력 비트 활성화.

### 구현 상세(권장 파일/타입, “누가 봐도 바로 찾게”)

> 아래는 “최대 품질” 레이어를 실제 코드로 옮길 때의 권장 배치다.  
> (이름은 제안이며, 핵심은 **SSOT 위치를 명확히** 하는 것.)

- 모델(SSOT):
  - `modules/llm/src/main/scala/lila/llm/model/authoring/AuthoringModel.scala`
    - `AuthorQuestion`, `AuthorQuestionKind`, `QuestionEvidence`, `EvidenceBranch`
  - `modules/llm/src/main/scala/lila/llm/model/authoring/NarrativeOutline.scala`
    - `NarrativeOutline`, `OutlineBeat`, `OutlineBeatKind`
- 질문 생성기:
  - `modules/llm/src/main/scala/lila/llm/analysis/AuthorQuestionGenerator.scala`
    - 입력: `ExtendedAnalysisData` + `IntegratedContext`
    - 출력: `List[AuthorQuestion]` (top 2~3)
- Probe 계획/오케스트레이터(SSOT):
  - `modules/llm/src/main/scala/lila/llm/analysis/ProbeDetector.scala`
    - 입력: `IntegratedContext` + `PlanScoringResult` + MultiPV + `AuthorQuestion` + candidates
    - 출력: `List[ProbeRequest]` (purpose/questionId/questionKind/multiPv/fen 포함)
- 검증 게이트:
  - `modules/llm/src/main/scala/lila/llm/analysis/EvidenceValidator.scala`
    - 입력: (PV/Probe/DB snippet) + FEN
    - 출력: `VerifiedEvidence` (인용 가능 여부/다운그레이드 사유 포함)
- 아웃라인 생성기:
  - `modules/llm/src/main/scala/lila/llm/analysis/NarrativeOutlineBuilder.scala`
    - 입력: `AuthorQuestion` + `VerifiedEvidence` + Perception snapshot
    - 출력: `NarrativeOutline` (문단/근거/교훈 매핑)
- 후처리(문장 편집기):
  - `modules/llm/src/main/scala/lila/llm/analysis/PostCritic.scala`
    - 중복/상투어/허세/근거없는 단정 자동 제거

통합 포인트(최소 변경):
- `NarrativeContext`에 새 섹션을 추가하거나(`authoring: Option[AuthoringSection]`), 우선은 `meta`에 포함시켜 점진 확장.
- `NarrativeContextBuilder.build`에서:
  1) `AuthorQuestionGenerator` 호출
  2) `ProbeDetector`에서 `probeRequests` 생성(ghost plan + evidence plan을 한 곳에서 합성)
  3) `probeResults` 수신 시 `EvidenceValidator` → `OutlineBuilder` → 렌더로 전달
- `BookStyleRenderer`는:
  - 기존 후보(a/b/c) 섹션을 유지하되, `Outline`이 있으면 “결정지점 문단”을 우선 렌더(또는 기존 섹션을 재정렬)

---

## 12) 품질 지표/검증(Definition of Done)

### 12.1 하드 게이트(0이어야 함)
- illegal SAN line rate = 0
- “캡처로 끝나는 인용 라인(체크/메이트 제외)” 비율 = 0
- 주체 뒤집힘 문장(“내 폰이 약점임을 증명”) = 0

### 12.2 소프트 지표(증가/감소 목표)
- One‑line worldview rate(분기 없는 주장) ↓
- Claim‑Evidence alignment score ↑ (주장마다 최소 1개 검증된 근거)
- Theme hit‑rate ↑ (테마 포지션에서 핵심 테마 2문장 내 등장)
- Redundancy score ↓ (상투어/반복)

### 12.3 실행 커맨드(현 상태)
- 테스트: `sbt llm/test`
- 코퍼스 리포트: `sbt "llm/runMain lila.llm.tools.BookCommentaryCorpusRunner"`
- 라이브 스모크: `sbt "llm/runMain lila.llm.tools.BookmakerLiveSmokeTest"`

---

## 13) 코드 맵(현재 구현 기준, 빠르게 찾아보기)

### 서버/분석
- Stage 1/2 API: `app/controllers/LlmController.scala`
- Stage 1/2 API facade: `modules/llm/src/main/LlmApi.scala`
- Deep analysis: `modules/llm/src/main/scala/lila/llm/analysis/CommentaryEngine.scala` (`assessExtended`)
- Context build: `modules/llm/src/main/scala/lila/llm/analysis/NarrativeContextBuilder.scala`
- Renderer: `modules/llm/src/main/scala/lila/llm/analysis/BookStyleRenderer.scala`
- Probes: `modules/llm/src/main/scala/lila/llm/analysis/ProbeDetector.scala`, `modules/llm/src/main/scala/lila/llm/model/ProbeModel.scala`
- Opening explorer: `modules/llm/src/main/scala/lila/llm/analysis/OpeningExplorerClient.scala`
- Opening events: `modules/llm/src/main/scala/lila/llm/analysis/OpeningEventDetector.scala`
- Utilities: `modules/llm/src/main/scala/lila/llm/analysis/NarrativeUtils.scala`

### 프론트
- Bookmaker orchestration: `ui/analyse/src/bookmaker.ts`
- UI renderer: `modules/analyse/src/main/ui/BookmakerRenderer.scala`

---

## 14) 이 문서가 “완전해야” 하는 이유(운영/협업 관점)

이 프로젝트는 “그럴듯한 텍스트 생성”이 아니라 **체스 내용의 신뢰**가 핵심이며,
- 한 번 신뢰가 깨지면(불법 라인/환각 인용) 사용자는 다시 돌아오지 않는다.
- 반대로, 지연시간이 늘어도 “책 수준”이면 사용자는 기다린다(요구사항).

따라서 개발은 “템플릿 문장 추가”가 아니라,
1) 저자 질문을 고르고  
2) 그 질문을 증명할 최소 분기를 확보하고  
3) 검증 통과한 증거만으로  
4) 교훈 중심으로 수렴  
하는 구조로 진행되어야 한다.

---

## Appendix A — “최대 품질”을 위한 추가 아이디어(선택)

- Position‑type별 전용 질문 템플릿(오프닝/중반/엔드)
- “독자 수준” 프리셋(초보/중급/상급): 질문 수/분기 수/라인 길이 조절
- 장기적으로는 오프라인 분석에서 “한 게임을 책 챕터처럼” 만드는 편집 레이어(챕터 아웃라인, 반복 제거, 테마 유지)
