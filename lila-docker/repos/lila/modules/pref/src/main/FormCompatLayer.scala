package lila.pref

import play.api.mvc.Request

// Simplified FormCompatLayer for analysis-only system
// Removed references to moretime, takeback, autoQueen, etc.
object FormCompatLayer:

  private type FormData = Map[String, Seq[String]]

  def apply(pref: Pref, req: Request[?]): FormData =
    reqToFormData(req)
      .pipe(
        moveTo(
          "display",
          List(
            "animation",
            "highlight",
            "destination",
            "coords",
            "pieceNotation"
          )
        )
      )

  private def addMissing(path: String, default: String)(data: FormData): FormData =
    data.updated(path, data.get(path).filter(_.nonEmpty) | List(default))

  private def moveTo(prefix: String, fields: List[String]) =
    moveToAndRename(prefix, fields.map(f => (f, f)))

  private def moveToAndRename(prefix: String, fields: List[(String, String)])(data: FormData): FormData =
    fields.foldLeft(data) { case (d, (orig, dest)) =>
      val newField = s"$prefix.$dest"
      d + (newField -> ~d.get(newField).orElse(d.get(orig)))
    }

  private def reqToFormData(req: Request[?]): FormData =
    (req.body match
      case body: play.api.mvc.AnyContent if body.asFormUrlEncoded.isDefined => body.asFormUrlEncoded.get
      case body: play.api.mvc.AnyContent if body.asMultipartFormData.isDefined =>
        body.asMultipartFormData.get.asFormUrlEncoded
      case _ => Map.empty[String, Seq[String]]
    ) ++ req.queryString
