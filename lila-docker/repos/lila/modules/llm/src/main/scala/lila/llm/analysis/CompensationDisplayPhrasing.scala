package lila.llm.analysis

private[analysis] object CompensationDisplayPhrasing:

  import StrategyPackSurface.*

  private val SquarePattern = """\b([a-h][1-8])\b""".r
  private val SanPattern = """(?:^|\s)(?:\d+\.(?:\.\.)?\s*)?(?:O-O(?:-O)?|[KQRBN]?[a-h]?[1-8]?x?[a-h][1-8](?:=[QRBN])?[+#]?)""".r
  private val FilePattern = """\b([a-h]-file)\b""".r
  private val PiecePattern = """\b(the )?(queen|rook|bishop|knight|king|pawn)s?\b""".r

  private def hasStrongCompensationAnchor(text: String): Boolean =
    val normalized = StrategyPackSurface.normalizeText(text).toLowerCase
    val hasSquare = SquarePattern.findFirstIn(normalized).nonEmpty
    val hasSan = SanPattern.findFirstIn(normalized).nonEmpty
    val hasFile = FilePattern.findFirstIn(normalized).nonEmpty
    val mentionsPiece = PiecePattern.findFirstIn(normalized).nonEmpty
    val mentionsFileOrLineAnchor =
      normalized.contains("queenside files") ||
        normalized.contains("central files") ||
        normalized.contains("open lines") ||
        normalized.contains("open files")
    val mentionsForcedDefense =
      normalized.contains("extra pawn still cannot get active") ||
        normalized.contains("extra pawn never gets to become active")
    val pressureOnConcrete =
      normalized.contains("pressure on ") || normalized.contains("pressure against ") || normalized.contains("pressure along ")
    val moveRoute = normalized.contains("head for ") || normalized.contains("bringing the ")
    hasSquare ||
    hasSan ||
    hasFile ||
    mentionsFileOrLineAnchor ||
    pressureOnConcrete ||
    (mentionsPiece && moveRoute) ||
    mentionsForcedDefense

  private def concreteCompensationWindow(surface: Snapshot): Option[String] =
    anchoredCompensationWindow(surface).filter(hasStrongCompensationAnchor)

  private def compensationIdeaSignature(text: String): String =
    StrategyPackSurface
      .normalizeText(text)
      .toLowerCase
      .replaceAll("""^the material can wait while\s+""", "")
      .replaceAll("""^winning the material back can wait because\s+""", "")
      .replaceAll("""^the point is to keep\s+""", "")
      .replaceAll("""^this works only while\s+""", "")
      .replaceAll("""^use that pressure(?: on [^.]+?)? first(?:, then think about winning the material back)?\.?$""", "use that pressure")
      .replaceAll("""^the [a-z]+ can head for\s+""", "")
      .replaceAll("""^a likely follow up is bringing the [a-z]+ to\s+""", "")
      .replaceAll("""^a likely follow up is\s+""", "")
      .replaceAll("""\s+before winning the material back\.?$""", "")
      .replaceAll("""\s+is still there\.?$""", "")
      .replaceAll("""\s+stays available\.?$""", "")
      .replaceAll("""\s+stays in view\.?$""", "")
      .replaceAll("""\s+next\.?$""", "")
      .replaceAll("""[^\w\s-]""", "")
      .replaceAll("""\s+""", " ")
      .trim

  private def sameCompensationIdea(left: String, right: String): Boolean =
    val leftSig = compensationIdeaSignature(left)
    val rightSig = compensationIdeaSignature(right)
    leftSig.nonEmpty && rightSig.nonEmpty && (
      leftSig == rightSig ||
        (leftSig.length >= 12 && leftSig.contains(rightSig)) ||
        (rightSig.length >= 12 && rightSig.contains(leftSig))
    )

  private def repeatedFollowUpAnchor(claim: String, support: String): Boolean =
    val claimLow = StrategyPackSurface.normalizeText(claim).toLowerCase
    val supportLow = StrategyPackSurface.normalizeText(support).toLowerCase
    StrategicSentenceRenderer.pieceHeadFor(supportLow).exists { case (_, square) =>
      claimLow.contains(s"head for $square") ||
      claimLow.contains(s"pressure on $square") ||
      claimLow.contains(s"pressure against $square")
    } || (
      supportLow.startsWith("a likely follow up is bringing the ") &&
        SquarePattern.findFirstMatchIn(supportLow).exists { m =>
          val square = m.group(1)
          claimLow.contains(s"head for $square") ||
          claimLow.contains(s"pressure on $square") ||
          claimLow.contains(s"pressure against $square")
        }
    )

  def dedupeCompensationSupport(claim: String, support: List[String]): List[String] =
    support.foldLeft(List.empty[String]) { (accepted, candidate) =>
      val duplicateOfClaim =
        sameCompensationIdea(claim, candidate) || repeatedFollowUpAnchor(claim, candidate)
      val duplicateOfAccepted =
        accepted.exists(existing => sameCompensationIdea(existing, candidate) || repeatedFollowUpAnchor(existing, candidate))
      if duplicateOfClaim || duplicateOfAccepted then accepted else accepted :+ candidate
    }

  def compensationNarrationEligible(surface: Snapshot): Boolean =
    if !surface.compensationPosition then false
    else
      val concreteGiven = surface.investedMaterial.exists(_ > 0)
      val concreteGained =
        List(
          concreteCompensationWindow(surface),
          surface.executionText,
          compensationExecutionTail(surface)
        ).flatten.exists(hasStrongCompensationAnchor)
      val concreteRecovery =
        List(
          compensationObjectiveText(surface),
          compensationPersistenceText(surface),
          surface.objectiveText,
          compensationSupportText(surface).find(text =>
            text.toLowerCase.contains("winning the material back") || text.toLowerCase.contains("this works only while")
          )
        ).flatten.exists(hasStrongCompensationAnchor)
      List(concreteGiven, concreteGained, concreteRecovery).count(identity) >= 2 && (concreteGained || concreteRecovery)

  def buildDisplayNormalization(
      surface: Snapshot,
      rawSubtype: CompensationSubtype,
      resolution: CompensationDisplaySubtypeResolver.DisplaySubtypeResolution
  ): DisplayNormalization =
    val normalizedSubtype = resolution.selectedDisplaySubtype.getOrElse(rawSubtype)
    val normalizedDominant =
      Option.when(resolution.normalizationActive)(normalizedDominantIdeaText(normalizedSubtype)).flatten
    val normalizedExecution =
      Option.when(resolution.normalizationActive)(normalizedExecutionText(surface, normalizedSubtype)).flatten
    val normalizedObjective =
      Option.when(resolution.normalizationActive)(normalizedObjectiveText(normalizedSubtype)).flatten
    val normalizedFocus =
      Option.when(resolution.normalizationActive)(normalizedLongTermFocusText(normalizedSubtype)).flatten
    val normalizedLead =
      Option.when(resolution.normalizationActive)(normalizedCompensationLead(surface, normalizedSubtype)).flatten
    DisplayNormalization(
      normalizedDominantIdeaText = normalizedDominant,
      normalizedExecutionText = normalizedExecution,
      normalizedObjectiveText = normalizedObjective,
      normalizedLongTermFocusText = normalizedFocus,
      normalizedCompensationLead = normalizedLead,
      normalizedCompensationSubtype = resolution.selectedDisplaySubtype,
      normalizationActive = resolution.normalizationActive,
      normalizationConfidence = resolution.normalizationConfidence,
      preparationSubtype = Option.when(resolution.preparationSubtype != rawSubtype)(resolution.preparationSubtype),
      payoffSubtype =
        Option.when(
          resolution.payoffSubtype != resolution.preparationSubtype || resolution.displaySubtypeSource == "payoff"
        )(resolution.payoffSubtype),
      selectedDisplaySubtype = resolution.selectedDisplaySubtype,
      displaySubtypeSource = resolution.displaySubtypeSource,
      payoffConfidence = resolution.payoffConfidence,
      pathConfidence = resolution.pathConfidence
    )

  def compensationPayoffText(surface: Snapshot): Option[String] =
    surface.effectiveCompensationSubtype.map {
      case CompensationSubtype("queenside", "target_fixing", _, _) =>
        "queenside pressure against fixed targets"
      case CompensationSubtype("queenside", "line_occupation", _, "durable_pressure") =>
        "durable queenside file pressure"
      case CompensationSubtype(_, "line_occupation", _, "durable_pressure") =>
        "open files and lasting pressure"
      case CompensationSubtype(_, "target_fixing", _, _) =>
        "fixed targets and lasting pressure"
      case CompensationSubtype(_, "counterplay_denial", _, _) =>
        "keeping the extra pawn from getting active"
      case CompensationSubtype("kingside", "break_preparation", _, _) =>
        "a break-driven initiative against the king"
      case CompensationSubtype("kingside", "defender_tied_down", _, _) =>
        "initiative while the defenders stay tied to the king"
      case CompensationSubtype(_, "defender_tied_down", _, _) =>
        "tying the defenders to a passive shell"
      case CompensationSubtype(_, "conversion_window", _, _) =>
        "favorable exchanges before the extra material can be recovered"
      case CompensationSubtype(theater, _, _, _) =>
        s"${StrategyPackSurface.theaterDisplay(theater)} pressure for the material".trim
    }

  def compensationPersistenceText(surface: Snapshot): Option[String] =
    surface.effectiveCompensationSubtype.map {
      case CompensationSubtype("queenside", "target_fixing", _, _) =>
        "the fixed queenside targets stay under pressure"
      case CompensationSubtype("queenside", "line_occupation", _, "durable_pressure") =>
        "the queenside files stay under pressure"
      case CompensationSubtype("center", "line_occupation", _, "durable_pressure") =>
        "the central files stay under pressure"
      case CompensationSubtype(_, "line_occupation", _, "durable_pressure") =>
        "pressure along the open lines keeps building"
      case CompensationSubtype(_, "target_fixing", _, _) =>
        "the fixed targets stay under pressure"
      case CompensationSubtype(_, "counterplay_denial", _, _) =>
        "the extra pawn never gets to become active"
      case CompensationSubtype("kingside", "break_preparation", _, _) =>
        "the break threats still have to land with force"
      case CompensationSubtype("kingside", "defender_tied_down", _, _) =>
        "the defenders keep getting dragged back to the king"
      case CompensationSubtype(_, "defender_tied_down", _, _) =>
        "the defender stays tied to passive defense"
      case CompensationSubtype(_, "conversion_window", _, _) =>
        "the favorable exchanges are still there"
      case CompensationSubtype(_, _, "intentionally_deferred", _) =>
        "the material can wait while the pressure is still there"
      case _ =>
        "the compensation remains durable"
    }

  private def anchoredCompensationWindow(surface: Snapshot): Option[String] =
    surface.compensationSummary.map(StrategyPackSurface.normalizeText).flatMap {
      case other if other.equalsIgnoreCase("initiative against the king") =>
        Some("the initiative against the king stays alive")
      case other if other.equalsIgnoreCase("attack on king") =>
        Some("the attack against the king stays alive")
      case other if other.toLowerCase.startsWith("initiative ") &&
          LiveNarrativeCompressionCore.hasConcreteAnchor(other) =>
        Some(s"${other.toLowerCase} stays alive")
      case other if other.toLowerCase.startsWith("pressure ") &&
          LiveNarrativeCompressionCore.hasConcreteAnchor(other) =>
        Some(s"${other.toLowerCase} is still there")
      case other if LiveNarrativeCompressionCore.hasConcreteAnchor(other) =>
        Some(s"${other.toLowerCase} stays available")
      case _ =>
        None
    }.orElse(
      surface.objectiveText.map(StrategyPackSurface.normalizeText).flatMap { other =>
        StrategicSentenceRenderer.pieceHeadFor(other)
          .map { case (piece, square) => s"the $piece can still head for $square" }
          .orElse {
            if other.toLowerCase.startsWith("pressure ") && LiveNarrativeCompressionCore.hasConcreteAnchor(other) then
              Some(s"${other.toLowerCase} is still there")
            else if LiveNarrativeCompressionCore.hasConcreteAnchor(other) then
              Some(s"${other.toLowerCase} stays in view")
            else None
          }
      }
    ).orElse(
      surface.executionText.map(StrategyPackSurface.normalizeText).flatMap { other =>
        StrategicSentenceRenderer.pieceToward(other)
          .map { case (piece, square) => s"the $piece can still head for $square" }
          .orElse {
            StrategicSentenceRenderer.pieceViaPath(other).flatMap { case (piece, path) =>
              path.split("-").lastOption.filter(_.nonEmpty).map(square => s"the $piece can still head for $square")
            }
          }
          .orElse {
            Option.when(LiveNarrativeCompressionCore.hasConcreteAnchor(other))(s"${other.toLowerCase} stays available")
          }
      }
    )

  private def compensationClaimFromWindow(window: String): String =
    s"The move gives up material because $window."

  private def compensationConditionFromWindow(window: String): String =
    s"That only works while $window."

  def compensationWhyNowText(surface: Snapshot): Option[String] =
    Option.when(surface.compensationPosition) {
      if !surface.normalizationActive then
        val attackLed =
          surface.preferRawAttackDisplay ||
            surface.rawDominantIdeaText.exists(_.toLowerCase.contains("king attack")) ||
            surface.compensationSummary.exists(_.toLowerCase.contains("initiative")) ||
            surface.compensationVectors.exists(_.toLowerCase.contains("initiative"))
        if attackLed then
          concreteCompensationWindow(surface).orElse(anchoredCompensationWindow(surface))
            .map(compensationClaimFromWindow)
            .getOrElse("The move gives up material to keep the attack against the king alive.")
        else
          concreteCompensationWindow(surface).orElse(anchoredCompensationWindow(surface))
            .map(compensationClaimFromWindow)
            .getOrElse("The move gives up material to keep the initiative against the king alive.")
      else
        surface.effectiveCompensationSubtype match
          case Some(CompensationSubtype("queenside", "target_fixing", _, _)) =>
            concreteCompensationWindow(surface)
              .map(compensationClaimFromWindow)
              .getOrElse("The move gives up material to keep the queenside targets tied down.")
          case Some(CompensationSubtype("center", "target_fixing", _, _)) =>
            concreteCompensationWindow(surface)
              .map(compensationClaimFromWindow)
              .getOrElse("The move gives up material to keep the central targets under pressure.")
          case Some(CompensationSubtype("queenside", "line_occupation", _, "durable_pressure")) =>
            concreteCompensationWindow(surface)
              .map(compensationClaimFromWindow)
              .getOrElse("The move gives up material to keep the queenside files active.")
          case Some(CompensationSubtype("center", "line_occupation", _, "durable_pressure")) =>
            concreteCompensationWindow(surface)
              .map(compensationClaimFromWindow)
              .getOrElse("The move gives up material to keep the central files active.")
          case Some(CompensationSubtype(_, "line_occupation", _, "durable_pressure")) =>
            concreteCompensationWindow(surface)
              .map(compensationClaimFromWindow)
              .getOrElse("The move gives up material to keep the open lines active.")
          case Some(CompensationSubtype(_, "counterplay_denial", _, _)) =>
            "The move gives up material to keep the extra pawn quiet."
          case Some(CompensationSubtype("kingside", "break_preparation", _, _)) =>
            "The move gives up material to keep the break ready."
          case Some(CompensationSubtype("kingside", "defender_tied_down", _, _)) =>
            "The move gives up material to keep the defenders tied to the king."
          case Some(CompensationSubtype(_, "conversion_window", _, _)) =>
            "The point is to force the favorable exchanges before winning the material back."
          case Some(_) =>
            anchoredCompensationWindow(surface)
              .map(compensationClaimFromWindow)
              .getOrElse("The move gives up material to keep the initiative against the king alive.")
          case None =>
            anchoredCompensationWindow(surface)
              .map(compensationClaimFromWindow)
              .getOrElse("The move gives up material to keep the initiative against the king alive.")
    }

  def compensationObjectiveText(surface: Snapshot): Option[String] =
    Option.when(surface.compensationPosition) {
      surface.effectiveCompensationSubtype match
        case Some(CompensationSubtype("queenside", "target_fixing", _, _)) =>
          concreteCompensationWindow(surface)
            .map(compensationConditionFromWindow)
            .getOrElse("That only works while the queenside targets stay tied down.")
        case Some(CompensationSubtype("center", "target_fixing", _, _)) =>
          concreteCompensationWindow(surface)
            .map(compensationConditionFromWindow)
            .getOrElse("That only works while the central targets stay under pressure.")
        case Some(CompensationSubtype(_, "line_occupation", _, "durable_pressure")) =>
          concreteCompensationWindow(surface)
            .map(compensationConditionFromWindow)
            .getOrElse("That only works while the open lines stay active.")
        case Some(CompensationSubtype(_, "counterplay_denial", _, _)) =>
          "That only works while the extra pawn still cannot get active."
        case Some(CompensationSubtype("kingside", "break_preparation", _, _)) =>
          "This works only while the break still has to be respected."
        case Some(CompensationSubtype(_, "conversion_window", _, _)) =>
          "The point is to force the favorable exchanges before winning the material back."
        case Some(_) =>
          concreteCompensationWindow(surface).orElse(anchoredCompensationWindow(surface))
            .map(compensationConditionFromWindow)
            .getOrElse("That only works while the initiative against the king is still there.")
        case None =>
          surface.objectiveText.map { obj =>
            StrategicSentenceRenderer.pieceHeadFor(obj)
              .map { case (piece, square) => s"That only works while the $piece can head for $square." }
              .getOrElse {
                val other = StrategyPackSurface.normalizeText(obj)
                if other.toLowerCase.startsWith("pressure ") then
                  s"That only works while $other is still there."
                else s"The point is to keep $other in play before winning the material back."
              }
          }.orElse(
            concreteCompensationWindow(surface).orElse(anchoredCompensationWindow(surface))
              .map(compensationConditionFromWindow)
          ).getOrElse("That only works while the initiative against the king is still there.")
    }

  def compensationExecutionTail(surface: Snapshot): Option[String] =
    compensationExecutionCue(surface).flatMap(StrategicSentenceRenderer.renderCompensationFollowUpFromExecution)

  def compensationSupportText(surface: Snapshot): List[String] =
    surface.effectiveCompensationSubtype.toList.flatMap {
      case CompensationSubtype("queenside", "target_fixing", "intentionally_deferred", _) =>
        List(
          "Keep the queenside targets tied down before thinking about the material."
        )
      case CompensationSubtype("queenside", "line_occupation", "intentionally_deferred", "durable_pressure") =>
        List(
          "The rooks can take over the queenside files next."
        )
      case CompensationSubtype("center", "line_occupation", "intentionally_deferred", "durable_pressure") =>
        List(
          "The pieces can take over the central files next."
        )
      case CompensationSubtype(_, "line_occupation", "intentionally_deferred", "durable_pressure") =>
        List(
          "Those open lines have to stay active for your pieces.",
          "If those lines dry up, the compensation dries up with them."
        )
      case CompensationSubtype(_, "counterplay_denial", _, _) =>
        List(
          "The extra pawn matters much less if it never gets to advance.",
          "This works only while the defender stays tied down."
        )
      case CompensationSubtype("kingside", "break_preparation", _, _) =>
        List(
          "The pawn only makes sense if the break is still ready to hit with tempo."
        )
      case CompensationSubtype(_, "conversion_window", _, _) =>
        List(
          "The compensation has to lead to favorable trades before the material comes back."
        )
      case CompensationSubtype(_, "defender_tied_down", _, _) =>
        List(
          "This works only while the defenders are still stuck protecting the king."
        )
      case _ =>
        Nil
    }.distinct

  private def normalizedDominantIdeaText(subtype: CompensationSubtype): Option[String] =
    subtype match
      case CompensationSubtype("queenside", "target_fixing", _, _) => Some("fixed queenside targets")
      case CompensationSubtype("queenside", "line_occupation", _, _) => Some("queenside file pressure")
      case CompensationSubtype("center", "target_fixing", _, _)     => Some("fixed central targets")
      case CompensationSubtype("center", "line_occupation", _, _)   => Some("central file pressure")
      case CompensationSubtype(_, "target_fixing", _, _)            => Some("fixed targets")
      case CompensationSubtype(_, "counterplay_denial", _, _)       => Some("counterplay denial")
      case CompensationSubtype(_, "defender_tied_down", _, _)       => Some("tying the defenders down")
      case CompensationSubtype(_, "line_occupation", _, _)          => Some("durable pressure along the files")
      case _                                                        => None

  private def normalizedExecutionText(
      surface: Snapshot,
      subtype: CompensationSubtype
  ): Option[String] =
    val subtypeRoute = StrategyPackSurface.alignedRoute(surface, subtype)
    val subtypeMoveRef = StrategyPackSurface.alignedMoveRef(surface, subtype)
    val subtypeTarget = StrategyPackSurface.alignedDirectionalTarget(surface, subtype)
    val fallback = compensationExecutionCue(surface)
    subtype match
      case CompensationSubtype("queenside", "target_fixing", _, _) =>
        subtypeMoveRef.map(ref => s"${StrategyPackSurface.pieceName(ref.piece)} toward ${StrategyPackSurface.normalizeText(ref.target).toLowerCase} to lean on the fixed queenside targets")
          .orElse(subtypeRoute.map(route => s"${StrategyPackSurface.pieceName(route.piece)} toward ${StrategyPackSurface.normalizeText(route.route.lastOption.getOrElse("c4")).toLowerCase} to lean on the fixed queenside targets"))
          .orElse(subtypeTarget.map(target => s"lean on the fixed queenside targets via ${StrategyPackSurface.normalizeText(target.targetSquare).toLowerCase}"))
          .orElse(fallback.map(execution => s"$execution to lean on the fixed queenside targets"))
      case CompensationSubtype("queenside", "line_occupation", _, _) =>
        subtypeRoute.map(route => s"${StrategyPackSurface.pieceName(route.piece)} toward ${StrategyPackSurface.normalizeText(route.route.lastOption.getOrElse("a1")).toLowerCase} to work on the queenside files")
          .orElse(fallback.map(execution => s"$execution to work on the queenside files"))
      case CompensationSubtype("center", "target_fixing", _, _) =>
        subtypeMoveRef.map(ref => s"${StrategyPackSurface.pieceName(ref.piece)} toward ${StrategyPackSurface.normalizeText(ref.target).toLowerCase} to lean on the fixed central targets")
          .orElse(subtypeTarget.map(target => s"lean on the fixed central targets via ${StrategyPackSurface.normalizeText(target.targetSquare).toLowerCase}"))
          .orElse(fallback.map(execution => s"$execution to keep the central targets fixed"))
      case CompensationSubtype("center", "line_occupation", _, _) =>
        subtypeRoute.map(route => s"${StrategyPackSurface.pieceName(route.piece)} toward ${StrategyPackSurface.normalizeText(route.route.lastOption.getOrElse("d4")).toLowerCase} to work on the central files")
          .orElse(fallback.map(execution => s"$execution to work on the central files"))
      case CompensationSubtype(_, "counterplay_denial", _, _) =>
        fallback.map(execution => s"$execution to stop the counterplay from getting started")
      case CompensationSubtype(_, "defender_tied_down", _, _) =>
        fallback.map(execution => s"$execution to keep the defenders tied down")
      case CompensationSubtype(_, "line_occupation", _, _) =>
        fallback.map(execution => s"$execution to keep the play on the open files")
      case CompensationSubtype(_, "target_fixing", _, _) =>
        fallback.map(execution => s"$execution to keep the targets tied down")
      case _ =>
        fallback

  private def normalizedObjectiveText(subtype: CompensationSubtype): Option[String] =
    subtype match
      case CompensationSubtype("queenside", "target_fixing", _, _) =>
        Some("queenside targets tied down before winning the material back")
      case CompensationSubtype("queenside", "line_occupation", _, _) =>
        Some("queenside file pressure before winning the material back")
      case CompensationSubtype("center", "target_fixing", _, _) =>
        Some("central targets tied down before winning the material back")
      case CompensationSubtype("center", "line_occupation", _, _) =>
        Some("central file pressure before winning the material back")
      case CompensationSubtype(_, "counterplay_denial", _, _) =>
        Some("stopping counterplay before winning the material back")
      case CompensationSubtype(_, "defender_tied_down", _, _) =>
        Some("keeping the defenders tied down while the material can wait")
      case CompensationSubtype(_, "line_occupation", _, _) =>
        Some("pressure along the files before winning the material back")
      case CompensationSubtype(_, "target_fixing", _, _) =>
        Some("fixed targets under pressure before winning the material back")
      case _ =>
        None

  private def normalizedLongTermFocusText(subtype: CompensationSubtype): Option[String] =
    normalizedObjectiveText(subtype)

  private def normalizedCompensationLead(
      surface: Snapshot,
      subtype: CompensationSubtype
  ): Option[String] =
    subtype match
      case CompensationSubtype("queenside", "target_fixing", _, _) =>
        Some("queenside pressure against fixed targets")
      case CompensationSubtype("queenside", "line_occupation", _, _) =>
        Some("durable queenside file pressure")
      case CompensationSubtype("center", "target_fixing", _, _) =>
        Some("central pressure against fixed targets")
      case CompensationSubtype("center", "line_occupation", _, _) =>
        Some("central file pressure while the material can wait")
      case CompensationSubtype(_, "counterplay_denial", _, _) =>
        Some("stopping counterplay while the material can wait")
      case CompensationSubtype(_, "defender_tied_down", _, _) =>
        Some("keeping the defenders tied down")
      case CompensationSubtype(_, "line_occupation", _, _) =>
        Some("lasting pressure along the files while the material can wait")
      case CompensationSubtype(_, "target_fixing", _, _) =>
        Some("fixed targets and lasting pressure")
      case _ =>
        compensationPayoffText(surface)

  private[analysis] def compensationExecutionCue(surface: Snapshot): Option[String] =
    val conciseRoute =
      surface.topRoute.flatMap { route =>
        route.route.lastOption.map(StrategyPackSurface.normalizeText).filter(_.nonEmpty).map { square =>
          s"${StrategyPackSurface.pieceName(route.piece)} toward ${square.toLowerCase}"
        }
      }
    val conciseMoveRef =
      surface.topMoveRef.map(ref => s"${StrategyPackSurface.pieceName(ref.piece)} toward ${StrategyPackSurface.normalizeText(ref.target).toLowerCase}")
        .filterNot(_.endsWith(" toward "))
    val conciseTarget =
      surface.topDirectionalTarget.map(target =>
        s"${StrategyPackSurface.pieceName(target.piece)} toward ${StrategyPackSurface.normalizeText(target.targetSquare).toLowerCase}"
      ).filterNot(_.endsWith(" toward "))
    val genericExecution =
      surface.rawExecutionText
        .map(_.toLowerCase)
        .map(text => text.replaceFirst("\\s+for\\s+.*$", "").trim)
        .filter(_.nonEmpty)
    surface.effectiveCompensationSubtype match
      case Some(CompensationSubtype(theater, mode, _, _))
          if Set("queenside", "center").contains(theater) || Set("line_occupation", "target_fixing", "counterplay_denial").contains(mode) =>
        conciseRoute.orElse(conciseMoveRef).orElse(conciseTarget).orElse(genericExecution)
      case _ =>
        surface.rawExecutionText.map(_.toLowerCase).filter(_.nonEmpty)
