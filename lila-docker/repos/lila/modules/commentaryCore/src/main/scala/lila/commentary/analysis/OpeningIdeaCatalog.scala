package lila.commentary.analysis

import lila.commentary.model.NarrativeContext

private[commentary] object OpeningIdeaCatalog:

  final case class OpeningIdea(
      family: String,
      pattern: String,
      title: String,
      prose: String,
      reasonTags: List[String]
  )

  private final case class OpeningIdeaEntry(
      family: String,
      pattern: String,
      familyMatchers: List[String],
      moveMatchers: Set[String],
      requiredPrimitiveTags: Set[String],
      requiredBoardFacts: List[MoveReviewBoardFacts.MoveFacts => Boolean],
      requiredPvFacts: List[Option[MoveReviewPvFacts.LineFacts] => Boolean],
      blockedWhen: List[MoveReviewBoardFacts.MoveFacts => Boolean],
      title: String => String,
      prose: String => String,
      reasonTags: List[String]
  ):
    def matches(
        openingName: String,
        san: String,
        uci: String,
        primitiveTags: Set[String],
        facts: MoveReviewBoardFacts.MoveFacts,
        lineFacts: Option[MoveReviewPvFacts.LineFacts]
    ): Boolean =
      familyMatchers.exists(openingName.contains) &&
        (moveMatchers.contains(san) || moveMatchers.contains(uci)) &&
        requiredPrimitiveTags.subsetOf(primitiveTags) &&
        requiredBoardFacts.forall(_(facts)) &&
        requiredPvFacts.forall(_(lineFacts)) &&
        !blockedWhen.exists(_(facts))

    def idea(playedSan: String): OpeningIdea =
      OpeningIdea(
        family = family,
        pattern = pattern,
        title = title(playedSan),
        prose = prose(playedSan),
        reasonTags = reasonTags
      )

  def matchIdea(
      ctx: NarrativeContext,
      facts: MoveReviewBoardFacts.MoveFacts,
      primitiveTags: Set[String],
      lineFacts: Option[MoveReviewPvFacts.LineFacts]
  ): Option[OpeningIdea] =
    val openingName =
      ctx.openingData.flatMap(_.name)
        .orElse(
          ctx.openingEvent.collect {
            case event: lila.commentary.model.OpeningEvent.Intro => event.name
          }
        )
        .map(_.toLowerCase)
        .getOrElse("")
    val san = stripSanSuffix(facts.san)
    val uci = facts.uci.toLowerCase
    entries.find(_.matches(openingName, san, uci, primitiveTags, facts, lineFacts)).map(_.idea(facts.san))

  private val entries: List[OpeningIdeaEntry] = List(
    entry(
      family = "Italian Game",
      pattern = "quiet_d3_setup",
      familyMatchers = List("italian"),
      moveMatchers = Set("Bc4", "f1c4"),
      requiredPrimitiveTags = Set("develops_piece", "targets_f7_or_f2", "controls_center"),
      requiredPvFacts = List(hasContinuation("d2d3")),
      title = san => s"$san builds a quiet Italian setup",
      prose = san =>
        s"$san develops the bishop into an Italian Game setup. From c4 it points at f7 and adds control over d5, while the PV keeps the quieter d3 structure available.",
      reasonTags = List("opening_idea", "develops_piece", "targets_f7_or_f2", "controls_center")
    ),
    entry(
      family = "Italian Game",
      pattern = "c3_d4_center_break",
      familyMatchers = List("italian"),
      moveMatchers = Set("Bc4", "f1c4", "c3", "c2c3", "d4", "d2d4"),
      requiredPrimitiveTags = Set("controls_center"),
      requiredPvFacts = List(hasAnyContinuation(Set("c2c3", "d2d4"))),
      title = san => s"$san prepares the Italian central break",
      prose = san =>
        s"$san belongs to the Italian center-break plan. The point is to place the pieces first, then use c3 or d4 to ask whether Black can hold the center.",
      reasonTags = List("opening_idea", "controls_center", "center_break_setup")
    ),
    entry(
      family = "Italian Game",
      pattern = "bishop_c4_f7_d5",
      familyMatchers = List("italian"),
      moveMatchers = Set("Bc4", "f1c4"),
      requiredPrimitiveTags = Set("develops_piece", "targets_f7_or_f2", "controls_center"),
      title = san => s"$san builds Italian pressure on f7",
      prose = san =>
        s"$san develops the bishop into an Italian Game setup. From c4 it points at f7 and adds control over d5, so the move is not just development; it shapes Black's next central and kingside decisions.",
      reasonTags = List("opening_idea", "develops_piece", "targets_f7_or_f2", "controls_center")
    ),
    entry(
      family = "Ruy Lopez",
      pattern = "a6_Ba4_maintains_pressure",
      familyMatchers = List("ruy lopez"),
      moveMatchers = Set("Bb5", "f1b5"),
      requiredPrimitiveTags = Set("develops_piece"),
      requiredBoardFacts = List(attacksSquare("c6")),
      requiredPvFacts = List(hasReplyAndContinuation("a7a6", "b5a4")),
      title = san => s"$san keeps the Ruy Lopez pressure alive",
      prose = san =>
        s"$san develops the bishop with a Ruy Lopez purpose: it pressures the c6 knight, and the PV shows the bishop can answer ...a6 without abandoning the pressure on e5.",
      reasonTags = List("opening_idea", "develops_piece", "controls_center")
    ),
    entry(
      family = "Ruy Lopez",
      pattern = "bishop_b5_c6_e5",
      familyMatchers = List("ruy lopez"),
      moveMatchers = Set("Bb5", "f1b5"),
      requiredPrimitiveTags = Set("develops_piece"),
      requiredBoardFacts = List(attacksSquare("c6")),
      title = san => s"$san asks how Black will hold e5",
      prose = san =>
        s"$san develops the bishop with a Ruy Lopez purpose: it puts pressure on the c6 knight, the main defender of e5. That separates the opening idea from a generic developing move because the piece activity is tied to Black's central pawn.",
      reasonTags = List("opening_idea", "develops_piece", "controls_center")
    ),
    entry(
      family = "Ruy Lopez",
      pattern = "Nf6_OO_king_safety_before_center",
      familyMatchers = List("ruy lopez"),
      moveMatchers = Set("O-O", "e1g1"),
      requiredPrimitiveTags = Set("king_safety"),
      title = san => s"$san puts king safety before the center opens",
      prose = san =>
        s"$san is the Ruy Lopez safety move: White gets the king out of the center before deciding whether the e-file and central tension should open.",
      reasonTags = List("opening_idea", "king_safety")
    ),
    entry(
      family = "Queen's Gambit",
      pattern = "exchange_vs_tension",
      familyMatchers = List("queen's gambit"),
      moveMatchers = Set("c4", "c2c4"),
      requiredPrimitiveTags = Set("controls_center"),
      requiredPvFacts = List(lineContains(Set("d5c4", "c4d5", "e7e6", "c7c6"))),
      title = san => s"$san keeps the d5 tension concrete",
      prose = san =>
        s"$san creates the Queen's Gambit tension by attacking d5 from the side. The PV matters because it shows whether Black supports that pawn or releases the tension by exchange.",
      reasonTags = List("opening_idea", "controls_center", "opens_line")
    ),
    entry(
      family = "Queen's Gambit",
      pattern = "c4_d5_tension",
      familyMatchers = List("queen's gambit"),
      moveMatchers = Set("c4", "c2c4"),
      requiredPrimitiveTags = Set("controls_center"),
      title = san => s"$san challenges Black's d5 center",
      prose = san =>
        s"$san creates the Queen's Gambit tension by attacking the d5 pawn from the side. The point is to make Black choose between holding the center structurally and releasing it under pressure.",
      reasonTags = List("opening_idea", "controls_center", "opens_line")
    ),
    entry(
      family = "Queen's Gambit",
      pattern = "Nc3_Nf3_e3_development_around_d5",
      familyMatchers = List("queen's gambit"),
      moveMatchers = Set("Nc3", "b1c3", "Nf3", "g1f3", "e3", "e2e3"),
      requiredPrimitiveTags = Set("controls_center"),
      title = san => s"$san develops around the d5 tension",
      prose = san =>
        s"$san is a Queen's Gambit development move tied to the d5 tension. It adds support before White decides whether to keep, increase, or release the central pressure.",
      reasonTags = List("opening_idea", "develops_piece", "controls_center")
    ),
    entry(
      family = "Sicilian Defense",
      pattern = "d4_cxd4_Nxd4_open_sicilian",
      familyMatchers = List("sicilian"),
      moveMatchers = Set("d4", "d2d4"),
      requiredPrimitiveTags = Set("controls_center", "opens_line"),
      requiredPvFacts = List(hasReplyAndContinuation("c5d4", "f3d4")),
      title = san => s"$san opens the Sicilian center",
      prose = san =>
        s"$san is the Open Sicilian commitment: White uses d4 to force the central exchange and bring a piece back into the center.",
      reasonTags = List("opening_idea", "controls_center", "opens_line")
    ),
    entry(
      family = "Sicilian Defense",
      pattern = "Nf3_prepares_d4",
      familyMatchers = List("sicilian"),
      moveMatchers = Set("Nf3", "g1f3"),
      requiredPrimitiveTags = Set("develops_piece", "controls_center"),
      title = san => s"$san prepares the d4 question",
      prose = san =>
        s"$san develops with a Sicilian purpose: it supports a later d4, so White can challenge Black's c5 setup with a piece ready to recapture.",
      reasonTags = List("opening_idea", "develops_piece", "controls_center")
    ),
    entry(
      family = "Sicilian Defense",
      pattern = "c5_d4_counter_center",
      familyMatchers = List("sicilian"),
      moveMatchers = Set("c5", "c7c5"),
      requiredPrimitiveTags = Set("controls_center"),
      title = san => s"$san contests d4 without symmetry",
      prose = san =>
        s"$san is the Sicilian central challenge: Black fights d4 from the flank instead of mirroring e5. That keeps the center asymmetrical and asks White to justify the extra space.",
      reasonTags = List("opening_idea", "controls_center")
    ),
    sicilianSetup("d6", "d7d6", "d6_center_choice", "supports the c5 structure before the center opens"),
    sicilianSetup("Nc6", "b8c6", "Nc6_center_choice", "develops into the fight over d4 and e5"),
    sicilianSetup("e6", "e7e6", "e6_center_choice", "keeps ...d5 and dark-square development in reserve"),
    entry(
      family = "French Defense",
      pattern = "advance_structure",
      familyMatchers = List("french"),
      moveMatchers = Set("e5", "e4e5"),
      requiredPrimitiveTags = Set("controls_center"),
      title = san => s"$san fixes the French pawn chain",
      prose = san =>
        s"$san advances into the French structure. The point is to claim space while accepting that Black will attack the chain from the base.",
      reasonTags = List("opening_idea", "controls_center")
    ),
    entry(
      family = "French Defense",
      pattern = "exchange_structure",
      familyMatchers = List("french"),
      moveMatchers = Set("exd5", "e4d5", "exd5", "e6d5"),
      requiredPrimitiveTags = Set("tempo"),
      title = san => s"$san releases the French center",
      prose = san =>
        s"$san moves the French structure toward an exchange formation. The explanation should stay local: the central tension is released rather than turned into a broad strategic verdict.",
      reasonTags = List("opening_idea", "controls_center")
    ),
    entry(
      family = "French Defense",
      pattern = "e6_d5_chain",
      familyMatchers = List("french"),
      moveMatchers = Set("e6", "e7e6"),
      requiredBoardFacts = List(attacksSquare("d5")),
      title = san => s"$san prepares a locked central fight",
      prose = san =>
        s"$san starts the French structure by preparing ...d5 under protection. The move accepts a slower light-squared bishop so Black can build a resilient central chain.",
      reasonTags = List("opening_idea", "defends_center_pawn")
    ),
    entry(
      family = "Caro-Kann Defense",
      pattern = "solid_center",
      familyMatchers = List("caro-kann"),
      moveMatchers = Set("d5", "d7d5"),
      requiredPrimitiveTags = Set("controls_center"),
      title = san => s"$san builds the Caro-Kann center",
      prose = san =>
        s"$san is the Caro-Kann central claim after ...c6. Black gets a solid d5 pawn without copying White's e-pawn structure.",
      reasonTags = List("opening_idea", "controls_center")
    ),
    entry(
      family = "Caro-Kann Defense",
      pattern = "exchange_structure",
      familyMatchers = List("caro-kann"),
      moveMatchers = Set("exd5", "e4d5", "c6d5"),
      requiredPrimitiveTags = Set("tempo"),
      title = san => s"$san clarifies the Caro-Kann structure",
      prose = san =>
        s"$san releases the Caro-Kann central tension. That is a structural clarification, not a standalone evaluation claim.",
      reasonTags = List("opening_idea", "controls_center")
    ),
    entry(
      family = "Caro-Kann Defense",
      pattern = "c6_d5_support",
      familyMatchers = List("caro-kann"),
      moveMatchers = Set("c6", "c7c6"),
      requiredBoardFacts = List(attacksSquare("d5")),
      title = san => s"$san prepares a supported d5 break",
      prose = san =>
        s"$san is a Caro-Kann setup move: it supports ...d5 before Black commits the center. The purpose is structural reliability rather than immediate piece activity.",
      reasonTags = List("opening_idea", "defends_center_pawn")
    )
  )

  private def entry(
      family: String,
      pattern: String,
      familyMatchers: List[String],
      moveMatchers: Set[String],
      requiredPrimitiveTags: Set[String] = Set.empty,
      requiredBoardFacts: List[MoveReviewBoardFacts.MoveFacts => Boolean] = Nil,
      requiredPvFacts: List[Option[MoveReviewPvFacts.LineFacts] => Boolean] = Nil,
      blockedWhen: List[MoveReviewBoardFacts.MoveFacts => Boolean] = Nil,
      title: String => String,
      prose: String => String,
      reasonTags: List[String]
  ): OpeningIdeaEntry =
    OpeningIdeaEntry(
      family,
      pattern,
      familyMatchers,
      moveMatchers,
      requiredPrimitiveTags,
      requiredBoardFacts,
      requiredPvFacts,
      blockedWhen,
      title,
      prose,
      reasonTags
    )

  private def sicilianSetup(san: String, uci: String, pattern: String, purpose: String): OpeningIdeaEntry =
    entry(
      family = "Sicilian Defense",
      pattern = pattern,
      familyMatchers = List("sicilian"),
      moveMatchers = Set(san, uci),
      title = played => s"$played chooses a Sicilian center setup",
      prose = played => s"$played $purpose. The claim stays bounded to how Black arranges the central fight.",
      reasonTags = List("opening_idea", "controls_center")
    )

  private def attacksSquare(square: String)(facts: MoveReviewBoardFacts.MoveFacts): Boolean =
    facts.attacks(square)

  private def hasContinuation(uci: String)(lineFacts: Option[MoveReviewPvFacts.LineFacts]): Boolean =
    lineFacts.flatMap(_.continuation).exists(move => MoveReviewPvFacts.normalizeUci(move.uci) == uci)

  private def hasAnyContinuation(ucis: Set[String])(lineFacts: Option[MoveReviewPvFacts.LineFacts]): Boolean =
    lineFacts.exists(line => line.continuation.exists(move => ucis.contains(MoveReviewPvFacts.normalizeUci(move.uci))))

  private def hasReplyAndContinuation(replyUci: String, continuationUci: String)(lineFacts: Option[MoveReviewPvFacts.LineFacts]): Boolean =
    lineFacts.exists(line =>
      line.reply.exists(move => MoveReviewPvFacts.normalizeUci(move.uci) == replyUci) &&
        line.continuation.exists(move => MoveReviewPvFacts.normalizeUci(move.uci) == continuationUci)
    )

  private def lineContains(ucis: Set[String])(lineFacts: Option[MoveReviewPvFacts.LineFacts]): Boolean =
    lineFacts.exists(line => line.line.moves.exists(move => ucis.contains(MoveReviewPvFacts.normalizeUci(move.uci))))

  private def stripSanSuffix(san: String): String =
    Option(san).getOrElse("").trim.replaceAll("""[+#?!]+$""", "")
