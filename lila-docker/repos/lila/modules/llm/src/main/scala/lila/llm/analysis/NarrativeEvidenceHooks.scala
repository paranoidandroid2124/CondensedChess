package lila.llm.analysis

import lila.llm.model.NarrativeContext

private[analysis] object NarrativeEvidenceHooks:

  def build(ctx: NarrativeContext): Option[String] =
    authorEvidenceHook(ctx)
      .orElse(probeRequestHook(ctx))
      .orElse(topVariationHook(ctx))
      .map(UserFacingSignalSanitizer.sanitize)

  private def authorEvidenceHook(ctx: NarrativeContext): Option[String] =
    ctx.authorEvidence.flatMap(_.branches).headOption.map { branch =>
      val key = normalizeText(branch.keyMove)
      val line = normalizeText(branch.line)
      val cp = branch.evalCp.map(cp => s" (${formatCp(cp)})").getOrElse("")
      if key.nonEmpty && line.nonEmpty then s"Probe evidence starts with $key: $line$cp."
      else if line.nonEmpty then s"Probe evidence follows $line$cp."
      else s"Probe evidence starts with $key$cp."
    }

  private def probeRequestHook(ctx: NarrativeContext): Option[String] =
    ctx.probeRequests.headOption.flatMap { req =>
      val plan = req.planName.map(normalizeText).filter(_.nonEmpty)
      val objective = req.objective.map(normalizeText).filter(_.nonEmpty)
      val moves = req.moves.take(2).map(m => NarrativeUtils.uciToSanOrFormat(req.fen, m)).filter(_.trim.nonEmpty)
      val moveText = Option.when(moves.nonEmpty)(s" through ${joinNatural(moves)}").getOrElse("")
      (plan orElse objective).map(label => s"Further probe work still targets $label$moveText.")
    }

  private def topVariationHook(ctx: NarrativeContext): Option[String] =
    ctx.engineEvidence.flatMap(_.best).flatMap { line =>
      val preview = variationPreview(ctx.fen, line, limit = 3)
      preview.map(text => s"The engine line begins $text.")
    }

  private def variationPreview(fen: String, line: lila.llm.model.strategic.VariationLine, limit: Int): Option[String] =
    val tokens =
      if line.parsedMoves.nonEmpty then line.parsedMoves.take(limit).map(_.san.trim).filter(_.nonEmpty)
      else NarrativeUtils.uciListToSan(fen, line.moves.take(limit)).map(_.trim).filter(_.nonEmpty)
    Option.when(tokens.nonEmpty)(tokens.mkString(" "))

  private def joinNatural(items: List[String]): String =
    items.map(normalizeText).filter(_.nonEmpty).distinct match
      case Nil         => ""
      case one :: Nil  => one
      case a :: b :: Nil => s"$a and $b"
      case many        => s"${many.dropRight(1).mkString(", ")}, and ${many.last}"

  private def normalizeText(raw: String): String =
    Option(raw).getOrElse("").replaceAll("""[_\-]+""", " ").replaceAll("\\s+", " ").trim

  private def formatCp(cp: Int): String =
    if cp > 0 then f"+${cp.toDouble / 100}%.2f"
    else f"${cp.toDouble / 100}%.2f"
