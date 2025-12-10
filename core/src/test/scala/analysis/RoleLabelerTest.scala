package chess
package analysis

import munit.FunSuite
import AnalysisModel.{EngineEval, EngineLine}
import FeatureExtractor._
import RoleLabeler.Role

class RoleLabelerTest extends FunSuite {
  
  val emptyFeatures = PositionFeatures(
    fen = "",
    sideToMove = "white",
    plyCount = 0,
    pawns = PawnStructureFeatures(0,0,Nil,Nil,0,0,0,0,0,0,0,0,0,0,false,false,false,false,false,false,0,0,false,false),
    activity = ActivityFeatures(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,false,false),
    kingSafety = KingSafetyFeatures("none","none",0,0,0,0,false,false,0.0,0.0,0,0),
    materialPhase = MaterialPhaseFeatures(0,0,0,0,0,0,0,"middlegame"),
    development = DevelopmentFeatures(0,0,0,0,false,false),
    space = SpaceFeatures(0,0,0,0,0,0),
    coordination = CoordinationFeatures(false,false,false,false,0,0),
    geometry = GeometryFeatures(false, false),
    tactics = TacticalFeatures(0,0,0,0,0,0)
  )

  // Helper to create a dummy context
  def createCtx(
    phase: String = "middlegame",
    evalBef: Int = 0,
    evalAft: Int = 0,
    devBef: Int = 3,
    devAft: Int = 3,
    spaceBef: Int = 5,
    spaceAft: Int = 5,
    isCapture: Boolean = false,
    passedPawns: Int = 0,
    advancedPawns: Int = 0
  ): AnalysisContext = {
    AnalysisContext(
      move = "e2e4",
      evalBefore = Some(EngineEval(10, List(EngineLine("e2e4", 50.0, Some(evalBef), None, Nil)))),
      evalAfter = Some(EngineEval(10, List(EngineLine("e2e4", 50.0, Some(evalAft), None, Nil)))),
      bestMoveBefore = None,
      bestMoveEval = None,
      featuresBefore = emptyFeatures.copy(
        development = DevelopmentFeatures(devBef, 0, 0, 0, false, false),
        space = SpaceFeatures(spaceBef, 0, 0, 0, 0, 0),
        materialPhase = MaterialPhaseFeatures(30,30,0,0,0,0,0,"middlegame")
      ),
      featuresAfter = emptyFeatures.copy(
        development = DevelopmentFeatures(devAft, 0, 0, 0, false, false),
        space = SpaceFeatures(spaceAft, 0, 0, 0, 0, 0),
        pawns = PawnStructureFeatures(0, 0, Nil, Nil, 0, 0, 0, 0, 0, 0, passedPawns, passedPawns, advancedPawns, advancedPawns, false, false, false, false, false, false, 0, 0, false, false),
        materialPhase = MaterialPhaseFeatures(27,27,0,0,0,0,0,"middlegame")
      ),
      phaseBefore = phase,
      phaseAfter = phase,
      isCapture = isCapture,
      isCheck = false,
      isCastle = false
    )
  }

  test("RoleLabeler - Development Advantage") {
    // Developed a piece: Unmoved minors 3 -> 2
    val ctx = createCtx(phase = "opening", devBef = 3, devAft = 2)
    val roles = RoleLabeler.label(ctx)
    assert(roles.contains(Role.DevelopmentAdvantage))
  }

  test("RoleLabeler - Space Grab") {
    // Central space 5 -> 8
    val ctx = createCtx(phase = "opening", spaceBef = 5, spaceAft = 8)
    val roles = RoleLabeler.label(ctx)
    assert(roles.contains(Role.SpaceGrab))
  }
  
  test("RoleLabeler - Equalizer (Swindle)") {
    // Eval -200 -> -40
    val ctx = createCtx(evalBef = -200, evalAft = -40)
    val roles = RoleLabeler.label(ctx)
    assert(roles.contains(Role.Equalizer))
  }
  
  test("RoleLabeler - Piece Trade (Simplification)") {
    // Need explicit features override for simplification trigger
    val ctxTrade = createCtx(isCapture = true).copy(
      featuresBefore = emptyFeatures.copy(materialPhase = MaterialPhaseFeatures(30, 30, 0, 0, 0, 0, 0, "middlegame")),
      featuresAfter = emptyFeatures.copy(materialPhase = MaterialPhaseFeatures(27, 27, 0, 0, 0, 0, 0, "middlegame")),
      phaseBefore = "middlegame"
    )
    val roles = RoleLabeler.label(ctxTrade)
    assert(roles.contains(Role.PieceTrade))
  }
  
  test("RoleLabeler - Promotion Race") {
    // Endgame with advanced passed pawns
    val ctx = createCtx(phase = "endgame", passedPawns = 1, advancedPawns = 1)
    val roles = RoleLabeler.label(ctx)
    assert(roles.contains(Role.PromotionRace))
  }
}
