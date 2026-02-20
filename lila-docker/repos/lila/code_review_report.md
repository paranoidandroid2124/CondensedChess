# 체스 LLM 해설 시스템 E2E 코드 리뷰 리포트 (FIDE 2300+ 관점)

본 리뷰는 `lila/modules/llm` 및 `ui/analyse/src/bookmaker` 영역을 중심으로, 해설 엔진의 체스 논리적 정합성, 왜곡/환각 가능성, 그리고 소프트웨어 회귀 리스크를 비판적으로 검토한 결과입니다.

---

## 1. 확정 사실 (파이프라인/설계)
- **엔진 의존형 실시간 평가 파이프라인**: 
  - UI(`probeOrchestrator.ts`)에서 짧은 timeout(move당 700~5000ms) 내에 다중 PV를 탐색하여 `EvalVariation`을 백엔드로 넘김.
- **모티프 및 평가 고착화 구조**:
  - `CommentaryEngine.scala`가 PV를 기반으로 `MoveAnalyzer.tokenizePv`를 호출해 모티프(전술/전략 패턴)를 하드코딩된 규칙에 따라 추출.
  - `PositionAnalyzer.scala`가 정적 보드 상태(기물 이동 가능 수, 구조)를 스캔하여 Advantage Features 생성.
- **템플릿과 정규식 기반 나이프 튜닝**:
  - `NarrativeLexicon.scala`에서 해시(bead)를 사용해 동일 국면에서는 동일한 텍스트 템플릿을 무작위 추출. 
  - `PostCritic.scala`에서 최종 생성 텍스트를 정규식/String replace로 강제 치환하여 분위기를 조율(톤다운).

---

## 2. 주요 결함 Top 10

### P0 (사실 정합성 오류 / 치명적 오판)
1. **[P0] 거짓 전초기지(Outpost) 인지오류** 
   - `MoveAnalyzer.scala:217` : 기물이 4랭크 이상(백 기준) 넘어갔다는 이유만으로 `isInEnemyTerritory`를 만족해 Outpost로 판정. 체스에서 Outpost는 폰의 보호를 받고 적 폰에 의해 쫓겨나지 않는 칸이어야 함.
2. **[P0] 기물 활동성(Mobility)의 무지성 합산**
   - `PositionAnalyzer.scala:264` (`pseudoMobility`) : 공격받는 칸, 내 기물로 막힌 칸 등의 제약 없이 단순히 합법/유사 합법 수(attacks)의 개수만 더함. 8칸 모두 자살 수인 림(rim)의 나이트가 중앙의 안정된 나이트보다 활동적이라고 평가할 수 있음.
3. **[P0] 강직형(Rigid) 오프닝 트랩 감지**
   - `TruthGate.scala:37` : 트랩을 UCI String 배열(`List("f3e5", "g4d1"...)`)의 정확한 순서로만 감지함. 체스의 핵심인 '수순 비틀기(Transposition)'를 전혀 고려하지 못해, 완전히 동일한 트랩 국면이어도 수순이 다르면 트랩 해설이 증발함.
4. **[P0] 거짓 인과관계 주입 (허위 해설)**
   - `CommentaryEngine.scala:567` : 엔진 평가치 차이가 0.2점(20cp) 이하일 때 무조건 `"Both branches are close, but [엔진수] keeps the cleaner move-order."`라는 템플릿 출력. 엔진 1픽 수가 매우 난해한 전술적 난전이고, 유저의 수가 깔끔한 전개 수여도 무조건 전자를 "cleaner"라고 왜곡함.

### P1 (해설의 강도/질적 저하 리스크)
5. **[P1] 후가공(PostCritic) 정규식의 회귀성**
   - `PostCritic.scala:125` : `"The game remains tense and double-edged."` 등의 문장을 하드코딩된 String Replace로 치환함. LLM 프롬프트가 미세하게 변경되어 문장 부호 하나라도 바뀌면 동작하지 않아 유지보수성이 최악(Brittle)임.
