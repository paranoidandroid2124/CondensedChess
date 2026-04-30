package lila.commentary.render.annotation

import lila.commentary.selection.WordingStrength

final case class EnglishLineCommentary(
    comments: Vector[EnglishLineComment],
    boundaries: Vector[LineCommentaryBoundary]
)

final case class EnglishLineComment(
    annotationId: String,
    comment: String,
    primaryProofId: String,
    wordingCap: WordingStrength
)

object EnglishLineCommentaryWriter:

  def write(plan: LineCommentaryPlan): EnglishLineCommentary =
    EnglishLineCommentary(
      comments = plan.notes.map(_.annotationId).filter(_.trim.nonEmpty).distinct.flatMap(annotationId =>
        commentFor(plan.notes.filter(_.annotationId == annotationId))
      ),
      boundaries = plan.boundaries
    )

  private def commentFor(notes: Vector[LineNote]): Option[EnglishLineComment] =
    for
      main <- notes.find(isUsableMain)
      mainLine <- sanLine(main.lineSan)
      result <- notes.find(note => isCompatibleResult(main, note))
      resultPhrase <- resultPhrase(result.meaning)
    yield
      val resourceClause = resourceSentenceClause(main, notes)
      val mainSentence =
        resourceClause match
          case Some((resourceLine, replyLine)) =>
            sentence(s"After $mainLine, $resourceLine is met by $replyLine, and $resultPhrase")
          case None =>
            sentence(s"After $mainLine, $resultPhrase")
      val detailSentences = notes.filter(_.primaryProofId == main.primaryProofId).flatMap(detailSentence)
      EnglishLineComment(
        annotationId = main.annotationId,
        comment = (Vector(mainSentence) ++ detailSentences).mkString(" "),
        primaryProofId = main.primaryProofId,
        wordingCap = WordingStrength.weaker(main.wordingCap, result.wordingCap)
      )

  private def isUsableMain(note: LineNote): Boolean =
    note.kind == LineNoteKind.MainLine &&
      note.meaning == LineNoteMeaning.MainLine &&
      note.wordingCap.rank >= WordingStrength.QualifiedSupport.rank &&
      sanLine(note.lineSan).nonEmpty

  private def isCompatibleResult(main: LineNote, note: LineNote): Boolean =
    note.kind == LineNoteKind.LineResult &&
      note.primaryProofId == main.primaryProofId &&
      note.wordingCap.rank >= WordingStrength.QualifiedSupport.rank &&
      resultPhrase(note.meaning).nonEmpty

  private def resourceSentenceClause(main: LineNote, notes: Vector[LineNote]): Option[(String, String)] =
    notes
      .find(note =>
        note.kind == LineNoteKind.DefensiveResource &&
          note.primaryProofId == main.primaryProofId &&
          note.meaning == LineNoteMeaning.DefensiveResource
      )
      .flatMap(note =>
        for
          resourceLine <- moveLine(note.resourceLine)
          replyLine <- moveLine(note.replyLine)
        yield (resourceLine, replyLine)
      )

  private def detailSentence(note: LineNote): Option[String] =
    note.kind match
      case LineNoteKind.SupportingLine =>
        Option
          .when(note.wordingCap.rank >= WordingStrength.QualifiedSupport.rank)(note)
          .flatMap(note =>
            for
              line <- sanLine(note.lineSan)
              phrase <- resultPhrase(note.meaning)
            yield sentence(s"In $line, $phrase")
          )
      case LineNoteKind.Caution =>
        for
          line <- moveLine(note.testedLine).orElse(sanLine(note.lineSan))
          phrase <- cautionPhrase(note.meaning)
        yield sentence(s"By contrast, $line $phrase")
      case _ => None

  private def resultPhrase(meaning: LineNoteMeaning): Option[String] =
    meaning match
      case LineNoteMeaning.PressurePersists => Some("the pressure stays on")
      case LineNoteMeaning.DoesNotRestoreCounterplay => Some("counterplay still does not return")
      case LineNoteMeaning.ResourceFails => Some("the defensive try falls short")
      case LineNoteMeaning.ResourceWorks => Some("the defensive idea works")
      case LineNoteMeaning.DefensiveHold => Some("the defense holds")
      case LineNoteMeaning.Simplifies => Some("the position simplifies")
      case LineNoteMeaning.Converts => Some("the continuation becomes clearer")
      case _ => None

  private def cautionPhrase(meaning: LineNoteMeaning): Option[String] =
    meaning match
      case LineNoteMeaning.EarlyMoveCaution | LineNoteMeaning.PrematureMove =>
        Some("comes too early")
      case LineNoteMeaning.ReleasesCounterplay =>
        Some("lets counterplay return")
      case _ => None

  private def sanLine(tokens: Vector[String]): Option[String] =
    val cleaned = tokens.map(sanToken)
    Option.when(cleaned.nonEmpty && cleaned.forall(_.nonEmpty))(cleaned.mkString(" "))

  private def moveLine(moves: Vector[LineNoteMove]): Option[String] =
    val cleaned = moves.map(move => sanToken(move.san))
    Option.when(cleaned.nonEmpty && cleaned.forall(_.nonEmpty))(cleaned.mkString(" "))

  private def sanToken(value: String): String =
    value.trim

  private def sentence(value: String): String =
    val normalized =
      value.trim
        .replaceAll("\\s+", " ")
        .replace(" ,", ",")
        .stripSuffix(".")
    s"$normalized."
