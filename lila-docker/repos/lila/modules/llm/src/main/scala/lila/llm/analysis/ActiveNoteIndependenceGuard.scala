package lila.llm.analysis

private[llm] object ActiveNoteIndependenceGuard:

  private val MinSentenceChars = 36
  private val MinLeadWords = 6

  def reasons(candidateText: String, priorText: String): List[String] =
    val candidate = Option(candidateText).getOrElse("").trim
    val prior = Option(priorText).getOrElse("").trim
    if candidate.isEmpty || prior.isEmpty then Nil
    else
      val normalizedPrior = normalizedText(prior)
      val sentenceReuse =
        normalizedSentences(candidate).exists(sentence =>
          sentence.length >= MinSentenceChars && normalizedPrior.contains(sentence)
        )
      val leadReuse = sharedLeadWordCount(candidate, prior) >= MinLeadWords
      List(
        Option.when(sentenceReuse || leadReuse)("active_note_prior_phrase_reuse")
      ).flatten

  private def normalizedText(text: String): String =
    Option(text)
      .getOrElse("")
      .toLowerCase
      .replaceAll("""[^\p{L}\p{N}+#-]+""", " ")
      .replaceAll("""\s+""", " ")
      .trim

  private def normalizedSentences(text: String): List[String] =
    Option(text)
      .getOrElse("")
      .split("""(?<=[.!?])\s+|\n+""")
      .toList
      .map(normalizedText)
      .filter(_.length >= MinSentenceChars)

  private def sharedLeadWordCount(left: String, right: String): Int =
    val leftWords = normalizedText(left).split(" ").toList.filter(wordCandidate)
    val rightWords = normalizedText(right).split(" ").toList.filter(wordCandidate)
    leftWords.zip(rightWords).takeWhile { case (l, r) => l == r }.size

  private def wordCandidate(token: String): Boolean =
    token.length >= 3 && token.exists(_.isLetterOrDigit)
