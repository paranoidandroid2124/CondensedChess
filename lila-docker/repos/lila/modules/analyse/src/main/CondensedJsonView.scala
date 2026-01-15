package lila.analyse

import play.api.libs.json.*
import lila.tree.Node
import lila.core.game.Pov

object CondensedJsonView:

  def apply(
      pov: Pov,
      root: Node,
      pref: lila.core.pref.Pref
  ): JsObject =
    Json.obj(
      "treeParts" -> Json.arr(Node.defaultNodeJsonWriter.writes(root)),
      "orientation" -> pov.color.name,
      "userAnalysis" -> true,
      "game" -> Json.obj(
        "id" -> "synthetic",
        "variant" -> Json.obj(
          "key" -> pov.game.variant.key.value,
          "name" -> pov.game.variant.name
        ),
        "player" -> pov.color.name,
        "status" -> Json.obj("id" -> 20, "name" -> "started"),
        "fen" -> chess.format.Fen.write(pov.game.chess).value,
        "turns" -> pov.game.chess.ply.value,
        "source" -> "ai",
        "speed" -> "correspondence",
        "perf" -> "bullet",
        "rated" -> false
      ),
      "player" -> Json.obj("color" -> pov.color.name),
      "opponent" -> Json.obj("color" -> (!pov.color).name),
      "pref" -> Json.obj(
        // AnalysePref (ui/analyse/src/interfaces.ts)
        "coords" -> pref.coords,
        "is3d" -> pref.is3d,
        "highlight" -> pref.highlight,
        "destination" -> pref.destination,
        "rookCastle" -> (pref.rookCastle != 0),
        "animationDuration" -> pref.animationMillis,
        "moveEvent" -> pref.moveEvent
      ),
      "takebackable" -> false,
      "moretimeable" -> false
    )

  /** Injects AI narration into the JSON structure.
    * This can be used to add the results from the llm module.
    */
  def withNarration(
      json: JsObject,
      narration: Map[String, String] // path -> text
  ): JsObject =
    if narration.isEmpty then json
    else
      val treeParts = (json \ "treeParts").asOpt[JsArray].map: parts =>
        JsArray(parts.value.map {
          case node: JsObject => injectNarration(node, narration, "")
          case other           => other
        })
      treeParts.fold(json)(parts => json ++ Json.obj("treeParts" -> parts))

  private def injectNarration(node: JsObject, narration: Map[String, String], path: String): JsObject =
    val id = (node \ "id").asOpt[String].getOrElse("")
    val fullPath = if path.isEmpty then id else s"$path$id"
    val nodeWithNarration = narration.get(fullPath).fold(node) { text =>
      val safeText = play.twirl.api.HtmlFormat.escape(text).toString
      val comments = (node \ "comments").asOpt[JsArray].getOrElse(JsArray())
      node ++ Json.obj("comments" -> (comments :+ Json.obj(
        "id" -> s"ai:$fullPath",
        "text" -> safeText,
        "by" -> "ai" // explicit source
      )))
    }
    
    (nodeWithNarration \ "children").asOpt[JsArray].fold(nodeWithNarration) { children =>
      val updatedChildren = children.value.map {
        case child: JsObject => injectNarration(child, narration, fullPath)
        case other           => other
      }
      nodeWithNarration ++ Json.obj("children" -> JsArray(updatedChildren))
    }
