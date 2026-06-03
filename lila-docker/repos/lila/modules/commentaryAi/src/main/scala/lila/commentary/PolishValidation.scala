package lila.commentary

private[commentary] object PolishValidation:

  case class ValidationResult(isValid: Boolean, reasons: List[String])

  private case class MoveMarker(number: Int, style: String):
    inline def token: String = s"$number$style"

  private val sanTokenPattern =
    """(?:O-O(?:-O)?|[KQRBNkqrbn]?[a-h]?[1-8]?x?[a-h][1-8](?:=[QRBNqrbn])?)"""
  private val moveTokenRegex =
    raw"""(?<![A-Za-z0-9])($sanTokenPattern(?:[+#])?(?:[!?]{1,2})?)""".r
  private val moveMarkerRegex =
    raw"""(?<![A-Za-z0-9])(\d+)(\.\.\.|\.|)(?=\s*$sanTokenPattern(?:[+#])?(?:[!?]{1,2})?)""".r
  private val evalTokenRegex =
    """(?i)\(\s*(?:[+-]?\d+(?:\.\d+)?|#?[+-]?\d+|mate\s+in\s+\d+|mated\s+in\s+\d+)\s*\)""".r
  private val variationBranchRegex =
    """(?m)^\s*([a-z]\))""".r
  private val promotionRoleRegex = """=([qrbn])""".r
  private val numericEvalRegex = """^([+-]?)(\d+(?:\.\d+)?)$""".r

  private def canonicalSanToken(token: String): String =
    val stripped = Option(token).getOrElse("").trim
      .replaceAll("""[!?]+$""", "")
      .replaceAll("""[+#]+$""", "")
    val promotionFixed =
      promotionRoleRegex.replaceAllIn(stripped, m => s"=${m.group(1).toUpperCase}")
    if promotionFixed.startsWith("O-O") then promotionFixed.toUpperCase
    else
      promotionFixed.headOption match
        case Some(ch) if "kqrbn".contains(ch) => ch.toUpper.toString + promotionFixed.drop(1)
        case _                               => promotionFixed

  private def canonicalEvalToken(token: String): String =
    val inner = Option(token).getOrElse("").trim
      .stripPrefix("(")
      .stripSuffix(")")
      .trim
      .toLowerCase
      .replaceAll("""\s+""", " ")
    inner match
      case numericEvalRegex(sign, number) =>
        val normalized = BigDecimal(number).bigDecimal.stripTrailingZeros.toPlainString
        s"$sign$normalized"
      case _ =>
        inner.replaceAll("""\s+""", "")

  private def extractMoveTokensOrdered(text: String): List[String] =
    val t = Option(text).getOrElse("")
    moveTokenRegex
      .findAllMatchIn(t)
      .map(m => canonicalSanToken(m.group(1)))
      .filter(_.nonEmpty)
      .toList

  private def extractMoveMarkersOrdered(text: String): List[MoveMarker] =
    val t = Option(text).getOrElse("")
    moveMarkerRegex
      .findAllMatchIn(t)
      .flatMap { m =>
        m.group(1).toIntOption.map(n => MoveMarker(number = n, style = m.group(2)))
      }
      .toList

  private def extractEvalTokensOrdered(text: String): List[String] =
    val t = Option(text).getOrElse("")
    evalTokenRegex.findAllMatchIn(t).map(m => canonicalEvalToken(m.group(0))).toList

  private def extractVariationBranchesOrdered(text: String): List[String] =
    val t = Option(text).getOrElse("")
    variationBranchRegex.findAllMatchIn(t).map(_.group(1).toLowerCase).toList

  private def isSubsequence(xs: List[String], ys: List[String]): Boolean =
    if xs.isEmpty then true
    else
      @annotation.tailrec
      def loop(restXs: List[String], restYs: List[String]): Boolean =
        (restXs, restYs) match
          case (Nil, _) => true
          case (_, Nil) => false
          case (xh :: xt, yh :: yt) =>
            if xh == yh then loop(xt, yt) else loop(restXs, yt)
      loop(xs, ys)

  private def tokenCounts(xs: List[String]): Map[String, Int] =
    xs.groupMapReduce(identity)(_ => 1)(_ + _)

  private def firstOccurrences(xs: List[String]): List[String] =
    xs.foldLeft(List.empty[String]) { (acc, token) =>
      if acc.contains(token) then acc else acc :+ token
    }

  def validatePolishedCommentary(
      polished: String,
      original: String,
      allowedSans: List[String]
  ): ValidationResult =
    val polishedMoves = extractMoveTokensOrdered(polished)
    val originalMoves = extractMoveTokensOrdered(original)
    val originalMarkers = extractMoveMarkersOrdered(original)
    val polishedMarkers = extractMoveMarkersOrdered(polished)
    val allowedMoves = originalMoves ++ allowedSans.map(canonicalSanToken).filter(_.nonEmpty)
    val allowedCount = tokenCounts(allowedMoves)
    val polishedCount = tokenCounts(polishedMoves)
    val originalHasMoves = originalMoves.nonEmpty

    val withinCountBudget = polishedCount.forall { case (san, count) =>
      count <= allowedCount.getOrElse(san, 0)
    }
    val sanPresenceSatisfied = !originalHasMoves || polishedMoves.nonEmpty
    val preservesOriginalSequence =
      isSubsequence(firstOccurrences(originalMoves), firstOccurrences(polishedMoves))
    val respectsAllowedOrder = isSubsequence(polishedMoves, allowedMoves)
    val numberingRequired = originalMarkers.nonEmpty && originalHasMoves
    val numberingSatisfied = !numberingRequired || polishedMarkers.nonEmpty
    val originalNumberSet = originalMarkers.map(_.number).toSet
    val polishedMarkersOnOriginalNumbers = polishedMarkers.filter(m => originalNumberSet.contains(m.number))
    val originalMarkerTokens = originalMarkers.map(_.token)
    val polishedMarkerTokensOnOriginalNumbers = polishedMarkersOnOriginalNumbers.map(_.token)
    val markerStyleOrderSatisfied =
      polishedMarkerTokensOnOriginalNumbers.isEmpty ||
        isSubsequence(polishedMarkerTokensOnOriginalNumbers, originalMarkerTokens)

    val originalEvalTokens = extractEvalTokensOrdered(original)
    val polishedEvalTokens = extractEvalTokensOrdered(polished)
    val evalAllowedCount = tokenCounts(originalEvalTokens)
    val evalPolishedCount = tokenCounts(polishedEvalTokens)
    val evalWithinCountBudget = evalPolishedCount.forall { case (token, count) =>
      count <= evalAllowedCount.getOrElse(token, 0)
    }
    val evalOrderSatisfied = isSubsequence(polishedEvalTokens, originalEvalTokens)

    val originalBranches = extractVariationBranchesOrdered(original)
    val polishedBranches = extractVariationBranchesOrdered(polished)
    val branchAllowedCount = tokenCounts(originalBranches)
    val branchPolishedCount = tokenCounts(polishedBranches)
    val branchWithinCountBudget = branchPolishedCount.forall { case (label, count) =>
      count <= branchAllowedCount.getOrElse(label, 0)
    }
    val branchOrderSatisfied = isSubsequence(originalBranches, polishedBranches)

    val reasons = List.newBuilder[String]
    if !sanPresenceSatisfied then reasons += "san_missing"
    if !withinCountBudget then reasons += "count_budget_exceeded"
    if !preservesOriginalSequence then reasons += "san_core_missing"
    if !respectsAllowedOrder then reasons += "san_order_violation"
    if !numberingSatisfied then reasons += "numbering_missing"
    if !markerStyleOrderSatisfied then reasons += "marker_style_mismatch"
    if !evalWithinCountBudget then reasons += "eval_count_budget_exceeded"
    if !evalOrderSatisfied then reasons += "eval_order_violation"
    if !branchWithinCountBudget then reasons += "variation_branch_count_exceeded"
    if !branchOrderSatisfied then reasons += "variation_branch_violation"

    val reasonList = reasons.result()
    ValidationResult(
      isValid = reasonList.isEmpty,
      reasons = reasonList
    )

  def isPolishedCommentaryValid(
      polished: String,
      original: String,
      allowedSans: List[String]
  ): Boolean =
    validatePolishedCommentary(polished, original, allowedSans).isValid
