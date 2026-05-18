package lila.commentary.analysis

import chess.{ King, Role, Square }
import lila.commentary.model.*

private[commentary] object CommentaryFactSurface:

  def statement(bead: Int, fact: Fact, ctx: NarrativeContext): Option[String] =
    fact match
      case Fact.HangingPiece(square, role, attackers, defenders, _) =>
        hangingStatement(
          bead = bead,
          square = square,
          role = role,
          attackers = attackers,
          defenders = defenders,
          tier = StandardCommentaryClaimPolicy.hangingTier(ctx, square, role, attackers, defenders)
        )

      case Fact.TargetPiece(square, role, attackers, defenders, _) =>
        if StandardCommentaryClaimPolicy.quietStandardPosition(ctx) &&
            !StandardCommentaryClaimPolicy.allowsAmberTier(ctx)
        then None
        else
          val balance = attackDefenseBalance(attackers, defenders)
          Some(
            NarrativeLexicon.pick(bead, List(
              s"Pressure can build against the ${roleLabel(role)} on ${square.key} ($balance).",
              s"The ${roleLabel(role)} on ${square.key} is a practical point to watch ($balance).",
              s"The ${roleLabel(role)} on ${square.key} can become a target if move order slips ($balance)."
            ))
          )

      case Fact.Pin(_, _, pinned, pinnedRole, behind, behindRole, isAbsolute, _) =>
        val abs = if isAbsolute then " (absolute)" else ""
        Some(
          NarrativeLexicon.pick(bead, List(
            s"The ${roleLabel(pinnedRole)} on ${pinned.key} is pinned$abs to the ${roleLabel(behindRole)} on ${behind.key}.",
            s"There's a pin: the ${roleLabel(pinnedRole)} on ${pinned.key} cannot move without exposing the ${roleLabel(behindRole)} on ${behind.key}.",
            s"${pinned.key} is pinned, leaving the ${roleLabel(pinnedRole)} with limited mobility.",
            s"The pin on ${pinned.key} slows coordination of that ${roleLabel(pinnedRole)}, costing valuable tempi.",
            s"The pin restrains the ${roleLabel(pinnedRole)} on ${pinned.key}, reducing practical flexibility."
          ))
        )

      case Fact.Fork(attacker, attackerRole, targets, _) =>
        Some(
          NarrativeLexicon.pick(bead, List(
            s"The ${roleLabel(attackerRole)} on ${attacker.key} has a fork idea against ${targetText(targets)}.",
            s"Watch for a fork by the ${roleLabel(attackerRole)} on ${attacker.key} hitting ${targetText(targets)}.",
            s"A fork motif is in the air: ${attacker.key} can attack ${targetText(targets)}."
          ))
        )

      case Fact.WeakSquare(square, color, reason, _) =>
        val owner = color.name.toLowerCase
        val why = reason.trim
        val detail = if why.nonEmpty then s" ($why)" else ""
        Some(
          NarrativeLexicon.pick(bead, List(
            s"${square.key} is a weak square for $owner$detail.",
            s"The square ${square.key} looks vulnerable$detail.",
            s"A potential outpost on ${square.key} appears$detail."
          ))
        )

      case Fact.Outpost(square, role, _) =>
        Some(
          NarrativeLexicon.pick(bead, List(
            s"${square.key} can serve as an outpost for a ${roleLabel(role)}.",
            s"An outpost on ${square.key} could be valuable for a ${roleLabel(role)}.",
            s"Keep ${square.key} in mind as an outpost square."
          ))
        )

      case Fact.Opposition(_, _, _, isDirect, oppositionType, _) =>
        val kind =
          if oppositionType.nonEmpty && !oppositionType.equalsIgnoreCase("None") then s"${oppositionType.toLowerCase} opposition"
          else if isDirect then "direct opposition"
          else "opposition"
        Some(
          NarrativeLexicon.pick(bead, List(
            s"The kings are in $kind.",
            s"$kind is an important endgame detail.",
            s"King opposition becomes a key factor."
          ))
        )

      case Fact.KingActivity(square, mobility, _, _) =>
        Some(
          NarrativeLexicon.pick(bead, List(
            s"The king on ${square.key} is active (mobility: $mobility).",
            s"King activity matters: ${square.key} has $mobility safe steps.",
            s"The king on ${square.key} is well-placed for the endgame."
          ))
        )

      case _ => None

  def branchReason(
      fact: Fact,
      hangingTier: Option[StandardCommentaryClaimPolicy.HangingTier] = None
  ): Option[String] =
    fact match
      case Fact.HangingPiece(square, role, _, _, _) =>
        hangingTier match
          case Some(StandardCommentaryClaimPolicy.HangingTier.Red) =>
            Some(s"It also keeps the ${roleLabel(role)} on ${square.key} from becoming a tactical liability.")
          case Some(StandardCommentaryClaimPolicy.HangingTier.Amber) =>
            Some(s"It also keeps pressure from building against the ${roleLabel(role)} on ${square.key}.")
          case _ => None
      case Fact.Pin(_, _, pinned, pinnedRole, _, _, _, _) =>
        Some(s"It reduces the pin pressure against the ${roleLabel(pinnedRole)} on ${pinned.key}.")
      case Fact.WeakSquare(square, _, _, _) =>
        Some(s"It prevents longer-term weakening around ${square.key}.")
      case _ => None

  def consequenceBody(fact: Fact): Option[String] =
    fact match
      case Fact.HangingPiece(square, role, _, defenders, _) if defenders.isEmpty =>
        Some(s"it leaves the ${roleLabel(role)} on ${square.key} hanging.")
      case Fact.Pin(_, _, pinned, pinnedRole, behind, behindRole, _, _) =>
        Some(s"it allows a pin on ${pinned.key}, tying the ${roleLabel(pinnedRole)} to the ${roleLabel(behindRole)} on ${behind.key}.")
      case Fact.Fork(attacker, attackerRole, targets, _) if targets.nonEmpty =>
        Some(s"it allows a fork by the ${roleLabel(attackerRole)} on ${attacker.key} against ${targetText(targets)}.")
      case Fact.Skewer(attacker, attackerRole, front, frontRole, back, backRole, _) =>
        Some(s"it allows a skewer: ${roleLabel(attackerRole)} on ${attacker.key} can hit ${roleLabel(frontRole)} on ${front.key} and then ${roleLabel(backRole)} on ${back.key}.")
      case Fact.WeakSquare(square, _, _, _) =>
        Some(s"it creates a durable weakness on ${square.key}.")
      case _ => None

  def issueConsequence(fact: Fact): Option[String] =
    fact match
      case Fact.WeakSquare(square, _, _, _) =>
        Some(s"Consequence: ${square.key} can become a long-term target.")
      case Fact.HangingPiece(square, role, _, _, _) =>
        Some(s"Consequence: the ${roleLabel(role)} on ${square.key} can become a direct tactical target.")
      case _ => None

  def tags(fact: Fact): List[String] =
    fact match
      case _: Fact.Fork                     => List("fork")
      case _: Fact.Pin                      => List("pin")
      case _: Fact.Skewer                   => List("skewer")
      case _: Fact.HangingPiece             => List("hanging_piece")
      case _: Fact.TargetPiece              => List("direct_threat")
      case _: Fact.DoubleCheck              => List("double_check")
      case _: Fact.KingActivity             => List("king_activity")
      case _: Fact.Opposition               => List("opposition")
      case _: Fact.RuleOfSquare             => List("rule_of_square")
      case _: Fact.RookEndgamePattern       => List("rook_endgame_pattern")
      case _: Fact.PawnPromotion            => List("pawn_promotion")
      case _: Fact.TriangulationOpportunity => List("triangulation")
      case _: Fact.Zugzwang                 => List("zugzwang")
      case _: Fact.WeakSquare               => List("weak_square")
      case _: Fact.Outpost                  => List("outpost")
      case _                                => Nil

  def factTags(facts: Iterable[Fact]): List[String] =
    facts.iterator.flatMap(tags).toList.distinct

  def motifTags(motifs: Iterable[Motif]): List[String] =
    val values = scala.collection.mutable.LinkedHashSet.empty[String]
    motifs.foreach {
      case _: Motif.Capture        => values += "capture_sequence"
      case _: Motif.PawnBreak      => values += "center_break_setup"
      case _: Motif.Centralization => values += "piece_activity"
      case _                       =>
    }
    values.toList

  def evidenceTags(facts: Iterable[Fact], motifs: Iterable[Motif]): List[String] =
    (factTags(facts) ++ motifTags(motifs)).distinct

  private def hangingStatement(
      bead: Int,
      square: Square,
      role: Role,
      attackers: List[Square],
      defenders: List[Square],
      tier: StandardCommentaryClaimPolicy.HangingTier
  ): Option[String] =
    val balance = attackDefenseBalance(attackers, defenders)
    tier match
      case StandardCommentaryClaimPolicy.HangingTier.Red =>
        Some(
          NarrativeLexicon.pick(bead, List(
            s"The ${roleLabel(role)} on ${square.key} is hanging ($balance).",
            s"The ${roleLabel(role)} on ${square.key} is underdefended: $balance.",
            s"Keep an eye on the ${roleLabel(role)} on ${square.key} — $balance."
          ))
        )
      case StandardCommentaryClaimPolicy.HangingTier.Amber =>
        Some(
          NarrativeLexicon.pick(bead, List(
            s"Pressure is building against the ${roleLabel(role)} on ${square.key} ($balance).",
            s"The ${roleLabel(role)} on ${square.key} can become a target if the pressure grows ($balance).",
            s"The ${roleLabel(role)} on ${square.key} needs watching ($balance)."
          ))
        )
      case StandardCommentaryClaimPolicy.HangingTier.Suppress =>
        None

  private def attackDefenseBalance(attackers: List[Square], defenders: List[Square]): String =
    val a = attackers.size
    val d = defenders.size
    val aText = if a == 0 then "no attackers" else s"$a ${plural(a, "attacker", "attackers")}"
    val dText = if d == 0 then "no defenders" else s"$d ${plural(d, "defender", "defenders")}"
    if d == 0 then s"$aText, $dText" else s"$aText vs $dText"

  private def plural(n: Int, one: String, many: String): String =
    if n == 1 then one else many

  private def targetText(targets: List[(Square, Role)]): String =
    targets.take(2).map { case (sq, r) => s"${roleLabel(r)} on ${sq.key}" } match
      case a :: b :: Nil => s"$a and $b"
      case a :: Nil      => a
      case _             => "multiple targets"

  def roleLabel(role: Role): String =
    role match
      case chess.Pawn   => "pawn"
      case chess.Knight => "knight"
      case chess.Bishop => "bishop"
      case chess.Rook   => "rook"
      case chess.Queen  => "queen"
      case King         => "king"
