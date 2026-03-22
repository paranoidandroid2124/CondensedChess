package lila.llm.analysis

import scala.util.matching.Regex

private[llm] object LiveNarrativeCompressionCore:

  private val systemLanguageBanFragments = List(
    "strategic stack",
    "ranked stack",
    "current support centers on",
    "support still centers on",
    "plan first",
    "refutation/hold",
    "hold/refutation",
    "latent plan",
    "probe evidence says",
    "evidence must show",
    "nominal evaluation",
    "cp compensation investment",
    "dominant thesis",
    "so the plan cannot drift",
    "still looks playable in the engine line",
    "needs stronger support beyond that line"
  )

  private val playerLanguageBanPatterns: List[(String, Regex)] = List(
    "plan fit" -> """(?i)\bplan fit\b""".r,
    "nominal evaluation" -> """(?i)\bnominal evaluation\b""".r,
    "practical conversion" -> """(?i)\bpractical conversion\b""".r,
    "conversion window" -> """(?i)\bconversion window\b""".r,
    "forgiveness" -> """(?i)\bforgiveness\b""".r,
    "activation lane" -> """(?i)\bactivation lane\b""".r,
    "counterplay suppression" -> """(?i)\bcounterplay suppression\b""".r,
    "cuts out counterplay" -> """(?i)\bcuts out counterplay\b""".r,
    "making available" -> """(?i)\bmaking [^.]+ available\b""".r,
    "coordination improvement" -> """(?i)\bcoordination improvement\b""".r,
    "rather than drifting" -> """(?i)\brather than drifting into\b""".r,
    "focused on" -> """(?i)\bkeeps the play focused on\b""".r,
    "foreground via" -> """(?i)\bin the foreground via\b""".r,
    "within normal bounds" -> """(?i)\bwithin normal bounds\b""".r,
    "contested objective" -> """(?i)\((?:contested|build)\)""".r,
    "concrete support placeholder" -> """(?i)\bstill needs more concrete support\b""".r,
    "idea support placeholder" -> """(?i)\bthe idea still needs concrete support\b""".r,
    "plan still revolves around" -> """(?i)\bthe plan still revolves around\b""".r,
    "useful route" -> """(?i)\ba useful route is\b""".r,
    "useful target" -> """(?i)\bthe next useful target is\b""".r,
    "pieces coordinated" -> """(?i)\bkeeps the pieces coordinated\b""".r,
    "easy to handle" -> """(?i)\bkeeps the position easy to handle\b""".r,
    "holds together" -> """(?i)\bholds the position together\b""".r,
    "keep bringing" -> """(?i)\bthe next step is to keep bringing the\b""".r
  )

  private val abstractClaimTokens = List(
    "initiative",
    "counterplay",
    "compensation",
    "conversion",
    "pressure",
    "attack",
    "plan",
    "campaign",
    "objective",
    "execution"
  )

  private val concreteAnchorPatterns: List[Regex] = List(
    """\b[a-h][1-8]\b""".r,
    """\b[a-h]-file\b""".r,
    """\b(?:queenside|kingside|central|open)\s+files?\b""".r,
    """\b(?:queenside|kingside|central|fixed)\s+targets?\b""".r,
    """\b(?:light|dark)-squared\b""".r,
    """\b(?:exchange|trade|recapture|pawn break|break|castling|castle|back-rank)\b""".r,
    """(?:^|\s)(?:\d+\.(?:\.\.)?\s*)?(?:O-O(?:-O)?|[KQRBN]?[a-h]?[1-8]?x?[a-h][1-8](?:=[QRBN])?[+#]?)""".r,
    """\.\.\.(?:O-O(?:-O)?|[KQRBN]?[a-h]?[1-8]?x?[a-h][1-8](?:=[QRBN])?[+#]?)""".r,
    """\b(?:king|queen|rook|bishop|knight|pawn)s?\b""".r
  )

  def systemLanguageBanList: List[String] = systemLanguageBanFragments

  def systemLanguageHits(raw: String): List[String] =
    val low = Option(raw).getOrElse("").toLowerCase
    systemLanguageBanFragments.filter(low.contains)

  def playerLanguageHits(raw: String): List[String] =
    val text = Option(raw).getOrElse("")
    playerLanguageBanPatterns.collect { case (label, pattern) if pattern.findFirstIn(text).nonEmpty => label }

  def hasConcreteAnchor(raw: String): Boolean =
    val text = Option(raw).getOrElse("")
    concreteAnchorPatterns.exists(_.findFirstIn(text).nonEmpty)

  def requiresConcreteAnchor(raw: String): Boolean =
    val low = Option(raw).getOrElse("").toLowerCase
    abstractClaimTokens.exists(low.contains)

  def keepPlayerFacingSentence(raw: String): Boolean =
    val text = Option(raw).getOrElse("").trim
    text.nonEmpty &&
    systemLanguageHits(text).isEmpty &&
    playerLanguageHits(text).isEmpty &&
    (!requiresConcreteAnchor(text) || hasConcreteAnchor(text))

  def renderPracticalBiasPlayer(factorRaw: String, descriptionRaw: String): Option[String] =
    val factor = Option(factorRaw).getOrElse("").trim.toLowerCase
    val description = Option(descriptionRaw).getOrElse("").trim.toLowerCase
    Option.when(factor.nonEmpty || description.nonEmpty) {
      if factor.contains("mobility") then "the pieces have more room"
      else if factor.contains("forgiveness") && """(\d+)\s+safe moves?""".r.findFirstMatchIn(description).nonEmpty then
        val count = """(\d+)\s+safe moves?""".r.findFirstMatchIn(description).map(_.group(1)).getOrElse("more")
        s"there are $count safe follow-up moves"
      else if factor.contains("forgiveness") then "there are more safe follow-up moves"
      else if factor.contains("king") then "the king has fewer checks to answer"
      else if factor.contains("coordination") then "the pieces work together more easily"
      else if factor.contains("simplification") || factor.contains("trade") then "the exchanges are easier to judge"
      else if factor.contains("space") then "the side with the move has more room"
      else if factor.contains("time") || factor.contains("tempo") then "the move keeps the initiative without losing time"
      else if hasConcreteAnchor(description) then description
      else if hasConcreteAnchor(factor) then factor
      else ""
    }.filter(_.nonEmpty)

  def rewritePlayerLanguage(raw: String): String =
    Option(raw).getOrElse("")
      .replaceAll("""(?i)\s+\((?:contested|build)\)""", "")
      .replaceAll("""(?i)\s*\((?:the idea still needs concrete support|still needs more concrete support)\)""", "")
      .replaceAll("""(?i)\b[A-Za-z_]+ and its (?:fluid|locked|symmetric|semi-open|open) center\b""", "the current structure")
      .replaceAll("""(?i)\bcoordination improvement\b""", "better piece coordination")
      .replaceAll("""(?i)\bplan activation lane\b""", "the main plan")
      .replaceAll("""(?i)\ba cleaner bishop circuit\b""", "a better bishop route")
      .replaceAll("""(?i)\bpiece activation before the break\b""", "a better square before the break")
      .replaceAll("""(?i)\bopen file occupation\b""", "control of the open file")
      .replaceAll("""(?i)\bsemi-open file occupation\b""", "control of the semi-open file")
      .replaceAll("""(?i)\bking-attack build-up around ([^.]+?)\b""", "pressure toward $1")
      .replaceAll("""(?i)\bfavorable trade or transformation around ([^.]+?)\b""", "exchanges around $1")
      .replaceAll("""(?i)\bspace gain or restriction around ([^.]+?)\b""", "space around $1")
      .replaceAll("""(?i)\bline occupation around ([^.]+?)\b""", "pressure along $1")
      .replaceAll("""(?i)\bimmediate tactical gain Counterplay\b""", "tactical counterplay")
      .replaceAll("""(?i)\bin the foreground via\b""", "with")
      .replaceAll("""(?i)\bso the plan cannot drift\b""", "so the idea stays clear")
      .replaceAll("""(?i)\beasier to organize\b""", "easier to carry out")
      .replaceAll("""(?i)\bmore confirmation is still needed\b""", "")
      .replaceAll("""(?i)\bfocus on ([a-h][1-8](?:,\s*[a-h][1-8])*)\b""", "pressure on $1")
      .replaceAll("""(?i)^the plan still revolves around ([^.]+)\.$""", "The key idea is $1.")
      .replaceAll("""(?i)^a useful route is ([^.]+)\.$""", "A likely follow-up is $1.")
      .replaceAll("""(?i)^the next useful target is ([^.]+)\.$""", "A concrete target is $1.")
      .replaceAll(
        """(?i)^the move keeps the play focused on ([^.]+?), with the pieces working through ([^.]+?) rather than drifting into ([^.]+)\.$""",
        "The move is really about $1, and $2 is the clearest way to build on it."
      )
      .replaceAll(
        """(?i)^the move keeps the play focused on ([^.]+?)\.$""",
        "The move is really about $1."
      )
      .replaceAll(
        """(?i)^the move gives up material to keep ([^.]+?) through ([^.]+?) rather than winning it back right away\.$""",
        "The move gives up material to keep $1, and $2 is the clearest way to use that compensation."
      )
      .replaceAll(
        """(?i)^the next moves still run through ([^.]+)\.$""",
        "The next useful route is $1."
      )
      .replaceAll(
        """(?i)^the next target is bringing the ([a-z]+) to ([a-h][1-8])\.$""",
        "The $1 can head for $2 next."
      )
      .replaceAll(
        """(?i)^the next target is bringing ([^.]+)\.$""",
        "The next step is $1."
      )
      .replaceAll(
        """(?i)^the move keeps the ([a-z]+) pointed toward ([a-h][1-8])\.$""",
        "The move keeps the $1 route toward $2 available."
      )
      .replaceAll(
        """(?i)^the move keeps the ([a-z]+) pointed toward ([a-h][1-8]) while ([^.]+)\.$""",
        "The move keeps the $1 route toward $2 available while $3."
      )
      .replaceAll(
        """(?i)^the next step is to keep bringing the ([a-z]+) to ([a-h][1-8])\.$""",
        "The next step is to bring the $1 to $2."
      )
      .replaceAll(
        """(?i)^the next step is to keep bringing the ([a-z]+)\.$""",
        "The next step is to improve the $1."
      )
      .replaceAll(
        """(?i)^the move reorganizes the pieces around the current structure\.$""",
        "The move improves the piece placement without changing the basic structure."
      )
      .replaceAll(
        """(?i)^the move reorganizes the pieces around ([^.]+)\.$""",
        "The move improves the piece placement around $1."
      )
      .replaceAll(
        """(?i)^the follow-up is to stay inside ([^.]+?) themes without losing the position's balance\.$""",
        "The move keeps development going without changing the basic balance."
      )
      .replaceAll(
        """(?i)^the follow-up is to finish development without giving up the center without losing the position's balance\.$""",
        "The next step is to finish development without giving up the center."
      )
      .replaceAll(
        """(?i)^the follow-up is to ([^.]+?) without losing the position's balance\.$""",
        "The next step is to $1."
      )
      .replaceAll(
        """(?i)^([a-h][1-8]) is the square that keeps ([^.]+?) grounded\.$""",
        "Pressure on $1 keeps $2 concrete."
      )
      .replaceAll(
        """(?i)(white|black) is investing material for ([^.]+?) while keeping [^.]+? rather than ([^.]+)\.""",
        "$1 is investing material for $2 rather than $3."
      )
      .replaceAll(
        """(?i)the point of the move is not an immediate score, but to accept a \d+cp compensation investment for ([^.]+)\.""",
        "The move invests material for $1 rather than trying to win it back at once."
      )
      .replaceAll(
        """(?i)(white|black) is not trying to win the material back immediately; the \d+cp compensation investment is for ([^.]+)\.""",
        "$1 is investing material for $2 rather than grabbing it back at once."
      )
      .replaceAll(
        """(?i)more important than the nominal evaluation is that the move creates an easier practical task through""",
        "The move is easier to handle because"
      )
      .replaceAll(
        """(?i)more important than the nominal evaluation is that the move creates a ([^.]+?) practical task\.""",
        "The move is easier to handle over the board."
      )
      .replaceAll(
        """(?i)the resulting task is ([^.]+?)\.""",
        "That version is easier to handle."
      )
      .replaceAll(
        """(?i)that matters because""",
        "That is easier because"
      )
      .replaceAll(
        """(?i)the compensation depends on""",
        "This works only while"
      )
      .replaceAll(
        """(?i)the compensation is still built around""",
        "The play still runs through"
      )
      .replaceAll(
        """(?i)the execution still runs through""",
        "The next moves still run through"
      )
      .replaceAll(
        """(?i)the long-term objective is""",
        "The next target is"
      )
      .replaceAll(
        """(?i)the move matters less for a direct gain than for cutting out""",
        "The move is mainly about stopping"
      )
      .replaceAll(
        """(?i)that strips away roughly \d+cp of counterplay\.""",
        "That makes the opponent's setup much harder to start."
      )
      .replaceAll(
        """(?i)keep counterplay tied down before recovering the material""",
        "keep the extra pawn from getting active before winning the material back"
      )
      .replaceAll(
        """(?i)it also gives ([^.]+?) more time to take hold\.""",
        "It buys time for $1."
      )
      .replaceAll(
        """(?i)turn the compensation into a clean transition""",
        "use the initiative to force favorable exchanges"
      )
      .replaceAll(
        """(?i)plan fit is shaped by""",
        "The structure points that way because"
      )
      .replaceAll(
        """(?i)making ([a-h][1-8]) available for the ([a-z]+)""",
        "bringing the $2 to $1"
      )
      .replaceAll(
        """(?i)making ([a-h][1-8]) available""",
        "preparing $1"
      )
      .replaceAll(
        """(?i)^the key decision is to choose ([^,]+?) and postpone ([^,]+?), because (.+)\.$""",
        "The move chooses $1 first and leaves $2 for later, because $3."
      )
      .replaceAll(
        """(?i)^a central square in the decision is ([^.]+)\.$""",
        "The move is really about pressure on $1."
      )
      .replaceAll(
        """(?i), aiming at [^.]+\.$""",
        "."
      )
      .replaceAll(
        """(?i)\s+near the center of the plan\b""",
        ""
      )
      .replaceAll(
        """(?i)\b\d+cp compensation investment\b""",
        "material investment"
      )
      .replaceAll(
        """(?i)\s+and\s+the idea still needs concrete support\.""",
        "."
      )
      .replaceAll(
        """(?i)\s+because\s+the idea still needs concrete support\.""",
        "."
      )
      .replaceAll("""\s{2,}""", " ")
      .trim

  def trimLeadScaffold(raw: String): String =
    Option(raw).getOrElse("").trim
      .replaceAll("""(?i)^the move keeps the game inside ([^.]+?) territory\.$""", "The move keeps the game in $1.")
      .replaceAll("""(?i)^the move keeps the game inside ([^.]+?) themes around ([^.]+)\.$""", "The move keeps the game in $1, with $2 as the main theme.")

  def isLowValueNarrativeSentence(raw: String): Boolean =
    val low = Option(raw).getOrElse("").trim.toLowerCase
    low.isEmpty ||
      low == "the move keeps the position within normal bounds." ||
      low == "the move keeps development going without changing the basic balance." ||
      low == "the next step is to finish development without giving up the center." ||
      low == "the move improves the piece placement without changing the basic structure." ||
      low == "the move improves the piece placement around the current structure." ||
      low == "the idea still needs concrete support." ||
      low == "still needs more concrete support." ||
      low.matches("""the move keeps the game in [^.]+\.""") ||
      low.matches("""the move keeps the game in [^.]+, with [^.]+ as the main theme\.""") ||
      low.matches("""the follow-up is to stay inside [^.]+ themes without losing the position's balance\.""") ||
      low.matches("""the next step is to keep bringing the [a-z]+(?: to [a-h][1-8])?\.""") ||
      low.matches("""the move keeps the [a-z]+ pointed toward [a-h][1-8](?: while [^.]+)?\.""") ||
      low.contains("keeps the pieces coordinated") ||
      low.contains("keeps the position easy to handle") ||
      low.contains("holds the position together") ||
      low.contains("the square that keeps") && low.contains("grounded") ||
      low.contains("still looks playable in the engine line") ||
      low.contains("needs stronger support beyond that line") ||
      low.contains("more confirmation is still needed") ||
      low.contains("so the plan cannot drift") ||
      low.contains("dominant thesis")

  def deterministicProse(slots: BookmakerPolishSlots): String =
    BookmakerSoftRepair.deterministicParagraphs(slots).mkString("\n\n").trim

  def deterministicFallbackProse(
      prose: String,
      slots: Option[BookmakerPolishSlots]
  ): String =
    slots.map(deterministicProse).filter(_.nonEmpty).getOrElse(Option(prose).getOrElse("").trim)

  def slotsFromCompressedProse(
      prose: String,
      lens: StrategicLens = StrategicLens.Decision
  ): Option[BookmakerPolishSlots] =
    val paragraphs =
      BookmakerProseContract.splitParagraphs(prose)
        .map(_.trim)
        .filter(_.nonEmpty)
    paragraphs.headOption.map { claim =>
      val supportPrimary = paragraphs.lift(1).filter(_.nonEmpty)
      val third = paragraphs.lift(2).filter(_.nonEmpty)
      val evidenceHook = third.filter(LineScopedCitation.hasConcreteSanLine)
      val tension = third.filterNot(LineScopedCitation.hasConcreteSanLine)
      val paragraphPlan =
        List(
          Some("p1=claim"),
          supportPrimary.map(_ => "p2=support"),
          evidenceHook.map(_ => "p3=cited_line")
            .orElse(tension.map(_ => "p3=practical_nuance"))
        ).flatten
      BookmakerPolishSlots(
        lens = lens,
        claim = claim,
        supportPrimary = supportPrimary,
        supportSecondary = None,
        tension = tension,
        evidenceHook = evidenceHook,
        coda = None,
        factGuardrails = evidenceHook.toList,
        paragraphPlan = if paragraphPlan.nonEmpty then paragraphPlan else List("p1=claim")
      )
    }

  def openingSlotsFromCompressedProse(prose: String): Option[BookmakerPolishSlots] =
    slotsFromCompressedProse(prose, StrategicLens.Opening)

  def practicalSlotsFromCompressedProse(prose: String): Option[BookmakerPolishSlots] =
    slotsFromCompressedProse(prose, StrategicLens.Practical)

  def decisionSlotsFromCompressedProse(prose: String): Option[BookmakerPolishSlots] =
    slotsFromCompressedProse(prose, StrategicLens.Decision)
