package lila.commentary.analysis

import chess.Color
import munit.FunSuite
import lila.commentary.model.{ Plan, PlanMatch, ProbeRequest }
import lila.commentary.model.authoring.{ PlanHypothesis, PlanViability }
import lila.commentary.model.strategic.PlanContinuity

final class PlanClaimBoundaryTest extends FunSuite:

  private def hypothesis(
      id: String,
      name: String,
      sources: List[String] = Nil,
      theme: String = "unknown",
      subplan: Option[String] = None
  ): PlanHypothesis =
    PlanHypothesis(
      planId = id,
      planName = name,
      rank = 0,
      score = 0.72,
      preconditions = Nil,
      executionSteps = Nil,
      failureModes = Nil,
      viability = PlanViability(score = 0.72, label = "medium", risk = "test"),
      evidenceSources = sources,
      themeL1 = theme,
      subplanId = subplan
    )

  test("explicit subplan becomes supportable proposal kind") {
    val proposal =
      PlanClaimBoundary.PlanProposal.fromHypothesis(
        hypothesis(
          id = "StructureFix",
          name = "Static weakness fixation",
          theme = PlanTaxonomy.PlanTheme.WeaknessFixation.id,
          subplan = Some(PlanTaxonomy.PlanKind.StaticWeaknessFixation.id)
        )
      )

    assertEquals(proposal.kind, Some(PlanTaxonomy.PlanKind.StaticWeaknessFixation))
    assertEquals(proposal.kindSource, PlanClaimBoundary.PlanKindSource.ExplicitSubplan)
    assertEquals(proposal.supportKind, Some(PlanTaxonomy.PlanKind.StaticWeaknessFixation))
    assertEquals(proposal.fallbackTheme, Some(PlanTaxonomy.PlanTheme.WeaknessFixation))
  }

  test("broad plan aliases remain proposal-only and do not mint default subplans") {
    val proposal =
      PlanClaimBoundary.PlanProposal.fromHypothesis(
        hypothesis(
          id = "PieceActivation",
          name = "Piece activation",
          sources = List("theme:piece_redeployment")
        )
      )

    assertEquals(proposal.theme, PlanTaxonomy.PlanTheme.PieceRedeployment)
    assertEquals(proposal.themeSource, PlanClaimBoundary.PlanThemeSource.SupportThemeTag)
    assertEquals(proposal.kind, None)
    assertEquals(proposal.supportKind, None)
    assertEquals(proposal.kindSource, PlanClaimBoundary.PlanKindSource.Missing)
    assertEquals(proposal.fallbackTheme, Some(PlanTaxonomy.PlanTheme.PieceRedeployment))
  }

  test("subplan support tags are typed support, not prose parsing") {
    val proposal =
      PlanClaimBoundary.PlanProposal.fromHypothesis(
        hypothesis(
          id = "FilePressure",
          name = "File pressure",
          sources = List("subplan:open_file_pressure")
        )
      )

    assertEquals(proposal.kind, Some(PlanTaxonomy.PlanKind.OpenFilePressure))
    assertEquals(proposal.kindSource, PlanClaimBoundary.PlanKindSource.SupportSubplanTag)
    assertEquals(proposal.theme, PlanTaxonomy.PlanTheme.PieceRedeployment)
    assertEquals(proposal.supportKind, Some(PlanTaxonomy.PlanKind.OpenFilePressure))
    assertEquals(proposal.fallbackTheme, Some(PlanTaxonomy.PlanTheme.PieceRedeployment))
  }

  test("plan rows need structured support tags before fallback theme is exposed") {
    val nameOnly =
      PlanClaimBoundary.PlanProposal.fromPlanRow(
        lila.commentary.model.PlanRow(
          rank = 1,
          name = "OpenFilePressure",
          score = 0.9,
          evidence = Nil,
          supports = Nil,
          blockers = Nil,
          missingPrereqs = Nil
        )
      )
    val tagged =
      PlanClaimBoundary.PlanProposal.fromPlanRow(
        lila.commentary.model.PlanRow(
          rank = 1,
          name = "OpenFilePressure",
          score = 0.9,
          evidence = Nil,
          supports = List("subplan:open_file_pressure"),
          blockers = Nil,
          missingPrereqs = Nil
        )
      )

    assertEquals(nameOnly.fallbackTheme, None)
    assertEquals(tagged.fallbackTheme, Some(PlanTaxonomy.PlanTheme.PieceRedeployment))
  }

  test("plan matches keep broad aliases proposal-only unless supports carry subplan tags") {
    val broad =
      PlanClaimBoundary.PlanProposal.fromPlanMatch(
        PlanMatch(
          plan = Plan.PieceActivation(Color.White),
          score = 0.82,
          evidence = Nil
        )
      )
    val tagged =
      PlanClaimBoundary.PlanProposal.fromPlanMatch(
        PlanMatch(
          plan = Plan.PieceActivation(Color.White),
          score = 0.82,
          evidence = Nil,
          supports = List("subplan:open_file_pressure")
        )
      )

    assertEquals(broad.theme, PlanTaxonomy.PlanTheme.PieceRedeployment)
    assertEquals(broad.themeSource, PlanClaimBoundary.PlanThemeSource.InferredProposal)
    assertEquals(broad.supportKind, None)
    assertEquals(broad.fallbackTheme, None)
    assertEquals(tagged.supportKind, Some(PlanTaxonomy.PlanKind.OpenFilePressure))
    assertEquals(tagged.fallbackTheme, Some(PlanTaxonomy.PlanTheme.PieceRedeployment))
  }

  test("probe requests do not get subplan support from broad plan aliases") {
    val broad =
      PlanClaimBoundary.PlanProposal.fromProbeRequest(
        ProbeRequest(
          id = "probe_broad_piece_activation",
          fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
          moves = List("g1f3"),
          depth = 18,
          planId = Some("PieceActivation"),
          planName = Some("Piece activation")
        )
      )
    val tagged =
      PlanClaimBoundary.PlanProposal.fromProbeRequest(
        ProbeRequest(
          id = "probe_tagged_file_pressure",
          fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
          moves = List("a1d1"),
          depth = 18,
          planName = Some("File pressure [subplan:open_file_pressure]")
        )
      )

    assertEquals(broad.theme, PlanTaxonomy.PlanTheme.PieceRedeployment)
    assertEquals(broad.themeSource, PlanClaimBoundary.PlanThemeSource.InferredProposal)
    assertEquals(broad.supportKind, None)
    assertEquals(broad.fallbackTheme, None)
    assertEquals(tagged.supportKind, Some(PlanTaxonomy.PlanKind.OpenFilePressure))
    assertEquals(tagged.fallbackTheme, Some(PlanTaxonomy.PlanTheme.PieceRedeployment))
  }

  test("plan continuity broad ids do not reopen user-facing fallback theme") {
    val proposal =
      PlanClaimBoundary.PlanProposal.fromContinuity(
        PlanContinuity(
          planName = "Piece Activation",
          planId = Some("PieceActivation"),
          consecutivePlies = 3,
          startingPly = 18
        )
      )

    assertEquals(proposal.theme, PlanTaxonomy.PlanTheme.PieceRedeployment)
    assertEquals(proposal.themeSource, PlanClaimBoundary.PlanThemeSource.InferredProposal)
    assertEquals(proposal.supportKind, None)
    assertEquals(proposal.fallbackTheme, None)
  }

  test("latent seed aliases remain proposal-only unless the seed id is an exact plan kind") {
    val seed =
      LatentSeedLibrary.all
        .find(_.id.equalsIgnoreCase("openfile_doubling"))
        .getOrElse(fail("missing openfile_doubling seed"))
    val proposal = PlanClaimBoundary.PlanProposal.fromSeed(seed)

    assertEquals(proposal.theme, PlanTaxonomy.PlanTheme.PieceRedeployment)
    assertEquals(proposal.themeSource, PlanClaimBoundary.PlanThemeSource.InferredProposal)
    assertEquals(proposal.supportKind, None)
    assertEquals(proposal.fallbackTheme, None)
  }