6. **[P1] 안전성 검증이 누락된 포크(Fork) 판정**
   - `MoveAnalyzer.scala:302` : 다중 타겟에 대해 `attackerIsSafe` 검증을 1차원적으로만(`attackers(dest).isEmpty`) 수행. 디스커버리 체카웃이나 엔진 PV 상 1수 뒤에 잡히는 포크 기물을 "성공적인 포크"라고 성급히 찬양할 위험이 있음.
7. **[P1] 맹목적인 백랭크 메이트 검증 로직**
   - `TruthGate.scala:110` : 백랭크 메이트 여부를 킹 앞 랭크에 "내 기물이 있는가"(`pieceAt(sq).exists(_.color == loser)`)로만 판정함. 본인의 폰이 아니라 남의 기물이 앞을 막고 있어도 백랭크 메이트로 착각할 수 있는 논리적 헛점.
8. **[P1] 잘못된 킹 안전성(King Safety) 평가방식**
   - `PositionAnalyzer.scala:399` : 적 기물이 킹 반경 어디든 공격하면 `attackersCount`를 늘림. 퀸이 조준하든 폰이 보호칸을 노리든 가중치 없이 +1로 동등하게 처리함.

### P2 (템플릿 남용 및 표현력 저하)
9. **[P2] 깊이가 얕은 대안수 왜곡**
   - 평가 시간이 모자랄 때 나온 얕은 Depth의 PV를 기반으로 "이 대안은 ~패배로 이어집니다"라고 평가 절하함 (`probeOrchestrator.ts`의 타임아웃 결과에 의존).
10. **[P2] 감탄사 남발 방지 불가**
    - `NarrativeLexicon.scala`는 단순히 bead 난수에 의해 Dramatic/Coach 스타일을 섞어 쓰지만, 국면의 본질적 난이도와 맞지 않게 극적인 템플릿(`"The board is ablaze!"`)이 무작위 표출될 여지가 큼.

---

## 3. 체스 강자 관점의 오판 시나리오 5개

| 시나리오 | 입력 조건 (상황) | 시스템의 잘못된 해설 예상 | 진실 (체스 강자의 시야) |
|---|---|---|---|
| **1. Транспозиция(Transposition) 누락** | Black이 스콜라 메이트 방어 시 `...Nc6` 대신 수순을 비틀어 `...g6` 등 다른 수순 사용 | *"이곳의 오프닝은 무난하며..."* (트랩 사실을 놓침) | 해당 국면은 트랩의 위협이 살아있는 구조적 분기점임. |
| **2. 0.1점 차이 허위 포장** | 복잡한 복전에서 엔진 탑무브(+0.4) 대신 인간적인 피스 전개수(+0.3)를 둠 | *"평가는 비슷하나 엔진픽이 더 깔끔한 수순(cleaner move-order)입니다."* | 실제로 유저의 수가 훨씬 안전하고 깔끔하며 엔진픽은 초고난이도의 외줄타기 전술일 때가 많음. |
| **3. 가짜 전초기지 부여** | 백이 나이트를 적 5랭크 중앙에 올렸으나, 다음 수에 적 c6 폰으로 바로 쫓겨날 수 있음. | *"강력한 전초기지(outpost)를 구축했습니다."* | c6 폰으로 즉각 쫓겨날 위치이므로 전초기지가 아님. (시간 낭비 / 템포 내줌) |
| **4. 무의미한 활동성 고득점** | 구석(H랭크)에 몰려 도망칠 곳 없는 나이트인데 공격 공격칸 4개는 비어있음 | *"나이트의 활동성(Mobility)이 좋으며..."* | 자살 수밖에 없는 완전히 갇힌 기물임. |
| **5. 단기 포크 과장** | 나이트가 포크를 걸었지만 사실 상대방의 디스커버리 핀에 걸려 바로 다음 수에 폰에 잡힘. | *"결정적인 포크(Fork) 전술입니다!"* | 포크가 성립하지 않는 블런더임(전술을 둔 플레이어가 오히려 망함). |

---

## 4. 최소 수정 제안

