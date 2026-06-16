package lila.commentary.analysis

import lila.commentary.analysis.semantic.{ DeferredRelationFallbackLane, RelationObservationCatalog }

private[analysis] object NarrativeMotifPrefixTable:

  private def normalizeMotifTag(raw: String): String =
    Option(raw).getOrElse("").trim
      .replaceAll("([a-z])([A-Z])", "$1_$2")
      .toLowerCase
      .replaceAll("[^a-z0-9]+", "_")
      .replaceAll("_+", "_")
      .stripPrefix("_")
      .stripSuffix("_")

  private def motifMatches(normalizedMotif: String, rawNeedle: String): Boolean =
    val motif = normalizeMotifTag(normalizedMotif)
    val needle = normalizeMotifTag(rawNeedle)
    motif.nonEmpty && needle.nonEmpty &&
      (motif.contains(needle) || motif.replace("_", "").contains(needle.replace("_", "")))

  private def deferredRelationTemplates(normalized: List[String]): Option[Option[List[String]]] =
    normalized.flatMap(RelationObservationCatalog.deferredFallbackForMotifTag).headOption.map { fallback =>
      fallback.lane match
        case DeferredRelationFallbackLane.ExchangeSequence | DeferredRelationFallbackLane.MaterialTransition =>
          Some(
            List(
              "The checked line points to a material transition rather than a named relation.",
              "The practical reading is a concrete exchange sequence, not a settled motif label.",
              "Material flow is the safer guide until the relation is replay-proven."
            )
          )
        case _ =>
          None
    }

  private final case class MotifPrefixRule(
      keys: List[String],
      templates: List[String],
      blockedKeys: List[String] = Nil,
      minPly: Option[Int] = None,
      maxPly: Option[Int] = None,
      allow: (List[String], Int) => Boolean = (_, _) => true
  ):
    def matches(normalized: List[String], ply: Int): Boolean =
      hasAnyMotif(normalized, keys) &&
        !hasAnyMotif(normalized, blockedKeys) &&
        minPly.forall(ply >= _) &&
        maxPly.forall(ply <= _) &&
        allow(normalized, ply)

  private def hasAnyMotif(normalized: List[String], keys: Seq[String]): Boolean =
    keys.exists(k => normalized.exists(m => motifMatches(m, k)))

  private val MotifPrefixRules: List[MotifPrefixRule] = List(
    MotifPrefixRule(List("greek_gift"), List(
      "A classic Greek Gift sacrifice is in the air.",
      "The setup hints at a Greek Gift pattern.",
      "The bishop sacrifice motif on h7/h2 is becoming relevant."
    )),
    MotifPrefixRule(List("smothered_mate"), List(
      "A potential smothered mate pattern is forming.",
      "The geometry of a smothered mate is starting to appear.",
      "Knight-and-queen coordination points toward smothered mate ideas."
    )),
    MotifPrefixRule(List("zugzwang"), List(
      "Zugzwang ideas are central: useful moves are running out.",
      "This has a zugzwang flavor where every move concedes something.",
      "The key endgame issue is zugzwang: improving moves are scarce."
    )),
    MotifPrefixRule(List("lucena"), List(
      "Lucena technique is central: bridge construction is the key method.",
      "This has a Lucena character where precise rook shelter shapes the checking plan.",
      "Lucena geometry is in focus, so building the bridge becomes the technical priority."
    )),
    MotifPrefixRule(List("philidor"), List(
      "Philidor defensive structure is relevant: third-rank control is critical.",
      "This resembles Philidor defense, where rook activity from the defensive rank matters most.",
      "The technical defensive method here is Philidor-style rook placement."
    )),
    MotifPrefixRule(List("vancura"), List(
      "Vancura defensive ideas are available through active lateral rook checks.",
      "This has Vancura flavor: rook activity can neutralize the advanced pawn.",
      "The defensive resource resembles Vancura geometry with checking distance."
    )),
    MotifPrefixRule(List("triangulation"), List(
      "Triangulation routes are becoming important for tempo control.",
      "This king-and-pawn ending hinges on triangulation and move-order precision.",
      "A triangulation idea can force a favorable tempo shift."
    )),
    MotifPrefixRule(List("connected_passers"), List(
      "Connected passers are the dominant technical asset here.",
      "The conversion plan revolves around advancing connected passed pawns in tandem.",
      "Connected passer coordination is the central winning mechanism."
    )),
    MotifPrefixRule(List("outside_passer"), List(
      "An outside passer can decoy the king and create play on the other wing.",
      "Outside-passer strategy is now the practical technical lever.",
      "The outside passer is a distraction tool that can reshape king placement."
    )),
    MotifPrefixRule(List("wrong_bishop_fortress"), List(
      "Wrong-bishop fortress logic gives the defender durable resources.",
      "This rook-pawn plus wrong-bishop setup keeps the promotion corner central.",
      "The key endgame resource is the wrong-bishop corner fortress."
    )),
    MotifPrefixRule(List("short_side_defense"), List(
      "Short-side defensive geometry is relevant in this rook ending.",
      "The defender is aiming for short-side checking distance and king shelter.",
      "Short-side defense is the technical checking setup to watch."
    )),
    MotifPrefixRule(List("promotion_race"), List(
      "A promotion race is the key practical axis, so tempo matters immediately.",
      "Promotion timing is now central: each king move changes the race outcome.",
      "This ending is about promotion race arithmetic and accurate tempo handling."
    )),
    MotifPrefixRule(List("forced_draw_resource"), List(
      "Concrete defensive resources are active despite apparent pressure.",
      "The defender has practical resources if move order stays precise.",
      "This phase contains technical defensive resources that can keep balance."
    )),
    MotifPrefixRule(List("opposition"), List(
      "King opposition is a defining feature of this ending.",
      "Opposition geometry is shaping which side can make progress.",
      "The battle revolves around timing opposition and king squares."
    )),
    MotifPrefixRule(List("isolated_pawn", "iqp"), List(
      "The isolated-queen-pawn structure is shaping the plans.",
      "The IQP defines the strategic battle here.",
      "Play revolves around the strengths and weaknesses of the isolated pawn."
    )),
    MotifPrefixRule(List("hanging_pawns"), List(
      "The hanging pawns in the center are a major strategic factor.",
      "Central hanging pawns keep the position tense.",
      "Managing the hanging pawns will decide the middlegame plans."
    ), blockedKeys = List("passed_pawn", "bad_bishop")),
    MotifPrefixRule(List("minority_attack"), List(
      "A minority attack structure is emerging on the queenside.",
      "Queenside minority attack ideas are now practical.",
      "The queenside minority attack is becoming a concrete lever."
    ), blockedKeys = List("liquidate", "liquidation"), allow = (normalized, ply) => !(hasAnyMotif(normalized, List("bad_bishop")) && ply >= 30)),
    MotifPrefixRule(List("opposite_bishops"), List(
      "Opposite-colored bishops sharpen attacking chances.",
      "With opposite-colored bishops, king attacks gain practical value.",
      "Opposite-colored bishops increase both drawing and attacking resources."
    )),
    MotifPrefixRule(List("underpromotion"), List(
      "An underpromotion resource is becoming relevant.",
      "Underpromotion choices may be non-standard here.",
      "A rare underpromotion idea appears in the position."
    )),
    MotifPrefixRule(List("stalemate", "stalemate_trick"), List(
      "A stalemate trick is part of the defensive resources.",
      "Stalemate motifs complicate straightforward conversion.",
      "The defender has potential stalemate-based counterplay."
    )),
    MotifPrefixRule(List("prophylaxis", "prophylactic"), List(
      "A prophylactic idea is a key theme in this position.",
      "A prophylactic move to restrict counterplay is the central task.",
      "The strongest plan starts with a prophylactic preventive move."
    )),
    MotifPrefixRule(List("interference"), List(
      "Interference motifs are cutting defensive coordination.",
      "A tactical interference idea is shaping move order.",
      "Interference on key lines is now the tactical backbone."
    )),
    MotifPrefixRule(List("deflection"), List(
      "Deflection ideas are pulling defenders off key squares.",
      "A deflection motif is dictating tactical priorities.",
      "The tactical battle revolves around a key deflection."
    )),
    MotifPrefixRule(List("rook_lift"), List(
      "A rook lift idea can accelerate the attack.",
      "Rook lift geometry is becoming available.",
      "The rook can swing into action via a lift."
    )),
    MotifPrefixRule(List("good_bishop"), List(
      "A good bishop is becoming a strong strategic asset.",
      "Activating the good bishop can improve long-range pressure.",
      "The good bishop has unobstructed diagonals and lasting influence."
    )),
    MotifPrefixRule(List("bishop_pair"), List(
      "The bishop pair is a long-term strategic asset.",
      "Open-board dynamics favor the bishop pair.",
      "The bishop pair increases pressure across both wings."
    )),
    MotifPrefixRule(List("passed_pawn"), List(
      "The passed pawn is now a central practical factor.",
      "Passed pawn dynamics are starting to dominate plans.",
      "Containing the passed pawn is becoming urgent."
    ), blockedKeys = List("rook_behind_passed_pawn")),
    MotifPrefixRule(List("bad_bishop"), List(
      "A bad bishop problem is limiting piece quality.",
      "The bad bishop is restricted by its own pawn chain.",
      "The bad bishop's scope is a strategic weakness here."
    )),
    MotifPrefixRule(List("knight_domination"), List(
      "A knight-domination pattern is emerging.",
      "The knight is outperforming its counterpart.",
      "Outpost control gives the knight a stable edge."
    )),
    MotifPrefixRule(List("battery"), List(
      "A battery alignment increases tactical pressure.",
      "Line-piece battery coordination is becoming dangerous.",
      "The battery motif is shaping immediate threats."
    )),
    MotifPrefixRule(List("simplification", "simplify"), List(
      "Simplification choices now define the practical result.",
      "Trade decisions and simplification are steering the game toward a technical phase.",
      "The position is entering a conversion-through-simplification stage."
    )),
    MotifPrefixRule(List("liquidate", "liquidation", "pawn_break"), List(
      "Central liquidation is changing the position's character.",
      "A central liquidation sequence is redefining strategic priorities.",
      "The structure is about to shift through liquidation."
    )),
    MotifPrefixRule(List("king_hunt"), List(
      "A king hunt scenario is developing.",
      "King safety has become the primary tactical axis of a king hunt.",
      "The attack can escalate into a direct king hunt."
    )),
    MotifPrefixRule(List("pawn_storm"), List(
      "A pawn storm structure is taking shape on the flank.",
      "Pawn storm timing is becoming the central attacking question.",
      "Flank pawn storms are now driving the initiative battle."
    )),
    MotifPrefixRule(List("repetition", "repeat"), List(
      "Repeat ideas are now part of the practical decision tree.",
      "The position allows a repeat if neither side commits.",
      "Draw-by-repeat resources are becoming relevant."
    )),
    MotifPrefixRule(List("novelty"), List(
      "An opening novelty has changed the expected plans.",
      "This move carries novelty value compared with mainline play.",
      "The game has left familiar theory with a fresh novelty."
    )),
    MotifPrefixRule(List("semi_open_file_control"), List(
      "Semi-open file pressure is now the key strategic lever.",
      "Control of the semi-open file is becoming the central plan.",
      "The semi-open file is the main channel for rook activity."
    )),
    MotifPrefixRule(List("rook_on_seventh"), List(
      "Rook activity on the seventh rank is becoming practical.",
      "Seventh-rank invasion ideas are now central.",
      "A rook on the seventh rank could decide the technical battle."
    )),
    MotifPrefixRule(List("rook_behind_passed_pawn"), List(
      "Rook placement behind the passed pawn is the technical priority.",
      "The key endgame principle is keeping the rook behind the passed pawn.",
      "Behind-the-passed-pawn rook geometry is becoming decisive."
    )),
    MotifPrefixRule(List("king_cut_off"), List(
      "Cutting off the enemy king is the key endgame method.",
      "King cut-off geometry is becoming the core technical idea.",
      "The conversion plan revolves around restricting the enemy king."
    )),
    MotifPrefixRule(List("doubled_rooks"), List(
      "Doubled rooks can generate immediate file pressure.",
      "The doubled-rooks setup is becoming the main attacking structure.",
      "Stacking rooks on one file is the practical plan."
    )),
    MotifPrefixRule(List("connected_rooks"), List(
      "Connected rooks improve coordination for both attack and defense.",
      "Connecting rooks is now a key positional milestone.",
      "Linking connected rooks makes file-control plans easier to execute."
    )),
    MotifPrefixRule(List("maneuver"), List(
      "A rerouting maneuver is now the practical plan.",
      "Piece transfer to a better square is the main idea.",
      "A switch in piece placement can improve control."
    )),
    MotifPrefixRule(List("knight_vs_bishop"), List(
      "The knight-versus-bishop balance now defines the strategic fight.",
      "This structure highlights a direct knight-and-bishop contrast.",
      "Closed/open square dynamics will decide whether the knight or bishop thrives."
    )),
    MotifPrefixRule(List("blockade"), List(
      "Blockade technique is central to the conversion plan.",
      "Stopping the passed pawn with a blockade is the key task.",
      "A stable blockade can neutralize the opponent's main counterplay."
    )),
    MotifPrefixRule(List("pin_queen"), List(
      "A pin against the queen is a tactical resource in the position.",
      "Queen pin geometry is becoming a practical motif.",
      "The tactical point is creating pressure through a queen pin."
    )),
    MotifPrefixRule(List("pin"), List(
      "Pin geometry is becoming a concrete tactical factor.",
      "A pin motif is shaping move-order choices.",
      "The position features tactical pressure through a pin."
    )),
    MotifPrefixRule(List("skewer_queen"), List(
      "A skewer against the queen is becoming a concrete tactical idea.",
      "Queen-skewer geometry is now part of the calculation tree.",
      "Skewering the queen is an active tactical theme in the position."
    )),
    MotifPrefixRule(List("skewer"), List(
      "A skewer motif is now relevant in the tactical battle.",
      "Line-based skewer ideas are shaping move order.",
      "Skewer geometry is part of the current tactical landscape."
    )),
    MotifPrefixRule(List("xray_queen"), List(
      "X-ray pressure toward the queen is becoming practical.",
      "The tactical idea is x-ray pressure on the queen.",
      "X-ray alignment against the queen is now a concrete resource."
    )),
    MotifPrefixRule(List("xray"), List(
      "X-ray pressure is becoming a practical tactical resource.",
      "Line-piece x-ray geometry now influences move order.",
      "X-ray motifs are part of the current tactical landscape."
    )),
    MotifPrefixRule(List("exchange_sacrifice"), List(
      "An exchange-sacrifice idea can create dominant file control.",
      "Sacrificing the exchange for initiative is now a practical option.",
      "Exchange investment for dominant activity is part of the position."
    )),
    MotifPrefixRule(List("color_complex"), List(
      "Color complex control is the key strategic battleground.",
      "The struggle revolves around weak color complex squares.",
      "Color complex imbalances are guiding both plans."
    )),
    MotifPrefixRule(List("open_file"), List(
      "Open-file control is a central strategic objective.",
      "The open file is the key channel for major-piece activity.",
      "File control along the open line can dictate the middlegame."
    ))
  )


  def templatesFor(motifs: List[String], ply: Int): Option[List[String]] =
    val normalized = motifs.map(normalizeMotifTag).filter(_.nonEmpty)
    val prefixCandidates = normalized.filterNot(RelationObservationCatalog.relationWitnessOnlyMotifTag)
    deferredRelationTemplates(prefixCandidates).getOrElse {
      MotifPrefixRules.find(_.matches(prefixCandidates, ply)).map(_.templates)
    }
