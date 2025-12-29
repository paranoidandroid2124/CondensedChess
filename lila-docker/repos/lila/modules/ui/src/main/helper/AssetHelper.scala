package lila.ui

import lila.ui.ScalatagsTemplate.*
import lila.core.data.Url

trait AssetHelper:
  def assetUrl(path: String): String = s"/assets/$path"
  def assetVersion: String = "1"
  def safeJsonValue(v: play.api.libs.json.JsValue): lila.core.data.SafeJsonStr = 
    lila.core.data.SafeJsonStr(v.toString)
  def imagePreload(url: Option[Url]): Frag = 
    url.fold(emptyFrag)(u => link(rel := "preload", href := u.value, attr("as") := "image"))