- **[수정 방향 1]: Outpost 정의 정상화 (MoveAnalyzer.scala)**
  - 판정 조건 추가: 적진 랭크 진입뿐만 아니라, `board.attackers(dest, !color)` 중에 적 폰(Pawn)이 없거나 내 폰으로 바로 수비 가능한지(Support) 확인 필수.
  - *회귀 위험: 낮음. 모티프 정확도가 직관적으로 상승.*
- **[수정 방향 2]: TrutGate 하드코딩 탈피 (TruthGate.scala)**
  - UCI 수순(`List[String]`)이 아니라 목표 형상(FEN/Bitboard 패턴 및 특정 수의 존재 여부) 기반으로 트랩을 감지하도록 개편.
  - *회귀 위험: 중간. 기존 테스트 깨질 확률 높음.*
- **[수정 방향 3]: 가짜 인과관계 텍스트 제거 (CommentaryEngine.scala)**
  - 평가치 차이가 20cp 이하일 때 "cleaner move-order"라고 단정짓는 로직 삭제. 대신 "실전적으로 동등한 대안" 수준의 중립적 문구로 교체.
  - *회귀 위험: 낮음.*
- **[수정 방향 4]: PostCritic 정규식 제거 (PostCritic.scala)**
  - LLM Prompt 단에서 톤앤매너를 제어하거나, NarrativeBuilder의 템플릿 단계에서 근본적 어휘를 조정하도록 하여 PostCritic의 하드코딩 Replace 체인 제거.

---

## 5. 필수 테스트 추가안 (Test Suite)

1. **`OutpostValidationTest`**
   - **검증 포인트**: 적 폰에 의해 즉시 물리칠 수 있는 위치의 폰/나이트는 Outpost 모티프로 할당되지 않는지 확인.
   - **실패 조건**: d5 칸에 들어간 백 나이트를 c6/e6 칸 흑폰이 지배하고 있을 때 Outpost 모티프 반환 시 실패.
2. **`TranspositionTrapTest`**
   - **검증 포인트**: 스콜라 메이트나 레갈 메이트의 수순 2가지를 무작위로 뒤바꾸어도 동일하게 트랩을 감지하는지.
   - **실패 조건**: 수순이 바뀌었다고 `TruthGate.detect`가 `None`을 반환하면 실패.
3. **`FalseCausalityTextTest`**
   - **검증 포인트**: 엔진과의 차이가 0.1점(10cp)인 상황에서 유저의 수가 전략적이고 엔진의 수가 전술적일 때 텍스트 생성 검증.
   - **실패 조건**: 텍스트에 "cleaner move", "more accurate route" 같은 단정적 헛소리가 포함되면 실패.

---

## 6. 릴리즈 게이트 제안 (배포 차단 기준)
1. **Fact Check Gate**: PGN 파서에서 `MoveAnalyzer` 통과 후 반환된 `Motif` 리스트 중 `Check`, `Mate`, `Outpost` 정보가 엔진 연산과 1개라도 모순될 경우 배포 차단.
2. **Brittle String Change Gate**: `PostCritic.scala` 또는 `NarrativeLexicon.scala` 내부의 특정 영어 문장 Replace 로직이 추가되면 CI에서 경고/차단(향후 LLM/프롬프트 의존성을 높이기 위함).
3. **Eval Deviation**: 얕은 Depth(Depth < 18)에서 추출된 PV로 인해 발생한 극단적 평가(Mate 등)가 Bookmaker 프론트엔드 단에서 사실인 양 렌더링될 조짐이 보일 경우 Fail.

---
### 🚨 지금 당장 최우선으로 고쳐야 할 3가지 (Hotfixes)
1. **가짜 전초기지 판정 삭제 (`MoveAnalyzer.scala:217`)**: 랭크 넘었다고 무조건 Outpost 주는 조건문 수정.
2. **허위 인과 텍스트 출력 차단 (`CommentaryEngine.scala:567`)**: 0.2점 차이에 "cleaner"를 강제하는 편향적 하드코딩 문구 즉각 제거.
3. **활동성(Mobility) 합산 오작동 보수 (`PositionAnalyzer.scala:264`)**: 자살/피격 칸으로 이동하는 수를 Mobility 스코어에서 제외.
