package lila.commentary.certification

import chess.{ Color, Square }

import lila.commentary.certification.CertificationHelpers.*
import lila.commentary.witness.{ Witness, WitnessAnchor, WitnessValue, WitnessPayload }

private[certification] object SpaceBindRestrictionCertificationRule extends CertificationRule:

  val familyId: CertificationId = CertificationId("SpaceBindRestrictionCertification")
  val scope: CertificationScope = CertificationScope.CurrentPosition
  val burdenTag: CertificationBurdenTag = CertificationBurdenTag("space_bind_persistence")
  protected val helperTags: Vector[String] =
    Vector("structural_space_host", "same_anchor_restriction_route", "best_defense_survival")
  protected val requiredEvidencePurposes: Vector[CertificationEvidencePurpose] =
    Vector(CertificationEvidencePurpose.BestDefenseSurvival, CertificationEvidencePurpose.TacticalReleaseDetection)
  protected val insufficientEvidenceVerdict: CertificationVerdict =
    CertificationVerdict.SupportOnly

  private final case class StructuralHost(
      witness: Witness,
      hostId: String,
      sector: String,
      claimedSquares: Set[Square]
  )

  private final case class RouteAnchor(
      witness: Witness,
      route: String,
      anchor: Square
  )

  private final case class RouteHostLink(
      host: StructuralHost,
      route: RouteAnchor
  )

  def candidateFor(
      color: Color,
      context: CertificationContext,
      extractedSoFar: CertificationSet
  ): Option[CertificationCandidate] =
    val structuralHosts =
      context.current
        .attachedWitnessesFor("structural_space_claim")
        .filter(_.color.contains(color))
        .flatMap: witness =>
          for
            hostId <- context.current.token(witness.payload, "host_id")
            sector <- witness.anchor match
              case WitnessAnchor.SectorAnchor(sector) => Some(sector.key)
              case _                                  => None
          yield StructuralHost(
            witness,
            hostId,
            sector,
            squareList(witness.payload, "claimed_squares").toSet
          )
    val outposts =
      context.current
        .primaryWitnessesFor("knight_on_outpost_square")
        .filter(_.color.contains(color))
    val restrictions =
      context.current
        .primaryWitnessesFor("short_run_slider_gate_restriction")
        .filter(_.color.contains(color))

    val routeAnchors =
      outposts
        .flatMap(witness => pieceAnchorSquare(witness).map(RouteAnchor(witness, "outpost_anchor", _))) ++
        restrictions.flatMap(witness =>
          pieceAnchorSquare(witness).map(RouteAnchor(witness, "non_outpost_space_bind", _))
        )
    val routeHostLinks =
      structuralHosts.flatMap: host =>
        routeAnchors.filter(routeBelongsToHost(_, host)).map(RouteHostLink(host, _))

    Option.when(routeHostLinks.nonEmpty):
      val linkedHosts = routeHostLinks.map(_.host).distinct
      val linkedRoutes = routeHostLinks.map(_.route).distinct
      val hostIds =
        linkedHosts.map(_.hostId).distinct.sorted
      val sectors = linkedHosts.map(_.sector).distinct.sorted
      val linkedRouteAnchors = linkedRoutes.map(_.anchor).distinct.sortBy(_.value)
      val outpostSquares =
        linkedRoutes.filter(_.route == "outpost_anchor").map(_.anchor).distinct.sortBy(_.value)
      val restrictionAnchors =
        linkedRoutes.filter(_.route == "non_outpost_space_bind").map(_.anchor).distinct.sortBy(_.value)
      CertificationCandidate(
        payload = WitnessPayload.from(
          Vector(
            "structural_host_ids" -> WitnessValue.TokenListValue(hostIds),
            "structural_sectors" -> WitnessValue.TokenListValue(sectors),
            "route_anchor_squares" -> WitnessValue.SquareListValue(linkedRouteAnchors),
            "outpost_squares" -> WitnessValue.SquareListValue(outpostSquares),
            "restriction_anchor_squares" -> WitnessValue.SquareListValue(restrictionAnchors),
            "route_host_links" -> WitnessValue.TokenListValue(
              routeHostLinks.map(link =>
                s"${link.route.anchor.key}|${link.route.route}|${link.host.sector}|${link.host.hostId}"
              ).distinct.sorted
            )
          )
        ),
        support = support(
          indices = linkedHosts.flatMap(_.witness.support.rootIndices) ++
            linkedRoutes.flatMap(_.witness.support.rootIndices),
          targetSquares = linkedHosts.flatMap(_.witness.support.targetSquares) ++ linkedRouteAnchors
        )
      )

  private def routeBelongsToHost(route: RouteAnchor, host: StructuralHost): Boolean =
    route.route match
      case "outpost_anchor" =>
        host.claimedSquares.contains(route.anchor) ||
          route.anchor.knightAttacks.exists(host.claimedSquares.contains)
      case "non_outpost_space_bind" =>
        route.witness.support.targetSquares.exists(square =>
          host.claimedSquares.contains(square) ||
            square.kingAttacks.exists(host.claimedSquares.contains)
        )
      case _ => false

  private def pieceAnchorSquare(witness: Witness): Option[chess.Square] =
    witness.anchor match
      case WitnessAnchor.PieceSquareAnchor(square) => Some(square)
      case _                                      => None

  private def squareList(payload: WitnessPayload, field: String): Vector[Square] =
    payload.get(field).collect { case WitnessValue.SquareListValue(values) => values }.getOrElse(Vector.empty)
