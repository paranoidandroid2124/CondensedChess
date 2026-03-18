package lila.llm.analysis

import lila.llm.model.{ CompensationInfo, ExtendedAnalysisData, NarrativeContext }
import lila.llm.model.strategic.Compensation

private[llm] object CompensationInterpretation:

  enum Source:
    case CurrentSemantic
    case AfterSemantic
    case DerivedCarrier

  final case class Signal(
      source: Source,
      summary: Option[String],
      vectors: List[String],
      investedMaterial: Option[Int]
  )

  final case class Decision(
      signal: Signal,
      accepted: Boolean,
      rejectionReason: Option[String],
      recaptureNeutralized: Boolean,
      thinReturnVectorOnly: Boolean,
      lateTechnicalConversionTail: Boolean,
      durableStructuralPressure: Boolean,
      persistenceClass: String
  )

  final case class SemanticDecision(
      source: Source,
      compensation: CompensationInfo,
      decision: Decision
  )

  final case class RawDecision(
      source: Source,
      compensation: Compensation,
      decision: Decision
  )

  def currentRawDecision(data: ExtendedAnalysisData): Option[RawDecision] =
    data.compensation.map { compensation =>
      RawDecision(
        source = Source.CurrentSemantic,
        compensation = compensation,
        decision =
          evaluate(
            signal = fromCompensation(compensation, Source.CurrentSemantic),
            phase = normalizePhase(data.phase),
            fenBefore = data.fen,
            playedMove = data.prevMove
          )
      )
    }

  def afterRawDecision(
      currentData: ExtendedAnalysisData,
      afterAnalysis: ExtendedAnalysisData
  ): Option[RawDecision] =
    currentData.prevMove.flatMap { playedMove =>
      afterAnalysis.compensation.map { compensation =>
        RawDecision(
          source = Source.AfterSemantic,
          compensation = compensation,
          decision =
            evaluate(
              signal = fromCompensation(compensation, Source.AfterSemantic),
              phase = normalizePhase(afterAnalysis.phase),
              fenBefore = currentData.fen,
              playedMove = Some(playedMove)
            )
        )
      }
    }

  def currentSemanticDecision(ctx: NarrativeContext): Option[SemanticDecision] =
    ctx.semantic.flatMap(_.compensation).map { compensation =>
      SemanticDecision(
        source = Source.CurrentSemantic,
        compensation = compensation,
        decision =
          evaluate(
            signal = fromCompensationInfo(compensation, Source.CurrentSemantic),
            phase = normalizePhase(ctx.phase.current),
            fenBefore = ctx.fen,
            playedMove = ctx.playedMove
          )
      )
    }

  def afterSemanticDecision(ctx: NarrativeContext): Option[SemanticDecision] =
    ctx.semantic.flatMap(_.afterCompensation).map { compensation =>
      SemanticDecision(
        source = Source.AfterSemantic,
        compensation = compensation,
        decision =
          evaluate(
            signal = fromCompensationInfo(compensation, Source.AfterSemantic),
            phase = normalizePhase(ctx.phase.current),
            fenBefore = ctx.fen,
            playedMove = ctx.playedMove
          )
      )
    }

  def effectiveSemanticDecision(ctx: NarrativeContext): Option[SemanticDecision] =
    currentSemanticDecision(ctx).filter(_.decision.accepted)
      .orElse(afterSemanticDecision(ctx).filter(_.decision.accepted))

  def derivedDecision(
      summary: Option[String],
      vectors: List[String],
      investedMaterial: Option[Int],
      phase: String,
      fenBefore: String,
      playedMove: Option[String]
  ): Option[Decision] =
    Option.when(
      summary.exists(_.trim.nonEmpty) ||
        vectors.exists(_.trim.nonEmpty) ||
        investedMaterial.exists(_ > 0)
    )(
      evaluate(
        signal =
          Signal(
            source = Source.DerivedCarrier,
            summary = normalizeOptional(summary),
            vectors = vectors.map(normalizeVectorDisplayText).filter(_.nonEmpty).distinct,
            investedMaterial = investedMaterial.filter(_ > 0)
          ),
        phase = normalizePhase(phase),
        fenBefore = fenBefore,
        playedMove = playedMove
      )
    )

  def surfaceDecision(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot
  ): Option[Decision] =
    derivedDecision(
      summary = surface.compensationSummary,
      vectors = surface.compensationVectors,
      investedMaterial = surface.investedMaterial,
      phase = ctx.phase.current,
      fenBefore = ctx.fen,
      playedMove = ctx.playedMove
    )

  def effectiveConsumerDecision(
      ctx: NarrativeContext,
      surface: StrategyPackSurface.Snapshot
  ): Option[Decision] =
    effectiveSemanticDecision(ctx).map(_.decision)
      .orElse(surfaceDecision(ctx, surface).filter(_.accepted))

  private def fromCompensation(compensation: Compensation, source: Source): Signal =
    Signal(
      source = source,
      summary = normalizeOptional(Option(compensation.conversionPlan)),
      vectors =
        compensation.returnVector.toList
          .sortBy { case (_, weight) => -weight }
          .map { case (label, _) => normalizeVectorDisplayText(label) }
          .filter(_.nonEmpty),
      investedMaterial = Option.when(compensation.investedMaterial > 0)(compensation.investedMaterial)
    )

  private def fromCompensationInfo(compensation: CompensationInfo, source: Source): Signal =
    Signal(
      source = source,
      summary = normalizeOptional(Option(compensation.conversionPlan)),
      vectors =
        compensation.returnVector.toList
          .sortBy { case (_, weight) => -weight }
          .map { case (label, _) => normalizeVectorDisplayText(label) }
          .filter(_.nonEmpty),
      investedMaterial = Option.when(compensation.investedMaterial > 0)(compensation.investedMaterial)
    )

  private def evaluate(
      signal: Signal,
      phase: String,
      fenBefore: String,
      playedMove: Option[String]
  ): Decision =
    val text = (signal.summary.toList ++ signal.vectors).map(normalizeLexicalText).mkString(" ")
    val invested = signal.investedMaterial.getOrElse(0)
    val hasInitiative = containsAny(text, List("initiative", "initiative alive", "initiative against"))
    val hasAttackCarrier =
      containsAny(
        text,
        List("attack on king", "attack", "mating attack", "king attack", "mating net", "king pressure")
      )
    val hasTimeCarrier =
      containsAny(text, List("time", "development lead", "lead in development", "tempo"))
    val hasLinePressure =
      containsAny(
        text,
        List(
          "line pressure",
          "open line",
          "open-line",
          "open file",
          "open-file",
          "file pressure",
          "line occupation",
          "open lines",
          "queenside file pressure",
          "central file pressure"
        )
      )
    val hasDelayedRecovery =
      containsAny(
        text,
        List(
          "delayed recovery",
          "delay recovery",
          "deferred",
          "recovering material later",
          "before recovering the material",
          "keep the material invested"
        )
      )
    val hasTargetFixing =
      containsAny(
        text,
        List(
          "target fixing",
          "target fixation",
          "fixed targets",
          "fixed target",
          "target pressure",
          "queenside targets"
        )
      )
    val hasCounterplayDenial =
      containsAny(
        text,
        List(
          "counterplay denial",
          "deny counterplay",
          "counterplay remains tied down",
          "counterplay tied down",
          "the extra pawn cannot breathe",
          "keeping the extra pawn tied down"
        )
      )
    val hasReturnVector = containsAny(text, List("return vector"))
    val hasTransitionWindow =
      containsAny(
        text,
        List("cash out", "clean transition", "conversion window", "transition window", "convert the compensation")
      )
    val durableStructuralPressure = hasLinePressure || hasDelayedRecovery || hasTargetFixing || hasCounterplayDenial
    val recaptureNeutralized =
      signal.investedMaterial.exists(_ > 0) &&
        playedMove.exists(move =>
          CompensationRecaptureGate.suppressAfterCompensation(fenBefore, move, signal.investedMaterial.getOrElse(0))
        )
    val thinReturnVectorOnly =
      phase != "opening" &&
        hasReturnVector &&
        !hasTransitionWindow &&
        !hasInitiative &&
        !hasAttackCarrier &&
        !hasTimeCarrier &&
        !durableStructuralPressure
    val lateTechnicalConversionTail =
      phase == "endgame" &&
        invested >= 500 &&
        !durableStructuralPressure &&
        (!hasInitiative && !hasAttackCarrier && !hasTimeCarrier || hasTransitionWindow)
    val persistenceClass =
      if durableStructuralPressure then "durable_pressure"
      else if hasInitiative || hasAttackCarrier || hasTimeCarrier then "non_immediate_transition"
      else if hasTransitionWindow || hasReturnVector then "transition_only"
      else "tactical_window"
    val hasAcceptedCarrier = hasInitiative || hasAttackCarrier || hasTimeCarrier || durableStructuralPressure
    val rejectionReason =
      if recaptureNeutralized then Some("recapture_neutralized")
      else if thinReturnVectorOnly then Some("thin_return_vector_only")
      else if lateTechnicalConversionTail then Some("late_technical_conversion_tail")
      else if invested <= 0 then Some("missing_material_investment")
      else if !hasAcceptedCarrier then Some("missing_structural_carrier")
      else None

    Decision(
      signal = signal,
      accepted = rejectionReason.isEmpty,
      rejectionReason = rejectionReason,
      recaptureNeutralized = recaptureNeutralized,
      thinReturnVectorOnly = thinReturnVectorOnly,
      lateTechnicalConversionTail = lateTechnicalConversionTail,
      durableStructuralPressure = durableStructuralPressure,
      persistenceClass = persistenceClass
    )

  private def normalizeOptional(value: Option[String]): Option[String] =
    value.map(normalizeDisplayText).filter(_.nonEmpty)

  private def normalizeDisplayText(value: String): String =
    Option(value).map(_.trim).getOrElse("").replaceAll("\\s+", " ").trim

  private def normalizeVectorDisplayText(value: String): String =
    normalizeDisplayText(value)
      .replace('_', ' ')
      .toLowerCase
      .replaceAll("\\s+", " ")
      .trim

  private def normalizeLexicalText(value: String): String =
    normalizeVectorDisplayText(value)
      .replaceAll("\\([^)]*\\)", " ")
      .replace('-', ' ')
      .toLowerCase
      .replaceAll("\\s+", " ")
      .trim

  private def normalizePhase(value: String): String =
    Option(value).map(_.trim.toLowerCase).getOrElse("")

  private def containsAny(text: String, phrases: List[String]): Boolean =
    phrases.exists(text.contains)
