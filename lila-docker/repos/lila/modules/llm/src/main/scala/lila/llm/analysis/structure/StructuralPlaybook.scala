package lila.llm.analysis.structure

import chess.Color
import lila.llm.model.PlanId
import lila.llm.model.structure.{ StructureId, StructuralPlaybookEntry }

object StructuralPlaybook:

  val entries: Map[StructureId, StructuralPlaybookEntry] = List(
    StructuralPlaybookEntry(
      structureId = StructureId.Carlsbad,
      whitePlans = List(PlanId.MinorityAttack, PlanId.FileControl, PlanId.PawnBreakPreparation),
      blackPlans = List(PlanId.KingsideAttack, PlanId.PawnBreakPreparation, PlanId.CentralControl),
      counterPlans = List(PlanId.PawnStorm),
      preconditions = List("minority_ready", "tension_or_break"),
      narrativeIntent = "fix queenside targets first, then open files with measured breaks",
      narrativeRisk = "medium"
    ),
    StructuralPlaybookEntry(
      structureId = StructureId.IQPWhite,
      whitePlans = List(PlanId.PieceActivation, PlanId.KingsideAttack, PlanId.PawnBreakPreparation),
      blackPlans = List(PlanId.WeakPawnAttack, PlanId.Blockade, PlanId.Simplification),
      counterPlans = List(PlanId.PawnChain),
      preconditions = List("piece_activity"),
      narrativeIntent = "keep pieces active before clarifying the center",
      narrativeRisk = "dynamic"
    ),
    StructuralPlaybookEntry(
      structureId = StructureId.IQPBlack,
      whitePlans = List(PlanId.WeakPawnAttack, PlanId.Blockade, PlanId.Simplification),
      blackPlans = List(PlanId.PieceActivation, PlanId.KingsideAttack, PlanId.PawnBreakPreparation),
      counterPlans = List(PlanId.PawnChain),
      preconditions = List("piece_activity"),
      narrativeIntent = "pressure central weakness while controlling tactical counterplay",
      narrativeRisk = "dynamic"
    ),
    StructuralPlaybookEntry(
      structureId = StructureId.HangingPawnsWhite,
      whitePlans = List(PlanId.PawnBreakPreparation, PlanId.SpaceAdvantage, PlanId.PieceActivation),
      blackPlans = List(PlanId.Blockade, PlanId.WeakPawnAttack, PlanId.Prophylaxis),
      counterPlans = List(PlanId.QueenTrade),
      preconditions = List("tension_or_break"),
      narrativeIntent = "prepare the right pawn break before committing pieces",
      narrativeRisk = "high"
    ),
    StructuralPlaybookEntry(
      structureId = StructureId.HangingPawnsBlack,
      whitePlans = List(PlanId.Blockade, PlanId.WeakPawnAttack, PlanId.Prophylaxis),
      blackPlans = List(PlanId.PawnBreakPreparation, PlanId.SpaceAdvantage, PlanId.PieceActivation),
      counterPlans = List(PlanId.QueenTrade),
      preconditions = List("tension_or_break"),
      narrativeIntent = "force the pawn pair to advance on your terms",
      narrativeRisk = "high"
    ),
    StructuralPlaybookEntry(
      structureId = StructureId.FrenchAdvanceChain,
      whitePlans = List(PlanId.KingsideAttack, PlanId.SpaceAdvantage, PlanId.PawnStorm),
      blackPlans = List(PlanId.PawnBreakPreparation, PlanId.Counterplay, PlanId.WeakPawnAttack),
      counterPlans = List(PlanId.Simplification),
      preconditions = List("locked_center"),
      narrativeIntent = "attack on the wing that benefits most from the locked center",
      narrativeRisk = "high"
    ),
    StructuralPlaybookEntry(
      structureId = StructureId.NajdorfScheveningenCenter,
      whitePlans = List(PlanId.KingsideAttack, PlanId.CentralControl, PlanId.PieceActivation),
      blackPlans = List(PlanId.PawnBreakPreparation, PlanId.QueensideAttack, PlanId.Counterplay),
      counterPlans = List(PlanId.QueenTrade),
      preconditions = List("tension_or_break"),
      narrativeIntent = "time central and flank breaks to keep initiative",
      narrativeRisk = "high"
    ),
    StructuralPlaybookEntry(
      structureId = StructureId.BenoniCenter,
      whitePlans = List(PlanId.QueensideAttack, PlanId.SpaceAdvantage, PlanId.CentralControl),
      blackPlans = List(PlanId.KingsideAttack, PlanId.Counterplay, PlanId.PawnBreakPreparation),
      counterPlans = List(PlanId.Simplification),
      preconditions = List("piece_activity"),
      narrativeIntent = "balance space edge against dynamic counterplay chances",
      narrativeRisk = "high"
    ),
    StructuralPlaybookEntry(
      structureId = StructureId.KIDLockedCenter,
      whitePlans = List(PlanId.QueensideAttack, PlanId.SpaceAdvantage, PlanId.Prophylaxis),
      blackPlans = List(PlanId.KingsideAttack, PlanId.PawnStorm, PlanId.Counterplay),
      counterPlans = List(PlanId.QueenTrade),
      preconditions = List("locked_center"),
      narrativeIntent = "keep opposite-wing plans synchronized with king safety",
      narrativeRisk = "high"
    ),
    StructuralPlaybookEntry(
      structureId = StructureId.SlavCaroTriangle,
      whitePlans = List(PlanId.CentralControl, PlanId.PieceActivation, PlanId.FileControl),
      blackPlans = List(PlanId.CentralControl, PlanId.PawnBreakPreparation, PlanId.Simplification),
      counterPlans = List(PlanId.Sacrifice),
      preconditions = List("solid_center"),
      narrativeIntent = "improve piece placement before forcing pawn decisions",
      narrativeRisk = "medium"
    ),
    StructuralPlaybookEntry(
      structureId = StructureId.MaroczyBind,
      whitePlans = List(PlanId.SpaceAdvantage, PlanId.PieceActivation, PlanId.Prophylaxis),
      blackPlans = List(PlanId.PawnBreakPreparation, PlanId.Counterplay, PlanId.QueensideAttack),
      counterPlans = List(PlanId.Simplification),
      preconditions = List("piece_activity"),
      narrativeIntent = "squeeze space while denying timely pawn breaks",
      narrativeRisk = "dynamic"
    ),
    StructuralPlaybookEntry(
      structureId = StructureId.Hedgehog,
      whitePlans = List(PlanId.SpaceAdvantage, PlanId.PieceActivation, PlanId.Prophylaxis),
      blackPlans = List(PlanId.Counterplay, PlanId.PawnBreakPreparation, PlanId.Prophylaxis),
      counterPlans = List(PlanId.PawnStorm),
      preconditions = List("counter_break_watch"),
      narrativeIntent = "maintain compact defense until the right counter-break appears",
      narrativeRisk = "dynamic"
    ),
    StructuralPlaybookEntry(
      structureId = StructureId.FianchettoShell,
      whitePlans = List(PlanId.KingsideAttack, PlanId.PawnStorm, PlanId.PieceActivation),
      blackPlans = List(PlanId.KingsideAttack, PlanId.PawnStorm, PlanId.PieceActivation),
      counterPlans = List(PlanId.QueenTrade),
      preconditions = List("king_flank_focus"),
      narrativeIntent = "build kingside pressure without loosening king cover",
      narrativeRisk = "dynamic"
    ),
    StructuralPlaybookEntry(
      structureId = StructureId.Stonewall,
      whitePlans = List(PlanId.KingsideAttack, PlanId.PieceActivation, PlanId.SpaceAdvantage),
      blackPlans = List(PlanId.KingsideAttack, PlanId.PieceActivation, PlanId.SpaceAdvantage),
      counterPlans = List(PlanId.WeakPawnAttack),
      preconditions = List("locked_center"),
      narrativeIntent = "anchor central chain, then expand where pieces are best placed",
      narrativeRisk = "medium"
    ),
    StructuralPlaybookEntry(
      structureId = StructureId.OpenCenter,
      whitePlans = List(PlanId.PieceActivation, PlanId.CentralControl, PlanId.FileControl),
      blackPlans = List(PlanId.PieceActivation, PlanId.CentralControl, PlanId.FileControl),
      counterPlans = List(PlanId.PawnChain),
      preconditions = List("open_center"),
      narrativeIntent = "prioritize activity and open lines over pawn grabs",
      narrativeRisk = "dynamic"
    ),
    StructuralPlaybookEntry(
      structureId = StructureId.LockedCenter,
      whitePlans = List(PlanId.PawnStorm, PlanId.QueensideAttack, PlanId.KingsideAttack),
      blackPlans = List(PlanId.PawnStorm, PlanId.QueensideAttack, PlanId.KingsideAttack),
      counterPlans = List(PlanId.Simplification),
      preconditions = List("locked_center"),
      narrativeIntent = "use locked center as timing cover for wing expansion",
      narrativeRisk = "high"
    ),
    StructuralPlaybookEntry(
      structureId = StructureId.FluidCenter,
      whitePlans = List(PlanId.PawnBreakPreparation, PlanId.CentralControl, PlanId.PieceActivation),
      blackPlans = List(PlanId.PawnBreakPreparation, PlanId.CentralControl, PlanId.PieceActivation),
      counterPlans = List(PlanId.PawnChain),
      preconditions = List("tension_or_break"),
      narrativeIntent = "keep options flexible and prepare the most favorable break",
      narrativeRisk = "balanced"
    ),
    StructuralPlaybookEntry(
      structureId = StructureId.SymmetricCenter,
      whitePlans = List(PlanId.PieceActivation, PlanId.CentralControl, PlanId.Prophylaxis),
      blackPlans = List(PlanId.PieceActivation, PlanId.CentralControl, PlanId.Prophylaxis),
      counterPlans = List(PlanId.Sacrifice),
      preconditions = List("solid_center"),
      narrativeIntent = "improve gradually, avoid irreversible pawn commitments",
      narrativeRisk = "balanced"
    )
  ).map(e => e.structureId -> e).toMap

  def lookup(id: StructureId): Option[StructuralPlaybookEntry] =
    entries.get(id)

  def expectedPlans(entry: StructuralPlaybookEntry, sideToMove: Color): List[PlanId] =
    if sideToMove == Color.White then entry.whitePlans else entry.blackPlans
