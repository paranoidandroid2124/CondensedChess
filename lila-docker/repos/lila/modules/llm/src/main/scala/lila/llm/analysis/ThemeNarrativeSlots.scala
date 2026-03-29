package lila.llm.analysis

final case class ThemeSlots(idea: String, evidence: String, hold: String)

object ThemeNarrativeSlots:
  private val slotsByTheme: Map[String, ThemeSlots] = Map(
    "restriction_prophylaxis" -> ThemeSlots(
      idea = "Restriction/prophylaxis first: limit the opponent plan before expansion.",
      evidence = "Probe lines and structure must show reduced opponent counterplay channels.",
      hold = "If counterplay remains fast, this prophylactic claim is deferred."
    ),
    "piece_redeployment" -> ThemeSlots(
      idea = "Piece redeployment first: improve the worst piece, re-anchor bishops, or claim strong files.",
      evidence = "Evidence requires concrete reroute or file-occupation feasibility with no tactical collapse.",
      hold = "If reroute squares are unstable, redeployment is held."
    ),
    "space_clamp" -> ThemeSlots(
      idea = "Space/clamp first: gain squares while restricting opponent mobility.",
      evidence = "Space gains must be stable under probe replies and center tension.",
      hold = "If the clamp overextends or opens tactical breaks, it is postponed."
    ),
    "weakness_fixation" -> ThemeSlots(
      idea = "Weakness fixation first: create and keep static targets.",
      evidence = "Evidence requires persistent weak points such as IQPs or backward pawns after best practical replies.",
      hold = "If weaknesses can be released quickly, fixation is downgraded."
    ),
    "pawn_break_preparation" -> ThemeSlots(
      idea = "Pawn-break preparation first: set conditions before committing to a break.",
      evidence = "Probe evidence must show break timing without losing center control.",
      hold = "If prep is incomplete or timing fails, the break stays conditional."
    ),
    "favorable_exchange" -> ThemeSlots(
      idea = "Favorable exchange first: trade only when the resulting structure or bad-piece liquidation improves.",
      evidence = "Evidence needs exchange sequences that preserve evaluation trend while keeping the better structure or king shield.",
      hold = "If exchanges release opponent activity, simplification is held."
    ),
    "flank_infrastructure" -> ThemeSlots(
      idea = "Flank infrastructure first: build hooks and lift channels before direct attack.",
      evidence = "Evidence must show rook-pawn/hook routes survive best defensive replies.",
      hold = "If center instability refutes flank timing, the attack is deferred."
    ),
    "advantage_transformation" -> ThemeSlots(
      idea = "Advantage transformation first: convert one edge type into a durable one, often via passer manufacture or invasion.",
      evidence = "Evidence requires conversion lines that retain advantage through transition and keep the transformed edge alive.",
      hold = "If conversion leaks initiative or structure, transformation is delayed."
    ),
    "immediate_tactical_gain" -> ThemeSlots(
      idea = "Immediate tactical gain takes priority over long-horizon planning, including batteries, overloads, and clearance ideas.",
      evidence = "Evidence requires forcing motifs or pressure alignments with stable tactical follow-through.",
      hold = "If tactical lines are not forcing, strategic claims resume priority."
    ),
    "unknown" -> ThemeSlots(
      idea = "Strategic route remains plan-first but conditional.",
      evidence = "Probe + structural evidence is required before promotion.",
      hold = "Without evidence, claims remain in hold status."
    )
  )

  def forTheme(themeId: String): ThemeSlots =
    slotsByTheme.getOrElse(themeId, slotsByTheme("unknown"))
