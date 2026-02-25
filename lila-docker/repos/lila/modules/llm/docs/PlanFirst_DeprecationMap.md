# Plan-First Deprecation Map (PV 중심 모듈 폐기 지도)

목표: `PV 우선 해설`에서 `Plan-first 제안 + Evidence 판정`으로 전환하면서, 기존 PV 중심 모듈을 언제/어떻게 강등/대체/삭제할지 운영 기준을 고정한다.

## 1-Page Map

| Module / Path | 현재 성격 (PV 의존도) | 분류 | 조치 시점 | 종료(삭제/강등) 게이트 |
|---|---|---|---|---|
| `analysis/PlanProposalEngine.scala` | 구조 피처 + seed 기반 plan-first 제안 | **Keep** | 즉시~상시 | plan-first 후보 `always-on`, 회귀 없음 |
| `analysis/PlanEvidenceEvaluator.scala` | request-aware validator + evidence partition | **Keep** | 즉시~상시 | fail-closed 계약 유지, main/latent 분리 안정 |
| `analysis/PositionAnalyzer.scala` / `StrategicFeatureExtractorImpl.scala` | 정적 전략 상태 추출 (PV 비의존) | **Keep** | 즉시~상시 | entrenched/rook-pawn/hook/clamp 오탐률 관리 |
| `analysis/PlanStateTracker.scala` (v3 write) | 시간축 phase/commitment 추적 | **Keep** | 즉시~상시 | v3 token 안정, 렌더 반영 |
| `analysis/HypothesisGenerator.scala` | rule+prior 랭킹, 일부 engine coupling | **Keep** | 즉시~상시 | prior shadow/canary에서 품질 gate 통과 |
| `analysis/ProbeDetector.scala`의 `competitiveProbes`/`defensiveProbes` | MultiPV 근접 대안 설명용(강한 PV 후행) | **Demote** | **R+1** (다음 릴리스) | plan-intent probe 커버리지가 동일 목적 대체 |
| `analysis/ProbeDetector.scala`의 `planScoring.topPlans` ghost probe | rule top-plan 후행 probe | **Demote** | **R+1~R+2** | hypothesis 기반 probe hit-rate가 동등 이상 |
| `analysis/NarrativeContextBuilder.scala`의 `buildAbsentFromTopMultiPvReasons` 휴리스틱 | 근거 없는 일반 문구 fallback | **Replace** | **R+1** | evidence-derived reason 90%+ 적용 |
| `analysis/NarrativeOutlineBuilder.scala`의 `best continuation` 고정 서두 | PV1 중심 서사 앵커 | **Replace** | **R+2** | plan distribution-first 템플릿 A/B 우위 |
| `analysis/BookStyleRenderer.scala`의 PV1 우선 wrap-up 문구 | 결과 설명이 PV 중심 | **Replace** | **R+2** | mainStrategicPlans 우선 렌더로 전환 완료 |
| `analysis/MotifTokenizer.scala`의 `tokenizePv` 중심 motif 공급 | 라인 의존 motif 생성 | **Delete 예정** | **R+3** | 상태/궤적 기반 motif 공급이 기능 parity 달성 |
| `model/Variation.scala`의 샘플 PV 인용 중심 보조 로직 | 문장용 PV 샘플링 | **Delete 예정** | **R+3** | evidence beat가 PV 샘플 의존 없이 생성 가능 |
| `LlmApi.scala`의 `PlanRecall@3`(top rule vs top hypothesis) 정의 | rule-top 기준 편향 지표 | **Replace** | **R+2** | hypothesis-first 기준 지표로 교체 |
| `ui/analyse/src/bookmaker/probeOrchestrator.ts`의 범용 motif 추론 fallback | 클라 휴리스틱 채움 | **Demote** | **R+2** | purpose별 필수 신호 생성률 목표치 달성 |
| `model/strategic/StrategicModels.scala`의 token v1/v2 읽기 | 구버전 토큰 호환 | **Compat only** | 상시 (제한) | `v1/v2 read, v3 write` 정책 유지 |
| `model/ProbeModel.scala`의 `fen` optional/legacy 결과 허용 | 구클라 응답 호환 | **Compat only** | 상시 (제한) | 클라 100% 신규 스키마 전환 전까지 유지 |

## 운영 규칙 (짧게)

- **Shadow**: Replace/Demote 대상은 계산만 하고 응답 반영 금지.
- **Canary**: 트래픽 일부에서만 반영, `PlanRecall@3/LatentPrecision/PV-coupling ratio` gate 적용.
- **Default-on**: gate 통과 후 활성화, 기존 PV 경로는 `Demote -> Delete` 순서로 제거.
- **Delete 실행 원칙**: 반드시 `대체 경로 parity + 회귀 테스트 + 롤백 플래그` 3조건 충족 후 삭제.
