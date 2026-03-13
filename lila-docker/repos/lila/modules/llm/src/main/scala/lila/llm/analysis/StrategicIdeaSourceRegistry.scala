package lila.llm.analysis

private[llm] object StrategicIdeaSourceRegistry:

  enum Category:
    case Authoritative
    case DerivedTyped
    case ProseOnly

  final case class Entry(id: String, category: Category)

  val entries: List[Entry] = List(
    Entry("board", Category.Authoritative),
    Entry("position_features", Category.Authoritative),
    Entry("strategic_state", Category.Authoritative),
    Entry("positional_features", Category.Authoritative),
    Entry("piece_activity", Category.Authoritative),
    Entry("structural_weaknesses", Category.Authoritative),
    Entry("prevented_plans", Category.Authoritative),
    Entry("threats_to_us", Category.Authoritative),
    Entry("threats_to_them", Category.Authoritative),
    Entry("pawn_analysis", Category.Authoritative),
    Entry("opponent_pawn_analysis", Category.Authoritative),
    Entry("endgame_features", Category.Authoritative),
    Entry("motifs", Category.Authoritative),
    Entry("route_structural_facts", Category.Authoritative),
    Entry("directional_target_structural_facts", Category.Authoritative),
    Entry("classification", Category.DerivedTyped),
    Entry("structure_profile", Category.DerivedTyped),
    Entry("plan_alignment_reason_codes", Category.DerivedTyped),
    Entry("plan_matches", Category.DerivedTyped),
    Entry("pawn_play_table", Category.DerivedTyped),
    Entry("signal_digest_prose", Category.ProseOnly),
    Entry("plan_name", Category.ProseOnly),
    Entry("long_term_focus", Category.ProseOnly),
    Entry("route_purpose", Category.ProseOnly),
    Entry("directional_target_reasons", Category.ProseOnly),
    Entry("directional_target_prerequisites", Category.ProseOnly),
    Entry("narrative_intent", Category.ProseOnly),
    Entry("narrative_risk", Category.ProseOnly)
  )

  val authoritative: Set[String] = idsFor(Category.Authoritative)
  val derivedTyped: Set[String] = idsFor(Category.DerivedTyped)
  val proseOnly: Set[String] = idsFor(Category.ProseOnly)

  def categoryOf(id: String): Option[Category] =
    entries.find(_.id == id).map(_.category)

  private def idsFor(category: Category): Set[String] =
    entries.collect { case Entry(id, `category`) => id }.toSet
