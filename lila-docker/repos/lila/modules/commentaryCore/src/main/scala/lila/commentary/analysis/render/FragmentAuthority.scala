package lila.commentary.analysis.render

private[commentary] enum FragmentAuthority:
  case render_only
  case support_only
  case unsafe_as_truth
  case unsafe_as_lesson
  case candidate_for_future_lesson
  case requires_move_linked_anchor

private[commentary] final case class AuthorityTaggedFragment(
    text: String,
    authority: FragmentAuthority,
    moveLinkedAnchor: Boolean = false,
    sceneGrounded: Boolean = false,
    evidenceBacked: Boolean = false,
    plannerOwned: Boolean = false,
    contractConsistent: Boolean = false,
    generalized: Boolean = false
):
  def rawText: String = Option(text).map(_.trim).getOrElse("")
  def hasRawText: Boolean = rawText.nonEmpty
  def groundedForAdmission: Boolean =
    moveLinkedAnchor || sceneGrounded || evidenceBacked || plannerOwned || contractConsistent
  def releasedText: String =
    if !hasRawText then ""
    else
      authority match
        case FragmentAuthority.unsafe_as_truth           => ""
        case FragmentAuthority.unsafe_as_lesson          => ""
        case FragmentAuthority.candidate_for_future_lesson => ""
        case FragmentAuthority.requires_move_linked_anchor =>
          if moveLinkedAnchor then rawText else ""
        case FragmentAuthority.support_only =>
          if !generalized || groundedForAdmission then rawText else ""
        case FragmentAuthority.render_only => rawText

private[commentary] object FragmentAuthority:

  def supportFragment(
      text: String,
      moveLinkedAnchor: Boolean = false,
      sceneGrounded: Boolean = false,
      evidenceBacked: Boolean = false,
      plannerOwned: Boolean = false,
      contractConsistent: Boolean = false,
      generalized: Boolean = false
  ): AuthorityTaggedFragment =
    AuthorityTaggedFragment(
      text = text,
      authority = FragmentAuthority.support_only,
      moveLinkedAnchor = moveLinkedAnchor,
      sceneGrounded = sceneGrounded,
      evidenceBacked = evidenceBacked,
      plannerOwned = plannerOwned,
      contractConsistent = contractConsistent,
      generalized = generalized
    )

  def releasedFragmentText(
      fragments: List[AuthorityTaggedFragment]
  ): String =
    fragments.flatMap(fragment => Option(fragment.releasedText).map(_.trim).filter(_.nonEmpty)).mkString(" ")

  def rawFragmentText(
      fragments: Iterable[AuthorityTaggedFragment]
  ): String =
    fragments.flatMap(fragment => Option(fragment.rawText).map(_.trim).filter(_.nonEmpty)).mkString(" ")
