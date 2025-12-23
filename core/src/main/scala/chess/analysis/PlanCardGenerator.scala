package chess.analysis

import chess.analysis.AnalysisTypes.*
import chess.analysis.ConceptLabeler.{PlanTag, SUCCESS_THRESHOLD_CP, FAILURE_THRESHOLD_CP}

object PlanCardGenerator:

  def generate(
    experiments: List[ExperimentResult],
    baselineEval: Int
  ): (List[PlanTag], List[RichTag], Map[String, PlanEvidence]) =

    val tags = List.newBuilder[PlanTag]
    val richTags = List.newBuilder[RichTag]
    val evidence = Map.newBuilder[String, PlanEvidence]

    experiments.foreach { ex =>
      val score = getScore(ex)
      val delta = score - baselineEval
      val cType = ex.metadata.get("candidateType").getOrElse("Generic")
      val move = ex.move.getOrElse("?")
      // Assuming PV is available
      val pvLines = ex.eval.lines.headOption.map(_.pv).getOrElse(List(move))
      
      val isRefuted = delta < -200

      // Map to Tags and Evidence
      cType match
        case "CentralBreak" =>
          if delta > SUCCESS_THRESHOLD_CP then
            add(tags, richTags, evidence, PlanTag.CentralBreakGood, "central_break_good", "Central Break", move, "Piece Activity", delta, pvLines)
          else if isRefuted then
            add(tags, richTags, evidence, PlanTag.CentralBreakRefuted, "central_break_refuted", "Central Break", move, "Failure", delta, pvLines)
          else if delta < FAILURE_THRESHOLD_CP then
             add(tags, richTags, evidence, PlanTag.CentralBreakPremature, "central_break_premature", "Central Break", move, "Premature", delta, pvLines)
        
        case "QueensideMajority" =>
          if delta > SUCCESS_THRESHOLD_CP then
             add(tags, richTags, evidence, PlanTag.QueensideMajorityGood, "queenside_expansion_good", "Queenside Expansion", move, "Create Passed Pawn", delta, pvLines)

        case "KingsidePawnStorm" =>
          if delta > SUCCESS_THRESHOLD_CP then
             add(tags, richTags, evidence, PlanTag.KingsideAttackGood, "kingside_attack_good", "Kingside Attack", move, "Checkmate", delta, pvLines)
          else if isRefuted then
             add(tags, richTags, evidence, PlanTag.KingsideAttackRefuted, "kingside_attack_refuted", "Kingside Attack", move, "Refuted", delta, pvLines)

        case "PieceImprovement" =>
           if delta > SUCCESS_THRESHOLD_CP then
             add(tags, richTags, evidence, PlanTag.PieceImprovementGood, "piece_improvement_good", "Piece Improvement", move, "Better Activity", delta, pvLines)

        case "RookLift" =>
           if delta > SUCCESS_THRESHOLD_CP then
             add(tags, richTags, evidence, PlanTag.RookLiftGood, "rook_lift_good", "Rook Lift", move, "Attack", delta, pvLines)
             
        // From QuietImprovementProbe
        case "QuietImprovement" =>
           if delta > SUCCESS_THRESHOLD_CP then
             add(tags, richTags, evidence, PlanTag.PieceImprovementGood, "quiet_improvement_good", "Quiet Improvement", move, "Strategic Value", delta, pvLines)

        case _ => // Ignored
    }

    (tags.result().distinct, richTags.result(), evidence.result())

  private def add(
      tags: scala.collection.mutable.Builder[PlanTag, List[PlanTag]],
      richTags: scala.collection.mutable.Builder[RichTag, List[RichTag]],
      evidence: scala.collection.mutable.Builder[(String, PlanEvidence), Map[String, PlanEvidence]],
      tag: PlanTag,
      idBase: String,
      concept: String,
      starterMove: String,
      goal: String,
      delta: Int,
      pv: List[String]
  ): Unit =
    tags += tag
    // Create unique ID for this instance? Or generic?
    // Using generic idBase works if only one per type per position mostly.
    val id = s"plan:$idBase" 
    val ref = EvidenceRef("plans", id)
    
    richTags += RichTag(id, delta.toDouble, TagCategory.Plan, List(ref))
    evidence += (id -> PlanEvidence(concept, starterMove, goal, delta.toDouble, pv))

  private def getScore(ex: ExperimentResult): Int =
    ex.eval.lines.headOption.flatMap(l => l.cp.orElse(l.mate.map(m => if m > 0 then 10000 - m else -10000 + m))).getOrElse(0)
