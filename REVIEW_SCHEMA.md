# Review JSON 스키마 (엔진+피처+크리티컬)

엔진/피처 분석 후 LLM에 전달하는 최종 JSON 구조입니다. 모든 값은 분석 결과에서만 가져오며, LLM은 새 수/숫자를 발명하지 않는 전제가 필요합니다.

```jsonc
{
  "opening": { "name": "Ruy Lopez", "eco": "C88", "ply": 7 }, // optional
  "oppositeColorBishops": false,
  "critical": [
    {
      "ply": 12,
      "reason": "ΔWin% blunder spike",
      "deltaWinPct": -35.4,
      "branches": [
        { "move": "c5", "winPct": 62.1, "label": "best (PV1)", "pv": ["c5","dxc5","Bxc5"] },
        { "move": "Re8", "winPct": 58.3, "label": "alt (PV2)", "pv": ["Re8","O-O"] },
        { "move": "h6", "winPct": 55.0, "label": "alt (PV3)", "pv": ["h6","Bh4"] }
      ]
    }
  ],
  "timeline": [
    {
      "ply": 1,
      "turn": "black",               // side to move
      "san": "e4",
      "uci": "e2e4",
      "fen": "…",
      "features": {
        "pawnIslands": 1,
        "isolatedPawns": 0,
        "doubledPawns": 0,
        "passedPawns": 0,
        "rookOpenFiles": 0,
        "rookSemiOpenFiles": 0,
        "bishopPair": true,
        "kingRingPressure": 0,
        "spaceControl": 0
      },
      "evalBeforeShallow": {
        "depth": 8,
        "lines": [
          { "move": "e2e4", "winPct": 53.39, "cp": 34, "pv": ["e2e4","e7e5"] },
          { "move": "d2d4", "winPct": 52.30, "cp": 23, "pv": ["d2d4","g8f6", "..."] }
        ]
      },
      "evalBeforeDeep": { "depth": 14, "lines": [ /* MultiPV */ ] },
      "winPctBefore": 52.70,          // best line win% before the played move
      "winPctAfterForPlayer": 53.79,   // after the played move, player-to-move perspective
      "deltaWinPct": 1.10,             // after - before
      "judgement": "ok"                // ok/inaccuracy/mistake/blunder
      "concepts": {
        "dynamic": 0.02,
        "drawish": 0.65,
        "imbalanced": 0.17,
        "tacticalDepth": 0.03,
        "blunderRisk": 0.02,
        "pawnStorm": 0.00
      }
    }
  ]
}
```

## 필드 설명
- `opening`: Opening DB 매칭 결과(ECO/이름/ply).
- `oppositeColorBishops`: 이색비숍 여부.
- `critical`: ΔWin%와 개념 점프 기반 상위 N(기본 5) 노드, 각 노드에 MultiPV 브랜치(best/PV2/PV3).
- `timeline`: ply별 상세
  - `features`: 구조/기물/킹 안전/공간 등 엔진 없이 계산된 피처.
  - `evalBefore*`: move 전 FEN의 Stockfish MultiPV 결과. `winPct`는 cp→Win% 변환.
  - `winPctBefore`: best line Win% (깊은 탐색) move 전.
  - `winPctAfterForPlayer`: move 후, 수를 둔 플레이어 관점 Win%.
  - `deltaWinPct`: after - before (플레이어 관점).
  - `judgement`: ΔWin% 기준 ok/inaccuracy/mistake/blunder.
  - `concepts`: 동적/정적/전술 느낌 점수(0–1), LLM이 “dynamic해졌다”, “drawish” 등을 표현할 때 사용.

## LLM 프롬프트 가이드 (한 번 호출)
- 시스템:
  - “너는 체스 코치. 아래 JSON은 엔진+피처 분석 결과다.”
  - “PGN/포지션을 직접 계산하지 말고 JSON의 eval/라인/태그만 사용하라. JSON 밖의 수/라인/평가를 생성하거나 추정하지 말 것.”
  - “새 수 발명 금지, 숫자 왜곡 금지, 주어진 PV 순서/수만 인용. PV 언급은 최대 1~2수 요약.”
  - “concepts/judgement/ΔWin%/critical branches를 근거로 인간적인 설명을 붙인다. 숫자만 나열하지 말고 ‘왜’ 의미가 있는지 풀어라.”
  - “언급하는 수가 timeline/critical에 실제 존재하는지 확인. move 번호를 추측하지 말고 JSON 그대로 사용.”
  - “모호하면 ‘JSON에 근거 부족’이라고 말하고 발명하지 마라.”
- 유저: 위 JSON
- 기대 출력:
  1) 게임 전체 요약(1–3단락), 오프닝 요약(ECO/라인 선택 영향).
  2) critical node별 장문 해설: 왜 중요한지(ΔWin%/concept jump), 실제 수 평가, 엔진 대안 비교(PV 요약), 개념 태그( dynamic/drawish/pawn storm 등).
  3) 미들→엔드 전환 포인트(drawish 상승 등).
  4) 전체 성격(dynamic/dry/imbalanced/drawish).
  5) 패턴/훈련 제안: concept/ΔWin% 누적을 근거로 추천(모델 게임/퍼즐/엔드게임 유형 등).
  6) (옵션) move별 한줄 코멘트: `judgement`/`deltaWinPct`/`concepts` 활용한 짧은 문장.

### 개념 해석 템플릿(예시)
- `blunderRisk`↑: 좋은 수/나쁜 수 승률 격차가 크고 왕 주변이 열려 있어, 한 수 실수 시 큰 손해로 이어지는 구간.
- `dynamic`↑: 캡처 가능 tension이나 왕 주변 위협이 많아 전술 충돌이 쉬운 구간.
- `dry`↑: 퀸 교환 가능, tension/open file 적고 평가 변동이 작아 돌파가 어려운 구간. 작은 실수 하나가 승패를 가를 수 있음.
- `fortress`↑: 우세하지만 돌파 수가 적고 평가가 안정돼 전환이 어려운 구간.
- `engineLike`↑: 최선 수와 2등 수의 승률 격차가 커서 인간에게는 찾기 어렵고 실수 여지가 큰 구간.

## JSON 생성 시 주의
- 엔진/피처 값만 사용, 추정/보간 금지.
- PV 밖의 수 언급 금지, ΔWin%는 플레이어 관점(after-before)로 계산.
- 스케일: `concepts`는 0–1, `winPct*`는 0–100.
